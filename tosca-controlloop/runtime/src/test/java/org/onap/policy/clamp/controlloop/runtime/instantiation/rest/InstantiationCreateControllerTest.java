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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoops;
import org.onap.policy.clamp.controlloop.models.messages.rest.instantiation.InstantiationResponse;
import org.onap.policy.clamp.controlloop.runtime.instantiation.ControlLoopInstantiationProvider;
import org.onap.policy.clamp.controlloop.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.controlloop.runtime.util.rest.CommonRestController;

/**
 * Class to perform unit test of {@link InstantiationCreateController}}.
 *
 */
public class InstantiationCreateControllerTest extends CommonRestController {

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
        CommonRestController.setUpBeforeClass("testCreate");
    }

    @AfterClass
    public static void teardownAfterClass() {
        CommonRestController.teardownAfterClass();
    }

    @Test
    public void testCreate_Unauthorized() throws Exception {
        ControlLoops controlLoops =
                InstantiationUtils.getControlLoopsFromResource(CL_INSTANTIATION_CREATE_JSON, "Unauthorized");

        assertUnauthorizedPost(GROUP_ENDPOINT, Entity.json(controlLoops));
    }

    @Test
    public void testCreate() throws Exception {
        ControlLoops controlLoopsFromRsc =
                InstantiationUtils.getControlLoopsFromResource(CL_INSTANTIATION_CREATE_JSON, "Create");

        Invocation.Builder invocationBuilder = super.sendRequest(GROUP_ENDPOINT);
        Response resp = invocationBuilder.post(Entity.json(controlLoopsFromRsc));
        assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
        InstantiationResponse instResponse = resp.readEntity(InstantiationResponse.class);
        InstantiationUtils.assertInstantiationResponse(instResponse, controlLoopsFromRsc);

        try (ControlLoopInstantiationProvider instantiationProvider =
                new ControlLoopInstantiationProvider(getParameters())) {
            for (ControlLoop controlLoopFromRsc : controlLoopsFromRsc.getControlLoopList()) {
                ControlLoops controlLoopsFromDb = instantiationProvider.getControlLoops(
                        controlLoopFromRsc.getKey().getName(), controlLoopFromRsc.getKey().getVersion());

                assertNotNull(controlLoopsFromDb);
                assertThat(controlLoopsFromDb.getControlLoopList()).hasSize(1);
                assertEquals(controlLoopFromRsc, controlLoopsFromDb.getControlLoopList().get(0));
            }
        }
    }

    @Test
    public void testBadRequest() throws Exception {
        ControlLoops controlLoopsFromRsc =
                InstantiationUtils.getControlLoopsFromResource(CL_INSTANTIATION_CREATE_JSON, "BadRequest");

        Invocation.Builder invocationBuilder = super.sendRequest(GROUP_ENDPOINT);
        Response resp = invocationBuilder.post(Entity.json(controlLoopsFromRsc));
        assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());

        // testing Bad Request: CL already defined
        resp = invocationBuilder.post(Entity.json(controlLoopsFromRsc));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
        InstantiationResponse instResponse = resp.readEntity(InstantiationResponse.class);
        assertNotNull(instResponse.getErrorDetails());
        assertNull(instResponse.getAffectedControlLoops());
    }
}
