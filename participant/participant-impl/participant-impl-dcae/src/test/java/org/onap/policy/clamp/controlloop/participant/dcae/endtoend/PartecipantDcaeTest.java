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

package org.onap.policy.clamp.controlloop.participant.dcae.endtoend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.integration.ClientAndServer;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantControlLoopStateChange;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantControlLoopUpdate;
import org.onap.policy.clamp.controlloop.participant.dcae.main.parameters.CommonTestData;
import org.onap.policy.clamp.controlloop.participant.dcae.main.rest.TestListenerUtils;
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.controlloop.participant.intermediary.comm.ControlLoopStateChangeListener;
import org.onap.policy.clamp.controlloop.participant.intermediary.comm.ControlLoopUpdateListener;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.utils.coder.CoderException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = {"classpath:application_test.properties"})
class PartecipantDcaeTest {

    private static final CommInfrastructure INFRA = CommInfrastructure.NOOP;
    private static final String TOPIC = "my-topic";

    private static final String LOOP = "pmsh_loop";
    private static final String BLUEPRINT_DEPLOYED = "BLUEPRINT_DEPLOYED";

    private static ClientAndServer mockClampServer;
    private static ClientAndServer mockConsulServer;

    @Autowired
    private ParticipantIntermediaryApi participantIntermediaryApi;

    /**
     * start Servers.
     */
    @BeforeAll
    public static void startServers() {

        // Clamp
        mockClampServer = ClientAndServer.startClientAndServer(8443);

        mockClampServer.when(request().withMethod("GET").withPath("/restservices/clds/v2/loop/getstatus/" + LOOP))
                .respond(response().withBody(CommonTestData.createJsonStatus(BLUEPRINT_DEPLOYED).toString())
                        .withStatusCode(200));

        mockClampServer.when(request().withMethod("PUT").withPath("/restservices/clds/v2/loop/deploy/" + LOOP))
                .respond(response().withStatusCode(202));

        mockClampServer.when(request().withMethod("PUT").withPath("/restservices/clds/v2/loop/undeploy/" + LOOP))
                .respond(response().withStatusCode(202));

        // Consul
        mockConsulServer = ClientAndServer.startClientAndServer(31321);

        mockConsulServer.when(request().withMethod("PUT").withPath("/v1/kv/dcae-pmsh:policy"))
                .respond(response().withStatusCode(200));
    }

    /**
     * stop Server.
     */
    @AfterAll
    public static void stopServer() {
        mockClampServer.stop();
        mockClampServer = null;

        mockConsulServer.stop();
        mockConsulServer = null;
    }

    @Test
    void testParticipantControlLoopStateChangeMessageListener() {
        ParticipantControlLoopStateChange participantControlLoopStateChangeMsg =
                TestListenerUtils.createControlLoopStateChangeMsg(ControlLoopOrderedState.UNINITIALISED);
        participantControlLoopStateChangeMsg.setOrderedState(ControlLoopOrderedState.PASSIVE);

        ControlLoopStateChangeListener clStateChangeListener =
                new ControlLoopStateChangeListener(participantIntermediaryApi.getParticipantHandler());

        clStateChangeListener.onTopicEvent(INFRA, TOPIC, null, participantControlLoopStateChangeMsg);
        assertEquals(ControlLoopOrderedState.PASSIVE, participantControlLoopStateChangeMsg.getOrderedState());

        participantControlLoopStateChangeMsg.setOrderedState(ControlLoopOrderedState.RUNNING);
        clStateChangeListener.onTopicEvent(INFRA, TOPIC, null, participantControlLoopStateChangeMsg);
        assertEquals(ControlLoopOrderedState.RUNNING, participantControlLoopStateChangeMsg.getOrderedState());

        participantControlLoopStateChangeMsg.setOrderedState(ControlLoopOrderedState.RUNNING);
        clStateChangeListener.onTopicEvent(INFRA, TOPIC, null, participantControlLoopStateChangeMsg);
        assertEquals(ControlLoopOrderedState.RUNNING, participantControlLoopStateChangeMsg.getOrderedState());
    }

    @Test
    void testControlLoopUpdateListener_ParticipantIdNoMatch() throws CoderException {
        ParticipantControlLoopUpdate participantControlLoopUpdateMsg = TestListenerUtils.createControlLoopUpdateMsg();
        participantControlLoopUpdateMsg.getParticipantId().setName("DummyName");
        participantControlLoopUpdateMsg.getControlLoop().setOrderedState(ControlLoopOrderedState.PASSIVE);

        ControlLoopUpdateListener clUpdateListener =
                new ControlLoopUpdateListener(participantIntermediaryApi.getParticipantHandler());
        clUpdateListener.onTopicEvent(INFRA, TOPIC, null, participantControlLoopUpdateMsg);

        // Verify the content in participantHandler
        assertNotEquals(participantControlLoopUpdateMsg.getParticipantId().getName(),
                participantIntermediaryApi.getParticipantHandler().getParticipantId().getName());
    }

    @Test
    void testControlLoopUpdateListenerPassive() throws CoderException {
        ParticipantControlLoopUpdate participantControlLoopUpdateMsg = TestListenerUtils.createControlLoopUpdateMsg();
        participantControlLoopUpdateMsg.getControlLoop().setOrderedState(ControlLoopOrderedState.PASSIVE);

        ControlLoopUpdateListener clUpdateListener =
                new ControlLoopUpdateListener(participantIntermediaryApi.getParticipantHandler());
        clUpdateListener.onTopicEvent(INFRA, TOPIC, null, participantControlLoopUpdateMsg);

        // Verify the content in participantHandler
        assertEquals(participantIntermediaryApi.getParticipantHandler().getParticipantId(),
                participantControlLoopUpdateMsg.getParticipantId());
        assertEquals(1, participantIntermediaryApi.getParticipantHandler().getControlLoopHandler().getControlLoops()
                .getControlLoopList().size());
    }

    @Test
    void testControlLoopUpdateListenerUninitialised() throws CoderException {
        ParticipantControlLoopUpdate participantControlLoopUpdateMsg = TestListenerUtils.createControlLoopUpdateMsg();
        participantControlLoopUpdateMsg.getControlLoop().setOrderedState(ControlLoopOrderedState.UNINITIALISED);

        ControlLoopUpdateListener clUpdateListener =
                new ControlLoopUpdateListener(participantIntermediaryApi.getParticipantHandler());
        clUpdateListener.onTopicEvent(INFRA, TOPIC, null, participantControlLoopUpdateMsg);

        // Verify the content in participantHandler
        assertEquals(participantIntermediaryApi.getParticipantHandler().getParticipantId(),
                participantControlLoopUpdateMsg.getParticipantId());
        assertEquals(1, participantIntermediaryApi.getParticipantHandler().getControlLoopHandler().getControlLoops()
                .getControlLoopList().size());
    }
}
