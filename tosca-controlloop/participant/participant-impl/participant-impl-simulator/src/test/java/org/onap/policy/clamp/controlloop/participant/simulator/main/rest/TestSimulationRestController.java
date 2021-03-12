/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.participant.simulator.main.rest;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.Participant;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantState;
import org.onap.policy.clamp.controlloop.models.messages.rest.TypedSimpleResponse;
import org.onap.policy.clamp.controlloop.participant.simulator.main.parameters.CommonTestData;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Class to perform unit test of {@link TestSimulationRestController}.
 */
public class TestSimulationRestController extends CommonParticipantRestServer {
    private static final String PARTICIPANTS_ENDPOINT = "participants";
    private static final String ELEMENTS_ENDPOINT = "elements";
    private static final CommInfrastructure INFRA = CommInfrastructure.NOOP;
    private static final String TOPIC = "my-topic";
    static CommonTestData commonTestData = new CommonTestData();
    private static RestController restController;

    /**
     * Setup before class, instantiate SimulationProvider.
     *
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        CommonParticipantRestServer.setUpBeforeClass();
    }

    @AfterClass
    public static void teardownAfterClass() {
        CommonParticipantRestServer.teardownAfterClass();
    }

    @Test
    public void testSwagger() throws Exception {
        super.testSwagger(ELEMENTS_ENDPOINT);
    }

    @Test
    public void testQuery_Unauthorized() throws Exception {
        Invocation.Builder invocationBuilder = super.sendNoAuthRequest(ELEMENTS_ENDPOINT);
        Response rawresp = invocationBuilder.buildGet().invoke();
        Assert.assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), rawresp.getStatus());
    }

    @Test
    public void testQueryParticipants() throws Exception {
        Participant participant = new Participant();
        ToscaConceptIdentifier participantId = CommonTestData.getParticipantId();
        participant.setDefinition(participantId);
        participant.setName(participantId.getName());
        participant.setVersion(participantId.getVersion());

        // GET REST call for querying the participants
        Invocation.Builder invocationBuilder =
                super.sendRequest(PARTICIPANTS_ENDPOINT + "/" + participant.getKey().getName()
                + "/" + participant.getVersion());

        Response rawresp = invocationBuilder.buildGet().invoke();
        // Response is not OK, as handling is not done
        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), rawresp.getStatus());
    }

    @Test
    public void testQueryControlLoopElements() throws Exception {
        // GET REST call for querying the controlLoop elements
        Invocation.Builder invocationBuilder =
                super.sendRequest(ELEMENTS_ENDPOINT + "/" + "PMSHInstance0" + "/" + "1.0.0");

        Response rawresp = invocationBuilder.buildGet().invoke();
        // Response is not OK, as handling is not done
        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), rawresp.getStatus());
    }

    @Test
    public void testUpdateParticipant() throws Exception {
        Participant participant = new Participant();
        ToscaConceptIdentifier participantId = CommonTestData.getParticipantId();
        participant.setDefinition(participantId);
        participant.setName(participantId.getName());
        participant.setVersion(participantId.getVersion());

        List<Participant> participants = new ArrayList<>();
        participants.add(participant);
        assertEquals(ParticipantState.UNKNOWN, participants.get(0).getParticipantState());
        // Change the state of the participant to PASSIVE from UNKNOWN
        participants.get(0).setParticipantState(ParticipantState.PASSIVE);
        Entity<Participant> entParticipant = Entity.entity(participants.get(0), MediaType.APPLICATION_JSON);

        // PUT REST call for updating Participant
        Invocation.Builder invocationBuilder = sendRequest(PARTICIPANTS_ENDPOINT);
        Response rawresp = invocationBuilder.put(entParticipant);
        TypedSimpleResponse<Participant> resp = rawresp.readEntity(TypedSimpleResponse.class);
        // Response is not OK, as handling is not done
        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), rawresp.getStatus());
    }

    @Test
    public void testUpdateControlLoopElement() throws Exception {
        ControlLoop controlLoop = TestListenerUtils.createControlLoop();
        List<ControlLoopElement> controlLoopElements = controlLoop.getElements();

        // Check the initial state on the ControlLoopElement, which is UNINITIALISED
        assertEquals(ControlLoopOrderedState.UNINITIALISED, controlLoopElements.get(0).getOrderedState());

        // Change the state of the ControlLoopElement to PASSIVE from UNINITIALISED
        controlLoopElements.get(0).setOrderedState(ControlLoopOrderedState.PASSIVE);
        Entity<ControlLoopElement> entClElement = Entity.entity(controlLoopElements.get(0), MediaType.APPLICATION_JSON);

        // PUT REST call for updating ControlLoopElement
        Invocation.Builder invocationBuilder = sendRequest(ELEMENTS_ENDPOINT);
        Response rawresp = invocationBuilder.put(entClElement);
        TypedSimpleResponse<ControlLoopElement> resp = rawresp.readEntity(TypedSimpleResponse.class);
        // Response is not OK, as handling is not done
        Assert.assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), rawresp.getStatus());
    }
}

