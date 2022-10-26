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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionHandler;
import org.onap.policy.clamp.acm.runtime.util.rest.CommonRestController;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantDeregister;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantDeregisterAck;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantRegisterAck;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantUpdateAck;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ServiceTemplateProvider;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class SupervisionMessagesTest extends CommonRestController {

    private static final String NOT_ACTIVE = "Not Active!";
    private static final Object lockit = new Object();
    private static final CommInfrastructure INFRA = CommInfrastructure.NOOP;
    private static final String TOPIC = "my-topic";
    private static SupervisionHandler supervisionHandler;

    /**
     * setup Db Provider Parameters.
     *
     * @throws PfModelException if an error occurs
     */
    @BeforeAll
    public static void setupDbProviderParameters() throws PfModelException {
        var acProvider = mock(AutomationCompositionProvider.class);
        var participantProvider = mock(ParticipantProvider.class);
        var serviceTemplateProvider = Mockito.mock(ServiceTemplateProvider.class);
        var automationCompositionUpdatePublisher = Mockito.mock(AutomationCompositionUpdatePublisher.class);
        var automationCompositionStateChangePublisher = Mockito.mock(AutomationCompositionStateChangePublisher.class);
        var participantRegisterAckPublisher = Mockito.mock(ParticipantRegisterAckPublisher.class);
        var participantDeregisterAckPublisher = Mockito.mock(ParticipantDeregisterAckPublisher.class);
        var participantUpdatePublisher = Mockito.mock(ParticipantUpdatePublisher.class);
        supervisionHandler = new SupervisionHandler(acProvider, participantProvider,
            serviceTemplateProvider, automationCompositionUpdatePublisher, automationCompositionStateChangePublisher,
            participantRegisterAckPublisher, participantDeregisterAckPublisher, participantUpdatePublisher);
    }

    @Test
    void testSendParticipantRegisterAck() throws Exception {
        final ParticipantRegisterAck participantRegisterAckMsg = new ParticipantRegisterAck();
        participantRegisterAckMsg.setMessage("ParticipantRegisterAck message");
        participantRegisterAckMsg.setResponseTo(UUID.randomUUID());
        participantRegisterAckMsg.setResult(true);

        synchronized (lockit) {
            ParticipantRegisterAckPublisher acRegisterAckPublisher = new ParticipantRegisterAckPublisher();
            acRegisterAckPublisher.active(List.of(Mockito.mock(TopicSink.class)));
            assertThatCode(() -> acRegisterAckPublisher.send(participantRegisterAckMsg)).doesNotThrowAnyException();
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
            assertThatCode(
                () -> participantDeregisterListener.onTopicEvent(INFRA, TOPIC, null, participantDeregisterMsg))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void testSendParticipantDeregisterAck() throws Exception {
        final ParticipantDeregisterAck participantDeregisterAckMsg = new ParticipantDeregisterAck();
        participantDeregisterAckMsg.setMessage("ParticipantDeregisterAck message");
        participantDeregisterAckMsg.setResponseTo(UUID.randomUUID());
        participantDeregisterAckMsg.setResult(true);

        synchronized (lockit) {
            ParticipantDeregisterAckPublisher acDeregisterAckPublisher = new ParticipantDeregisterAckPublisher();
            acDeregisterAckPublisher.active(Collections.singletonList(Mockito.mock(TopicSink.class)));
            assertThatCode(() -> acDeregisterAckPublisher.send(participantDeregisterAckMsg)).doesNotThrowAnyException();
        }
    }

    @Test
    void testReceiveParticipantUpdateAckMessage() throws Exception {
        final ParticipantUpdateAck participantUpdateAckMsg = new ParticipantUpdateAck();
        participantUpdateAckMsg.setMessage("ParticipantUpdateAck message");
        participantUpdateAckMsg.setResponseTo(UUID.randomUUID());
        participantUpdateAckMsg.setResult(true);
        participantUpdateAckMsg.setParticipantId(getParticipantId());
        participantUpdateAckMsg.setParticipantType(getParticipantType());

        synchronized (lockit) {
            ParticipantUpdateAckListener participantUpdateAckListener =
                new ParticipantUpdateAckListener(supervisionHandler);
            assertThatCode(() -> participantUpdateAckListener.onTopicEvent(INFRA, TOPIC, null, participantUpdateAckMsg))
                .doesNotThrowAnyException();
        }
    }

    @Test
    void testSendAutomationCompositionStateChangePublisherNotActive() {
        var publisher = new AutomationCompositionStateChangePublisher();
        assertThatThrownBy(() -> publisher.send(getAutomationComposition(), 0)).hasMessage(NOT_ACTIVE);
    }

    @Test
    void testSendAutomationCompositionStateChangePublisher() {
        var publisher = new AutomationCompositionStateChangePublisher();
        var topicSink = mock(TopicSink.class);
        publisher.active(List.of(topicSink));
        publisher.send(getAutomationComposition(), 0);
        verify(topicSink).send(anyString());
    }

    @Test
    void testParticipantUpdatePublisherDecomisioning() {
        var publisher = new ParticipantUpdatePublisher(mock(ServiceTemplateProvider.class));
        var topicSink = mock(TopicSink.class);
        publisher.active(List.of(topicSink));
        publisher.sendDecomisioning();
        verify(topicSink).send(anyString());
    }

    @Test
    void testParticipantUpdatePublisherComissioning() {
        var publisher = new ParticipantUpdatePublisher(mock(ServiceTemplateProvider.class));
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

    @Test
    void testParticipantDeregisterAckPublisher() {
        var publisher = new ParticipantDeregisterAckPublisher();
        var topicSink = mock(TopicSink.class);
        publisher.active(List.of(topicSink));
        publisher.send(UUID.randomUUID());
        verify(topicSink).send(anyString());
    }

    private AutomationComposition getAutomationComposition() {
        var automationComposition = new AutomationComposition();
        automationComposition.setName("NAME");
        automationComposition.setVersion("0.0.1");
        automationComposition.setState(AutomationCompositionState.UNINITIALISED);
        return automationComposition;
    }

    private ToscaConceptIdentifier getParticipantId() {
        return new ToscaConceptIdentifier("org.onap.PM_Policy", "1.0.0");
    }

    private ToscaConceptIdentifier getParticipantType() {
        return new ToscaConceptIdentifier("org.onap.policy.acm.PolicyAutomationCompositionParticipant", "2.3.1");
    }
}
