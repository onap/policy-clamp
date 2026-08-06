/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.participant.intermediary.comm;

import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.policy.clamp.acm.participant.intermediary.handler.ParticipantHandler;
import org.onap.policy.clamp.common.acm.utils.NetLoggerUtil;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantKafkaMessage;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantRegisterAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatusReq;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantSync;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Spring Kafka-based message listener for participant sync messages.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@KafkaListener(
        topics = "${participant.intermediaryParameters.topics.syncTopic}",
        containerFactory = "acmListenerContainerFactory",
        groupId = "${random.uuid}"
)
public class ParticipantSyncMessageListener {

    private final ParticipantHandler participantHandler;

    @Value("${participant.intermediaryParameters.topics.syncTopic}")
    private String syncTopic;

    /**
     * Handle ParticipantSync messages.
     * @param participantSync the message
     */
    @KafkaHandler
    public void onTopicEvent(final ParticipantSync participantSync) {
        processIfApplicable(participantSync, participantHandler::handleParticipantSync);
    }

    /**
     * Handle ParticipantRegisterAck messages.
     * @param participantRegisterAck the message
     */
    @KafkaHandler
    public void onTopicEvent(final ParticipantRegisterAck participantRegisterAck) {
        processIfApplicable(participantRegisterAck, participantHandler::handleParticipantRegisterAck);
    }

    /**
     * Handle ParticipantStatusReq messages.
     * @param participantStatusReq the message
     */
    @KafkaHandler
    public void onTopicEvent(final ParticipantStatusReq participantStatusReq) {
        processIfApplicable(participantStatusReq, participantHandler::handleParticipantStatusReq);
    }

    /**
     * Log any unhandled events.
     * @param participantKafkaMessage the unhandled message
     */
    @KafkaHandler(isDefault = true)
    public void onUnhandledEvent(final ParticipantKafkaMessage participantKafkaMessage) {
        log.info("discarding event of type {}", participantKafkaMessage.getMessageType());
    }

    private <T> void processIfApplicable(final T message, final Consumer<T> handler) {
        if (participantHandler.appliesTo((ParticipantKafkaMessage) message)) {
            NetLoggerUtil.log(NetLoggerUtil.EventType.IN, "KAFKA", syncTopic, message.toString());
            handler.accept(message);
        }
    }
}
