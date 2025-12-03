/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022-2024 Nordix Foundation.
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

import static org.onap.policy.common.message.bus.properties.MessageBusProperties.PROPERTY_KAFKA_SINK_TOPICS;
import static org.onap.policy.common.message.bus.properties.MessageBusProperties.PROPERTY_TOPIC_SERVERS_SUFFIX;
import static org.onap.policy.common.message.bus.properties.MessageBusProperties.PROPERTY_TOPIC_SINK_PARTITION_KEY_SUFFIX;

import com.google.re2j.Pattern;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.onap.policy.common.message.bus.utils.KafkaPropertyUtils;
import org.onap.policy.common.parameters.topic.BusTopicParams;
import org.onap.policy.common.utils.properties.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory of KAFKA Reader Topics indexed by topic name.
 */
class IndexedKafkaTopicSinkFactory implements KafkaTopicSinkFactory {
    private static final Pattern COMMA_SPACE_PAT = Pattern.compile("\\s*,\\s*");
    private static final String MISSING_TOPIC = "A topic must be provided";

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(IndexedKafkaTopicSinkFactory.class);

    /**
     * KAFKA Topic Name Index.
     */
    protected HashMap<String, KafkaTopicSink> kafkaTopicSinks = new HashMap<>();

    @Override
    public KafkaTopicSink build(BusTopicParams busTopicParams) {

        if (busTopicParams.getServers() == null || busTopicParams.getServers().isEmpty()) {
            throw new IllegalArgumentException("KAFKA Server(s) must be provided");
        }

        if (StringUtils.isBlank(busTopicParams.getTopic())) {
            throw new IllegalArgumentException(MISSING_TOPIC);
        }

        synchronized (this) {
            if (kafkaTopicSinks.containsKey(busTopicParams.getTopic())) {
                return kafkaTopicSinks.get(busTopicParams.getTopic());
            }

            KafkaTopicSink kafkaTopicWriter = makeSink(busTopicParams);
            if (busTopicParams.isManaged()) {
                kafkaTopicSinks.put(busTopicParams.getTopic(), kafkaTopicWriter);
            }

            return kafkaTopicWriter;
        }
    }


    @Override
    public KafkaTopicSink build(List<String> servers, String topic) {
        return this.build(BusTopicParams.builder()
                .servers(servers)
                .topic(topic)
                .managed(true)
                .useHttps(false)
                .build());
    }


    @Override
    public List<KafkaTopicSink> build(Properties properties) {

        String writeTopics = properties.getProperty(PROPERTY_KAFKA_SINK_TOPICS);
        if (StringUtils.isBlank(writeTopics)) {
            logger.info("{}: no topic for KAFKA Sink", this);
            return new ArrayList<>();
        }

        List<KafkaTopicSink> newKafkaTopicSinks = new ArrayList<>();
        synchronized (this) {
            for (String topic : COMMA_SPACE_PAT.split(writeTopics)) {
                addTopic(newKafkaTopicSinks, topic.toLowerCase(), properties);
            }
            return newKafkaTopicSinks;
        }
    }

    private void addTopic(List<KafkaTopicSink> newKafkaTopicSinks, String topic, Properties properties) {
        if (this.kafkaTopicSinks.containsKey(topic)) {
            newKafkaTopicSinks.add(this.kafkaTopicSinks.get(topic));
            return;
        }

        String topicPrefix = PROPERTY_KAFKA_SINK_TOPICS + "." + topic;

        var props = new PropertyUtils(properties, topicPrefix,
            (name, value, ex) -> logger.warn("{}: {} {} is in invalid format for topic sink {} ",
                this, name, value, topic));

        String servers = properties.getProperty(topicPrefix + PROPERTY_TOPIC_SERVERS_SUFFIX);
        if (StringUtils.isBlank(servers)) {
            logger.error("{}: no KAFKA servers configured for sink {}", this, topic);
            return;
        }

        KafkaTopicSink kafkaTopicWriter = this.build(KafkaPropertyUtils.makeBuilder(props, topic, servers)
                .partitionId(props.getString(PROPERTY_TOPIC_SINK_PARTITION_KEY_SUFFIX, null))
                .build());
        newKafkaTopicSinks.add(kafkaTopicWriter);
    }

    @Override
    public void destroy(String topic) {

        if (topic == null || topic.isEmpty()) {
            throw new IllegalArgumentException(MISSING_TOPIC);
        }

        KafkaTopicSink kafkaTopicWriter;
        synchronized (this) {
            if (!kafkaTopicSinks.containsKey(topic)) {
                return;
            }

            kafkaTopicWriter = kafkaTopicSinks.remove(topic);
        }

        kafkaTopicWriter.shutdown();
    }

    @Override
    public void destroy() {
        List<KafkaTopicSink> writers = this.inventory();
        for (KafkaTopicSink writer : writers) {
            writer.shutdown();
        }

        synchronized (this) {
            this.kafkaTopicSinks.clear();
        }
    }

    @Override
    public KafkaTopicSink get(String topic) {

        if (topic == null || topic.isEmpty()) {
            throw new IllegalArgumentException(MISSING_TOPIC);
        }

        synchronized (this) {
            if (kafkaTopicSinks.containsKey(topic)) {
                return kafkaTopicSinks.get(topic);
            } else {
                throw new IllegalStateException("KafkaTopicSink for " + topic + " not found");
            }
        }
    }

    @Override
    public synchronized List<KafkaTopicSink> inventory() {
        return new ArrayList<>(this.kafkaTopicSinks.values());
    }

    /**
     * Makes a new sink.
     *
     * @param busTopicParams parameters to use to configure the sink
     * @return a new sink
     */
    protected KafkaTopicSink makeSink(BusTopicParams busTopicParams) {
        return new InlineKafkaTopicSink(busTopicParams);
    }


    @Override
    public String toString() {
        return "IndexedKafkaTopicSinkFactory " + kafkaTopicSinks.keySet();
    }

}
