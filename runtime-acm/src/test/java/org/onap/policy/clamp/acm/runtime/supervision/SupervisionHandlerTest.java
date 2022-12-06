/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation.
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.onap.policy.clamp.acm.runtime.util.CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionStateChangePublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionUpdatePublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantDeregisterAckPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantRegisterAckPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantUpdatePublisher;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionException;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.concepts.ParticipantHealthStatus;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionAck;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantDeregister;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantRegister;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantStatus;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantUpdateAck;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class SupervisionHandlerTest {
    private static final String AC_INSTANTIATION_CREATE_JSON = "src/test/resources/rest/acm/AutomationComposition.json";
    private static final ToscaConceptIdentifier identifier = new ToscaConceptIdentifier("PMSHInstance0Crud", "1.0.1");
    private static final ToscaConceptIdentifier participantId = new ToscaConceptIdentifier("ParticipantId", "1.0.0");
    private static final ToscaConceptIdentifier participantType =
            new ToscaConceptIdentifier("ParticipantType", "1.0.0");

    @Test
    void testTriggerAutomationCompositionSupervision() throws AutomationCompositionException {
        var automationCompositionUpdatePublisher = mock(AutomationCompositionUpdatePublisher.class);
        var handler = createSupervisionHandlerForTrigger(automationCompositionUpdatePublisher);

        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Crud");
        automationComposition.setOrderedState(AutomationCompositionOrderedState.PASSIVE);
        automationComposition.setState(AutomationCompositionState.UNINITIALISED);
        handler.triggerAutomationCompositionSupervision(automationComposition);

        verify(automationCompositionUpdatePublisher).send(automationComposition);
    }

    @Test
    void testAcUninitialisedToUninitialised() {
        var handler = createSupervisionHandlerForTrigger();
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Crud");
        automationComposition.setOrderedState(AutomationCompositionOrderedState.UNINITIALISED);
        automationComposition.setState(AutomationCompositionState.UNINITIALISED);

        assertThatThrownBy(() -> handler.triggerAutomationCompositionSupervision(automationComposition))
                .hasMessageMatching("Automation composition is already in state UNINITIALISED");
    }

    @Test
    void testAcUninitialisedToPassive() throws AutomationCompositionException {
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Crud");
        automationComposition.setOrderedState(AutomationCompositionOrderedState.UNINITIALISED);
        automationComposition.setState(AutomationCompositionState.PASSIVE);
        automationComposition.setCompositionId(UUID.randomUUID());

        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var acDefinitionProvider = Mockito.mock(AcDefinitionProvider.class);
        when(acDefinitionProvider.getAcDefinition(automationComposition.getCompositionId())).thenReturn(
                Objects.requireNonNull(InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML)));

        var automationCompositionStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);

        var handler = new SupervisionHandler(automationCompositionProvider, mock(ParticipantProvider.class),
                acDefinitionProvider, mock(AutomationCompositionUpdatePublisher.class),
                automationCompositionStateChangePublisher, mock(ParticipantRegisterAckPublisher.class),
                mock(ParticipantDeregisterAckPublisher.class), mock(ParticipantUpdatePublisher.class));

        handler.triggerAutomationCompositionSupervision(automationComposition);

        verify(automationCompositionStateChangePublisher).send(any(AutomationComposition.class), eq(1));
    }

    @Test
    void testAcPassiveToPassive() {
        var handler = createSupervisionHandlerForTrigger();
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Crud");
        automationComposition.setOrderedState(AutomationCompositionOrderedState.PASSIVE);
        automationComposition.setState(AutomationCompositionState.PASSIVE);

        assertThatThrownBy(() -> handler.triggerAutomationCompositionSupervision(automationComposition))
                .hasMessageMatching("Automation composition is already in state PASSIVE");
    }

    @Test
    void testAcTransitioning() {
        var handler = createSupervisionHandlerForTrigger();
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Crud");
        automationComposition.setOrderedState(AutomationCompositionOrderedState.UNINITIALISED);
        automationComposition.setState(AutomationCompositionState.PASSIVE2UNINITIALISED);

        assertThatThrownBy(() -> handler.triggerAutomationCompositionSupervision(automationComposition))
                .hasMessageMatching("Automation composition is already in state "
                        + "PASSIVE2UNINITIALISED and transitioning to state UNINITIALISED");

        automationComposition.setOrderedState(AutomationCompositionOrderedState.PASSIVE);
        automationComposition.setState(AutomationCompositionState.UNINITIALISED2PASSIVE);
        assertThatThrownBy(() -> handler.triggerAutomationCompositionSupervision(automationComposition))
                .hasMessageMatching("Automation composition is already in state "
                        + "UNINITIALISED2PASSIVE and transitioning to state PASSIVE");

        automationComposition.setOrderedState(AutomationCompositionOrderedState.RUNNING);
        automationComposition.setState(AutomationCompositionState.PASSIVE2RUNNING);
        assertThatThrownBy(() -> handler.triggerAutomationCompositionSupervision(automationComposition))
                .hasMessageMatching("Automation composition is already in state "
                        + "PASSIVE2RUNNING and transitioning to state RUNNING");
    }

    @Test
    void testAcRunningToPassive() throws AutomationCompositionException {
        var automationCompositionStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var handler = createSupervisionHandler(mock(AutomationCompositionProvider.class),
                mock(ParticipantProvider.class), mock(ParticipantRegisterAckPublisher.class),
                mock(ParticipantDeregisterAckPublisher.class), mock(AutomationCompositionUpdatePublisher.class),
                automationCompositionStateChangePublisher, mock(ParticipantUpdatePublisher.class),
                AutomationCompositionOrderedState.PASSIVE, AutomationCompositionState.UNINITIALISED);

        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Crud");
        automationComposition.setOrderedState(AutomationCompositionOrderedState.PASSIVE);
        automationComposition.setState(AutomationCompositionState.RUNNING);

        handler.triggerAutomationCompositionSupervision(automationComposition);

        verify(automationCompositionStateChangePublisher).send(any(AutomationComposition.class), eq(1));
    }

    @Test
    void testAcRunningToRunning() {
        var handler = createSupervisionHandlerForTrigger();

        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Crud");
        automationComposition.setOrderedState(AutomationCompositionOrderedState.RUNNING);
        automationComposition.setState(AutomationCompositionState.RUNNING);

        assertThatThrownBy(() -> handler.triggerAutomationCompositionSupervision(automationComposition))
                .hasMessageMatching("Automation composition is already in state RUNNING");
    }

    @Test
    void testAcRunningToUninitialised() {
        var handler = createSupervisionHandlerForTrigger();

        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Crud");
        automationComposition.setOrderedState(AutomationCompositionOrderedState.RUNNING);
        automationComposition.setState(AutomationCompositionState.UNINITIALISED);

        assertThatThrownBy(() -> handler.triggerAutomationCompositionSupervision(automationComposition))
                .hasMessageMatching(
                        "Automation composition can't transition from state UNINITIALISED to state RUNNING");
    }

    @Test
    void testAcPassiveToRunning() throws AutomationCompositionException {
        var automationCompositionStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var handler = createSupervisionHandler(mock(AutomationCompositionProvider.class),
                mock(ParticipantProvider.class), mock(ParticipantRegisterAckPublisher.class),
                mock(ParticipantDeregisterAckPublisher.class), mock(AutomationCompositionUpdatePublisher.class),
                automationCompositionStateChangePublisher, mock(ParticipantUpdatePublisher.class),
                AutomationCompositionOrderedState.PASSIVE, AutomationCompositionState.UNINITIALISED);

        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Crud");
        automationComposition.setOrderedState(AutomationCompositionOrderedState.RUNNING);
        automationComposition.setState(AutomationCompositionState.PASSIVE);

        handler.triggerAutomationCompositionSupervision(automationComposition);

        verify(automationCompositionStateChangePublisher).send(any(AutomationComposition.class), eq(0));
    }

    @Test
    void testHandleAutomationCompositionStateChangeAckMessage() {
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var handler = createSupervisionHandler(automationCompositionProvider, mock(ParticipantProvider.class),
                mock(ParticipantRegisterAckPublisher.class), mock(ParticipantDeregisterAckPublisher.class),
                mock(AutomationCompositionUpdatePublisher.class), mock(AutomationCompositionStateChangePublisher.class),
                mock(ParticipantUpdatePublisher.class), AutomationCompositionOrderedState.PASSIVE,
                AutomationCompositionState.UNINITIALISED);
        var automationCompositionAckMessage =
                new AutomationCompositionAck(ParticipantMessageType.AUTOMATION_COMPOSITION_STATECHANGE_ACK);
        automationCompositionAckMessage.setAutomationCompositionResultMap(Map.of());
        automationCompositionAckMessage.setAutomationCompositionId(identifier);

        handler.handleAutomationCompositionStateChangeAckMessage(automationCompositionAckMessage);

        verify(automationCompositionProvider).saveAutomationComposition(any(AutomationComposition.class));
    }

    @Test
    void testHandleAutomationCompositionUpdateAckMessage() {
        var automationCompositionAckMessage =
                new AutomationCompositionAck(ParticipantMessageType.AUTOMATION_COMPOSITION_UPDATE_ACK);
        automationCompositionAckMessage.setParticipantId(participantId);
        automationCompositionAckMessage.setParticipantType(participantType);
        automationCompositionAckMessage.setAutomationCompositionResultMap(Map.of());
        automationCompositionAckMessage.setAutomationCompositionId(identifier);
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var handler = createSupervisionHandler(automationCompositionProvider, mock(ParticipantProvider.class),
                mock(ParticipantRegisterAckPublisher.class), mock(ParticipantDeregisterAckPublisher.class),
                mock(AutomationCompositionUpdatePublisher.class), mock(AutomationCompositionStateChangePublisher.class),
                mock(ParticipantUpdatePublisher.class), AutomationCompositionOrderedState.PASSIVE,
                AutomationCompositionState.UNINITIALISED);

        handler.handleAutomationCompositionUpdateAckMessage(automationCompositionAckMessage);

        verify(automationCompositionProvider).saveAutomationComposition(any(AutomationComposition.class));
    }

    @Test
    void testHandleParticipantDeregister() throws PfModelException {
        var participant = new Participant();
        participant.setName(participantId.getName());
        participant.setVersion(participantId.getVersion());
        participant.setParticipantType(participantType);

        var participantProvider = mock(ParticipantProvider.class);
        when(participantProvider.findParticipant(participantId.getName(), participantId.getVersion()))
                .thenReturn(Optional.of(participant));

        var participantDeregisterMessage = new ParticipantDeregister();
        participantDeregisterMessage.setMessageId(UUID.randomUUID());
        participantDeregisterMessage.setParticipantId(participantId);
        participantDeregisterMessage.setParticipantType(participantType);
        var participantDeregisterAckPublisher = mock(ParticipantDeregisterAckPublisher.class);
        var handler = createSupervisionHandler(mock(AutomationCompositionProvider.class), participantProvider,
                mock(ParticipantRegisterAckPublisher.class), participantDeregisterAckPublisher,
                mock(AutomationCompositionUpdatePublisher.class), mock(AutomationCompositionStateChangePublisher.class),
                mock(ParticipantUpdatePublisher.class), AutomationCompositionOrderedState.PASSIVE,
                AutomationCompositionState.UNINITIALISED);

        handler.handleParticipantMessage(participantDeregisterMessage);

        verify(participantProvider).saveParticipant(any());
        verify(participantDeregisterAckPublisher).send(participantDeregisterMessage.getMessageId());
    }

    @Test
    void testHandleParticipantRegister() throws PfModelException {
        var participant = new Participant();
        participant.setName(participantId.getName());
        participant.setVersion(participantId.getVersion());
        participant.setParticipantType(participantType);

        var participantRegisterMessage = new ParticipantRegister();
        participantRegisterMessage.setMessageId(UUID.randomUUID());
        participantRegisterMessage.setParticipantId(participantId);
        participantRegisterMessage.setParticipantType(participantType);
        var participantProvider = mock(ParticipantProvider.class);
        var participantRegisterAckPublisher = mock(ParticipantRegisterAckPublisher.class);
        var handler = createSupervisionHandler(mock(AutomationCompositionProvider.class), participantProvider,
                participantRegisterAckPublisher, mock(ParticipantDeregisterAckPublisher.class),
                mock(AutomationCompositionUpdatePublisher.class), mock(AutomationCompositionStateChangePublisher.class),
                mock(ParticipantUpdatePublisher.class), AutomationCompositionOrderedState.PASSIVE,
                AutomationCompositionState.UNINITIALISED);

        handler.handleParticipantMessage(participantRegisterMessage);

        verify(participantProvider).saveParticipant(any());
        verify(participantRegisterAckPublisher).send(participantRegisterMessage.getMessageId(), participantId,
                participantType);
    }

    @Test
    void testParticipantUpdateAck() throws PfModelException {
        var participant = new Participant();
        participant.setName(participantId.getName());
        participant.setVersion(participantId.getVersion());
        participant.setParticipantType(participantType);

        var participantProvider = mock(ParticipantProvider.class);
        when(participantProvider.findParticipant(participantId.getName(), participantId.getVersion()))
                .thenReturn(Optional.of(participant));

        var participantUpdateAckMessage = new ParticipantUpdateAck();
        participantUpdateAckMessage.setParticipantId(participantId);
        participantUpdateAckMessage.setParticipantType(participantType);
        participantUpdateAckMessage.setState(ParticipantState.PASSIVE);
        var handler = createSupervisionHandler(mock(AutomationCompositionProvider.class), participantProvider,
                mock(ParticipantRegisterAckPublisher.class), mock(ParticipantDeregisterAckPublisher.class),
                mock(AutomationCompositionUpdatePublisher.class), mock(AutomationCompositionStateChangePublisher.class),
                mock(ParticipantUpdatePublisher.class), AutomationCompositionOrderedState.PASSIVE,
                AutomationCompositionState.UNINITIALISED);

        handler.handleParticipantMessage(participantUpdateAckMessage);

        verify(participantProvider).saveParticipant(any());
    }

    @Test
    void testHandleParticipantStatus() throws PfModelException {
        var participantStatusMessage = new ParticipantStatus();
        participantStatusMessage.setParticipantId(participantId);
        participantStatusMessage.setParticipantType(participantType);
        participantStatusMessage.setState(ParticipantState.PASSIVE);
        participantStatusMessage.setHealthStatus(ParticipantHealthStatus.HEALTHY);

        var participantProvider = mock(ParticipantProvider.class);
        var handler = createSupervisionHandler(mock(AutomationCompositionProvider.class), participantProvider,
                mock(ParticipantRegisterAckPublisher.class), mock(ParticipantDeregisterAckPublisher.class),
                mock(AutomationCompositionUpdatePublisher.class), mock(AutomationCompositionStateChangePublisher.class),
                mock(ParticipantUpdatePublisher.class), AutomationCompositionOrderedState.PASSIVE,
                AutomationCompositionState.UNINITIALISED);
        handler.handleParticipantMessage(participantStatusMessage);

        verify(participantProvider).saveParticipant(any());
    }

    @Test
    void testHandleSendCommissionMessage() throws PfModelException {
        var participantUpdatePublisher = mock(ParticipantUpdatePublisher.class);
        var handler = createSupervisionHandler(mock(AutomationCompositionProvider.class),
                mock(ParticipantProvider.class), mock(ParticipantRegisterAckPublisher.class),
                mock(ParticipantDeregisterAckPublisher.class), mock(AutomationCompositionUpdatePublisher.class),
                mock(AutomationCompositionStateChangePublisher.class), participantUpdatePublisher,
                AutomationCompositionOrderedState.PASSIVE, AutomationCompositionState.UNINITIALISED);
        handler.handleSendCommissionMessage(participantId.getName(), participantId.getVersion());

        verify(participantUpdatePublisher).sendComissioningBroadcast(participantId.getName(),
                participantId.getVersion());
    }

    @Test
    void testHandleSendDeCommissionMessage() throws PfModelException {
        var participantUpdatePublisher = mock(ParticipantUpdatePublisher.class);
        var handler = createSupervisionHandler(mock(AutomationCompositionProvider.class),
                mock(ParticipantProvider.class), mock(ParticipantRegisterAckPublisher.class),
                mock(ParticipantDeregisterAckPublisher.class), mock(AutomationCompositionUpdatePublisher.class),
                mock(AutomationCompositionStateChangePublisher.class), participantUpdatePublisher,
                AutomationCompositionOrderedState.PASSIVE, AutomationCompositionState.UNINITIALISED);
        handler.handleSendDeCommissionMessage();

        verify(participantUpdatePublisher).sendDecomisioning();
    }

    private SupervisionHandler createSupervisionHandler(AutomationCompositionProvider automationCompositionProvider,
            ParticipantProvider participantProvider, ParticipantRegisterAckPublisher participantRegisterAckPublisher,
            ParticipantDeregisterAckPublisher participantDeregisterAckPublisher,
            AutomationCompositionUpdatePublisher automationCompositionUpdatePublisher,
            AutomationCompositionStateChangePublisher automationCompositionStateChangePublisher,
            ParticipantUpdatePublisher participantUpdatePublisher, AutomationCompositionOrderedState orderedState,
            AutomationCompositionState state) {
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Crud");

        automationComposition.setOrderedState(orderedState);
        automationComposition.setState(state);
        when(automationCompositionProvider.findAutomationComposition(identifier))
                .thenReturn(Optional.of(automationComposition));

        var acDefinitionProvider = Mockito.mock(AcDefinitionProvider.class);
        when(acDefinitionProvider.getServiceTemplateList(any(), any())).thenReturn(List
                .of(Objects.requireNonNull(InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML))));
        when(acDefinitionProvider.getAcDefinition(automationComposition.getCompositionId()))
                .thenReturn(InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML));

        return new SupervisionHandler(automationCompositionProvider, participantProvider, acDefinitionProvider,
                automationCompositionUpdatePublisher, automationCompositionStateChangePublisher,
                participantRegisterAckPublisher, participantDeregisterAckPublisher, participantUpdatePublisher);
    }

    private SupervisionHandler createSupervisionHandlerForTrigger() {
        return new SupervisionHandler(mock(AutomationCompositionProvider.class), mock(ParticipantProvider.class),
                mock(AcDefinitionProvider.class), mock(AutomationCompositionUpdatePublisher.class),
                mock(AutomationCompositionStateChangePublisher.class), mock(ParticipantRegisterAckPublisher.class),
                mock(ParticipantDeregisterAckPublisher.class), mock(ParticipantUpdatePublisher.class));
    }

    private SupervisionHandler createSupervisionHandlerForTrigger(
            AutomationCompositionUpdatePublisher automationCompositionUpdatePublisher) {

        return new SupervisionHandler(mock(AutomationCompositionProvider.class), mock(ParticipantProvider.class),
                mock(AcDefinitionProvider.class), automationCompositionUpdatePublisher,
                mock(AutomationCompositionStateChangePublisher.class), mock(ParticipantRegisterAckPublisher.class),
                mock(ParticipantDeregisterAckPublisher.class), mock(ParticipantUpdatePublisher.class));
    }
}
