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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.onap.policy.common.message.bus.properties.MessageBusProperties.PROPERTY_ALLOW_SELF_SIGNED_CERTIFICATES_SUFFIX;
import static org.onap.policy.common.message.bus.properties.MessageBusProperties.PROPERTY_HTTP_HTTPS_SUFFIX;
import static org.onap.policy.common.message.bus.properties.MessageBusProperties.PROPERTY_MANAGED_SUFFIX;
import static org.onap.policy.common.message.bus.properties.MessageBusProperties.PROPERTY_TOPIC_EFFECTIVE_TOPIC_SUFFIX;

import java.util.List;
import java.util.Properties;
import java.util.function.Predicate;
import org.onap.policy.common.message.bus.event.Topic;
import org.onap.policy.common.parameters.topic.BusTopicParams;

/**
 * Base class for Topic Factory tests that use BusTopicParams.
 *
 * @param <T> type of topic managed by the factory
 */
public abstract class BusTopicFactoryTestBase<T extends Topic> extends TopicFactoryTestBase<T> {

    /**
     * Builds a topic.
     *
     * @param params the parameters used to configure the topic
     * @return a new topic
     */
    protected abstract T buildTopic(BusTopicParams params);

    /**
     * Builds a topic.
     *
     * @param servers list of servers
     * @param topic the topic name
     * @return a new topic
     */
    protected abstract T buildTopic(List<String> servers, String topic);

    /**
     * Gets the parameters used to build the most recent topic.
     *
     * @return the most recent topic's parameters
     */
    protected abstract BusTopicParams getLastParams();

    /**
     * Tests building a topic using BusTopicParams.
     */
    public void testBuildBusTopicParams() {
        initFactory();

        // two unmanaged topics
        T item = buildTopic(makeBuilder().managed(false).effectiveTopic(null).build());
        T item2 = buildTopic(makeBuilder().managed(false).topic(TOPIC2).build());
        assertNotNull(item);
        assertNotNull(item2);
        assertEquals(item.getTopic(), item.getEffectiveTopic());
        assertNotEquals(item2.getTopic(), item2.getEffectiveTopic());
        assertNotSame(item, item2);

        // duplicate topics, but since they aren't managed, they should be different
        T item3 = buildTopic(makeBuilder().managed(false).build());
        T item4 = buildTopic(makeBuilder().managed(false).effectiveTopic(TOPIC2).build());
        assertNotNull(item3);
        assertNotNull(item4);
        assertEquals(MY_TOPIC, item4.getTopic());
        assertEquals(TOPIC2, item4.getEffectiveTopic());
        assertNotSame(item, item3);
        assertNotSame(item, item4);
        assertNotSame(item3, item4);

        // two managed topics
        T item5 = buildTopic(makeBuilder().build());
        T item6 = buildTopic(makeBuilder().topic(TOPIC2).build());
        assertNotNull(item5);
        assertNotNull(item6);

        // re-build same managed topics - should get exact same objects
        assertSame(item5, buildTopic(makeBuilder().topic(MY_TOPIC).build()));
        assertSame(item6, buildTopic(makeBuilder().topic(TOPIC2).build()));
    }

    /**
     * Tests exception cases when building a topic using BusTopicParams.
     */
    public void testBuildBusTopicParams_Ex() {
        // null topic
        assertThatIllegalArgumentException().isThrownBy(() -> buildTopic(makeBuilder().topic(null).build()));

        // empty topic
        assertThatIllegalArgumentException().isThrownBy(() -> buildTopic(makeBuilder().topic("").build()));
    }

    /**
     * Tests building a topic using a list of servers and a topic.
     */
    public void testBuildListOfStringString() {
        initFactory();

        T item1 = buildTopic(servers, MY_TOPIC);
        assertNotNull(item1);

        // check parameters that were used
        BusTopicParams params = getLastParams();
        assertEquals(servers, params.getServers());
        assertEquals(MY_TOPIC, params.getTopic());
        assertTrue(params.isManaged());
        assertFalse(params.isUseHttps());

        T item2 = buildTopic(servers, TOPIC2);
        assertNotNull(item2);
        assertNotSame(item1, item2);

        // duplicate - should be the same, as these topics are managed
        T item3 = buildTopic(servers, TOPIC2);
        assertSame(item2, item3);
    }

