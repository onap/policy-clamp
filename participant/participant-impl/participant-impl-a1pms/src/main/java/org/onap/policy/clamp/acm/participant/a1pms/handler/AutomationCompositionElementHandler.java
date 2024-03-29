/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2024 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.a1pms.handler;

import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.http.HttpStatus;
import org.onap.policy.clamp.acm.participant.a1pms.exception.A1PolicyServiceException;
import org.onap.policy.clamp.acm.participant.a1pms.models.ConfigurationEntity;
import org.onap.policy.clamp.acm.participant.a1pms.webclient.AcA1PmsClient;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.acm.participant.intermediary.api.impl.AcElementListenerV1;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
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
public class AutomationCompositionElementHandler extends AcElementListenerV1 {

    private static final Coder CODER = new StandardCoder();

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final AcA1PmsClient acA1PmsClient;

    // Map of acElement Id and A1PMS services
    @Getter(AccessLevel.PACKAGE)
    private final Map<UUID, ConfigurationEntity> configRequestMap = new ConcurrentHashMap<>();

    public AutomationCompositionElementHandler(ParticipantIntermediaryApi intermediaryApi,
        AcA1PmsClient acA1PmsClient) {
        super(intermediaryApi);
        this.acA1PmsClient = acA1PmsClient;
    }

    /**
     * Handle a automation composition element state change.
     *
     * @param automationCompositionId the ID of the automation composition
     * @param automationCompositionElementId the ID of the automation composition element
     * @throws A1PolicyServiceException in case of a model exception
     */
    @Override
    public void undeploy(UUID automationCompositionId, UUID automationCompositionElementId)
            throws A1PolicyServiceException {
        var configurationEntity = configRequestMap.get(automationCompositionElementId);
        if (configurationEntity != null && acA1PmsClient.isPmsHealthy()) {
            acA1PmsClient.deleteService(configurationEntity.getPolicyServiceEntities());
            configRequestMap.remove(automationCompositionElementId);
            intermediaryApi.updateAutomationCompositionElementState(automationCompositionId,
                    automationCompositionElementId, DeployState.UNDEPLOYED, null, StateChangeResult.NO_ERROR,
                    "Undeployed");
        } else {
            LOGGER.warn("Failed to connect with A1PMS. Service configuration is: {}", configurationEntity);
            throw new A1PolicyServiceException(HttpStatus.SC_SERVICE_UNAVAILABLE, "Unable to connect with A1PMS");
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
            throws A1PolicyServiceException {
        try {
            var configurationEntity = CODER.convert(properties, ConfigurationEntity.class);
            var violations = Validation.buildDefaultValidatorFactory().getValidator().validate(configurationEntity);
            if (violations.isEmpty()) {
                if (acA1PmsClient.isPmsHealthy()) {
                    acA1PmsClient.createService(configurationEntity.getPolicyServiceEntities());
                    configRequestMap.put(element.getId(), configurationEntity);

                    intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, element.getId(),
                            DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Deployed");
                } else {
                    LOGGER.error("Failed to connect with A1PMS");
                    throw new A1PolicyServiceException(HttpStatus.SC_SERVICE_UNAVAILABLE,
                            "Unable to connect with A1PMS");
                }
            } else {
                LOGGER.error("Violations found in the config request parameters: {}", violations);
                throw new ValidationException("Constraint violations in the config request");
            }
        } catch (ValidationException | CoderException | A1PolicyServiceException e) {
            throw new A1PolicyServiceException(HttpStatus.SC_BAD_REQUEST, "Invalid Configuration", e);
        }
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
            } catch (ValidationException | CoderException e) {
                throw new A1PolicyServiceException(HttpStatus.SC_BAD_REQUEST, "Invalid Configuration", e);
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
}
