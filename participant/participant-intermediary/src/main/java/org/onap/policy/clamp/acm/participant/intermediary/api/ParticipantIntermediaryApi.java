/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation.
 *  Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.clamp.acm.participant.intermediary.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaProperty;

/**
 * This interface is used by participant implementations to use the participant intermediary.
 */
public interface ParticipantIntermediaryApi {

    /**
     * Register a listener for automation composition elements that are mediated by the intermediary.
     *
     * @param automationCompositionElementListener The automation composition element listener to register
     */
    void registerAutomationCompositionElementListener(
        AutomationCompositionElementListener automationCompositionElementListener);

    /**
     * Get participants loops from the intermediary API.
     *
     * @param name the participant name, null for all
     * @param version the participant version, null for all
     * @return the participants
     */
    List<Participant> getParticipants(String name, String version);

    /**
     * Get common properties of a automation composition element.
     *
     * @param acElementDef the automation composition element definition
     * @return the common properties
     */
    Map<String, ToscaProperty> getAcElementDefinitionCommonProperties(ToscaConceptIdentifier acElementDef);

    /**
     * Update the state of a participant.
     *
     * @param definition the definition of the participant to update the state on
     * @param state the state of the participant
     * @return the participant
     */
    Participant updateParticipantState(ToscaConceptIdentifier definition, ParticipantState state);

    /**
     * Get automation compositions from the intermediary API.
     *
     * @param name the automation composition element name, null for all
     * @param version the automation composition element version, null for all
     * @return the automation composition elements
     */
    AutomationCompositions getAutomationCompositions(String name, String version);

    /**
     * Get automation composition elements from the intermediary API.
     *
     * @param name the automation composition element name, null for all
     * @param version the automation composition element version, null for all
     * @return the automation composition elements
     */
    Map<UUID, AutomationCompositionElement> getAutomationCompositionElements(String name, String version);

    /**
     * Get automation composition element from the intermediary API.
     *
     * @param id automation composition element ID
     * @return the automation composition element
     */
    AutomationCompositionElement getAutomationCompositionElement(UUID id);

    /**
     * Update the state of a automation composition element.
     *
     * @param id the ID of the automation composition element to update the state on
     * @param currentState the state of the automation composition element
     * @param newState the state of the automation composition element
     * @return AutomationCompositionElement updated automation composition element
     */
    AutomationCompositionElement updateAutomationCompositionElementState(ToscaConceptIdentifier automationCompositionId,
        UUID id, AutomationCompositionOrderedState currentState, AutomationCompositionState newState,
        ParticipantMessageType messageType);
}
