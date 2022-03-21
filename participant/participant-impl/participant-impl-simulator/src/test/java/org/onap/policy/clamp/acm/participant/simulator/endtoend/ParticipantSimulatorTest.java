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

package org.onap.policy.clamp.acm.participant.simulator.endtoend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.acm.participant.intermediary.comm.AutomationCompositionUpdateListener;
import org.onap.policy.clamp.acm.participant.intermediary.handler.ParticipantHandler;
import org.onap.policy.clamp.acm.participant.simulator.main.parameters.CommonTestData;
import org.onap.policy.clamp.acm.participant.simulator.main.rest.AbstractRestController;
import org.onap.policy.clamp.acm.participant.simulator.utils.TestListenerUtils;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionUpdate;
import org.onap.policy.clamp.models.acm.messages.rest.TypedSimpleResponse;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.gson.GsonMessageBodyHandler;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ParticipantSimulatorTest {

    private static final String PARTICIPANTS_ENDPOINT = "participants";
    private static final String ELEMENTS_ENDPOINT = "elements";
    private static final CommInfrastructure INFRA = CommInfrastructure.NOOP;
    private static final String TOPIC = "my-topic";

    @Value("${spring.security.user.name}")
    private String user;

    @Value("${spring.security.user.password}")
    private String password;

    @LocalServerPort
    private int randomServerPort;

    @Autowired
    private ParticipantIntermediaryApi participantIntermediaryApi;

    @Autowired
    private ParticipantHandler participantHandler;

    private static final Object lockit = new Object();
    private boolean check = false;

    private void setUp() {
        synchronized (lockit) {
            if (!check) {
                check = true;
                AutomationCompositionUpdateListener acUpdateListener =
                    new AutomationCompositionUpdateListener(participantHandler);

                AutomationCompositionUpdate automationCompositionUpdateMsg =
                    TestListenerUtils.createAutomationCompositionUpdateMsg();
                acUpdateListener.onTopicEvent(INFRA, TOPIC, null, automationCompositionUpdateMsg);
            }
        }
    }

    @Test
    void testEndParticipantsSwagger() {
        testSwagger(PARTICIPANTS_ENDPOINT);
    }

    @Test
    void testElementsSwagger() {
        testSwagger(ELEMENTS_ENDPOINT);
    }

    @Test
    void testProducerYaml() {
        final Client client = ClientBuilder.newBuilder().build();

        client.property(ClientProperties.METAINF_SERVICES_LOOKUP_DISABLE, "true");
        client.register(GsonMessageBodyHandler.class);
        client.register(HttpAuthenticationFeature.basic(user, password));

        String path = getPath(PARTICIPANTS_ENDPOINT + "/org.onap.PM_CDS_Blueprint/1");
        final WebTarget webTarget = client.target(path);

        Response response = webTarget.request("application/yaml").get();

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void testQuery_Unauthorized() {
        String path = PARTICIPANTS_ENDPOINT + "/org.onap.PM_CDS_Blueprint/1";

        Response response = performRequest(path, true, null).get();
        assertThat(response.getStatus()).isEqualTo(200);

        // unauthorized call
        response = performRequest(path, false, null).get();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void testQueryParticipants() {
        Participant participant = new Participant();
        ToscaConceptIdentifier participantId = CommonTestData.getParticipantId();
        participant.setDefinition(participantId);
        participant.setName(participantId.getName());
        participant.setVersion(participantId.getVersion());
        UUID uuid = UUID.randomUUID();

        // GET REST call for querying the participants
        Response response = performGet(
            PARTICIPANTS_ENDPOINT + "/" + participant.getKey().getName() + "/" + participant.getKey().getVersion(),
            uuid);
        checkResponseEntity(response, uuid);

        Participant[] returnValue = response.readEntity(Participant[].class);
        assertThat(returnValue).hasSize(1);
        // Verify the result of GET participants with what is stored
        assertEquals(participant.getDefinition(), returnValue[0].getDefinition());
    }

    @Test
    void testQueryAutomationCompositionElements() {
        setUp();
        UUID uuid = UUID.randomUUID();
        ToscaConceptIdentifier participantId = CommonTestData.getParticipantId();

        // GET REST call for querying the automationComposition elements
        Response response =
            performGet(ELEMENTS_ENDPOINT + "/" + participantId.getName() + "/" + participantId.getVersion(), uuid);
        checkResponseEntity(response, uuid);

        Map<?, ?> returnValue = response.readEntity(Map.class);
        // Verify the result of GET automation composition elements with what is stored
        assertThat(returnValue).isEmpty();
    }

    @Test
    void testUpdateParticipant() {
        setUp();
        List<Participant> participants = participantIntermediaryApi.getParticipants(
            CommonTestData.getParticipantId().getName(), CommonTestData.getParticipantId().getVersion());
        assertEquals(ParticipantState.UNKNOWN, participants.get(0).getParticipantState());
        // Change the state of the participant to PASSIVE from UNKNOWN
        participants.get(0).setParticipantState(ParticipantState.PASSIVE);
        UUID uuid = UUID.randomUUID();

        // PUT REST call for updating Participant
        Response response = performPut(PARTICIPANTS_ENDPOINT, Entity.json(participants.get(0)), uuid);
        checkResponseEntity(response, uuid);

        TypedSimpleResponse<Participant> resp = response.readEntity(new GenericType<>() {
        });
        assertNotNull(resp.getResponse());
        // Verify the response and state returned by PUT REST call for updating participants
        assertEquals(participants.get(0).getDefinition(), resp.getResponse().getDefinition());
        assertEquals(ParticipantState.PASSIVE, resp.getResponse().getParticipantState());
    }

    @Test
    void testUpdateAutomationCompositionElement() {
        setUp();
        AutomationComposition automationComposition = TestListenerUtils.createAutomationComposition();
        Map<UUID, AutomationCompositionElement> automationCompositionElements =
            participantIntermediaryApi.getAutomationCompositionElements(automationComposition.getDefinition().getName(),
                automationComposition.getDefinition().getVersion());

        UUID uuid = automationCompositionElements.keySet().iterator().next();
        AutomationCompositionElement automationCompositionElement = automationCompositionElements.get(uuid);

        automationCompositionElement.setOrderedState(AutomationCompositionOrderedState.PASSIVE);
        // PUT REST call for updating AutomationCompositionElement
        Response response = performPut(ELEMENTS_ENDPOINT, Entity.json(automationCompositionElement), uuid);
        checkResponseEntity(response, uuid);

        TypedSimpleResponse<AutomationCompositionElement> resp = response.readEntity(new GenericType<>() {
        });
        assertNotNull(resp.getResponse());
        // Verify the response and state returned by PUT REST call for updating participants
        assertEquals(automationCompositionElement.getDefinition(), resp.getResponse().getDefinition());
        assertEquals(AutomationCompositionOrderedState.PASSIVE, resp.getResponse().getOrderedState());
    }

    private String getPath(String path) {
        return "http://localhost:" + randomServerPort + "/onap/participantsim/v2/" + path;
    }

    void testSwagger(String endPoint) {
        final Client client = ClientBuilder.newBuilder().build();

        client.property(ClientProperties.METAINF_SERVICES_LOOKUP_DISABLE, "true");
        client.register(GsonMessageBodyHandler.class);
        client.register(HttpAuthenticationFeature.basic(user, password));

        final WebTarget webTarget = client.target(getPath("api-docs"));

        Response response = webTarget.request(MediaType.APPLICATION_JSON).get();

        assertThat(response.getStatus()).isEqualTo(200);
        assertTrue(response.readEntity(String.class).contains("/onap/participantsim/v2/" + endPoint));
    }

    private Invocation.Builder performRequest(String endpoint, boolean includeAuth, UUID uuid) {
        final Client client = ClientBuilder.newBuilder().build();

        client.property(ClientProperties.METAINF_SERVICES_LOOKUP_DISABLE, "true");
        client.register(GsonMessageBodyHandler.class);
        if (includeAuth) {
            client.register(HttpAuthenticationFeature.basic(user, password));
        }
        Invocation.Builder builder = client.target(getPath(endpoint)).request(MediaType.APPLICATION_JSON);
        if (uuid != null) {
            builder = builder.header(AbstractRestController.REQUEST_ID_NAME, uuid.toString());
        }
        return builder;
    }

    private Response performGet(String endpoint, UUID uuid) {
        return performRequest(endpoint, true, uuid).get();
    }

    private void checkResponseEntity(Response response, UUID uuid) {
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(getHeader(response.getHeaders(), AbstractRestController.VERSION_MINOR_NAME)).isEqualTo("0");
        assertThat(getHeader(response.getHeaders(), AbstractRestController.VERSION_PATCH_NAME)).isEqualTo("0");
        assertThat(getHeader(response.getHeaders(), AbstractRestController.VERSION_LATEST_NAME)).isEqualTo("1.0.0");
        assertThat(getHeader(response.getHeaders(), AbstractRestController.REQUEST_ID_NAME)).isEqualTo(uuid.toString());
    }

    private String getHeader(MultivaluedMap<String, Object> httpHeaders, String param) {
        List<Object> list = httpHeaders.get(param);
        assertThat(list).hasSize(1);
        assertThat(list.get(0)).isNotNull();
        return (String) list.get(0);
    }

    private Response performPut(String endpoint, final Entity<?> entity, UUID uuid) {
        return performRequest(endpoint, true, uuid).put(entity);
    }
}
