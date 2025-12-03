/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023-2024 Nordix Foundation.
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

package org.onap.policy.common.message.bus.event.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onap.policy.common.message.bus.event.base.BusConsumer.FetchingBusConsumer;
import org.onap.policy.common.message.bus.event.base.BusConsumer.KafkaConsumerWrapper;
import org.onap.policy.common.message.bus.properties.MessageBusProperties;
import org.onap.policy.common.parameters.topic.BusTopicParams;

class BusConsumerTest extends TopicTestBase {

    private static final int SHORT_TIMEOUT_MILLIS = 10;
    private static final int LONG_TIMEOUT_MILLIS = 3000;

    @Mock
    KafkaConsumer<String, String> mockedKafkaConsumer;

    AutoCloseable closeable;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeable.close();
    }


    @Test
    void testFetchingBusConsumer() {
        // should not be negative
        var cons = new FetchingBusConsumerImpl(makeBuilder().fetchTimeout(-1).build());
        assertThat(cons.getSleepTime()).isEqualTo(MessageBusProperties.DEFAULT_TIMEOUT_MS_FETCH);

        // should not be zero
        cons = new FetchingBusConsumerImpl(makeBuilder().fetchTimeout(0).build());
        assertThat(cons.getSleepTime()).isEqualTo(MessageBusProperties.DEFAULT_TIMEOUT_MS_FETCH);

        // should not be too large
        cons = new FetchingBusConsumerImpl(
                        makeBuilder().fetchTimeout(MessageBusProperties.DEFAULT_TIMEOUT_MS_FETCH + 100).build());
        assertThat(cons.getSleepTime()).isEqualTo(MessageBusProperties.DEFAULT_TIMEOUT_MS_FETCH);

        // should not be what was specified
        cons = new FetchingBusConsumerImpl(makeBuilder().fetchTimeout(100).build());
        assertThat(cons.getSleepTime()).isEqualTo(100);
    }

    @Test
    void testFetchingBusConsumerSleepAfterFetchFailure() throws InterruptedException {

        var cons = new FetchingBusConsumerImpl(makeBuilder().fetchTimeout(SHORT_TIMEOUT_MILLIS).build()) {

            private CountDownLatch started = new CountDownLatch(1);

            @Override
            protected void sleepAfterFetchFailure() {
                started.countDown();
                super.sleepAfterFetchFailure();
            }
        };

        // full sleep
        long tstart = System.currentTimeMillis();
        cons.sleepAfterFetchFailure();
        assertThat(System.currentTimeMillis() - tstart).isGreaterThanOrEqualTo(SHORT_TIMEOUT_MILLIS);

        // close while sleeping - sleep should halt prematurely
        cons.fetchTimeout = LONG_TIMEOUT_MILLIS;
        cons.started = new CountDownLatch(1);
        Thread thread = new Thread(cons::sleepAfterFetchFailure);
        tstart = System.currentTimeMillis();
        thread.start();
        cons.started.await();
        cons.close();
        thread.join();
        assertThat(System.currentTimeMillis() - tstart).isLessThan(LONG_TIMEOUT_MILLIS);

        // interrupt while sleeping - sleep should halt prematurely
        cons.fetchTimeout = LONG_TIMEOUT_MILLIS;
        cons.started = new CountDownLatch(1);
        thread = new Thread(cons::sleepAfterFetchFailure);
        tstart = System.currentTimeMillis();
        thread.start();
        cons.started.await();
        thread.interrupt();
        thread.join();
        assertThat(System.currentTimeMillis() - tstart).isLessThan(LONG_TIMEOUT_MILLIS);
    }

    @Test
    void testKafkaConsumerWrapper() {
        // verify that different wrappers can be built
        assertThatCode(() -> new KafkaConsumerWrapper(makeKafkaBuilder().build())).doesNotThrowAnyException();
    }

    @Test
    void testKafkaConsumerWrapper_InvalidTopic() {
        BusTopicParams params = makeBuilder().topic(null).build();
        assertThatThrownBy(() -> new KafkaConsumerWrapper(params))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testKafkaConsumerWrapperFetch() {

        //Setup Properties for consumer
        Properties kafkaProps = new Properties();
        kafkaProps.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        kafkaProps.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "test");
        kafkaProps.setProperty("enable.auto.commit", "true");
        kafkaProps.setProperty("auto.commit.interval.ms", "1000");
        kafkaProps.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
            "org.apache.kafka.common.serialization.StringDeserializer");
        kafkaProps.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            "org.apache.kafka.common.serialization.StringDeserializer");
        kafkaProps.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        kafkaProps.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        KafkaConsumerWrapper kafka = new KafkaConsumerWrapper(makeKafkaBuilder().build());
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(kafkaProps);
        kafka.consumer = consumer;

        assertThrows(java.lang.IllegalStateException.class, () -> kafka.fetch().iterator().hasNext());
        consumer.close();
    }

    @Test
    void testFetchNoMessages() {
        KafkaConsumerWrapper kafkaConsumerWrapper = new KafkaConsumerWrapper(makeKafkaBuilder().build());
        kafkaConsumerWrapper.consumer = mockedKafkaConsumer;

        when(mockedKafkaConsumer.poll(any())).thenReturn(new ConsumerRecords<>(Collections.emptyMap()));

        Iterable<String> result = kafkaConsumerWrapper.fetch();

        verify(mockedKafkaConsumer).poll(any());

        assertNotNull(result);

        assertFalse(result.iterator().hasNext());

        mockedKafkaConsumer.close();
    }

    @Test
    void testFetchWithMessages() {
        // Setup
        KafkaConsumerWrapper kafkaConsumerWrapper = new KafkaConsumerWrapper(makeKafkaBuilder().build());
        kafkaConsumerWrapper.consumer = mockedKafkaConsumer;

        ConsumerRecord<String, String> customerRecord =
            new ConsumerRecord<>("my-effective-topic", 0, 0, "key", "value");
        Map<TopicPartition, List<ConsumerRecord<String, String>>> recordsMap = new HashMap<>();
        recordsMap.put(new TopicPartition("my-effective-topic", 0), Collections.singletonList(customerRecord));
        ConsumerRecords<String, String> consumerRecords = new ConsumerRecords<>(recordsMap);

        when(mockedKafkaConsumer.poll(any())).thenReturn(consumerRecords);

        Iterable<String> result = kafkaConsumerWrapper.fetch();

        verify(mockedKafkaConsumer, times(1)).poll(any());

        verify(mockedKafkaConsumer, times(1)).commitSync(any(Map.class));

        assertNotNull(result);

        assertTrue(result.iterator().hasNext());

        assertEquals("value", result.iterator().next());

        mockedKafkaConsumer.close();
    }

    @Test
    void testFetchWithMessagesAndTraceParent() {
        // Setup
        KafkaConsumerWrapper kafkaConsumerWrapper = new KafkaConsumerWrapper(makeKafkaBuilder().build());
        kafkaConsumerWrapper.consumer = mockedKafkaConsumer;

        ConsumerRecord<String, String> customerRecord =
            new ConsumerRecord<>("my-effective-topic", 0, 0, "key", "value");
        customerRecord.headers().add(
                "traceparent",
                "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01".getBytes(StandardCharsets.UTF_8)
        );

        Map<TopicPartition, List<ConsumerRecord<String, String>>> recordsMap = new HashMap<>();
        recordsMap.put(new TopicPartition("my-effective-topic", 0), Collections.singletonList(customerRecord));
        ConsumerRecords<String, String> consumerRecords = new ConsumerRecords<>(recordsMap);

        when(mockedKafkaConsumer.poll(any())).thenReturn(consumerRecords);

        Iterable<String> result = kafkaConsumerWrapper.fetch();

        verify(mockedKafkaConsumer, times(1)).poll(any());

        verify(mockedKafkaConsumer, times(1)).commitSync(any(Map.class));

        assertNotNull(result);

        assertTrue(result.iterator().hasNext());

        assertEquals("value", result.iterator().next());

        mockedKafkaConsumer.close();
    }


    @Test
    void testKafkaConsumerWrapperClose() {
        assertThatCode(() -> new KafkaConsumerWrapper(makeKafkaBuilder().build()).close()).doesNotThrowAnyException();
    }

    @Test
    void testKafkaConsumerWrapperToString() {
        assertNotNull(new KafkaConsumerWrapper(makeKafkaBuilder().build()) {}.toString());
    }

    private static class FetchingBusConsumerImpl extends FetchingBusConsumer {

        protected FetchingBusConsumerImpl(BusTopicParams busTopicParams) {
            super(busTopicParams);
        }

        @Override
        public Iterable<String> fetch() {
            return null;
        }
    }
}
