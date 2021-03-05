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
 * Class to perform unit test of {@link InstantiationDeleteController}}.
 *
 */
public class InstantiationDeleteControllerTest extends CommonRestController {

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
        CommonRestController.setUpBeforeClass("testDelete");
    }

    @AfterClass
    public static void teardownAfterClass() {
        CommonRestController.teardownAfterClass();
    }

    @Test
    public void testDelete_Unauthorized() throws Exception {
        assertUnauthorizedDelete(GROUP_ENDPOINT);
    }

    @Test
    public void testDelete_NoResultWithThisName() throws Exception {
        Invocation.Builder invocationBuilder = super.sendRequest(GROUP_ENDPOINT + "?name=noResultWithThisName");
        Response resp = invocationBuilder.delete();
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), resp.getStatus());
        InstantiationResponse instResponse = resp.readEntity(InstantiationResponse.class);
        assertNotNull(instResponse.getErrorDetails());
        assertNull(instResponse.getAffectedControlLoops());
    }

    @Test
    public void testDelete() throws Exception {
        ControlLoops controlLoopsFromRsc =
                InstantiationUtils.getControlLoopsFromResource(CL_INSTANTIATION_CREATE_JSON, "Delete");
        try (ControlLoopInstantiationProvider instantiationProvider =
                new ControlLoopInstantiationProvider(getParameters())) {
            instantiationProvider.createControlLoops(controlLoopsFromRsc);

            for (ControlLoop controlLoopFromRsc : controlLoopsFromRsc.getControlLoopList()) {
                Invocation.Builder invocationBuilder =
                        super.sendRequest(GROUP_ENDPOINT + "?name=" + controlLoopFromRsc.getKey().getName()
                                + "&version=" + controlLoopFromRsc.getKey().getVersion());
                Response resp = invocationBuilder.delete();
                assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
                InstantiationResponse instResponse = resp.readEntity(InstantiationResponse.class);
                InstantiationUtils.assertInstantiationResponse(instResponse, controlLoopFromRsc);

                ControlLoops controlLoopsFromDb = instantiationProvider.getControlLoops(
                        controlLoopFromRsc.getKey().getName(), controlLoopFromRsc.getKey().getVersion());
                assertThat(controlLoopsFromDb.getControlLoopList()).isEmpty();
            }
        }
    }

    @Test
    public void testDeleteBadRequest() throws Exception {
        ControlLoops controlLoopsFromRsc =
                InstantiationUtils.getControlLoopsFromResource(CL_INSTANTIATION_CREATE_JSON, "BadRequest");
        try (ControlLoopInstantiationProvider instantiationProvider =
                new ControlLoopInstantiationProvider(getParameters())) {
            instantiationProvider.createControlLoops(controlLoopsFromRsc);

            for (ControlLoop controlLoopFromRsc : controlLoopsFromRsc.getControlLoopList()) {
                Invocation.Builder invocationBuilder =
                        super.sendRequest(GROUP_ENDPOINT + "?name=" + controlLoopFromRsc.getKey().getName());
                Response resp = invocationBuilder.delete();
                // should be BAD_REQUEST
                assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), resp.getStatus());
            }
        }
    }
}
