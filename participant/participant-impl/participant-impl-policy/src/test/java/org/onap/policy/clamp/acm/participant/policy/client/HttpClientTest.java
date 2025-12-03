/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022, 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.participant.policy.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.policy.concepts.DeploymentSubGroup;
import org.onap.policy.clamp.acm.participant.policy.main.parameters.ParticipantPolicyParameters;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionRuntimeException;
import org.onap.policy.common.parameters.rest.RestClientParameters;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

/**
 * Tests for api and pap http clients.
 */
class HttpClientTest {

    private static PolicyApiHttpClient apiHttpClient;

    private static PolicyPapHttpClient papHttpClient;

    /**
     * Set up Mock server.
     */
    @BeforeAll
    static void setUpMockServer() throws IOException, InterruptedException {
        // Setup mock web server
        int mockServerPort = 42545;
        MockWebServer mockServer = new MockWebServer();
        mockServer.start(mockServerPort);
        mockServer.setDispatcher(new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest request) {
                String path = request.getPath();
                assert path != null;
                if (path.equals("/policy/api/v1/policytypes") && "POST".equals(request.getMethod())) {
                    return new MockResponse()
                            .setResponseCode(200)
                            .addHeader("Content-Type", "application/json");
                }
                if (path.equals("/policy/api/v1/policies") && "POST".equals(request.getMethod())) {
                    return new MockResponse()
                            .setResponseCode(200)
                            .addHeader("Content-Type", "application/json");
                }
                if (path.matches("^/policy/api/v1/policytypes/[^/]+/versions/[^/]+$")
                        && "DELETE".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200);
                }
                if (path.matches("^/policy/api/v1/policies/[^/]+/versions/[^/]+$")
                        && "DELETE".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(200);
                }
                if (path.equals("/policy/pap/v1/pdps/deployments/batch")
                        && "POST".equals(request.getMethod())) {
                    return new MockResponse()
                            .setResponseCode(200)
                            .addHeader("Content-Type", "application/json");
                }
                return new MockResponse().setResponseCode(404);
            }
        });

        // Setup mock api and pap client
        ParticipantPolicyParameters params = new ParticipantPolicyParameters();
        RestClientParameters restClientParameters = getMockClientParameters(mockServerPort);
        params.setPolicyApiParameters(restClientParameters);
        params.setPolicyPapParameters(restClientParameters);

        apiHttpClient = new PolicyApiHttpClient(params);
        papHttpClient = new PolicyPapHttpClient(params);
    }


    @Test
    void testCreatePolicy() {
        assertDoesNotThrow(() -> apiHttpClient.createPolicy(getTestToscaServiceTemplate()));
    }

    @Test
    void testCreatePolicyTypes() {
        assertDoesNotThrow(() -> apiHttpClient.createPolicyType(getTestToscaServiceTemplate()));
    }

    @Test
    void testDeletePolicy() {
        assertDoesNotThrow(() -> apiHttpClient.deletePolicy("dummyPolicy", "1.0.0"));
    }

    @Test
    void testDeletePolicyType() {
        assertDoesNotThrow(() -> apiHttpClient.deletePolicyType("dummyPolicy", "1.0.0"));
    }

    @Test
    void testDeployPolicy() {
        assertDoesNotThrow(() -> papHttpClient.handlePolicyDeployOrUndeploy("dummyPolicy", "1.0.0",
                DeploymentSubGroup.Action.POST));
    }

    @Test
    void testUnDeployPolicy() {
        assertDoesNotThrow(() -> papHttpClient.handlePolicyDeployOrUndeploy("dummyPolicy", "1.0.0",
                DeploymentSubGroup.Action.DELETE));
    }

    @Test
    void testInvalidEndpoint() {
        Response response = apiHttpClient.executePost("/invalid", Entity.entity(getTestToscaServiceTemplate(),
                MediaType.APPLICATION_JSON));
        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    void testInvalidClientParameter() {
        var parameters = new ParticipantPolicyParameters();
        assertThrows(AutomationCompositionRuntimeException.class,
               () -> new PolicyApiHttpClient(parameters));
    }


    private ToscaServiceTemplate getTestToscaServiceTemplate() {
        return new ToscaServiceTemplate();
    }

    private static RestClientParameters getMockClientParameters(int port) {
        RestClientParameters params = new RestClientParameters();
        params.setName("policyClient");
        params.setHostname("localhost");
        params.setPort(port);
        params.setUseHttps(false);
        return params;
    }



}
