/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation.
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.onap.policy.clamp.acm.runtime.util.CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup;
import org.onap.policy.clamp.acm.runtime.participants.AcmParticipantProvider;
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionAcHandler;
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionHandler;
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionParticipantHandler;
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeployAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantDeregister;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantDeregisterAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrimeAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantRegister;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantRegisterAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatus;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class SupervisionMessagesTest {

    private static final String AC_INSTANTIATION_UPDATE_JSON =
            "src/test/resources/rest/acm/AutomationCompositionUpdate.json";
    private static final String NOT_ACTIVE = "Not Active!";
    private static final CommInfrastructure INFRA = CommInfrastructure.NOOP;
    private static final String TOPIC = "my-topic";

    private static final String TOSCA_ELEMENT_NAME = "org.onap.policy.clamp.acm.AutomationCompositionElement";

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
        var supervisionHandler = mock(SupervisionParticipantHandler.class);
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
    void testReceiveParticipantPrimeAckMessage() {
        final var participantPrimeAckMsg = new ParticipantPrimeAck();
        var supervisionHandler = mock(SupervisionHandler.class);
        var participantPrimeAckListener = new ParticipantPrimeAckListener(supervisionHandler);
        participantPrimeAckListener.onTopicEvent(INFRA, TOPIC, null, participantPrimeAckMsg);
        verify(supervisionHandler).handleParticipantMessage(participantPrimeAckMsg);
    }

    @Test
    void testSendAutomationCompositionStateChangePublisherNotActive() {
        var publisher = new AutomationCompositionStateChangePublisher();
        assertThatThrownBy(() -> publisher.send(getAutomationComposition(), 0, true)).hasMessage(NOT_ACTIVE);
    }

    private AutomationComposition getAutomationComposition() {
        var automationComposition = new AutomationComposition();
        automationComposition.setName("NAME");
        automationComposition.setVersion("0.0.1");
        automationComposition.setDeployState(DeployState.DEPLOYED);
        automationComposition.setLockState(LockState.UNLOCKING);
        return automationComposition;
    }

    @Test
    void testSendAutomationCompositionStateChangePublisher() {
        var publisher = new AutomationCompositionStateChangePublisher();
        var topicSink = mock(TopicSink.class);
        publisher.active(List.of(topicSink));
        publisher.send(getAutomationComposition(), 0, true);
        verify(topicSink).send(anyString());
        publisher.stop();
    }

    @Test
    void testParticipantPrimePublisherDecommissioning() {
        var publisher = new ParticipantPrimePublisher(mock(ParticipantProvider.class),
                mock(AcmParticipantProvider.class), mock(AcRuntimeParameterGroup.class));
        var topicSink = mock(TopicSink.class);
        publisher.active(List.of(topicSink));
        publisher.sendDepriming(UUID.randomUUID());
        verify(topicSink).send(anyString());
    }

    @Test
    void testParticipantPrimePublisherPriming() {
        var participantId = UUID.randomUUID();
        Map<ToscaConceptIdentifier, UUID> supportedElementMap = new HashMap<>();
        supportedElementMap.put(
                new ToscaConceptIdentifier("org.onap.policy.clamp.acm.PolicyAutomationCompositionElement", "1.0.0"),
                participantId);
        supportedElementMap.put(new ToscaConceptIdentifier(
                "org.onap.policy.clamp.acm.K8SMicroserviceAutomationCompositionElement", "1.0.0"), participantId);
        supportedElementMap.put(
                new ToscaConceptIdentifier("org.onap.policy.clamp.acm.HttpAutomationCompositionElement", "1.0.0"),
                participantId);
        var participantProvider = mock(ParticipantProvider.class);
        when(participantProvider.getSupportedElementMap()).thenReturn(supportedElementMap);
        var publisher = new ParticipantPrimePublisher(participantProvider, mock(AcmParticipantProvider.class),
                CommonTestData.getTestParamaterGroup());
        var topicSink = mock(TopicSink.class);
        publisher.active(List.of(topicSink));
        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        serviceTemplate.setName("Name");
        serviceTemplate.setVersion("1.0.0");
        var acmDefinition = new AutomationCompositionDefinition();
        acmDefinition.setCompositionId(UUID.randomUUID());
        acmDefinition.setServiceTemplate(serviceTemplate);
        var acElements = AcmUtils
                .extractAcElementsFromServiceTemplate(serviceTemplate, TOSCA_ELEMENT_NAME);
        acmDefinition.setElementStateMap(AcmUtils.createElementStateMap(acElements, AcTypeState.COMMISSIONED));
        var preparation = publisher.prepareParticipantPriming(acmDefinition);
        publisher.sendPriming(preparation, acmDefinition.getCompositionId(), null);
        verify(topicSink).send(anyString());
    }

    @Test
    void testParticipantStatusReqPublisher() {
        var publisher = new ParticipantStatusReqPublisher();
        var topicSink = mock(TopicSink.class);
        publisher.active(List.of(topicSink));
        publisher.send(CommonTestData.getParticipantId());
        verify(topicSink).send(anyString());
    }

    @Test
    void testParticipantRegisterAckPublisher() {
        var publisher = new ParticipantRegisterAckPublisher();
        var topicSink = mock(TopicSink.class);
        publisher.active(List.of(topicSink));
        publisher.send(UUID.randomUUID(), CommonTestData.getParticipantId());
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

    @Test
    void testAcElementPropertiesPublisher() {
        var publisher = new AcElementPropertiesPublisher();
        var topicSink = mock(TopicSink.class);
        publisher.active(List.of(topicSink));
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_UPDATE_JSON, "Crud");
        publisher.send(automationComposition);
        verify(topicSink).send(anyString());
    }

    @Test
    void testAutomationCompositionMigrationPublisher() {
        var publisher = new AutomationCompositionMigrationPublisher();
        var topicSink = mock(TopicSink.class);
        publisher.active(List.of(topicSink));
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_UPDATE_JSON, "Crud");
        publisher.send(automationComposition, UUID.randomUUID());
        verify(topicSink).send(anyString());
    }

    @Test
    void testParticipantRestartPublisher() {
        var publisher = new ParticipantRestartPublisher(CommonTestData.getTestParamaterGroup());
        var topicSink = mock(TopicSink.class);
        publisher.active(List.of(topicSink));

        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        var acmDefinition = new AutomationCompositionDefinition();
        acmDefinition.setCompositionId(UUID.randomUUID());
        acmDefinition.setServiceTemplate(serviceTemplate);
        var acElements = AcmUtils
                .extractAcElementsFromServiceTemplate(serviceTemplate, "");
        acmDefinition.setElementStateMap(AcmUtils.createElementStateMap(acElements, AcTypeState.PRIMED));

        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_UPDATE_JSON, "Crud");

        var participantId = automationComposition.getElements().values().iterator().next().getParticipantId();
        acmDefinition.getElementStateMap().values().iterator().next().setParticipantId(participantId);

        publisher.send(participantId, acmDefinition, List.of(automationComposition));
        verify(topicSink).send(anyString());
    }

    @Test
    void testParticipantRegisterListener() {
        final var participantRegister = new ParticipantRegister();
        var supervisionHandler = mock(SupervisionParticipantHandler.class);
        var participantRegisterListener = new ParticipantRegisterListener(supervisionHandler);
        participantRegisterListener.onTopicEvent(INFRA, TOPIC, null, participantRegister);
        verify(supervisionHandler).handleParticipantMessage(participantRegister);
    }

    @Test
    void testParticipantStatusListener() {
        final var participantStatus = new ParticipantStatus();
        var supervisionHandler = mock(SupervisionParticipantHandler.class);
        var participantStatusListener = new ParticipantStatusListener(supervisionHandler);
        participantStatusListener.onTopicEvent(INFRA, TOPIC, null, participantStatus);
        verify(supervisionHandler).handleParticipantMessage(participantStatus);
    }

    @Test
    void testAutomationCompositionUpdateAckListener() {
        final var automationCompositionAck =
                new AutomationCompositionDeployAck(ParticipantMessageType.AUTOMATION_COMPOSITION_DEPLOY);
        var supervisionHandler = mock(SupervisionAcHandler.class);
        var acUpdateAckListener = new AutomationCompositionUpdateAckListener(supervisionHandler);
        acUpdateAckListener.onTopicEvent(INFRA, TOPIC, null, automationCompositionAck);
        verify(supervisionHandler).handleAutomationCompositionUpdateAckMessage(automationCompositionAck);
    }

    @Test
    void testAutomationCompositionStateChangeAckListener() {
        final var automationCompositionAck =
                new AutomationCompositionDeployAck(ParticipantMessageType.AUTOMATION_COMPOSITION_STATE_CHANGE);
        var supervisionHandler = mock(SupervisionAcHandler.class);
        var acStateChangeAckListener = new AutomationCompositionStateChangeAckListener(supervisionHandler);
        acStateChangeAckListener.onTopicEvent(INFRA, TOPIC, null, automationCompositionAck);
        verify(supervisionHandler).handleAutomationCompositionStateChangeAckMessage(automationCompositionAck);
    }
}
