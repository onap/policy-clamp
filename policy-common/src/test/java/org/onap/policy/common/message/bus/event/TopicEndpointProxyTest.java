/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024 Nordix Foundation.
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

package org.onap.policy.common.message.bus.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.message.bus.event.Topic.CommInfrastructure;
import org.onap.policy.common.message.bus.event.kafka.KafkaTopicFactories;
import org.onap.policy.common.message.bus.event.kafka.KafkaTopicPropertyBuilder;
import org.onap.policy.common.message.bus.event.noop.NoopTopicFactories;
import org.onap.policy.common.message.bus.event.noop.NoopTopicPropertyBuilder;
import org.onap.policy.common.message.bus.properties.MessageBusProperties;
import org.onap.policy.common.parameters.topic.TopicParameterGroup;
import org.onap.policy.common.parameters.topic.TopicParameters;
import org.onap.policy.common.utils.gson.GsonTestUtils;

class TopicEndpointProxyTest {

    private static final String NOOP_SOURCE_TOPIC = "noop-source";
    private static final String NOOP_SINK_TOPIC = "noop-sink";

    private static final String KAFKA_SOURCE_TOPIC = "kafka-source";
    private static final String KAFKA_SINK_TOPIC = "kafka-sink";

    private final Properties configuration = new Properties();
    private final TopicParameterGroup group = new TopicParameterGroup();

    /**
     * Constructor.
     */
    public TopicEndpointProxyTest() {
        group.setTopicSinks(new LinkedList<>());
        group.setTopicSources(new LinkedList<>());

        NoopTopicPropertyBuilder noopSourceBuilder =
                new NoopTopicPropertyBuilder(MessageBusProperties.PROPERTY_NOOP_SOURCE_TOPICS)
                        .makeTopic(NOOP_SOURCE_TOPIC);
        configuration.putAll(noopSourceBuilder.build());
        group.getTopicSources().add(noopSourceBuilder.getParams());

        NoopTopicPropertyBuilder noopSinkBuilder =
                new NoopTopicPropertyBuilder(MessageBusProperties.PROPERTY_NOOP_SINK_TOPICS)
                        .makeTopic(NOOP_SINK_TOPIC);
        configuration.putAll(noopSinkBuilder.build());
        group.getTopicSinks().add(noopSinkBuilder.getParams());

        TopicParameters invalidCommInfraParams =
                new NoopTopicPropertyBuilder(MessageBusProperties.PROPERTY_NOOP_SOURCE_TOPICS)
                        .makeTopic(NOOP_SOURCE_TOPIC).getParams();
        invalidCommInfraParams.setTopicCommInfrastructure(Topic.CommInfrastructure.REST.name());
        group.getTopicSources().add(invalidCommInfraParams);
        group.getTopicSinks().add(invalidCommInfraParams);
    }

    private <T extends Topic> boolean exists(List<T> topics, String topicName) {
        return topics.stream().map(Topic::getTopic).anyMatch(topicName::equals);
    }

    private <T extends Topic> boolean allSources(List<T> topics) {
        return exists(topics, NOOP_SOURCE_TOPIC);
    }

    private <T extends Topic> boolean allSinks(List<T> topics) {
        return exists(topics, NOOP_SINK_TOPIC);
    }

    private <T extends Topic> boolean anySource(List<T> topics) {
        return exists(topics, NOOP_SOURCE_TOPIC);
    }

    private <T extends Topic> boolean anySink(List<T> topics) {
        return exists(topics, NOOP_SINK_TOPIC);
    }

    /**
     * Destroys all managed topics.
     */
    @AfterEach
    public void tearDown() {
        NoopTopicFactories.getSinkFactory().destroy();
        NoopTopicFactories.getSourceFactory().destroy();
        KafkaTopicFactories.getSinkFactory().destroy();
        KafkaTopicFactories.getSourceFactory().destroy();
    }

    @Test
    void testSerialize() {
        TopicEndpoint manager = new TopicEndpointProxy();

        manager.addTopicSources(configuration);
        manager.addTopicSinks(configuration);

        assertThatCode(() -> new GsonTestUtils().compareGson(manager, TopicEndpointProxyTest.class))
                .doesNotThrowAnyException();
    }

    @Test
    void testAddTopicSourcesListOfTopicParameters() {
        TopicEndpoint manager = new TopicEndpointProxy();

        List<TopicSource> sources = manager.addTopicSources(group.getTopicSources());
        assertSame(1, sources.size());

        assertTrue(allSources(sources));
        assertFalse(anySink(sources));

        sources = manager.addTopicSources(group.getTopicSources());
        assertSame(1, sources.size());
        assertTrue(allSources(sources));
    }

