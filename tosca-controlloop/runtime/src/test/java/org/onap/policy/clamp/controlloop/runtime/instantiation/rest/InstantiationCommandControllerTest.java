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

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoops;
import org.onap.policy.clamp.controlloop.models.messages.rest.instantiation.InstantiationCommand;
import org.onap.policy.clamp.controlloop.models.messages.rest.instantiation.InstantiationResponse;
import org.onap.policy.clamp.controlloop.runtime.instantiation.ControlLoopInstantiationProvider;
import org.onap.policy.clamp.controlloop.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.controlloop.runtime.util.rest.CommonRestController;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Class to perform unit test of {@link InstantiationCommandController}}.
 *
 */
public class InstantiationCommandControllerTest extends CommonRestController {

    private static final String CL_INSTANTIATION_CREATE_JSON =
            "src/test/resources/rest/controlloops/ControlLoops.json";

    private static final String CL_INSTANTIATION_CHANGE_STATE_JSON =
            "src/test/resources/rest/controlloops/PassiveCommand.json";

    private static final String GROUP_ENDPOINT = "instantiation/command";

    /**
     * starts Main and inserts a commissioning template.
     *
     * @throws Exception if an error occurs
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        CommonRestController.setUpBeforeClass("testCommand");

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
    public void testUpdate_Unauthorized() throws Exception {
        InstantiationCommand instantiationCommand = InstantiationUtils
                .getInstantiationCommandFromResource(CL_INSTANTIATION_CHANGE_STATE_JSON, "Unauthorized");

        Invocation.Builder invocationBuilder = super.sendNoAuthRequest(GROUP_ENDPOINT);
        Response rawresp = invocationBuilder.put(Entity.json(instantiationCommand));
        Assert.assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), rawresp.getStatus());
    }

    @Test
    public void test_NotFound1() throws Exception {
        Invocation.Builder invocationBuilder = super.sendRequest(GROUP_ENDPOINT);
        Response resp = invocationBuilder.put(Entity.json(new InstantiationCommand()));
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
    }

    @Test
    public void test_NotFound2() throws Exception {
        InstantiationCommand command =
                InstantiationUtils.getInstantiationCommandFromResource(CL_INSTANTIATION_CHANGE_STATE_JSON, "Command");
        command.setOrderedState(null);

        Invocation.Builder invocationBuilder = super.sendRequest(GROUP_ENDPOINT);
        Response resp = invocationBuilder.put(Entity.json(command));
        Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testCommand() throws Exception {
        InstantiationCommand command =
                InstantiationUtils.getInstantiationCommandFromResource(CL_INSTANTIATION_CHANGE_STATE_JSON, "Command");

        Invocation.Builder invocationBuilder = super.sendRequest(GROUP_ENDPOINT);
        Response resp = invocationBuilder.put(Entity.json(command));
        Assert.assertEquals(Response.Status.ACCEPTED.getStatusCode(), resp.getStatus());
        InstantiationResponse instResponse = resp.readEntity(InstantiationResponse.class);
        InstantiationUtils.assertInstantiationResponse(instResponse, command);

        // check passive state on DB
        try (ControlLoopInstantiationProvider instantiationProvider =
                new ControlLoopInstantiationProvider(getParameters())) {
            for (ToscaConceptIdentifier toscaConceptIdentifier : command.getControlLoopIdentifierList()) {
                ControlLoops controlLoopsGet = instantiationProvider.getControlLoops(toscaConceptIdentifier.getName(),
                        toscaConceptIdentifier.getVersion());
                assertThat(controlLoopsGet.getControlLoopList()).hasSize(1);
                Assert.assertEquals(command.getOrderedState(),
                        controlLoopsGet.getControlLoopList().get(0).getOrderedState());
            }
        }
    }
}
