/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2026 Deutsche Telekom. All rights reserved.
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

package org.onap.policy.clamp.acm.participant.intermediary.config

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.time.Duration
import java.util.concurrent.TimeUnit
import org.onap.policy.clamp.acm.participant.intermediary.parameters.KafkaParameters
import org.onap.policy.clamp.acm.participant.intermediary.parameters.ParticipantIntermediaryParameters
import org.onap.policy.clamp.acm.participant.intermediary.parameters.ParticipantParameters
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration
import org.springframework.boot.micrometer.tracing.autoconfigure.MicrometerTracingAutoConfiguration
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.OpenTelemetryTracingAutoConfiguration
import org.springframework.boot.opentelemetry.autoconfigure.OpenTelemetrySdkAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.KafkaTemplate
import spock.lang.Specification

/**
 * The Kafka instrumentation must be driven by the Spring-managed OpenTelemetry SDK.
 *
 * <p>Spring Boot builds its SDK with {@code OpenTelemetrySdk.builder()} and never calls
 * {@code buildAndRegisterGlobal()}, so anything reaching for {@code GlobalOpenTelemetry.get()}
 * gets a no-op instance and silently drops every span.
 */
class KafkaTracingSpec extends Specification {

    static class RecordingSpanExporter implements SpanExporter {

        final List<SpanData> exported = Collections.synchronizedList([])

        @Override
        CompletableResultCode export(Collection<SpanData> spans) {
            exported.addAll(spans)
            CompletableResultCode.ofSuccess()
        }

        @Override
        CompletableResultCode flush() {
            CompletableResultCode.ofSuccess()
        }

        @Override
        CompletableResultCode shutdown() {
            CompletableResultCode.ofSuccess()
        }
    }

    @Configuration
    static class TestConfig {

        @Bean
        ParticipantParameters participantParameters() {
            var kafka = new KafkaParameters(
                    bootstrapServers: 'localhost:9092',
                    authExceptionRetryInterval: Duration.ofSeconds(5))
            kafka.consumer.groupId = 'kafka-tracing-spec'
            return { -> new ParticipantIntermediaryParameters(kafka: kafka) } as ParticipantParameters
        }

        @Bean
        RecordingSpanExporter recordingSpanExporter() {
            new RecordingSpanExporter()
        }
    }

    def contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ObservationAutoConfiguration,
                    MicrometerTracingAutoConfiguration,
                    OpenTelemetryTracingAutoConfiguration,
                    OpenTelemetrySdkAutoConfiguration))
            .withUserConfiguration(KafkaConfig, TestConfig)
            .withPropertyValues(
                    'management.tracing.export.enabled=true',
                    'management.tracing.sampling.probability=1.0')

    def "Kafka observations are recorded by the Spring-managed OpenTelemetry SDK"() {
        expect:
        contextRunner.run { context ->
            var registry = context.getBean(ObservationRegistry)
            var template = context.getBean('acmKafkaTemplate', KafkaTemplate)
            var listenerFactory = context.getBean('acmListenerContainerFactory',
                    ConcurrentKafkaListenerContainerFactory)

            assert !registry.isNoop()
            // KafkaTemplate resolves the registry from the context in afterSingletonsInstantiated()
            assert template.observationRegistry === registry
            assert listenerFactory.containerProperties.observationEnabled

            Observation.createNotStarted('acm.kafka.spec', registry).observe {}
            context.getBean(SdkTracerProvider).forceFlush().join(10, TimeUnit.SECONDS)

            assert context.getBean(RecordingSpanExporter).exported*.name == ['acm.kafka.spec']
            assert GlobalOpenTelemetry.get() !== context.getBean(OpenTelemetry)
        }
    }
}
