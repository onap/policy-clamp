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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@EmbeddedKafka(topics = {"test-topic"})
@DirtiesContext
@ActiveProfiles({"test", "default"})
class ParticipantMessageSerializerIntegrationTest {

    private static final String TEST_TOPIC = "test-topic";

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ConsumerFactory<String, Object> consumerFactory;

    @Test
    void testSerializerWithKafka() throws Exception {
        var containerProperties = new ContainerProperties(TEST_TOPIC);
        var container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);

        var receivedMessage = new Object[1];
        var latch = new CountDownLatch(1);

        container.setupMessageListener((MessageListener<String, Object>) consumerRecord -> {
            receivedMessage[0] = consumerRecord.value();
            latch.countDown();
        });

        container.start();
        ContainerTestUtils.waitForAssignment(container, embeddedKafka.getPartitionsPerTopic());

        var originalMessage = new ParticipantStatus();
        originalMessage.setParticipantId(UUID.randomUUID());
        originalMessage.setTimestamp(Instant.now());

        kafkaTemplate.send(TEST_TOPIC, originalMessage).get(10, TimeUnit.SECONDS);

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();

        container.stop();

        assertThat(receivedMessage[0]).isInstanceOf(ParticipantStatus.class);
        var deserializedMessage = (ParticipantStatus) receivedMessage[0];
        assertEquals(originalMessage.getParticipantId(), deserializedMessage.getParticipantId());
        assertEquals(originalMessage.getTimestamp(), deserializedMessage.getTimestamp());
    }
}
