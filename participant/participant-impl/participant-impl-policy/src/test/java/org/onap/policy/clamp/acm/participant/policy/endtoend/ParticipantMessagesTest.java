/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2026 OpenInfra Foundation Europe. All rights reserved.
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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.acm.participant.intermediary.handler.ParticipantHandler;
import org.onap.policy.clamp.acm.participant.policy.main.parameters.CommonTestData;
import org.onap.policy.clamp.acm.participant.policy.main.utils.TestListenerUtils;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantDeregister;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantDeregisterAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrimeAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantRegister;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantRegisterAck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "default"})
@EmbeddedKafka
class ParticipantMessagesTest {

    @Autowired
    private ParticipantHandler participantHandler;

    @Autowired
    private ParticipantMessagePublisher participantMessagePublisher;

    @Test
    void testSendParticipantRegisterMessage() {
        final var participantRegisterMsg = new ParticipantRegister();
        participantRegisterMsg.setParticipantId(CommonTestData.getParticipantId());
        participantRegisterMsg.setTimestamp(Instant.now());

        assertThatCode(() -> participantMessagePublisher.sendParticipantRegister(participantRegisterMsg))
            .doesNotThrowAnyException();
    }

    @Test
    void testReceiveParticipantRegisterAckMessage() {
        final var participantRegisterAckMsg = new ParticipantRegisterAck();
        participantRegisterAckMsg.setMessage("ParticipantRegisterAck message");
        participantRegisterAckMsg.setResponseTo(UUID.randomUUID());

        assertThatCode(() -> participantHandler.handleParticipantRegisterAck(participantRegisterAckMsg))
            .doesNotThrowAnyException();
    }

    @Test
    void testSendParticipantDeregisterMessage() {
        final var participantDeregisterMsg = new ParticipantDeregister();
        participantDeregisterMsg.setParticipantId(CommonTestData.getParticipantId());
        participantDeregisterMsg.setTimestamp(Instant.now());

        assertThatCode(() -> participantMessagePublisher.sendParticipantDeregister(participantDeregisterMsg))
            .doesNotThrowAnyException();
    }

    @Test
    void testReceiveParticipantDeregisterAckMessage() {
        final var participantDeregisterAckMsg = new ParticipantDeregisterAck();
        participantDeregisterAckMsg.setMessage("ParticipantDeregisterAck message");
        participantDeregisterAckMsg.setResponseTo(UUID.randomUUID());

        assertThatCode(() -> participantHandler.handleParticipantDeregisterAck(participantDeregisterAckMsg))
            .doesNotThrowAnyException();
    }

    @Test
    void testReceiveParticipantUpdateMessage() {
        var participantPrimeMsg = TestListenerUtils.createParticipantPrimeMsg();

        assertThatCode(() -> participantHandler.handleParticipantPrime(participantPrimeMsg))
            .doesNotThrowAnyException();
    }

    @Test
    void testSendParticipantPrimeAckMessage() {
        final var participantPrimeAckMsg = new ParticipantPrimeAck();
        participantPrimeAckMsg.setMessage("ParticipantPrimeAck message");
        participantPrimeAckMsg.setResponseTo(UUID.randomUUID());

        assertThatCode(() -> participantMessagePublisher.sendParticipantPrimeAck(participantPrimeAckMsg))
            .doesNotThrowAnyException();
    }
}
