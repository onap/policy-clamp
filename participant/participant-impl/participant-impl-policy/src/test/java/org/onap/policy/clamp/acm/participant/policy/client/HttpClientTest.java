/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation.
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

import java.io.IOException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.policy.main.parameters.ParticipantPolicyParameters;
import org.onap.policy.clamp.acm.participant.policy.main.utils.MockServer;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionRuntimeException;
import org.onap.policy.common.endpoints.parameters.RestClientParameters;
import org.onap.policy.common.utils.network.NetworkUtil;
import org.onap.policy.models.pdp.concepts.DeploymentSubGroup;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

/**
 * Tests for api and pap http clients.
 */
public class HttpClientTest {

    private static int mockServerPort;

    private static PolicyApiHttpClient apiHttpClient;

    private static PolicyPapHttpClient papHttpClient;

    private static MockServer mockServer;

    /**
     * Set up Mock server.
     */
    @BeforeAll
    static void setUpMockServer() throws IOException, InterruptedException {
        mockServerPort = NetworkUtil.allocPort();
        mockServer = new MockServer(mockServerPort);
        mockServer.validate();
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
        assertThrows(AutomationCompositionRuntimeException.class,
               () -> new PolicyApiHttpClient(new ParticipantPolicyParameters()));
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