    @Test
    void testAddTopicSourcesKafka() {
        TopicEndpoint manager = new TopicEndpointProxy();

        KafkaTopicPropertyBuilder kafkaTopicPropertyBuilder =
            new KafkaTopicPropertyBuilder(MessageBusProperties.PROPERTY_KAFKA_SOURCE_TOPICS)
                .makeTopic(KAFKA_SOURCE_TOPIC);

        configuration.putAll(kafkaTopicPropertyBuilder.build());
        group.getTopicSources().add(kafkaTopicPropertyBuilder.getParams());
        List<TopicSource> sources = manager.addTopicSources(group.getTopicSources());
        assertSame(2, sources.size());

        configuration.remove(KAFKA_SOURCE_TOPIC);
        group.setTopicSources(new LinkedList<>());
        sources = manager.addTopicSources(group.getTopicSources());
        assertSame(0, sources.size());
    }

    @Test
    void testAddTopicSourcesProperties() {
        TopicEndpoint manager = new TopicEndpointProxy();

        List<TopicSource> sources = manager.addTopicSources(configuration);
        assertSame(1, sources.size());

        assertTrue(allSources(sources));
        assertFalse(anySink(sources));
    }

    @Test
    void testAddTopicSinksListOfTopicParameters() {
        TopicEndpoint manager = new TopicEndpointProxy();

        List<TopicSink> sinks = manager.addTopicSinks(group.getTopicSinks());
        assertSame(1, sinks.size());

        assertFalse(anySource(sinks));
        assertTrue(allSinks(sinks));
    }

    @Test
    void testAddTopicSinksListOfTopicParametersKafka() {
        TopicEndpoint manager = new TopicEndpointProxy();

        List<TopicSink> sinks = manager.addTopicSinks(group.getTopicSinks());
        assertSame(1, sinks.size());

        KafkaTopicPropertyBuilder kafkaTopicPropertyBuilder =
            new KafkaTopicPropertyBuilder(MessageBusProperties.PROPERTY_KAFKA_SINK_TOPICS)
                .makeTopic(KAFKA_SINK_TOPIC);

        configuration.putAll(kafkaTopicPropertyBuilder.build());
        group.getTopicSources().add(kafkaTopicPropertyBuilder.getParams());
        sinks = manager.addTopicSinks(group.getTopicSources());
        assertSame(2, sinks.size());

        configuration.remove(KAFKA_SOURCE_TOPIC);
        group.setTopicSources(new LinkedList<>());
        sinks = manager.addTopicSinks(group.getTopicSources());
        assertSame(0, sinks.size());
    }

    @Test
    void testAddTopicSinksProperties() {
        TopicEndpoint manager = new TopicEndpointProxy();

        List<TopicSink> sinks = manager.addTopicSinks(configuration);
        assertSame(1, sinks.size());

        assertFalse(anySource(sinks));
        assertTrue(allSinks(sinks));
    }

    @Test
    void testAddTopicsProperties() {
        TopicEndpoint manager = new TopicEndpointProxy();

        List<Topic> topics = manager.addTopics(configuration);
        assertSame(2, topics.size());

        assertTrue(allSources(topics));
        assertTrue(allSinks(topics));
    }

    @Test
    void testAddTopicsTopicParameterGroup() {
        TopicEndpoint manager = new TopicEndpointProxy();

        List<Topic> topics = manager.addTopics(group);
        assertSame(2, topics.size());

        assertTrue(allSources(topics));
        assertTrue(allSinks(topics));
    }

    @Test
    void testAddTopicsTopicParameterGroupNull() {
        TopicEndpoint manager = new TopicEndpointProxy();

        List<Topic> topics = manager.addTopics(new TopicParameterGroup());
        assertEquals(0, topics.size());
    }

    @Test
    void testLockSinks_lockSources_locked() {
        TopicEndpoint manager = new TopicEndpointProxy();
        manager.lock();
        for (Topic topic : manager.addTopics(group)) {
            assertTrue(topic.isLocked());
        }
    }

    @Test
    void testLockSinks_lockSources_unlocked() {
        TopicEndpoint manager = new TopicEndpointProxy();
        for (Topic topic : manager.addTopics(group)) {
            assertFalse(topic.isLocked());
        }
    }

