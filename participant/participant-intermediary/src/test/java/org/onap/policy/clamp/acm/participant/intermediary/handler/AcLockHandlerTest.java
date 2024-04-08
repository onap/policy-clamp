/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation.
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.main.parameters.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionStateChange;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.LockOrder;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class AcLockHandlerTest {

    @Test
    void handleAcStateChangeNullTest() {
        var cacheProvider = mock(CacheProvider.class);
        var ach = new AcLockHandler(cacheProvider, mock(ThreadHandler.class));

        var automationCompositionStateChange = new AutomationCompositionStateChange();
        assertDoesNotThrow(() -> ach.handleAutomationCompositionStateChange(automationCompositionStateChange));

        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        automationCompositionStateChange.setAutomationCompositionId(automationComposition.getInstanceId());
        assertDoesNotThrow(() -> ach.handleAutomationCompositionStateChange(automationCompositionStateChange));
    }

    @Test
    void handleAcStateChangeLockTest() {
        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        automationComposition.setDeployState(DeployState.DEPLOYED);
        automationComposition.setLockState(LockState.UNLOCKED);
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        when(cacheProvider.getCommonProperties(any(UUID.class), any(UUID.class))).thenReturn(Map.of());

        var listener = mock(ThreadHandler.class);
        var ach = new AcLockHandler(cacheProvider, listener);
        Map<ToscaConceptIdentifier, AutomationCompositionElementDefinition> map = new HashMap<>();
        for (var element : automationComposition.getElements().values()) {
            map.put(element.getDefinition(), new AutomationCompositionElementDefinition());
        }
        when(cacheProvider.getAcElementsDefinitions())
                .thenReturn(Map.of(automationComposition.getCompositionId(), map));
        var automationCompositionStateChange = CommonTestData.getStateChange(CommonTestData.getParticipantId(),
                automationComposition.getInstanceId(), DeployOrder.NONE, LockOrder.LOCK);
        ach.handleAutomationCompositionStateChange(automationCompositionStateChange);
        verify(listener, times(automationComposition.getElements().size())).lock(any(), any(), any());
        for (var element : automationComposition.getElements().values()) {
            assertEquals(LockState.LOCKING, element.getLockState());
        }
    }

    @Test
    void handleAcStateChangeUnlockTest() {
        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        automationComposition.setDeployState(DeployState.DEPLOYED);
        automationComposition.setLockState(LockState.LOCKED);
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        when(cacheProvider.getCommonProperties(any(UUID.class), any(UUID.class))).thenReturn(Map.of());

        var listener = mock(ThreadHandler.class);
        var ach = new AcLockHandler(cacheProvider, listener);
        Map<ToscaConceptIdentifier, AutomationCompositionElementDefinition> map = new HashMap<>();
        for (var element : automationComposition.getElements().values()) {
            map.put(element.getDefinition(), new AutomationCompositionElementDefinition());
        }
        when(cacheProvider.getAcElementsDefinitions())
                .thenReturn(Map.of(automationComposition.getCompositionId(), map));
        var automationCompositionStateChange = CommonTestData.getStateChange(CommonTestData.getParticipantId(),
                automationComposition.getInstanceId(), DeployOrder.NONE, LockOrder.UNLOCK);
        ach.handleAutomationCompositionStateChange(automationCompositionStateChange);
        verify(listener, times(automationComposition.getElements().size())).unlock(any(), any(), any());
        for (var element : automationComposition.getElements().values()) {
            assertEquals(LockState.UNLOCKING, element.getLockState());
        }
    }
}
