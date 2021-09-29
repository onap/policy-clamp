/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
 * ================================================================================
 * Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ClElementStatisticsList;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElementDefinition;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopInfo;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoops;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.Participant;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantDefinition;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantHealthStatus;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantStatistics;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ControlLoopStateChange;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ControlLoopUpdate;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantAckMessage;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantDeregister;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantDeregisterAck;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantMessage;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantRegister;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantRegisterAck;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantStatus;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantStatusReq;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantUpdate;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantUpdateAck;
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ControlLoopElementListener;
import org.onap.policy.clamp.controlloop.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.controlloop.participant.intermediary.parameters.ParticipantParameters;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class is responsible for managing the state of a participant.
 */
@Component
public class ParticipantHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantHandler.class);

    @Getter
    private final ToscaConceptIdentifier participantType;

    @Getter
    private final ToscaConceptIdentifier participantId;

    private final ControlLoopHandler controlLoopHandler;
    private final ParticipantStatistics participantStatistics;
    private final ParticipantMessagePublisher publisher;

    @Setter
    private ParticipantState state = ParticipantState.UNKNOWN;

    @Setter
    private ParticipantHealthStatus healthStatus = ParticipantHealthStatus.UNKNOWN;

    private final List<ControlLoopElementDefinition> clElementDefsOnThisParticipant = new ArrayList<>();

    /**
     * Constructor, set the participant ID and sender.
     *
     * @param parameters the parameters of the participant
     * @param publisher the publisher for sending responses to messages
     */
    public ParticipantHandler(ParticipantParameters parameters, ParticipantMessagePublisher publisher,
            ControlLoopHandler controlLoopHandler) {
        this.participantType = parameters.getIntermediaryParameters().getParticipantType();
        this.participantId = parameters.getIntermediaryParameters().getParticipantId();
        this.publisher = publisher;
        this.controlLoopHandler = controlLoopHandler;
        this.participantStatistics = new ParticipantStatistics();
        this.participantStatistics.setParticipantId(participantId);
        this.participantStatistics.setState(state);
        this.participantStatistics.setHealthStatus(healthStatus);
        this.participantStatistics.setTimeStamp(Instant.now());
    }

    /**
     * Method which handles a participant health check event from clamp.
     *
     * @param participantStatusReqMsg participant participantStatusReq message
     */
    public void handleParticipantStatusReq(final ParticipantStatusReq participantStatusReqMsg) {
        var participantStatus = makeHeartbeat(true);
        publisher.sendParticipantStatus(participantStatus);
    }

    /**
     * Update ControlLoopElement statistics. The control loop elements listening will be
     * notified to retrieve statistics from respective controlloop elements, and controlloopelements
     * data on the handler will be updated.
     *
     * @param controlLoops the control loops
     * @param clElementListener control loop element listener
     */
    private void updateClElementStatistics(ControlLoops controlLoops, ControlLoopElementListener clElementListener) {
        for (ControlLoop controlLoop : controlLoops.getControlLoopList()) {
            for (ControlLoopElement element : controlLoop.getElements().values()) {
                try {
                    clElementListener.handleStatistics(element.getId());
                } catch (PfModelException e) {
                    LOGGER.debug("Getting statistics for Control loop element failed for element ID {}",
                            element.getId(), e);
                }
            }
        }
    }

    /**
     * Handle a control loop update message.
     *
     * @param updateMsg the update message
     */
    public void handleControlLoopUpdate(ControlLoopUpdate updateMsg) {
        controlLoopHandler.handleControlLoopUpdate(updateMsg, clElementDefsOnThisParticipant);
    }

    /**
     * Handle a control loop state change message.
     *
     * @param stateChangeMsg the state change message
     */
    public void handleControlLoopStateChange(ControlLoopStateChange stateChangeMsg) {
        controlLoopHandler.handleControlLoopStateChange(stateChangeMsg);
    }

    private void handleStateChange(ParticipantState newParticipantState, ParticipantUpdateAck response) {
        if (state.equals(newParticipantState)) {
            response.setResult(false);
            response.setMessage("Participant already in state " + newParticipantState);
        } else {
            response.setResult(true);
            response.setMessage("Participant state changed from " + state + " to " + newParticipantState);
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

        var participantUpdateAck = new ParticipantUpdateAck();
        handleStateChange(participantState, participantUpdateAck);
        publisher.sendParticipantUpdateAck(participantUpdateAck);
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
     * Get common properties of a controlloopelement.
     *
     * @param clElementDef the control loop element definition
     * @return the common properties
     */
    public Map<String, ToscaProperty> getClElementDefinitionCommonProperties(ToscaConceptIdentifier clElementDef) {
        Map<String, ToscaProperty> commonPropertiesMap = new HashMap<>();
        clElementDefsOnThisParticipant.stream().forEach(definition -> {
            if (definition.getClElementDefinitionId().equals(clElementDef)) {
                commonPropertiesMap.putAll(definition.getCommonPropertiesMap());
            }
        });
        return commonPropertiesMap;
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
     * Check if a participant message applies to this participant handler.
     *
     * @param participantMsg the message to check
     * @return true if it applies, false otherwise
     */
    public boolean appliesTo(ParticipantAckMessage participantMsg) {
        return participantMsg.appliesTo(participantType, participantId);
    }

    /**
     * Method to send ParticipantRegister message to controlloop runtime.
     */
    public void sendParticipantRegister() {
        var participantRegister = new ParticipantRegister();
        participantRegister.setParticipantId(participantId);
        participantRegister.setParticipantType(participantType);

        publisher.sendParticipantRegister(participantRegister);
    }

    /**
     * Handle a participantRegister Ack message.
     *
     * @param participantRegisterAckMsg the participantRegisterAck message
     */
    public void handleParticipantRegisterAck(ParticipantRegisterAck participantRegisterAckMsg) {
        LOGGER.debug("ParticipantRegisterAck message received as responseTo {}",
                participantRegisterAckMsg.getResponseTo());
        statusToPassive();
        publisher.sendParticipantStatus(makeHeartbeat(false));
    }

    private void statusToPassive() {
        if (ParticipantHealthStatus.UNKNOWN.equals(this.healthStatus)) {
            this.healthStatus = ParticipantHealthStatus.HEALTHY;
        }

        if (ParticipantState.UNKNOWN.equals(this.state) || ParticipantState.TERMINATED.equals(this.state)) {
            this.state = ParticipantState.PASSIVE;
        }

    }

    /**
     * Method to send ParticipantDeregister message to controlloop runtime.
     */
    public void sendParticipantDeregister() {
        var participantDeregister = new ParticipantDeregister();
        participantDeregister.setParticipantId(participantId);
        participantDeregister.setParticipantType(participantType);

        publisher.sendParticipantDeregister(participantDeregister);
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

        if (!participantUpdateMsg.getParticipantDefinitionUpdates().isEmpty()) {
            statusToPassive();
            // This message is to commission the controlloop
            for (ParticipantDefinition participantDefinition : participantUpdateMsg.getParticipantDefinitionUpdates()) {
                if (participantDefinition.getParticipantType().equals(participantType)) {
                    clElementDefsOnThisParticipant.addAll(participantDefinition.getControlLoopElementDefinitionList());
                    break;
                }
            }
        } else {
            // This message is to decommission the controlloop
            clElementDefsOnThisParticipant.clear();
            this.state = ParticipantState.TERMINATED;
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
        participantUpdateAck.setParticipantId(participantId);
        participantUpdateAck.setParticipantType(participantType);
        participantUpdateAck.setState(state);
        publisher.sendParticipantUpdateAck(participantUpdateAck);
    }

    /**
     * Dispatch a heartbeat for this participant.
     */
    public void sendHeartbeat() {
        publisher.sendHeartbeat(makeHeartbeat(false));
    }

    /**
     * Method to send heartbeat to controlloop runtime.
     */
    public ParticipantStatus makeHeartbeat(boolean responseToParticipantStatusReq) {
        if (!responseToParticipantStatusReq) {
            var controlLoops = controlLoopHandler.getControlLoops();
            for (var clElementListener : controlLoopHandler.getListeners()) {
                updateClElementStatistics(controlLoops, clElementListener);
            }
        }
        this.participantStatistics.setState(state);
        this.participantStatistics.setHealthStatus(healthStatus);
        this.participantStatistics.setTimeStamp(Instant.now());

        var heartbeat = new ParticipantStatus();
        heartbeat.setParticipantId(participantId);
        heartbeat.setParticipantStatistics(participantStatistics);
        heartbeat.setParticipantType(participantType);
        heartbeat.setHealthStatus(healthStatus);
        heartbeat.setState(state);
        heartbeat.setControlLoopInfoList(getControlLoopInfoList());

        if (responseToParticipantStatusReq) {
            ParticipantDefinition participantDefinition = new ParticipantDefinition();
            participantDefinition.setParticipantId(participantId);
            participantDefinition.setParticipantType(participantType);
            participantDefinition.setControlLoopElementDefinitionList(clElementDefsOnThisParticipant);
            heartbeat.setParticipantDefinitionUpdates(List.of(participantDefinition));
        }

        return heartbeat;
    }

    private List<ControlLoopInfo> getControlLoopInfoList() {
        List<ControlLoopInfo> controlLoopInfoList = new ArrayList<>();
        for (var entry : controlLoopHandler.getControlLoopMap().entrySet()) {
            var clInfo = new ControlLoopInfo();
            clInfo.setControlLoopId(entry.getKey());
            var clStatitistics = new ControlLoopStatistics();
            clStatitistics.setControlLoopId(entry.getKey());
            var clElementStatisticsList = new ClElementStatisticsList();
            clElementStatisticsList
                    .setClElementStatistics(entry.getValue().getElements().values()
                            .stream()
                            .map(ControlLoopElement::getClElementStatistics)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()));
            clStatitistics.setClElementStatisticsList(clElementStatisticsList);
            clInfo.setControlLoopStatistics(clStatitistics);
            clInfo.setState(entry.getValue().getState());
            controlLoopInfoList.add(clInfo);
        }
        return controlLoopInfoList;
    }
}
