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

import com.zaxxer.hikari.HikariDataSource
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.context.Scope
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.datasource.DelegatingDataSource
import spock.lang.Specification

class JdbcTelemetryBeanPostProcessorSpec extends Specification {

    def processor = new JdbcTelemetryBeanPostProcessor()
    def applicationContext = Mock(ApplicationContext)

    def setup() {
        applicationContext.getBean(OpenTelemetry) >> OpenTelemetry.noop()
        processor.setApplicationContext(applicationContext)
    }

    def "non-HikariDataSource bean is returned unchanged"() {
        given:
        def bean = new Object()

        expect:
        processor.postProcessAfterInitialization(bean, "someBean") is bean
    }

    def "HikariDataSource is wrapped in a DelegatingDataSource"() {
        given:
        def hikari = new HikariDataSource()

        when:
        def result = processor.postProcessAfterInitialization(hikari, "dataSource")

        then:
        result instanceof DelegatingDataSource

        cleanup:
        hikari.close()
    }

    def "getConnection() delegates to hikari when no active span"() {
        given:
        def hikari = Mock(HikariDataSource)
        def conn = Mock(java.sql.Connection)
        hikari.getConnection() >> conn

        when:
        def wrapper = processor.postProcessAfterInitialization(hikari, "dataSource")
        def result = wrapper.getConnection()

        then:
        result == conn
    }

    def "getConnection(user, pass) delegates to hikari when no active span"() {
        given:
        def hikari = Mock(HikariDataSource)
        def conn = Mock(java.sql.Connection)
        hikari.getConnection("user", "pass") >> conn

        when:
        def wrapper = processor.postProcessAfterInitialization(hikari, "dataSource")
        def result = wrapper.getConnection("user", "pass")

        then:
        result == conn
    }

    def "getConnection() uses tracingDataSource when span is active"() {
        given:
        def meta = Mock(java.sql.DatabaseMetaData) { getURL() >> "jdbc:h2:mem:test" }
        def conn = Mock(java.sql.Connection) { getMetaData() >> meta }
        def hikari = Mock(HikariDataSource)
        hikari.getConnection() >> conn
        hikari.getConnection(_ as String, _ as String) >> conn
        def wrapper = processor.postProcessAfterInitialization(hikari, "dataSource")

        def spanContext = SpanContext.create(
                "0af7651916cd43dd8448eb211c80319c",
                "b7ad6b7169203331",
                TraceFlags.getSampled(),
                TraceState.getDefault())
        Scope scope = Span.wrap(spanContext).makeCurrent()

        when:
        def result = wrapper.getConnection()

        then:
        result != null

        cleanup:
        scope.close()
    }

    def "getConnection(user, pass) uses tracingDataSource when span is active"() {
        given:
        def meta = Mock(java.sql.DatabaseMetaData) { getURL() >> "jdbc:h2:mem:test" }
        def conn = Mock(java.sql.Connection) { getMetaData() >> meta }
        def hikari = Mock(HikariDataSource)
        hikari.getConnection() >> conn
        hikari.getConnection(_ as String, _ as String) >> conn
        def wrapper = processor.postProcessAfterInitialization(hikari, "dataSource")

        def spanContext = SpanContext.create(
                "0af7651916cd43dd8448eb211c80319c",
                "b7ad6b7169203331",
                TraceFlags.getSampled(),
                TraceState.getDefault())
        Scope scope = Span.wrap(spanContext).makeCurrent()

        when:
        def result = wrapper.getConnection("user", "pass")

        then:
        result != null

        cleanup:
        scope.close()
    }
}
