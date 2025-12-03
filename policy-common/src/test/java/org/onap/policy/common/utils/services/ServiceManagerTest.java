/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.common.utils.services;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.LinkedList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.capabilities.Startable;
import org.onap.policy.common.utils.services.ServiceManager.RunnableWithEx;

class ServiceManagerTest {
    private static final String MY_NAME = "my-name";
    private static final String ALREADY_RUNNING = MY_NAME + " is already running";
    private static final String EXPECTED_EXCEPTION = "expected exception";

    private ServiceManager svcmgr;

    /**
     * Initializes {@link #svcmgr}.
     */
    @BeforeEach
    public void setUp() {
        svcmgr = new ServiceManager(MY_NAME);
    }

    @Test
    void testServiceName() {
        assertEquals("service manager", new ServiceManager().getName());
    }

    @Test
    void testGetName() {
        assertEquals(MY_NAME, svcmgr.getName());
    }

    @Test
    void testAddAction() throws Exception {
        RunnableWithEx start1 = mock(RunnableWithEx.class);
        RunnableWithEx stop1 = mock(RunnableWithEx.class);
        svcmgr.addAction("first action", start1, stop1);

        RunnableWithEx start2 = mock(RunnableWithEx.class);
        RunnableWithEx stop2 = mock(RunnableWithEx.class);
        svcmgr.addAction("second action", start2, stop2);

        svcmgr.start();
        verify(start1).run();
        verify(start2).run();
        verify(stop1, never()).run();
        verify(stop2, never()).run();

        // cannot add while running
        assertThatIllegalStateException().isThrownBy(() -> svcmgr.addAction("fail action", start1, stop1))
                        .withMessage(ALREADY_RUNNING + "; cannot add fail action");

        svcmgr.stop();
        verify(start1).run();
        verify(start2).run();
        verify(stop1).run();
        verify(stop2).run();
    }

    @Test
    void testAddStartable() {
        Startable start1 = mock(Startable.class);
        svcmgr.addService("first startable", start1);

        Startable start2 = mock(Startable.class);
        svcmgr.addService("second startable", start2);

        svcmgr.start();
        verify(start1).start();
        verify(start1, never()).stop();
        verify(start2).start();
        verify(start2, never()).stop();

        // cannot add while running
        assertThatIllegalStateException().isThrownBy(() -> svcmgr.addService("fail startable", start1))
                        .withMessage(ALREADY_RUNNING + "; cannot add fail startable");

        svcmgr.stop();
        verify(start1).start();
        verify(start1).stop();
        verify(start2).start();
        verify(start2).stop();
    }

    @Test
    void testStart() {
        Startable start1 = mock(Startable.class);
        svcmgr.addService("test start", start1);

        assertTrue(svcmgr.start());

        assertTrue(svcmgr.isAlive());
        verify(start1).start();
        verify(start1, never()).stop();

        // cannot re-start
        assertThatIllegalStateException().isThrownBy(() -> svcmgr.start()).withMessage(ALREADY_RUNNING);

        // verify that it didn't try to start the service again
        verify(start1).start();

        // still running
        assertTrue(svcmgr.isAlive());
    }

    @Test
    void testStart_Ex() {
        Startable start1 = mock(Startable.class);
        svcmgr.addService("test start ex", start1);

        Startable start2 = mock(Startable.class);
        svcmgr.addService("second test start ex", start2);

        // this one will throw an exception
        Startable start3 = mock(Startable.class);
        RuntimeException exception = new RuntimeException(EXPECTED_EXCEPTION);
        when(start3.start()).thenThrow(exception);
        svcmgr.addService("third test start ex", start3);

        Startable start4 = mock(Startable.class);
        svcmgr.addService("fourth test start ex", start4);

        Startable start5 = mock(Startable.class);
        svcmgr.addService("fifth test start ex", start5);

        assertThatThrownBy(() -> svcmgr.start()).isInstanceOf(ServiceManagerException.class).hasCause(exception);

        assertFalse(svcmgr.isAlive());

        verify(start1).start();
        verify(start2).start();
        verify(start3).start();
        verify(start4, never()).start();
        verify(start5, never()).start();

        verify(start1).stop();
        verify(start2).stop();
        verify(start3, never()).stop();
        verify(start4, never()).stop();
        verify(start5, never()).stop();
    }

