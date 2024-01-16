/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation.
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

import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.extension.trace.jaeger.sampler.JaegerRemoteSampler;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenTelConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "tracing", name = "enabled", havingValue = "true", matchIfMissing = false)
    @ConditionalOnExpression("'http'.equals('${tracing.exporter.protocol}')")
    OtlpHttpSpanExporter otlpHttpSpanExporter(@Value("${tracing.exporter.endpoint:http://jaeger:4318/v1/traces}") String url) {
        return OtlpHttpSpanExporter.builder()
                .setEndpoint(url)
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "tracing", name = "enabled", havingValue = "true", matchIfMissing = false)
    @ConditionalOnExpression("'grpc'.equals('${tracing.exporter.protocol}')")
    OtlpGrpcSpanExporter otlpGrpcSpanExporter(@Value("${tracing.exporter.endpoint:http://jaeger:4317}") String url) {
        return OtlpGrpcSpanExporter.builder()
                .setEndpoint(url)
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "tracing", name = "enabled", havingValue = "true", matchIfMissing = false)
    JaegerRemoteSampler jaegerRemoteSampler(
            @Value("${tracing.sampler.jaeger-remote.endpoint:http://jaeger:14250}") String url,
            @Value("${SERVICE_ID:unknown_service}") String serviceId) {
        return JaegerRemoteSampler.builder()
                .setEndpoint(url)
                .setPollingInterval(Duration.ofSeconds(30))
                .setInitialSampler(Sampler.alwaysOff())
                .setServiceName(serviceId)
                .build();
    }
}
