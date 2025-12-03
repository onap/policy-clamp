/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2018-2021 AT&T Intellectual Property. All rights reserved.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.onap.policy.common.message.bus.event.Topic.CommInfrastructure;
import org.onap.policy.common.message.bus.event.TopicListener;
import org.onap.policy.common.parameters.topic.BusTopicParams;
import org.onap.policy.common.utils.gson.GsonTestUtils;
import org.onap.policy.common.utils.network.NetworkUtil;

class SingleThreadedBusTopicSourceTest extends TopicTestBase {
    private Thread thread;
    private BusConsumer cons;
    private TopicListener listener;
    private SingleThreadedBusTopicSourceImpl source;

    /**
     * Creates the object to be tested, as well as various mocks.
     */
    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();

        thread = mock(Thread.class);
        cons = mock(BusConsumer.class);
        listener = mock(TopicListener.class);
        source = new SingleThreadedBusTopicSourceImpl(makeBuilder().build());
    }

    @AfterEach
    public void tearDown() {
        source.shutdown();
    }

    @Test
    void testSerialize() {
        assertThatCode(() -> new GsonTestUtils().compareGson(source, SingleThreadedBusTopicSourceTest.class))
                        .doesNotThrowAnyException();
    }

    @Test
    void testRegister() {
        source.register(listener);
        assertEquals(1, source.initCount);
        source.offer(MY_MESSAGE);
        verify(listener).onTopicEvent(CommInfrastructure.NOOP, MY_TOPIC, MY_MESSAGE);

        // register another - should not re-init
        TopicListener listener2 = mock(TopicListener.class);
        source.register(listener2);
        assertEquals(1, source.initCount);
        source.offer(MY_MESSAGE + "z");
        verify(listener).onTopicEvent(CommInfrastructure.NOOP, MY_TOPIC, MY_MESSAGE + "z");
        verify(listener2).onTopicEvent(CommInfrastructure.NOOP, MY_TOPIC, MY_MESSAGE + "z");

        // re-register - should not re-init
        source.register(listener);
        assertEquals(1, source.initCount);
        source.offer(MY_MESSAGE2);
        verify(listener).onTopicEvent(CommInfrastructure.NOOP, MY_TOPIC, MY_MESSAGE2);

        // lock & register - should not init
        source = new SingleThreadedBusTopicSourceImpl(makeBuilder().build());
        source.lock();
        source.register(listener);
        assertEquals(0, source.initCount);

        // exception during init
        source = new SingleThreadedBusTopicSourceImpl(makeBuilder().build());
        source.initEx = true;
        source.register(listener);
    }

    @Test
    void testUnregister() {
        TopicListener listener2 = mock(TopicListener.class);
        source.register(listener);
        source.register(listener2);

        // unregister first listener - should NOT invoke close
        source.unregister(listener);
        verify(cons, never()).close();
        assertEquals(Arrays.asList(listener2), source.snapshotTopicListeners());

        // unregister same listener - should not invoke close
        source.unregister(listener);
        verify(cons, never()).close();
        assertEquals(Arrays.asList(listener2), source.snapshotTopicListeners());

        // unregister second listener - SHOULD invoke close
        source.unregister(listener2);
        verify(cons).close();
        assertTrue(source.snapshotTopicListeners().isEmpty());

        // unregister same listener - should not invoke close again
        source.unregister(listener2);
        verify(cons).close();
        assertTrue(source.snapshotTopicListeners().isEmpty());
    }

    @Test
    void testToString() {
        assertTrue(source.toString().startsWith("SingleThreadedBusTopicSource ["));
    }

    @Test
    void testMakePollerThread() {
        SingleThreadedBusTopicSource source2 = new SingleThreadedBusTopicSource(makeBuilder().build()) {
            @Override
            public CommInfrastructure getTopicCommInfrastructure() {
                return CommInfrastructure.NOOP;
            }

            @Override
            public void init() throws MalformedURLException {
                // do nothing
            }
        };

        assertNotNull(source2.makePollerThread());
    }

    @Test
    void testSingleThreadedBusTopicSource() {
        // Note: if the value contains "-", it's probably a UUID

        // verify that different wrappers can be built
        source = new SingleThreadedBusTopicSourceImpl(makeBuilder().build());
        assertThat(source.getConsumerGroup()).isEqualTo(MY_CONS_GROUP);
        assertThat(source.getConsumerInstance()).isEqualTo(MY_CONS_INST);

        // group is null => group is UUID, instance is as provided
        source = new SingleThreadedBusTopicSourceImpl(makeBuilder().consumerGroup(null).build());
        assertThat(source.getConsumerGroup()).contains("-").isNotEqualTo(NetworkUtil.getHostname());
        assertThat(source.getConsumerInstance()).isEqualTo(MY_CONS_INST);

        // instance is null => group is as provided, instance is UUID
        source = new SingleThreadedBusTopicSourceImpl(makeBuilder().consumerInstance(null).build());
        assertThat(source.getConsumerGroup()).isEqualTo(MY_CONS_GROUP);
        assertThat(source.getConsumerInstance()).contains("-").isNotEqualTo(NetworkUtil.getHostname());

        // group & instance are null => group is UUID, instance is hostname
        source = new SingleThreadedBusTopicSourceImpl(makeBuilder().consumerGroup(null).consumerInstance(null).build());
        assertThat(source.getConsumerGroup()).contains("-").isNotEqualTo(NetworkUtil.getHostname());
        assertThat(source.getConsumerInstance()).isEqualTo(NetworkUtil.getHostname());

        assertThatCode(() -> new SingleThreadedBusTopicSourceImpl(
                        makeBuilder().fetchLimit(-1).fetchTimeout(-1).build())).doesNotThrowAnyException();
    }

    @Test
    void testStart() {
        source.start();
        assertTrue(source.isAlive());
        assertEquals(1, source.initCount);
        verify(thread).start();

        // attempt to start again - nothing should be invoked again
        source.start();
        assertTrue(source.isAlive());
        assertEquals(1, source.initCount);
        verify(thread).start();

        // stop & re-start
        source.stop();
        source.start();
        assertTrue(source.isAlive());
        assertEquals(2, source.initCount);
        verify(thread, times(2)).start();
    }

    @Test
    void testStart_Locked() {
        source.lock();
        assertThatThrownBy(() -> source.start()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testStart_InitEx() {
        assertThatThrownBy(() -> {
            source.initEx = true;

            source.start();
        }).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testStop() {
        source.start();
        source.stop();
        verify(cons).close();

        // stop it again - not re-closed
        source.stop();
        verify(cons).close();

        // start & stop again, but with an exception
        doThrow(new RuntimeException(EXPECTED)).when(cons).close();
        source.start();
        source.stop();
    }

    @Test
    void testRun() throws Exception {
        source.register(listener);

        /*
         * Die in the middle of fetching messages. Also, throw an exception during the
         * first fetch attempt.
         */
        when(cons.fetch()).thenAnswer(new Answer<Iterable<String>>() {
            int count = 0;

            @Override
            public Iterable<String> answer(InvocationOnMock invocation) throws Throwable {
                if (++count > 1) {
                    source.alive = false;
                    return Arrays.asList(MY_MESSAGE, MY_MESSAGE2);

                } else {
                    throw new IOException(EXPECTED);
                }
            }
        });
        source.alive = true;
        source.run();
        assertEquals(Arrays.asList(MY_MESSAGE), Arrays.asList(source.getRecentEvents()));
        verify(listener).onTopicEvent(CommInfrastructure.NOOP, MY_TOPIC, MY_MESSAGE);
        verify(listener, never()).onTopicEvent(CommInfrastructure.NOOP, MY_TOPIC, MY_MESSAGE2);

        /*
         * Die AFTER fetching messages.
         */
        final String msga = "message-A";
        final String msgb = "message-B";
        when(cons.fetch()).thenAnswer(new Answer<Iterable<String>>() {
            int count = 0;

            @Override
            public Iterable<String> answer(InvocationOnMock invocation) throws Throwable {
                if (++count > 1) {
                    source.alive = false;
                    return Collections.emptyList();

                } else {
                    return Arrays.asList(msga, msgb);
                }
            }
        });
        source.alive = true;
        source.run();
        verify(listener).onTopicEvent(CommInfrastructure.NOOP, MY_TOPIC, msga);
        verify(listener).onTopicEvent(CommInfrastructure.NOOP, MY_TOPIC, msgb);

        assertEquals(Arrays.asList(MY_MESSAGE, msga, msgb), Arrays.asList(source.getRecentEvents()));
    }

    @Test
    void testOffer() {
        source.register(listener);
        source.offer(MY_MESSAGE);
        verify(listener).onTopicEvent(CommInfrastructure.NOOP, MY_TOPIC, MY_MESSAGE);
        assertEquals(Arrays.asList(MY_MESSAGE), Arrays.asList(source.getRecentEvents()));
    }

    @Test
    void testOffer_NotStarted() {
        assertThatThrownBy(() -> source.offer(MY_MESSAGE)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testGetConsumerGroup() {
        assertEquals(MY_CONS_GROUP, source.getConsumerGroup());
    }

    @Test
    void testGetConsumerInstance() {
        assertEquals(MY_CONS_INST, source.getConsumerInstance());
    }

    @Test
    void testShutdown() {
        source.register(listener);

        source.shutdown();
        verify(cons).close();
        assertTrue(source.snapshotTopicListeners().isEmpty());
    }

    @Test
    void testGetFetchTimeout() {
        assertEquals(MY_FETCH_TIMEOUT, source.getFetchTimeout());
    }

    @Test
    void testGetFetchLimit() {
        assertEquals(MY_FETCH_LIMIT, source.getFetchLimit());
    }

    /**
     * Implementation of SingleThreadedBusTopicSource that counts the number of times
     * init() is invoked.
     */
    private class SingleThreadedBusTopicSourceImpl extends SingleThreadedBusTopicSource {

        private int initCount = 0;
        private boolean initEx = false;

        public SingleThreadedBusTopicSourceImpl(BusTopicParams busTopicParams) {
            super(busTopicParams);
        }

        @Override
        public CommInfrastructure getTopicCommInfrastructure() {
            return CommInfrastructure.NOOP;
        }

        @Override
        public void init() throws MalformedURLException {
            ++initCount;

            if (initEx) {
                throw new MalformedURLException(EXPECTED);
            }

            consumer = cons;
        }

        @Override
        protected Thread makePollerThread() {
            return thread;
        }

    }
}
