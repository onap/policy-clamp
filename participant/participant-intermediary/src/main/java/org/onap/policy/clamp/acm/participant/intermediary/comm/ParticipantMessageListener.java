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
        if (participantHandler.appliesTo(participantPrime)) {
            logEvent(participantPrime.toString());
            participantHandler.handleParticipantPrime(participantPrime);
        }
    }

    /**
     * Handle AutomationCompositionDeploy messages.
     * @param automationCompositionDeploy the message
     */
    @KafkaHandler
    public void onTopicEvent(final AutomationCompositionDeploy automationCompositionDeploy) {
        if (participantHandler.appliesTo(automationCompositionDeploy)) {
            logEvent(automationCompositionDeploy.toString());
            participantHandler.handleAutomationCompositionDeploy(automationCompositionDeploy);
        }
    }

    /**
     * Handle AutomationCompositionStateChange messages.
     * @param automationCompositionStateChange the message
     */
    @KafkaHandler
    public void onTopicEvent(final AutomationCompositionStateChange automationCompositionStateChange) {
        if (participantHandler.appliesTo(automationCompositionStateChange)) {
            logEvent(automationCompositionStateChange.toString());
            participantHandler.handleAutomationCompositionStateChange(automationCompositionStateChange);
        }
    }

    /**
     * Handle AutomationCompositionPrepare messages.
     * @param automationCompositionPrepare the message
     */
    @KafkaHandler
    public void onTopicEvent(final AutomationCompositionPrepare automationCompositionPrepare) {
        if (participantHandler.appliesTo(automationCompositionPrepare)) {
            logEvent(automationCompositionPrepare.toString());
            participantHandler.handleAutomationCompositionPrepare(automationCompositionPrepare);
        }
    }

    /**
     * Handle AutomationCompositionMigration messages.
     * @param automationCompositionMigration the message
     */
    @KafkaHandler
    public void onTopicEvent(final AutomationCompositionMigration automationCompositionMigration) {
        if (participantHandler.appliesTo(automationCompositionMigration)) {
            logEvent(automationCompositionMigration.toString());
            participantHandler.handleAutomationCompositionMigration(automationCompositionMigration);
        }
    }

    /**
     * Handle PropertiesUpdate messages.
     * @param propertiesUpdate the message
     */
    @KafkaHandler
    public void onTopicEvent(final PropertiesUpdate propertiesUpdate) {
        if (participantHandler.appliesTo(propertiesUpdate)) {
            logEvent(propertiesUpdate.toString());
            participantHandler.handleAcPropertyUpdate(propertiesUpdate);
        }
    }

    /**
     * Handle ParticipantDeregisterAck messages.
     * @param participantDeregisterAck the message
     */
    @KafkaHandler
    public void onTopicEvent(final ParticipantDeregisterAck participantDeregisterAck) {
        if (participantHandler.appliesTo(participantDeregisterAck)) {
            logEvent(participantDeregisterAck.toString());
            participantHandler.handleParticipantDeregisterAck(participantDeregisterAck);
        }
    }

    /**
     * Log any unhandled events.
     * @param participantKafkaMessage the unhandled message
     */
    @KafkaHandler(isDefault = true)
    public void onUnhandledEvent(final ParticipantKafkaMessage participantKafkaMessage) {
        log.info("discarding event of type {}", participantKafkaMessage.getMessageType());
    }

    private void logEvent(final String message) {
        NetLoggerUtil.log(NetLoggerUtil.EventType.IN, "KAFKA", operationTopic, message);
    }
}
