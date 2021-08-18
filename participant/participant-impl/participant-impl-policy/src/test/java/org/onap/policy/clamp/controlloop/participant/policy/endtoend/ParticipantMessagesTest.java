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

package org.onap.policy.clamp.controlloop.participant.policy.endtoend;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ClElementStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ClElementStatisticsList;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElementDefinition;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopInfo;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantDefinition;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ControlLoopUpdate;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantDeregister;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantDeregisterAck;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantRegister;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantRegisterAck;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantStatus;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantUpdate;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantUpdateAck;
import org.onap.policy.clamp.controlloop.participant.intermediary.comm.ControlLoopUpdateListener;
import org.onap.policy.clamp.controlloop.participant.intermediary.comm.ParticipantDeregisterAckListener;
import org.onap.policy.clamp.controlloop.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.controlloop.participant.intermediary.comm.ParticipantRegisterAckListener;
import org.onap.policy.clamp.controlloop.participant.intermediary.comm.ParticipantUpdateListener;
import org.onap.policy.clamp.controlloop.participant.intermediary.handler.ParticipantHandler;
import org.onap.policy.clamp.controlloop.participant.policy.main.utils.TestListenerUtils;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = {"classpath:application_test.properties"})
class ParticipantMessagesTest {

    private static final Object lockit = new Object();
    private static final CommInfrastructure INFRA = CommInfrastructure.NOOP;
    private static final String TOPIC = "my-topic";

    @Autowired
    private ParticipantHandler participantHandler;

    @Test
    void testSendParticipantRegisterMessage() throws Exception {
        final ParticipantRegister participantRegisterMsg = new ParticipantRegister();
        participantRegisterMsg.setParticipantId(getParticipantId());
        participantRegisterMsg.setTimestamp(Instant.now());
        participantRegisterMsg.setParticipantType(getParticipantType());

        synchronized (lockit) {
            ParticipantMessagePublisher participantMessagePublisher =
                    new ParticipantMessagePublisher();
            participantMessagePublisher.active(Collections.singletonList(Mockito.mock(TopicSink.class)));
            participantMessagePublisher.sendParticipantRegister(participantRegisterMsg);
        }
    }

    @Test
    void testReceiveParticipantRegisterAckMessage() throws Exception {
        final ParticipantRegisterAck participantRegisterAckMsg = new ParticipantRegisterAck();
        participantRegisterAckMsg.setMessage("ParticipantRegisterAck message");
        participantRegisterAckMsg.setResponseTo(UUID.randomUUID());
        participantRegisterAckMsg.setResult(true);

        synchronized (lockit) {
            ParticipantRegisterAckListener participantRegisterAckListener =
                new ParticipantRegisterAckListener(participantHandler);
            participantRegisterAckListener.onTopicEvent(INFRA, TOPIC, null, participantRegisterAckMsg);
        }
    }

    @Test
    void testSendParticipantDeregisterMessage() throws Exception {
        final ParticipantDeregister participantDeregisterMsg = new ParticipantDeregister();
        participantDeregisterMsg.setParticipantId(getParticipantId());
        participantDeregisterMsg.setTimestamp(Instant.now());
        participantDeregisterMsg.setParticipantType(getParticipantType());

        synchronized (lockit) {
            ParticipantMessagePublisher participantMessagePublisher =
                    new ParticipantMessagePublisher();
            participantMessagePublisher.active(Collections.singletonList(Mockito.mock(TopicSink.class)));
            participantMessagePublisher.sendParticipantDeregister(participantDeregisterMsg);
        }
    }

    @Test
    void testReceiveParticipantDeregisterAckMessage() throws Exception {
        final ParticipantDeregisterAck participantDeregisterAckMsg = new ParticipantDeregisterAck();
        participantDeregisterAckMsg.setMessage("ParticipantDeregisterAck message");
        participantDeregisterAckMsg.setResponseTo(UUID.randomUUID());
        participantDeregisterAckMsg.setResult(true);

        synchronized (lockit) {
            ParticipantDeregisterAckListener participantDeregisterAckListener =
                    new ParticipantDeregisterAckListener(participantHandler);
            participantDeregisterAckListener.onTopicEvent(INFRA, TOPIC, null, participantDeregisterAckMsg);
        }
    }

    @Test
    void testReceiveParticipantUpdateMessage() throws Exception {
        ParticipantUpdate participantUpdateMsg = TestListenerUtils.createParticipantUpdateMsg();

        synchronized (lockit) {
            ParticipantUpdateListener participantUpdateListener = new ParticipantUpdateListener(participantHandler);
            participantUpdateListener.onTopicEvent(INFRA, TOPIC, null, participantUpdateMsg);
        }

        // Verify the result of GET participants with what is stored
        assertEquals("org.onap.PM_Policy", participantHandler.getParticipantId().getName());
    }

