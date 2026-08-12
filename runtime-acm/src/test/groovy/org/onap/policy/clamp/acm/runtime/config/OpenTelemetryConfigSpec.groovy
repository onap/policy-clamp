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

import io.opentelemetry.sdk.extension.trace.jaeger.sampler.JaegerRemoteSampler
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import io.opentelemetry.api.OpenTelemetry
import spock.lang.Specification

class OpenTelemetryConfigSpec extends Specification {

    static final TRACING_EXPORT_OFF = "management.tracing.export.enabled=false"
    static final TRACING_EXPORT_ON = "management.tracing.export.enabled=true"
    static final ENDPOINT = "management.opentelemetry.tracing.jaeger-remote-sampler.endpoint=http://jaeger:14250"
    static final ENDPOINT_EMPTY = "management.opentelemetry.tracing.jaeger-remote-sampler.endpoint="
    static final APP_NAME = "spring.application.name=acm-runtime"

    @Configuration
    static class OpenTelemetryStubConfig {
        @Bean
        OpenTelemetry openTelemetry() {
            return OpenTelemetry.noop()
        }
    }

    def contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenTelemetryConfig))
            .withUserConfiguration(OpenTelemetryStubConfig)

    def "JaegerRemoteSampler bean #scenario"() {
        given: "an application context with properties: #properties"
        def runner = contextRunner.withPropertyValues(properties as String[])

        expect: "the JaegerRemoteSampler bean presence is #expected"
        runner.run { context ->
            assert context.containsBean('jaegerRemoteSampler') == expected
        }

        where:
        scenario                                    | properties                                              || expected
        "not created when tracing export disabled"  | [TRACING_EXPORT_OFF, ENDPOINT, APP_NAME]                || false
        "not created when endpoint absent"          | [APP_NAME]                                              || false
        "not created when endpoint empty"           | [ENDPOINT_EMPTY, APP_NAME]                              || false
        "created when endpoint configured"          | [TRACING_EXPORT_ON, ENDPOINT, APP_NAME]                 || true
    }

    def "JaegerRemoteSampler bean is created with correct configuration"() {
        given: "tracing enabled with a jaeger endpoint"
        def runner = contextRunner.withPropertyValues(TRACING_EXPORT_ON, ENDPOINT, APP_NAME)

        expect: "the JaegerRemoteSampler bean is created and configured"
        runner.run { context ->
            def sampler = context.getBean(JaegerRemoteSampler)
            assert sampler != null
        }
    }

    def "jdbcTelemetryPostProcessor bean is created when tracing export enabled"() {
        given: "tracing enabled"
        def runner = contextRunner.withPropertyValues(TRACING_EXPORT_ON, APP_NAME)

        expect: "the jdbcTelemetryPostProcessor bean is present"
        runner.run { context ->
            assert context.containsBean('jdbcTelemetryPostProcessor')
        }
    }

    def "jdbcTelemetryPostProcessor bean is not created when tracing export disabled"() {
        given: "tracing disabled"
        def runner = contextRunner.withPropertyValues(TRACING_EXPORT_OFF, APP_NAME)

        expect: "the jdbcTelemetryPostProcessor bean is absent"
        runner.run { context ->
            assert !context.containsBean('jdbcTelemetryPostProcessor')
        }
    }
}
