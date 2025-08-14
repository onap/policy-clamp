/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.runtime.main.rest;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_PLAIN;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.message.bus.event.TopicEndpoint;
import org.onap.policy.common.message.bus.event.TopicEndpointManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;

@AutoConfigureObservability
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ActuatorControllerTest {

    @Autowired
    private WebTestClient webClient;

    @Value("${spring.security.user.name}")
    private String username;
    @Value("${spring.security.user.password}")
    private String password;

    @BeforeEach
    void beforeEach() {
        var filter = ExchangeFilterFunctions.basicAuthentication(username, password);
        webClient = webClient.mutate().filter(filter).build();
    }

    @Test
    void testGetHealth() {
        webClient.get().uri("/health").accept(APPLICATION_JSON)
            .exchange().expectStatus().isOk()
            .expectBody().jsonPath("$.status.code").isEqualTo("UP");

        TopicEndpoint topicEndpoint = TopicEndpointManager.getManager();
        topicEndpoint.stop();
        webClient.get().uri("/health").accept(APPLICATION_JSON)
            .exchange().expectStatus().is5xxServerError()
            .expectBody().jsonPath("$.status.code").isEqualTo("DOWN");
        topicEndpoint.start();
    }

    @Test
    void testHealthIndicator() {
        TopicEndpoint topicEndpoint = TopicEndpointManager.getManager();
        topicEndpoint.getNoopTopicSource("policy-acruntime-participant").stop();
        webClient.get().uri("/health").accept(APPLICATION_JSON)
            .exchange().expectStatus().is5xxServerError()
            .expectBody().jsonPath("$.status.code").isEqualTo("OUT_OF_SERVICE");
        topicEndpoint.getNoopTopicSource("policy-acruntime-participant").start();

        topicEndpoint.getNoopTopicSink("acm-ppnt-sync").stop();
        webClient.get().uri("/health").accept(APPLICATION_JSON)
            .exchange().expectStatus().is5xxServerError()
            .expectBody().jsonPath("$.status.code").isEqualTo("OUT_OF_SERVICE");
        topicEndpoint.getNoopTopicSink("acm-ppnt-sync").start();
    }

    @Test
    void testGetMetrics() {
        webClient.get().uri("/metrics").accept(APPLICATION_JSON)
            .exchange().expectStatus().isOk();
    }

    @Test
    void testGetPrometheus() {
        webClient.get().uri("/prometheus").accept(TEXT_PLAIN)
            .exchange().expectStatus().isOk();
    }

    @Test
    void testGetSwagger() {
        webClient.get().uri("/v3/api-docs").accept(APPLICATION_JSON)
            .exchange().expectStatus().isOk();
    }
}
