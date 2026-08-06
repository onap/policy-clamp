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
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantKafkaMessage
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessageType
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantRegisterAck
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

    def "should handle #messageType when appliesTo"() {
        given:
        participantHandler.appliesTo(message) >> true

        when:
        listener.onTopicEvent(message)

        then:
        1 * participantHandler."$handlerMethod"(message)

        where:
        message                    | handlerMethod                  | messageType
        new ParticipantSync()      | "handleParticipantSync"        | "ParticipantSync"
        new ParticipantRegisterAck() | "handleParticipantRegisterAck" | "ParticipantRegisterAck"
        new ParticipantStatusReq() | "handleParticipantStatusReq"   | "ParticipantStatusReq"
    }

    def "should skip #messageType when does not apply"() {
        given:
        participantHandler.appliesTo(message) >> false

        when:
        listener.onTopicEvent(message)

        then:
        0 * participantHandler."$handlerMethod"(_)

        where:
        message                    | handlerMethod                  | messageType
        new ParticipantSync()      | "handleParticipantSync"        | "ParticipantSync"
        new ParticipantRegisterAck() | "handleParticipantRegisterAck" | "ParticipantRegisterAck"
        new ParticipantStatusReq() | "handleParticipantStatusReq"   | "ParticipantStatusReq"
    }

    def "should log and discard unhandled events"() {
        given:
        def message = Mock(ParticipantKafkaMessage)
        message.getMessageType() >> ParticipantMessageType.PARTICIPANT_STATUS

        when:
        listener.onUnhandledEvent(message)

        then:
        0 * participantHandler._
    }
}