    /**
     * Tests building a topic using Properties. Verifies parameters specific to Bus
     * topics.
     */
    public void testBuildProperties() {
        initFactory();

        List<T> topics = buildTopics(makePropBuilder().makeTopic(MY_TOPIC).build());
        assertEquals(1, topics.size());
        assertEquals(MY_TOPIC, topics.get(0).getTopic());
        assertEquals(MY_EFFECTIVE_TOPIC, topics.get(0).getEffectiveTopic());

        BusTopicParams params = getLastParams();
        assertTrue(params.isManaged());
        assertTrue(params.isUseHttps());
        assertTrue(params.isAllowSelfSignedCerts());
        assertEquals(MY_API_KEY, params.getApiKey());
        assertEquals(MY_API_SECRET, params.getApiSecret());
        assertEquals(List.of(SERVER), params.getServers());
        assertEquals(MY_TOPIC, params.getTopic());
        assertEquals(MY_EFFECTIVE_TOPIC, params.getEffectiveTopic());

        List<T> topics2 = buildTopics(makePropBuilder().makeTopic(TOPIC3)
            .removeTopicProperty(PROPERTY_TOPIC_EFFECTIVE_TOPIC_SUFFIX).build());
        assertEquals(1, topics2.size());
        assertEquals(TOPIC3, topics2.get(0).getTopic());
        assertEquals(topics2.get(0).getTopic(), topics2.get(0).getEffectiveTopic());
    }

    @Override
    void testBuildProperties_Variations() {
        super.testBuildProperties_Variations();

        // check boolean properties that default to true
        checkDefault(PROPERTY_MANAGED_SUFFIX, BusTopicParams::isManaged);

        // check boolean properties that default to false
        checkDefault(PROPERTY_HTTP_HTTPS_SUFFIX, params -> !params.isUseHttps());
        checkDefault(PROPERTY_ALLOW_SELF_SIGNED_CERTIFICATES_SUFFIX, params -> !params.isAllowSelfSignedCerts());
    }

    /**
     * Verifies that a parameter has the correct default, if the original builder property
     * is not provided.
     *
     * @param builderName name of the builder property
     * @param validate function to test the validity of the property
     * @param values the values to which the property should be set, defaults to
     *        {@code null} and ""
     */
    protected void checkDefault(String builderName, Predicate<BusTopicParams> validate, Object... values) {
        Object[] values2 = (values.length > 0 ? values : new Object[] {null, ""});

        for (Object value : values2) {
            // always start with a fresh factory
            initFactory();

            TopicPropertyBuilder builder = makePropBuilder().makeTopic(MY_TOPIC);

            if (value == null) {
                builder.removeTopicProperty(builderName);

            } else {
                builder.setTopicProperty(builderName, value.toString());
            }

            assertEquals(1, buildTopics(builder.build()).size(), "size for default " + value);
            assertTrue(validate.test(getLastParams()), "default for " + value);
        }
    }

    /**
     * Verifies that an "additional" property does not exist, if the original builder
     * property is not provided.
     *
     * @param builderName name of the builder property
     * @param addName name of the "additional" property
     */
    public void expectNullAddProp(String builderName, String addName) {

        // remove the property
        initFactory();
        Properties props = makePropBuilder().makeTopic(MY_TOPIC).removeTopicProperty(builderName).build();
        assertEquals(1, buildTopics(props).size());
        assertFalse(getLastParams().getAdditionalProps().containsKey(addName));


        // repeat, this time using an empty string instead of null
        initFactory();
        props = makePropBuilder().makeTopic(MY_TOPIC).setTopicProperty(builderName, "").build();
        assertEquals(1, buildTopics(props).size());
        assertFalse(getLastParams().getAdditionalProps().containsKey(addName));
    }
}
