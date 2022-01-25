/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.simulator.main.handler;

import java.time.Instant;
import java.util.UUID;
import lombok.Setter;
import org.onap.policy.clamp.acm.participant.intermediary.api.AutomationCompositionElementListener;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.models.acm.concepts.AcElementStatistics;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class handles implementation of automationCompositionElement updates.
 */
@Component
public class AutomationCompositionElementHandler implements AutomationCompositionElementListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutomationCompositionElementHandler.class);

    @Setter
    private ParticipantIntermediaryApi intermediaryApi;

    /**
     * Callback method to handle a automation composition element state change.
     *
     * @param automationCompositionElementId the ID of the automation composition element
     * @param currentState the current state of the automation composition element
     * @param newState the state to which the automation composition element is changing to
     * @throws PfModelException in case of an exception
     */
    @Override
    public void automationCompositionElementStateChange(ToscaConceptIdentifier automationCompositionId,
        UUID automationCompositionElementId, AutomationCompositionState currentState,
        AutomationCompositionOrderedState newState) throws PfModelException {
        switch (newState) {
            case UNINITIALISED:
                intermediaryApi.updateAutomationCompositionElementState(automationCompositionId,
                    automationCompositionElementId, newState, AutomationCompositionState.UNINITIALISED,
                    ParticipantMessageType.AUTOMATION_COMPOSITION_STATE_CHANGE);
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
                LOGGER.debug("Unknown orderedstate {}", newState);
                break;
        }
    }

    /**
     * Callback method to handle an update on a automation composition element.
     *
     * @param element the information on the automation composition element
     * @param acElementDefinition toscaNodeTemplate
     * @throws PfModelException in case of an exception
     */
    @Override
    public void automationCompositionElementUpdate(ToscaConceptIdentifier automationCompositionId,
        AutomationCompositionElement element, ToscaNodeTemplate acElementDefinition) throws PfModelException {
        intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, element.getId(),
            element.getOrderedState(), AutomationCompositionState.PASSIVE,
            ParticipantMessageType.AUTOMATION_COMPOSITION_UPDATE);
    }

    @Override
    public void handleStatistics(UUID automationCompositionElementId) throws PfModelException {
        var acElement = intermediaryApi.getAutomationCompositionElement(automationCompositionElementId);
        if (acElement != null) {
            var acElementStatistics = new AcElementStatistics();
            acElementStatistics.setState(acElement.getState());
            acElementStatistics.setTimeStamp(Instant.now());
            intermediaryApi.updateAutomationCompositionElementStatistics(automationCompositionElementId,
                acElementStatistics);
        }
    }

}
