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

package org.onap.policy.clamp.controlloop.participant.simulator.simulation;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopException;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoops;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.Participant;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.clamp.controlloop.models.messages.rest.TypedSimpleResponse;
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ParticipantIntermediaryApi;
import org.springframework.stereotype.Service;

/**
 * This provider class simulation of participants and control loop elements.
 */
@Service
public class SimulationProvider {

    private final ParticipantIntermediaryApi intermediaryApi;

    /**
     * Create a participant simulation provider.
     * @param intermediaryApi the intermediary to use for talking to the CLAMP runtime
     */
    public SimulationProvider(ParticipantIntermediaryApi intermediaryApi) {
        this.intermediaryApi = intermediaryApi;
    }

    /**
     * Get the control loops.
     *
     * @param name the controlLoop, null to get all
     * @param version the controlLoop, null to get all
     * @return the control loops
     * @throws ControlLoopException on errors getting the control loops
     */
    public ControlLoops getControlLoops(String name, String version) throws ControlLoopException {
        return intermediaryApi.getControlLoops(name, version);
    }

    /**
     * Get the simulated control loop elements.
     *
     * @param name the controlLoopElement, null to get all
     * @param version the controlLoopElement, null to get all
     * @return the control loop elements
     */
    public Map<UUID, ControlLoopElement> getControlLoopElements(String name, String version) {
        return intermediaryApi.getControlLoopElements(name, version);
    }

    /**
     * Update the given control loop element in the simulator.
     *
     * @param element the control loop element to update
     * @return response simple response returned
     */
    public TypedSimpleResponse<ControlLoopElement> updateControlLoopElement(ControlLoopElement element) {
        TypedSimpleResponse<ControlLoopElement> response = new TypedSimpleResponse<>();
        response.setResponse(intermediaryApi.updateControlLoopElementState(null, element.getId(),
            element.getOrderedState(), element.getState(), ParticipantMessageType.CONTROL_LOOP_STATE_CHANGE));
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
