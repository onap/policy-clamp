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

    def "given a non-serializable object, getJson should return the message toString as fallback"() {
        given: "a layout instance and a map containing a non-serializable object"
        def layout = new LoggingConsoleLayout()
        def fallbackMsg = "fallback message"

        when: "getJson is called with the non-serializable map"
        def result = layout.getJson([message: fallbackMsg, bad: new WillNotSerialize()])

        then: "the result should be the message value from the map"
        result == fallbackMsg
    }

    @Unroll
    def "given #label configuration, doLayout should produce valid JSON with correct fields"() {
        given: "a layout configured with timestamp format '#tsFormat', timezone '#tz', and params '#params'"
        def layout = new LoggingConsoleLayout()
        layout.timestampFormat = tsFormat
        layout.timestampFormatTimezoneId = tz
        layout.staticParameters = params
        if (callStart) layout.start()

        def event = createEvent(level, message, logger, thread, exception)

        when: "doLayout is called with the logging event"
        def result = layout.doLayout(event)
        def map = MAPPER.readValue(result, Map)

        then: "the JSON output should contain correct severity, message, logger, thread, and timestamp"
        map.severity == level.toString()
        map.message == message
        map.extra_data.logger == logger
        map.extra_data.thread == thread
        map.timestamp != null

        and: "exception stack trace should be present only when an exception is provided"
        exception ? map.extra_data.exception.stack_trace != null : !map.extra_data.containsKey("exception")

        and: "static parameters should appear as top-level keys when properly configured"
        hasStaticParams ? (map.service_id == "policy-acm" && map.application_id == "policy-acm") : true

        where:
        label                | tsFormat          | tz    | params                                            | callStart | level      | message            | logger                     | thread     | exception                                                                        | hasStaticParams
        "defaults"           | null              | null  | null                                              | true      | Level.INFO | '{"key": "value"}' | "test.Logger"              | "main"     | null                                                                             | false
        "no start called"    | null              | null  | null                                              | false     | Level.INFO | '{"key": "value"}' | "test.Logger"              | "main"     | null                                                                             | false
        "blank values"       | ""                | ""    | ""                                                | true      | Level.INFO | '{"key": "value"}' | "test.Logger"              | "main"     | null                                                                             | false
        "custom format"      | FORMAT            | null  | null                                              | true      | Level.INFO | '{"key": "value"}' | "test.Logger"              | "main"     | null                                                                             | false
        "format+blank tz"    | FORMAT            | ""    | ""                                                | true      | Level.INFO | '{"key": "value"}' | "test.Logger"              | "main"     | null                                                                             | false
        "full config"        | FORMAT            | "UTC" | "service_id=policy-acm|application_id=policy-acm" | true      | Level.INFO | '{"key": "value"}' | "test.Logger"              | "main"     | null                                                                             | true
        "invalid format"     | "wrong Timestamp" | null  | "wrong Parameter"                                 | true      | Level.INFO | '{"key": "value"}' | "test.Logger"              | "main"     | null                                                                             | false
        "format needing tz"  | "yyyy-MM-dd'T'HH:mm:ss.SSSXXX" | null | null                                 | true      | Level.INFO | '{"key": "value"}' | "test.Logger"              | "main"     | null                                                                             | false
        "with exception"     | FORMAT            | "UTC" | "service_id=policy-acm|application_id=policy-acm" | true      | Level.INFO | '{"key": "value"}' | "test.Logger"              | "main"     | new Throwable("PSQL Exception: Unable to modify object")                         | true
        "chained exception"  | FORMAT            | "UTC" | null                                              | true      | Level.ERROR| "Operation failed" | "org.onap.policy.Service"  | "worker-1" | new RuntimeException("outer", new IllegalStateException("inner cause"))           | false
    }

    def createEvent(Level level, String message, String logger, String thread, Throwable exception) {
        Stub(ILoggingEvent) {
            getThreadName() >> thread
            getLevel() >> level
            getFormattedMessage() >> message
            getLoggerName() >> logger
            getInstant() >> Instant.now()
            getThrowableProxy() >> (exception ? new ThrowableProxy(exception) : null)
        }
    }

    static class WillNotSerialize {
        WillNotSerialize getSelf() { this }
    }
}
