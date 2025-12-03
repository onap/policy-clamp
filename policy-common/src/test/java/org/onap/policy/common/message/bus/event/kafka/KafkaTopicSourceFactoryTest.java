/*
 * ============LICENSE_START=======================================================
 * ONAP Policy Engine - Common Modules
 * ================================================================================
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.onap.policy.common.message.bus.properties.MessageBusProperties.PROPERTY_KAFKA_SOURCE_TOPICS;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.message.bus.event.base.TopicPropertyBuilder;
import org.onap.policy.common.parameters.topic.BusTopicParams;

class KafkaTopicSourceFactoryTest extends KafkaTopicFactoryTestBase<KafkaTopicSource> {

    private SourceFactory factory;

    public static final String KAFKA_SERVER = "localhost:9092";

    /**
     * Creates the object to be tested.
     */
    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();

        factory = new SourceFactory();
    }

    @AfterEach
    public void tearDown() {
        factory.destroy();
    }

    @Test
    @Override
    public void testBuildProperties() {

        initFactory();

        List<KafkaTopicSource> topics = buildTopics(makePropBuilder().makeTopic(MY_TOPIC).build());
        assertEquals(1, topics.size());
        assertEquals(MY_TOPIC, topics.get(0).getTopic());
        assertEquals(MY_EFFECTIVE_TOPIC, topics.get(0).getEffectiveTopic());

        BusTopicParams params = getLastParams();
        assertTrue(params.isManaged());
        assertFalse(params.isUseHttps());
        assertEquals(List.of(KAFKA_SERVER), params.getServers());
        assertEquals(MY_TOPIC, params.getTopic());
        assertEquals(MY_EFFECTIVE_TOPIC, params.getEffectiveTopic());
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
        assertTrue(factory.toString().startsWith("IndexedKafkaTopicSourceFactory ["));
    }

    @Override
    protected void initFactory() {
        if (factory != null) {
            factory.destroy();
        }

        factory = new SourceFactory();
    }

    @Override
    protected List<KafkaTopicSource> buildTopics(Properties properties) {
        return factory.build(properties);
    }

    @Override
    protected KafkaTopicSource buildTopic(BusTopicParams params) {
        return factory.build(params);
    }

    @Override
    protected KafkaTopicSource buildTopic(List<String> servers, String topic) {
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
    protected List<KafkaTopicSource> getInventory() {
        return factory.inventory();
    }

    @Override
    protected KafkaTopicSource getTopic(String topic) {
        return factory.get(topic);
    }

    @Override
    protected BusTopicParams getLastParams() {
        return factory.params.getLast();
    }

    @Override
    protected TopicPropertyBuilder makePropBuilder() {
        return new KafkaTopicPropertyBuilder(PROPERTY_KAFKA_SOURCE_TOPICS);
    }

    /**
     * Factory that records the parameters of all the sources it creates.
     */
    private static class SourceFactory extends IndexedKafkaTopicSourceFactory {
        private final Deque<BusTopicParams> params = new LinkedList<>();

        @Override
        protected KafkaTopicSource makeSource(BusTopicParams busTopicParams) {
            params.add(busTopicParams);
            return super.makeSource(busTopicParams);
        }
    }
}