    @Test
    void testSendParticipantUpdateAckMessage() throws Exception {
        final ParticipantUpdateAck participantUpdateAckMsg = new ParticipantUpdateAck();
        participantUpdateAckMsg.setMessage("ParticipantUpdateAck message");
        participantUpdateAckMsg.setResponseTo(UUID.randomUUID());
        participantUpdateAckMsg.setResult(true);

        synchronized (lockit) {
            ParticipantMessagePublisher participantMessagePublisher = new ParticipantMessagePublisher();
            participantMessagePublisher.active(Collections.singletonList(Mockito.mock(TopicSink.class)));
            participantMessagePublisher.sendParticipantUpdateAck(participantUpdateAckMsg);
        }
    }

    @Test
    void testParticipantStatusHeartbeat() throws Exception {
        final ParticipantStatus heartbeat = new ParticipantStatus();
        heartbeat.setParticipantId(getParticipantId());
        ControlLoopInfo clInfo = getControlLoopInfo(getControlLoopId());
        clInfo.setControlLoopId(getControlLoopId());
        heartbeat.setControlLoopInfoList(List.of(clInfo));

        ControlLoopElementDefinition clDefinition = getClElementDefinition();
        List<ControlLoopElementDefinition> controlLoopElementDefinitionList =
            List.of(clDefinition);
        ParticipantDefinition participantDefinition = new ParticipantDefinition();
        participantDefinition.setParticipantId(getParticipantId());
        participantDefinition.setControlLoopElementDefinitionList(controlLoopElementDefinitionList);
        List<ParticipantDefinition> participantDefinitionUpdates = List.of(participantDefinition);
        heartbeat.setParticipantDefinitionUpdates(participantDefinitionUpdates);

        synchronized (lockit) {
            ParticipantMessagePublisher publisher = new ParticipantMessagePublisher();
            publisher.active(Collections.singletonList(Mockito.mock(TopicSink.class)));
            assertThatCode(() -> publisher.sendHeartbeat(heartbeat)).doesNotThrowAnyException();
        }
    }

    private ToscaConceptIdentifier getParticipantId() {
        return new ToscaConceptIdentifier("org.onap.PM_Policy", "1.0.0");
    }

    private ToscaConceptIdentifier getParticipantType() {
        return new ToscaConceptIdentifier("org.onap.policy.controlloop.PolicyControlLoopParticipant", "2.3.1");
    }

    private ToscaConceptIdentifier getControlLoopId() {
        return new ToscaConceptIdentifier("PMSHInstance0", "1.0.0");
    }

    private ControlLoopInfo getControlLoopInfo(ToscaConceptIdentifier id) {
        ControlLoopInfo clInfo = new ControlLoopInfo();
        clInfo.setState(ControlLoopState.PASSIVE2RUNNING);

        ControlLoopStatistics clStatistics = new ControlLoopStatistics();
        clStatistics.setControlLoopId(id);
        clStatistics.setAverageExecutionTime(12345);
        clStatistics.setEventCount(12345);
        clStatistics.setLastEnterTime(12345);
        clStatistics.setLastExecutionTime(12345);
        clStatistics.setLastStart(12345);
        clStatistics.setTimeStamp(Instant.ofEpochMilli(3000));
        clStatistics.setUpTime(12345);
        ClElementStatisticsList clElementStatisticsList = new ClElementStatisticsList();
        ClElementStatistics clElementStatistics = new ClElementStatistics();
        clElementStatistics.setParticipantId(new ToscaConceptIdentifier("defName", "0.0.1"));
        clElementStatistics.setTimeStamp(Instant.now());
        clElementStatisticsList.setClElementStatistics(List.of(clElementStatistics));
        clStatistics.setClElementStatisticsList(clElementStatisticsList);

        clInfo.setControlLoopStatistics(clStatistics);
        return clInfo;
    }

    private ControlLoopElementDefinition getClElementDefinition() {
        ToscaNodeTemplate toscaNodeTemplate = new ToscaNodeTemplate();
        toscaNodeTemplate.setName("serviceTemplate");
        toscaNodeTemplate.setDerivedFrom("parentServiceTemplate");
        toscaNodeTemplate.setDescription("Description of serviceTemplate");
        toscaNodeTemplate.setVersion("1.2.3");

        ControlLoopElementDefinition clDefinition = new ControlLoopElementDefinition();
        clDefinition.setCommonPropertiesMap(Map.of("Prop1", "Prop1Value"));
        clDefinition.setControlLoopElementToscaNodeTemplate(toscaNodeTemplate);
        Map<String, String> commonPropertiesMap = Map.of("Prop1", "PropValue");
        clDefinition.setCommonPropertiesMap(commonPropertiesMap);
        return clDefinition;
    }
}
