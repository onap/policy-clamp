/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation.
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
import io.opentelemetry.context.Context;
import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.http.HttpStatus;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.InstanceElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.acm.participant.intermediary.api.impl.AcElementListenerV2;
import org.onap.policy.clamp.acm.participant.kserve.exception.KserveException;
import org.onap.policy.clamp.acm.participant.kserve.k8s.InferenceServiceValidator;
import org.onap.policy.clamp.acm.participant.kserve.k8s.KserveClient;
import org.onap.policy.clamp.acm.participant.kserve.models.ConfigurationEntity;
import org.onap.policy.clamp.acm.participant.kserve.models.KserveInferenceEntity;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
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
public class AutomationCompositionElementHandler extends AcElementListenerV2 {

    private static final Coder CODER = new StandardCoder();

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ExecutorService executor = Context.taskWrapping(
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));

    private final KserveClient kserveClient;

    public AutomationCompositionElementHandler(ParticipantIntermediaryApi intermediaryApi, KserveClient kserveClient) {
        super(intermediaryApi);
        this.kserveClient = kserveClient;
    }

    private static class ThreadConfig {

        private int uninitializedToPassiveTimeout = 60;
        private int statusCheckInterval = 30;
    }

    @Override
    public void undeploy(CompositionElementDto compositionElement, InstanceElementDto instanceElement)
            throws PfModelException {
        Map<String, Object> properties = new HashMap<>(compositionElement.inProperties());
        properties.putAll(instanceElement.inProperties());
        var configurationEntity = getConfigurationEntity(properties);
        if (configurationEntity != null) {
            try {
                for (KserveInferenceEntity kserveInferenceEntity : configurationEntity.getKserveInferenceEntities()) {
                    kserveClient.undeployInferenceService(kserveInferenceEntity.getNamespace(),
                            kserveInferenceEntity.getName());
                }
                intermediaryApi.updateAutomationCompositionElementState(instanceElement.instanceId(),
                        instanceElement.elementId(), DeployState.UNDEPLOYED, null,
                        StateChangeResult.NO_ERROR, "Undeployed");
            } catch (IOException | ApiException exception) {
                LOGGER.warn("Deletion of Inference service failed", exception);
                intermediaryApi.updateAutomationCompositionElementState(instanceElement.instanceId(),
                        instanceElement.elementId(), DeployState.DEPLOYED, null,
                        StateChangeResult.FAILED, "Undeploy Failed");
            }
        }
    }

    /**
     * Callback method to handle an update on an automation composition element.
     *
     * @param compositionElement the information of the Automation Composition Definition Element
     * @param instanceElement the information of the Automation Composition Instance Element
     * @throws PfModelException if error occurs
     */
    @Override
    public void deploy(CompositionElementDto compositionElement, InstanceElementDto instanceElement)
            throws PfModelException {
        Map<String, Object> properties = new HashMap<>(compositionElement.inProperties());
        properties.putAll(instanceElement.inProperties());
        try {
            var configurationEntity = getConfigurationEntity(properties);
            boolean isAllInferenceSvcDeployed = true;
            var config = getThreadConfig(properties);
            for (var kserveInferenceEntity : configurationEntity.getKserveInferenceEntities()) {
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
                intermediaryApi.updateAutomationCompositionElementState(instanceElement.instanceId(),
                        instanceElement.elementId(), DeployState.DEPLOYED, null,
                        StateChangeResult.NO_ERROR, "Deployed");
            } else {
                LOGGER.error("Inference Service deployment failed");
                intermediaryApi.updateAutomationCompositionElementState(instanceElement.instanceId(),
                        instanceElement.elementId(), DeployState.UNDEPLOYED, null,
                        StateChangeResult.FAILED, "Deploy Failed");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KserveException("Interrupt in configuring the inference service", e);
        } catch (IOException | ExecutionException | ApiException e) {
            throw new KserveException("Failed to configure the inference service", e);
        }

    }

    private ConfigurationEntity getConfigurationEntity(Map<String, Object> properties) throws KserveException {
        try {
            var configurationEntity = CODER.convert(properties, ConfigurationEntity.class);
            try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
                var violations = validatorFactory.getValidator().validate(configurationEntity);
                if (!violations.isEmpty()) {
                    LOGGER.error("Violations found in the config request parameters: {}", violations);
                    throw new ValidationException("Constraint violations in the config request");
                }
            }
            return  configurationEntity;
        } catch (CoderException e) {
            throw new KserveException(HttpStatus.SC_BAD_REQUEST, "Invalid inference service configuration", e);
        }
    }

    private ThreadConfig getThreadConfig(Map<String, Object> properties) throws KserveException {
        try {
            return CODER.convert(properties, ThreadConfig.class);
        } catch (CoderException e) {
            throw new KserveException(HttpStatus.SC_BAD_REQUEST, "Invalid inference service configuration", e);
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
        var result = executor.submit(new InferenceServiceValidator(inferenceServiceName, namespace, timeout,
                statusCheckInterval, kserveClient), "Done");
        return (!result.get().isEmpty()) && result.isDone();
    }
}
