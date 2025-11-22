/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2026 OpenInfra Foundation Europe. All rights reserved.
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.onap.policy.clamp.acm.runtime.util.CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup;
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
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionMigration;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionPrepare;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionStateChange;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantDeregister;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantDeregisterAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrime;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrimeAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantRegister;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantRegisterAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantReqSync;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatus;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatusReq;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantSync;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.PropertiesUpdate;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class SupervisionMessagesTest {

    private static final String AC_INSTANTIATION_UPDATE_JSON =
            "src/test/resources/rest/acm/AutomationCompositionUpdate.json";

    private static final String TOSCA_ELEMENT_NAME = "org.onap.policy.clamp.acm.AutomationCompositionElement";

    private SupervisionAcHandler supervisionAcHandler;
    private SupervisionParticipantHandler supervisionParticipantHandler;
    private SupervisionHandler supervisionHandler;
    private ParticipantMessageListener participantMessageListener;
    private ParticipantPublisher participantPublisher;
    private ParticipantAckPublisher participantAckPublisher;

    @BeforeEach
    void setup() {
        supervisionAcHandler = mock(SupervisionAcHandler.class);
        supervisionParticipantHandler = mock(SupervisionParticipantHandler.class);
        supervisionHandler = mock(SupervisionHandler.class);
        participantMessageListener = new ParticipantMessageListener(
                supervisionAcHandler, supervisionParticipantHandler, supervisionHandler);
        participantPublisher = mock(ParticipantPublisher.class);
        participantAckPublisher = mock(ParticipantAckPublisher.class);
    }

    @Test
    void testSendParticipantRegisterAck() {
        var acRegisterAckPublisher = new ParticipantRegisterAckPublisher(participantAckPublisher);
        acRegisterAckPublisher.send(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        verify(participantAckPublisher).sendToSyncTopic(any(ParticipantRegisterAck.class));
    }

    @Test
    void testReceiveParticipantDeregister() {
        final var participantDeregisterMsg = new ParticipantDeregister();
        participantMessageListener.onTopicEvent(participantDeregisterMsg);
        verify(supervisionParticipantHandler).handleParticipantMessage(participantDeregisterMsg);
    }

    @Test
    void testSendParticipantDeregisterAck() {
        var acDeregisterAckPublisher = new ParticipantDeregisterAckPublisher(participantAckPublisher);
        acDeregisterAckPublisher.send(UUID.randomUUID());
        verify(participantAckPublisher).send(any(ParticipantDeregisterAck.class));
    }

    @Test
    void testReceiveParticipantPrimeAckMessage() {
        final var participantPrimeAckMsg = new ParticipantPrimeAck();
        participantMessageListener.onTopicEvent(participantPrimeAckMsg);
        verify(supervisionHandler).handleParticipantMessage(participantPrimeAckMsg);
    }

    private AutomationComposition getAutomationComposition() {
        var automationComposition = new AutomationComposition();
        automationComposition.setName("NAME");
        automationComposition.setVersion("0.0.1");
        automationComposition.setDeployState(DeployState.DEPLOYED);
        automationComposition.setLockState(LockState.UNLOCKING);
        automationComposition.setElements(new HashMap<>());
        return automationComposition;
    }

    @Test
    void testSendAutomationCompositionStateChangePublisher() {
        var publisher = new AutomationCompositionStateChangePublisher(participantPublisher);
        publisher.send(getAutomationComposition(), 0, true, UUID.randomUUID());
        verify(participantPublisher).send(any(AutomationCompositionStateChange.class));
    }

    @Test
    void testParticipantPrimePublisherDecommissioning() {
        var publisher = new ParticipantPrimePublisher(
                mock(ParticipantProvider.class), mock(AcRuntimeParameterGroup.class), participantPublisher);
        publisher.sendDepriming(UUID.randomUUID(), Set.of(UUID.randomUUID()), UUID.randomUUID());
        verify(participantPublisher).send(any());
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
        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        serviceTemplate.setName("Name");
        serviceTemplate.setVersion("1.0.0");
        var acmDefinition = new AutomationCompositionDefinition();
        acmDefinition.setCompositionId(UUID.randomUUID());
        acmDefinition.setServiceTemplate(serviceTemplate);
        var acElements = AcmUtils
                .extractAcElementsFromServiceTemplate(serviceTemplate, TOSCA_ELEMENT_NAME);
        acmDefinition.setElementStateMap(AcmUtils.createElementStateMap(acElements, AcTypeState.COMMISSIONED));
        var publisher = new ParticipantPrimePublisher(participantProvider, CommonTestData.getTestParamaterGroup(),
                participantPublisher);
        var preparation = publisher.prepareParticipantPriming(acmDefinition);
        publisher.sendPriming(preparation, acmDefinition.getCompositionId(), acmDefinition.getRevisionId());
        verify(participantPublisher).send(any(ParticipantPrime.class));
    }

    @Test
    void testParticipantStatusReqPublisher() {
        var publisher = new ParticipantStatusReqPublisher(participantPublisher);
        publisher.send(CommonTestData.getParticipantId());
        verify(participantPublisher).sendToSyncTopic(any(ParticipantStatusReq.class));
    }

    @Test
    void testParticipantRegisterAckPublisher() {
        var publisher = new ParticipantRegisterAckPublisher(participantAckPublisher);
        publisher.send(UUID.randomUUID(), CommonTestData.getParticipantId(), CommonTestData.getReplicaId());
        verify(participantAckPublisher).sendToSyncTopic(any(ParticipantRegisterAck.class));
    }

    @Test
    void testParticipantDeregisterAckPublisher() {
        var publisher = new ParticipantDeregisterAckPublisher(participantAckPublisher);
        publisher.send(UUID.randomUUID());
        verify(participantAckPublisher).send(any(ParticipantDeregisterAck.class));
    }

    @Test
    void testAcElementPropertiesPublisher() {
        var publisher = new AcElementPropertiesPublisher(participantPublisher);
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_UPDATE_JSON, "Crud");
        publisher.send(automationComposition, UUID.randomUUID());
        verify(participantPublisher).send(any(PropertiesUpdate.class));
    }

    @Test
    void testAutomationCompositionMigrationPublisher() {
        var publisher = new AutomationCompositionMigrationPublisher(participantPublisher);
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_UPDATE_JSON, "Crud");
        publisher.send(automationComposition, 0, UUID.randomUUID(), UUID.randomUUID(), true);
        verify(participantPublisher).send(any(AutomationCompositionMigration.class));
    }

    @Test
    void testAcPreparePublisher() {
        var publisher = new AcPreparePublisher(participantPublisher);
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_UPDATE_JSON, "Crud");
        publisher.sendPrepare(automationComposition, 0, UUID.randomUUID());
        verify(participantPublisher).send(any(AutomationCompositionPrepare.class));
    }

    @Test
    void testAcReviewPublisher() {
        var publisher = new AcPreparePublisher(participantPublisher);
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_UPDATE_JSON, "Crud");
        publisher.sendReview(automationComposition, UUID.randomUUID());
        verify(participantPublisher).send(any(AutomationCompositionPrepare.class));
    }

    @Test
    void testParticipantSyncPublisherAutomationComposition() {
        var publisher = new ParticipantSyncPublisher(CommonTestData.getTestParamaterGroup(), participantPublisher);

        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_UPDATE_JSON, "Crud");
        publisher.sendSync(automationComposition);
        verify(participantPublisher).sendToSyncTopic(any(ParticipantSync.class));

        clearInvocations(participantPublisher);
        automationComposition.setDeployState(DeployState.DELETED);
        publisher.sendSync(automationComposition);
        verify(participantPublisher).sendToSyncTopic(any(ParticipantSync.class));

        clearInvocations(participantPublisher);
        automationComposition.getElements().values().iterator().next().setDeployState(DeployState.DELETED);
        publisher.sendDeleteSync(automationComposition, UUID.randomUUID());
        verify(participantPublisher).sendToSyncTopic(any(ParticipantSync.class));
    }

    @Test
    void testParticipantSyncPublisherAcDefinition() {
        var publisher = new ParticipantSyncPublisher(CommonTestData.getTestParamaterGroup(), participantPublisher);
        var acmDefinition = getAcmDefinition();
        publisher.sendSync(acmDefinition, null);
        verify(participantPublisher).sendToSyncTopic(any(ParticipantSync.class));
    }

    @Test
    void testParticipantSyncPublisherAcDefinitionCommissioned() {
        var publisher = new ParticipantSyncPublisher(CommonTestData.getTestParamaterGroup(), participantPublisher);
        var acmDefinition = getAcmDefinition();
        acmDefinition.setState(AcTypeState.COMMISSIONED);
        publisher.sendSync(acmDefinition, UUID.randomUUID());
        verify(participantPublisher).sendToSyncTopic(any(ParticipantSync.class));
    }

    @Test
    void testParticipantSyncPublisherRestart() {
        var publisher = new ParticipantSyncPublisher(CommonTestData.getTestParamaterGroup(), participantPublisher);
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_UPDATE_JSON, "Crud");
        var participantId = automationComposition.getElements().values().iterator().next().getParticipantId();
        var acmDefinition = getAcmDefinition();
        acmDefinition.getElementStateMap().values().iterator().next().setParticipantId(participantId);
        var replicaId = UUID.randomUUID();
        publisher.sendRestartMsg(participantId, replicaId, acmDefinition, List.of(automationComposition));
        verify(participantPublisher).sendToSyncTopic(any(ParticipantSync.class));
    }

    private AutomationCompositionDefinition getAcmDefinition() {
        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        var acmDefinition = new AutomationCompositionDefinition();
        acmDefinition.setCompositionId(UUID.randomUUID());
        acmDefinition.setState(AcTypeState.PRIMED);
        acmDefinition.setServiceTemplate(serviceTemplate);
        var acElements = AcmUtils.extractAcElementsFromServiceTemplate(serviceTemplate, TOSCA_ELEMENT_NAME);
        acmDefinition.setElementStateMap(AcmUtils.createElementStateMap(acElements, AcTypeState.PRIMED));
        acmDefinition.getElementStateMap().values().forEach(element -> element.setParticipantId(UUID.randomUUID()));
        return acmDefinition;
    }

    @Test
    void testParticipantRegisterListener() {
        final var participantRegister = new ParticipantRegister();
        participantMessageListener.onTopicEvent(participantRegister);
        verify(supervisionParticipantHandler).handleParticipantMessage(participantRegister);
    }

    @Test
    void testParticipantStatusListener() {
        final var participantStatus = new ParticipantStatus();
        participantMessageListener.onTopicEvent(participantStatus);
        verify(supervisionParticipantHandler).handleParticipantMessage(participantStatus);
    }

    @Test
    void testAutomationCompositionUpdateAckListener() {
        final var automationCompositionAck =
                new AutomationCompositionDeployAck(ParticipantMessageType.AUTOMATION_COMPOSITION_DEPLOY_ACK);
        participantMessageListener.onTopicEvent(automationCompositionAck);
        verify(supervisionAcHandler).handleAutomationCompositionUpdateAckMessage(automationCompositionAck);
    }

    @Test
    void testAutomationCompositionStateChangeAckListener() {
        final var automationCompositionAck =
                new AutomationCompositionDeployAck(ParticipantMessageType.AUTOMATION_COMPOSITION_STATECHANGE_ACK);
        participantMessageListener.onTopicEvent(automationCompositionAck);
        verify(supervisionAcHandler).handleAutomationCompositionStateChangeAckMessage(automationCompositionAck);
    }

    @Test
    void testParticipantMessageListener() {
        final var participantReqSync = new ParticipantReqSync();
        participantMessageListener.onTopicEvent(participantReqSync);
        verify(supervisionParticipantHandler).handleParticipantReqSync(participantReqSync);
    }
}
