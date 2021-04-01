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

package org.onap.policy.clamp.controlloop.participant.intermediary.api;

import java.util.List;
import java.util.UUID;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ClElementStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoops;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.Participant;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantStatistics;
import org.onap.policy.clamp.controlloop.participant.intermediary.handler.ParticipantHandler;
import org.onap.policy.clamp.controlloop.participant.intermediary.parameters.ParticipantIntermediaryParameters;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * This interface is used by participant implementations to use the participant intermediary.
 */
public interface ParticipantIntermediaryApi {
    /**
     * Initialise the participant intermediary.
     *
     * @param parameters the parameters for the intermediary
     */
    void init(ParticipantIntermediaryParameters parameters);

    /**
     * Close the intermediary.
     */
    void close();

    /**
     * Register a listener for control loop elements that are mediated by the intermediary.
     *
     * @param controlLoopElementListener The control loop element listener to register
     */
    void registerControlLoopElementListener(ControlLoopElementListener controlLoopElementListener);

    /**
     * Get participants loops from the intermediary API.
     *
     * @param name the participant name, null for all
     * @param version the participant version, null for all
     * @return the participants
     */
    List<Participant> getParticipants(String name, String version);

    /**
     * Update the state of a participant.
     *
     * @param definition the definition of the participant to update the state on
     * @param state the state of the participant
     * @return the participant
     */
    Participant updateParticipantState(ToscaConceptIdentifier definition, ParticipantState state);

    /**
     * Update the statistics of a participant.
     *
     * @param participantStatistics the statistics of the participant
     */
    void updateParticipantStatistics(ParticipantStatistics participantStatistics);

    /**
     * Get control loops from the intermediary API.
     *
     * @param name the control loop element name, null for all
     * @param version the control loop element version, null for all
     * @return the control loop elements
     */
    ControlLoops getControlLoops(String name, String version);

    /**
     * Get control loop elements from the intermediary API.
     *
     * @param name the control loop element name, null for all
     * @param version the control loop element version, null for all
     * @return the control loop elements
     */
    List<ControlLoopElement> getControlLoopElements(String name, String version);

    /**
     * Update the state of a control loop.
     *
     * @param definition the ID of the control loop to update the state on
     * @param state the state of the control loop
     * @return ControlLoop updated control loop
     */
    ControlLoop updateControlLoopState(ToscaConceptIdentifier definition, ControlLoopOrderedState state);

    /**
     * Update the state of a control loop element.
     *
     * @param id the ID of the control loop element to update the state on
     * @param state the state of the control loop element
     * @return ControlLoopElement updated control loop element
     */
    ControlLoopElement updateControlLoopElementState(UUID id, ControlLoopOrderedState state);

    /**
     * Update the control loop element statistics.
     *
     * @param elementStatistics the updated statistics
     */
    void updateControlLoopElementStatistics(ClElementStatistics elementStatistics);

    /**
     * Returns participantHandler, This will not be used in real world, but for junits,
     * if participantHandler is not returned, there is no way to test state change messages
     * without dmaap simulator.
     *
     * @return ParticipantHandler returns a participantHandler
     */
    ParticipantHandler getParticipantHandler();
}
