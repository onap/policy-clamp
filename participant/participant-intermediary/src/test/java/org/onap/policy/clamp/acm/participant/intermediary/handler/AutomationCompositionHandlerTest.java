/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation.
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionDto;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.acm.participant.intermediary.main.parameters.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDeploy;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeploy;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeployAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionMigration;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionStateChange;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrimeAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.PropertiesUpdate;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.LockOrder;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class AutomationCompositionHandlerTest {

    @Test
    void handleAutomationCompositionStateChangeNullTest() {
        var participantMessagePublisher = mock(ParticipantMessagePublisher.class);
        var cacheProvider = mock(CacheProvider.class);
        var ach =
                new AutomationCompositionHandler(cacheProvider, participantMessagePublisher, mock(ThreadHandler.class));

        var automationCompositionStateChange = new AutomationCompositionStateChange();
        assertDoesNotThrow(() -> ach.handleAutomationCompositionStateChange(automationCompositionStateChange));

        automationCompositionStateChange.setAutomationCompositionId(UUID.randomUUID());
        automationCompositionStateChange.setDeployOrderedState(DeployOrder.DELETE);
        assertDoesNotThrow(() -> ach.handleAutomationCompositionStateChange(automationCompositionStateChange));
        verify(participantMessagePublisher).sendAutomationCompositionAck(any(AutomationCompositionDeployAck.class));

        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        automationCompositionStateChange.setAutomationCompositionId(automationComposition.getInstanceId());
        when(cacheProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        automationCompositionStateChange.setDeployOrderedState(DeployOrder.UPDATE);
        assertDoesNotThrow(() -> ach.handleAutomationCompositionStateChange(automationCompositionStateChange));
    }

    @Test
    void handleAutomationCompositionStateChangeUndeployTest() {
        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        when(cacheProvider.getCommonProperties(any(UUID.class), any(UUID.class))).thenReturn(Map.of());

        var participantMessagePublisher = mock(ParticipantMessagePublisher.class);
        var listener = mock(ThreadHandler.class);
        var ach = new AutomationCompositionHandler(cacheProvider, participantMessagePublisher, listener);
        Map<ToscaConceptIdentifier, AutomationCompositionElementDefinition> map = new HashMap<>();
        for (var element : automationComposition.getElements().values()) {
            map.put(element.getDefinition(), new AutomationCompositionElementDefinition());
        }
        when(cacheProvider.getAcElementsDefinitions())
            .thenReturn(Map.of(automationComposition.getCompositionId(), map));
        var automationCompositionStateChange = CommonTestData.getStateChange(CommonTestData.getParticipantId(),
            automationComposition.getInstanceId(), DeployOrder.UNDEPLOY, LockOrder.NONE);

        ach.handleAutomationCompositionStateChange(automationCompositionStateChange);
        verify(listener, times(automationComposition.getElements().size())).undeploy(any(), any(), any());
        for (var element : automationComposition.getElements().values()) {
            assertEquals(DeployState.UNDEPLOYING, element.getDeployState());
        }
    }

    @Test
    void handleAutomationCompositionStateChangeLockTest() {
        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        when(cacheProvider.getCommonProperties(any(UUID.class), any(UUID.class))).thenReturn(Map.of());

        var participantMessagePublisher = mock(ParticipantMessagePublisher.class);
        var listener = mock(ThreadHandler.class);
        var ach = new AutomationCompositionHandler(cacheProvider, participantMessagePublisher, listener);
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
    void handleAutomationCompositionStateChangeUnlockTest() {
        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        when(cacheProvider.getCommonProperties(any(UUID.class), any(UUID.class))).thenReturn(Map.of());

        var participantMessagePublisher = mock(ParticipantMessagePublisher.class);
        var listener = mock(ThreadHandler.class);
        var ach = new AutomationCompositionHandler(cacheProvider, participantMessagePublisher, listener);
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

    @Test
    void handleAutomationCompositionStateChangeDeleteTest() {
        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        when(cacheProvider.getCommonProperties(any(UUID.class), any(UUID.class))).thenReturn(Map.of());

        var participantMessagePublisher = mock(ParticipantMessagePublisher.class);
        var listener = mock(ThreadHandler.class);
        var ach = new AutomationCompositionHandler(cacheProvider, participantMessagePublisher, listener);
        Map<ToscaConceptIdentifier, AutomationCompositionElementDefinition> map = new HashMap<>();
        for (var element : automationComposition.getElements().values()) {
            map.put(element.getDefinition(), new AutomationCompositionElementDefinition());
        }
        when(cacheProvider.getAcElementsDefinitions())
            .thenReturn(Map.of(automationComposition.getCompositionId(), map));
        var automationCompositionStateChange = CommonTestData.getStateChange(CommonTestData.getParticipantId(),
            automationComposition.getInstanceId(), DeployOrder.DELETE, LockOrder.NONE);
        ach.handleAutomationCompositionStateChange(automationCompositionStateChange);
        verify(listener, times(automationComposition.getElements().size())).delete(any(), any(), any());
        for (var element : automationComposition.getElements().values()) {
            assertEquals(DeployState.DELETING, element.getDeployState());
        }
    }

    @Test
    void handleAcPropertyUpdateTest() {
        var cacheProvider = mock(CacheProvider.class);
        var listener = mock(ThreadHandler.class);
        var participantMessagePublisher = mock(ParticipantMessagePublisher.class);
        var ach = new AutomationCompositionHandler(cacheProvider, participantMessagePublisher, listener);

        var updateMsg = new PropertiesUpdate();
        assertDoesNotThrow(() -> ach.handleAcPropertyUpdate(updateMsg));

        updateMsg.setParticipantId(CommonTestData.getParticipantId());
        when(cacheProvider.getParticipantId()).thenReturn(CommonTestData.getParticipantId());
        var participantDeploy = new ParticipantDeploy();
        participantDeploy.setParticipantId(CommonTestData.getParticipantId());
        updateMsg.getParticipantUpdatesList().add(participantDeploy);

        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        updateMsg.setAutomationCompositionId(automationComposition.getInstanceId());
        when(cacheProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        var acElementDeploy = new AcElementDeploy();
        acElementDeploy.setProperties(Map.of());
        acElementDeploy.setId(automationComposition.getElements().values().iterator().next().getId());
        participantDeploy.getAcElementList().add(acElementDeploy);

        Map<ToscaConceptIdentifier, AutomationCompositionElementDefinition> map = new HashMap<>();
        for (var element : automationComposition.getElements().values()) {
            map.put(element.getDefinition(), new AutomationCompositionElementDefinition());
        }
        when(cacheProvider.getAcElementsDefinitions())
            .thenReturn(Map.of(automationComposition.getCompositionId(), map));
        ach.handleAcPropertyUpdate(updateMsg);
        verify(listener).update(any(), any(), any(), any());
    }

    @Test
    void handleAutomationCompositionDeployTest() {
        var cacheProvider = mock(CacheProvider.class);
        var listener = mock(ThreadHandler.class);
        var participantMessagePublisher = mock(ParticipantMessagePublisher.class);
        var ach = new AutomationCompositionHandler(cacheProvider, participantMessagePublisher, listener);

        var deployMsg = new AutomationCompositionDeploy();
        assertDoesNotThrow(() -> ach.handleAutomationCompositionDeploy(deployMsg));

        deployMsg.setParticipantId(CommonTestData.getParticipantId());
        when(cacheProvider.getParticipantId()).thenReturn(CommonTestData.getParticipantId());
        var participantDeploy = new ParticipantDeploy();
        participantDeploy.setParticipantId(CommonTestData.getParticipantId());
        deployMsg.getParticipantUpdatesList().add(participantDeploy);

        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        deployMsg.setAutomationCompositionId(automationComposition.getInstanceId());
        when(cacheProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        Map<ToscaConceptIdentifier, AutomationCompositionElementDefinition> map = new HashMap<>();
        for (var element : automationComposition.getElements().values()) {
            var acElementDeploy = new AcElementDeploy();
            acElementDeploy.setProperties(Map.of());
            acElementDeploy.setId(element.getId());
            participantDeploy.getAcElementList().add(acElementDeploy);
            map.put(element.getDefinition(), new AutomationCompositionElementDefinition());
        }
        when(cacheProvider.getAcElementsDefinitions())
            .thenReturn(Map.of(automationComposition.getCompositionId(), map));

        ach.handleAutomationCompositionDeploy(deployMsg);
        verify(listener, times(automationComposition.getElements().size())).deploy(any(), any(), any());
    }

    @Test
    void handleComposiotPrimeTest() {
        var acElementDefinition = new AutomationCompositionElementDefinition();
        acElementDefinition.setAcElementDefinitionId(new ToscaConceptIdentifier("key", "1.0.0"));
        var toscaNodeTemplate = new ToscaNodeTemplate();
        toscaNodeTemplate.setProperties(Map.of());
        acElementDefinition.setAutomationCompositionElementToscaNodeTemplate(toscaNodeTemplate);
        var list = List.of(acElementDefinition);
        var compositionId = UUID.randomUUID();
        var messageId = UUID.randomUUID();
        var listener = mock(ThreadHandler.class);
        var ach = new AutomationCompositionHandler(mock(CacheProvider.class), mock(ParticipantMessagePublisher.class),
            listener);
        ach.prime(messageId, compositionId, list);
        verify(listener).prime(any(UUID.class), any(CompositionDto.class));
    }

    @Test
    void handleCompositionDeprimeTest() {
        var acElementDefinition = new AutomationCompositionElementDefinition();
        acElementDefinition.setAcElementDefinitionId(new ToscaConceptIdentifier("key", "1.0.0"));
        var toscaNodeTemplate = new ToscaNodeTemplate();
        toscaNodeTemplate.setProperties(Map.of());
        acElementDefinition.setAutomationCompositionElementToscaNodeTemplate(toscaNodeTemplate);
        var compositionId = UUID.randomUUID();
        var listener = mock(ThreadHandler.class);
        var cacheProvider = mock(CacheProvider.class);
        var ach = new AutomationCompositionHandler(cacheProvider, mock(ParticipantMessagePublisher.class),
            listener);
        when(cacheProvider.getAcElementsDefinitions())
            .thenReturn(Map.of(compositionId, Map.of(new ToscaConceptIdentifier(), acElementDefinition)));
        var messageId = UUID.randomUUID();
        ach.deprime(messageId, compositionId);
        verify(listener).deprime(any(UUID.class), any(CompositionDto.class));
    }

    @Test
    void handleCompositionAlreadyDeprimedTest() {
        var messageId = UUID.randomUUID();
        var compositionId = UUID.randomUUID();
        var participantMessagePublisher =  mock(ParticipantMessagePublisher.class);
        var ach = new AutomationCompositionHandler(mock(CacheProvider.class), participantMessagePublisher,
            mock(ThreadHandler.class));
        ach.deprime(messageId, compositionId);
        verify(participantMessagePublisher).sendParticipantPrimeAck(any(ParticipantPrimeAck.class));
    }

    @Test
    void restartedTest() {
        var acElementDefinition = new AutomationCompositionElementDefinition();
        acElementDefinition.setAcElementDefinitionId(new ToscaConceptIdentifier("key", "1.0.0"));
        var toscaNodeTemplate = new ToscaNodeTemplate();
        toscaNodeTemplate.setProperties(Map.of());
        acElementDefinition.setAutomationCompositionElementToscaNodeTemplate(toscaNodeTemplate);
        var list = List.of(acElementDefinition);
        var state = AcTypeState.PRIMED;
        var participantRestartAc = CommonTestData.createParticipantRestartAc();
        var automationCompositionList = List.of(participantRestartAc);
        var listener = mock(ThreadHandler.class);
        var cacheProvider = mock(CacheProvider.class);
        var ach = new AutomationCompositionHandler(cacheProvider, mock(ParticipantMessagePublisher.class), listener);
        var compositionId = UUID.randomUUID();
        var messageId = UUID.randomUUID();
        ach.restarted(messageId, compositionId, list, state, automationCompositionList);
        verify(cacheProvider).initializeAutomationComposition(compositionId, participantRestartAc);
        verify(listener).restarted(any(), any(), any(), any());
    }

    @Test
    void handleAutomationCompositionMigrationTest() {
        var listener = mock(ThreadHandler.class);
        var cacheProvider = mock(CacheProvider.class);
        var ach = new AutomationCompositionHandler(cacheProvider, mock(ParticipantMessagePublisher.class), listener);
        var migrationMsg = new AutomationCompositionMigration();
        assertDoesNotThrow(() -> ach.handleAutomationCompositionMigration(migrationMsg));
        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        migrationMsg.setCompositionTargetId(UUID.randomUUID());
        migrationMsg.setAutomationCompositionId(automationComposition.getInstanceId());
        assertDoesNotThrow(() -> ach.handleAutomationCompositionMigration(migrationMsg));
        when(cacheProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        var participantDeploy = new ParticipantDeploy();
        participantDeploy.setParticipantId(CommonTestData.getParticipantId());
        when(cacheProvider.getParticipantId()).thenReturn(CommonTestData.getParticipantId());
        migrationMsg.getParticipantUpdatesList().add(participantDeploy);
        Map<ToscaConceptIdentifier, AutomationCompositionElementDefinition> map = new HashMap<>();
        for (var element : automationComposition.getElements().values()) {
            var acElementDeploy = new AcElementDeploy();
            acElementDeploy.setProperties(Map.of());
            acElementDeploy.setId(element.getId());
            acElementDeploy.setDefinition(element.getDefinition());
            participantDeploy.getAcElementList().add(acElementDeploy);
            map.put(element.getDefinition(), new AutomationCompositionElementDefinition());
        }
        when(cacheProvider.getAcElementsDefinitions())
            .thenReturn(Map.of(automationComposition.getCompositionId(), map,
                migrationMsg.getCompositionTargetId(), map));

        ach.handleAutomationCompositionMigration(migrationMsg);
        verify(listener, times(automationComposition.getElements().size())).migrate(any(), any(), any(), any(), any());
    }
}
