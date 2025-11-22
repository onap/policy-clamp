/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021,2025 OpenInfra Foundation Europe. All rights reserved.
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
import org.onap.policy.clamp.common.acm.utils.NetLoggerUtil;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantAckMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
// TODO make abstract class using generics for ParticipantMessage and ParticipantAckMessage types
public class ParticipantAckPublisher {

    @Value("${runtime.topics.operationTopic}")
    private String operationTopic;

    @Value("${runtime.topics.syncTopic}")
    private String syncTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Method to send Participant message to participants on demand.
     *
     * @param participantAckMessage the Participant message
     */
    public void send(final ParticipantAckMessage participantAckMessage) {
        this.send(true, participantAckMessage);
    }

    /**
     * Method to send Participant message to participants on demand.
     *
     * @param participantAckMessage the Participant message
     */
    public void send(final boolean isDefaultTopic, final ParticipantAckMessage participantAckMessage) {
        final String topic = getTopic(isDefaultTopic);
        NetLoggerUtil.log(NetLoggerUtil.EventType.OUT, "KAFKA", topic, participantAckMessage.toString());
        try {
            kafkaTemplate.send(getTopic(isDefaultTopic), participantAckMessage).join();
        } catch (final Exception e) {
            log.warn("send to {} failed because of {}", getTopic(isDefaultTopic), e.getMessage(), e);
            //log.warn("{}: SEND of {} cannot be performed because of {}", this, participantMessage, e.getMessage(), e);
        }
    }

    private String getTopic(final boolean isDefaultTopic) {
        return isDefaultTopic ? operationTopic : syncTopic;
    }
}
