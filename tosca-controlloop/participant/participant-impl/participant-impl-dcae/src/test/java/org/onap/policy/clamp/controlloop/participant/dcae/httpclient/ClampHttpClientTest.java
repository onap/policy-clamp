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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockserver.integration.ClientAndServer;
import org.onap.policy.clamp.controlloop.participant.dcae.main.parameters.CommonTestData;
import org.onap.policy.clamp.controlloop.participant.dcae.main.parameters.ParticipantDcaeParameters;
import org.onap.policy.clamp.controlloop.participant.dcae.model.Loop;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.StandardCoder;

/**
 * Class to perform unit test of {@link ClampHttpClient}.
 *
 */
public class ClampHttpClientTest {

    private static final String LOOP = "pmsh_loop";
    private static final String BLUEPRINT_DEPLOYED = "BLUEPRINT_DEPLOYED";

    private static ClientAndServer mockServer;
    private static ParticipantDcaeParameters parameters;
    public static final Coder CODER = new StandardCoder();

    /**
     * Set up.
     */
    @BeforeClass
    public static void setUp() {
        CommonTestData commonTestData = new CommonTestData();

        parameters = commonTestData.toObject(
                commonTestData.getParticipantParameterGroupMap(CommonTestData.PARTICIPANT_GROUP_NAME),
                ParticipantDcaeParameters.class);

        mockServer = ClientAndServer.startClientAndServer(parameters.getClampClientParameters().getPort());

        mockServer.when(request().withMethod("GET").withPath("/restservices/clds/v2/loop/getstatus/" + LOOP))
                .respond(response().withBody(CommonTestData.createJsonStatus(BLUEPRINT_DEPLOYED)).withStatusCode(200));

        mockServer.when(request().withMethod("PUT").withPath("/restservices/clds/v2/loop/deploy/" + LOOP))
                .respond(response().withStatusCode(202));

        mockServer.when(request().withMethod("PUT").withPath("/restservices/clds/v2/loop/undeploy/" + LOOP))
                .respond(response().withStatusCode(202));
    }

    @AfterClass
    public static void stopServer() {
        mockServer.stop();
        mockServer = null;
    }

    @Test
    public void test_getstatus() throws Exception {
        try (ClampHttpClient client = new ClampHttpClient(parameters.getClampClientParameters())) {

            Loop status = client.getstatus(LOOP);

            String json = CommonTestData.createJsonStatus(BLUEPRINT_DEPLOYED);
            Loop loop = CODER.convert(json, Loop.class);

            assertThat(ClampHttpClient.getStatusCode(status)).isEqualTo(ClampHttpClient.getStatusCode(loop));

        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void test_deploy() throws Exception {
        try (ClampHttpClient client = new ClampHttpClient(parameters.getClampClientParameters())) {

            assertTrue(client.deploy(LOOP));

        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void test_undeploy() throws Exception {
        try (ClampHttpClient client = new ClampHttpClient(parameters.getClampClientParameters())) {

            assertTrue(client.undeploy(LOOP));

        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void test_getStatusCodeNull() {
        assertThat(ClampHttpClient.getStatusCode(null)).isEqualTo(ClampHttpClient.STATUS_NOT_FOUND);
    }

    @Test
    public void test_getStatusEmptyMap() {
        assertThat(ClampHttpClient.getStatusCode(new Loop())).isEqualTo(ClampHttpClient.STATUS_NOT_FOUND);
    }
}
