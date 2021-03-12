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
import org.onap.policy.clamp.controlloop.models.messages.rest.instantiation.InstantiationCommand;
import org.onap.policy.clamp.controlloop.models.messages.rest.instantiation.InstantiationResponse;
import org.onap.policy.clamp.controlloop.runtime.instantiation.ControlLoopInstantiationProvider;
import org.onap.policy.clamp.controlloop.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.controlloop.runtime.util.rest.CommonRestController;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Class to perform unit test of {@link InstantiationController}}.
 *
 */
public class InstantiationControllerTest extends CommonRestController {

    private static final String CL_INSTANTIATION_CREATE_JSON = "src/test/resources/rest/controlloops/ControlLoops.json";

    private static final String CL_INSTANTIATION_UPDATE_JSON =
            "src/test/resources/rest/controlloops/ControlLoopsUpdate.json";

    private static final String CL_INSTANTIATION_CHANGE_STATE_JSON =
            "src/test/resources/rest/controlloops/PassiveCommand.json";

    private static final String INSTANTIATION_ENDPOINT = "instantiation";

    private static final String INSTANTIATION_COMMAND_ENDPOINT = "instantiation/command";

    private static final String TOSCA_TEMPLATE_YAML = "examples/controlloop/PMSubscriptionHandling.yaml";

