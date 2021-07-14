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

package org.onap.policy.clamp.controlloop.runtime.supervision.comm;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElementDefinition;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantDeregister;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantDeregisterAck;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantRegister;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantRegisterAck;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantUpdate;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantUpdateAck;
import org.onap.policy.clamp.controlloop.runtime.commissioning.CommissioningProvider;
import org.onap.policy.clamp.controlloop.runtime.main.parameters.ClRuntimeParameterGroup;
import org.onap.policy.clamp.controlloop.runtime.monitoring.MonitoringProvider;
import org.onap.policy.clamp.controlloop.runtime.supervision.SupervisionHandler;
import org.onap.policy.clamp.controlloop.runtime.util.CommonTestData;
import org.onap.policy.clamp.controlloop.runtime.util.rest.CommonRestController;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

class SupervisionMessagesTest extends CommonRestController {

    private static final Object lockit = new Object();
    private static final CommInfrastructure INFRA = CommInfrastructure.NOOP;
    private static final String TOPIC = "my-topic";
    private static final long interval = 1000;
    private static ClRuntimeParameterGroup controlLoopParameters;
    private static SupervisionHandler supervisionHandler;
    private static CommissioningProvider commissioningProvider;

    /**
     * setup Db Provider Parameters.
     *
     * @throws PfModelException if an error occurs
     */
    @BeforeAll
    public static void setupDbProviderParameters() throws PfModelException {
        controlLoopParameters = CommonTestData.geParameterGroup(0, "instantproviderdb");
        commissioningProvider = new CommissioningProvider(controlLoopParameters);
        var monitoringProvider = new MonitoringProvider(controlLoopParameters);
        supervisionHandler = new SupervisionHandler(controlLoopParameters, monitoringProvider, commissioningProvider);
        supervisionHandler.startProviders();
        supervisionHandler.startAndRegisterPublishers(Collections.singletonList(Mockito.mock(TopicSink.class)));
    }

    @Test
    void testReceiveParticipantRegister() throws Exception {
        final ParticipantRegister participantRegisterMsg = new ParticipantRegister();
        participantRegisterMsg.setParticipantId(getParticipantId());
        participantRegisterMsg.setTimestamp(Instant.now());
        participantRegisterMsg.setParticipantType(getParticipantType());

        synchronized (lockit) {
            ParticipantRegisterListener participantRegisterListener =
                    new ParticipantRegisterListener(supervisionHandler);
            participantRegisterListener.onTopicEvent(INFRA, TOPIC, null, participantRegisterMsg);
        }
    }

    @Test
    void testSendParticipantRegisterAck() throws Exception {
        final ParticipantRegisterAck participantRegisterAckMsg = new ParticipantRegisterAck();
        participantRegisterAckMsg.setMessage("ParticipantRegisterAck message");
        participantRegisterAckMsg.setMessageType(ParticipantMessageType.PARTICIPANT_REGISTER_ACK);
        participantRegisterAckMsg.setResponseTo(UUID.randomUUID());
        participantRegisterAckMsg.setResult(true);

        synchronized (lockit) {
            ParticipantRegisterAckPublisher clRegisterAckPublisher = new ParticipantRegisterAckPublisher(
                           Collections.singletonList(Mockito.mock(TopicSink.class)), interval);
            clRegisterAckPublisher.send(participantRegisterAckMsg);
        }
    }

    @Test
    void testReceiveParticipantDeregister() throws Exception {
        final ParticipantDeregister participantDeregisterMsg = new ParticipantDeregister();
        participantDeregisterMsg.setParticipantId(getParticipantId());
        participantDeregisterMsg.setTimestamp(Instant.now());
        participantDeregisterMsg.setParticipantType(getParticipantType());

        synchronized (lockit) {
            ParticipantDeregisterListener participantDeregisterListener =
                    new ParticipantDeregisterListener(supervisionHandler);
            participantDeregisterListener.onTopicEvent(INFRA, TOPIC, null, participantDeregisterMsg);
        }
    }

