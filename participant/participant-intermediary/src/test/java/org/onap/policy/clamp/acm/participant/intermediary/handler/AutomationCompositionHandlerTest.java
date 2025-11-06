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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.acm.participant.intermediary.handler.cache.AcDefinition;
import org.onap.policy.clamp.acm.participant.intermediary.handler.cache.CacheProvider;
import org.onap.policy.clamp.acm.participant.intermediary.main.parameters.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.MigrationState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDeploy;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeploy;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeployAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionMigration;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionStateChange;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.PropertiesUpdate;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.LockOrder;
import org.onap.policy.clamp.models.acm.utils.AcmStateUtils;
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
    void handleAcStateChangeUndeployTest() {
        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        automationComposition.setCompositionId(UUID.randomUUID());
        automationComposition.setInstanceId(UUID.randomUUID());
        automationComposition.setCompositionTargetId(UUID.randomUUID());
        var participantDeploy =
                CommonTestData.createparticipantDeploy(CommonTestData.getParticipantId(), automationComposition);

        var cacheProvider = new CacheProvider(CommonTestData.getParticipantParameters());
        cacheProvider.initializeAutomationComposition(automationComposition.getCompositionId(),
                automationComposition.getInstanceId(), participantDeploy, UUID.randomUUID());

        var automationCompositionStateChange = CommonTestData.getStateChange(CommonTestData.getParticipantId(),
                automationComposition.getInstanceId(), DeployOrder.UNDEPLOY, LockOrder.NONE);

        var participantMessagePublisher = mock(ParticipantMessagePublisher.class);
        var listener = mock(ThreadHandler.class);
        var ach = new AutomationCompositionHandler(cacheProvider, participantMessagePublisher, listener);
        ach.handleAutomationCompositionStateChange(automationCompositionStateChange);
        automationComposition = cacheProvider.getAutomationComposition(automationComposition.getInstanceId());
        verify(listener, times(automationComposition.getElements().size())).undeploy(any(), any(), any());
        for (var element : automationComposition.getElements().values()) {
            assertEquals(DeployState.UNDEPLOYING, element.getDeployState());
        }
    }

    @Test
    void handleAutomationCompositionStateChangeUndeployTest() {
        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        when(cacheProvider.getCommonProperties(any(UUID.class), any(UUID.class))).thenReturn(Map.of());

        var acDefinition = new AcDefinition();
        acDefinition.setCompositionId(automationComposition.getCompositionId());
        for (var element : automationComposition.getElements().values()) {
            acDefinition.getElements().put(element.getDefinition(), new AutomationCompositionElementDefinition());
        }
        when(cacheProvider.getAcElementsDefinitions())
            .thenReturn(Map.of(automationComposition.getCompositionId(), acDefinition));
        var automationCompositionStateChange = CommonTestData.getStateChange(CommonTestData.getParticipantId(),
            automationComposition.getInstanceId(), DeployOrder.UNDEPLOY, LockOrder.NONE);

        var participantMessagePublisher = mock(ParticipantMessagePublisher.class);
        var listener = mock(ThreadHandler.class);
        var ach = new AutomationCompositionHandler(cacheProvider, participantMessagePublisher, listener);
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

        var acDefinition = new AcDefinition();
        acDefinition.setCompositionId(automationComposition.getCompositionId());
        for (var element : automationComposition.getElements().values()) {
            acDefinition.getElements().put(element.getDefinition(), new AutomationCompositionElementDefinition());
        }
        when(cacheProvider.getAcElementsDefinitions())
            .thenReturn(Map.of(automationComposition.getCompositionId(), acDefinition));
        var automationCompositionStateChange = CommonTestData.getStateChange(CommonTestData.getParticipantId(),
            automationComposition.getInstanceId(), DeployOrder.DELETE, LockOrder.NONE);
        var participantMessagePublisher = mock(ParticipantMessagePublisher.class);
        var listener = mock(ThreadHandler.class);
        var ach = new AutomationCompositionHandler(cacheProvider, participantMessagePublisher, listener);
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

        var acDefinition = new AcDefinition();
        acDefinition.setCompositionId(automationComposition.getCompositionId());
        for (var element : automationComposition.getElements().values()) {
            acDefinition.getElements().put(element.getDefinition(), new AutomationCompositionElementDefinition());
        }
        when(cacheProvider.getAcElementsDefinitions())
            .thenReturn(Map.of(automationComposition.getCompositionId(), acDefinition));
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
        var acDefinition = new AcDefinition();
        acDefinition.setCompositionId(automationComposition.getCompositionId());
        for (var element : automationComposition.getElements().values()) {
            var acElementDeploy = new AcElementDeploy();
            acElementDeploy.setProperties(Map.of());
            acElementDeploy.setId(element.getId());
            participantDeploy.getAcElementList().add(acElementDeploy);
            acDefinition.getElements().put(element.getDefinition(), new AutomationCompositionElementDefinition());
        }
        when(cacheProvider.getAcElementsDefinitions())
            .thenReturn(Map.of(automationComposition.getCompositionId(), acDefinition));

        ach.handleAutomationCompositionDeploy(deployMsg);
        verify(listener, times(automationComposition.getElements().size())).deploy(any(), any(), any());

        clearInvocations(listener);
        deployMsg.setStartPhase(2);
        deployMsg.setFirstStartPhase(false);
        ach.handleAutomationCompositionDeploy(deployMsg);
        verify(listener, times(0)).deploy(any(), any(), any());
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
    void handleAcMigrationStageTest() {
        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        AcmStateUtils.setCascadedState(automationComposition, DeployState.DEPLOYED, LockState.LOCKED);
        automationComposition.setCompositionId(UUID.randomUUID());
        automationComposition.setInstanceId(UUID.randomUUID());

        var acMigrate = new AutomationComposition(automationComposition);
        AcmStateUtils.setCascadedState(acMigrate, DeployState.MIGRATING, LockState.LOCKED);
        acMigrate.setCompositionTargetId(UUID.randomUUID());

        // remove first element
        var elementRemoved = acMigrate.getElements().values().iterator().next();
        elementRemoved.setMigrationState(MigrationState.REMOVED);

        // add new element
        var element = new AutomationCompositionElement(elementRemoved);
        element.setDefinition(new ToscaConceptIdentifier("policy.clamp.new.element", "1.2.4"));
        element.setId(UUID.randomUUID());
        element.setMigrationState(MigrationState.NEW);
        acMigrate.getElements().put(element.getId(), element);

        // replacing definition version excluding the removed element
        acMigrate.getElements().values().stream()
                .filter(el -> !el.getId().equals(elementRemoved.getId()))
                .forEach(el -> el.setDefinition(
                        new ToscaConceptIdentifier(el.getDefinition().getName(), "1.2.4")));

        var migrateDefinitions =
                CommonTestData.createAutomationCompositionElementDefinitionList(acMigrate);

        // scenario 1,2
        migrateDefinitions.forEach(el -> el.getAutomationCompositionElementToscaNodeTemplate()
                .setProperties(Map.of("stage", List.of(1, 2))));

        var participantDeploy =
                CommonTestData.createparticipantDeploy(CommonTestData.getParticipantId(), automationComposition);
        var definitions =
                CommonTestData.createAutomationCompositionElementDefinitionList(automationComposition);
        var cacheProvider = createCacheProvider(participantDeploy, automationComposition.getCompositionId(),
                automationComposition.getInstanceId(), definitions,
                acMigrate.getCompositionTargetId(), migrateDefinitions);

        definitions.forEach(el -> el.getAutomationCompositionElementToscaNodeTemplate()
                .setProperties(Map.of("stage", List.of(1, 2))));
        automationComposition.getElements().put(element.getId(), element);

        // expected the element deleted
        testMigration(cacheProvider, acMigrate, 0, 1, false);

        // expected 4 elements from stage 1
        testMigration(cacheProvider, acMigrate, 1, 4, false);

        // scenario 0,2
        cacheProvider = createCacheProvider(participantDeploy, automationComposition.getCompositionId(),
                automationComposition.getInstanceId(), definitions,
                acMigrate.getCompositionTargetId(), migrateDefinitions);

        migrateDefinitions.forEach(el -> el.getAutomationCompositionElementToscaNodeTemplate()
                .setProperties(Map.of("stage", List.of(0, 2))));

        // expected the element deleted + 4 elements from stage 0
        testMigration(cacheProvider, acMigrate, 0, 5, false);

        // expected 0 elements
        testMigration(cacheProvider, acMigrate, 1, 0, false);
    }

    private CacheProvider createCacheProvider(ParticipantDeploy participantDeploy,
            UUID compositionId, UUID instanceId, List<AutomationCompositionElementDefinition> definitions,
            UUID compositionTargetId, List<AutomationCompositionElementDefinition> migrateDefinitions) {
        var cacheProvider = new CacheProvider(CommonTestData.getParticipantParameters());
        cacheProvider.addElementDefinition(compositionId, definitions, UUID.randomUUID());
        cacheProvider.initializeAutomationComposition(compositionId, instanceId, participantDeploy, UUID.randomUUID());
        cacheProvider.addElementDefinition(compositionTargetId, migrateDefinitions, UUID.randomUUID());
        return cacheProvider;
    }

    private void testMigration(CacheProvider cacheProvider, AutomationComposition acMigrate,
            int stage, int expectedMigrated, boolean rollback) {
        var migrationMsg = new AutomationCompositionMigration();
        migrationMsg.setStage(stage);
        migrationMsg.setFirstStage(rollback && stage == 2);
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

    @Test
    void handleAcRollbackStageTest() {
        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        AcmStateUtils.setCascadedState(automationComposition, DeployState.MIGRATING, LockState.LOCKED);
        automationComposition.setCompositionId(UUID.randomUUID());
        automationComposition.setInstanceId(UUID.randomUUID());
        automationComposition.setCompositionTargetId(UUID.randomUUID());

        var acRollback = new AutomationComposition(automationComposition);
        AcmStateUtils.setCascadedState(acRollback, DeployState.MIGRATION_REVERTING, LockState.LOCKED);

        var acRollbackDefinitions =
                CommonTestData.createAutomationCompositionElementDefinitionList(acRollback);
        acRollbackDefinitions.forEach(el -> el.getAutomationCompositionElementToscaNodeTemplate()
                .setProperties(Map.of("stage", List.of(1, 2))));

        // remove first element
        var elementRemoved = automationComposition.getElements().values().iterator().next();
        elementRemoved.setMigrationState(MigrationState.REMOVED);
        acRollback.getElements().get(elementRemoved.getId()).setMigrationState(MigrationState.REMOVED);

        // add new element
        var element = new AutomationCompositionElement(elementRemoved);
        element.setDefinition(new ToscaConceptIdentifier("policy.clamp.new.element", "1.2.4"));
        element.setId(UUID.randomUUID());
        element.setMigrationState(MigrationState.NEW);
        automationComposition.getElements().put(element.getId(), element);
        acRollback.getElements().put(element.getId(), element);

        // replacing definition version excluding the removed element
        automationComposition.getElements().values().stream()
                .filter(el -> !el.getId().equals(elementRemoved.getId()))
                .forEach(el -> el.setDefinition(
                        new ToscaConceptIdentifier(el.getDefinition().getName(), "1.2.4")));

        var acDefinitions =
                CommonTestData.createAutomationCompositionElementDefinitionList(automationComposition);
        acDefinitions.forEach(el -> el.getAutomationCompositionElementToscaNodeTemplate()
                .setProperties(Map.of("stage", List.of(1, 2))));

        var participantDeploy =
                CommonTestData.createparticipantDeploy(CommonTestData.getParticipantId(), automationComposition);
        var cacheProvider = new CacheProvider(CommonTestData.getParticipantParameters());
        cacheProvider.addElementDefinition(
                automationComposition.getCompositionTargetId(), acDefinitions, UUID.randomUUID());
        cacheProvider.initializeAutomationComposition(automationComposition.getCompositionId(),
                automationComposition.getInstanceId(), participantDeploy, UUID.randomUUID());
        cacheProvider.addElementDefinition(
                automationComposition.getCompositionId(), acRollbackDefinitions, UUID.randomUUID());

        // expected default elements
        testMigration(cacheProvider, acRollback, 1, 4, true);

        // expected default elements and new element deleted
        testMigration(cacheProvider, acRollback, 2, 5, true);
    }

}
