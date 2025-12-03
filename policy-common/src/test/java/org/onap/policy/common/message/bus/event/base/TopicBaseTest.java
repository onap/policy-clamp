/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.message.bus.event.Topic.CommInfrastructure;
import org.onap.policy.common.message.bus.event.TopicListener;
import org.onap.policy.common.utils.gson.GsonTestUtils;

class TopicBaseTest extends TopicTestBase {

    private TopicBaseImpl base;

    /**
     * Creates the object to be tested.
     */
    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();

        base = new TopicBaseImpl(servers, MY_TOPIC);
    }

    @Test
    void testTopicBase_NullServers() {
        assertThatThrownBy(() -> new TopicBaseImpl(null, MY_TOPIC)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testTopicBase_EmptyServers() {
        List<String> testList = Collections.emptyList();
        assertThatThrownBy(() -> new TopicBaseImpl(testList, MY_TOPIC))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testTopicBase_NullTopic() {
        assertThatThrownBy(() -> new TopicBaseImpl(servers, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testTopicBase_EmptyTopic() {
        assertThatThrownBy(() -> new TopicBaseImpl(servers, "")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testTopicBase_EffectiveTopic() {
        TopicBase baseEf = new TopicBaseImpl(servers, MY_TOPIC, MY_EFFECTIVE_TOPIC);
        assertEquals(MY_TOPIC, baseEf.getTopic());
        assertEquals(MY_EFFECTIVE_TOPIC, baseEf.getEffectiveTopic());
    }

    @Test
    void testTopicBase_NullEffectiveTopic() {
        TopicBase baseEf = new TopicBaseImpl(servers, MY_TOPIC, null);
        assertEquals(MY_TOPIC, baseEf.getTopic());
        assertEquals(MY_TOPIC, baseEf.getEffectiveTopic());
    }

    @Test
    void testTopicBase_EmptyEffectiveTopic() {
        TopicBase baseEf = new TopicBaseImpl(servers, MY_TOPIC, "");
        assertEquals(MY_TOPIC, baseEf.getTopic());
        assertEquals(MY_TOPIC, baseEf.getEffectiveTopic());
    }

    @Test
    void testSerialize() {
        assertThatCode(() -> new GsonTestUtils().compareGson(base, TopicBaseTest.class)).doesNotThrowAnyException();
    }

    @Test
    void testRegister() {
        TopicListener listener = mock(TopicListener.class);
        base.register(listener);
        assertEquals(List.of(listener), base.snapshotTopicListeners());

        // re-register - list should be unchanged
        base.register(listener);
        assertEquals(List.of(listener), base.snapshotTopicListeners());

        // register a new listener
        TopicListener listener2 = mock(TopicListener.class);
        base.register(listener2);
        assertEquals(List.of(listener, listener2), base.snapshotTopicListeners());
    }

    @Test
    void testRegister_NullListener() {
        assertThatThrownBy(() -> base.register(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testUnregister() {
        // register two listeners
        TopicListener listener = mock(TopicListener.class);
        TopicListener listener2 = mock(TopicListener.class);
        base.register(listener);
        base.register(listener2);

        // unregister one
        base.unregister(listener);
        assertEquals(List.of(listener2), base.snapshotTopicListeners());

        // unregister the other
        base.unregister(listener2);
        assertTrue(base.snapshotTopicListeners().isEmpty());

        // unregister again
        base.unregister(listener2);
        assertTrue(base.snapshotTopicListeners().isEmpty());
    }

    @Test
    void testUnregister_NullListener() {
        base.register(mock(TopicListener.class));
        assertThatThrownBy(() -> base.unregister(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testBroadcast() {
        // register two listeners
        TopicListener listener = mock(TopicListener.class);
        TopicListener listener2 = mock(TopicListener.class);
        base.register(listener);
        base.register(listener2);

        // broadcast a message
        final String msg1 = "message-A";
        base.broadcast(msg1);
        verify(listener).onTopicEvent(CommInfrastructure.NOOP, MY_TOPIC, msg1);
        verify(listener2).onTopicEvent(CommInfrastructure.NOOP, MY_TOPIC, msg1);

        // broadcast another message, with an exception
        final String msg2 = "message-B";
        doThrow(new RuntimeException(EXPECTED)).when(listener).onTopicEvent(any(), any(), any());
        base.broadcast(msg2);
        verify(listener).onTopicEvent(CommInfrastructure.NOOP, MY_TOPIC, msg2);
        verify(listener2).onTopicEvent(CommInfrastructure.NOOP, MY_TOPIC, msg2);
    }

    @Test
    void testLock_testUnlock() {
        assertFalse(base.isLocked());
        assertTrue(base.lock());
        assertEquals(0, base.startCount);
        assertEquals(1, base.stopCount);

        // lock again - should not stop again
        assertTrue(base.isLocked());
        assertTrue(base.lock());
        assertEquals(0, base.startCount);
        assertEquals(1, base.stopCount);

        assertTrue(base.isLocked());
        assertTrue(base.unlock());
        assertEquals(1, base.startCount);
        assertEquals(1, base.stopCount);

        // unlock again - should not start again
        assertFalse(base.isLocked());
        assertTrue(base.unlock());
        assertEquals(1, base.startCount);
        assertEquals(1, base.stopCount);
    }

    /**
     * Tests lock/unlock when the stop/start methods return false.
     */
    @Test
    void testLock_testUnlock_FalseReturns() {

        // lock, but stop returns false
        base.stopReturn = false;
        assertFalse(base.lock());
        assertTrue(base.isLocked());
        assertTrue(base.lock());

        // unlock, but start returns false
        base.startReturn = false;
        assertFalse(base.unlock());
        assertFalse(base.isLocked());
        assertTrue(base.unlock());
    }

    /**
     * Tests lock/unlock when the start method throws an exception.
     */
    @Test
    void testLock_testUnlock_Exception() {

        // lock & re-lock, but start throws an exception
        base.startEx = true;
        assertTrue(base.lock());
        assertFalse(base.unlock());
        assertFalse(base.isLocked());
        assertTrue(base.unlock());
    }

    @Test
    void testIsLocked() {
        assertFalse(base.isLocked());
        base.lock();
        assertTrue(base.isLocked());
        base.unlock();
        assertFalse(base.isLocked());
    }

    @Test
    void testGetTopic() {
        assertEquals(MY_TOPIC, base.getTopic());
    }

    @Test
    void testGetEffectiveTopic() {
        assertEquals(MY_TOPIC, base.getTopic());
        assertEquals(MY_TOPIC, base.getEffectiveTopic());
    }

    @Test
    void testIsAlive() {
        assertFalse(base.isAlive());
        base.start();
        assertTrue(base.isAlive());
        base.stop();
        assertFalse(base.isAlive());
    }

    @Test
    void testGetServers() {
        assertEquals(servers, base.getServers());
    }

    @Test
    void testGetRecentEvents() {
        assertEquals(0, base.getRecentEvents().length);

        base.addEvent("recent-A");
        base.addEvent("recent-B");

        String[] recent = base.getRecentEvents();
        assertEquals(2, recent.length);
        assertEquals("recent-A", recent[0]);
        assertEquals("recent-B", recent[1]);
    }

    @Test
    void testToString() {
        assertNotNull(base.toString());
    }

    /**
     * Implementation of TopicBase.
     */
    private static class TopicBaseImpl extends TopicBase {
        private int startCount = 0;
        private int stopCount = 0;
        private boolean startReturn = true;
        private boolean stopReturn = true;
        private boolean startEx = false;

        /**
         * Constructor.
         *
         * @param servers list of servers
         * @param topic topic name
         */
        public TopicBaseImpl(List<String> servers, String topic) {
            super(servers, topic);
        }

        /**
         * Constructor.
         *
         * @param servers list of servers
         * @param topic topic name
         * @param effectiveTopic effective topic name for network communication
         */
        public TopicBaseImpl(List<String> servers, String topic, String effectiveTopic) {
            super(servers, topic, effectiveTopic);
        }

        @Override
        public CommInfrastructure getTopicCommInfrastructure() {
            return CommInfrastructure.NOOP;
        }

        @Override
        public boolean start() {
            ++startCount;

            if (startEx) {
                throw new RuntimeException(EXPECTED);
            }

            alive = true;
            return startReturn;
        }

        @Override
        public boolean stop() {
            ++stopCount;
            alive = false;
            return stopReturn;
        }

        @Override
        public void shutdown() {
            // do nothing
        }

        /**
         * Adds an event to the list of recent events.
         *
         * @param event event to be added
         */
        public void addEvent(String event) {
            recentEvents.add(event);
        }
    }
}
