/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantDeregisterAckPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantRegisterAckPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantRestartPublisher;
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionInfo;
import org.onap.policy.clamp.models.acm.concepts.NodeTemplateState;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantDeregister;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantRegister;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantStatus;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class SupervisionParticipantHandlerTest {

    private static final String AC_INSTANTIATION_CREATE_JSON = "src/test/resources/rest/acm/AutomationComposition.json";

    @Test
    void testHandleParticipantDeregister() {
        var participant = CommonTestData.createParticipant(CommonTestData.getParticipantId());

        var participantProvider = mock(ParticipantProvider.class);
        when(participantProvider.findParticipant(CommonTestData.getParticipantId()))
                .thenReturn(Optional.of(participant));

        var participantDeregisterMessage = new ParticipantDeregister();
        participantDeregisterMessage.setMessageId(UUID.randomUUID());
        participantDeregisterMessage.setParticipantId(CommonTestData.getParticipantId());
        var participantDeregisterAckPublisher = mock(ParticipantDeregisterAckPublisher.class);
        var handler =
                new SupervisionParticipantHandler(participantProvider, mock(ParticipantRegisterAckPublisher.class),
                        participantDeregisterAckPublisher, mock(AutomationCompositionProvider.class),
                        mock(AcDefinitionProvider.class), mock(ParticipantRestartPublisher.class),
                        mock(AcRuntimeParameterGroup.class));

        handler.handleParticipantMessage(participantDeregisterMessage);

        verify(participantProvider).updateParticipant(any());
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
                mock(AcDefinitionProvider.class), mock(ParticipantRestartPublisher.class),
                mock(AcRuntimeParameterGroup.class));
        handler.handleParticipantMessage(participantRegisterMessage);

        verify(participantProvider).saveParticipant(any());
        verify(participantRegisterAckPublisher).send(participantRegisterMessage.getMessageId(),
                CommonTestData.getParticipantId());
    }

    @Test
    void testHandleParticipantRestart() {
        var participantRegisterMessage = new ParticipantRegister();
        participantRegisterMessage.setMessageId(UUID.randomUUID());
        var participantId = CommonTestData.getParticipantId();
        participantRegisterMessage.setParticipantId(participantId);
        var supportedElementType = CommonTestData.createParticipantSupportedElementType();
        participantRegisterMessage.setParticipantSupportedElementType(List.of(supportedElementType));

        var participant = new Participant();
        participant.setParticipantId(participantId);
        var participantProvider = mock(ParticipantProvider.class);
        when(participantProvider.findParticipant(participantId)).thenReturn(Optional.of(participant));
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
        var participantRestartPublisher = mock(ParticipantRestartPublisher.class);
        var handler = new SupervisionParticipantHandler(participantProvider, participantRegisterAckPublisher,
                mock(ParticipantDeregisterAckPublisher.class), automationCompositionProvider, acDefinitionProvider,
                participantRestartPublisher, CommonTestData.getTestParamaterGroup());
        handler.handleParticipantMessage(participantRegisterMessage);

        verify(participantRegisterAckPublisher).send(participantRegisterMessage.getMessageId(), participantId);
        verify(acDefinitionProvider).updateAcDefinition(any(AutomationCompositionDefinition.class),
                eq(CommonTestData.TOSCA_COMP_NAME));
        verify(participantRestartPublisher).send(any(), any(AutomationCompositionDefinition.class), any());
    }

    @Test
    void testHandleParticipantStatus() {
        var participantStatusMessage = createParticipantStatus();
        participantStatusMessage.setAutomationCompositionInfoList(List.of(new AutomationCompositionInfo()));

        var participantProvider = mock(ParticipantProvider.class);
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var handler =
                new SupervisionParticipantHandler(participantProvider, mock(ParticipantRegisterAckPublisher.class),
                        mock(ParticipantDeregisterAckPublisher.class), automationCompositionProvider,
                        mock(AcDefinitionProvider.class), mock(ParticipantRestartPublisher.class),
                        mock(AcRuntimeParameterGroup.class));
        var participant = CommonTestData.createParticipant(CommonTestData.getParticipantId());
        when(participantProvider.findParticipant(CommonTestData.getParticipantId()))
                .thenReturn(Optional.of(participant));
        handler.handleParticipantMessage(participantStatusMessage);

        verify(automationCompositionProvider).upgradeStates(any());
    }

    @Test
    void testAcDefinitionOutProperties() {
        var participantStatusMessage = createParticipantStatus();
        participantStatusMessage.setAutomationCompositionInfoList(List.of(new AutomationCompositionInfo()));
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
        when(acDefinitionProvider.findAcDefinition(compositionId)).thenReturn(Optional.of(acDefinition));

        var participantProvider = mock(ParticipantProvider.class);
        var handler =
                new SupervisionParticipantHandler(participantProvider, mock(ParticipantRegisterAckPublisher.class),
                        mock(ParticipantDeregisterAckPublisher.class), mock(AutomationCompositionProvider.class),
                        acDefinitionProvider, mock(ParticipantRestartPublisher.class),
                       CommonTestData.getTestParamaterGroup());
        handler.handleParticipantMessage(participantStatusMessage);

        verify(acDefinitionProvider).updateAcDefinition(acDefinition, CommonTestData.TOSCA_COMP_NAME);
    }

    @Test
    void testHandleParticipantStatusNotRegisterd() {
        var participantStatusMessage = createParticipantStatus();
        participantStatusMessage.setAutomationCompositionInfoList(List.of(new AutomationCompositionInfo()));

        var participantProvider = mock(ParticipantProvider.class);
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var handler =
                new SupervisionParticipantHandler(participantProvider, mock(ParticipantRegisterAckPublisher.class),
                        mock(ParticipantDeregisterAckPublisher.class), automationCompositionProvider,
                        mock(AcDefinitionProvider.class), mock(ParticipantRestartPublisher.class),
                        mock(AcRuntimeParameterGroup.class));
        handler.handleParticipantMessage(participantStatusMessage);

        verify(participantProvider).saveParticipant(any());
        verify(automationCompositionProvider).upgradeStates(any());
    }

    @Test
    void testHandleParticipantStatusCheckOnline() {
        var participantStatusMessage = createParticipantStatus();
        participantStatusMessage.setAutomationCompositionInfoList(List.of(new AutomationCompositionInfo()));

        var participantProvider = mock(ParticipantProvider.class);
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var handler =
                new SupervisionParticipantHandler(participantProvider, mock(ParticipantRegisterAckPublisher.class),
                        mock(ParticipantDeregisterAckPublisher.class), automationCompositionProvider,
                        mock(AcDefinitionProvider.class), mock(ParticipantRestartPublisher.class),
                        mock(AcRuntimeParameterGroup.class));
        var participant = CommonTestData.createParticipant(CommonTestData.getParticipantId());
        participant.setParticipantState(ParticipantState.OFF_LINE);
        when(participantProvider.findParticipant(CommonTestData.getParticipantId()))
                .thenReturn(Optional.of(participant));
        handler.handleParticipantMessage(participantStatusMessage);

        verify(participantProvider).saveParticipant(any());
        verify(automationCompositionProvider).upgradeStates(any());
    }

    private ParticipantStatus createParticipantStatus() {
        var statusMessage = new ParticipantStatus();
        statusMessage.setParticipantId(CommonTestData.getParticipantId());
        statusMessage.setState(ParticipantState.ON_LINE);
        var supportedElementType = CommonTestData.createParticipantSupportedElementType();
        statusMessage.setParticipantSupportedElementType(List.of(supportedElementType));
        return statusMessage;
    }
}
