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

package org.onap.policy.clamp.controlloop.runtime.instantiation.rest;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoops;
import org.onap.policy.clamp.controlloop.runtime.instantiation.ControlLoopInstantiationProvider;
import org.onap.policy.clamp.controlloop.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.controlloop.runtime.util.rest.CommonRestController;

/**
 * Class to perform unit test of {@link InstantiationQueryController}}.
 *
 */
public class InstantiationQueryControllerTest extends CommonRestController {

    private static final String CL_INSTANTIATION_CREATE_JSON =
            "src/test/resources/rest/controlloops/ControlLoops.json";

    private static final String GROUP_ENDPOINT = "instantiation";

    /**
     * starts Main and inserts a commissioning template.
     *
     * @throws Exception if an error occurs
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        CommonRestController.setUpBeforeClass("testQuery");
    }

    @AfterClass
    public static void teardownAfterClass() {
        CommonRestController.teardownAfterClass();
    }

    @Test
    public void testSwagger() throws Exception {
        super.testSwagger(GROUP_ENDPOINT);
    }

    @Test
    public void testQuery_Unauthorized() throws Exception {
        Invocation.Builder invocationBuilder = super.sendNoAuthRequest(GROUP_ENDPOINT);
        Response rawresp = invocationBuilder.buildGet().invoke();
        Assert.assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), rawresp.getStatus());
    }

    @Test
    public void testQuery_NoResultWithThisName() throws Exception {
        Invocation.Builder invocationBuilder = super.sendRequest(GROUP_ENDPOINT + "?name=noResultWithThisName");
        Response rawresp = invocationBuilder.buildGet().invoke();
        Assert.assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        ControlLoops resp = rawresp.readEntity(ControlLoops.class);
        Assert.assertTrue(resp.getControlLoopList().isEmpty());
    }

    @Test
    public void testQuery() throws Exception {
        // inserts a ControlLoops to DB
        ControlLoops controlLoops =
                InstantiationUtils.getControlLoopsFromResource(CL_INSTANTIATION_CREATE_JSON, "Query");
        try (ControlLoopInstantiationProvider instantiationProvider =
                new ControlLoopInstantiationProvider(getParameters())) {
            instantiationProvider.createControlLoops(controlLoops);
        }

        for (ControlLoop controlLoopFromRsc : controlLoops.getControlLoopList()) {
            Invocation.Builder invocationBuilder =
                    super.sendRequest(GROUP_ENDPOINT + "?name=" + controlLoopFromRsc.getKey().getName());
            Response rawresp = invocationBuilder.buildGet().invoke();
            Assert.assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
            ControlLoops controlLoopsQuery = rawresp.readEntity(ControlLoops.class);
            Assert.assertNotNull(controlLoopsQuery);
            Assert.assertEquals(1, controlLoopsQuery.getControlLoopList().size());
            Assert.assertEquals(controlLoopFromRsc, controlLoopsQuery.getControlLoopList().get(0));
        }
    }
}
