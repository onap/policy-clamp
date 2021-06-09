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

package org.onap.policy.clamp.controlloop.participant.simulator.endtoend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.Participant;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantState;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantControlLoopUpdate;
import org.onap.policy.clamp.controlloop.models.messages.rest.TypedSimpleResponse;
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.controlloop.participant.intermediary.comm.ControlLoopUpdateListener;
import org.onap.policy.clamp.controlloop.participant.simulator.main.parameters.CommonTestData;
import org.onap.policy.clamp.controlloop.participant.simulator.main.rest.AbstractRestController;
import org.onap.policy.clamp.controlloop.participant.simulator.main.rest.TestListenerUtils;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = {"classpath:application_test.properties"})
class ParticipantSimulatorTest {

    private static final String PARTICIPANTS_ENDPOINT = "participants";
    private static final String ELEMENTS_ENDPOINT = "elements";
    private static final CommInfrastructure INFRA = CommInfrastructure.NOOP;
    private static final String TOPIC = "my-topic";

    public static final Coder coder = new StandardCoder();

    @Value("${spring.security.user.name}")
    private String user;

    @Value("${spring.security.user.password}")
    private String password;

    @LocalServerPort
    private int randomServerPort;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ParticipantIntermediaryApi participantIntermediaryApi;

    private static final Object lockit = new Object();
    private boolean check = false;

    private void setUpBeforeClass() throws Exception {
        synchronized (lockit) {
            if (!check) {
                check = true;
                ControlLoopUpdateListener clUpdateListener =
                        new ControlLoopUpdateListener(participantIntermediaryApi.getParticipantHandler());

                ParticipantControlLoopUpdate participantControlLoopUpdateMsg =
                        TestListenerUtils.createControlLoopUpdateMsg();
                participantControlLoopUpdateMsg.getControlLoop().setOrderedState(ControlLoopOrderedState.PASSIVE);
                clUpdateListener.onTopicEvent(INFRA, TOPIC, null, participantControlLoopUpdateMsg);

            }
        }
    }

    private String getPath(String path) {
        return "http://localhost:" + randomServerPort + "/onap/participantsim/v2/" + path;
    }

    void testSwagger(String endPoint) {
        HttpEntity<Void> request = new HttpEntity<>(null, createHttpHeaders());
        ResponseEntity<String> response =
                restTemplate.exchange(getPath("api-docs"), HttpMethod.GET, request, String.class);
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertTrue(response.getBody().contains("/onap/participantsim/v2/" + endPoint));
    }

    @Test
    void testEndParticipatsSwagger() {
        testSwagger(PARTICIPANTS_ENDPOINT);
    }

    @Test
    void testElementsSwagger() {
        testSwagger(ELEMENTS_ENDPOINT);
    }

    @Test
    void testProducerYaml() {
        MediaType yamlMediaType = new MediaType("application", "yaml");
        HttpHeaders headers = createHttpHeaders();
        headers.setAccept(Collections.singletonList(yamlMediaType));
        HttpEntity<Void> request = new HttpEntity<>(null, headers);
        String path = getPath(PARTICIPANTS_ENDPOINT + "/org.onap.PM_CDS_Blueprint/1");

        ResponseEntity<String> response = restTemplate.exchange(path, HttpMethod.GET, request, String.class);

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertTrue(response.getHeaders().getContentType().isCompatibleWith(yamlMediaType));
    }

    @Test
    void testQuery_Unauthorized() throws Exception {
        String path = getPath(PARTICIPANTS_ENDPOINT + "/org.onap.PM_CDS_Blueprint/1");

        // authorized call
        ResponseEntity<String> response =
                restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(null, createHttpHeaders()), String.class);
        assertThat(response.getStatusCodeValue()).isEqualTo(200);

