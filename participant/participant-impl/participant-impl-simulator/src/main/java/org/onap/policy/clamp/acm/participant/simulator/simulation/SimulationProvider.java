/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.simulator.simulation;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionException;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.messages.rest.TypedSimpleResponse;
import org.springframework.stereotype.Service;

/**
 * This provider class simulation of participants and automation composition elements.
 */
@Service
public class SimulationProvider {

    private final ParticipantIntermediaryApi intermediaryApi;

    /**
     * Create a participant simulation provider.
     *
     * @param intermediaryApi the intermediary to use for talking to the CLAMP runtime
     */
    public SimulationProvider(ParticipantIntermediaryApi intermediaryApi) {
        this.intermediaryApi = intermediaryApi;
    }

    /**
     * Get the automation compositions.
     *
     * @param name the automationComposition, null to get all
     * @param version the automationComposition, null to get all
     * @return the automation compositions
     * @throws AutomationCompositionException on errors getting the automation compositions
     */
    public AutomationCompositions getAutomationCompositions(String name, String version)
        throws AutomationCompositionException {
        return intermediaryApi.getAutomationCompositions(name, version);
    }

    /**
     * Get the simulated automation composition elements.
     *
     * @param name the automationCompositionElement, null to get all
     * @param version the automationCompositionElement, null to get all
     * @return the automation composition elements
     */
    public Map<UUID, AutomationCompositionElement> getAutomationCompositionElements(String name, String version) {
        return intermediaryApi.getAutomationCompositionElements(name, version);
    }

    /**
     * Update the given automation composition element in the simulator.
     *
     * @param element the automation composition element to update
     * @return response simple response returned
     */
    public TypedSimpleResponse<AutomationCompositionElement> updateAutomationCompositionElement(
        AutomationCompositionElement element) {
        TypedSimpleResponse<AutomationCompositionElement> response = new TypedSimpleResponse<>();
        response.setResponse(intermediaryApi.updateAutomationCompositionElementState(null, element.getId(),
            element.getOrderedState(), element.getState(), ParticipantMessageType.AUTOMATION_COMPOSITION_STATE_CHANGE));
        return response;
    }

    /**
     * Get the current simulated participants.
     *
     * @param name the participant, null to get all
     * @param version the participant, null to get all
     * @return the list of participants
     */
    public List<Participant> getParticipants(String name, String version) {
        return intermediaryApi.getParticipants(name, version);
    }

    /**
     * Update a simulated participant.
     *
     * @param participant the participant to update
     * @return TypedSimpleResponse simple response
     */
    public TypedSimpleResponse<Participant> updateParticipant(Participant participant) {
        TypedSimpleResponse<Participant> response = new TypedSimpleResponse<>();
        response.setResponse(
            intermediaryApi.updateParticipantState(participant.getDefinition(), participant.getParticipantState()));
        return response;
    }
}
