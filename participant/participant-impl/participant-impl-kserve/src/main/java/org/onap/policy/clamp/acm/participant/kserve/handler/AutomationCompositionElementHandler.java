/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.clamp.acm.participant.kserve.handler;

import io.kubernetes.client.openapi.ApiException;
import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpStatus;
import org.onap.policy.clamp.acm.participant.intermediary.api.AutomationCompositionElementListener;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.acm.participant.kserve.exception.KserveException;
import org.onap.policy.clamp.acm.participant.kserve.k8s.InferenceServiceValidator;
import org.onap.policy.clamp.acm.participant.kserve.k8s.KserveClient;
import org.onap.policy.clamp.acm.participant.kserve.models.ConfigurationEntity;
import org.onap.policy.clamp.acm.participant.kserve.models.KserveInferenceEntity;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.base.PfModelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class handles implementation of automationCompositionElement updates.
 */
@Component
@RequiredArgsConstructor
public class AutomationCompositionElementHandler implements AutomationCompositionElementListener {

    private static final Coder CODER = new StandardCoder();

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private final ParticipantIntermediaryApi intermediaryApi;

    private final KserveClient kserveClient;

    @Getter(AccessLevel.PACKAGE)
    private final Map<UUID, ConfigurationEntity> configRequestMap = new ConcurrentHashMap<>();

    private static class ThreadConfig {

        private int uninitializedToPassiveTimeout = 60;
        private int statusCheckInterval = 30;
    }

    @Override
    public void undeploy(UUID automationCompositionId, UUID automationCompositionElementId) {
        var configurationEntity = configRequestMap.get(automationCompositionElementId);
        if (configurationEntity != null) {
            try {
                for (KserveInferenceEntity kserveInferenceEntity : configurationEntity.getKserveInferenceEntities()) {
                    kserveClient.undeployInferenceService(kserveInferenceEntity.getNamespace(),
                            kserveInferenceEntity.getName());
                }
                configRequestMap.remove(automationCompositionElementId);
                intermediaryApi.updateAutomationCompositionElementState(automationCompositionId,
                        automationCompositionElementId, DeployState.UNDEPLOYED, null, StateChangeResult.NO_ERROR,
                        "Undeployed");
            } catch (IOException | ApiException exception) {
                LOGGER.warn("Deletion of Inference service failed", exception);
            }
        }
    }

    /**
     * Callback method to handle an update on an automation composition element.
     *
     * @param automationCompositionId the ID of the automation composition
     * @param element the information on the automation composition element
     * @param properties properties Map
     */
    @Override
    public void deploy(UUID automationCompositionId, AcElementDeploy element, Map<String, Object> properties)
            throws PfModelException {
        try {
            var configurationEntity = CODER.convert(properties, ConfigurationEntity.class);
            var violations = Validation.buildDefaultValidatorFactory().getValidator().validate(configurationEntity);
            if (violations.isEmpty()) {
                boolean isAllInferenceSvcDeployed = true;
                var config = CODER.convert(properties, ThreadConfig.class);
                for (KserveInferenceEntity kserveInferenceEntity : configurationEntity.getKserveInferenceEntities()) {
                    kserveClient.deployInferenceService(kserveInferenceEntity.getNamespace(),
                            kserveInferenceEntity.getPayload());

                    if (!checkInferenceServiceStatus(kserveInferenceEntity.getName(),
                            kserveInferenceEntity.getNamespace(), config.uninitializedToPassiveTimeout,
                            config.statusCheckInterval)) {
                        isAllInferenceSvcDeployed = false;
                        break;
                    }
                }
                if (isAllInferenceSvcDeployed) {
                    configRequestMap.put(element.getId(), configurationEntity);
                    intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, element.getId(),
                            DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Deployed");
                } else {
                    LOGGER.error("Inference Service deployment failed");
                }
            } else {
                LOGGER.error("Violations found in the config request parameters: {}", violations);
                throw new ValidationException("Constraint violations in the config request");
            }
        } catch (CoderException e) {
            throw new KserveException(HttpStatus.SC_BAD_REQUEST, "Invalid inference service configuration", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KserveException("Interrupt in configuring the inference service", e);
        } catch (IOException | ExecutionException | ApiException e) {
            throw new KserveException("Failed to configure the inference service", e);
        }
    }

    /**
     * Check the status of Inference Service.
     *
     * @param inferenceServiceName name of the inference service
     * @param namespace kubernetes namespace
     * @param timeout Inference service time check
     * @param statusCheckInterval Status check time interval
     * @return status of the inference service
     * @throws ExecutionException Exception on execution
     * @throws InterruptedException Exception on inference service status check
     */
    public boolean checkInferenceServiceStatus(String inferenceServiceName, String namespace, int timeout,
            int statusCheckInterval) throws ExecutionException, InterruptedException {
        // Invoke runnable thread to check pod status
        Future<String> result = executor.submit(new InferenceServiceValidator(inferenceServiceName, namespace, timeout,
                statusCheckInterval, kserveClient), "Done");
        return (!result.get().isEmpty()) && result.isDone();
    }

    @Override
    public void lock(UUID instanceId, UUID elementId) throws PfModelException {
        intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId, null, LockState.LOCKED,
                StateChangeResult.NO_ERROR, "Locked");
    }

