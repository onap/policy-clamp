/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation.
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

import io.micrometer.core.annotation.Timed;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.acm.participant.intermediary.parameters.ParticipantParameters;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionInfo;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantSupportedElementType;
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
    private final UUID participantId;

    private final AutomationCompositionHandler automationCompositionHandler;
    private final ParticipantMessagePublisher publisher;

    private final Map<UUID, List<AutomationCompositionElementDefinition>> acElementDefsMap = new HashMap<>();

    private final List<ParticipantSupportedElementType> supportedAcElementTypes;

    /**
     * Constructor, set the participant ID and sender.
     *
     * @param parameters the parameters of the participant
     * @param publisher the publisher for sending responses to messages
     */
    public ParticipantHandler(ParticipantParameters parameters, ParticipantMessagePublisher publisher,
            AutomationCompositionHandler automationCompositionHandler) {
        this.participantId = parameters.getIntermediaryParameters().getParticipantId();
        this.publisher = publisher;
        this.automationCompositionHandler = automationCompositionHandler;
        this.supportedAcElementTypes = parameters.getIntermediaryParameters().getParticipantSupportedElementTypes();
    }

    /**
     * Method which handles a participant health check event from clamp.
     *
     * @param participantStatusReqMsg participant participantStatusReq message
     */
    @Timed(value = "listener.participant_status_req", description = "PARTICIPANT_STATUS_REQ messages received")
    public void handleParticipantStatusReq(final ParticipantStatusReq participantStatusReqMsg) {
        var participantStatus = makeHeartbeat(true);
        participantStatus.setParticipantSupportedElementType(this.supportedAcElementTypes);
        publisher.sendParticipantStatus(participantStatus);
    }

    /**
     * Handle a automation composition update message.
     *
     * @param updateMsg the update message
     */
    @Timed(
            value = "listener.automation_composition_update",
            description = "AUTOMATION_COMPOSITION_UPDATE messages received")
    public void handleAutomationCompositionUpdate(AutomationCompositionUpdate updateMsg) {
        automationCompositionHandler.handleAutomationCompositionUpdate(updateMsg,
                acElementDefsMap.get(updateMsg.getCompositionId()));
    }

    /**
     * Handle a automation composition state change message.
     *
     * @param stateChangeMsg the state change message
     */
    @Timed(
            value = "listener.automation_composition_state_change",
            description = "AUTOMATION_COMPOSITION_STATE_CHANGE messages received")
    public void handleAutomationCompositionStateChange(AutomationCompositionStateChange stateChangeMsg) {
        automationCompositionHandler.handleAutomationCompositionStateChange(stateChangeMsg,
                acElementDefsMap.get(stateChangeMsg.getCompositionId()));
    }

    /**
     * Check if a participant message applies to this participant handler.
     *
     * @param participantMsg the message to check
     * @return true if it applies, false otherwise
     */
    public boolean appliesTo(ParticipantMessage participantMsg) {
        return participantMsg.appliesTo(participantId);
    }

    /**
     * Check if a participant message applies to this participant handler.
     *
     * @param participantMsg the message to check
     * @return true if it applies, false otherwise
     */
    public boolean appliesTo(ParticipantAckMessage participantMsg) {
        return participantMsg.appliesTo(participantId);
    }

    /**
     * Method to send ParticipantRegister message to automation composition runtime.
     */
    public void sendParticipantRegister() {
        var participantRegister = new ParticipantRegister();
        participantRegister.setParticipantId(participantId);
        participantRegister.setParticipantSupportedElementType(supportedAcElementTypes);

        publisher.sendParticipantRegister(participantRegister);
    }

    /**
     * Handle a participantRegister Ack message.
     *
     * @param participantRegisterAckMsg the participantRegisterAck message
     */
    @Timed(value = "listener.participant_register_ack", description = "PARTICIPANT_REGISTER_ACK messages received")
    public void handleParticipantRegisterAck(ParticipantRegisterAck participantRegisterAckMsg) {
        LOGGER.debug("ParticipantRegisterAck message received as responseTo {}",
                participantRegisterAckMsg.getResponseTo());
        publisher.sendParticipantStatus(makeHeartbeat(false));
    }

    /**
     * Method to send ParticipantDeregister message to automation composition runtime.
     */
    public void sendParticipantDeregister() {
        var participantDeregister = new ParticipantDeregister();
        participantDeregister.setParticipantId(participantId);

        publisher.sendParticipantDeregister(participantDeregister);
    }

    /**
     * Handle a participantDeregister Ack message.
     *
     * @param participantDeregisterAckMsg the participantDeregisterAck message
     */
    @Timed(value = "listener.participant_deregister_ack", description = "PARTICIPANT_DEREGISTER_ACK messages received")
    public void handleParticipantDeregisterAck(ParticipantDeregisterAck participantDeregisterAckMsg) {
        LOGGER.debug("ParticipantDeregisterAck message received as responseTo {}",
                participantDeregisterAckMsg.getResponseTo());
    }

    /**
     * Handle a ParticipantUpdate message.
     *
     * @param participantUpdateMsg the ParticipantUpdate message
     */
    @Timed(value = "listener.participant_update", description = "PARTICIPANT_UPDATE messages received")
    public void handleParticipantUpdate(ParticipantUpdate participantUpdateMsg) {
        LOGGER.debug("ParticipantUpdate message received for participantId {}",
                participantUpdateMsg.getParticipantId());

        acElementDefsMap.putIfAbsent(participantUpdateMsg.getCompositionId(), new ArrayList<>());
        if (!participantUpdateMsg.getParticipantDefinitionUpdates().isEmpty()) {
            // This message is to commission the automation composition
            for (var participantDefinition : participantUpdateMsg.getParticipantDefinitionUpdates()) {
                if (participantDefinition.getParticipantId().equals(participantId)) {
                    acElementDefsMap.get(participantUpdateMsg.getCompositionId())
                            .addAll(participantDefinition.getAutomationCompositionElementDefinitionList());
                    break;
                }
            }
        } else {
            // This message is to decommission the automation composition
            acElementDefsMap.get(participantUpdateMsg.getCompositionId()).clear();
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
        participantUpdateAck.setState(ParticipantState.ON_LINE);
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
        heartbeat.setState(ParticipantState.ON_LINE);
        heartbeat.setAutomationCompositionInfoList(getAutomationCompositionInfoList());

        if (responseToParticipantStatusReq) {
            List<ParticipantDefinition> participantDefinitionList = new ArrayList<>(acElementDefsMap.size());
            for (var acElementDefsOnThisParticipant : acElementDefsMap.values()) {
                var participantDefinition = new ParticipantDefinition();
                participantDefinition.setParticipantId(participantId);
                participantDefinition.setAutomationCompositionElementDefinitionList(acElementDefsOnThisParticipant);
                participantDefinitionList.add(participantDefinition);
            }
            heartbeat.setParticipantDefinitionUpdates(participantDefinitionList);
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
