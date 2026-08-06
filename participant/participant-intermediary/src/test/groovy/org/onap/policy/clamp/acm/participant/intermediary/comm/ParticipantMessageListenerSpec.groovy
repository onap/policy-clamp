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

import org.onap.policy.clamp.acm.participant.intermediary.handler.ParticipantHandler
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeploy
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionMigration
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionPrepare
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionStateChange
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantDeregisterAck
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrime
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatus
import org.onap.policy.clamp.models.acm.messages.kafka.participant.PropertiesUpdate
import spock.lang.Specification

class ParticipantMessageListenerSpec extends Specification {

    ParticipantHandler participantHandler = Mock()
    ParticipantMessageListener listener

    def setup() {
        listener = new ParticipantMessageListener(participantHandler)
        listener.operationTopic = "policy-acruntime-participant"
    }

    def "handle #message.class.simpleName when message applies"() {
        given:
        participantHandler.appliesTo(message) >> true

        when:
        listener.onTopicEvent(message)

        then:
        1 * participantHandler."$handlerMethod"(message)

        where:
        message                                | handlerMethod
        new ParticipantPrime()                 | "handleParticipantPrime"
        new AutomationCompositionDeploy()      | "handleAutomationCompositionDeploy"
        new AutomationCompositionStateChange() | "handleAutomationCompositionStateChange"
        new AutomationCompositionPrepare()     | "handleAutomationCompositionPrepare"
        new AutomationCompositionMigration()   | "handleAutomationCompositionMigration"
        new PropertiesUpdate()                 | "handleAcPropertyUpdate"
        new ParticipantDeregisterAck()         | "handleParticipantDeregisterAck"
    }

    def "skip #message.class.simpleName when message does not apply"() {
        given:
        participantHandler.appliesTo(message) >> false

        when:
        listener.onTopicEvent(message)

        then:
        1 * participantHandler.appliesTo(message)
        0 * participantHandler._

        where:
        message << [
            new ParticipantPrime(),
            new AutomationCompositionDeploy(),
            new AutomationCompositionStateChange(),
            new AutomationCompositionPrepare(),
            new AutomationCompositionMigration(),
            new PropertiesUpdate(),
            new ParticipantDeregisterAck()
        ]
    }

    def "discard unhandled events"() {
        when:
        listener.onUnhandledEvent(new ParticipantStatus())

        then:
        0 * participantHandler._
    }
}
