/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2017-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2018 Samsung Electronics Co., Ltd.
 * Modifications Copyright (C) 2020,2023 Bell Canada. All rights reserved.
 * Modifications Copyright (C) 2022-2024 Nordix Foundation.
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

import static org.onap.policy.common.message.bus.properties.MessageBusProperties.DEFAULT_TIMEOUT_MS_FETCH;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.kafkaclients.v2_6.TracingConsumerInterceptor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Headers;
import org.onap.policy.common.parameters.topic.BusTopicParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper around libraries to consume from message bus.
 */
public interface BusConsumer {

    /**
     * fetch messages.
     *
     * @return list of messages
     * @throws IOException when error encountered by underlying libraries
     */
    Iterable<String> fetch() throws IOException;

    /**
     * close underlying library consumer.
     */
    void close();

    /**
     * Consumer that handles fetch() failures by sleeping.
     */
    abstract class FetchingBusConsumer implements BusConsumer {
        private static final Logger logger = LoggerFactory.getLogger(FetchingBusConsumer.class);

        /**
         * Fetch timeout.
         */
        protected int fetchTimeout;

        /**
         * Time to sleep on a fetch failure.
         */
        @Getter
        private final int sleepTime;

        /**
         * Counted down when {@link #close()} is invoked.
         */
        private final CountDownLatch closeCondition = new CountDownLatch(1);


        /**
         * Constructs the object.
         *
         * @param busTopicParams parameters for the bus topic
         */
        protected FetchingBusConsumer(BusTopicParams busTopicParams) {
            this.fetchTimeout = busTopicParams.getFetchTimeout();

            if (this.fetchTimeout <= 0) {
                this.sleepTime = DEFAULT_TIMEOUT_MS_FETCH;
            } else {
                // don't sleep too long, even if fetch timeout is large
                this.sleepTime = Math.min(this.fetchTimeout, DEFAULT_TIMEOUT_MS_FETCH);
            }
        }

        /**
         * Causes the thread to sleep; invoked after fetch() fails.  If the consumer is closed,
         * or the thread is interrupted, then this will return immediately.
         */
        protected void sleepAfterFetchFailure() {
            try {
                logger.info("{}: backoff for {}ms", this, sleepTime);
                if (this.closeCondition.await(this.sleepTime, TimeUnit.MILLISECONDS)) {
                    logger.info("{}: closed while handling fetch error", this);
                }

            } catch (InterruptedException e) {
                logger.warn("{}: interrupted while handling fetch error", this, e);
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public void close() {
            this.closeCondition.countDown();
        }
    }

    /**
     * Kafka based consumer.
     */
    class KafkaConsumerWrapper extends FetchingBusConsumer {

        /**
         * logger.
         */
        private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerWrapper.class);

        private static final String KEY_DESERIALIZER = "org.apache.kafka.common.serialization.StringDeserializer";

        /**
         * Kafka consumer.
         */
        protected KafkaConsumer<String, String> consumer;
        protected Properties kafkaProps;

        protected boolean allowTracing;

        /**
         * Kafka Consumer Wrapper.
         * BusTopicParam - object contains the following parameters
         * servers - messaging bus hosts.
         * topic - topic
         *
         * @param busTopicParams - The parameters for the bus topic
         */
        public KafkaConsumerWrapper(BusTopicParams busTopicParams) {
            super(busTopicParams);

            if (busTopicParams.isTopicInvalid()) {
                throw new IllegalArgumentException("No topic for Kafka");
            }

            //Setup Properties for consumer
            kafkaProps = new Properties();
            kafkaProps.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                busTopicParams.getServers().get(0));

            if (busTopicParams.isAdditionalPropsValid()) {
                kafkaProps.putAll(busTopicParams.getAdditionalProps());
            }

            if (kafkaProps.get(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG) == null) {
                kafkaProps.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, KEY_DESERIALIZER);
            }
            if (kafkaProps.get(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG) == null) {
                kafkaProps.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KEY_DESERIALIZER);
            }
            if (kafkaProps.get(ConsumerConfig.GROUP_ID_CONFIG) == null) {
                kafkaProps.setProperty(ConsumerConfig.GROUP_ID_CONFIG, busTopicParams.getConsumerGroup());
            }
            if (busTopicParams.isAllowTracing()) {
                this.allowTracing = true;
                kafkaProps.setProperty(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG,
                    TracingConsumerInterceptor.class.getName());
            }

            consumer = new KafkaConsumer<>(kafkaProps);
            //Subscribe to the topic
            consumer.subscribe(List.of(busTopicParams.getTopic()));
        }

        @Override
        public Iterable<String> fetch() {
            ConsumerRecords<String, String> records = this.consumer.poll(Duration.ofMillis(fetchTimeout));
            if (records == null || records.count() <= 0) {
                return Collections.emptyList();
            }
            List<String> messages = new ArrayList<>(records.count());
            try {
                if (allowTracing) {
                    createParentTraceContext(records);
                }

                for (TopicPartition partition : records.partitions()) {
                    List<ConsumerRecord<String, String>> partitionRecords = records.records(partition);
                    for (ConsumerRecord<String, String> partitionRecord : partitionRecords) {
                        messages.add(partitionRecord.value());
                    }
                    long lastOffset = partitionRecords.get(partitionRecords.size() - 1).offset();
                    consumer.commitSync(Collections.singletonMap(partition, new OffsetAndMetadata(lastOffset + 1)));
                }
            } catch (Exception e) {
                logger.error("{}: cannot fetch, throwing exception after sleep...", this);
                sleepAfterFetchFailure();
                throw e;
            }
            return messages;
        }

        private void createParentTraceContext(ConsumerRecords<String, String> records) {
            TraceParentInfo traceParentInfo = new TraceParentInfo();
            for (ConsumerRecord<String, String> consumerRecord : records) {

                Headers consumerRecordHeaders = consumerRecord.headers();
                traceParentInfo = processTraceParentHeader(consumerRecordHeaders);
            }

            SpanContext spanContext = SpanContext.createFromRemoteParent(
                traceParentInfo.getTraceId(), traceParentInfo.getSpanId(),
                TraceFlags.getSampled(), TraceState.builder().build());

            Context.current().with(Span.wrap(spanContext)).makeCurrent();
        }

        private TraceParentInfo processTraceParentHeader(Headers headers) {
            TraceParentInfo traceParentInfo = new TraceParentInfo();
            if (headers.lastHeader("traceparent") != null) {
                traceParentInfo.setParentTraceId(new String(headers.lastHeader(
                    "traceparent").value(), StandardCharsets.UTF_8));

                String[] parts = traceParentInfo.getParentTraceId().split("-");
                traceParentInfo.setTraceId(parts[1]);
                traceParentInfo.setSpanId(parts[2]);
            }

            return traceParentInfo;
        }

        @Data
        @NoArgsConstructor
        private static class TraceParentInfo {
            private String parentTraceId;
            private String traceId;
            private String spanId;
        }

        @Override
        public void close() {
            super.close();
            this.consumer.close();
            logger.info("Kafka Consumer exited {}", this);
        }

        @Override
        public String toString() {
            return "KafkaConsumerWrapper [fetchTimeout=" + fetchTimeout + "]";
        }
    }
}


