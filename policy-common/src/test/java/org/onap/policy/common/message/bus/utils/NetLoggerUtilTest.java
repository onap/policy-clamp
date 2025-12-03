/*-
 * ============LICENSE_START=======================================================
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

package org.onap.policy.common.message.bus.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.message.bus.event.Topic.CommInfrastructure;
import org.onap.policy.common.message.bus.features.NetLoggerFeatureApi;
import org.onap.policy.common.message.bus.features.NetLoggerFeatureProviders;
import org.onap.policy.common.message.bus.utils.NetLoggerUtil.EventType;
import org.slf4j.Logger;

/**
 * Test class for network log utilities such as logging and feature invocation.
 */
class NetLoggerUtilTest {

    private static final String TEST_TOPIC = "test-topic";
    private static final String MESSAGE = "hello world!";
    /**
     * Test feature used for junits.
     */
    private static NetLoggerFeature netLoggerFeature;

    /**
     * Obtains the test implementation of NetLoggerFeatureApi.
     */
    @BeforeAll
    public static void setUp() {
        netLoggerFeature = (NetLoggerFeature) NetLoggerFeatureProviders.getProviders().getList().get(0);
    }

    /**
     * Clears events list and resets return/exceptions flags before invoking every unit test.
     */
    @BeforeEach
    public void reset() {
        TestAppender.clear();
        netLoggerFeature.setReturnValue(false, false);
        netLoggerFeature.setExceptions(false, false);
    }

    /**
     * Tests obtaining the network logger instance.
     */
    @Test
    void getNetworkLoggerTest() {
        assertEquals("network", NetLoggerUtil.getNetworkLogger().getName());
    }

    /**
     * Tests logging a message to the network logger and invoking features before/after logging.
     */
    @Test
    void logTest() {
        NetLoggerUtil.log(EventType.IN, CommInfrastructure.NOOP, TEST_TOPIC, MESSAGE);
        assertEquals(3, TestAppender.events.size());
    }

    /**
     * Tests that the network logger is used to log messages if a logger is not passed in.
     */
    @Test
    void logDefaultTest() {
        NetLoggerUtil.log(null, EventType.IN, CommInfrastructure.NOOP, TEST_TOPIC, MESSAGE);
        assertEquals(3, TestAppender.events.size());
        assertEquals("network", TestAppender.events.get(0).getLoggerName());
    }

    /**
     * Tests a NetLoggerFeature that replaces base implementation before logging.
     */
    @Test
    void beforeLogReturnTrueTest() {
        netLoggerFeature.setReturnValue(true, false);
        NetLoggerUtil.log(null, EventType.IN, CommInfrastructure.NOOP, TEST_TOPIC, MESSAGE);
        assertEquals(1, TestAppender.events.size());
    }

    /**
     * Tests a NetLoggerFeature that post processes a logged message.
     */
    @Test
    void afterLogReturnTrueTest() {
        netLoggerFeature.setReturnValue(false, true);
        NetLoggerUtil.log(null, EventType.IN, CommInfrastructure.NOOP, TEST_TOPIC, MESSAGE);
        assertEquals(3, TestAppender.events.size());
    }

    /**
     * Tests throwing an exception in the before hook.
     */
    @Test
    void beforeLogExceptionTest() {
        netLoggerFeature.setExceptions(true, false);
        NetLoggerUtil.log(null, EventType.IN, CommInfrastructure.NOOP, TEST_TOPIC, MESSAGE);
        assertEquals(2, TestAppender.events.size());
    }

    /**
     * Tests throwing an exception in the after hook.
     */
    @Test
    void afterLogExceptionTest() {
        netLoggerFeature.setExceptions(false, true);
        NetLoggerUtil.log(null, EventType.IN, CommInfrastructure.NOOP, TEST_TOPIC, MESSAGE);
        assertEquals(2, TestAppender.events.size());
    }

    /**
     * A custom list appender to track messages being logged to the network logger.
     * NOTE: Check src/test/resources/logback-test.xml for network logger configurations.
     */
    public static class TestAppender extends AppenderBase<ILoggingEvent> {

        /**
         * List of logged events.
         */
        private static final List<ILoggingEvent> events = new ArrayList<>();

        /**
         * Called after every unit test to clear list of events.
         */
        public static void clear() {
            events.clear();
        }

        /**
         * Appends each event to the event list.
         */
        @Override
        protected void append(ILoggingEvent event) {
            events.add(event);
        }

    }

    /**
     * Test implementation of NetLoggerFeatureApi to be used by junits.
     */
    public static class NetLoggerFeature implements NetLoggerFeatureApi {

        /**
         * Used for setting the return values of before/after hooks.
         */
        private boolean beforeReturn = false;
        private boolean afterReturn = false;

        /**
         * Used for throwing an exception in the before/after hooks.
         */
        private boolean beforeException = false;
        private boolean afterException = false;


        /**
         * Gets sequence number.
         */
        @Override
        public int getSequenceNumber() {
            return 0;
        }

        /**
         * Get beforeLog return value.
         */
        public boolean getBeforeReturn() {
            return this.beforeReturn;
        }

        /**
         * Get afterLog return value.
         */
        public boolean getAfterReturn() {
            return this.afterReturn;
        }

        /**
         * Sets the return value for the before/after hooks.
         *
         * @param beforeVal beforeLog() return value
         * @param afterVal  afterLog() return value
         */
        public void setReturnValue(boolean beforeVal, boolean afterVal) {
            this.beforeReturn = beforeVal;
            this.afterReturn = afterVal;
        }

        /**
         * Gets beforeException boolean.
         */
        public boolean getBeforeException() {
            return this.beforeException;
        }

        /**
         * Gets afterException boolean.
         */
        public boolean getAfterException() {
            return this.afterException;
        }

        /**
         * Sets before/after flags to determine if the feature should throw an exception.
         */
        public void setExceptions(boolean beforeException, boolean afterException) {
            this.beforeException = beforeException;
            this.afterException = afterException;
        }

        /**
         * Simple beforeLog message.
         */
        @Override
        public boolean beforeLog(Logger eventLogger, EventType type, CommInfrastructure protocol, String topic,
                                 String message) {

            if (beforeException) {
                throw new RuntimeException("beforeLog exception");
            }

            eventLogger.info("before feature test");

            return this.beforeReturn;
        }

        /**
         * Simple afterLog message.
         */
        @Override
        public boolean afterLog(Logger eventLogger, EventType type, CommInfrastructure protocol, String topic,
                                String message) {

            if (afterException) {
                throw new RuntimeException("afterLog exception");
            }

            eventLogger.info("after feature test");

            return this.afterReturn;
        }

    }

}
