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

package org.onap.policy.clamp.acm.participant.intermediary.comm

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import java.util.concurrent.CompletableFuture
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeployAck
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantDeregister
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrimeAck
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantRegister
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantReqSync
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatus
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import spock.lang.Specification

class ParticipantMessagePublisherSpec extends Specification {

    static final PARTICIPANT_ID = UUID.fromString("101c62b3-8918-41b9-a747-d21eb79c6c01")
    static final COMPOSITION_ID = UUID.fromString("709c62b3-8918-41b9-a747-d21eb79c6c02")
    static final TOPIC = "policy-acruntime-participant"

    KafkaTemplate kafkaTemplate = Mock()
    ParticipantMessagePublisher publisher

    def setup() {
        publisher = new ParticipantMessagePublisher(kafkaTemplate)
        publisher.operationTopic = TOPIC  // Note groovy allows setting private fields directly
    }

    def "send ParticipantStatus with participantId as partition key"() {
        given:
        def message = new ParticipantStatus(participantId: PARTICIPANT_ID)

        when:
        publisher.sendParticipantStatus(message)

        then:
        1 * kafkaTemplate.send(TOPIC, PARTICIPANT_ID.toString(), message) >> completedFuture()
    }

    def "send ParticipantRegister with participantId as partition key"() {
        given:
        def message = new ParticipantRegister(participantId: PARTICIPANT_ID)

        when:
        publisher.sendParticipantRegister(message)

        then:
        1 * kafkaTemplate.send(TOPIC, PARTICIPANT_ID.toString(), message) >> completedFuture()
    }

    def "send ParticipantDeregister with participantId as partition key"() {
        given:
        def message = new ParticipantDeregister(participantId: PARTICIPANT_ID)

        when:
        publisher.sendParticipantDeregister(message)

        then:
        1 * kafkaTemplate.send(TOPIC, PARTICIPANT_ID.toString(), message) >> completedFuture()
    }

    def "send ParticipantPrimeAck with compositionId as partition key"() {
        given:
        def message = new ParticipantPrimeAck(participantId: PARTICIPANT_ID, compositionId: COMPOSITION_ID)

        when:
        publisher.sendParticipantPrimeAck(message)

        then:
        1 * kafkaTemplate.send(TOPIC, COMPOSITION_ID.toString(), message) >> completedFuture()
    }

    def "send AutomationCompositionDeployAck with compositionId as partition key"() {
        given:
        def message = new AutomationCompositionDeployAck(participantId: PARTICIPANT_ID, compositionId: COMPOSITION_ID)

        when:
        publisher.sendAutomationCompositionAck(message)

        then:
        1 * kafkaTemplate.send(TOPIC, COMPOSITION_ID.toString(), message) >> completedFuture()
    }

    def "send ParticipantReqSync with participantId as partition key"() {
        given:
        def message = new ParticipantReqSync(participantId: PARTICIPANT_ID)

        when:
        publisher.sendParticipantReqSync(message)

        then:
        1 * kafkaTemplate.send(TOPIC, PARTICIPANT_ID.toString(), message) >> completedFuture()
    }

    def "send without partition key when message has no IDs (should never happen)"() {
        given:
        def message = new ParticipantStatus()

        when:
        publisher.sendParticipantStatus(message)

        then:
        1 * kafkaTemplate.send(TOPIC, message) >> completedFuture()
    }

    def "log warning and do not throw when send fails"() {
        given: 'logging is captured'
        def logger = (Logger) LoggerFactory.getLogger(ParticipantMessagePublisher)
        def appender = new ListAppender<ILoggingEvent>()
        appender.start()
        logger.addAppender(appender)

        and: 'kafka is down'
        kafkaTemplate.send(*_) >> failedFuture("Kafka down")

        when: 'a message is sent'
        publisher.sendParticipantRegister(new ParticipantRegister(participantId: PARTICIPANT_ID))

        then: 'no exception is thrown'
        noExceptionThrown()
        and: 'a warning is logged'
        appender.list.any { it.level == Level.WARN && it.formattedMessage.contains("Kafka down") }

        cleanup:
        logger.detachAppender(appender)
    }

    private static CompletableFuture<SendResult> completedFuture() {
        return CompletableFuture.completedFuture(null)
    }

    private static CompletableFuture<SendResult> failedFuture(String message) {
        def future = new CompletableFuture<SendResult>()
        future.completeExceptionally(new RuntimeException(message))
        return future
    }
}
