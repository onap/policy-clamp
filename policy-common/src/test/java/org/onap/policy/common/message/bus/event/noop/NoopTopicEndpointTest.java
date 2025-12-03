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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.message.bus.event.Topic.CommInfrastructure;
import org.onap.policy.common.message.bus.event.TopicListener;
import org.onap.policy.common.message.bus.event.base.TopicTestBase;

public abstract class NoopTopicEndpointTest<F extends NoopTopicFactory<T>, T extends NoopTopicEndpoint>
    extends TopicTestBase {

    protected final F factory;
    protected T endpoint;

    public NoopTopicEndpointTest(F factory) {
        this.factory = factory;
    }

    protected abstract boolean io(String message);

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        this.endpoint = this.factory.build(servers, MY_TOPIC);
    }

    @Test
    void testIo() {
        TopicListener listener = mock(TopicListener.class);
        this.endpoint.register(listener);
        this.endpoint.start();

        assertTrue(io(MY_MESSAGE));
        assertSame(MY_MESSAGE, this.endpoint.getRecentEvents()[0]);
        assertEquals(Collections.singletonList(MY_MESSAGE), Arrays.asList(this.endpoint.getRecentEvents()));
        verify(listener).onTopicEvent(CommInfrastructure.NOOP, MY_TOPIC, MY_MESSAGE);

        this.endpoint.unregister(listener);
    }

    @Test
    void testIoNullMessage() {
        assertThatThrownBy(() -> io(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testIoEmptyMessage() {
        assertThatThrownBy(() -> io("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testOfferNotStarted() {
        assertThatThrownBy(() -> io(MY_MESSAGE)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testGetTopicCommInfrastructure() {
        assertEquals(CommInfrastructure.NOOP, this.endpoint.getTopicCommInfrastructure());
    }

    @Test
    void testStart_testStop_testShutdown() {
        this.endpoint.start();
        assertTrue(this.endpoint.isAlive());

        // start again
        this.endpoint.start();
        assertTrue(this.endpoint.isAlive());

        // stop
        this.endpoint.stop();
        assertFalse(this.endpoint.isAlive());

        // re-start again
        this.endpoint.start();
        assertTrue(this.endpoint.isAlive());

        // shutdown
        this.endpoint.shutdown();
        assertFalse(this.endpoint.isAlive());
    }

    @Test
    void testStart_Locked() {
        this.endpoint.lock();
        assertThatThrownBy(() -> this.endpoint.start()).isInstanceOf(IllegalStateException.class);
    }

}
