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

package org.onap.policy.clamp.acm.participant.intermediary.config

import java.time.Duration
import org.onap.policy.clamp.acm.participant.intermediary.parameters.KafkaParameters
import org.onap.policy.clamp.acm.participant.intermediary.parameters.ParticipantIntermediaryParameters
import org.onap.policy.clamp.acm.participant.intermediary.parameters.ParticipantParameters
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import spock.lang.Specification

class KafkaConfigSpec extends Specification {

    KafkaConfig kafkaConfig
    ParticipantParameters participantParameters

    def setup() {
        kafkaConfig = new KafkaConfig()
        kafkaConfig.tracingEnabled = false

        def kafkaParams = new KafkaParameters(
                bootstrapServers: "localhost:9092",
                authExceptionRetryInterval: Duration.ofSeconds(5),
                properties: ["security.protocol": "PLAINTEXT"],
                consumer: new KafkaParameters.ConsumerParameters(
                        groupId: "test-group",
                        properties: ["auto.offset.reset": "earliest"]
                ),
                producer: new KafkaParameters.ProducerParameters(
                        properties: ["acks": "all"]
                )
        )

        def intermediaryParams = new ParticipantIntermediaryParameters(kafka: kafkaParams)

        participantParameters = Mock(ParticipantParameters)
        participantParameters.getIntermediaryParameters() >> intermediaryParams
    }

    def "should create consumer factory"() {
        expect:
        kafkaConfig.acmConsumerFactory(participantParameters) instanceof DefaultKafkaConsumerFactory
    }

    def "should create producer factory"() {
        expect:
        kafkaConfig.acmProducerFactory(participantParameters) instanceof DefaultKafkaProducerFactory
    }

    def "should create KafkaTemplate from producer factory"() {
        given:
        def producerFactory = kafkaConfig.acmProducerFactory(participantParameters)

        expect:
        kafkaConfig.acmKafkaTemplate(producerFactory, participantParameters) instanceof KafkaTemplate
    }

    def "should create listener container factory"() {
        given:
        def consumerFactory = kafkaConfig.acmConsumerFactory(participantParameters)

        expect:
        kafkaConfig.acmListenerContainerFactory(consumerFactory, participantParameters) instanceof ConcurrentKafkaListenerContainerFactory
    }

    def "should enable observation when tracing is enabled"() {
        given:
        kafkaConfig.tracingEnabled = true
        def producerFactory = kafkaConfig.acmProducerFactory(participantParameters)
        def consumerFactory = kafkaConfig.acmConsumerFactory(participantParameters)

        when:
        def template = kafkaConfig.acmKafkaTemplate(producerFactory, participantParameters)
        def listenerFactory = kafkaConfig.acmListenerContainerFactory(consumerFactory, participantParameters)

        then:
        template.observationEnabled == true
        listenerFactory.containerProperties.observationEnabled == true
    }

    def "should disable observation when tracing is disabled"() {
        given:
        kafkaConfig.tracingEnabled = false
        def producerFactory = kafkaConfig.acmProducerFactory(participantParameters)
        def consumerFactory = kafkaConfig.acmConsumerFactory(participantParameters)

        when:
        def template = kafkaConfig.acmKafkaTemplate(producerFactory, participantParameters)
        def listenerFactory = kafkaConfig.acmListenerContainerFactory(consumerFactory, participantParameters)

        then:
        template.observationEnabled == false
        listenerFactory.containerProperties.observationEnabled == false
    }
}
