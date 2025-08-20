/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.runtime.supervision;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.acm.runtime.main.utils.EncryptionUtils;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantDeregisterAckPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantRegisterAckPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantSyncPublisher;
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionInfo;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionRollback;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.NodeTemplateState;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantReplica;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantDeregister;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantRegister;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantReqSync;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatus;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.MessageProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaTopologyTemplate;

class SupervisionParticipantHandlerTest {

    private static final String AC_INSTANTIATION_CREATE_JSON = "src/test/resources/rest/acm/AutomationComposition.json";

    @Test
    void testHandleParticipantDeregister() {
        var participantDeregisterMessage = new ParticipantDeregister();
        participantDeregisterMessage.setMessageId(UUID.randomUUID());
        participantDeregisterMessage.setParticipantId(CommonTestData.getParticipantId());
        var participantDeregisterAckPublisher = mock(ParticipantDeregisterAckPublisher.class);
        var participantProvider = mock(ParticipantProvider.class);
        var handler =
                new SupervisionParticipantHandler(participantProvider, mock(ParticipantRegisterAckPublisher.class),
                        participantDeregisterAckPublisher, mock(AutomationCompositionProvider.class),
                        mock(AcDefinitionProvider.class), mock(ParticipantSyncPublisher.class),
                        mock(MessageProvider.class), mock(EncryptionUtils.class));

        handler.handleParticipantMessage(participantDeregisterMessage);
        verify(participantDeregisterAckPublisher).send(participantDeregisterMessage.getMessageId());

        clearInvocations(participantDeregisterAckPublisher);
        var replica = CommonTestData.createParticipantReplica(CommonTestData.getReplicaId());
        participantDeregisterMessage.setReplicaId(replica.getReplicaId());
        when(participantProvider.findParticipantReplica(replica.getReplicaId()))
                .thenReturn(Optional.of(replica));
        handler.handleParticipantMessage(participantDeregisterMessage);
        verify(participantProvider).deleteParticipantReplica(CommonTestData.getReplicaId());
        verify(participantDeregisterAckPublisher).send(participantDeregisterMessage.getMessageId());
    }

    @Test
    void testHandleParticipantRegister() {
        var participantRegisterMessage = new ParticipantRegister();
        participantRegisterMessage.setMessageId(UUID.randomUUID());
        participantRegisterMessage.setParticipantId(CommonTestData.getParticipantId());
        var supportedElementType = CommonTestData.createParticipantSupportedElementType();
        participantRegisterMessage.setParticipantSupportedElementType(List.of(supportedElementType));

        var participantProvider = mock(ParticipantProvider.class);
        var participantRegisterAckPublisher = mock(ParticipantRegisterAckPublisher.class);
        var handler = new SupervisionParticipantHandler(participantProvider, participantRegisterAckPublisher,
                mock(ParticipantDeregisterAckPublisher.class), mock(AutomationCompositionProvider.class),
                mock(AcDefinitionProvider.class), mock(ParticipantSyncPublisher.class),
                mock(MessageProvider.class), mock(EncryptionUtils.class));
        handler.handleParticipantMessage(participantRegisterMessage);

        verify(participantProvider).saveParticipant(any());
        verify(participantRegisterAckPublisher).send(participantRegisterMessage.getMessageId(),
                CommonTestData.getParticipantId(), null);
    }

