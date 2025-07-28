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

package org.onap.policy.clamp.acm.participant.intermediary.handler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.acm.participant.intermediary.handler.cache.AcDefinition;
import org.onap.policy.clamp.acm.participant.intermediary.handler.cache.CacheProvider;
import org.onap.policy.clamp.acm.participant.intermediary.main.parameters.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeployAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrimeAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatus;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class AutomationCompositionOutHandlerTest {

    private static final ToscaConceptIdentifier ELEMENT_ID = new ToscaConceptIdentifier("code", "1.0.0");

    @Test
    void updateAutomationCompositionElementStateNullTest() {
        var cacheProvider = mock(CacheProvider.class);
        var publisher = mock(ParticipantMessagePublisher.class);
        var acOutHandler = new AutomationCompositionOutHandler(publisher, cacheProvider);

        assertDoesNotThrow(
                () -> acOutHandler.updateAutomationCompositionElementState(null, null, null, null, null, null));

        assertDoesNotThrow(() -> acOutHandler.updateAutomationCompositionElementState(null,
                UUID.randomUUID(), null, null, null, null));

        assertDoesNotThrow(() -> acOutHandler.updateAutomationCompositionElementState(UUID.randomUUID(),
                null, null, null, null, null));

        assertDoesNotThrow(() -> acOutHandler.updateAutomationCompositionElementState(UUID.randomUUID(),
                UUID.randomUUID(), null, null, null, null));

        assertDoesNotThrow(() -> acOutHandler.updateAutomationCompositionElementState(UUID.randomUUID(),
                UUID.randomUUID(), DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, null));

        assertDoesNotThrow(() -> acOutHandler.updateAutomationCompositionElementState(UUID.randomUUID(),
                UUID.randomUUID(), DeployState.DEPLOYED, null, null, null));

        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        when(cacheProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        assertDoesNotThrow(() -> acOutHandler.updateAutomationCompositionElementState(
                automationComposition.getInstanceId(), UUID.randomUUID(), DeployState.DEPLOYED,
            null, StateChangeResult.NO_ERROR, null));

        var elementId = automationComposition.getElements().values().iterator().next().getId();
        assertDoesNotThrow(() -> acOutHandler.updateAutomationCompositionElementState(
                automationComposition.getInstanceId(), elementId, null, null,
                StateChangeResult.NO_ERROR, null));

        assertDoesNotThrow(() -> acOutHandler.updateAutomationCompositionElementStage(
                elementId, null, StateChangeResult.NO_ERROR, 0, null));
        assertDoesNotThrow(() -> acOutHandler.updateAutomationCompositionElementStage(
                null, elementId, StateChangeResult.NO_ERROR, 0, null));
        assertDoesNotThrow(() -> acOutHandler.updateAutomationCompositionElementStage(
                UUID.randomUUID(), elementId, StateChangeResult.NO_ERROR, 0, null));
        assertDoesNotThrow(() -> acOutHandler.updateAutomationCompositionElementStage(
                automationComposition.getInstanceId(), UUID.randomUUID(),
                StateChangeResult.NO_ERROR, 0, null));
        assertDoesNotThrow(() -> acOutHandler.updateAutomationCompositionElementState(
                automationComposition.getInstanceId(), elementId, DeployState.DEPLOYED, LockState.LOCKED,
                StateChangeResult.NO_ERROR, null));
        assertDoesNotThrow(() -> acOutHandler.updateAutomationCompositionElementState(
                automationComposition.getInstanceId(), elementId, DeployState.DEPLOYING, null,
                StateChangeResult.NO_ERROR, ""));
        assertDoesNotThrow(() -> acOutHandler.updateAutomationCompositionElementState(
                automationComposition.getInstanceId(), elementId, DeployState.DEPLOYED, null,
                StateChangeResult.TIMEOUT, ""));

        verify(publisher, times(0)).sendAutomationCompositionAck(any());
    }

    @Test
    void updateAutomationCompositionElementStageTest() {
        var publisher = mock(ParticipantMessagePublisher.class);
        var cacheProvider = mock(CacheProvider.class);
        var acOutHandler = new AutomationCompositionOutHandler(publisher, cacheProvider);
        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        when(cacheProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        var elementId = automationComposition.getElements().values().iterator().next().getId();
        acOutHandler.updateAutomationCompositionElementStage(
                automationComposition.getInstanceId(), elementId, StateChangeResult.NO_ERROR, 0, "OK");
        verify(publisher).sendAutomationCompositionAck(any(AutomationCompositionDeployAck.class));
    }

    @Test
    void updateAutomationCompositionElementStateDeployedTest() {
        var publisher = mock(ParticipantMessagePublisher.class);
        var cacheProvider = mock(CacheProvider.class);
        var acOutHandler = new AutomationCompositionOutHandler(publisher, cacheProvider);

        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        when(cacheProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        var elementId = automationComposition.getElements().values().iterator().next().getId();
        acOutHandler.updateAutomationCompositionElementState(automationComposition.getInstanceId(), elementId,
                DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Deployed");
        verify(publisher).sendAutomationCompositionAck(any(AutomationCompositionDeployAck.class));
    }

    @Test
    void updateAutomationCompositionElementStatePrepareTest() {
        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        automationComposition.setSubState(SubState.PREPARING);
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        var element = automationComposition.getElements().values().iterator().next();
        element.setSubState(SubState.PREPARING);
        var elementId = element.getId();
        var publisher = mock(ParticipantMessagePublisher.class);
        var acOutHandler = new AutomationCompositionOutHandler(publisher, cacheProvider);
        acOutHandler.updateAutomationCompositionElementState(automationComposition.getInstanceId(), elementId,
                DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Prepare completed");
        verify(publisher).sendAutomationCompositionAck(any(AutomationCompositionDeployAck.class));
    }

    @Test
    void updateAcElementStatePrepareFailTest() {
        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        automationComposition.setSubState(SubState.PREPARING);
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        var element = automationComposition.getElements().values().iterator().next();
        element.setSubState(SubState.PREPARING);
        var elementId = element.getId();
        var publisher = mock(ParticipantMessagePublisher.class);
        var acOutHandler = new AutomationCompositionOutHandler(publisher, cacheProvider);
        acOutHandler.updateAutomationCompositionElementState(automationComposition.getInstanceId(), elementId,
                DeployState.DEPLOYED, null, StateChangeResult.FAILED, "Prepare failed");
        verify(publisher).sendAutomationCompositionAck(any(AutomationCompositionDeployAck.class));
    }

    @Test
    void updateAutomationCompositionElementStateLockTest() {
        var publisher = mock(ParticipantMessagePublisher.class);
        var cacheProvider = mock(CacheProvider.class);
        var acOutHandler = new AutomationCompositionOutHandler(publisher, cacheProvider);

        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        when(cacheProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        var elementId = automationComposition.getElements().values().iterator().next().getId();
        acOutHandler.updateAutomationCompositionElementState(automationComposition.getInstanceId(), elementId, null,
                LockState.LOCKED, StateChangeResult.NO_ERROR, "Locked");
        verify(publisher).sendAutomationCompositionAck(any(AutomationCompositionDeployAck.class));
    }

    @Test
    void updateAutomationCompositionElementStateDeleteTest() {
        var publisher = mock(ParticipantMessagePublisher.class);
        var cacheProvider = mock(CacheProvider.class);
        var acOutHandler = new AutomationCompositionOutHandler(publisher, cacheProvider);

        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        when(cacheProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        for (var element : automationComposition.getElements().values()) {
            acOutHandler.updateAutomationCompositionElementState(automationComposition.getInstanceId(), element.getId(),
                    DeployState.DELETED, null, StateChangeResult.NO_ERROR, "Deleted");
        }
        verify(publisher, times(automationComposition.getElements().size()))
                .sendAutomationCompositionAck(any(AutomationCompositionDeployAck.class));
        verify(cacheProvider).removeAutomationComposition(automationComposition.getInstanceId());
    }

    @Test
    void sendAcElementInfoTestNull() {
        var publisher = mock(ParticipantMessagePublisher.class);
        var cacheProvider = mock(CacheProvider.class);
        var acOutHandler = new AutomationCompositionOutHandler(publisher, cacheProvider);

        assertDoesNotThrow(() -> acOutHandler.sendAcElementInfo(null, null, null, null, null));
        assertDoesNotThrow(() -> acOutHandler.sendAcElementInfo(UUID.randomUUID(), null, null, null, null));
        assertDoesNotThrow(
                () -> acOutHandler.sendAcElementInfo(UUID.randomUUID(), UUID.randomUUID(), null, null, null));

        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        when(cacheProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        assertDoesNotThrow(() -> acOutHandler.sendAcElementInfo(automationComposition.getInstanceId(),
                UUID.randomUUID(), null, null, null));
    }

    @Test
    void sendAcElementInfoTest() {
        var publisher = mock(ParticipantMessagePublisher.class);
        var cacheProvider = mock(CacheProvider.class);
        var acOutHandler = new AutomationCompositionOutHandler(publisher, cacheProvider);

        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        when(cacheProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        var elementId = automationComposition.getElements().values().iterator().next().getId();
        acOutHandler.sendAcElementInfo(automationComposition.getInstanceId(), elementId, "", "", Map.of());
        verify(publisher).sendParticipantStatus(any(ParticipantStatus.class));
    }

    @Test
    void updateCompositionStateNullTest() {
        var publisher = mock(ParticipantMessagePublisher.class);
        var cacheProvider = mock(CacheProvider.class);
        var acOutHandler = new AutomationCompositionOutHandler(publisher, cacheProvider);

        assertDoesNotThrow(
                () -> acOutHandler.updateCompositionState(null, null, null, null));
        assertDoesNotThrow(() -> acOutHandler.updateCompositionState(UUID.randomUUID(), null,
                                StateChangeResult.NO_ERROR, null));
        assertDoesNotThrow(
                () -> acOutHandler.updateCompositionState(UUID.randomUUID(), AcTypeState.PRIMED, null, null));
        assertDoesNotThrow(() -> acOutHandler.updateCompositionState(UUID.randomUUID(), AcTypeState.PRIMING,
                StateChangeResult.NO_ERROR, null));
        assertDoesNotThrow(() -> acOutHandler.updateCompositionState(UUID.randomUUID(), AcTypeState.DEPRIMING,
                StateChangeResult.NO_ERROR, null));
        assertDoesNotThrow(() -> acOutHandler.updateCompositionState(UUID.randomUUID(), AcTypeState.PRIMED,
                StateChangeResult.TIMEOUT, null));

        verify(publisher, times(0)).sendParticipantPrimeAck(any());
    }

    @Test
    void updateCompositionStatePrimedTest() {
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getParticipantId()).thenReturn(UUID.randomUUID());
        var publisher = mock(ParticipantMessagePublisher.class);
        var acOutHandler = new AutomationCompositionOutHandler(publisher, cacheProvider);
        var compositionId = UUID.randomUUID();
        acOutHandler.updateCompositionState(compositionId, AcTypeState.PRIMED, StateChangeResult.NO_ERROR, "Primed");
        verify(publisher).sendParticipantPrimeAck(any(ParticipantPrimeAck.class));
        verify(cacheProvider, times(0)).removeElementDefinition(compositionId);
    }

    @Test
    void updateCompositionStateDeprimingTest() {
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getParticipantId()).thenReturn(UUID.randomUUID());
        var publisher = mock(ParticipantMessagePublisher.class);
        var acOutHandler = new AutomationCompositionOutHandler(publisher, cacheProvider);
        var compositionId = UUID.randomUUID();
        acOutHandler.updateCompositionState(compositionId, AcTypeState.COMMISSIONED, StateChangeResult.NO_ERROR,
                "Deprimed");
        verify(publisher).sendParticipantPrimeAck(any(ParticipantPrimeAck.class));
        verify(cacheProvider).removeElementDefinition(compositionId);
    }

    @Test
    void sendAcDefinitionInfoTest() {
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getParticipantId()).thenReturn(UUID.randomUUID());
        var compositionId = UUID.randomUUID();
        var acDefinition = new AcDefinition();
        acDefinition.setCompositionId(compositionId);
        acDefinition.getElements().put(ELEMENT_ID, new AutomationCompositionElementDefinition());
        when(cacheProvider.getAcElementsDefinitions()).thenReturn(Map.of(compositionId, acDefinition));
        var publisher = mock(ParticipantMessagePublisher.class);
        var acOutHandler = new AutomationCompositionOutHandler(publisher, cacheProvider);

        acOutHandler.sendAcDefinitionInfo(null, null, Map.of());
        verify(publisher, times(0)).sendParticipantStatus(any(ParticipantStatus.class));

        acOutHandler.sendAcDefinitionInfo(UUID.randomUUID(), null, Map.of());
        verify(publisher, times(0)).sendParticipantStatus(any(ParticipantStatus.class));

        acOutHandler.sendAcDefinitionInfo(compositionId, new ToscaConceptIdentifier("wrong", "1.0.0"), Map.of());
        verify(publisher, times(0)).sendParticipantStatus(any(ParticipantStatus.class));

        acOutHandler.sendAcDefinitionInfo(compositionId, ELEMENT_ID, Map.of());
        verify(publisher).sendParticipantStatus(any(ParticipantStatus.class));
    }

    @Test
    void sendAcDefinitionInfoSingleTest() {
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getParticipantId()).thenReturn(UUID.randomUUID());
        var compositionId = UUID.randomUUID();
        var acDefinition = new AcDefinition();
        acDefinition.setCompositionId(compositionId);
        acDefinition.getElements().put(ELEMENT_ID, new AutomationCompositionElementDefinition());
        when(cacheProvider.getAcElementsDefinitions()).thenReturn(Map.of(compositionId, acDefinition));
        var publisher = mock(ParticipantMessagePublisher.class);
        var acOutHandler = new AutomationCompositionOutHandler(publisher, cacheProvider);

        // if there is only one element
        acOutHandler.sendAcDefinitionInfo(compositionId, null, Map.of());
        verify(publisher).sendParticipantStatus(any(ParticipantStatus.class));
    }

    @Test
    void updateMigrationStatusTest() {
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getParticipantId()).thenReturn(UUID.randomUUID());
        var publisher = mock(ParticipantMessagePublisher.class);
        var acOutHandler = new AutomationCompositionOutHandler(publisher, cacheProvider);

        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        when(cacheProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);

        var compositionTarget = UUID.randomUUID();
        automationComposition.setCompositionTargetId(compositionTarget);
        automationComposition.setDeployState(DeployState.DEPLOYED);
        when(cacheProvider.getAcElementsDefinitions()).thenReturn(Map.of(compositionTarget, new AcDefinition()));

        for (var element : automationComposition.getElements().values()) {
            acOutHandler.updateAutomationCompositionElementState(automationComposition.getInstanceId(), element.getId(),
                    DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "");
        }
        verify(publisher, times(automationComposition.getElements().size()))
                .sendAutomationCompositionAck(any(AutomationCompositionDeployAck.class));
        assertEquals(compositionTarget, automationComposition.getCompositionId());
    }

    @Test
    void updateFailMigrationTest() {
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getParticipantId()).thenReturn(UUID.randomUUID());
        var publisher = mock(ParticipantMessagePublisher.class);
        var acOutHandler = new AutomationCompositionOutHandler(publisher, cacheProvider);

        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        when(cacheProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);

        automationComposition.setCompositionTargetId(UUID.randomUUID());
        automationComposition.setDeployState(DeployState.MIGRATING);
        var compositionId = automationComposition.getCompositionId();
        for (var element : automationComposition.getElements().values()) {
            element.setDeployState(DeployState.MIGRATING);
            acOutHandler.updateAutomationCompositionElementState(automationComposition.getInstanceId(),
                    element.getId(), DeployState.DEPLOYED, null, StateChangeResult.FAILED, "");
        }
        verify(publisher, times(automationComposition.getElements().size()))
                .sendAutomationCompositionAck(any(AutomationCompositionDeployAck.class));
        assertEquals(compositionId, automationComposition.getCompositionId());
    }
}
