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

import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import spock.lang.Specification

class OpenTelemetryConfigSpec extends Specification {

    static final TRACING_EXPORT_OFF = "management.tracing.export.enabled=false"
    static final ENDPOINT = "management.opentelemetry.tracing.jaeger-remote-sampler.endpoint=http://jaeger:14250"
    static final ENDPOINT_EMPTY = "management.opentelemetry.tracing.jaeger-remote-sampler.endpoint="
    static final APP_NAME = "spring.application.name=acm-runtime"

    def contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenTelemetryConfig))

    def "jdbcTelemetryPostProcessor bean is registered when tracing export is enabled"() {
        expect:
        contextRunner.run { context ->
            assert context.containsBean('jdbcTelemetryPostProcessor')
            assert context.getBean('jdbcTelemetryPostProcessor') instanceof org.springframework.beans.factory.config.BeanPostProcessor
        }
    }

    def "jdbcTelemetryPostProcessor bean is not registered when tracing export is disabled"() {
        expect:
        contextRunner.withPropertyValues(TRACING_EXPORT_OFF).run { context ->
            assert !context.containsBean('jdbcTelemetryPostProcessor')
        }
    }

    def "JaegerRemoteSampler bean #scenario"() {
        given: "an application context with properties: #properties"
        def runner = contextRunner.withPropertyValues(properties as String[])

        expect: "the JaegerRemoteSampler bean presence is #expected"
        runner.run { context ->
            assert context.containsBean('jaegerRemoteSampler') == expected
        }

        where:
        scenario                                    | properties                                || expected
        "not created when tracing export disabled"  | [TRACING_EXPORT_OFF, ENDPOINT, APP_NAME]  || false
        "not created when endpoint absent"          | [APP_NAME]                                || false
        "not created when endpoint empty"           | [ENDPOINT_EMPTY, APP_NAME]                || false
        "created when endpoint configured"          | [ENDPOINT, APP_NAME]                      || true
    }
}