    @Test
    void testStart_RewindEx() {
        Startable start1 = mock(Startable.class);
        svcmgr.addService("test start rewind", start1);

        // this one will throw an exception during rewind
        Startable start2 = mock(Startable.class);
        RuntimeException exception2 = new RuntimeException(EXPECTED_EXCEPTION);
        when(start2.stop()).thenThrow(exception2);
        svcmgr.addService("second test start rewind", start2);

        // this one will throw an exception
        Startable start3 = mock(Startable.class);
        RuntimeException exception = new RuntimeException(EXPECTED_EXCEPTION);
        when(start3.start()).thenThrow(exception);
        svcmgr.addService("third test start rewind", start3);

        Startable start4 = mock(Startable.class);
        svcmgr.addService("fourth test start rewind", start4);

        Startable start5 = mock(Startable.class);
        svcmgr.addService("fifth test start rewind", start5);

        assertThatThrownBy(() -> svcmgr.start()).isInstanceOf(ServiceManagerException.class).hasCause(exception);

        assertFalse(svcmgr.isAlive());
    }

    @Test
    void testStop() {
        Startable start1 = mock(Startable.class);
        svcmgr.addService("first stop", start1);

        // cannot stop until started
        assertThatIllegalStateException().isThrownBy(() -> svcmgr.stop()).withMessage(MY_NAME + " is not running");

        // verify that it didn't try to stop the service
        verify(start1, never()).stop();

        // start it
        svcmgr.start();

        assertTrue(svcmgr.stop());

        assertFalse(svcmgr.isAlive());
        verify(start1).stop();
    }

    @Test
    void testStop_Ex() throws Exception {
        RunnableWithEx start1 = mock(RunnableWithEx.class);
        RunnableWithEx stop1 = mock(RunnableWithEx.class);
        svcmgr.addAction("first stop ex", start1, stop1);

        Startable start2 = mock(Startable.class);
        svcmgr.addService("second stop ex", start2);

        svcmgr.start();
        verify(start1).run();
        verify(stop1, never()).run();
        verify(start2).start();
        verify(start2, never()).stop();

        svcmgr.stop();
        verify(start1).run();
        verify(stop1).run();
        verify(start2).start();
        verify(start2).stop();

        assertFalse(svcmgr.isAlive());
    }

    @Test
    void testShutdown() {
        Startable start1 = mock(Startable.class);
        svcmgr.addService("first stop", start1);

        // cannot stop until started
        assertThatIllegalStateException().isThrownBy(() -> svcmgr.shutdown()).withMessage(MY_NAME + " is not running");

        // verify that it didn't try to stop the service
        verify(start1, never()).stop();

        // start it
        svcmgr.start();

        svcmgr.shutdown();

        assertFalse(svcmgr.isAlive());
        verify(start1).stop();
    }

    @Test
    void testRewind() {
        RunnableWithEx starter = mock(RunnableWithEx.class);
        LinkedList<String> lst = new LinkedList<>();

        svcmgr.addAction("first rewind", starter, () -> lst.add("rewind1"));
        svcmgr.addAction("second rewind", starter, () -> lst.add("rewind2"));

        // this one will throw an exception during rewind
        RuntimeException exception = new RuntimeException(EXPECTED_EXCEPTION);
        svcmgr.addAction("third rewind", starter, () -> {
            lst.add("rewind3");
            throw exception;
        });

        svcmgr.addAction("fourth rewind", starter, () -> lst.add("rewind4"));
        svcmgr.addAction("fifth rewind", starter, () -> lst.add("rewind5"));

        svcmgr.start();

        assertThatThrownBy(() -> svcmgr.stop()).isInstanceOf(ServiceManagerException.class).hasCause(exception);

        assertFalse(svcmgr.isAlive());

        // all of them should have been stopped, in reverse order
        assertEquals(Arrays.asList("rewind5", "rewind4", "rewind3", "rewind2", "rewind1").toString(), lst.toString());
    }

}
