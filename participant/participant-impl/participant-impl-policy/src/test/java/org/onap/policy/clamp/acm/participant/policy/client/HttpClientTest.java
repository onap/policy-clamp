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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.policy.concepts.DeploymentSubGroup;
import org.onap.policy.clamp.acm.participant.policy.main.parameters.CommonTestData;
import org.onap.policy.clamp.acm.participant.policy.main.parameters.ParticipantPolicyParameters;
import org.onap.policy.clamp.acm.participant.policy.main.parameters.RestClientParameters;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.springframework.web.reactive.function.client.WebClientException;

/**
 * Tests for api and pap http clients.
 */
class HttpClientTest {

    private static PolicyApiHttpClient apiHttpClient;

    private static PolicyPapHttpClient papHttpClient;

    private static ToscaServiceTemplate serviceTemplate;

    /**
     * Set up Mock server.
     */
    @BeforeAll
    static void setUpMockServer() throws IOException {
        serviceTemplate = CommonTestData.getToscaServiceTemplate("test/funtional-pmsh.yaml");
        // Setup mock web server
        int mockServerPort = 42545;
        var mockServer = new MockWebServer();
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
                if (path.equals("/policy/api/v1/policies") && "POST".equals(request.getMethod())
                        && request.getBody().size() < 40) {
                    return new MockResponse().setResponseCode(404);
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
                if (path.matches("^/policy/api/v1/policies/wrongPolicy/versions/[^/]+$")
                        && "DELETE".equals(request.getMethod())) {
                    return new MockResponse().setResponseCode(404);
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
        var params = new ParticipantPolicyParameters();
        var restClientParameters = getMockClientParameters(mockServerPort);
        params.setPolicyApiParameters(restClientParameters);
        params.setPolicyPapParameters(restClientParameters);

        apiHttpClient = new PolicyApiHttpClient(params);
        papHttpClient = new PolicyPapHttpClient(params);
    }

    @Test
    void testCreatePolicy() {
        assertDoesNotThrow(() -> apiHttpClient.createPolicy(serviceTemplate));
    }

    @Test
    void testCreateBabPolicy() {
        var badServiceTemplate = new ToscaServiceTemplate();
        assertThrows(WebClientException.class, () -> apiHttpClient.createPolicy(badServiceTemplate));
    }

    @Test
    void testCreatePolicyTypes() {
        assertDoesNotThrow(() -> apiHttpClient.createPolicyType(serviceTemplate));
    }

    @Test
    void testDeletePolicy() {
        assertDoesNotThrow(() -> apiHttpClient.deletePolicy("dummyPolicy", "1.0.0"));
    }

    @Test
    void testBadDeletePolicy() {
        assertThrows(WebClientException.class, () -> apiHttpClient.deletePolicy("wrongPolicy", "1.0.0"));
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
        assertThrows(WebClientException.class, () -> apiHttpClient.executePost("/invalid", serviceTemplate));
    }

    private static RestClientParameters getMockClientParameters(int port) {
        var params = new RestClientParameters();
        params.setClientName("policyClient");
        params.setHostname("localhost");
        params.setUserName("user");
        params.setPassword("pwd");
        params.setPort(port);
        return params;
    }
}
