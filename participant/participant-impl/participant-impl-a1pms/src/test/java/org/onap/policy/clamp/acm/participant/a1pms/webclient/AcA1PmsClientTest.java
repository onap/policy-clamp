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

package org.onap.policy.clamp.acm.participant.a1pms.webclient;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.onap.policy.clamp.acm.participant.a1pms.parameters.A1PmsParameters;
import org.onap.policy.clamp.acm.participant.a1pms.utils.CommonTestData;
import org.onap.policy.clamp.acm.participant.a1pms.utils.MockServerRest;
import org.onap.policy.common.utils.network.NetworkUtil;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class AcA1PmsClientTest {

    private static int mockServerPort;

    private static MockServerRest mockServer;

    private static CommonTestData commonTestData;

    @Mock
    private A1PmsParameters a1PmsParameters;


    private final String healthyUrl = "/healthy";

    private final String servicesUrl = "/services/success";

    private final String serviceUrl = "/service/success/{serviceId}";

    /**
     * Set up Mock server.
     */
    @BeforeAll
    static void setUpMockServer() throws IOException, InterruptedException {
        mockServerPort = NetworkUtil.allocPort();
        mockServer = new MockServerRest(mockServerPort);
        mockServer.validate();
        commonTestData = new CommonTestData();
    }

    @AfterAll
    static void stopServer() throws Exception {
        mockServer.close();
        mockServer = null;
    }

    void initializePmsHealthParameters(String healthUrl) {
        String testMockUrl = "http://localhost";
        when(a1PmsParameters.getBaseUrl()).thenReturn(testMockUrl + ":" + mockServerPort);
        var endpoints = new A1PmsParameters.Endpoints();
        endpoints.setHealth(healthUrl);
        when(a1PmsParameters.getEndpoints()).thenReturn(endpoints);
    }

    void initializePmsServicesParameters(String servicesUrl) {
        initializePmsHealthParameters(healthyUrl);
        var endpoints = a1PmsParameters.getEndpoints();
        endpoints.setServices(servicesUrl);
        when(a1PmsParameters.getEndpoints()).thenReturn(endpoints);
    }

    void initializePmsServiceParameters(String serviceUrl) {
        initializePmsServicesParameters(servicesUrl);
        var endpoints = a1PmsParameters.getEndpoints();
        endpoints.setService(serviceUrl);
        when(a1PmsParameters.getEndpoints()).thenReturn(endpoints);
    }


    @Test
    void test_healthyPms() {
        initializePmsHealthParameters(healthyUrl);
        var client = new AcA1PmsClient(a1PmsParameters);
        assertTrue(client.isPmsHealthy());
    }

    @Test
    void test_unhealthyPms() {
        String unhealthyUrl = "/unhealthy";
        initializePmsHealthParameters(unhealthyUrl);
        var client = new AcA1PmsClient(a1PmsParameters);
        assertFalse(client.isPmsHealthy());
    }

    @Test
    void test_createServicesSuccess() {
        initializePmsServicesParameters(servicesUrl);
        var client = new AcA1PmsClient(a1PmsParameters);
        assertDoesNotThrow(() -> client.createService(commonTestData.getValidPolicyEntities()));
    }

    @Test
    void test_createServicesFailure() {
        String createServiceFailureUrl = "services/failure";
        initializePmsServicesParameters(createServiceFailureUrl);
        var client = new AcA1PmsClient(a1PmsParameters);
        String expectedMessage = "Error in creating policy service";
        assertThrows(Exception.class,
                () -> client.createService(commonTestData.getValidPolicyEntities()), expectedMessage);
    }

    @Test
    void test_deleteServicesSuccess() {
        initializePmsServiceParameters(serviceUrl);
        var client = new AcA1PmsClient(a1PmsParameters);
        assertDoesNotThrow(() -> client.deleteService(commonTestData.getValidPolicyEntities()));
    }

    @Test
    void test_deleteServicesFailure() {
        String deleteServiceFailureUrl = "services/failure/{serviceId}";
        initializePmsServiceParameters(deleteServiceFailureUrl);
        var client = new AcA1PmsClient(a1PmsParameters);
        String expectedMessage = "Error in deleting policy service";
        assertThrows(Exception.class,
                () -> client.deleteService(commonTestData.getValidPolicyEntities()), expectedMessage);
    }
}
