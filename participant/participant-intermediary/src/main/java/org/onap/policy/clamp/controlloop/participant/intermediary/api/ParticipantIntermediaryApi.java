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
import java.util.Map;
import java.util.UUID;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ClElementStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoops;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.Participant;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantStatistics;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

/**
 * This interface is used by participant implementations to use the participant intermediary.
 */
public interface ParticipantIntermediaryApi {

    /**
     * Register a listener for control loop elements that are mediated by the intermediary.
     *
     * @param controlLoopElementListener The control loop element listener to register
     */
    void registerControlLoopElementListener(ControlLoopElementListener controlLoopElementListener);

    /**
     * Send participant register message to controlloop runtime.
     */
    void sendParticipantRegister();

    /**
     * Send participant deregister message to controlloop runtime.
     */
    void sendParticipantDeregister();

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
    Map<UUID, ControlLoopElement> getControlLoopElements(String name, String version);

    /**
     * Get ToscaServiceTemplate from the intermediary API.
     *
     * @return the control loop element
     */
    ToscaServiceTemplate getToscaServiceTemplate();

    /**
     * Get control loop element from the intermediary API.
     *
     * @param id control loop element ID
     * @return the control loop element
     */
    ControlLoopElement getControlLoopElement(UUID id);

    /**
     * Update the state of a control loop element.
     *
     * @param id the ID of the control loop element to update the state on
     * @param currentState the state of the control loop element
     * @param newState the state of the control loop element
     * @return ControlLoopElement updated control loop element
     */
    ControlLoopElement updateControlLoopElementState(UUID id, ControlLoopOrderedState currentState,
            ControlLoopState newState);

    /**
     * Update the control loop element statistics.
     *
     * @param id the ID of the control loop element to update the state on
     * @param elementStatistics the updated statistics
     */
    void updateControlLoopElementStatistics(UUID id, ClElementStatistics elementStatistics);
}
