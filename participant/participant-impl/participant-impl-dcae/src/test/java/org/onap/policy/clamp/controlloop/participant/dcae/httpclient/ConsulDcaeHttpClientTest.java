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

package org.onap.policy.clamp.controlloop.participant.dcae.httpclient;

import static org.junit.Assert.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.integration.ClientAndServer;
import org.onap.policy.clamp.controlloop.participant.dcae.main.parameters.CommonTestData;
import org.onap.policy.clamp.controlloop.participant.dcae.main.parameters.ParticipantDcaeParameters;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Class to perform unit test of {@link ConsulDcaeHttpClient}.
 *
 */
@ExtendWith(SpringExtension.class)
class ConsulDcaeHttpClientTest {

    private static ClientAndServer mockServer;
    private static ParticipantDcaeParameters parameters;

    @BeforeAll
    public static void startServer() {
        CommonTestData commonTestData = new CommonTestData();

        parameters = commonTestData.getParticipantDcaeParameters();

        mockServer = ClientAndServer.startClientAndServer(parameters.getConsulClientParameters().getPort());

        mockServer.when(request().withMethod("PUT").withPath("/v1/kv/dcae-pmsh:policy"))
                .respond(response().withStatusCode(200));
    }

    @AfterAll
    public static void stopServer() {
        mockServer.stop();
        mockServer = null;
    }

    @Test
    void test_deploy() throws Exception {
        try (ConsulDcaeHttpClient client = new ConsulDcaeHttpClient(parameters)) {

            assertTrue(client.deploy("policy", ""));

        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        }
    }
}
