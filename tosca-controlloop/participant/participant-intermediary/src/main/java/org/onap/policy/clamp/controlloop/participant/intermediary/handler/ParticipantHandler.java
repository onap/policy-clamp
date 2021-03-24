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

package org.onap.policy.clamp.controlloop.participant.intermediary.handler;

import java.io.Closeable;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.Participant;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantHealthStatus;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantState;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantResponseDetails;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantResponseStatus;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantStateChange;
import org.onap.policy.clamp.controlloop.participant.intermediary.comm.MessageSender;
import org.onap.policy.clamp.controlloop.participant.intermediary.comm.ParticipantStatusPublisher;
import org.onap.policy.clamp.controlloop.participant.intermediary.parameters.ParticipantIntermediaryParameters;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for managing the state of a participant.
 */
@Getter
public class ParticipantHandler implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantHandler.class);

    private final ToscaConceptIdentifier participantId;
    private final MessageSender sender;
    private final ControlLoopHandler controlLoopHandler;

    @Setter
    private ParticipantState state = ParticipantState.UNKNOWN;

    @Setter
    private ParticipantHealthStatus healthStatus = ParticipantHealthStatus.UNKNOWN;

    /**
     * Constructor, set the participant ID and sender.
     *
     * @param parameters the parameters of the participant
     * @param publisher the publisher for sending responses to messages
     */
    public ParticipantHandler(ParticipantIntermediaryParameters parameters, ParticipantStatusPublisher publisher) {
        this.participantId = parameters.getParticipantId();
        this.sender = new MessageSender(this, publisher, parameters.getReportingTimeInterval());
        this.controlLoopHandler = new ControlLoopHandler(parameters, sender);
    }

    @Override
    public void close() {
        sender.close();
        controlLoopHandler.close();
    }

    /**
     * Method which handles a participant state change event from clamp.
     *
     * @param stateChangeMsg participant state change message
     */
    public void handleParticipantStateChange(final ParticipantStateChange stateChangeMsg) {

        if (!stateChangeMsg.appliesTo(participantId)) {
            return;
        }

        ParticipantResponseDetails response = new ParticipantResponseDetails(stateChangeMsg);

        switch (stateChangeMsg.getState()) {
            case PASSIVE:
                handlePassiveState(response);
                break;
            case ACTIVE:
                handleActiveState(response);
                break;
            case SAFE:
                handleSafeState(response);
                break;
            case TEST:
                handleTestState(response);
                break;
            case TERMINATED:
                handleTerminatedState(response);
                break;
            default:
                LOGGER.debug("StateChange message has no state, state is null {}", stateChangeMsg.getParticipantId());
                response.setResponseStatus(ParticipantResponseStatus.FAIL);
                response.setResponseMessage("StateChange message has invalid state for participantId "
                    + stateChangeMsg.getParticipantId());
                break;
        }

        sender.sendResponse(response);
    }

    /**
     * Method to handle when the new state from participant is active.
     *
     * @param response participant response
     */
    private void handleActiveState(final ParticipantResponseDetails response) {
        handleStateChange(ParticipantState.ACTIVE, response);
    }

    /**
     * Method to handle when the new state from participant is passive.
     *
     * @param response participant response
     */
    private void handlePassiveState(final ParticipantResponseDetails response) {
        handleStateChange(ParticipantState.PASSIVE, response);
    }

    /**
     * Method to handle when the new state from participant is safe.
     *
     * @param response participant response
     */
    private void handleSafeState(final ParticipantResponseDetails response) {
        handleStateChange(ParticipantState.SAFE, response);
    }

    /**
     * Method to handle when the new state from participant is TEST.
     *
     * @param response participant response
     */
    private void handleTestState(final ParticipantResponseDetails response) {
        handleStateChange(ParticipantState.TEST, response);
    }

    /**
     * Method to handle when the new state from participant is Terminated.
     *
     * @param response participant response
     */
    private void handleTerminatedState(final ParticipantResponseDetails response) {
        handleStateChange(ParticipantState.TERMINATED, response);
    }

    private void handleStateChange(ParticipantState newParticipantState, ParticipantResponseDetails response) {
        if (state.equals(newParticipantState)) {
            response.setResponseStatus(ParticipantResponseStatus.SUCCESS);
            response.setResponseMessage("Participant already in state " + newParticipantState);
        } else {
            response.setResponseStatus(ParticipantResponseStatus.SUCCESS);
            response.setResponseMessage("Participant state changed from " + state + " to " + newParticipantState);
            state = newParticipantState;
        }
    }

    /**
     * Method to update participant state.
     *
     * @param definition participant definition
     * @param participantState participant state
     */
    public Participant updateParticipantState(ToscaConceptIdentifier definition,
            ParticipantState participantState) {
        if (!Objects.equals(definition, participantId)) {
            LOGGER.debug("No participant with this ID {}", definition.getName());
            return null;
        }
        ParticipantResponseDetails response = new ParticipantResponseDetails();
        handleStateChange(participantState, response);
        sender.sendResponse(response);
        return getParticipant(definition.getName(), definition.getVersion());
    }

    /**
     * Get participants as a {@link Participant} class.
     *
     * @return the participant
     */
    public Participant getParticipant(String name, String version) {
        if (participantId.getName().equals(name)) {
            Participant participant = new Participant();
            participant.setDefinition(participantId);
            participant.setParticipantState(state);
            participant.setHealthStatus(healthStatus);
            return participant;
        }
        return null;
    }
}
