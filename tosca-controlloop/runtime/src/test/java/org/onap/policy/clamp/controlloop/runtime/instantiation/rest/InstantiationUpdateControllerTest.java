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

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoops;
import org.onap.policy.clamp.controlloop.models.messages.rest.instantiation.InstantiationResponse;
import org.onap.policy.clamp.controlloop.runtime.instantiation.ControlLoopInstantiationProvider;
import org.onap.policy.clamp.controlloop.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.controlloop.runtime.util.rest.CommonRestController;

/**
 * Class to perform unit test of {@link InstantiationUpdateController}}.
 *
 */
public class InstantiationUpdateControllerTest extends CommonRestController {

    private static final String CL_INSTANTIATION_CREATE_JSON =
            "src/test/resources/rest/controlloops/ControlLoops.json";

    private static final String CL_INSTANTIATION_UPDATE_JSON =
            "src/test/resources/rest/controlloops/ControlLoopsUpdate.json";

    private static final String GROUP_ENDPOINT = "instantiation";

    /**
     * starts Main and inserts a commissioning template.
     *
     * @throws Exception if an error occurs
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        CommonRestController.setUpBeforeClass("testUpdate");
    }

    @AfterClass
    public static void teardownAfterClass() {
        CommonRestController.teardownAfterClass();
    }

    @Test
    public void testUpdate_Unauthorized() throws Exception {
        ControlLoops controlLoops =
                InstantiationUtils.getControlLoopsFromResource(CL_INSTANTIATION_UPDATE_JSON, "Unauthorized");

        Invocation.Builder invocationBuilder = super.sendNoAuthRequest(GROUP_ENDPOINT);
        Response rawresp = invocationBuilder.put(Entity.json(controlLoops));
        Assert.assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), rawresp.getStatus());
    }

    @Test
    public void testUpdate() throws Exception {
        ControlLoops controlLoopsCreate =
                InstantiationUtils.getControlLoopsFromResource(CL_INSTANTIATION_CREATE_JSON, "Update");

        ControlLoops controlLoops =
                InstantiationUtils.getControlLoopsFromResource(CL_INSTANTIATION_UPDATE_JSON, "Update");

        try (ControlLoopInstantiationProvider instantiationProvider =
                new ControlLoopInstantiationProvider(getParameters())) {
            instantiationProvider.createControlLoops(controlLoopsCreate);

            Invocation.Builder invocationBuilder = super.sendRequest(GROUP_ENDPOINT);
            Response resp = invocationBuilder.put(Entity.json(controlLoops));
            Assert.assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());

            InstantiationResponse instResponse = resp.readEntity(InstantiationResponse.class);
            InstantiationUtils.assertInstantiationResponse(instResponse, controlLoops);

            for (ControlLoop controlLoopUpdate : controlLoops.getControlLoopList()) {
                ControlLoops controlLoopsFromDb = instantiationProvider
                        .getControlLoops(controlLoopUpdate.getKey().getName(), controlLoopUpdate.getKey().getVersion());

                Assert.assertNotNull(controlLoopsFromDb);
                Assert.assertEquals(1, controlLoopsFromDb.getControlLoopList().size());
                Assert.assertEquals(controlLoopUpdate, controlLoopsFromDb.getControlLoopList().get(0));
            }

        }

    }
}
