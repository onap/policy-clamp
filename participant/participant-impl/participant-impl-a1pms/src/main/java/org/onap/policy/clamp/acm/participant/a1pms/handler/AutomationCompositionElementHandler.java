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
import java.util.HashMap;
import java.util.Map;
import org.apache.hc.core5.http.HttpStatus;
import org.onap.policy.clamp.acm.participant.a1pms.exception.A1PolicyServiceException;
import org.onap.policy.clamp.acm.participant.a1pms.models.ConfigurationEntity;
import org.onap.policy.clamp.acm.participant.a1pms.webclient.AcA1PmsClient;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.InstanceElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.acm.participant.intermediary.api.impl.AcElementListenerV3;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class handles implementation of automationCompositionElement updates.
 */
@Component
public class AutomationCompositionElementHandler extends AcElementListenerV3 {

    private static final Coder CODER = new StandardCoder();

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final AcA1PmsClient acA1PmsClient;

    public AutomationCompositionElementHandler(ParticipantIntermediaryApi intermediaryApi,
        AcA1PmsClient acA1PmsClient) {
        super(intermediaryApi);
        this.acA1PmsClient = acA1PmsClient;
    }

    /**
     * Handle a automation composition element state change.
     *
     * @param compositionElement the information of the Automation Composition Definition Element
     * @param instanceElement the information of the Automation Composition Instance Element
     * @throws A1PolicyServiceException in case of a model exception
     */
    @Override
    public void undeploy(CompositionElementDto compositionElement, InstanceElementDto instanceElement)
            throws A1PolicyServiceException {
        Map<String, Object> properties = new HashMap<>(compositionElement.inProperties());
        properties.putAll(instanceElement.inProperties());
        var configurationEntity = getConfigurationEntity(properties);
        if (configurationEntity != null && acA1PmsClient.isPmsHealthy()) {
            acA1PmsClient.deleteService(configurationEntity.getPolicyServiceEntities());
            intermediaryApi.updateAutomationCompositionElementState(instanceElement.instanceId(),
                    instanceElement.elementId(), DeployState.UNDEPLOYED, null,
                    StateChangeResult.NO_ERROR, "Undeployed");
        } else {
            LOGGER.warn("Failed to connect with A1PMS. Service configuration is: {}", configurationEntity);
            throw new A1PolicyServiceException(HttpStatus.SC_SERVICE_UNAVAILABLE, "Unable to connect with A1PMS");
        }
    }

    /**
     * Callback method to handle an update on an automation composition element.
     *
     * @param compositionElement the information of the Automation Composition Definition Element
     * @param instanceElement the information of the Automation Composition Instance Element
     * @throws A1PolicyServiceException in case of a model exception
     */
    @Override
    public void deploy(CompositionElementDto compositionElement, InstanceElementDto instanceElement)
            throws A1PolicyServiceException {
        Map<String, Object> properties = new HashMap<>(compositionElement.inProperties());
        properties.putAll(instanceElement.inProperties());
        try {
            var configurationEntity = getConfigurationEntity(properties);
            if (acA1PmsClient.isPmsHealthy()) {
                acA1PmsClient.createService(configurationEntity.getPolicyServiceEntities());

                intermediaryApi.updateAutomationCompositionElementState(instanceElement.instanceId(),
                        instanceElement.elementId(), DeployState.DEPLOYED, null,
                        StateChangeResult.NO_ERROR, "Deployed");
            } else {
                LOGGER.error("Failed to connect with A1PMS");
                throw new A1PolicyServiceException(HttpStatus.SC_SERVICE_UNAVAILABLE, "Unable to connect with A1PMS");
            }
        } catch (ValidationException | A1PolicyServiceException e) {
            throw new A1PolicyServiceException(HttpStatus.SC_BAD_REQUEST, "Invalid Configuration", e);
        }
    }

    private ConfigurationEntity getConfigurationEntity(Map<String, Object> properties) throws A1PolicyServiceException {
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
            throw new A1PolicyServiceException(HttpStatus.SC_BAD_REQUEST, "Invalid Configuration", e);
        }
    }
}
