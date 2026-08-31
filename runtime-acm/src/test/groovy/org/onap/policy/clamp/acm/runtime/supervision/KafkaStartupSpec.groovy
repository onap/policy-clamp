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
package org.onap.policy.clamp.acm.runtime.supervision

import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup
import org.onap.policy.clamp.acm.runtime.main.parameters.Topics
import org.springframework.kafka.KafkaException
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.core.KafkaAdmin
import spock.lang.Specification

class KafkaStartupSpec extends Specification {

    def registry = Mock(KafkaListenerEndpointRegistry)
    def kafkaAdmin = Mock(KafkaAdmin)

    def "should start listeners immediately when topic validation is disabled"() {
        given:
        def startup = createKafkaStartup(false)

        when:
        startup.startListenersWhenReady()

        then:
        0 * kafkaAdmin.describeTopics(*_)
        1 * registry.getListenerContainers() >> []
    }

    def "should validate topics before starting listeners"() {
        given:
        def startup = createKafkaStartup(true)

        when:
        startup.startListenersWhenReady()

        then:
        1 * kafkaAdmin.describeTopics('operationTopic', 'syncTopic')
        1 * registry.getListenerContainers() >> []
    }

    def "should retry when topic check fails then succeeds"() {
        given:
        def startup = createKafkaStartup(true)

        when:
        startup.startListenersWhenReady()

        then:
        2 * kafkaAdmin.describeTopics('operationTopic', 'syncTopic') >> { throw new KafkaException('broker not available') } >> [:]
        1 * registry.getListenerContainers() >> []
    }

    private KafkaStartup createKafkaStartup(boolean enableTopicValidation) {
        def params = new AcRuntimeParameterGroup(
                topics: new Topics(operationTopic: 'operationTopic', syncTopic: 'syncTopic'),
                topicValidation: enableTopicValidation
        )
        return new KafkaStartup(registry, kafkaAdmin, params)
    }
}