        // unauthorized call
        response = restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(null, new HttpHeaders()), String.class);
        assertThat(response.getStatusCodeValue()).isEqualTo(401);
    }

    private HttpHeaders createHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(user, password);
        return headers;
    }

    protected <T> ResponseEntity<T> performGet(String endpoint, Class<T> responseType, UUID uuid) throws Exception {
        HttpHeaders httpHeaders = createHttpHeaders();
        if (uuid != null) {
            httpHeaders.add(AbstractRestController.REQUEST_ID_NAME, uuid.toString());
        }
        HttpEntity<Void> request = new HttpEntity<>(null, httpHeaders);
        return restTemplate.exchange(getPath(endpoint), HttpMethod.GET, request, responseType);
    }

    protected <T> ResponseEntity<T> performGet(String endpoint, Class<T> responseType) throws Exception {
        return performGet(endpoint, responseType, null);
    }

    protected <T, R> ResponseEntity<R> performPut(String path, T body, ParameterizedTypeReference<R> responseType,
            UUID uuid) throws Exception {
        HttpHeaders httpHeaders = createHttpHeaders();
        if (uuid != null) {
            httpHeaders.add(AbstractRestController.REQUEST_ID_NAME, uuid.toString());
        }
        HttpEntity<T> request = new HttpEntity<>(body, httpHeaders);
        return restTemplate.exchange(getPath(path), HttpMethod.PUT, request, responseType);
    }

    // ParameterizedTypeReference<T> responseType

    @Test
    void testQueryParticipants() throws Exception {
        Participant participant = new Participant();
        ToscaConceptIdentifier participantId = CommonTestData.getParticipantId();
        participant.setDefinition(participantId);
        participant.setName(participantId.getName());
        participant.setVersion(participantId.getVersion());
        UUID uuid = UUID.randomUUID();

        // GET REST call for querying the participants
        ResponseEntity<String> response = performGet(
                PARTICIPANTS_ENDPOINT + "/" + participant.getKey().getName() + "/" + participant.getKey().getVersion(),
                String.class, uuid);
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getHeaders().get(AbstractRestController.VERSION_MINOR_NAME)).contains("0");
        assertThat(response.getHeaders().get(AbstractRestController.VERSION_PATCH_NAME)).contains("0");
        assertThat(response.getHeaders().get(AbstractRestController.VERSION_LATEST_NAME)).contains("1.0.0");
        assertThat(response.getHeaders().get(AbstractRestController.REQUEST_ID_NAME)).contains(uuid.toString());

        Participant[] returnValue = coder.decode(response.getBody(), Participant[].class);
        assertThat(returnValue).hasSize(1);
        // Verify the result of GET participants with what is stored
        assertEquals(returnValue[0].getDefinition(), participant.getDefinition());
    }

    @Test
    void testQueryControlLoopElements() throws Exception {
        setUpBeforeClass();
        UUID uuid = UUID.randomUUID();
        ToscaConceptIdentifier participantId = CommonTestData.getParticipantId();

        // GET REST call for querying the controlLoop elements
        ResponseEntity<String> response =
                performGet(ELEMENTS_ENDPOINT + "/" + participantId.getName() + "/" + participantId.getVersion(),
                        String.class, uuid);
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getHeaders().get(AbstractRestController.VERSION_MINOR_NAME)).contains("0");
        assertThat(response.getHeaders().get(AbstractRestController.VERSION_PATCH_NAME)).contains("0");
        assertThat(response.getHeaders().get(AbstractRestController.VERSION_LATEST_NAME)).contains("1.0.0");
        assertThat(response.getHeaders().get(AbstractRestController.REQUEST_ID_NAME)).contains(uuid.toString());

        Map returnValue = coder.decode(response.getBody(), Map.class);
        // Verify the result of GET controlloop elements with what is stored
        // assertThat(returnValue).hasSize(5);
    }

    @Test
    void testUpdateParticipant() throws Exception {
        setUpBeforeClass();
        List<Participant> participants = participantIntermediaryApi.getParticipants(
                CommonTestData.getParticipantId().getName(), CommonTestData.getParticipantId().getVersion());
        assertEquals(ParticipantState.UNKNOWN, participants.get(0).getParticipantState());
        // Change the state of the participant to PASSIVE from UNKNOWN
        participants.get(0).setParticipantState(ParticipantState.PASSIVE);
        UUID uuid = UUID.randomUUID();

        // PUT REST call for updating Participant
        ResponseEntity<TypedSimpleResponse<Participant>> response = performPut(PARTICIPANTS_ENDPOINT,
                participants.get(0), new ParameterizedTypeReference<TypedSimpleResponse<Participant>>() {}, uuid);
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getHeaders().get(AbstractRestController.VERSION_MINOR_NAME)).contains("0");
        assertThat(response.getHeaders().get(AbstractRestController.VERSION_PATCH_NAME)).contains("0");
        assertThat(response.getHeaders().get(AbstractRestController.VERSION_LATEST_NAME)).contains("1.0.0");
        assertThat(response.getHeaders().get(AbstractRestController.REQUEST_ID_NAME)).contains(uuid.toString());

        TypedSimpleResponse<Participant> resp = response.getBody();
        assertNotNull(resp.getResponse());
        // Verify the response and state returned by PUT REST call for updating participants
        assertEquals(participants.get(0).getDefinition(), resp.getResponse().getDefinition());
        assertEquals(ParticipantState.PASSIVE, resp.getResponse().getParticipantState());
    }

    @Test
    @Disabled
    void testUpdateControlLoopElement() throws Exception {
        setUpBeforeClass();
        ControlLoop controlLoop = TestListenerUtils.createControlLoop();
        Map<UUID, ControlLoopElement> controlLoopElements = participantIntermediaryApi.getControlLoopElements(
                controlLoop.getDefinition().getName(), controlLoop.getDefinition().getVersion());

        // Check the initial state on the ControlLoopElement, which is UNINITIALISED
        assertEquals(ControlLoopOrderedState.UNINITIALISED, controlLoopElements.get(0).getOrderedState());

        // Change the state of the ControlLoopElement to PASSIVE from UNINITIALISED
        controlLoopElements.get(0).setOrderedState(ControlLoopOrderedState.PASSIVE);
        UUID uuid = UUID.randomUUID();

        // PUT REST call for updating ControlLoopElement
        ResponseEntity<TypedSimpleResponse<ControlLoopElement>> response =
                performPut(ELEMENTS_ENDPOINT, controlLoopElements.get(0),
                        new ParameterizedTypeReference<TypedSimpleResponse<ControlLoopElement>>() {}, uuid);
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getHeaders().get(AbstractRestController.VERSION_MINOR_NAME)).contains("0");
        assertThat(response.getHeaders().get(AbstractRestController.VERSION_PATCH_NAME)).contains("0");
        assertThat(response.getHeaders().get(AbstractRestController.VERSION_LATEST_NAME)).contains("1.0.0");
        assertThat(response.getHeaders().get(AbstractRestController.REQUEST_ID_NAME)).contains(uuid.toString());

        TypedSimpleResponse<ControlLoopElement> resp = response.getBody();
        assertNotNull(resp.getResponse());
        // Verify the response and state returned by PUT REST call for updating participants
        assertEquals(controlLoopElements.get(0).getDefinition(), resp.getResponse().getDefinition());
        assertEquals(ControlLoopOrderedState.PASSIVE, resp.getResponse().getOrderedState());
    }
}