    @Override
    public void unlock(UUID instanceId, UUID elementId) throws PfModelException {
        intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId, null, LockState.UNLOCKED,
                StateChangeResult.NO_ERROR, "Unlocked");
    }

    @Override
    public void delete(UUID instanceId, UUID elementId) throws PfModelException {
        intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId, DeployState.DELETED, null,
                StateChangeResult.NO_ERROR, "Deleted");
    }

    @Override
    public void update(UUID instanceId, AcElementDeploy element, Map<String, Object> properties)
            throws PfModelException {
        intermediaryApi.updateAutomationCompositionElementState(instanceId, element.getId(), DeployState.DEPLOYED, null,
                StateChangeResult.NO_ERROR, "Update not supported");
    }

    @Override
    public void prime(UUID compositionId, List<AutomationCompositionElementDefinition> elementDefinitionList)
            throws PfModelException {
        intermediaryApi.updateCompositionState(compositionId, AcTypeState.PRIMED, StateChangeResult.NO_ERROR, "Primed");
    }

    @Override
    public void deprime(UUID compositionId) throws PfModelException {
        intermediaryApi.updateCompositionState(compositionId, AcTypeState.COMMISSIONED, StateChangeResult.NO_ERROR,
                "Deprimed");
    }

    @Override
    public void handleRestartComposition(UUID compositionId,
            List<AutomationCompositionElementDefinition> elementDefinitionList, AcTypeState state)
            throws PfModelException {
        var finalState = AcTypeState.PRIMED.equals(state) || AcTypeState.PRIMING.equals(state) ? AcTypeState.PRIMED
                : AcTypeState.COMMISSIONED;
        intermediaryApi.updateCompositionState(compositionId, finalState, StateChangeResult.NO_ERROR, "Restarted");
    }

    @Override
    public void handleRestartInstance(UUID automationCompositionId, AcElementDeploy element,
            Map<String, Object> properties, DeployState deployState, LockState lockState) throws PfModelException {
        if (DeployState.DEPLOYING.equals(deployState)) {
            deploy(automationCompositionId, element, properties);
            return;
        }
        if (DeployState.UNDEPLOYING.equals(deployState) || DeployState.DEPLOYED.equals(deployState)
                || DeployState.UPDATING.equals(deployState)) {
            try {
                var configurationEntity = CODER.convert(properties, ConfigurationEntity.class);
                configRequestMap.put(element.getId(), configurationEntity);
            } catch (CoderException e) {
                throw new KserveException(HttpStatus.SC_BAD_REQUEST, "Invalid inference service configuration", e);
            }
        }
        if (DeployState.UNDEPLOYING.equals(deployState)) {
            undeploy(automationCompositionId, element.getId());
            return;
        }
        deployState = AcmUtils.deployCompleted(deployState);
        lockState = AcmUtils.lockCompleted(deployState, lockState);
        intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, element.getId(), deployState,
                lockState, StateChangeResult.NO_ERROR, "Restarted");
    }

    @Override
    public void migrate(UUID automationCompositionId, AcElementDeploy element, UUID compositionTargetId,
            Map<String, Object> properties) throws PfModelException {
        intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, element.getId(),
                DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Migrated");
    }
}