    @Test
    void testGetTopicSources() {
        TopicEndpoint manager = new TopicEndpointProxy();

        manager.addTopicSources(configuration);
        manager.addTopicSinks(configuration);

        List<TopicSource> sources = manager.getTopicSources();
        assertSame(1, sources.size());

        assertTrue(allSources(sources));
        assertFalse(anySink(sources));

        assertThatThrownBy(() -> manager.getKafkaTopicSource("testTopic"))
            .hasMessageContaining("KafkaTopiceSource for testTopic not found");

        List<String> topicName = null;
        assertThatThrownBy(() -> manager.getTopicSources(topicName))
            .hasMessageContaining("must provide a list of topics");
    }

    @Test
    void testGetTopicSinks() {
        TopicEndpoint manager = new TopicEndpointProxy();

        manager.addTopicSources(configuration);
        manager.addTopicSinks(configuration);

        List<TopicSink> sinks = manager.getTopicSinks();
        assertSame(1, sinks.size());

        assertFalse(anySource(sinks));
        assertTrue(allSinks(sinks));

        final List<String> sinks2 = null;
        assertThatThrownBy(() -> manager.getTopicSinks(sinks2)).hasMessageContaining("must provide a list of topics");

        List<String> sinks3 = List.of(NOOP_SINK_TOPIC);
        assertThatCode(() -> manager.getTopicSinks(sinks3)).doesNotThrowAnyException();

        String sinkTest = null;
        assertThatThrownBy(() -> manager.getTopicSinks(sinkTest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid parameter");

        assertThatThrownBy(() -> manager.getKafkaTopicSink("testTopic"))
            .hasMessageContaining("KafkaTopicSink for testTopic not found");
    }

    @Test
    void testGetNoopTopicSources() {
        TopicEndpoint manager = new TopicEndpointProxy();

        manager.addTopicSources(configuration);
        assertSame(1, manager.getNoopTopicSources().size());
    }

    @Test
    void testGetNoopTopicSinks() {
        TopicEndpoint manager = new TopicEndpointProxy();

        manager.addTopicSinks(configuration);
        assertSame(1, manager.getNoopTopicSinks().size());
    }

    @Test
    void testLifecycle() {
        TopicEndpoint manager = new TopicEndpointProxy();

        assertTrue(manager.start());
        assertTrue(manager.isAlive());

        assertTrue(manager.stop());
        assertFalse(manager.isAlive());

        assertTrue(manager.start());
        assertTrue(manager.isAlive());

        manager.shutdown();
        assertFalse(manager.isAlive());
    }

    @Test
    void testLock() {
        TopicEndpoint manager = new TopicEndpointProxy();

        manager.lock();
        assertTrue(manager.isLocked());

        manager.unlock();
        assertFalse(manager.isLocked());
    }

    @Test
    void testGetTopicSource() {
        TopicEndpoint manager = new TopicEndpointProxy();
        manager.addTopicSources(configuration);

        assertSame(NOOP_SOURCE_TOPIC, manager.getTopicSource(CommInfrastructure.NOOP, NOOP_SOURCE_TOPIC).getTopic());

        assertThatIllegalStateException()
                .isThrownBy(() -> manager.getTopicSource(CommInfrastructure.NOOP, NOOP_SINK_TOPIC));
    }

    @Test
    void testGetTopicSink() {
        TopicEndpoint manager = new TopicEndpointProxy();
        manager.addTopicSinks(configuration);

        assertSame(NOOP_SINK_TOPIC, manager.getTopicSink(CommInfrastructure.NOOP, NOOP_SINK_TOPIC).getTopic());

        assertThatIllegalStateException()
                .isThrownBy(() -> manager.getTopicSink(CommInfrastructure.NOOP, NOOP_SOURCE_TOPIC));
    }

    @Test
    void testGetNoopTopicSource() {
        TopicEndpoint manager = new TopicEndpointProxy();
        manager.addTopicSources(configuration);

        assertSame(NOOP_SOURCE_TOPIC, manager.getNoopTopicSource(NOOP_SOURCE_TOPIC).getTopic());

        assertThatIllegalArgumentException().isThrownBy(() -> manager.getNoopTopicSource(null));
        assertThatIllegalArgumentException().isThrownBy(() -> manager.getNoopTopicSource(""));
    }

    @Test
    void testGetNoopTopicSink() {
        TopicEndpoint manager = new TopicEndpointProxy();
        manager.addTopicSinks(configuration);

        assertSame(NOOP_SINK_TOPIC, manager.getNoopTopicSink(NOOP_SINK_TOPIC).getTopic());

        assertThatIllegalArgumentException().isThrownBy(() -> manager.getNoopTopicSink(null));
        assertThatIllegalArgumentException().isThrownBy(() -> manager.getNoopTopicSink(""));
    }
}
