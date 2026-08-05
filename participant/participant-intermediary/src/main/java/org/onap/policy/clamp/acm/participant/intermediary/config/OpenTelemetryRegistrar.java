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

package org.onap.policy.clamp.acm.participant.intermediary.config;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;

/**
 * Bridges the Spring-managed {@link OpenTelemetry} bean to {@link GlobalOpenTelemetry}.
 *
 * <p>The message-bus library uses {@code GlobalOpenTelemetry.get()} to obtain the OpenTelemetry
 * instance for Kafka tracing interceptors. Spring Boot's OpenTelemetry auto-configuration creates
 * an {@code OpenTelemetry} bean but does not register it as the global instance. This configuration
 * bridges the two so that message-bus Kafka tracing works correctly when the application uses
 * Spring Boot's OTel auto-configuration (e.g. {@code spring-boot-starter-opentelemetry}).
 *
 * <p>When no {@code OpenTelemetry} bean is present (e.g. tracing is disabled or the application
 * uses raw SDK auto-configuration via {@code OTEL_*} environment variables), this class is a no-op.
 *
 * <p>This class will be removed together with the message-bus when the migration to Spring Kafka
 * is complete.
 */
@Configuration
@RequiredArgsConstructor
public class OpenTelemetryRegistrar {

    private final ObjectProvider<OpenTelemetry> openTelemetryProvider;

    @PostConstruct
    void registerGlobalOpenTelemetry() {
        openTelemetryProvider.ifAvailable(GlobalOpenTelemetry::set);
    }
}
