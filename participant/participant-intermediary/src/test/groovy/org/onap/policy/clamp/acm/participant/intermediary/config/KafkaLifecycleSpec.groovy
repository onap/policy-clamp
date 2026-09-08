/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

import org.onap.policy.clamp.acm.participant.intermediary.comm.HeartbeatSender
import org.onap.policy.clamp.acm.participant.intermediary.handler.ParticipantHandler
import org.onap.policy.clamp.acm.participant.intermediary.parameters.ParticipantIntermediaryParameters
import org.onap.policy.clamp.acm.participant.intermediary.parameters.ParticipantParameters
import org.onap.policy.clamp.acm.participant.intermediary.parameters.Topics
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.core.KafkaAdmin
import org.springframework.kafka.listener.MessageListenerContainer
import spock.lang.Specification

class KafkaLifecycleSpec extends Specification {

    def registry = Mock(KafkaListenerEndpointRegistry)
    def kafkaAdmin = Mock(KafkaAdmin)
    def participantHandler = Mock(ParticipantHandler)
    def messageSender = Mock(HeartbeatSender)
    def operationContainer = Mock(MessageListenerContainer)
    def syncContainer = Mock(MessageListenerContainer)
    def kafkaLifecycle

    def setup() {
        registry.getListenerContainer("acmOperationListener") >> operationContainer
        registry.getListenerContainer("acmSyncListener") >> syncContainer
        kafkaAdmin.describeTopics(_, _) >> [:]

        def params = Mock(ParticipantParameters)
        params.getIntermediaryParameters() >> new ParticipantIntermediaryParameters(
                topics: new Topics("policy-acruntime-participant", "acm-ppnt-sync"))

        kafkaLifecycle = new KafkaLifecycle(registry, kafkaAdmin, participantHandler, messageSender, params)
    }

    def "initially not running"() {
        expect:
        !kafkaLifecycle.isRunning()
    }

    def "start waits for kafka, starts containers, registers and starts heartbeat"() {
        when:
        kafkaLifecycle.start()

        then:
        1 * operationContainer.start()
        1 * syncContainer.start()
        1 * participantHandler.sendParticipantRegister()
        1 * messageSender.startScheduler()
        kafkaLifecycle.isRunning()
    }

    def "stop deregisters and stops containers"() {
        given:
        kafkaLifecycle.start()

        when:
        kafkaLifecycle.stop()

        then:
        1 * messageSender.close()
        1 * participantHandler.sendParticipantDeregister()
        1 * operationContainer.stop()
        1 * syncContainer.stop()
        !kafkaLifecycle.isRunning()
    }

    def "phase is MAX_VALUE - 50"() {
        expect:
        kafkaLifecycle.getPhase() == Integer.MAX_VALUE - 50
    }
}
