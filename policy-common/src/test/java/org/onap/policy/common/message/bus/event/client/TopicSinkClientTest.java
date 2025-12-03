/*-
 * ============LICENSE_START=======================================================
 * ONAP PAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2019, 2024 Nordix Foundation.
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

package org.onap.policy.common.message.bus.event.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.message.bus.event.TopicEndpointManager;
import org.onap.policy.common.message.bus.event.TopicSink;

class TopicSinkClientTest {
    private static final String TOPIC = "my-topic";

    private TopicSinkClient client;
    private TopicSink sink;
    private List<TopicSink> sinks;

    /**
     * Creates mocks and an initial client object.
     *
     * @throws Exception if an error occurs
     */
    @BeforeEach
    public void setUp() throws Exception {
        sink = mock(TopicSink.class);
        when(sink.send(anyString())).thenReturn(true);

        sinks = Arrays.asList(sink, null);

        client = new TopicSinkClient2(TOPIC);

        Properties props = new Properties();
        props.setProperty("noop.sink.topics", TOPIC);

        // clear all topics and then configure one topic
        TopicEndpointManager.getManager().shutdown();
        TopicEndpointManager.getManager().addTopicSinks(props);
    }

    @AfterAll
    public static void tearDown() {
        // clear all topics after the tests
        TopicEndpointManager.getManager().shutdown();
    }

    /**
     * Uses a real NO-OP topic sink.
     */
    @Test
    void testGetTopicSinks() throws Exception {

        sink = TopicEndpointManager.getManager().getNoopTopicSink(TOPIC);
        assertNotNull(sink);

        final AtomicReference<String> evref = new AtomicReference<>(null);

        sink.register((infra, topic, event) -> evref.set(event));
        sink.start();

        client = new TopicSinkClient(TOPIC);
        client.send(100);

        assertEquals("100", evref.get());
    }

    @Test
    void testTopicSinkClient() {
        // unknown topic -> should throw exception
        sinks = new LinkedList<>();
        assertThatThrownBy(() -> new TopicSinkClient2(TOPIC)).isInstanceOf(TopicSinkClientException.class)
            .hasMessage("no sinks for topic: my-topic");
    }

    @Test
    void testTopicSinkClient_GetTopic() throws TopicSinkClientException {
        assertEquals(TOPIC, new TopicSinkClient(TopicEndpointManager.getManager().getNoopTopicSink(TOPIC)).getTopic());
        assertEquals(TOPIC, new TopicSinkClient(TOPIC).getTopic());

        assertThatThrownBy(() -> new TopicSinkClient((TopicSink) null))
            .hasMessageContaining("sink is marked non-null but is null");
        assertThatThrownBy(() -> new TopicSinkClient("blah")).isInstanceOf(TopicSinkClientException.class)
            .hasMessage("no sinks for topic: blah");
    }

    @Test
    void testSend() {
        client.send(Arrays.asList("abc", "def"));
        verify(sink).send("['abc','def']".replace('\'', '"'));

        // sink send fails
        when(sink.send(anyString())).thenReturn(false);
        assertFalse(client.send("ghi"));

        // sink send throws an exception
        final RuntimeException ex = new RuntimeException("expected exception");
        when(sink.send(anyString())).thenThrow(ex);
        assertFalse(client.send("jkl"));
    }

    /**
     * TopicSinkClient with some overrides.
     */
    private class TopicSinkClient2 extends TopicSinkClient {

        public TopicSinkClient2(final String topic) throws TopicSinkClientException {
            super(topic);
        }

        @Override
        protected List<TopicSink> getTopicSinks(final String topic) {
            return sinks;
        }
    }
}