    @Test
    void testHandleParticipantSyncRestart() {
        var participantRegisterMessage = new ParticipantRegister();
        participantRegisterMessage.setMessageId(UUID.randomUUID());
        var participantId = CommonTestData.getParticipantId();
        participantRegisterMessage.setParticipantId(participantId);
        var replicaId = CommonTestData.getReplicaId();
        participantRegisterMessage.setReplicaId(replicaId);
        var supportedElementType = CommonTestData.createParticipantSupportedElementType();
        participantRegisterMessage.setParticipantSupportedElementType(List.of(supportedElementType));

        var participant = new Participant();
        var replica = new ParticipantReplica();
        replica.setReplicaId(replicaId);
        replica.setParticipantState(ParticipantState.OFF_LINE);
        participant.setParticipantId(participantId);
        participant.getReplicas().put(replica.getReplicaId(), replica);
        var participantProvider = mock(ParticipantProvider.class);
        when(participantProvider.findParticipant(participantId)).thenReturn(Optional.of(participant));
        when(participantProvider.findParticipantReplica(replicaId)).thenReturn(Optional.of(replica));
        var compositionId = UUID.randomUUID();
        var composition2Id = UUID.randomUUID();
        when(participantProvider.getCompositionIds(participantId)).thenReturn(Set.of(compositionId, composition2Id));

        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var acDefinition = new AutomationCompositionDefinition();
        acDefinition.setState(AcTypeState.COMMISSIONED);
        acDefinition.setCompositionId(composition2Id);
        when(acDefinitionProvider.getAcDefinition(composition2Id)).thenReturn(acDefinition);

        acDefinition = new AutomationCompositionDefinition();
        acDefinition.setCompositionId(compositionId);
        acDefinition.setState(AcTypeState.PRIMED);
        var nodeTemplateState = new NodeTemplateState();
        nodeTemplateState.setParticipantId(participantId);
        acDefinition.setElementStateMap(Map.of("code", nodeTemplateState));
        when(acDefinitionProvider.getAcDefinition(compositionId)).thenReturn(acDefinition);

        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Crud");
        automationComposition.getElements().values().iterator().next().setParticipantId(participantId);
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        when(automationCompositionProvider.getAcInstancesByCompositionId(compositionId))
                .thenReturn(List.of(automationComposition));

        var participantRegisterAckPublisher = mock(ParticipantRegisterAckPublisher.class);
        var participantSyncPublisher = mock(ParticipantSyncPublisher.class);
        var handler = new SupervisionParticipantHandler(participantProvider, participantRegisterAckPublisher,
                mock(ParticipantDeregisterAckPublisher.class), automationCompositionProvider, acDefinitionProvider,
                participantSyncPublisher, mock(MessageProvider.class), mock(EncryptionUtils.class));
        handler.handleParticipantMessage(participantRegisterMessage);

        verify(participantRegisterAckPublisher)
                .send(participantRegisterMessage.getMessageId(), participantId, replicaId);
        verify(acDefinitionProvider, times(0)).updateAcDefinition(any(AutomationCompositionDefinition.class),
                eq(CommonTestData.TOSCA_COMP_NAME));
        verify(participantSyncPublisher, times(0))
                .sendRestartMsg(any(), any(), any(AutomationCompositionDefinition.class), any());
    }

    @Test
    void testHandleParticipantStatusWithInstanceOutProperties() {
        var participantStatusMessage = createParticipantStatus();
        participantStatusMessage.setAutomationCompositionInfoList(List.of(new AutomationCompositionInfo()));
        participantStatusMessage.setCompositionId(UUID.randomUUID());

        var participantProvider = mock(ParticipantProvider.class);
        var participant = CommonTestData.createParticipant(CommonTestData.getParticipantId());
        when(participantProvider.findParticipant(CommonTestData.getParticipantId()))
                .thenReturn(Optional.of(participant));
        var replica = CommonTestData.createParticipantReplica(CommonTestData.getReplicaId());
        participantStatusMessage.setReplicaId(replica.getReplicaId());
        when(participantProvider.findParticipantReplica(replica.getReplicaId()))
                .thenReturn(Optional.of(replica));

        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var messageProvider = mock(MessageProvider.class);
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var handler = new SupervisionParticipantHandler(participantProvider,
                mock(ParticipantRegisterAckPublisher.class), mock(ParticipantDeregisterAckPublisher.class),
                automationCompositionProvider, acDefinitionProvider, mock(ParticipantSyncPublisher.class),
                messageProvider, mock(EncryptionUtils.class));
        handler.handleParticipantMessage(participantStatusMessage);

        verify(messageProvider).saveInstanceOutProperties(any(ParticipantStatus.class));
    }

    @Test
    void testHandleParticipantStatusWithCompositionOutProperties() {
        var participantStatusMessage = createParticipantStatus();
        var participantDefinition = new ParticipantDefinition();
        participantStatusMessage.setParticipantDefinitionUpdates(List.of(participantDefinition));
        participantDefinition.setParticipantId(participantStatusMessage.getParticipantId());
        var acElementDefinition = new AutomationCompositionElementDefinition();
        acElementDefinition.setAcElementDefinitionId(new ToscaConceptIdentifier("code", "1.0.0"));
        participantDefinition.setAutomationCompositionElementDefinitionList(List.of(acElementDefinition));

        var compositionId = UUID.randomUUID();
        participantStatusMessage.setCompositionId(compositionId);
        var acDefinition = new AutomationCompositionDefinition();
        acDefinition.setState(AcTypeState.COMMISSIONED);
        acDefinition.setCompositionId(compositionId);
        var nodeTemplateState = new NodeTemplateState();
        acDefinition.setElementStateMap(
                Map.of(acElementDefinition.getAcElementDefinitionId().getName(), nodeTemplateState));
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var messageProvider = mock(MessageProvider.class);
        when(acDefinitionProvider.findAcDefinition(compositionId)).thenReturn(Optional.of(acDefinition));
        when(acDefinitionProvider.getAcDefinition(compositionId)).thenReturn(acDefinition);

        var participantProvider = mock(ParticipantProvider.class);
        var handler = new SupervisionParticipantHandler(participantProvider,
                mock(ParticipantRegisterAckPublisher.class), mock(ParticipantDeregisterAckPublisher.class),
                mock(AutomationCompositionProvider.class), acDefinitionProvider, mock(ParticipantSyncPublisher.class),
                messageProvider, mock(EncryptionUtils.class));
        handler.handleParticipantMessage(participantStatusMessage);
        verify(messageProvider).saveCompositionOutProperties(any(), any());
    }

