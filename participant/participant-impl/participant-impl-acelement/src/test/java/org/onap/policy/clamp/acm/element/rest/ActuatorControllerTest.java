/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation.
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

package org.onap.policy.clamp.acm.element.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.policy.clamp.acm.element.utils.CommonActuatorController;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@AutoConfigureObservability(tracing = false)
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles({ "test", "default" })
class ActuatorControllerTest extends CommonActuatorController {

    private static final String HEALTH_ENDPOINT = "onap/policy/clamp/acelement/v2/health";
    private static final String METRICS_ENDPOINT = "onap/policy/clamp/acelement/v2/metrics";
    private static final String PROMETHEUS_ENDPOINT = "onap/policy/clamp/acelement/v2/prometheus";
    private static final String SWAGGER_ENDPOINT = "onap/policy/clamp/acelement/v2/v3/api-docs";

    @LocalServerPort
    private int randomServerPort;

    @BeforeEach
    public void setUpPort() {
        super.setHttpPrefix(randomServerPort);
    }

    @Test
    void testGetHealth_Unauthorized() {
        assertUnauthorizedActGet(HEALTH_ENDPOINT);
    }

    @Test
    void testGetMetrics_Unauthorized() {
        assertUnauthorizedActGet(METRICS_ENDPOINT);
    }

    @Test
    void testGetPrometheus_Unauthorized() {
        assertUnauthorizedActGet(PROMETHEUS_ENDPOINT);
    }

    @Test
    void testGetSwagger_Unauthorized() {
        assertUnauthorizedActGet(SWAGGER_ENDPOINT);
    }

    @Test
    void testGetHealth() {
        var invocationBuilder = super.sendActRequest(HEALTH_ENDPOINT);
        var rawresp = invocationBuilder.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
    }

    @Test
    void testGetMetrics() {
        var invocationBuilder = super.sendActRequest(METRICS_ENDPOINT);
        var rawresp = invocationBuilder.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
    }

    @Test
    void testGetPrometheus() {
        var invocationBuilder = super.sendActRequest(PROMETHEUS_ENDPOINT);
        var rawresp = invocationBuilder.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
    }

    @Test
    void testGetSwagger() {
        var invocationBuilder = super.sendActRequest(SWAGGER_ENDPOINT);
        var rawresp = invocationBuilder.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
    }
}
