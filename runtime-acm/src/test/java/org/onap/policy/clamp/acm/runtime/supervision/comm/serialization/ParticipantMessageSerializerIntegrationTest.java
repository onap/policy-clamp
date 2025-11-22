/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.runtime.supervision.comm.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka
@ActiveProfiles({"test", "default"})
class ParticipantMessageSerializerIntegrationTest {

    private static final String TEST_TOPIC = "test-serialization-topic";

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private ParticipantStatus receivedMessage;

    @Test
    void testSerializerWithKafka() {
        var originalMessage = new ParticipantStatus();
        originalMessage.setParticipantId(UUID.randomUUID());
        originalMessage.setTimestamp(Instant.now());

        kafkaTemplate.send(TEST_TOPIC, originalMessage);

        await().atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                assertThat(receivedMessage).isNotNull();
                assertThat(receivedMessage.getParticipantId()).isEqualTo(originalMessage.getParticipantId());
                assertThat(receivedMessage.getTimestamp()).isEqualTo(originalMessage.getTimestamp());
            });
    }

    @KafkaListener(topics = TEST_TOPIC)
    void listen(ParticipantStatus message) {
        receivedMessage = message;
    }
}
