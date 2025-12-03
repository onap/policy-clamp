/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018-2020 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.common.parameters.topic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.parameters.topic.BusTopicParams.TopicParamsBuilder;

class BusTopicParamsTest {

    public static final String MY_AFT_ENV = "my-aft-env";
    public static final String MY_API_KEY = "my-api-key";
    public static final String MY_API_SECRET = "my-api-secret";
    public static final String MY_BASE_PATH = "my-base";
    public static final String MY_CLIENT_NAME = "my-client";
    public static final String MY_CONS_GROUP = "my-cons-group";
    public static final String MY_CONS_INST = "my-cons-inst";
    public static final String MY_ENV = "my-env";
    public static final int MY_FETCH_LIMIT = 100;
    public static final int MY_FETCH_TIMEOUT = 101;
    public static final String MY_HOST = "my-host";
    public static final String MY_LAT = "my-lat";
    public static final String MY_LONG = "my-long";
    public static final String MY_PARTNER = "my-partner";
    public static final String MY_PASS = "my-pass";
    public static final int MY_PORT = 102;
    public static final String MY_TOPIC = "my-topic";
    public static final String MY_EFFECTIVE_TOPIC = "my-effective-topic";
    public static final String MY_USERNAME = "my-user";
    public static final String MY_PARTITION = "my-partition";
    public static final String MY_SERIALIZER = "org.apache.kafka.common.serialization.StringSerializer";

    protected Map<String, String> addProps;
    protected TopicParamsBuilder builder;

    @BeforeEach
    public void setUp() {
        addProps = new TreeMap<>();
        addProps.put("my-key-A", "my-value-A");
        addProps.put("my-key-B", "my-value-B");

        builder = makeBuilder();
    }

    @Test
    void testGetters() {
        BusTopicParams params = makeBuilder().build();

        Assertions.assertEquals(addProps, params.getAdditionalProps());
        Assertions.assertEquals(MY_AFT_ENV, params.getAftEnvironment());
        assertTrue(params.isAllowSelfSignedCerts());
        Assertions.assertEquals(MY_API_KEY, params.getApiKey());
        Assertions.assertEquals(MY_API_SECRET, params.getApiSecret());
        Assertions.assertEquals(MY_BASE_PATH, params.getBasePath());
        Assertions.assertEquals(MY_CLIENT_NAME, params.getClientName());
        Assertions.assertEquals(MY_CONS_GROUP, params.getConsumerGroup());
        Assertions.assertEquals(MY_CONS_INST, params.getConsumerInstance());
        Assertions.assertEquals(MY_ENV, params.getEnvironment());
        Assertions.assertEquals(MY_FETCH_LIMIT, params.getFetchLimit());
        Assertions.assertEquals(MY_FETCH_TIMEOUT, params.getFetchTimeout());
        Assertions.assertEquals(MY_HOST, params.getHostname());
        Assertions.assertEquals(MY_LAT, params.getLatitude());
        Assertions.assertEquals(MY_LONG, params.getLongitude());
        assertTrue(params.isManaged());
        Assertions.assertEquals(MY_PARTITION, params.getPartitionId());
        Assertions.assertEquals(MY_PARTNER, params.getPartner());
        Assertions.assertEquals(MY_PASS, params.getPassword());
        Assertions.assertEquals(MY_PORT, params.getPort());
        Assertions.assertEquals(List.of("localhost"), params.getServers());
        Assertions.assertEquals(MY_TOPIC, params.getTopic());
        Assertions.assertEquals(MY_EFFECTIVE_TOPIC, params.getEffectiveTopic());
        assertTrue(params.isUseHttps());
        Assertions.assertEquals(MY_USERNAME, params.getUserName());
    }

    @Test
    void testBooleanGetters() {
        // ensure that booleans are independent of each other
        testBoolean("true:false:false", TopicParamsBuilder::allowSelfSignedCerts);
        testBoolean("false:true:false", TopicParamsBuilder::managed);
        testBoolean("false:false:true", TopicParamsBuilder::useHttps);
    }

    @Test
    void testValidators() {
        BusTopicParams params = makeBuilder().build();

        // test validity methods
        assertTrue(params.isAdditionalPropsValid());
        assertFalse(params.isAftEnvironmentInvalid());
        assertTrue(params.isApiKeyValid());
        assertTrue(params.isApiSecretValid());
        assertFalse(params.isClientNameInvalid());
        assertFalse(params.isConsumerGroupInvalid());
        assertFalse(params.isConsumerInstanceInvalid());
        assertFalse(params.isEnvironmentInvalid());
        assertFalse(params.isHostnameInvalid());
        assertFalse(params.isLatitudeInvalid());
        assertFalse(params.isLongitudeInvalid());
        assertFalse(params.isPartitionIdInvalid());
        assertFalse(params.isPartnerInvalid());
        assertTrue(params.isPasswordValid());
        assertFalse(params.isPortInvalid());
        assertFalse(params.isServersInvalid());
        assertFalse(params.isTopicInvalid());
        assertTrue(params.isUserNameValid());
    }

