/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.runtime.supervision.comm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionAcHandler;
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionHandler;
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionParticipantHandler;
import org.onap.policy.clamp.common.acm.utils.NetLoggerUtil;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeployAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantDeregister;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessage;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrimeAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantRegister;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantReqSync;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listener for ParticipantStatus messages sent by participants.
 */
@Component
@KafkaListener(topics = "${runtime.topics.operationTopic}")
@RequiredArgsConstructor
@Slf4j
public class ParticipantMessageListener {

    private static final String KAFKA = "KAFKA";

    @Value("${runtime.topics.operationTopic}")
    private String topic;

    private final SupervisionAcHandler supervisionAcHandler;
    private final SupervisionParticipantHandler supervisionParticipantHandler;
    private final SupervisionHandler supervisionHandler;

    /**
     * Handle ParticipantRegister messages.
     * @param participantRegisterMessage the message
     */
    @KafkaHandler
    public void onTopicEvent(final ParticipantRegister participantRegisterMessage) {
        NetLoggerUtil.log(NetLoggerUtil.EventType.IN, KAFKA, topic, participantRegisterMessage.toString());
        supervisionParticipantHandler.handleParticipantMessage(participantRegisterMessage);
    }

    /**
     * Handle ParticipantDeregister messages.
     * @param participantDeregisterMessage the message
     */
    @KafkaHandler
    public void onTopicEvent(final ParticipantDeregister participantDeregisterMessage) {
        NetLoggerUtil.log(NetLoggerUtil.EventType.IN, KAFKA, topic, participantDeregisterMessage.toString());
        supervisionParticipantHandler.handleParticipantMessage(participantDeregisterMessage);
    }

    /**
     * Handle ParticipantReqSync messages.
     * @param participantReqSync the message
     */
    @KafkaHandler
    public void onTopicEvent(final ParticipantReqSync participantReqSync) {
        NetLoggerUtil.log(NetLoggerUtil.EventType.IN, KAFKA, topic, participantReqSync.toString());
        supervisionParticipantHandler.handleParticipantReqSync(participantReqSync);
    }

    /**
     * Handle ParticipantStatus messages.
     * @param participantStatusMessage the message
     */
    @KafkaHandler
    public void onTopicEvent(final ParticipantStatus participantStatusMessage) {
        NetLoggerUtil.log(NetLoggerUtil.EventType.IN, KAFKA, topic, participantStatusMessage.toString());
        supervisionParticipantHandler.handleParticipantMessage(participantStatusMessage);
    }

    /**
     * Handle ParticipantPrimeAck messages.
     * @param participantPrimeAckMessage the message
     */
    @KafkaHandler
    public void onTopicEvent(final ParticipantPrimeAck participantPrimeAckMessage) {
        NetLoggerUtil.log(NetLoggerUtil.EventType.IN, KAFKA, topic, participantPrimeAckMessage.toString());
        supervisionHandler.handleParticipantMessage(participantPrimeAckMessage);
    }

    /**
     * Handle AutomationCompositionDeployAck messages.
     * @param automationCompositionDeployAck the message
     */
    @KafkaHandler
    public void onTopicEvent(final AutomationCompositionDeployAck automationCompositionDeployAck) {
        NetLoggerUtil.log(NetLoggerUtil.EventType.IN, KAFKA, topic, automationCompositionDeployAck.toString());
        switch (automationCompositionDeployAck.getMessageType()) {
            case AUTOMATION_COMPOSITION_DEPLOY_ACK:
                supervisionAcHandler.handleAutomationCompositionUpdateAckMessage(automationCompositionDeployAck);
                break;
            case AUTOMATION_COMPOSITION_STATECHANGE_ACK:
                supervisionAcHandler.handleAutomationCompositionStateChangeAckMessage(automationCompositionDeployAck);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Handle any unhandled events.
     * @param object the unhandled event
     */
    @KafkaHandler(isDefault = true)
    public void onUnhandledEvent(final Object object) {
        if (object instanceof ParticipantMessage participantMessage) {
            log.info("discarding event of type {}", participantMessage.getMessageType());
        } else {
            log.warn("received unknown message type: {}", object.getClass().getName());
        }
    }

}
