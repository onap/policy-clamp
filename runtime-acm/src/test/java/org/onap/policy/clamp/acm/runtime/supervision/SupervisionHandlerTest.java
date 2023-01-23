/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation.
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

import static org.assertj.core.api.Assertions.assertThatCode;
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
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantUpdatePublisher;
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionException;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionAck;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantUpdateAck;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.models.base.PfModelException;

class SupervisionHandlerTest {
    private static final String AC_INSTANTIATION_CREATE_JSON = "src/test/resources/rest/acm/AutomationComposition.json";
    private static final UUID IDENTIFIER = UUID.randomUUID();

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

        var handler = new SupervisionHandler(automationCompositionProvider, acDefinitionProvider,
                mock(AutomationCompositionUpdatePublisher.class), automationCompositionStateChangePublisher,
                mock(ParticipantUpdatePublisher.class));

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
                mock(AutomationCompositionUpdatePublisher.class), automationCompositionStateChangePublisher,
                mock(ParticipantUpdatePublisher.class), AutomationCompositionOrderedState.PASSIVE,
                AutomationCompositionState.UNINITIALISED);

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
                mock(AutomationCompositionUpdatePublisher.class), automationCompositionStateChangePublisher,
                mock(ParticipantUpdatePublisher.class), AutomationCompositionOrderedState.PASSIVE,
                AutomationCompositionState.UNINITIALISED);

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
        var handler = createSupervisionHandler(automationCompositionProvider,
                mock(AutomationCompositionUpdatePublisher.class), mock(AutomationCompositionStateChangePublisher.class),
                mock(ParticipantUpdatePublisher.class), AutomationCompositionOrderedState.PASSIVE,
                AutomationCompositionState.UNINITIALISED);
        var automationCompositionAckMessage =
                new AutomationCompositionAck(ParticipantMessageType.AUTOMATION_COMPOSITION_STATECHANGE_ACK);
        automationCompositionAckMessage.setAutomationCompositionResultMap(Map.of());
        automationCompositionAckMessage.setAutomationCompositionId(IDENTIFIER);

        handler.handleAutomationCompositionStateChangeAckMessage(automationCompositionAckMessage);

        verify(automationCompositionProvider).updateAutomationComposition(any(AutomationComposition.class));
    }

    @Test
    void testHandleAutomationCompositionUpdateAckMessage() {
        var automationCompositionAckMessage =
                new AutomationCompositionAck(ParticipantMessageType.AUTOMATION_COMPOSITION_UPDATE_ACK);
        automationCompositionAckMessage.setParticipantId(CommonTestData.getParticipantId());
        automationCompositionAckMessage.setAutomationCompositionResultMap(Map.of());
        automationCompositionAckMessage.setAutomationCompositionId(IDENTIFIER);
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var handler = createSupervisionHandler(automationCompositionProvider,
                mock(AutomationCompositionUpdatePublisher.class), mock(AutomationCompositionStateChangePublisher.class),
                mock(ParticipantUpdatePublisher.class), AutomationCompositionOrderedState.PASSIVE,
                AutomationCompositionState.UNINITIALISED);

        handler.handleAutomationCompositionUpdateAckMessage(automationCompositionAckMessage);

        verify(automationCompositionProvider).updateAutomationComposition(any(AutomationComposition.class));
    }

    @Test
    void testParticipantUpdateAck() {
        var participantUpdateAckMessage = new ParticipantUpdateAck();
        participantUpdateAckMessage.setParticipantId(CommonTestData.getParticipantId());
        participantUpdateAckMessage.setState(ParticipantState.ON_LINE);
        var handler = createSupervisionHandler(mock(AutomationCompositionProvider.class),
                mock(AutomationCompositionUpdatePublisher.class), mock(AutomationCompositionStateChangePublisher.class),
                mock(ParticipantUpdatePublisher.class), AutomationCompositionOrderedState.PASSIVE,
                AutomationCompositionState.UNINITIALISED);

        assertThatCode(() -> handler.handleParticipantMessage(participantUpdateAckMessage)).doesNotThrowAnyException();
    }

    @Test
    void testHandleSendCommissionMessage() {
        var participantUpdatePublisher = mock(ParticipantUpdatePublisher.class);
        var handler = createSupervisionHandler(mock(AutomationCompositionProvider.class),
                mock(AutomationCompositionUpdatePublisher.class), mock(AutomationCompositionStateChangePublisher.class),
                participantUpdatePublisher, AutomationCompositionOrderedState.PASSIVE,
                AutomationCompositionState.UNINITIALISED);
        var acmDefinition = new AutomationCompositionDefinition();
        handler.handleSendCommissionMessage(acmDefinition);

        verify(participantUpdatePublisher).sendComissioningBroadcast(acmDefinition);
    }

    @Test
    void testHandleSendDeCommissionMessage() throws PfModelException {
        var participantUpdatePublisher = mock(ParticipantUpdatePublisher.class);
        var handler = createSupervisionHandler(mock(AutomationCompositionProvider.class),
                mock(AutomationCompositionUpdatePublisher.class), mock(AutomationCompositionStateChangePublisher.class),
                participantUpdatePublisher, AutomationCompositionOrderedState.PASSIVE,
                AutomationCompositionState.UNINITIALISED);
        handler.handleSendDeCommissionMessage(IDENTIFIER);

        verify(participantUpdatePublisher).sendDecomisioning(IDENTIFIER);
    }

    private SupervisionHandler createSupervisionHandler(AutomationCompositionProvider automationCompositionProvider,
            AutomationCompositionUpdatePublisher automationCompositionUpdatePublisher,
            AutomationCompositionStateChangePublisher automationCompositionStateChangePublisher,
            ParticipantUpdatePublisher participantUpdatePublisher, AutomationCompositionOrderedState orderedState,
            AutomationCompositionState state) {
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Crud");

        automationComposition.setOrderedState(orderedState);
        automationComposition.setState(state);
        when(automationCompositionProvider.findAutomationComposition(IDENTIFIER))
                .thenReturn(Optional.of(automationComposition));

        var acDefinitionProvider = Mockito.mock(AcDefinitionProvider.class);
        when(acDefinitionProvider.getServiceTemplateList(any(), any())).thenReturn(List
                .of(Objects.requireNonNull(InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML))));
        when(acDefinitionProvider.getAcDefinition(automationComposition.getCompositionId()))
                .thenReturn(InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML));

        return new SupervisionHandler(automationCompositionProvider, acDefinitionProvider,
                automationCompositionUpdatePublisher, automationCompositionStateChangePublisher,
                participantUpdatePublisher);
    }

    private SupervisionHandler createSupervisionHandlerForTrigger() {
        return new SupervisionHandler(mock(AutomationCompositionProvider.class), mock(AcDefinitionProvider.class),
                mock(AutomationCompositionUpdatePublisher.class), mock(AutomationCompositionStateChangePublisher.class),
                mock(ParticipantUpdatePublisher.class));
    }

    private SupervisionHandler createSupervisionHandlerForTrigger(
            AutomationCompositionUpdatePublisher automationCompositionUpdatePublisher) {

        return new SupervisionHandler(mock(AutomationCompositionProvider.class), mock(AcDefinitionProvider.class),
                automationCompositionUpdatePublisher, mock(AutomationCompositionStateChangePublisher.class),
                mock(ParticipantUpdatePublisher.class));
    }
}
