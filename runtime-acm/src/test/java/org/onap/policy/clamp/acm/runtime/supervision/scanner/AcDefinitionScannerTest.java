/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.runtime.supervision.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.onap.policy.clamp.acm.runtime.util.CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantSyncPublisher;
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.NodeTemplateState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.document.concepts.DocMessage;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.utils.TimestampHelper;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class AcDefinitionScannerTest {

    private static final UUID COMPOSITION_ID = UUID.randomUUID();
    private static final Map<String, Object> OUT_PROPERTIES = Map.of("key", "value");

    @Test
    void testFailScenario() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var participantSyncPublisher = mock(ParticipantSyncPublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");
        var acDefinitionScanner = new AcDefinitionScanner(acDefinitionProvider,
                participantSyncPublisher, acRuntimeParameterGroup);
        var acDefinition = createAutomationCompositionDefinition(AcTypeState.PRIMING, StateChangeResult.NO_ERROR);
        var element = acDefinition.getElementStateMap().values().iterator().next();
        var docMessage = new DocMessage();
        docMessage.setCompositionId(COMPOSITION_ID);
        docMessage.setMessageType(ParticipantMessageType.PARTICIPANT_PRIME_ACK);
        docMessage.setStateChangeResult(StateChangeResult.FAILED);
        docMessage.setCompositionState(AcTypeState.COMMISSIONED);
        docMessage.setParticipantId(element.getParticipantId());
        var result = acDefinitionScanner.scanMessage(acDefinition, docMessage);
        assertTrue(result.isUpdated());
        assertTrue(result.isToBeSync());
        assertEquals(docMessage.getCompositionState(),
                acDefinition.getElementStateMap().get(element.getNodeTemplateStateId().toString()).getState());
        assertEquals(docMessage.getStateChangeResult(), acDefinition.getStateChangeResult());

    }

    @Test
    void testWithWrongData() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var participantSyncPublisher = mock(ParticipantSyncPublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");
        var acDefinitionScanner = new AcDefinitionScanner(acDefinitionProvider,
                participantSyncPublisher, acRuntimeParameterGroup);
        var acDefinition = createAutomationCompositionDefinition(AcTypeState.DEPRIMING, StateChangeResult.NO_ERROR);
        var element = acDefinition.getElementStateMap().values().iterator().next();
        var docMessage = new DocMessage();
        docMessage.setCompositionId(COMPOSITION_ID);
        docMessage.setStateChangeResult(StateChangeResult.NO_ERROR);
        docMessage.setCompositionState(AcTypeState.COMMISSIONED);
        docMessage.setParticipantId(element.getParticipantId());

        // wrong MessageType
        docMessage.setMessageType(ParticipantMessageType.AUTOMATION_COMPOSITION_STATECHANGE_ACK);
        var result = acDefinitionScanner.scanMessage(acDefinition, docMessage);
        assertFalse(result.isUpdated());
        assertFalse(result.isToBeSync());

        // wrong elementId in outProperties
        docMessage.setMessageType(ParticipantMessageType.PARTICIPANT_STATUS);
        docMessage.setOutProperties(OUT_PROPERTIES);
        docMessage.setAcElementDefinitionId(new ToscaConceptIdentifier("wrong", "1.0.1"));
        result = acDefinitionScanner.scanMessage(acDefinition, docMessage);
        assertFalse(result.isUpdated());
        assertFalse(result.isToBeSync());

        // wrong participantId in StateChange
        docMessage.setMessageType(ParticipantMessageType.PARTICIPANT_PRIME_ACK);
        docMessage.setParticipantId(UUID.randomUUID());
        result = acDefinitionScanner.scanMessage(acDefinition, docMessage);
        assertFalse(result.isUpdated());
        assertFalse(result.isToBeSync());
    }

    @Test
    void testScanMessageStateChange() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var participantSyncPublisher = mock(ParticipantSyncPublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");
        var acDefinitionScanner = new AcDefinitionScanner(acDefinitionProvider,
                participantSyncPublisher, acRuntimeParameterGroup);
        var acDefinition = createAutomationCompositionDefinition(AcTypeState.DEPRIMING, StateChangeResult.NO_ERROR);
        var element = acDefinition.getElementStateMap().values().iterator().next();
        var docMessage = new DocMessage();
        docMessage.setCompositionId(COMPOSITION_ID);
        docMessage.setMessageType(ParticipantMessageType.PARTICIPANT_PRIME_ACK);
        docMessage.setStateChangeResult(StateChangeResult.NO_ERROR);
        docMessage.setCompositionState(AcTypeState.COMMISSIONED);
        docMessage.setParticipantId(element.getParticipantId());
        var result = acDefinitionScanner.scanMessage(acDefinition, docMessage);
        assertTrue(result.isUpdated());
        assertFalse(result.isToBeSync());
        assertEquals(docMessage.getCompositionState(),
                acDefinition.getElementStateMap().get(element.getNodeTemplateStateId().toString()).getState());
    }

    @Test
    void testScanMessageOutProperties() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var participantSyncPublisher = mock(ParticipantSyncPublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");
        var acDefinitionScanner = new AcDefinitionScanner(acDefinitionProvider,
                participantSyncPublisher, acRuntimeParameterGroup);
        var acDefinition = createAutomationCompositionDefinition(AcTypeState.DEPRIMING, StateChangeResult.NO_ERROR);
        var element = acDefinition.getElementStateMap().values().iterator().next();
        var docMessage = new DocMessage();
        docMessage.setCompositionId(COMPOSITION_ID);
        docMessage.setMessageType(ParticipantMessageType.PARTICIPANT_STATUS);
        docMessage.setOutProperties(OUT_PROPERTIES);
        docMessage.setAcElementDefinitionId(element.getNodeTemplateId());
        var result = acDefinitionScanner.scanMessage(acDefinition, docMessage);
        assertTrue(result.isUpdated());
        assertTrue(result.isToBeSync());
        assertEquals(docMessage.getOutProperties(),
                acDefinition.getElementStateMap().get(element.getNodeTemplateStateId().toString()).getOutProperties());
    }

    private AutomationCompositionDefinition createAutomationCompositionDefinition(AcTypeState acTypeState,
            StateChangeResult stateChangeResult) {
        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        serviceTemplate.setMetadata(Map.of("compositionId", COMPOSITION_ID));
        var acDefinition = new AutomationCompositionDefinition();
        acDefinition.setState(acTypeState);
        acDefinition.setStateChangeResult(stateChangeResult);
        acDefinition.setCompositionId(COMPOSITION_ID);
        acDefinition.setLastMsg(TimestampHelper.now());
        acDefinition.setServiceTemplate(serviceTemplate);
        var node = new NodeTemplateState();
        node.setState(acTypeState);
        node.setNodeTemplateStateId(UUID.randomUUID());
        node.setParticipantId(UUID.randomUUID());
        node.setNodeTemplateId(new ToscaConceptIdentifier("name", "1.0.0"));
        acDefinition.setElementStateMap(Map.of(node.getNodeTemplateStateId().toString(), node));
        return acDefinition;
    }

    @Test
    void testAcDefinitionPrimeFailed() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var participantSyncPublisher = mock(ParticipantSyncPublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");
        var acDefinitionScanner = new AcDefinitionScanner(acDefinitionProvider,
                participantSyncPublisher, acRuntimeParameterGroup);
        var acDefinition = createAutomationCompositionDefinition(AcTypeState.PRIMING, StateChangeResult.FAILED);
        acDefinitionScanner.scanAutomationCompositionDefinition(acDefinition, new UpdateSync());
        verify(acDefinitionProvider, times(0)).updateAcDefinitionState(any());
        verify(participantSyncPublisher, times(0)).sendSync(any(), any());
    }

    @Test
    void testAcDefinitionPrimeTimeout() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var participantSyncPublisher = mock(ParticipantSyncPublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");
        var acDefinitionScanner = new AcDefinitionScanner(acDefinitionProvider, participantSyncPublisher,
                acRuntimeParameterGroup);
        var acDefinition = createAutomationCompositionDefinition(AcTypeState.DEPRIMING, StateChangeResult.NO_ERROR);
        acDefinitionScanner.scanAutomationCompositionDefinition(acDefinition, new UpdateSync());
        // Ac Definition in Depriming state
        verify(acDefinitionProvider, times(0)).updateAcDefinitionState(any());
        verify(participantSyncPublisher, times(0)).sendSync(any(), any());

        acDefinition.setState(AcTypeState.PRIMING);
        acDefinitionScanner.scanAutomationCompositionDefinition(acDefinition, new UpdateSync());
        // Ac Definition in Priming state
        verify(acDefinitionProvider, times(0)).updateAcDefinitionState(any());
        verify(participantSyncPublisher, times(0)).sendSync(any(), any());

        acRuntimeParameterGroup.getParticipantParameters().setMaxOperationWaitMs(-1);
        acDefinitionScanner = new AcDefinitionScanner(acDefinitionProvider, participantSyncPublisher,
                acRuntimeParameterGroup);
        acDefinition = createAutomationCompositionDefinition(AcTypeState.PRIMING, StateChangeResult.NO_ERROR);
        acDefinitionScanner.scanAutomationCompositionDefinition(acDefinition, new UpdateSync());
        // set Timeout
        verify(acDefinitionProvider).updateAcDefinitionState(acDefinition);
        verify(participantSyncPublisher).sendSync(any(AutomationCompositionDefinition.class), any());

        clearInvocations(acDefinitionProvider);
        clearInvocations(participantSyncPublisher);
        acDefinition = createAutomationCompositionDefinition(AcTypeState.PRIMING, StateChangeResult.TIMEOUT);
        acDefinitionScanner.scanAutomationCompositionDefinition(acDefinition, new UpdateSync());
        // already in Timeout
        verify(acDefinitionProvider, times(0)).updateAcDefinitionState(any());
        verify(participantSyncPublisher, times(0)).sendSync(any(), any());

        clearInvocations(acDefinitionProvider);
        clearInvocations(participantSyncPublisher);
        // retry by the user
        acDefinition.setStateChangeResult(StateChangeResult.NO_ERROR);
        acDefinitionScanner.scanAutomationCompositionDefinition(acDefinition, new UpdateSync());
        // set Timeout
        verify(acDefinitionProvider).updateAcDefinitionState(acDefinition);
        verify(participantSyncPublisher).sendSync(any(AutomationCompositionDefinition.class), any());

        clearInvocations(acDefinitionProvider);
        for (var element : acDefinition.getElementStateMap().values()) {
            element.setState(AcTypeState.PRIMED);
        }
        acDefinitionScanner.scanAutomationCompositionDefinition(acDefinition, new UpdateSync());
        // completed
        verify(acDefinitionProvider).updateAcDefinitionState(acDefinition);
    }
}
