/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation.
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

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidationException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.http.HttpStatus;
import org.onap.policy.clamp.acm.participant.a1pms.exception.A1PolicyServiceException;
import org.onap.policy.clamp.acm.participant.a1pms.models.ConfigurationEntity;
import org.onap.policy.clamp.acm.participant.a1pms.webclient.AcA1PmsClient;
import org.onap.policy.clamp.acm.participant.intermediary.api.AutomationCompositionElementListener;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class handles implementation of automationCompositionElement updates.
 */
@Component
@RequiredArgsConstructor
public class AutomationCompositionElementHandler implements AutomationCompositionElementListener {

    private static final Coder CODER = new StandardCoder();

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Setter
    private ParticipantIntermediaryApi intermediaryApi;

    private final AcA1PmsClient acA1PmsClient;

    // Map of acElement Id and A1PMS services
    @Getter(AccessLevel.PACKAGE)
    private final Map<UUID, ConfigurationEntity> configRequestMap = new HashMap<>();

    /**
     * Handle a automation composition element state change.
     *
     * @param automationCompositionElementId the ID of the automation composition element
     * @param currentState                   the current state of the automation composition element
     * @param newState                       the state to which the automation composition element is changing to
     * @throws PfModelException in case of a model exception
     */
    @Override
    public void automationCompositionElementStateChange(ToscaConceptIdentifier automationCompositionId,
            UUID automationCompositionElementId, AutomationCompositionState currentState,
            AutomationCompositionOrderedState newState) throws A1PolicyServiceException {
        switch (newState) {
            case UNINITIALISED:
                ConfigurationEntity configurationEntity = configRequestMap.get(automationCompositionElementId);
                if (configurationEntity != null && acA1PmsClient.isPmsHealthy()) {
                    acA1PmsClient.deleteService(configurationEntity.getPolicyServiceEntities());
                    configRequestMap.remove(automationCompositionElementId);
                    intermediaryApi.updateAutomationCompositionElementState(automationCompositionId,
                            automationCompositionElementId, newState, AutomationCompositionState.UNINITIALISED,
                            ParticipantMessageType.AUTOMATION_COMPOSITION_STATE_CHANGE);
                } else {
                    LOGGER.warn("Failed to connect with A1PMS. Service configuration is: {}", configurationEntity);
                    throw new A1PolicyServiceException(HttpStatus.SC_SERVICE_UNAVAILABLE,
                            "Unable to connect with A1PMS");
                }
                break;
            case PASSIVE:
                intermediaryApi.updateAutomationCompositionElementState(automationCompositionId,
                        automationCompositionElementId, newState, AutomationCompositionState.PASSIVE,
                        ParticipantMessageType.AUTOMATION_COMPOSITION_STATE_CHANGE);
                break;
            case RUNNING:
                intermediaryApi.updateAutomationCompositionElementState(automationCompositionId,
                        automationCompositionElementId, newState, AutomationCompositionState.RUNNING,
                        ParticipantMessageType.AUTOMATION_COMPOSITION_STATE_CHANGE);
                break;
            default:
                LOGGER.warn("Cannot transition from state {} to state {}", currentState, newState);
                break;
        }
    }

    /**
     * Callback method to handle an update on an automation composition element.
     *
     * @param element      the information on the automation composition element
     * @param nodeTemplate toscaNodeTemplate
     */
    @Override
    public void automationCompositionElementUpdate(ToscaConceptIdentifier automationCompositionId,
            AutomationCompositionElement element, ToscaNodeTemplate nodeTemplate) throws A1PolicyServiceException {
        try {
            var configurationEntity = CODER.convert(nodeTemplate.getProperties(), ConfigurationEntity.class);
            Set<ConstraintViolation<ConfigurationEntity>> violations =
                    Validation.buildDefaultValidatorFactory().getValidator().validate(configurationEntity);
            if (violations.isEmpty()) {
                if (acA1PmsClient.isPmsHealthy()) {
                    acA1PmsClient.createService(configurationEntity.getPolicyServiceEntities());
                    configRequestMap.put(element.getId(), configurationEntity);

                    intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, element.getId(),
                            AutomationCompositionOrderedState.PASSIVE, AutomationCompositionState.PASSIVE,
                            ParticipantMessageType.AUTOMATION_COMPOSITION_STATE_CHANGE);
                } else {
                    LOGGER.error("Failed to connect with A1PMS");
                    throw new A1PolicyServiceException(HttpStatus.SC_SERVICE_UNAVAILABLE,
                            "Unable to connect with A1PMS");
                }
            } else {
                LOGGER.error("Violations found in the config request parameters: {}", violations);
                throw new ValidationException("Constraint violations in the config request");
            }
        } catch (ValidationException | CoderException e) {
            LOGGER.error("Error invoking the A1PMS request for the config ", e);
            throw new A1PolicyServiceException(HttpStatus.SC_BAD_REQUEST, "Invalid Configuration", e);
        } catch (A1PolicyServiceException e) {
            LOGGER.error("Error invoking the A1PMS request for the config ", e);
            throw e;
        }
    }
}
