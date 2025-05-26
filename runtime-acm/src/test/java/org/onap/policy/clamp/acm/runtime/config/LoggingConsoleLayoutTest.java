/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.clamp.acm.runtime.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggerContextVO;
import ch.qos.logback.classic.spi.ThrowableProxy;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;

class LoggingConsoleLayoutTest {

    private static  final String FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSX";

    private static final Coder CODER = new StandardCoder();

    @Data
    private static class DummyEvent implements ILoggingEvent {

        private String threadName = "main";
        private Level level = Level.INFO;
        private String message = "{\"key\": \"value\"}";
        private Object[] argumentArray;
        private String loggerName = LoggingConsoleLayoutTest.class.getCanonicalName();
        private LoggerContextVO loggerContextVO;
        private IThrowableProxy throwableProxy;
        private StackTraceElement[] callerData;
        private List<Marker> markerList;
        private Map<String, String> mdc;
        private long timeStamp = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        private int nanoseconds;
        private long sequenceNumber;
        private List<KeyValuePair> keyValuePairs;

        @Override
        public void prepareForDeferredProcessing() {
            // dummy implementation
        }

        @Override
        public boolean hasCallerData() {
            return false;
        }

        @Override
        public Map<String, String> getMDCPropertyMap() {
            return Map.of();
        }

        @Override
        public String getFormattedMessage() {
            return message;
        }
    }

    @Test
    void testLog() throws CoderException {
        var layout = new LoggingConsoleLayout();
        var event = new DummyEvent();

        // start() not called
        testingResult(layout, event);

        // start() with default data format
        layout.start();
        testingResult(layout, event);

        // TimestampFormat
        layout.setTimestampFormat(FORMAT);
        layout.start();
        testingResult(layout, event);

        // wrong format
        layout.setTimestampFormat("wrong Timestamp");
        layout.setStaticParameters("wrong Parameter");
        layout.start();
        testingResult(layout, event);

        // TimestampFormat and TimestampFormatTimezoneId
        layout.setTimestampFormat(FORMAT);
        layout.setTimestampFormatTimezoneId("UTC");
        layout.setStaticParameters("service_id=policy-acm|application_id=policy-acm");
        layout.start();
        testingResult(layout, event);

        // null TimestampFormat
        layout.setTimestampFormat(null);
        layout.setStaticParameters(null);
        layout.start();
        testingResult(layout, event);

        // blank TimestampFormat
        layout.setTimestampFormat("");
        layout.setStaticParameters("");
        layout.start();
        testingResult(layout, event);

        // null TimestampFormatTimezoneId
        layout.setTimestampFormat(FORMAT);
        layout.setTimestampFormatTimezoneId(null);
        layout.start();
        testingResult(layout, event);

        // blank TimestampFormatTimezoneId
        layout.setTimestampFormat(FORMAT);
        layout.setTimestampFormatTimezoneId("");
        layout.start();
        testingResult(layout, event);

        //event with exception
        layout.setTimestampFormat(FORMAT);
        layout.setTimestampFormatTimezoneId("UTC");
        layout.setStaticParameters("service_id=policy-acm|application_id=policy-acm");
        layout.start();
        var throwable = new Throwable("PSQL Exception: Unable to modify object");
        event.setThrowableProxy(new ThrowableProxy(throwable));
        testingResult(layout, event);
    }

    private void testingResult(LoggingConsoleLayout layout, DummyEvent event) throws CoderException {
        var result = layout.doLayout(event);
        assertThat(result).isNotNull();
        var map = CODER.decode(result, Map.class);
        assertEquals(event.level.toString(), map.get("severity"));
        assertEquals(event.message, map.get("message"));
        @SuppressWarnings("unchecked")
        var extraData = (Map<String, Object>) map.get("extra_data");
        assertEquals(event.loggerName, extraData.get("logger"));
        assertEquals(event.threadName, extraData.get("thread"));
        if (event.getThrowableProxy() != null) {
            assertTrue(((Map<?, ?>) extraData.get("exception")).containsKey("stack_trace"));
        }
    }
}
