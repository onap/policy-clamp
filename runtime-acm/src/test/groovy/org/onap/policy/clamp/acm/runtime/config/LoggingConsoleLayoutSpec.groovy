/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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
package org.onap.policy.clamp.acm.runtime.config

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxy
import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.policy.common.utils.coder.MapperFactory
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Instant

class LoggingConsoleLayoutSpec extends Specification {

    static final ObjectMapper MAPPER = MapperFactory.createJsonMapper()
    static final String FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSX"

    def "given a non-serializable object, getJson should return a non-null fallback result"() {
        given: "a layout instance and a map containing a non-serializable object"
        def layout = new LoggingConsoleLayout()

        when: "getJson is called with the non-serializable map"
        def result = layout.getJson([message: new WillNotSerialize()])

        then: "the result should not be null, indicating graceful handling"
        result != null
    }

    @Unroll
    def "given #label configuration, doLayout should produce valid JSON with correct severity and message"() {
        given: "a layout configured with timestamp format '#tsFormat', timezone '#tz', and params '#params'"
        def layout = new LoggingConsoleLayout()
        layout.timestampFormat = tsFormat
        layout.timestampFormatTimezoneId = tz
        layout.staticParameters = params
        layout.start()

        def event = createEvent(exception)

        when: "doLayout is called with the logging event"
        def result = layout.doLayout(event)
        def map = MAPPER.readValue(result, Map)

        then: "the JSON output should contain correct severity, message, logger, and thread"
        map.severity == "INFO"
        map.message == '{"key": "value"}'
        map.extra_data.logger == "test.Logger"
        map.extra_data.thread == "main"

        and: "exception stack trace should be present only when an exception is provided"
        exception ? map.extra_data.exception.stack_trace != null : !map.extra_data.containsKey("exception")

        where:
        label                | tsFormat           | tz    | params                                            | exception
        "defaults"           | null               | null  | null                                              | false
        "blank values"       | ""                 | ""    | ""                                                | false
        "custom format"      | FORMAT             | null  | null                                              | false
        "format+blank tz"    | FORMAT             | ""    | ""                                                | false
        "full config"        | FORMAT             | "UTC" | "service_id=policy-acm|application_id=policy-acm" | false
        "invalid format"     | "wrong Timestamp"  | null  | "wrong Parameter"                                 | false
        "with exception"     | FORMAT             | "UTC" | "service_id=policy-acm|application_id=policy-acm" | true
    }

    private ILoggingEvent createEvent(boolean withException) {
        def event = Stub(ILoggingEvent) {
            getThreadName() >> "main"
            getLevel() >> Level.INFO
            getFormattedMessage() >> '{"key": "value"}'
            getLoggerName() >> "test.Logger"
            getInstant() >> Instant.now()
            getThrowableProxy() >> (withException
                    ? new ThrowableProxy(new Throwable("PSQL Exception: Unable to modify object"))
                    : null)
        }
        return event
    }

    static class WillNotSerialize {
        WillNotSerialize getSelf() { this }
    }
}
