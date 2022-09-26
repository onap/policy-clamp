/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.http.webclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.policy.clamp.acm.participant.http.main.models.ConfigRequest;
import org.onap.policy.clamp.acm.participant.http.main.webclient.AcHttpClient;
import org.onap.policy.clamp.acm.participant.http.utils.CommonTestData;
import org.onap.policy.clamp.acm.participant.http.utils.MockServerRest;
import org.onap.policy.common.utils.network.NetworkUtil;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class AcHttpClientTest {

    private static CommonTestData commonTestData;

    private static int mockServerPort;

    private final String testMockUrl = "http://localhost";

    private static MockServerRest mockServer;

    /**
     * Set up Mock server.
     */
    @BeforeAll
    static void setUpMockServer() throws IOException {
        mockServerPort = NetworkUtil.allocPort();
        mockServer = new MockServerRest(mockServerPort);
        commonTestData = new CommonTestData();
    }

    @AfterAll
    public static void stopServer() throws Exception {
        mockServer.close();
        mockServer = null;
    }

    @Test
    void test_validRequest() {
        // Add valid rest requests POST, GET
        var configurationEntity = commonTestData.getConfigurationEntity();
        Map<ToscaConceptIdentifier, Pair<Integer, String>> responseMap = new HashMap<>();

        var headers = commonTestData.getHeaders();
        var configRequest =
                new ConfigRequest(testMockUrl + ":" + mockServerPort, headers, List.of(configurationEntity), 10);

        var client = new AcHttpClient(configRequest, responseMap);
        assertDoesNotThrow(client::run);
        assertThat(responseMap).hasSize(2).containsKey(commonTestData.restParamsWithGet().getRestRequestId());

        var restResponseMap = responseMap.get(commonTestData.restParamsWithGet().getRestRequestId());
        assertThat(restResponseMap.getKey()).isEqualTo(200);
    }

    @Test
    void test_invalidRequest() {
        // Add rest requests Invalid POST, Valid GET
        var configurationEntity = commonTestData.getInvalidConfigurationEntity();
        Map<ToscaConceptIdentifier, Pair<Integer, String>> responseMap = new HashMap<>();

        var headers = commonTestData.getHeaders();
        var configRequest =
                new ConfigRequest(testMockUrl + ":" + mockServerPort, headers, List.of(configurationEntity), 10);

        var client = new AcHttpClient(configRequest, responseMap);
        assertDoesNotThrow(client::run);
        assertThat(responseMap).hasSize(2).containsKey(commonTestData.restParamsWithGet().getRestRequestId());
        var response = responseMap.get(commonTestData.restParamsWithInvalidPost().getRestRequestId());
        assertThat(response.getKey()).isEqualTo(404);
    }
}
