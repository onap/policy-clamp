/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2026 OpenInfra Foundation Europe. All rights reserved.
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;

@AutoConfigureMetrics
@AutoConfigureTracing
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = {"management.server.port:"})
@ActiveProfiles("test")
class ActuatorControllerTest {

    public static final String HEALTH = "/actuator/health";
    public static final String STATUS_CODE = "$.status";

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
        webClient.get().uri(HEALTH).accept(APPLICATION_JSON)
            .exchange().expectStatus().isOk()
            .expectBody().jsonPath(STATUS_CODE).isEqualTo("UP");
    }

    @Test
    void testGetMetrics() {
        webClient.get().uri("/actuator/metrics").accept(APPLICATION_JSON)
            .exchange().expectStatus().isOk();
    }

    @Test
    void testGetPrometheus() {
        webClient.get().uri("/actuator/prometheus").accept(TEXT_PLAIN)
            .exchange().expectStatus().isOk();
    }

    @Test
    void testGetSwagger() {
        webClient.get().uri("/v3/api-docs").accept(APPLICATION_JSON)
            .exchange().expectStatus().isOk();
    }
}
