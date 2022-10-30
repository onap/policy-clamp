/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.intermediary.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.acm.participant.intermediary.parameters.ParticipantParameters;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionInfo;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantHealthStatus;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionStateChange;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionUpdate;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantAckMessage;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantDeregister;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantDeregisterAck;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessage;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantRegister;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantRegisterAck;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantStatus;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantStatusReq;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantUpdate;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantUpdateAck;
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

    private final AutomationCompositionHandler automationCompositionHandler;
    private final ParticipantMessagePublisher publisher;

    @Setter
    private ParticipantState state = ParticipantState.UNKNOWN;

    @Setter
    private ParticipantHealthStatus healthStatus = ParticipantHealthStatus.UNKNOWN;

    private final List<AutomationCompositionElementDefinition> acElementDefsOnThisParticipant = new ArrayList<>();

    /**
     * Constructor, set the participant ID and sender.
     *
     * @param parameters the parameters of the participant
     * @param publisher the publisher for sending responses to messages
     */
    public ParticipantHandler(ParticipantParameters parameters, ParticipantMessagePublisher publisher,
        AutomationCompositionHandler automationCompositionHandler) {
        this.participantType = parameters.getIntermediaryParameters().getParticipantType();
        this.participantId = parameters.getIntermediaryParameters().getParticipantId();
        this.publisher = publisher;
        this.automationCompositionHandler = automationCompositionHandler;
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
     * Handle a automation composition update message.
     *
     * @param updateMsg the update message
     */
    public void handleAutomationCompositionUpdate(AutomationCompositionUpdate updateMsg) {
        automationCompositionHandler.handleAutomationCompositionUpdate(updateMsg, acElementDefsOnThisParticipant);
    }

    /**
     * Handle a automation composition state change message.
     *
     * @param stateChangeMsg the state change message
     */
    public void handleAutomationCompositionStateChange(AutomationCompositionStateChange stateChangeMsg) {
        automationCompositionHandler.handleAutomationCompositionStateChange(stateChangeMsg,
            acElementDefsOnThisParticipant);
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
     * Get common properties of a automation composition element.
     *
     * @param acElementDef the automation composition element definition
     * @return the common properties
     */
    public Map<String, ToscaProperty> getAcElementDefinitionCommonProperties(ToscaConceptIdentifier acElementDef) {
        Map<String, ToscaProperty> commonPropertiesMap = new HashMap<>();
        acElementDefsOnThisParticipant.stream().forEach(definition -> {
            if (definition.getAcElementDefinitionId().equals(acElementDef)) {
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
     * Method to send ParticipantRegister message to automation composition runtime.
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
     * Method to send ParticipantDeregister message to automation composition runtime.
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
            // This message is to commission the automation composition
            for (ParticipantDefinition participantDefinition : participantUpdateMsg.getParticipantDefinitionUpdates()) {
                if (participantDefinition.getParticipantType().equals(participantType)) {
                    acElementDefsOnThisParticipant
                        .addAll(participantDefinition.getAutomationCompositionElementDefinitionList());
                    break;
                }
            }
        } else {
            // This message is to decommission the automation composition
            acElementDefsOnThisParticipant.clear();
            this.state = ParticipantState.TERMINATED;
        }
        sendParticipantUpdateAck(participantUpdateMsg.getMessageId());
    }

    /**
     * Method to send ParticipantUpdateAck message to automation composition runtime.
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
     * Method to send heartbeat to automation composition runtime.
     */
    public ParticipantStatus makeHeartbeat(boolean responseToParticipantStatusReq) {
        var heartbeat = new ParticipantStatus();
        heartbeat.setParticipantId(participantId);
        heartbeat.setParticipantType(participantType);
        heartbeat.setHealthStatus(healthStatus);
        heartbeat.setState(state);
        heartbeat.setAutomationCompositionInfoList(getAutomationCompositionInfoList());

        if (responseToParticipantStatusReq) {
            ParticipantDefinition participantDefinition = new ParticipantDefinition();
            participantDefinition.setParticipantId(participantId);
            participantDefinition.setParticipantType(participantType);
            participantDefinition.setAutomationCompositionElementDefinitionList(acElementDefsOnThisParticipant);
            heartbeat.setParticipantDefinitionUpdates(List.of(participantDefinition));
        }

        return heartbeat;
    }

    private List<AutomationCompositionInfo> getAutomationCompositionInfoList() {
        List<AutomationCompositionInfo> automationCompositionInfoList = new ArrayList<>();
        for (var entry : automationCompositionHandler.getAutomationCompositionMap().entrySet()) {
            var acInfo = new AutomationCompositionInfo();
            acInfo.setAutomationCompositionId(entry.getKey());
            acInfo.setState(entry.getValue().getState());
            automationCompositionInfoList.add(acInfo);
        }
        return automationCompositionInfoList;
    }
}
