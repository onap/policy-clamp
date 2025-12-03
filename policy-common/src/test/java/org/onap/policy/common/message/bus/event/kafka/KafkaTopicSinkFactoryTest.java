/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022, 2024 Nordix Foundation.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.onap.policy.common.message.bus.properties.MessageBusProperties.PROPERTY_KAFKA_SINK_TOPICS;
import static org.onap.policy.common.message.bus.properties.MessageBusProperties.PROPERTY_TOPIC_EFFECTIVE_TOPIC_SUFFIX;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.message.bus.event.base.TopicPropertyBuilder;
import org.onap.policy.common.parameters.topic.BusTopicParams;

class KafkaTopicSinkFactoryTest extends KafkaTopicFactoryTestBase<KafkaTopicSink> {

    private SinkFactory factory;
    public static final String KAFKA_SERVER = "localhost:9092";

    /**
     * Creates the object to be tested.
     */
    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();

        factory = new SinkFactory();
    }

    @AfterEach
    public void tearDown() {
        factory.destroy();
    }

    @Test
    @Override
    public void testBuildBusTopicParams() {
        super.testBuildBusTopicParams();
        super.testBuildBusTopicParams_Ex();
    }

    @Test
    @Override
    public void testBuildListOfStringString() {
        super.testBuildListOfStringString();

        // check parameters that were used
        BusTopicParams params = getLastParams();
        assertFalse(params.isAllowSelfSignedCerts());
    }

    @Test
    @Override
    public void testBuildProperties() {
        List<KafkaTopicSink> topics = buildTopics(makePropBuilder().makeTopic(MY_TOPIC).build());
        assertEquals(1, topics.size());
        assertEquals(MY_TOPIC, topics.get(0).getTopic());
        assertEquals(MY_EFFECTIVE_TOPIC, topics.get(0).getEffectiveTopic());

        BusTopicParams params = getLastParams();
        assertTrue(params.isManaged());
        assertFalse(params.isUseHttps());
        assertEquals(List.of(KAFKA_SERVER), params.getServers());
        assertEquals(MY_TOPIC, params.getTopic());
        assertEquals(MY_EFFECTIVE_TOPIC, params.getEffectiveTopic());
        assertEquals(MY_PARTITION, params.getPartitionId());
        assertNotNull(params.getAdditionalProps());

        List<KafkaTopicSink> topics2 = buildTopics(makePropBuilder().makeTopic(TOPIC3)
            .removeTopicProperty(PROPERTY_TOPIC_EFFECTIVE_TOPIC_SUFFIX).build());
        assertEquals(1, topics2.size());
        assertEquals(TOPIC3, topics2.get(0).getTopic());
        assertEquals(topics2.get(0).getTopic(), topics2.get(0).getEffectiveTopic());

        initFactory();

        assertEquals(1, buildTopics(makePropBuilder().makeTopic(MY_TOPIC).build()).size());
    }

    @Test
    void testBuildFromProperties() {
        Properties props = makePropBuilder().makeTopic(MY_TOPIC).build();
        var listTopic = factory.build(props);
        assertNotNull(listTopic);
    }

    @Test
    @Override
    public void testDestroyString_testGet_testInventory() {
        super.testDestroyString_testGet_testInventory();
        super.testDestroyString_Ex();
    }

    @Test
    @Override
    public void testDestroy() {
        super.testDestroy();
    }

    @Test
    void testGet() {
        super.testGet_Ex();
    }

    @Test
    void testToString() {
        assertTrue(factory.toString().startsWith("IndexedKafkaTopicSinkFactory ["));
    }

    @Override
    protected void initFactory() {
        if (factory != null) {
            factory.destroy();
        }

        factory = new SinkFactory();
    }

    @Override
    protected List<KafkaTopicSink> buildTopics(Properties properties) {
        return factory.build(properties);
    }

    @Override
    protected KafkaTopicSink buildTopic(BusTopicParams params) {
        return factory.build(params);
    }

    @Override
    protected KafkaTopicSink buildTopic(List<String> servers, String topic) {
        return factory.build(servers, topic);
    }

    @Override
    protected void destroyFactory() {
        factory.destroy();
    }

    @Override
    protected void destroyTopic(String topic) {
        factory.destroy(topic);
    }

    @Override
    protected List<KafkaTopicSink> getInventory() {
        return factory.inventory();
    }

    @Override
    protected KafkaTopicSink getTopic(String topic) {
        return factory.get(topic);
    }

    @Override
    protected BusTopicParams getLastParams() {
        return factory.params.getLast();
    }

    @Override
    protected TopicPropertyBuilder makePropBuilder() {
        return new KafkaTopicPropertyBuilder(PROPERTY_KAFKA_SINK_TOPICS);
    }

    /**
     * Factory that records the parameters of all the sinks it creates.
     */
    private static class SinkFactory extends IndexedKafkaTopicSinkFactory {
        private Deque<BusTopicParams> params = new LinkedList<>();

        @Override
        protected KafkaTopicSink makeSink(BusTopicParams busTopicParams) {
            params.add(busTopicParams);
            return super.makeSink(busTopicParams);
        }
    }
}
