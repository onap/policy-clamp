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
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeploy;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionMigration;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionPrepare;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionStateChange;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantDeregisterAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantKafkaMessage;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrime;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.PropertiesUpdate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Spring Kafka-based message listener for participant operation messages.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@KafkaListener(
        topics = "${participant.intermediaryParameters.topics.operationTopic}",
        containerFactory = "acmListenerContainerFactory"
)
public class ParticipantMessageListener {

    private final ParticipantHandler participantHandler;

    @Value("${participant.intermediaryParameters.topics.operationTopic}")
    private String operationTopic;

    /**
     * Handle ParticipantPrime messages.
     * @param participantPrime the message
     */
    @KafkaHandler
    public void onTopicEvent(final ParticipantPrime participantPrime) {
        processIfApplicable(participantPrime, participantHandler::handleParticipantPrime);
    }

    /**
     * Handle AutomationCompositionDeploy messages.
     * @param automationCompositionDeploy the message
     */
    @KafkaHandler
    public void onTopicEvent(final AutomationCompositionDeploy automationCompositionDeploy) {
        processIfApplicable(automationCompositionDeploy, participantHandler::handleAutomationCompositionDeploy);
    }

    /**
     * Handle AutomationCompositionStateChange messages.
     * @param automationCompositionStateChange the message
     */
    @KafkaHandler
    public void onTopicEvent(final AutomationCompositionStateChange automationCompositionStateChange) {
        processIfApplicable(automationCompositionStateChange,
                participantHandler::handleAutomationCompositionStateChange);
    }

    /**
     * Handle AutomationCompositionPrepare messages.
     * @param automationCompositionPrepare the message
     */
    @KafkaHandler
    public void onTopicEvent(final AutomationCompositionPrepare automationCompositionPrepare) {
        processIfApplicable(automationCompositionPrepare, participantHandler::handleAutomationCompositionPrepare);
    }

    /**
     * Handle AutomationCompositionMigration messages.
     * @param automationCompositionMigration the message
     */
    @KafkaHandler
    public void onTopicEvent(final AutomationCompositionMigration automationCompositionMigration) {
        processIfApplicable(automationCompositionMigration, participantHandler::handleAutomationCompositionMigration);
    }

    /**
     * Handle PropertiesUpdate messages.
     * @param propertiesUpdate the message
     */
    @KafkaHandler
    public void onTopicEvent(final PropertiesUpdate propertiesUpdate) {
        processIfApplicable(propertiesUpdate, participantHandler::handleAcPropertyUpdate);
    }

    /**
     * Handle ParticipantDeregisterAck messages.
     * @param participantDeregisterAck the message
     */
    @KafkaHandler
    public void onTopicEvent(final ParticipantDeregisterAck participantDeregisterAck) {
        processIfApplicable(participantDeregisterAck, participantHandler::handleParticipantDeregisterAck);
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
            NetLoggerUtil.logIncoming("KAFKA", operationTopic, message.toString());
            handler.accept(message);
        }
    }
}
