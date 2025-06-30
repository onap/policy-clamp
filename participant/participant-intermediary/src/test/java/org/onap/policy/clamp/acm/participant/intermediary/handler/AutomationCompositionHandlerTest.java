/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.acm.participant.intermediary.main.parameters.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDeploy;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeploy;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeployAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionMigration;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionStateChange;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.PropertiesUpdate;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.LockOrder;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

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

        clearInvocations(listener);
        automationCompositionStateChange.setStartPhase(2);
        ach.handleAutomationCompositionStateChange(automationCompositionStateChange);
        verify(listener, times(0)).undeploy(any(), any(), any());
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

        clearInvocations(listener);
        automationCompositionStateChange.setStartPhase(2);
        ach.handleAutomationCompositionStateChange(automationCompositionStateChange);
        verify(listener, times(0)).delete(any(), any(), any());
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

        clearInvocations(listener);
        deployMsg.setStartPhase(2);
        deployMsg.setFirstStartPhase(false);
        ach.handleAutomationCompositionDeploy(deployMsg);
        verify(listener, times(0)).deploy(any(), any(), any());
    }

    @Test
    void handleMigrationNullTest() {
        var ach = new AutomationCompositionHandler(
                mock(CacheProvider.class), mock(ParticipantMessagePublisher.class), mock(ThreadHandler.class));
        var migrationMsg = new AutomationCompositionMigration();
        var rollbackMsg = new AutomationCompositionMigration();
        rollbackMsg.setRollback(true);

        migrationMsg.setStage(0);
        assertDoesNotThrow(() -> ach.handleAutomationCompositionMigration(migrationMsg));
        migrationMsg.setAutomationCompositionId(UUID.randomUUID());
        migrationMsg.setCompositionTargetId(UUID.randomUUID());
        assertDoesNotThrow(() -> ach.handleAutomationCompositionMigration(migrationMsg));

        rollbackMsg.setStage(0);
        assertDoesNotThrow(() -> ach.handleAutomationCompositionMigration(rollbackMsg));
        rollbackMsg.setAutomationCompositionId(UUID.randomUUID());
        rollbackMsg.setCompositionTargetId(UUID.randomUUID());
        assertDoesNotThrow(() -> ach.handleAutomationCompositionMigration(rollbackMsg));
    }

    @Test
    void handleAutomationCompositionMigrationTest() {
        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        automationComposition.setCompositionId(UUID.randomUUID());
        automationComposition.setInstanceId(UUID.randomUUID());
        automationComposition.setCompositionTargetId(UUID.randomUUID());
        var definitions =
                CommonTestData.createAutomationCompositionElementDefinitionList(automationComposition);
        var participantDeploy =
                CommonTestData.createparticipantDeploy(CommonTestData.getParticipantId(), automationComposition);

        var cacheProvider = createCacheProvider(participantDeploy, automationComposition.getCompositionId(),
                automationComposition.getInstanceId(), definitions,
                automationComposition.getCompositionTargetId(), definitions);

        var cacheProviderRollback = createCacheProvider(participantDeploy, automationComposition.getCompositionId(),
                automationComposition.getInstanceId(), definitions,
                automationComposition.getCompositionTargetId(), definitions);

        testMigration(cacheProvider, automationComposition, 0,
                automationComposition.getElements().size(), false);
        testMigration(cacheProviderRollback, automationComposition, 0,
                automationComposition.getElements().size(), true);
    }

    @Test
    void handleMigrationAddRemoveTest() {
        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        automationComposition.setCompositionId(UUID.randomUUID());
        automationComposition.setInstanceId(UUID.randomUUID());

        var acMigrate = new AutomationComposition(automationComposition);
        acMigrate.setCompositionTargetId(UUID.randomUUID());

        // replacing first element with new one
        var element = acMigrate.getElements().values().iterator().next();
        element.setDefinition(new ToscaConceptIdentifier("policy.clamp.new.element", "1.0.0"));
        element.setId(UUID.randomUUID());

        var migrateDefinitions =
                CommonTestData.createAutomationCompositionElementDefinitionList(acMigrate);

        var participantDeploy =
                CommonTestData.createparticipantDeploy(CommonTestData.getParticipantId(), automationComposition);
        var definitions =
                CommonTestData.createAutomationCompositionElementDefinitionList(automationComposition);
        var cacheProvider = createCacheProvider(participantDeploy, automationComposition.getCompositionId(),
                automationComposition.getInstanceId(), definitions,
                acMigrate.getCompositionTargetId(), migrateDefinitions);
        var cacheProviderRollback = createCacheProvider(participantDeploy, automationComposition.getCompositionId(),
                automationComposition.getInstanceId(), definitions,
                acMigrate.getCompositionTargetId(), migrateDefinitions);

        testMigration(cacheProvider, acMigrate, 0, acMigrate.getElements().size() + 1, false);
        testMigration(cacheProviderRollback, acMigrate, 0, acMigrate.getElements().size() + 1, true);
    }

    @Test
    void handleAcMigrationStageTest() {
        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        automationComposition.setCompositionId(UUID.randomUUID());
        automationComposition.setInstanceId(UUID.randomUUID());

        var acMigrate = new AutomationComposition(automationComposition);
        acMigrate.setCompositionTargetId(UUID.randomUUID());

        // replacing first element with new one
        var element = acMigrate.getElements().values().iterator().next();
        element.setDefinition(new ToscaConceptIdentifier("policy.clamp.new.element", "1.2.4"));
        element.setId(UUID.randomUUID());

        // replacing definition version
        acMigrate.getElements().values().forEach(el -> el.setDefinition(
                new ToscaConceptIdentifier(el.getDefinition().getName(), "1.2.4")));

        var migrateDefinitions =
                CommonTestData.createAutomationCompositionElementDefinitionList(acMigrate);

        migrateDefinitions.forEach(el -> el.getAutomationCompositionElementToscaNodeTemplate()
                .setProperties(Map.of("stage", List.of(0, 1))));

        var participantDeploy =
                CommonTestData.createparticipantDeploy(CommonTestData.getParticipantId(), automationComposition);
        var definitions =
                CommonTestData.createAutomationCompositionElementDefinitionList(automationComposition);
        var cacheProvider = createCacheProvider(participantDeploy, automationComposition.getCompositionId(),
                automationComposition.getInstanceId(), definitions,
                acMigrate.getCompositionTargetId(), migrateDefinitions);
        var cacheProviderRollback = createCacheProvider(participantDeploy, automationComposition.getCompositionId(),
                automationComposition.getInstanceId(), definitions,
                acMigrate.getCompositionTargetId(), migrateDefinitions);


        // scenario 1,2
        migrateDefinitions.forEach(el -> el.getAutomationCompositionElementToscaNodeTemplate()
                .setProperties(Map.of("stage", List.of(1, 2))));

        // expected the element deleted
        testMigration(cacheProvider, acMigrate, 0, 1, false);
        testMigration(cacheProviderRollback, acMigrate, 0, 1, true);

        // expected 4 elements from stage 1
        testMigration(cacheProvider, acMigrate, 1, 4, false);
        testMigration(cacheProviderRollback, acMigrate, 1, 4, true);

        // scenario 0,2
        cacheProvider = createCacheProvider(participantDeploy, automationComposition.getCompositionId(),
                automationComposition.getInstanceId(), definitions,
                acMigrate.getCompositionTargetId(), migrateDefinitions);
        cacheProviderRollback = createCacheProvider(participantDeploy, automationComposition.getCompositionId(),
                automationComposition.getInstanceId(), definitions,
                acMigrate.getCompositionTargetId(), migrateDefinitions);

        migrateDefinitions.forEach(el -> el.getAutomationCompositionElementToscaNodeTemplate()
                .setProperties(Map.of("stage", List.of(0, 2))));

        // expected the element deleted + 4 elements from stage 0
        testMigration(cacheProvider, acMigrate, 0, 5, false);
        testMigration(cacheProviderRollback, acMigrate, 0, 5, true);

        // expected 0 elements
        testMigration(cacheProvider, acMigrate, 1, 0, false);
        testMigration(cacheProviderRollback, acMigrate, 1, 0, true);
    }

    private CacheProvider createCacheProvider(ParticipantDeploy participantDeploy,
            UUID compositionId, UUID instanceId, List<AutomationCompositionElementDefinition> definitions,
            UUID compositionTargetId, List<AutomationCompositionElementDefinition> migrateDefinitions) {
        var cacheProvider = new CacheProvider(CommonTestData.getParticipantParameters());
        cacheProvider.addElementDefinition(compositionId, definitions);
        cacheProvider.initializeAutomationComposition(compositionId, instanceId, participantDeploy);
        cacheProvider.addElementDefinition(compositionTargetId, migrateDefinitions);
        return cacheProvider;
    }

    private void testMigration(CacheProvider cacheProvider, AutomationComposition acMigrate,
            int stage, int expectedMigrated, boolean rollback) {
        var migrationMsg = new AutomationCompositionMigration();
        migrationMsg.setStage(stage);
        migrationMsg.setCompositionId(acMigrate.getCompositionId());
        migrationMsg.setAutomationCompositionId(acMigrate.getInstanceId());
        migrationMsg.setCompositionTargetId(acMigrate.getCompositionTargetId());
        var participantMigrate = CommonTestData.createparticipantDeploy(cacheProvider.getParticipantId(), acMigrate);
        migrationMsg.setParticipantUpdatesList(List.of(participantMigrate));
        var listener = mock(ThreadHandler.class);

        clearInvocations();
        var ach = new AutomationCompositionHandler(cacheProvider,
                mock(ParticipantMessagePublisher.class), listener);

        clearInvocations();
        migrationMsg.setRollback(rollback);
        ach.handleAutomationCompositionMigration(migrationMsg);

        if (!rollback) {
            verify(listener, times(expectedMigrated)).migrate(any(), any(), any(), any(), any(), anyInt());
        } else {
            verify(listener, times(expectedMigrated)).rollback(any(), any(), any(), any(), any(), anyInt());
        }
    }
}