    @Test
    void testAcOutPropertiesNotValid() {
        var participantStatusMessage = createParticipantStatus();
        var participantDefinition = new ParticipantDefinition();
        participantStatusMessage.setParticipantDefinitionUpdates(List.of(participantDefinition));
        participantDefinition.setParticipantId(participantStatusMessage.getParticipantId());
        var acElementDefinition = new AutomationCompositionElementDefinition();
        acElementDefinition.setAcElementDefinitionId(new ToscaConceptIdentifier("code", "1.0.0"));
        participantDefinition.setAutomationCompositionElementDefinitionList(List.of(acElementDefinition));

        var compositionId = UUID.randomUUID();
        participantStatusMessage.setCompositionId(compositionId);
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var messageProvider = mock(MessageProvider.class);
        var participantProvider = mock(ParticipantProvider.class);
        var handler = new SupervisionParticipantHandler(participantProvider,
                mock(ParticipantRegisterAckPublisher.class), mock(ParticipantDeregisterAckPublisher.class),
                mock(AutomationCompositionProvider.class), acDefinitionProvider, mock(ParticipantSyncPublisher.class),
                messageProvider, mock(EncryptionUtils.class));
        handler.handleParticipantMessage(participantStatusMessage);
        verify(messageProvider, times(0)).saveCompositionOutProperties(any(), any());
    }

    @Test
    void testHandleParticipantStatusNotRegisterd() {
        var participantStatusMessage = createParticipantStatus();
        participantStatusMessage.setAutomationCompositionInfoList(List.of());
        participantStatusMessage.setCompositionId(UUID.randomUUID());

        var participantProvider = mock(ParticipantProvider.class);
        var handler =
                new SupervisionParticipantHandler(participantProvider, mock(ParticipantRegisterAckPublisher.class),
                        mock(ParticipantDeregisterAckPublisher.class), mock(AutomationCompositionProvider.class),
                        mock(AcDefinitionProvider.class), mock(ParticipantSyncPublisher.class),
                        mock(MessageProvider.class), mock(EncryptionUtils.class));
        handler.handleParticipantMessage(participantStatusMessage);

        verify(participantProvider).saveParticipant(any());
    }

    @Test
    void testHandleParticipantStatusCheckOnline() {
        var participantStatusMessage = createParticipantStatus();
        participantStatusMessage.setCompositionId(UUID.randomUUID());

        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var acDefinition = new AutomationCompositionDefinition();
        acDefinition.setCompositionId(participantStatusMessage.getCompositionId());
        when(acDefinitionProvider.getAcDefinition(acDefinition.getCompositionId())).thenReturn(acDefinition);

        var participantProvider = mock(ParticipantProvider.class);
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var messageProvider = mock(MessageProvider.class);
        var handler = new SupervisionParticipantHandler(participantProvider,
                mock(ParticipantRegisterAckPublisher.class), mock(ParticipantDeregisterAckPublisher.class),
                automationCompositionProvider, acDefinitionProvider, mock(ParticipantSyncPublisher.class),
                messageProvider, mock(EncryptionUtils.class));
        var participant = CommonTestData.createParticipant(CommonTestData.getParticipantId());
        when(participantProvider.findParticipant(CommonTestData.getParticipantId()))
                .thenReturn(Optional.of(participant));
        handler.handleParticipantMessage(participantStatusMessage);

        verify(participantProvider).saveParticipant(any());
    }

    private ParticipantStatus createParticipantStatus() {
        var statusMessage = new ParticipantStatus();
        statusMessage.setParticipantId(CommonTestData.getParticipantId());
        statusMessage.setState(ParticipantState.ON_LINE);
        var supportedElementType = CommonTestData.createParticipantSupportedElementType();
        statusMessage.setParticipantSupportedElementType(List.of(supportedElementType));
        return statusMessage;
    }

    @Test
    void testHandleParticipantReqSyncComposition() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var acDefinition = new AutomationCompositionDefinition();
        var compositionId = UUID.randomUUID();
        acDefinition.setCompositionId(compositionId);
        when(acDefinitionProvider.getAcDefinition(compositionId)).thenReturn(acDefinition);

