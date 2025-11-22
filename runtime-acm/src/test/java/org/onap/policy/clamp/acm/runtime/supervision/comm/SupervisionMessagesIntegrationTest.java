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

package org.onap.policy.clamp.acm.runtime.supervision.comm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatusReq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"acm-ppnt-sync"})
@DirtiesContext
@ActiveProfiles({"test", "default"}) // TODO have kafka profile (also have separate db profile)
@TestPropertySource(properties = {
    "runtime.topics.syncTopic=acm-ppnt-sync",
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "logging.level.org.apache.kafka=WARN",
    "logging.level.kafka=WARN"
})
class SupervisionMessagesIntegrationTest {

    @Autowired
    private ParticipantStatusReqPublisher participantStatusReqPublisher;

    private ParticipantStatusReq receivedMessage;

    @Test
    void testParticipantStatusReqPublisher_EndToEnd() {
        var participantId = CommonTestData.getParticipantId();

        participantStatusReqPublisher.send(participantId);

        await().atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                assertThat(receivedMessage).isNotNull();
                assertThat(receivedMessage.getParticipantId()).isEqualTo(participantId);
            });
    }

    @KafkaListener(topics = "acm-ppnt-sync")
    void listen(ParticipantStatusReq message) {
        receivedMessage = message;
    }
}
