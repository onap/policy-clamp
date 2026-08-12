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
import org.springframework.context.ApplicationContext
import spock.lang.Specification

class JdbcTelemetryBeanPostProcessorSpec extends Specification {

    def processor = new JdbcTelemetryBeanPostProcessor()

    def "setApplicationContext stores the context"() {
        given:
        def ctx = Mock(ApplicationContext)

        when:
        processor.setApplicationContext(ctx)

        then:
        noExceptionThrown()
    }

    def "postProcessAfterInitialization returns non-HikariDataSource bean unchanged"() {
        given:
        def bean = "someBean"

        expect:
        processor.postProcessAfterInitialization(bean, "myBean") == bean
    }

    def "postProcessAfterInitialization wraps HikariDataSource with OpenTelemetry instrumentation"() {
        given:
        def ctx = Mock(ApplicationContext)
        ctx.getBean(OpenTelemetry) >> OpenTelemetry.noop()
        processor.setApplicationContext(ctx)

        def hikariDs = new HikariDataSource()
        hikariDs.setJdbcUrl("jdbc:h2:mem:testdb")
        hikariDs.setDriverClassName("org.h2.Driver")

        when:
        def result = processor.postProcessAfterInitialization(hikariDs, "dataSource")

        then:
        result != null
        result != hikariDs
    }
}
