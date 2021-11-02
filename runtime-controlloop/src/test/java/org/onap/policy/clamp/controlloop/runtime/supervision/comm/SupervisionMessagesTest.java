/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.runtime.supervision.comm;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ClElementStatisticsProvider;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ControlLoopProvider;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ParticipantStatisticsProvider;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ServiceTemplateProvider;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantDeregister;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantDeregisterAck;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantRegisterAck;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantUpdateAck;
import org.onap.policy.clamp.controlloop.runtime.main.parameters.ClRuntimeParameterGroup;
import org.onap.policy.clamp.controlloop.runtime.monitoring.MonitoringProvider;
import org.onap.policy.clamp.controlloop.runtime.supervision.SupervisionHandler;
import org.onap.policy.clamp.controlloop.runtime.util.CommonTestData;
import org.onap.policy.clamp.controlloop.runtime.util.rest.CommonRestController;
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
    private static ControlLoopProvider clProvider;

    /**
     * setup Db Provider Parameters.
     *
     * @throws PfModelException if an error occurs
     */
    @BeforeAll
    public static void setupDbProviderParameters() throws PfModelException {
        ClRuntimeParameterGroup controlLoopParameters = CommonTestData.geParameterGroup("instantproviderdb");

        clProvider = new ControlLoopProvider(controlLoopParameters.getDatabaseProviderParameters());

        var participantStatisticsProvider = mock(ParticipantStatisticsProvider.class);
        var clElementStatisticsProvider = mock(ClElementStatisticsProvider.class);
        var monitoringProvider =
                new MonitoringProvider(participantStatisticsProvider, clElementStatisticsProvider, clProvider);
        var participantProvider = new ParticipantProvider(controlLoopParameters.getDatabaseProviderParameters());
        var serviceTemplateProvider = Mockito.mock(ServiceTemplateProvider.class);
        var controlLoopUpdatePublisher = Mockito.mock(ControlLoopUpdatePublisher.class);
        var controlLoopStateChangePublisher = Mockito.mock(ControlLoopStateChangePublisher.class);
        var participantRegisterAckPublisher = Mockito.mock(ParticipantRegisterAckPublisher.class);
        var participantDeregisterAckPublisher = Mockito.mock(ParticipantDeregisterAckPublisher.class);
        var participantUpdatePublisher = Mockito.mock(ParticipantUpdatePublisher.class);
        supervisionHandler = new SupervisionHandler(clProvider, participantProvider, monitoringProvider,
                serviceTemplateProvider, controlLoopUpdatePublisher, controlLoopStateChangePublisher,
                participantRegisterAckPublisher, participantDeregisterAckPublisher, participantUpdatePublisher);
    }

    @AfterAll
    public static void closeDbProvider() throws PfModelException {
        clProvider.close();
    }

    @Test
    void testSendParticipantRegisterAck() throws Exception {
        final ParticipantRegisterAck participantRegisterAckMsg = new ParticipantRegisterAck();
        participantRegisterAckMsg.setMessage("ParticipantRegisterAck message");
        participantRegisterAckMsg.setResponseTo(UUID.randomUUID());
        participantRegisterAckMsg.setResult(true);

        synchronized (lockit) {
            ParticipantRegisterAckPublisher clRegisterAckPublisher = new ParticipantRegisterAckPublisher();
            clRegisterAckPublisher.active(Collections.singletonList(Mockito.mock(TopicSink.class)));
            assertThatCode(() -> clRegisterAckPublisher.send(participantRegisterAckMsg)).doesNotThrowAnyException();
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
            ParticipantDeregisterAckPublisher clDeregisterAckPublisher = new ParticipantDeregisterAckPublisher();
            clDeregisterAckPublisher.active(Collections.singletonList(Mockito.mock(TopicSink.class)));
            assertThatCode(() -> clDeregisterAckPublisher.send(participantDeregisterAckMsg)).doesNotThrowAnyException();
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
    void testSendControlLoopStateChangePublisherNotActive() {
        var publisher = new ControlLoopStateChangePublisher();
        assertThatThrownBy(() -> publisher.send(getControlLoop(), 0)).hasMessage(NOT_ACTIVE);
    }

    @Test
    void testSendControlLoopStateChangePublisher() {
        var publisher = new ControlLoopStateChangePublisher();
        var topicSink = mock(TopicSink.class);
        publisher.active(List.of(topicSink));
        publisher.send(getControlLoop(), 0);
        verify(topicSink).send(anyString());
    }

    private ControlLoop getControlLoop() {
        var controlLoop = new ControlLoop();
        controlLoop.setName("NAME");
        controlLoop.setVersion("0.0.1");
        controlLoop.setState(ControlLoopState.UNINITIALISED);
        return controlLoop;
    }

    private ToscaConceptIdentifier getParticipantId() {
        return new ToscaConceptIdentifier("org.onap.PM_Policy", "1.0.0");
    }

    private ToscaConceptIdentifier getParticipantType() {
        return new ToscaConceptIdentifier("org.onap.policy.controlloop.PolicyControlLoopParticipant", "2.3.1");
    }
}
