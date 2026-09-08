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

package org.onap.policy.clamp.acm.runtime.config

import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup
import org.onap.policy.clamp.acm.runtime.main.parameters.Topics
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantStatusReqPublisher
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.core.KafkaAdmin
import org.springframework.kafka.listener.MessageListenerContainer
import spock.lang.Specification

class KafkaLifecycleSpec extends Specification {

    def registry = Mock(KafkaListenerEndpointRegistry)
    def kafkaAdmin = Mock(KafkaAdmin)
    def statusReqPublisher = Mock(ParticipantStatusReqPublisher)
    def container = Mock(MessageListenerContainer)
    def kafkaLifecycle

    def setup() {
        registry.getListenerContainer("participantMessageListener") >> container
        kafkaAdmin.describeTopics(_, _) >> [:]

        def parameterGroup = new AcRuntimeParameterGroup(
                topics: new Topics("operationTopic", "syncTopic"))

        kafkaLifecycle = new KafkaLifecycle(registry, kafkaAdmin, statusReqPublisher, parameterGroup)
    }

    def "initially not running"() {
        expect:
        !kafkaLifecycle.isRunning()
    }

    def "start waits for kafka, starts containers and sends status request"() {
        when:
        kafkaLifecycle.start()

        then:
        1 * container.start()
        1 * statusReqPublisher.broadcast()
        kafkaLifecycle.isRunning()
    }

    def "stop stops containers"() {
        given:
        kafkaLifecycle.start()

        when:
        kafkaLifecycle.stop()

        then:
        1 * container.stop()
        !kafkaLifecycle.isRunning()
    }

    def "phase is MAX_VALUE - 50"() {
        expect:
        kafkaLifecycle.getPhase() == Integer.MAX_VALUE - 50
    }
}