        var participantSyncPublisher = mock(ParticipantSyncPublisher.class);
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var handler =
                new SupervisionParticipantHandler(mock(ParticipantProvider.class),
                        mock(ParticipantRegisterAckPublisher.class), mock(ParticipantDeregisterAckPublisher.class),
                        automationCompositionProvider, acDefinitionProvider, participantSyncPublisher,
                        mock(MessageProvider.class), mock(EncryptionUtils.class));
        var participantReqSync = createParticipantReqSync(compositionId);
        handler.handleParticipantReqSync(participantReqSync);
        verify(participantSyncPublisher).sendRestartMsg(CommonTestData.getParticipantId(),
                CommonTestData.getReplicaId(), acDefinition, List.of());

        clearInvocations(participantSyncPublisher);
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Crud");
        automationComposition.setCompositionId(compositionId);
        automationComposition.setInstanceId(UUID.randomUUID());
        automationComposition.setDeployState(DeployState.DEPLOYED);
        when(automationCompositionProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);

        participantReqSync.setAutomationCompositionId(automationComposition.getInstanceId());
        handler.handleParticipantReqSync(participantReqSync);
        verify(participantSyncPublisher).sendRestartMsg(CommonTestData.getParticipantId(),
                CommonTestData.getReplicaId(), acDefinition, List.of(automationComposition));
    }

    @Test
    void testHandleParticipantReqSyncCompositionTarget() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var acDefinition = new AutomationCompositionDefinition();
        var compositionId = UUID.randomUUID();
        acDefinition.setCompositionId(compositionId);
        when(acDefinitionProvider.getAcDefinition(compositionId)).thenReturn(acDefinition);

        var participantSyncPublisher = mock(ParticipantSyncPublisher.class);
        var handler = new SupervisionParticipantHandler(mock(ParticipantProvider.class),
                mock(ParticipantRegisterAckPublisher.class), mock(ParticipantDeregisterAckPublisher.class),
                mock(AutomationCompositionProvider.class), acDefinitionProvider,
                participantSyncPublisher, mock(MessageProvider.class), mock(EncryptionUtils.class));
        var participantReqSync = createParticipantReqSync(null);
        participantReqSync.setCompositionTargetId(compositionId);
        handler.handleParticipantReqSync(participantReqSync);
        verify(participantSyncPublisher).sendRestartMsg(CommonTestData.getParticipantId(),
                CommonTestData.getReplicaId(), acDefinition, List.of());
    }


    @Test
    void testHandleParticipantReqSyncAutomationComposition() {
        var acDefinition = new AutomationCompositionDefinition();
        acDefinition.setServiceTemplate(new ToscaServiceTemplate());
        acDefinition.getServiceTemplate().setToscaTopologyTemplate(new ToscaTopologyTemplate());
        var compositionId = UUID.randomUUID();
        acDefinition.setCompositionId(compositionId);
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        when(acDefinitionProvider.getAcDefinition(compositionId)).thenReturn(acDefinition);
        var automationComposition = new AutomationComposition();
        automationComposition.setElements(Map.of());
        automationComposition.setPhase(1);
        automationComposition.setCompositionTargetId(compositionId);
        automationComposition.setInstanceId(UUID.randomUUID());
        automationComposition.setDeployState(DeployState.MIGRATING);
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        when(automationCompositionProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);

        var participantSyncPublisher = mock(ParticipantSyncPublisher.class);
        var handler =
                new SupervisionParticipantHandler(mock(ParticipantProvider.class),
                        mock(ParticipantRegisterAckPublisher.class), mock(ParticipantDeregisterAckPublisher.class),
                        automationCompositionProvider, acDefinitionProvider, participantSyncPublisher,
                        mock(MessageProvider.class), mock(EncryptionUtils.class));
        var participantReqSync = createParticipantReqSync(null);
        participantReqSync.setAutomationCompositionId(automationComposition.getInstanceId());

        handler.handleParticipantReqSync(participantReqSync);
        verify(participantSyncPublisher).sendSync(automationComposition);

        clearInvocations(participantSyncPublisher);
        automationComposition.setPhase(0);
        when(automationCompositionProvider.getAutomationCompositionRollback(automationComposition.getInstanceId()))
                .thenReturn(new AutomationCompositionRollback());
        handler.handleParticipantReqSync(participantReqSync);
        verify(participantSyncPublisher).sendSync(automationComposition);
    }

    private ParticipantReqSync createParticipantReqSync(UUID compositionId) {
        var participantReqSync = new ParticipantReqSync();
        participantReqSync.setCompositionId(compositionId);
        participantReqSync.setParticipantId(CommonTestData.getParticipantId());
        participantReqSync.setReplicaId(CommonTestData.getReplicaId());
        return participantReqSync;
    }
}
