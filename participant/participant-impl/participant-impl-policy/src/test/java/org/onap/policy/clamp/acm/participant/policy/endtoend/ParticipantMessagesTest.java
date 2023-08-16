/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation.
 *  Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.clamp.acm.participant.policy.endtoend;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantDeregisterAckListener;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantPrimeListener;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantRegisterAckListener;
import org.onap.policy.clamp.acm.participant.intermediary.handler.ParticipantHandler;
import org.onap.policy.clamp.acm.participant.policy.main.parameters.CommonTestData;
import org.onap.policy.clamp.acm.participant.policy.main.utils.TestListenerUtils;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantDeregister;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantDeregisterAck;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantPrimeAck;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantRegister;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantRegisterAck;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ParticipantMessagesTest {

    private static final Object lockit = new Object();
    private static final CommInfrastructure INFRA = CommInfrastructure.NOOP;
    private static final String TOPIC = "my-topic";

    @Autowired
    private ParticipantHandler participantHandler;

    @Test
    void testSendParticipantRegisterMessage() {
        final var participantRegisterMsg = new ParticipantRegister();
        participantRegisterMsg.setParticipantId(CommonTestData.getParticipantId());
        participantRegisterMsg.setTimestamp(Instant.now());

        synchronized (lockit) {
            var participantMessagePublisher = new ParticipantMessagePublisher();
            participantMessagePublisher.active(Collections.singletonList(Mockito.mock(TopicSink.class)));
            assertThatCode(() -> participantMessagePublisher.sendParticipantRegister(participantRegisterMsg))
                .doesNotThrowAnyException();
        }
    }

    @Test
    void testReceiveParticipantRegisterAckMessage() {
        final var participantRegisterAckMsg = new ParticipantRegisterAck();
        participantRegisterAckMsg.setMessage("ParticipantRegisterAck message");
        participantRegisterAckMsg.setResponseTo(UUID.randomUUID());
        participantRegisterAckMsg.setResult(true);

        synchronized (lockit) {
            var participantRegisterAckListener = new ParticipantRegisterAckListener(participantHandler);
            assertThatCode(() -> participantRegisterAckListener.onTopicEvent(INFRA, TOPIC, null,
                participantRegisterAckMsg)).doesNotThrowAnyException();
        }
    }

    @Test
    void testSendParticipantDeregisterMessage() {
        final var participantDeregisterMsg = new ParticipantDeregister();
        participantDeregisterMsg.setParticipantId(CommonTestData.getParticipantId());
        participantDeregisterMsg.setTimestamp(Instant.now());

        synchronized (lockit) {
            var participantMessagePublisher = new ParticipantMessagePublisher();
            participantMessagePublisher.active(Collections.singletonList(Mockito.mock(TopicSink.class)));
            assertThatCode(() -> participantMessagePublisher.sendParticipantDeregister(participantDeregisterMsg))
                .doesNotThrowAnyException();
        }
    }

    @Test
    void testReceiveParticipantDeregisterAckMessage() {
        final var participantDeregisterAckMsg = new ParticipantDeregisterAck();
        participantDeregisterAckMsg.setMessage("ParticipantDeregisterAck message");
        participantDeregisterAckMsg.setResponseTo(UUID.randomUUID());
        participantDeregisterAckMsg.setResult(true);

        synchronized (lockit) {
            var participantDeregisterAckListener = new ParticipantDeregisterAckListener(participantHandler);
            assertThatCode(() -> participantDeregisterAckListener.onTopicEvent(INFRA, TOPIC, null,
                participantDeregisterAckMsg)).doesNotThrowAnyException();
        }
    }

    @Test
    void testReceiveParticipantUpdateMessage() {
        var participantPrimeMsg = TestListenerUtils.createParticipantPrimeMsg();

        synchronized (lockit) {
            var participantPrimeListener = new ParticipantPrimeListener(participantHandler);
            assertThatCode(() -> participantPrimeListener.onTopicEvent(INFRA, TOPIC, null, participantPrimeMsg))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void testSendParticipantPrimeAckMessage() {
        final var participantPrimeAckMsg = new ParticipantPrimeAck();
        participantPrimeAckMsg.setMessage("ParticipantPrimeAck message");
        participantPrimeAckMsg.setResponseTo(UUID.randomUUID());
        participantPrimeAckMsg.setResult(true);

        synchronized (lockit) {
            var participantMessagePublisher = new ParticipantMessagePublisher();
            participantMessagePublisher.active(Collections.singletonList(Mockito.mock(TopicSink.class)));
            assertThatCode(() -> participantMessagePublisher.sendParticipantPrimeAck(participantPrimeAckMsg))
                .doesNotThrowAnyException();
        }
    }
}