    @Test
    void testInvertedValidators() {
        Assertions.assertFalse(makeBuilder().additionalProps(null).build().isAdditionalPropsValid());
        Assertions.assertTrue(makeBuilder().aftEnvironment("").build().isAftEnvironmentInvalid());
        Assertions.assertFalse(makeBuilder().apiKey("").build().isApiKeyValid());
        Assertions.assertFalse(makeBuilder().apiSecret("").build().isApiSecretValid());
        Assertions.assertTrue(makeBuilder().clientName("").build().isClientNameInvalid());
        Assertions.assertTrue(makeBuilder().consumerGroup("").build().isConsumerGroupInvalid());
        Assertions.assertTrue(makeBuilder().consumerInstance("").build().isConsumerInstanceInvalid());
        Assertions.assertTrue(makeBuilder().environment("").build().isEnvironmentInvalid());
        Assertions.assertTrue(makeBuilder().hostname("").build().isHostnameInvalid());
        Assertions.assertTrue(makeBuilder().latitude("").build().isLatitudeInvalid());
        Assertions.assertTrue(makeBuilder().longitude("").build().isLongitudeInvalid());
        Assertions.assertTrue(makeBuilder().partitionId("").build().isPartitionIdInvalid());
        Assertions.assertTrue(makeBuilder().partner("").build().isPartnerInvalid());
        Assertions.assertFalse(makeBuilder().password("").build().isPasswordValid());
        Assertions.assertTrue(makeBuilder().port(-1).build().isPortInvalid());
        Assertions.assertTrue(makeBuilder().port(65536).build().isPortInvalid());
        Assertions.assertTrue(makeBuilder().servers(null).build().isServersInvalid());
        Assertions.assertTrue(makeBuilder().servers(new LinkedList<>()).build().isServersInvalid());
        Assertions.assertTrue(makeBuilder().servers(List.of("")).build().isServersInvalid());
        Assertions.assertFalse(makeBuilder().servers(List.of("one-server")).build().isServersInvalid());
        Assertions.assertTrue(makeBuilder().topic("").build().isTopicInvalid());
        Assertions.assertFalse(makeBuilder().userName("").build().isUserNameValid());
    }

    /**
     * Tests the boolean methods by applying a function, once with {@code false} and once
     * with {@code true}. Verifies that all the boolean methods return the correct
     * value by concatenating them.
     *
     * @param expectedTrue the string that is expected when {@code true} is passed to the
     *        method
     * @param function function to be applied to the builder
     */
    private void testBoolean(String expectedTrue, BiConsumer<TopicParamsBuilder, Boolean> function) {
        TopicParamsBuilder topicParamsBuilder = BusTopicParams.builder();

        // first try the "false" case
        function.accept(topicParamsBuilder, false);

        BusTopicParams params = topicParamsBuilder.build();
        assertEquals("false:false:false",
                        params.isAllowSelfSignedCerts() + ":" + params.isManaged() + ":" + params.isUseHttps());


        // now try the "true" case
        function.accept(topicParamsBuilder, true);

        params = topicParamsBuilder.build();
        assertEquals(expectedTrue,
                        params.isAllowSelfSignedCerts() + ":" + params.isManaged() + ":" + params.isUseHttps());
    }

    public TopicParamsBuilder makeBuilder() {

        return BusTopicParams.builder().additionalProps(addProps).aftEnvironment(MY_AFT_ENV).allowSelfSignedCerts(true)
            .apiKey(MY_API_KEY).apiSecret(MY_API_SECRET).basePath(MY_BASE_PATH).clientName(MY_CLIENT_NAME)
            .consumerGroup(MY_CONS_GROUP).consumerInstance(MY_CONS_INST).environment(MY_ENV)
            .fetchLimit(MY_FETCH_LIMIT).fetchTimeout(MY_FETCH_TIMEOUT).hostname(MY_HOST).latitude(MY_LAT)
            .longitude(MY_LONG).managed(true).partitionId(MY_PARTITION).partner(MY_PARTNER)
            .password(MY_PASS).port(MY_PORT).servers(List.of("localhost")).topic(MY_TOPIC)
            .effectiveTopic(MY_EFFECTIVE_TOPIC).useHttps(true).allowTracing(true).userName(MY_USERNAME)
            .serializationProvider(MY_SERIALIZER);
    }
}
