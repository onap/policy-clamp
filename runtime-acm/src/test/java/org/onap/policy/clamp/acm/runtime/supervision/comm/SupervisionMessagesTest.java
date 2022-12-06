/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation.
 *  Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.clamp.acm.runtime.supervision.comm;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionHandler;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionAck;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantDeregister;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantDeregisterAck;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantRegister;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantRegisterAck;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantStatus;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantUpdateAck;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class SupervisionMessagesTest {

    private static final String NOT_ACTIVE = "Not Active!";
    private static final CommInfrastructure INFRA = CommInfrastructure.NOOP;
    private static final String TOPIC = "my-topic";

    @Test
    void testSendParticipantRegisterAck() {
        var acRegisterAckPublisher = new ParticipantRegisterAckPublisher();
        var topicSink = mock(TopicSink.class);
        acRegisterAckPublisher.active(List.of(topicSink));
        acRegisterAckPublisher.send(new ParticipantRegisterAck());
        verify(topicSink).send(anyString());
        acRegisterAckPublisher.stop();
    }

    @Test
    void testSendParticipantRegisterAckNoActive() {
        var acRegisterAckPublisher = new ParticipantRegisterAckPublisher();
        assertThatThrownBy(() -> acRegisterAckPublisher.send(new ParticipantRegisterAck()))
                .hasMessageMatching(NOT_ACTIVE);
    }

    @Test
    void testReceiveParticipantDeregister() {
        final var participantDeregisterMsg = new ParticipantDeregister();
        var supervisionHandler = mock(SupervisionHandler.class);
        var participantDeregisterListener = new ParticipantDeregisterListener(supervisionHandler);
        participantDeregisterListener.onTopicEvent(INFRA, TOPIC, null, participantDeregisterMsg);
        verify(supervisionHandler).handleParticipantMessage(participantDeregisterMsg);
    }

    @Test
    void testSendParticipantDeregisterAck() {
        var acDeregisterAckPublisher = new ParticipantDeregisterAckPublisher();
        var topicSink = mock(TopicSink.class);
        acDeregisterAckPublisher.active(Collections.singletonList(topicSink));
        acDeregisterAckPublisher.send(new ParticipantDeregisterAck());
        verify(topicSink).send(anyString());
        acDeregisterAckPublisher.stop();
    }

    void testSendParticipantDeregisterAckNoActive() {
        var acDeregisterAckPublisher = new ParticipantDeregisterAckPublisher();
        assertThatThrownBy(() -> acDeregisterAckPublisher.send(new ParticipantDeregisterAck()))
                .hasMessageMatching(NOT_ACTIVE);
    }

    @Test
    void testReceiveParticipantUpdateAckMessage() {
        final var participantUpdateAckMsg = new ParticipantUpdateAck();
        var supervisionHandler = mock(SupervisionHandler.class);
        var participantUpdateAckListener = new ParticipantUpdateAckListener(supervisionHandler);
        participantUpdateAckListener.onTopicEvent(INFRA, TOPIC, null, participantUpdateAckMsg);
        verify(supervisionHandler).handleParticipantMessage(participantUpdateAckMsg);
    }

    @Test
    void testSendAutomationCompositionStateChangePublisherNotActive() {
        var publisher = new AutomationCompositionStateChangePublisher();
        assertThatThrownBy(() -> publisher.send(getAutomationComposition(), 0)).hasMessage(NOT_ACTIVE);
    }

    private AutomationComposition getAutomationComposition() {
        var automationComposition = new AutomationComposition();
        automationComposition.setName("NAME");
        automationComposition.setVersion("0.0.1");
        automationComposition.setState(AutomationCompositionState.UNINITIALISED);
        return automationComposition;
    }

    @Test
    void testSendAutomationCompositionStateChangePublisher() {
        var publisher = new AutomationCompositionStateChangePublisher();
        var topicSink = mock(TopicSink.class);
        publisher.active(List.of(topicSink));
        publisher.send(getAutomationComposition(), 0);
        verify(topicSink).send(anyString());
        publisher.stop();
    }

    @Test
    void testParticipantUpdatePublisherDecomisioning() {
        var publisher = new ParticipantUpdatePublisher(mock(AcDefinitionProvider.class));
        var topicSink = mock(TopicSink.class);
        publisher.active(List.of(topicSink));
        publisher.sendDecomisioning();
        verify(topicSink).send(anyString());
    }

    @Test
    void testParticipantUpdatePublisherComissioning() {
        var publisher = new ParticipantUpdatePublisher(mock(AcDefinitionProvider.class));
        var topicSink = mock(TopicSink.class);
        publisher.active(List.of(topicSink));
        publisher.sendComissioningBroadcast("NAME", "1.0.0");
        verify(topicSink, times(0)).send(anyString());
    }

    @Test
    void testParticipantStatusReqPublisher() {
        var publisher = new ParticipantStatusReqPublisher();
        var topicSink = mock(TopicSink.class);
        publisher.active(List.of(topicSink));
        publisher.send(getParticipantId());
        verify(topicSink).send(anyString());
    }

    @Test
    void testParticipantRegisterAckPublisher() {
        var publisher = new ParticipantRegisterAckPublisher();
        var topicSink = mock(TopicSink.class);
        publisher.active(List.of(topicSink));
        publisher.send(UUID.randomUUID(), getParticipantId(), getParticipantType());
        verify(topicSink).send(anyString());
    }

    private ToscaConceptIdentifier getParticipantId() {
        return new ToscaConceptIdentifier("org.onap.PM_Policy", "1.0.0");
    }

    private ToscaConceptIdentifier getParticipantType() {
        return new ToscaConceptIdentifier("org.onap.policy.acm.PolicyAutomationCompositionParticipant", "2.3.1");
    }

    @Test
    void testParticipantDeregisterAckPublisher() {
        var publisher = new ParticipantDeregisterAckPublisher();
        var topicSink = mock(TopicSink.class);
        publisher.active(List.of(topicSink));
        publisher.send(UUID.randomUUID());
        verify(topicSink).send(anyString());
    }

    @Test
    void testParticipantRegisterListener() {
        final var participantRegister = new ParticipantRegister();
        var supervisionHandler = mock(SupervisionHandler.class);
        var participantRegisterListener = new ParticipantRegisterListener(supervisionHandler);
        participantRegisterListener.onTopicEvent(INFRA, TOPIC, null, participantRegister);
        verify(supervisionHandler).handleParticipantMessage(participantRegister);
    }

    @Test
    void testParticipantStatusListener() {
        final var participantStatus = new ParticipantStatus();
        var supervisionHandler = mock(SupervisionHandler.class);
        var participantStatusListener = new ParticipantStatusListener(supervisionHandler);
        participantStatusListener.onTopicEvent(INFRA, TOPIC, null, participantStatus);
        verify(supervisionHandler).handleParticipantMessage(participantStatus);
    }

    @Test
    void testAutomationCompositionUpdateAckListener() {
        final var automationCompositionAck =
                new AutomationCompositionAck(ParticipantMessageType.AUTOMATION_COMPOSITION_UPDATE);
        var supervisionHandler = mock(SupervisionHandler.class);
        var acUpdateAckListener = new AutomationCompositionUpdateAckListener(supervisionHandler);
        acUpdateAckListener.onTopicEvent(INFRA, TOPIC, null, automationCompositionAck);
        verify(supervisionHandler).handleAutomationCompositionUpdateAckMessage(automationCompositionAck);
    }

    @Test
    void testAutomationCompositionStateChangeAckListener() {
        final var automationCompositionAck =
                new AutomationCompositionAck(ParticipantMessageType.AUTOMATION_COMPOSITION_STATE_CHANGE);
        var supervisionHandler = mock(SupervisionHandler.class);
        var acStateChangeAckListener = new AutomationCompositionStateChangeAckListener(supervisionHandler);
        acStateChangeAckListener.onTopicEvent(INFRA, TOPIC, null, automationCompositionAck);
        verify(supervisionHandler).handleAutomationCompositionStateChangeAckMessage(automationCompositionAck);
    }

}