    @Test
    void testSendParticipantDeregisterAck() throws Exception {
        final ParticipantDeregisterAck participantDeregisterAckMsg = new ParticipantDeregisterAck();
        participantDeregisterAckMsg.setMessage("ParticipantDeregisterAck message");
        participantDeregisterAckMsg.setMessageType(ParticipantMessageType.PARTICIPANT_DEREGISTER_ACK);
        participantDeregisterAckMsg.setResponseTo(UUID.randomUUID());
        participantDeregisterAckMsg.setResult(true);

        synchronized (lockit) {
            ParticipantDeregisterAckPublisher clDeregisterAckPublisher = new ParticipantDeregisterAckPublisher(
                           Collections.singletonList(Mockito.mock(TopicSink.class)), interval);
            clDeregisterAckPublisher.send(participantDeregisterAckMsg);
        }
    }

    @Test
    void testSendParticipantUpdate() throws Exception {
        final ParticipantUpdate participantUpdateMsg = new ParticipantUpdate();
        participantUpdateMsg.setParticipantId(getParticipantId());
        participantUpdateMsg.setTimestamp(Instant.now());
        participantUpdateMsg.setParticipantType(getParticipantType());
        participantUpdateMsg.setTimestamp(Instant.ofEpochMilli(3000));
        participantUpdateMsg.setMessageId(UUID.randomUUID());

        ToscaServiceTemplate toscaServiceTemplate = new ToscaServiceTemplate();
        toscaServiceTemplate.setName("serviceTemplate");
        toscaServiceTemplate.setDerivedFrom("parentServiceTemplate");
        toscaServiceTemplate.setDescription("Description of serviceTemplate");
        toscaServiceTemplate.setVersion("1.2.3");

        ControlLoopElementDefinition clDefinition = new ControlLoopElementDefinition();
        clDefinition.setId(UUID.randomUUID());
        clDefinition.setControlLoopElementToscaServiceTemplate(toscaServiceTemplate);
        Map<String, String> commonPropertiesMap = new LinkedHashMap<>();
        commonPropertiesMap.put("Prop1", "PropValue");
        clDefinition.setCommonPropertiesMap(commonPropertiesMap);

        Map<UUID, ControlLoopElementDefinition> controlLoopElementDefinitionMap = new LinkedHashMap<>();
        controlLoopElementDefinitionMap.put(UUID.randomUUID(), clDefinition);

        Map<ToscaConceptIdentifier, Map<UUID, ControlLoopElementDefinition>>
            participantDefinitionUpdateMap = new LinkedHashMap<>();
        participantDefinitionUpdateMap.put(getParticipantId(), controlLoopElementDefinitionMap);
        participantUpdateMsg.setParticipantDefinitionUpdateMap(participantDefinitionUpdateMap);

        synchronized (lockit) {
            ParticipantUpdatePublisher clUpdatePublisher = new ParticipantUpdatePublisher(
                           Collections.singletonList(Mockito.mock(TopicSink.class)), interval);
            clUpdatePublisher.send(participantUpdateMsg);
        }
    }

    @Test
    void testReceiveParticipantUpdateAckMessage() throws Exception {
        final ParticipantUpdateAck participantUpdateAckMsg = new ParticipantUpdateAck();
        participantUpdateAckMsg.setMessage("ParticipantUpdateAck message");
        participantUpdateAckMsg.setMessageType(ParticipantMessageType.PARTICIPANT_UPDATE_ACK);
        participantUpdateAckMsg.setResponseTo(UUID.randomUUID());
        participantUpdateAckMsg.setResult(true);

        synchronized (lockit) {
            ParticipantUpdateAckListener participantUpdateAckListener =
                    new ParticipantUpdateAckListener(supervisionHandler);
            participantUpdateAckListener.onTopicEvent(INFRA, TOPIC, null, participantUpdateAckMsg);
        }
    }

    private ToscaConceptIdentifier getParticipantId() {
        ToscaConceptIdentifier participantId = new ToscaConceptIdentifier("org.onap.PM_Policy", "1.0.0");
        return participantId;
    }

    private ToscaConceptIdentifier getParticipantType() {
        ToscaConceptIdentifier participantType = new ToscaConceptIdentifier(
                        "org.onap.policy.controlloop.PolicyControlLoopParticipant", "2.3.1");
        return participantType;
    }
}
