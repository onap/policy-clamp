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
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantRegisterAck
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatus
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatusReq
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantSync
import spock.lang.Specification

class ParticipantSyncMessageListenerSpec extends Specification {

    ParticipantHandler participantHandler = Mock()
    ParticipantSyncMessageListener listener

    def setup() {
        listener = new ParticipantSyncMessageListener(participantHandler)
        listener.syncTopic = "acm-ppnt-sync"
    }

    def "handle #message.class.simpleName when message applies"() {
        given:
        participantHandler.appliesTo(message) >> true

        when:
        listener.onTopicEvent(message)

        then:
        1 * participantHandler."$handlerMethod"(message)

        where:
        message                      | handlerMethod
        new ParticipantSync()        | "handleParticipantSync"
        new ParticipantRegisterAck() | "handleParticipantRegisterAck"
        new ParticipantStatusReq()   | "handleParticipantStatusReq"
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
            new ParticipantSync(),
            new ParticipantRegisterAck(),
            new ParticipantStatusReq()
        ]
    }

    def "discard unhandled events"() {
        when:
        listener.onUnhandledEvent(new ParticipantStatus())

        then:
        0 * participantHandler._
    }
}