    /**
     * starts Main and inserts a commissioning template.
     *
     * @throws Exception if an error occurs
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        CommonRestController.setUpBeforeClass("InstApi");

        // to validate control Loop, it needs to define ToscaServiceTemplate
        InstantiationUtils.storeToscaServiceTemplate(TOSCA_TEMPLATE_YAML, getParameters());

        ControlLoops controlLoops =
                InstantiationUtils.getControlLoopsFromResource(CL_INSTANTIATION_CREATE_JSON, "Command");
        try (ControlLoopInstantiationProvider instantiationProvider =
                new ControlLoopInstantiationProvider(getParameters())) {
            instantiationProvider.createControlLoops(controlLoops);
        }
    }

    @AfterClass
    public static void teardownAfterClass() {
        CommonRestController.teardownAfterClass();
    }

    @Test
    public void testSwagger() throws Exception {
        super.testSwagger(INSTANTIATION_ENDPOINT);
    }

    @Test
    public void testCreate_Unauthorized() throws Exception {
        ControlLoops controlLoops =
                InstantiationUtils.getControlLoopsFromResource(CL_INSTANTIATION_CREATE_JSON, "Unauthorized");

        assertUnauthorizedPost(INSTANTIATION_ENDPOINT, Entity.json(controlLoops));
    }

    @Test
    public void testQuery_Unauthorized() throws Exception {
        assertUnauthorizedGet(INSTANTIATION_ENDPOINT);
    }

    @Test
    public void testUpdate_Unauthorized() throws Exception {
        ControlLoops controlLoops =
                InstantiationUtils.getControlLoopsFromResource(CL_INSTANTIATION_UPDATE_JSON, "Unauthorized");

        assertUnauthorizedPut(INSTANTIATION_ENDPOINT, Entity.json(controlLoops));
    }

    @Test
    public void testDelete_Unauthorized() throws Exception {
        assertUnauthorizedDelete(INSTANTIATION_ENDPOINT);
    }

    @Test
    public void testCommand_Unauthorized() throws Exception {
        InstantiationCommand instantiationCommand = InstantiationUtils
                .getInstantiationCommandFromResource(CL_INSTANTIATION_CHANGE_STATE_JSON, "Unauthorized");

        assertUnauthorizedPut(INSTANTIATION_COMMAND_ENDPOINT, Entity.json(instantiationCommand));
    }

    @Test
    public void testCreate() throws Exception {
        ControlLoops controlLoopsFromRsc =
                InstantiationUtils.getControlLoopsFromResource(CL_INSTANTIATION_CREATE_JSON, "Create");

        Invocation.Builder invocationBuilder = super.sendRequest(INSTANTIATION_ENDPOINT);
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
    public void testCreateBadRequest() throws Exception {
        ControlLoops controlLoopsFromRsc =
                InstantiationUtils.getControlLoopsFromResource(CL_INSTANTIATION_CREATE_JSON, "CreateBadRequest");

        Invocation.Builder invocationBuilder = super.sendRequest(INSTANTIATION_ENDPOINT);
        Response resp = invocationBuilder.post(Entity.json(controlLoopsFromRsc));
        assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());

        // testing Bad Request: CL already defined
        resp = invocationBuilder.post(Entity.json(controlLoopsFromRsc));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
        InstantiationResponse instResponse = resp.readEntity(InstantiationResponse.class);
        assertNotNull(instResponse.getErrorDetails());
        assertNull(instResponse.getAffectedControlLoops());
    }

    @Test
    public void testQuery_NoResultWithThisName() throws Exception {
        Invocation.Builder invocationBuilder = super.sendRequest(INSTANTIATION_ENDPOINT + "?name=noResultWithThisName");
        Response rawresp = invocationBuilder.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        ControlLoops resp = rawresp.readEntity(ControlLoops.class);
        assertThat(resp.getControlLoopList()).isEmpty();
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
                    super.sendRequest(INSTANTIATION_ENDPOINT + "?name=" + controlLoopFromRsc.getKey().getName());
            Response rawresp = invocationBuilder.buildGet().invoke();
            assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
            ControlLoops controlLoopsQuery = rawresp.readEntity(ControlLoops.class);
            assertNotNull(controlLoopsQuery);
            assertThat(controlLoopsQuery.getControlLoopList()).hasSize(1);
            assertEquals(controlLoopFromRsc, controlLoopsQuery.getControlLoopList().get(0));
        }
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

            Invocation.Builder invocationBuilder = super.sendRequest(INSTANTIATION_ENDPOINT);
            Response resp = invocationBuilder.put(Entity.json(controlLoops));
            assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());

            InstantiationResponse instResponse = resp.readEntity(InstantiationResponse.class);
            InstantiationUtils.assertInstantiationResponse(instResponse, controlLoops);

            for (ControlLoop controlLoopUpdate : controlLoops.getControlLoopList()) {
                ControlLoops controlLoopsFromDb = instantiationProvider
                        .getControlLoops(controlLoopUpdate.getKey().getName(), controlLoopUpdate.getKey().getVersion());

                assertNotNull(controlLoopsFromDb);
                assertThat(controlLoopsFromDb.getControlLoopList()).hasSize(1);
                assertEquals(controlLoopUpdate, controlLoopsFromDb.getControlLoopList().get(0));
            }
        }
    }

    @Test
    public void testDelete_NoResultWithThisName() throws Exception {
        Invocation.Builder invocationBuilder = super.sendRequest(INSTANTIATION_ENDPOINT + "?name=noResultWithThisName");
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
                        super.sendRequest(INSTANTIATION_ENDPOINT + "?name=" + controlLoopFromRsc.getKey().getName()
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
                InstantiationUtils.getControlLoopsFromResource(CL_INSTANTIATION_CREATE_JSON, "DelBadRequest");
        try (ControlLoopInstantiationProvider instantiationProvider =
                new ControlLoopInstantiationProvider(getParameters())) {
            instantiationProvider.createControlLoops(controlLoopsFromRsc);

            for (ControlLoop controlLoopFromRsc : controlLoopsFromRsc.getControlLoopList()) {
                Invocation.Builder invocationBuilder =
                        super.sendRequest(INSTANTIATION_ENDPOINT + "?name=" + controlLoopFromRsc.getKey().getName());
                Response resp = invocationBuilder.delete();
                // should be BAD_REQUEST
                assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), resp.getStatus());
            }
        }
    }

    @Test
    public void testCommand_NotFound1() throws Exception {
        Invocation.Builder invocationBuilder = super.sendRequest(INSTANTIATION_COMMAND_ENDPOINT);
        Response resp = invocationBuilder.put(Entity.json(new InstantiationCommand()));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testCommand_NotFound2() throws Exception {
        InstantiationCommand command =
                InstantiationUtils.getInstantiationCommandFromResource(CL_INSTANTIATION_CHANGE_STATE_JSON, "Command");
        command.setOrderedState(null);

        Invocation.Builder invocationBuilder = super.sendRequest(INSTANTIATION_COMMAND_ENDPOINT);
        Response resp = invocationBuilder.put(Entity.json(command));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testCommand() throws Exception {
        InstantiationCommand command =
                InstantiationUtils.getInstantiationCommandFromResource(CL_INSTANTIATION_CHANGE_STATE_JSON, "Command");

        Invocation.Builder invocationBuilder = super.sendRequest(INSTANTIATION_COMMAND_ENDPOINT);
        Response resp = invocationBuilder.put(Entity.json(command));
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), resp.getStatus());
        InstantiationResponse instResponse = resp.readEntity(InstantiationResponse.class);
        InstantiationUtils.assertInstantiationResponse(instResponse, command);

        // check passive state on DB
        try (ControlLoopInstantiationProvider instantiationProvider =
                new ControlLoopInstantiationProvider(getParameters())) {
            for (ToscaConceptIdentifier toscaConceptIdentifier : command.getControlLoopIdentifierList()) {
                ControlLoops controlLoopsGet = instantiationProvider.getControlLoops(toscaConceptIdentifier.getName(),
                        toscaConceptIdentifier.getVersion());
                assertThat(controlLoopsGet.getControlLoopList()).hasSize(1);
                assertEquals(command.getOrderedState(), controlLoopsGet.getControlLoopList().get(0).getOrderedState());
            }
        }
    }
}
