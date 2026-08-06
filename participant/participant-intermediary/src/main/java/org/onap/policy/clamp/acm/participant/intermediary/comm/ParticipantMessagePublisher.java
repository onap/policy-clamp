/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2026 OpenInfra Foundation Europe. All rights reserved.
 *  Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.policy.clamp.common.acm.utils.NetLoggerUtil;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeployAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantDeregister;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantKafkaMessage;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrimeAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantRegister;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantReqSync;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Unified participant message publisher using ParticipantKafkaMessage interface.
 */
@Component
@Slf4j
public class ParticipantMessagePublisher {

    @Value("${participant.intermediaryParameters.topics.operationTopic}")
    private String operationTopic;

    private final KafkaTemplate<String, ParticipantKafkaMessage> kafkaTemplate;

    public ParticipantMessagePublisher(
            @Qualifier("acmKafkaTemplate") KafkaTemplate<String, ParticipantKafkaMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Method to send Participant Request Sync message to clamp.
     *
     * @param participantReqSync the Participant Request Sync
     */
    @Timed(value = "publisher.participant_req_sync", description = "PARTICIPANT_REQ_SYNC_MSG messages published")
    public void sendParticipantReqSync(final ParticipantReqSync participantReqSync) {
        send(participantReqSync, "Sent Participant Request Sync to CLAMP");
    }

    /**
     * Method to send Participant Status message to clamp on demand.
     *
     * @param participantStatus the Participant Status
     */
    @Timed(value = "publisher.participant_status", description = "PARTICIPANT_STATUS messages published")
    public void sendParticipantStatus(final ParticipantStatus participantStatus) {
        send(participantStatus, "Sent Participant Status message to CLAMP");
    }

    /**
     * Method to send Participant Register message to clamp.
     *
     * @param participantRegister the Participant Register
     */
    @Timed(value = "publisher.participant_register", description = "PARTICIPANT_REGISTER messages published")
    public void sendParticipantRegister(final ParticipantRegister participantRegister) {
        send(participantRegister, "Sent Participant Register message to CLAMP");
    }

    /**
     * Method to send Participant Deregister message to clamp.
     *
     * @param participantDeregister the Participant Deregister
     */
    @Timed(value = "publisher.participant_deregister", description = "PARTICIPANT_DEREGISTER messages published")
    public void sendParticipantDeregister(final ParticipantDeregister participantDeregister) {
        send(participantDeregister, "Sent Participant Deregister message to CLAMP");
    }

    /**
     * Method to send Participant Prime Ack message to runtime.
     *
     * @param participantPrimeAck the Participant Prime Ack
     */
    @Timed(value = "publisher.participant_prime_ack", description = "PARTICIPANT_PRIME_ACK messages published")
    public void sendParticipantPrimeAck(final ParticipantPrimeAck participantPrimeAck) {
        send(participantPrimeAck, "Sent Participant Prime Ack message to CLAMP");
    }

    /**
     * Method to send AutomationComposition Update/StateChange Ack message to runtime.
     *
     * @param automationCompositionAck AutomationComposition Update/StateChange Ack
     */
    @Timed(value = "publisher.automation_composition_update_ack",
            description = "AUTOMATION_COMPOSITION_UPDATE_ACK/AUTOMATION_COMPOSITION_STATECHANGE_ACK messages published")
    public void sendAutomationCompositionAck(final AutomationCompositionDeployAck automationCompositionAck) {
        send(automationCompositionAck, "Sent AutomationComposition Update/StateChange Ack to runtime");
    }

    private void send(final ParticipantKafkaMessage message, final String logMessage) {
        NetLoggerUtil.log(NetLoggerUtil.EventType.OUT, "KAFKA", operationTopic,
                logMessage + " - " + message.toString());
        try {
            if (message.getPartitionKey() == null) {
                kafkaTemplate.send(operationTopic, message).join();
            } else {
                kafkaTemplate.send(operationTopic, message.getPartitionKey(), message).join();
            }
        } catch (final Exception e) {
            log.warn("send to {} failed because of {}", operationTopic, e.getMessage(), e);
        }
    }
}
