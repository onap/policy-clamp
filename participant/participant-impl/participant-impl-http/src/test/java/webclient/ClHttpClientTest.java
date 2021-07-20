/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

package webclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.policy.clamp.controlloop.participant.http.main.models.ConfigRequest;
import org.onap.policy.clamp.controlloop.participant.http.main.models.ConfigurationEntity;
import org.onap.policy.clamp.controlloop.participant.http.main.webclient.ClHttpClient;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import utils.CommonTestData;

@ExtendWith(SpringExtension.class)
class ClHttpClientTest {

    private CommonTestData commonTestData = new CommonTestData();

    private String testBaseUrl = "https://httpbin.org";

    private Map<ToscaConceptIdentifier, Pair<Integer, String>> responseMap = new HashMap<>();

    @BeforeEach
    void clearClResponseMap() {
        responseMap.clear();
    }

    @Test
    void test_validRequest() {
        //Add valid rest requests POST, GET
        ConfigurationEntity configurationEntity = commonTestData.getConfigurationEntity();

        Map<String, String> headers = commonTestData.getHeaders();
        ConfigRequest configRequest = new ConfigRequest(testBaseUrl, headers,
            List.of(configurationEntity), 10);

        ClHttpClient client = new ClHttpClient(configRequest, responseMap);
        assertDoesNotThrow(client::run);
        assertThat(responseMap).hasSize(2).containsKey(commonTestData
            .restParamsWithGet().getRestRequestId());

        Pair<Integer, String> restResponseMap = responseMap.get(commonTestData
            .restParamsWithGet().getRestRequestId());
        assertThat(restResponseMap.getKey()).isEqualTo(200);
    }

    @Test
    void test_invalidRequest() {
        //Add rest requests Invalid POST, Valid GET
        ConfigurationEntity configurationEntity = commonTestData.getInvalidConfigurationEntity();

        Map<String, String> headers = commonTestData.getHeaders();
        ConfigRequest configRequest = new ConfigRequest(testBaseUrl, headers,
            List.of(configurationEntity), 10);

        ClHttpClient client = new ClHttpClient(configRequest, responseMap);
        assertDoesNotThrow(client::run);
        assertThat(responseMap).hasSize(2).containsKey(commonTestData
            .restParamsWithGet().getRestRequestId());
        Pair<Integer, String> response = responseMap
            .get(commonTestData.restParamsWithInvalidPost().getRestRequestId());
        assertThat(response.getKey()).isEqualTo(404);
    }
}
