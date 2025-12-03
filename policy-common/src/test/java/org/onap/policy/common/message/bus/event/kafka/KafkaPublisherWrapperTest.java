/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2024 Nordix Foundation
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
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.common.message.bus.event.kafka;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.onap.policy.common.parameters.topic.BusTopicParams;

class KafkaPublisherWrapperTest {

    private KafkaPublisherWrapper kafkaPublisherWrapper;
    private Producer<String, String> mockProducer;
    private BusTopicParams mockBusTopicParams;

    @BeforeEach
    void setUp() {
        mockProducer = mock(KafkaProducer.class);
        mockBusTopicParams = mock(BusTopicParams.class);

        when(mockBusTopicParams.getTopic()).thenReturn("testTopic");
        when(mockBusTopicParams.getServers()).thenReturn(Collections.singletonList("localhost:9092"));
        when(mockBusTopicParams.isTopicInvalid()).thenReturn(false);
        when(mockBusTopicParams.isAdditionalPropsValid()).thenReturn(false);
        when(mockBusTopicParams.isAllowTracing()).thenReturn(false);

        kafkaPublisherWrapper = new KafkaPublisherWrapper(mockBusTopicParams) {
            private Producer<String, String> createProducer(Properties props) { // NOSONAR instance creation
                return mockProducer;
            }
        };
    }

    @Test
    void testConstructor() {
        verify(mockBusTopicParams).getTopic();
        verify(mockBusTopicParams).getServers();
        verify(mockBusTopicParams).isTopicInvalid();
        verify(mockBusTopicParams).isAdditionalPropsValid();
        verify(mockBusTopicParams).isAllowTracing();
    }

    @Test
    void testSendSuccess() {
        when(mockProducer.send(ArgumentMatchers.any(ProducerRecord.class))).thenReturn(null);
        assertTrue(kafkaPublisherWrapper.send("partitionId", "testMessage"));
    }

    @Test
    void testSendNullMessage() {
        IllegalArgumentException thrown = assertThrows(
            IllegalArgumentException.class,
            () -> kafkaPublisherWrapper.send("partitionId", null),
            "Expected send() to throw, but it didn't"
        );
        assertEquals("No message provided", thrown.getMessage());
    }

    @Test
    void testSendFailure() {
        when(mockProducer.send(ArgumentMatchers.any(ProducerRecord.class))).thenThrow(RuntimeException.class);
        assertTrue(kafkaPublisherWrapper.send("partitionId", "testMessage"));
    }

    @Test
    void testClose() {
        assertThatCode(kafkaPublisherWrapper::close).doesNotThrowAnyException();
    }
}
