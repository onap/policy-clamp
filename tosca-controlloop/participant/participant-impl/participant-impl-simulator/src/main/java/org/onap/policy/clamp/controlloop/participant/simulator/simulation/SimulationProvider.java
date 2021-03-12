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

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import lombok.Getter;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopException;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopRuntimeException;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoops;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.Participant;
import org.onap.policy.clamp.controlloop.models.messages.rest.TypedSimpleResponse;
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ParticipantIntermediaryFactory;
import org.onap.policy.clamp.controlloop.participant.intermediary.parameters.ParticipantIntermediaryParameters;

/**
 * This provider class simulation of participants and control loop elements.
 */
public class SimulationProvider implements Closeable {
    @Getter
    private final ParticipantIntermediaryApi intermediaryApi;

    /**
     * Create a participant simulation provider.
     *
     * @throws ControlLoopRuntimeException on errors creating the provider
     */
    public SimulationProvider(ParticipantIntermediaryParameters participantParameters)
                     throws ControlLoopRuntimeException {
        intermediaryApi = new ParticipantIntermediaryFactory().createApiImplementation();
        intermediaryApi.init(participantParameters);
    }

    @Override
    public void close() throws IOException {
        intermediaryApi.close();
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
     * Update the given control loop in the simulator.
     *
     * @param controlLoop the control loop to update
     * @return response simple response returned
     * @throws ControlLoopException on errors updating the control loop
     */
    public TypedSimpleResponse<ControlLoop> updateControlLoop(ControlLoop controlLoop)
            throws ControlLoopException {
        TypedSimpleResponse<ControlLoop> response = new TypedSimpleResponse<>();
        ControlLoop updatedControlLoop = intermediaryApi.updateControlLoopState(
                controlLoop.getDefinition(), controlLoop.getOrderedState());
        response.setResponse(updatedControlLoop);
        return response;
    }

    /**
     * Get the simulated control loop elements.
     *
     * @param name the controlLoopElement, null to get all
     * @param version the controlLoopElement, null to get all
     * @return the control loop elements
     * @throws ControlLoopException on errors getting the control loop elements
     */
    public List<ControlLoopElement> getControlLoopElements(String name, String version) throws ControlLoopException {
        return intermediaryApi.getControlLoopElements(name, version);
    }

    /**
     * Update the given control loop element in the simulator.
     *
     * @param element the control loop element to update
     * @return response simple response returned
     * @throws ControlLoopException on errors updating the control loop element
     */
    public TypedSimpleResponse<ControlLoopElement> updateControlLoopElement(ControlLoopElement element)
            throws ControlLoopException {
        TypedSimpleResponse<ControlLoopElement> response = new TypedSimpleResponse<>();
        response.setResponse(intermediaryApi.updateControlLoopElementState(
                element.getId(), element.getOrderedState()));
        return response;
    }

    /**
     * Get the current simulated participants.
     *
     * @param name the participant, null to get all
     * @param version the participant, null to get all
     * @return the list of participants
     * @throws ControlLoopException on errors getting the participants
     */
    public List<Participant> getParticipants(String name, String version) throws ControlLoopException {
        return intermediaryApi.getParticipants(name, version);
    }

    /**
     * Update a simulated participant.
     *
     * @param participant the participant to update
     * @return TypedSimpleResponse simple response
     * @throws ControlLoopException on errors updating the participant
     */

    public TypedSimpleResponse<Participant> updateParticipant(Participant participant) throws ControlLoopException {
        TypedSimpleResponse<Participant> response = new TypedSimpleResponse<>();
        response.setResponse(intermediaryApi.updateParticipantState(
                participant.getDefinition(), participant.getParticipantState()));
        return response;
    }
}
