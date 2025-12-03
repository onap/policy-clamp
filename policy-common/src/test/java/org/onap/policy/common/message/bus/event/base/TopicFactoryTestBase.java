/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018-2020 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.common.message.bus.event.base;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.onap.policy.common.message.bus.properties.MessageBusProperties.PROPERTY_TOPIC_SERVERS_SUFFIX;

import java.util.List;
import java.util.Properties;
import org.onap.policy.common.message.bus.event.Topic;

/**
 * Base class for XxxTopicFactory tests.
 *
 * @param <T> type of topic managed by the factory
 */
public abstract class TopicFactoryTestBase<T extends Topic> extends TopicTestBase {

    public static final String SERVER = "my-server";
    public static final String TOPIC2 = "my-topic-2";
    public static final String TOPIC3 = "my-topic-3";

    /**
     * Initializes a new factory.
     */
    protected abstract void initFactory();

    /**
     * Makes a property builder.
     *
     * @return a new property builder
     */
    protected abstract TopicPropertyBuilder makePropBuilder();

    /**
     * Builds a set of topics.
     *
     * @param properties the properties used to configure the topics
     * @return a list of new topics
     */
    protected abstract List<T> buildTopics(Properties properties);

    /**
     * Destroys the factory.
     */
    protected abstract void destroyFactory();

    /**
     * Destroys a topic within the factory.
     *
     * @param topic the topic to destroy
     */
    protected abstract void destroyTopic(String topic);

    /**
     * Gets the list of topics from the factory.
     *
     * @return the topic inventory
     */
    protected abstract List<T> getInventory();

    /**
     * Gets a topic from the factory.
     *
     * @param topic the topic name
     * @return the topic
     */
    protected abstract T getTopic(String topic);


    /**
     * Tests building a topic using varied Properties.
     */
    void testBuildProperties_Variations() {
        initFactory();

        // null topic list
        assertTrue(buildTopics(makePropBuilder().build()).isEmpty());

        // empty topic list
        assertTrue(buildTopics(makePropBuilder().addTopic("").build()).isEmpty());

        // null servers
        assertTrue(buildTopics(makePropBuilder().makeTopic(MY_TOPIC).removeTopicProperty(PROPERTY_TOPIC_SERVERS_SUFFIX)
            .build()).isEmpty());

        // empty servers
        assertTrue(buildTopics(makePropBuilder().makeTopic(MY_TOPIC).setTopicProperty(PROPERTY_TOPIC_SERVERS_SUFFIX, "")
            .build()).isEmpty());
    }

    /**
     * Tests building multiple topics using Properties.
     */
    public void testBuildProperties_Multiple() {
        initFactory();

        // make two fully-defined topics, and add two duplicate topic names to the list
        TopicPropertyBuilder builder =
            makePropBuilder().makeTopic(MY_TOPIC).makeTopic(TOPIC2).addTopic(MY_TOPIC).addTopic(MY_TOPIC);

        List<T> lst = buildTopics(builder.build());
        assertEquals(4, lst.size());

        int index = 0;
        T item = lst.get(index++);
        assertNotSame(item, lst.get(index++));
        assertSame(item, lst.get(index++));
        assertSame(item, lst.get(index++));
    }

    /**
     * Tests destroy(topic), get(topic), and inventory() methods.
     */
    public void testDestroyString_testGet_testInventory() {
        initFactory();

        List<T> lst = buildTopics(makePropBuilder().makeTopic(MY_TOPIC).makeTopic(TOPIC2).build());

        int index = 0;
        T item1 = lst.get(index++);
        T item2 = lst.get(index++);

        assertEquals(2, getInventory().size());
        assertTrue(getInventory().contains(item1));
        assertTrue(getInventory().contains(item2));

        item1.start();
        item2.start();

        assertEquals(item1, getTopic(MY_TOPIC));
        assertEquals(item2, getTopic(TOPIC2));

        destroyTopic(MY_TOPIC);
        assertFalse(item1.isAlive());
        assertTrue(item2.isAlive());
        assertEquals(item2, getTopic(TOPIC2));
        assertEquals(1, getInventory().size());
        assertTrue(getInventory().contains(item2));

        // repeat
        destroyTopic(MY_TOPIC);
        assertFalse(item1.isAlive());
        assertTrue(item2.isAlive());

        // with other topic
        destroyTopic(TOPIC2);
        assertFalse(item1.isAlive());
        assertFalse(item2.isAlive());
        assertEquals(0, getInventory().size());
    }

    /**
     * Tests exception cases with destroy(topic).
     */
    public void testDestroyString_Ex() {
        // null topic
        assertThatIllegalArgumentException().as("null topic").isThrownBy(() -> destroyTopic(null));

        // empty topic
        assertThatIllegalArgumentException().as("empty topic").isThrownBy(() -> destroyTopic(""));
    }

    /**
     * Tests the destroy() method.
     */
    public void testDestroy() {
        initFactory();

        List<T> lst = buildTopics(makePropBuilder().makeTopic(MY_TOPIC).makeTopic(TOPIC2).build());

        int index = 0;
        T item1 = lst.get(index++);
        T item2 = lst.get(index++);

        item1.start();
        item2.start();

        destroyFactory();

        assertFalse(item1.isAlive());
        assertFalse(item2.isAlive());
        assertEquals(0, getInventory().size());
    }

    /**
     * Tests exception cases with get(topic).
     */
    public void testGet_Ex() {
        // null topic
        assertThatIllegalArgumentException().as("null topic").isThrownBy(() -> getTopic(null));

        // empty topic
        assertThatIllegalArgumentException().as("empty topic").isThrownBy(() -> getTopic(""));

        // unknown topic
        initFactory();
        buildTopics(makePropBuilder().makeTopic(MY_TOPIC).build());

        assertThatIllegalStateException().as("unknown topic").isThrownBy(() -> getTopic(TOPIC2));
    }
}
