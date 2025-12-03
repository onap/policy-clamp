/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024 Nordix Foundation
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

package org.onap.policy.common.message.bus.event.noop;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.onap.policy.common.message.bus.properties.MessageBusProperties.PROPERTY_MANAGED_SUFFIX;
import static org.onap.policy.common.message.bus.properties.MessageBusProperties.PROPERTY_TOPIC_SERVERS_SUFFIX;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.message.bus.event.Topic.CommInfrastructure;
import org.onap.policy.common.message.bus.event.base.TopicFactoryTestBase;
import org.onap.policy.common.message.bus.event.base.TopicPropertyBuilder;
import org.onap.policy.common.message.bus.event.base.TopicTestBase;
import org.onap.policy.common.parameters.topic.BusTopicParams;

public abstract class NoopTopicFactoryTest<F extends NoopTopicFactory<T>, T extends NoopTopicEndpoint>
    extends TopicFactoryTestBase<T> {

    private static final List<String> NOOP_SERVERS = List.of(CommInfrastructure.NOOP.toString());
    private F factory = null;

    protected abstract F buildFactory();

    /**
     * Creates the object to be tested.
     */
    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        initFactory();
    }

    @AfterEach
    void tearDown() {
        factory.destroy();
    }

    @Test
    void testBuildBusTopicParams() {
        initFactory();

        T item1 = buildTopic(makeParams(servers));
        assertNotNull(item1);

        assertEquals(servers, item1.getServers());
        assertEquals(MY_TOPIC, item1.getTopic());
    }

    @Test
    void testBuildListOfStringStringBoolean() {
        initFactory();

        T item1 = buildTopic(servers, MY_TOPIC, true);
        assertNotNull(item1);

        assertEquals(servers, item1.getServers());
        assertEquals(MY_TOPIC, item1.getTopic());

        // managed topic - should not build a new one
        assertEquals(item1, buildTopic(servers, MY_TOPIC, true));

        T item2 = buildTopic(servers, TOPIC2, true);
        assertNotNull(item2);
        assertNotSame(item1, item2);

        // duplicate - should be the same, as these topics are managed
        List<String> randomServers = new ArrayList<>();
        randomServers.add(RandomStringUtils.randomAlphanumeric(8));
        T item3 = buildTopic(randomServers, TOPIC2, true);
        assertSame(item2, item3);

        T item4 = buildTopic(Collections.emptyList(), TOPIC2, true);
        assertSame(item3, item4);

        // null server list
        initFactory();
        assertEquals(NOOP_SERVERS, buildTopic(null, MY_TOPIC, true).getServers());

        // empty server list
        initFactory();
        assertEquals(NOOP_SERVERS, buildTopic(Collections.emptyList(), MY_TOPIC, true).getServers());

        // unmanaged topic
        initFactory();
        item1 = buildTopic(servers, MY_TOPIC, false);
        assertNotSame(item1, buildTopic(servers, MY_TOPIC, false));
    }

    @Test
    void testBuildListOfStringStringBoolean_NullTopic() {
        assertThatThrownBy(() -> buildTopic(servers, null, true)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testBuildListOfStringStringBoolean_EmptyTopic() {
        assertThatThrownBy(() -> buildTopic(servers, "", true)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testBuildProperties() {
        // managed topic
        initFactory();
        assertEquals(1, buildTopics(makePropBuilder().makeTopic(MY_TOPIC).build()).size());
        assertNotNull(factory.get(MY_TOPIC));

        // unmanaged topic - get() will throw an exception
        initFactory();
        assertEquals(1, buildTopics(makePropBuilder().makeTopic(MY_TOPIC)
                        .setTopicProperty(PROPERTY_MANAGED_SUFFIX, "false").build()).size());
        assertThatIllegalStateException().isThrownBy(() -> factory.get(MY_TOPIC));

        // managed undefined - default to true
        initFactory();
        assertEquals(1, buildTopics(
                        makePropBuilder().makeTopic(MY_TOPIC).removeTopicProperty(PROPERTY_MANAGED_SUFFIX).build())
                                        .size());
        assertNotNull(factory.get(MY_TOPIC));

        // managed empty - default to true
        initFactory();
        assertEquals(1, buildTopics(
                        makePropBuilder().makeTopic(MY_TOPIC).setTopicProperty(PROPERTY_MANAGED_SUFFIX, "").build())
                                        .size());
        assertNotNull(factory.get(MY_TOPIC));

        initFactory();

        // null topic list
        assertTrue(buildTopics(makePropBuilder().build()).isEmpty());

        // empty topic list
        assertTrue(buildTopics(makePropBuilder().addTopic("").build()).isEmpty());

        // null server list
        initFactory();
        T endpoint = buildTopics(makePropBuilder().makeTopic(MY_TOPIC)
                        .removeTopicProperty(PROPERTY_TOPIC_SERVERS_SUFFIX).build()).get(0);
        assertEquals(NOOP_SERVERS, endpoint.getServers());

        // empty server list
        initFactory();
        endpoint = buildTopics(makePropBuilder().makeTopic(MY_TOPIC).setTopicProperty(PROPERTY_TOPIC_SERVERS_SUFFIX, "")
                        .build()).get(0);
        assertEquals(NOOP_SERVERS, endpoint.getServers());

        // test other options
        super.testBuildProperties_Multiple();
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

    @Override
    protected void initFactory() {
        if (factory != null) {
            factory.destroy();
        }

        factory = buildFactory();
    }

    @Override
    protected List<T> buildTopics(Properties properties) {
        return factory.build(properties);
    }

    protected T buildTopic(BusTopicParams param) {
        return factory.build(param);
    }

    protected T buildTopic(List<String> servers, String topic, boolean managed) {
        return factory.build(servers, topic, managed);
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
    protected List<T> getInventory() {
        return factory.inventory();
    }

    @Override
    protected T getTopic(String topic) {
        return factory.get(topic);
    }

    @Override
    protected TopicPropertyBuilder makePropBuilder() {
        return new NoopTopicPropertyBuilder(factory.getTopicsPropertyName());
    }

    private BusTopicParams makeParams(List<String> servers) {
        BusTopicParams params = new BusTopicParams();

        params.setServers(servers);
        params.setTopic(TopicTestBase.MY_TOPIC);
        params.setManaged(true);

        return params;
    }
}
