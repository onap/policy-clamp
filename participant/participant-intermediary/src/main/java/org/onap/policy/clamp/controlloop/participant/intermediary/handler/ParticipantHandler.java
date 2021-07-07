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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElementDefinition;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.Participant;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantHealthStatus;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantStatistics;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantControlLoopStateChange;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantControlLoopUpdate;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantDeregister;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantDeregisterAck;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantHealthCheck;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantMessage;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantRegister;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantRegisterAck;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantResponseDetails;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantResponseStatus;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantStateChange;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantStatus;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantUpdate;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantUpdateAck;
import org.onap.policy.clamp.controlloop.participant.intermediary.comm.MessageSender;
import org.onap.policy.clamp.controlloop.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.controlloop.participant.intermediary.parameters.ParticipantParameters;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class is responsible for managing the state of a participant.
 */
@Getter
@Component
public class ParticipantHandler implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantHandler.class);

    private final ToscaConceptIdentifier participantType;
    private final ToscaConceptIdentifier participantId;
    private final MessageSender sender;
    private final ControlLoopHandler controlLoopHandler;
    private final ParticipantStatistics participantStatistics;

    @Setter
    private ParticipantState state = ParticipantState.UNKNOWN;

    @Setter
    private ParticipantHealthStatus healthStatus = ParticipantHealthStatus.UNKNOWN;

    private final Map<UUID, ControlLoopElementDefinition> clElementDefsOnThisParticipant = new LinkedHashMap<>();

    /**
     * Constructor, set the participant ID and sender.
     *
     * @param parameters the parameters of the participant
     * @param publisher the publisher for sending responses to messages
     */
    public ParticipantHandler(ParticipantParameters parameters, ParticipantMessagePublisher publisher) {
        this.participantType = parameters.getIntermediaryParameters().getParticipantType();
        this.participantId = parameters.getIntermediaryParameters().getParticipantId();
        this.sender =
                new MessageSender(this, publisher,
                        parameters.getIntermediaryParameters().getReportingTimeIntervalMs());
        this.controlLoopHandler = new ControlLoopHandler(parameters.getIntermediaryParameters(), sender);
        this.participantStatistics = new ParticipantStatistics();
    }

    @Override
    public void close() {
        sender.close();
    }

    /**
     * Method which handles a participant state change event from clamp.
     *
     * @param stateChangeMsg participant state change message
     */
    public void handleParticipantStateChange(final ParticipantStateChange stateChangeMsg) {

        if (!stateChangeMsg.appliesTo(participantType, participantId)) {
            return;
        }

        var response = new ParticipantResponseDetails(stateChangeMsg);

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
                response.setResponseMessage(
                        "StateChange message has invalid state for participantId " + stateChangeMsg.getParticipantId());
                break;
        }

        sender.sendResponse(response);
    }

    /**
     * Method which handles a participant health check event from clamp.
     *
     * @param healthCheckMsg participant health check message
     */
    public void handleParticipantHealthCheck(final ParticipantHealthCheck healthCheckMsg) {
        var response = new ParticipantResponseDetails(healthCheckMsg);
        response.setResponseStatus(ParticipantResponseStatus.SUCCESS);
        response.setResponseMessage(healthStatus.toString());

        sender.sendResponse(response);
    }

    /**
     * Handle a control loop update message.
     *
     * @param updateMsg the update message
     */
    public void handleControlLoopUpdate(ParticipantControlLoopUpdate updateMsg) {
        controlLoopHandler.handleControlLoopUpdate(updateMsg);
    }

    /**
     * Handle a control loop state change message.
     *
     * @param stateChangeMsg the state change message
     */
    public void handleControlLoopStateChange(ParticipantControlLoopStateChange stateChangeMsg) {
        controlLoopHandler.handleControlLoopStateChange(stateChangeMsg);
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
     * @return the participant
     */
    public Participant updateParticipantState(ToscaConceptIdentifier definition, ParticipantState participantState) {
        if (!Objects.equals(definition, participantId)) {
            LOGGER.debug("No participant with this ID {}", definition.getName());
            return null;
        }
        var response = new ParticipantResponseDetails();
        handleStateChange(participantState, response);
        sender.sendResponse(response);
        return getParticipant(definition.getName(), definition.getVersion());
    }

    /**
     * Get participants as a {@link Participant} class.
     *
     * @param name the participant name to get
     * @param version the version of the participant to get
     * @return the participant
     */
    public Participant getParticipant(String name, String version) {
        if (participantId.getName().equals(name)) {
            var participant = new Participant();
            participant.setDefinition(participantId);
            participant.setParticipantState(state);
            participant.setHealthStatus(healthStatus);
            return participant;
        }
        return null;
    }

    /**
     * Check if a participant message applies to this participant handler.
     *
     * @param participantMsg the message to check
     * @return true if it applies, false otherwise
     */
    public boolean appliesTo(ParticipantMessage participantMsg) {
        return participantMsg.appliesTo(participantType, participantId);
    }

    /**
     * Method to send ParticipantRegister message to controlloop runtime.
     */
    public void sendParticipantRegister() {
        var participantRegister = new ParticipantRegister();
        participantRegister.setParticipantId(participantId);
        participantRegister.setParticipantType(participantType);

        sender.sendParticipantRegister(participantRegister);
    }

    /**
     * Handle a participantRegister Ack message.
     *
     * @param participantRegisterAckMsg the participantRegisterAck message
     */
    public void handleParticipantRegisterAck(ParticipantRegisterAck participantRegisterAckMsg) {
        LOGGER.debug("ParticipantRegisterAck message received as responseTo {}",
            participantRegisterAckMsg.getResponseTo());
    }

    /**
     * Method to send ParticipantDeregister message to controlloop runtime.
     */
    public void sendParticipantDeregister() {
        var participantDeregister = new ParticipantDeregister();
        participantDeregister.setParticipantId(participantId);
        participantDeregister.setParticipantType(participantType);

        sender.sendParticipantDeregister(participantDeregister);
    }

    /**
     * Handle a participantDeregister Ack message.
     *
     * @param participantDeregisterAckMsg the participantDeregisterAck message
     */
    public void handleParticipantDeregisterAck(ParticipantDeregisterAck participantDeregisterAckMsg) {
        LOGGER.debug("ParticipantDeregisterAck message received as responseTo {}",
            participantDeregisterAckMsg.getResponseTo());
    }

    /**
     * Handle a ParticipantUpdate message.
     *
     * @param participantUpdateMsg the ParticipantUpdate message
     */
    public void handleParticipantUpdate(ParticipantUpdate participantUpdateMsg) {
        LOGGER.debug("ParticipantUpdate message received for participantId {}",
            participantUpdateMsg.getParticipantId());

        if (!participantUpdateMsg.appliesTo(participantType, participantId)) {
            return;
        }

        Map<UUID, ControlLoopElementDefinition> clDefinitionMap =
                participantUpdateMsg.getParticipantDefinitionUpdateMap().get(participantUpdateMsg.getParticipantId());

        for (ControlLoopElementDefinition element : clDefinitionMap.values()) {
            clElementDefsOnThisParticipant.put(element.getId(), element);
        }

        sendParticipantUpdateAck(participantUpdateMsg.getMessageId());
    }

    /**
     * Method to send ParticipantUpdateAck message to controlloop runtime.
     */
    public void sendParticipantUpdateAck(UUID messageId) {
        var participantUpdateAck = new ParticipantUpdateAck();
        participantUpdateAck.setResponseTo(messageId);
        participantUpdateAck.setMessage("Participant Update Ack message");
        participantUpdateAck.setResult(true);

        sender.sendParticipantUpdateAck(participantUpdateAck);
    }

    /**
     * Method to send heartbeat to controlloop runtime.
     */
    public ParticipantStatus makeHeartbeat() {
        ParticipantStatus heartbeat = new ParticipantStatus();
        heartbeat.setParticipantId(participantId);
        heartbeat.setParticipantStatistics(participantStatistics);
        heartbeat.setParticipantType(participantType);
        heartbeat.setHealthStatus(healthStatus);
        heartbeat.setMessage("Participant heartbeat message sent from -> " + participantId.getName());
        return heartbeat;
    }
}
