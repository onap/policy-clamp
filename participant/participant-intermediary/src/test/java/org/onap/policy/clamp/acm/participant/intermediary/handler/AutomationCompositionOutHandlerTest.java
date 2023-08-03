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

package org.onap.policy.clamp.acm.participant.intermediary.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.acm.participant.intermediary.main.parameters.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionDeployAck;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantPrimeAck;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantStatus;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class AutomationCompositionOutHandlerTest {

    @Test
    void updateAutomationCompositionElementStateNullTest() {
        var cacheProvider = mock(CacheProvider.class);
        var acOutHandler = new AutomationCompositionOutHandler(mock(ParticipantMessagePublisher.class), cacheProvider);

        assertDoesNotThrow(
                () -> acOutHandler.updateAutomationCompositionElementState(null, null, null, null, null, null));

        assertDoesNotThrow(() -> acOutHandler.updateAutomationCompositionElementState(UUID.randomUUID(),
                UUID.randomUUID(), null, null, null, null));

        assertDoesNotThrow(() -> acOutHandler.updateAutomationCompositionElementState(UUID.randomUUID(),
                UUID.randomUUID(), DeployState.DEPLOYED, null, null, null));

        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        when(cacheProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        assertDoesNotThrow(() -> acOutHandler.updateAutomationCompositionElementState(
                automationComposition.getInstanceId(), UUID.randomUUID(), DeployState.DEPLOYED, null, null, null));

        var elementId = automationComposition.getElements().values().iterator().next().getId();
        assertDoesNotThrow(() -> acOutHandler.updateAutomationCompositionElementState(
                automationComposition.getInstanceId(), elementId, null, null, null, null));
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
    void updateAutomationCompositionElementStateRestartedTest() {
        var publisher = mock(ParticipantMessagePublisher.class);
        var cacheProvider = mock(CacheProvider.class);
        var acOutHandler = new AutomationCompositionOutHandler(publisher, cacheProvider);

        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        when(cacheProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        var element = automationComposition.getElements().values().iterator().next();
        element.setRestarting(true);
        acOutHandler.updateAutomationCompositionElementState(automationComposition.getInstanceId(), element.getId(),
                DeployState.DEPLOYED, LockState.LOCKED, StateChangeResult.NO_ERROR, "Restarted");
        verify(publisher).sendAutomationCompositionAck(any(AutomationCompositionDeployAck.class));
        assertThat(element.getRestarting()).isNull();
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
    void updateCompositionStatePrimedTest() {
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getParticipantId()).thenReturn(UUID.randomUUID());
        var publisher = mock(ParticipantMessagePublisher.class);
        var acOutHandler = new AutomationCompositionOutHandler(publisher, cacheProvider);
        var compositionId = UUID.randomUUID();
        acOutHandler.updateCompositionState(compositionId, AcTypeState.PRIMED, StateChangeResult.NO_ERROR,
                "Primed");
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
        var elementId = new ToscaConceptIdentifier("code", "1.0.0");
        var mapAcElementsDefinitions =
                Map.of(compositionId, Map.of(elementId, new AutomationCompositionElementDefinition()));
        when(cacheProvider.getAcElementsDefinitions()).thenReturn(mapAcElementsDefinitions);
        var publisher = mock(ParticipantMessagePublisher.class);
        var acOutHandler = new AutomationCompositionOutHandler(publisher, cacheProvider);

        acOutHandler.sendAcDefinitionInfo(null, null, Map.of());
        verify(publisher, times(0)).sendHeartbeat(any(ParticipantStatus.class));

        acOutHandler.sendAcDefinitionInfo(UUID.randomUUID(), null, Map.of());
        verify(publisher, times(0)).sendHeartbeat(any(ParticipantStatus.class));

        acOutHandler.sendAcDefinitionInfo(compositionId, new ToscaConceptIdentifier("wrong", "1.0.0"), Map.of());
        verify(publisher, times(0)).sendHeartbeat(any(ParticipantStatus.class));

        acOutHandler.sendAcDefinitionInfo(compositionId, elementId, Map.of());
        verify(publisher).sendHeartbeat(any(ParticipantStatus.class));
    }
}
