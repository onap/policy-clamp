/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation.
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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeploy;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionMigration;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionStateChange;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantAckMessage;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantDeregister;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantDeregisterAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessage;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrime;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantRegister;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantRegisterAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantRestart;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatus;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatusReq;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.PropertiesUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class is responsible for managing the state of a participant.
 */
@Component
@RequiredArgsConstructor
public class ParticipantHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantHandler.class);

    private final AutomationCompositionHandler automationCompositionHandler;
    private final ParticipantMessagePublisher publisher;
    private final CacheProvider cacheProvider;

    /**
     * Method which handles a participant health check event from clamp.
     *
     * @param participantStatusReqMsg participant participantStatusReq message
     */
    @Timed(value = "listener.participant_status_req", description = "PARTICIPANT_STATUS_REQ messages received")
    public void handleParticipantStatusReq(final ParticipantStatusReq participantStatusReqMsg) {
        publisher.sendParticipantStatus(makeHeartbeat());
    }

    /**
     * Handle a automation composition update message.
     *
     * @param updateMsg the update message
     */
    @Timed(
            value = "listener.automation_composition_update",
            description = "AUTOMATION_COMPOSITION_UPDATE messages received")
    public void handleAutomationCompositionDeploy(AutomationCompositionDeploy updateMsg) {
        automationCompositionHandler.handleAutomationCompositionDeploy(updateMsg);
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
        automationCompositionHandler.handleAutomationCompositionStateChange(stateChangeMsg);
    }

    /**
     * Handle a automation composition migration message.
     *
     * @param migrationMsg the migration message
     */
    @Timed(
            value = "listener.automation_composition_migration",
            description = "AUTOMATION_COMPOSITION_MIGRATION messages received")
    public void handleAutomationCompositionMigration(AutomationCompositionMigration migrationMsg) {
        automationCompositionHandler.handleAutomationCompositionMigration(migrationMsg);
    }

    /**
     * Handle a automation composition property update message.
     *
     * @param propertyUpdateMsg the property update message
     */
    @Timed(value = "listener.properties_update", description = "PROPERTIES_UPDATE message received")
    public void handleAcPropertyUpdate(PropertiesUpdate propertyUpdateMsg) {
        automationCompositionHandler.handleAcPropertyUpdate(propertyUpdateMsg);
    }

    /**
     * Check if a participant message applies to this participant handler.
     *
     * @param participantMsg the message to check
     * @return true if it applies, false otherwise
     */
    public boolean appliesTo(ParticipantMessage participantMsg) {
        return participantMsg.appliesTo(cacheProvider.getParticipantId());
    }

    /**
     * Check if a participant message applies to this participant handler.
     *
     * @param participantMsg the message to check
     * @return true if it applies, false otherwise
     */
    public boolean appliesTo(ParticipantAckMessage participantMsg) {
        return participantMsg.appliesTo(cacheProvider.getParticipantId());
    }

    /**
     * Method to send ParticipantRegister message to automation composition runtime.
     */
    public void sendParticipantRegister() {
        var participantRegister = new ParticipantRegister();
        participantRegister.setParticipantId(cacheProvider.getParticipantId());
        participantRegister.setParticipantSupportedElementType(cacheProvider.getSupportedAcElementTypes());

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
        publisher.sendParticipantStatus(makeHeartbeat());
    }

    /**
     * Method to send ParticipantDeregister message to automation composition runtime.
     */
    public void sendParticipantDeregister() {
        var participantDeregister = new ParticipantDeregister();
        participantDeregister.setParticipantId(cacheProvider.getParticipantId());
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
     * Handle a ParticipantPrime message.
     *
     * @param participantPrimeMsg the ParticipantPrime message
     */
    @Timed(value = "listener.participant_prime", description = "PARTICIPANT_PRIME messages received")
    public void handleParticipantPrime(ParticipantPrime participantPrimeMsg) {
        LOGGER.debug("ParticipantPrime message received for participantId {}", participantPrimeMsg.getParticipantId());

        if (!participantPrimeMsg.getParticipantDefinitionUpdates().isEmpty()) {
            // prime
            List<AutomationCompositionElementDefinition> list = new ArrayList<>();
            for (var participantDefinition : participantPrimeMsg.getParticipantDefinitionUpdates()) {
                if (participantDefinition.getParticipantId().equals(cacheProvider.getParticipantId())) {
                    list.addAll(participantDefinition.getAutomationCompositionElementDefinitionList());
                }
            }
            if (!list.isEmpty()) {
                cacheProvider.addElementDefinition(participantPrimeMsg.getCompositionId(), list);
                automationCompositionHandler.prime(participantPrimeMsg.getMessageId(),
                    participantPrimeMsg.getCompositionId(), list);
            }
        } else {
            // deprime
            automationCompositionHandler.deprime(participantPrimeMsg.getMessageId(),
                    participantPrimeMsg.getCompositionId());
        }
    }

    /**
     * Handle a ParticipantRestart message.
     *
     * @param participantRestartMsg the participantRestart message
     */
    @Timed(value = "listener.participant_restart", description = "PARTICIPANT_RESTART messages received")
    public void handleParticipantRestart(ParticipantRestart participantRestartMsg) {
        LOGGER.debug("ParticipantRestart message received for participantId {}",
                participantRestartMsg.getParticipantId());
        List<AutomationCompositionElementDefinition> list = new ArrayList<>();
        for (var participantDefinition : participantRestartMsg.getParticipantDefinitionUpdates()) {
            list.addAll(participantDefinition.getAutomationCompositionElementDefinitionList());
        }
        if (!AcTypeState.COMMISSIONED.equals(participantRestartMsg.getState())) {
            cacheProvider.addElementDefinition(participantRestartMsg.getCompositionId(), list);
        }
        automationCompositionHandler.restarted(participantRestartMsg.getMessageId(),
                participantRestartMsg.getCompositionId(), list, participantRestartMsg.getState(),
                participantRestartMsg.getAutomationcompositionList());
    }

    /**
     * Dispatch a heartbeat for this participant.
     */
    public void sendHeartbeat() {
        if (publisher.isActive()) {
            publisher.sendHeartbeat(makeHeartbeat());
        }
    }

    /**
     * Method to send heartbeat to automation composition runtime.
     */
    private ParticipantStatus makeHeartbeat() {
        var heartbeat = new ParticipantStatus();
        heartbeat.setParticipantId(cacheProvider.getParticipantId());
        heartbeat.setState(ParticipantState.ON_LINE);
        heartbeat.setParticipantSupportedElementType(cacheProvider.getSupportedAcElementTypes());

        return heartbeat;
    }
}
