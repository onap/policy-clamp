/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.participant.sim.main.handler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.ElementStageDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.ElementState;
import org.onap.policy.clamp.acm.participant.intermediary.api.ElementStateDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.InstanceElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.acm.participant.intermediary.api.impl.ParticipantIntermediaryApiImpl;
import org.onap.policy.clamp.acm.participant.sim.comm.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class AutomationCompositionElementHandlerTest {

    private static final ToscaConceptIdentifier ELEMENT_DEFINITION_ID = new ToscaConceptIdentifier("name", "1.0.0");
    private static final CompositionElementDto COMPOSITION_ELEMENT =
        new CompositionElementDto(UUID.randomUUID(), ELEMENT_DEFINITION_ID, Map.of(), Map.of());
    private static final InstanceElementDto INSTANCE_ELEMENT =
        new InstanceElementDto(UUID.randomUUID(), UUID.randomUUID(), Map.of(), new HashMap<>());
    private static final CompositionDto COMPOSITION = new CompositionDto(UUID.randomUUID(),
        Map.of(ELEMENT_DEFINITION_ID, Map.of()), Map.of(ELEMENT_DEFINITION_ID, new HashMap<>()));

    @Test
    void testDeploy() {
        var config = CommonTestData.createSimConfig();
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandler(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        acElementHandler.deploy(COMPOSITION_ELEMENT, INSTANCE_ELEMENT);
        verify(intermediaryApi).updateAutomationCompositionElementState(
            new ElementStateDto(INSTANCE_ELEMENT.instanceId(), INSTANCE_ELEMENT.elementId(), DeployState.DEPLOYED,
                StateChangeResult.NO_ERROR, "Deployed",
                null, null, INSTANCE_ELEMENT.outProperties()));

        config.setDeploySuccess(false);
        acElementHandler.deploy(COMPOSITION_ELEMENT, INSTANCE_ELEMENT);
        verify(intermediaryApi).updateAutomationCompositionElementState(
            new ElementStateDto(INSTANCE_ELEMENT.instanceId(), INSTANCE_ELEMENT.elementId(), DeployState.UNDEPLOYED,
                StateChangeResult.FAILED, "Deploy failed!",
                null, null, INSTANCE_ELEMENT.outProperties()));
    }

    @Test
    void testUndeploy() {
        var config = CommonTestData.createSimConfig();
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandler(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        acElementHandler.undeploy(COMPOSITION_ELEMENT, INSTANCE_ELEMENT);
        verify(intermediaryApi).updateAutomationCompositionElementState(
            new ElementStateDto(INSTANCE_ELEMENT.instanceId(), INSTANCE_ELEMENT.elementId(), DeployState.UNDEPLOYED,
                StateChangeResult.NO_ERROR, "Undeployed",
                null, null, INSTANCE_ELEMENT.outProperties()));

        config.setUndeploySuccess(false);
        acElementHandler.undeploy(COMPOSITION_ELEMENT, INSTANCE_ELEMENT);
        verify(intermediaryApi).updateAutomationCompositionElementState(
            new ElementStateDto(INSTANCE_ELEMENT.instanceId(), INSTANCE_ELEMENT.elementId(), DeployState.DEPLOYED,
                StateChangeResult.FAILED, "Undeploy failed!",
                null, null, INSTANCE_ELEMENT.outProperties()));
    }

    @Test
    void testLock() {
        var config = CommonTestData.createSimConfig();
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandler(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        acElementHandler.lock(COMPOSITION_ELEMENT, INSTANCE_ELEMENT);
        verify(intermediaryApi).updateAutomationCompositionElementState(new ElementStateDto(
            INSTANCE_ELEMENT.instanceId(), INSTANCE_ELEMENT.elementId(), null, LockState.LOCKED,
            StateChangeResult.NO_ERROR, "Locked", null, null, INSTANCE_ELEMENT.outProperties()));

        config.setLockSuccess(false);
        acElementHandler.lock(COMPOSITION_ELEMENT, INSTANCE_ELEMENT);
        verify(intermediaryApi).updateAutomationCompositionElementState(new ElementStateDto(
            INSTANCE_ELEMENT.instanceId(), INSTANCE_ELEMENT.elementId(), null, LockState.UNLOCKED,
            StateChangeResult.FAILED, "Lock failed!", null, null, INSTANCE_ELEMENT.outProperties()));
    }

    @Test
    void testUnlock() {
        var config = CommonTestData.createSimConfig();
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandler(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        acElementHandler.unlock(COMPOSITION_ELEMENT, INSTANCE_ELEMENT);
        verify(intermediaryApi).updateAutomationCompositionElementState(new ElementStateDto(
            INSTANCE_ELEMENT.instanceId(), INSTANCE_ELEMENT.elementId(), null, LockState.UNLOCKED,
            StateChangeResult.NO_ERROR, "Unlocked", null, null, INSTANCE_ELEMENT.outProperties()));

        config.setUnlockSuccess(false);
        acElementHandler.unlock(COMPOSITION_ELEMENT, INSTANCE_ELEMENT);
        verify(intermediaryApi).updateAutomationCompositionElementState(new ElementStateDto(
            INSTANCE_ELEMENT.instanceId(), INSTANCE_ELEMENT.elementId(), null, LockState.LOCKED,
            StateChangeResult.FAILED, "Unlock failed!", null, null, INSTANCE_ELEMENT.outProperties()));
    }

    @Test
    void testUpdate() {
        var config = CommonTestData.createSimConfig();
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandler(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        var instanceElementUpdated = new InstanceElementDto(
            INSTANCE_ELEMENT.instanceId(), INSTANCE_ELEMENT.elementId(),
            Map.of("key", "value"), Map.of("id", "value"));
        acElementHandler.update(COMPOSITION_ELEMENT, INSTANCE_ELEMENT, instanceElementUpdated);
        verify(intermediaryApi).updateAutomationCompositionElementState(new ElementStateDto(
            INSTANCE_ELEMENT.instanceId(), INSTANCE_ELEMENT.elementId(), DeployState.DEPLOYED,
            StateChangeResult.NO_ERROR, "Updated", null, null,
            instanceElementUpdated.outProperties()));

        config.setUpdateSuccess(false);
        acElementHandler.update(COMPOSITION_ELEMENT, INSTANCE_ELEMENT, instanceElementUpdated);
        verify(intermediaryApi).updateAutomationCompositionElementState(new ElementStateDto(
            INSTANCE_ELEMENT.instanceId(), INSTANCE_ELEMENT.elementId(), DeployState.DEPLOYED,
            StateChangeResult.FAILED, "Update failed!", null, null,
            instanceElementUpdated.outProperties()));
    }

    @Test
    void testDelete() {
        var config = CommonTestData.createSimConfig();
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandler(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        acElementHandler.delete(COMPOSITION_ELEMENT, INSTANCE_ELEMENT);
        verify(intermediaryApi).deleteAutomationCompositionElementState(
            INSTANCE_ELEMENT.instanceId(), INSTANCE_ELEMENT.elementId());

        config.setDeleteSuccess(false);
        acElementHandler.delete(COMPOSITION_ELEMENT, INSTANCE_ELEMENT);
        verify(intermediaryApi).updateAutomationCompositionElementState(new ElementStateDto(
            INSTANCE_ELEMENT.instanceId(), INSTANCE_ELEMENT.elementId(), DeployState.UNDEPLOYED,
            StateChangeResult.FAILED, "Delete failed!", null, null, INSTANCE_ELEMENT.outProperties()));
    }

    @Test
    void testPrime() {
        var config = CommonTestData.createSimConfig();
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandler(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        acElementHandler.prime(COMPOSITION);
        verify(intermediaryApi).updateCompositionState(COMPOSITION.compositionId(), AcTypeState.PRIMED,
                StateChangeResult.NO_ERROR, "Primed", COMPOSITION.outPropertiesMap());

        config.setPrimeSuccess(false);
        acElementHandler.prime(COMPOSITION);
        verify(intermediaryApi).updateCompositionState(COMPOSITION.compositionId(), AcTypeState.COMMISSIONED,
                StateChangeResult.FAILED, "Prime failed!", COMPOSITION.outPropertiesMap());
    }

    @Test
    void testDeprime() {
        var config = CommonTestData.createSimConfig();
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandler(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        acElementHandler.deprime(COMPOSITION);
        verify(intermediaryApi).updateCompositionState(COMPOSITION.compositionId(), AcTypeState.COMMISSIONED,
                StateChangeResult.NO_ERROR, "Deprimed", COMPOSITION.outPropertiesMap());

        config.setDeprimeSuccess(false);
        acElementHandler.deprime(COMPOSITION);
        verify(intermediaryApi).updateCompositionState(COMPOSITION.compositionId(), AcTypeState.PRIMED,
                StateChangeResult.FAILED, "Deprime failed!", COMPOSITION.outPropertiesMap());
    }

    @Test
    void testMigrate() {
        var config = CommonTestData.createSimConfig();
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandler(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        var compositionElementTarget = new CompositionElementDto(UUID.randomUUID(), new ToscaConceptIdentifier(),
            Map.of(), Map.of());
        var instanceElementMigrated = new InstanceElementDto(
            INSTANCE_ELEMENT.instanceId(), INSTANCE_ELEMENT.elementId(),
            Map.of("key", "value"), new HashMap<>());
        acElementHandler
            .migrate(COMPOSITION_ELEMENT, compositionElementTarget, INSTANCE_ELEMENT, instanceElementMigrated, 0);
        verify(intermediaryApi).updateAutomationCompositionElementState(new ElementStateDto(
            INSTANCE_ELEMENT.instanceId(), INSTANCE_ELEMENT.elementId(),
            DeployState.DEPLOYED, StateChangeResult.NO_ERROR, "Migrated",
            null, null, instanceElementMigrated.outProperties()));

        config.setMigrateSuccess(false);
        acElementHandler
            .migrate(COMPOSITION_ELEMENT, compositionElementTarget, INSTANCE_ELEMENT, instanceElementMigrated, 0);
        verify(intermediaryApi).updateAutomationCompositionElementState(new ElementStateDto(
            INSTANCE_ELEMENT.instanceId(), INSTANCE_ELEMENT.elementId(), DeployState.DEPLOYED,
            StateChangeResult.FAILED, "Migrate failed!",
            null, null, instanceElementMigrated.outProperties()));
    }

    @Test
    void testMigrateStage() {
        var config = CommonTestData.createSimConfig();
        var intermediaryApi = spy(new ParticipantIntermediaryApiImpl(mock(), mock()));
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandler(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        var compositionElementTarget = new CompositionElementDto(UUID.randomUUID(), new ToscaConceptIdentifier(),
            Map.of("stage", List.of(1, 2)), Map.of());
        var instanceElementMigrated = new InstanceElementDto(INSTANCE_ELEMENT.instanceId(),
            INSTANCE_ELEMENT.elementId(), Map.of(), new HashMap<>());
        acElementHandler
            .migrate(COMPOSITION_ELEMENT, compositionElementTarget, INSTANCE_ELEMENT, instanceElementMigrated, 1);
        verify(intermediaryApi).updateAutomationCompositionElementStage(new ElementStageDto(
            INSTANCE_ELEMENT.instanceId(), INSTANCE_ELEMENT.elementId(), "stage 1 Migrated", 2,
            null, null, instanceElementMigrated.outProperties()));
    }

    @Test
    void testMigrateAdd() {
        var config = CommonTestData.createSimConfig();
        var intermediaryApi = spy(new ParticipantIntermediaryApiImpl(mock(), mock()));
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandler(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        var compositionElement = new CompositionElementDto(
            UUID.randomUUID(), new ToscaConceptIdentifier(), Map.of(), Map.of(), ElementState.NOT_PRESENT);

        var instanceElement = new InstanceElementDto(
            UUID.randomUUID(), UUID.randomUUID(), Map.of(), Map.of(), ElementState.NOT_PRESENT);

        var compoElTargetAdd = new CompositionElementDto(
            UUID.randomUUID(), new ToscaConceptIdentifier(), Map.of(), Map.of(), ElementState.NEW);
        var inElMigratedAdd = new InstanceElementDto(instanceElement.instanceId(), instanceElement.elementId(),
            Map.of(), new HashMap<>(), ElementState.NEW);
        acElementHandler
            .migrate(compositionElement, compoElTargetAdd, instanceElement, inElMigratedAdd, 0);
        verify(intermediaryApi).updateAutomationCompositionElementState(new ElementStateDto(
            instanceElement.instanceId(), instanceElement.elementId(),
            DeployState.DEPLOYED, StateChangeResult.NO_ERROR, "Migrated",
            null, null, inElMigratedAdd.outProperties()));
    }

    @Test
    void testMigrateRemove() {
        var config = CommonTestData.createSimConfig();
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandler(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);

        var compoElTargetRemove = new CompositionElementDto(UUID.randomUUID(), new ToscaConceptIdentifier(),
            Map.of(), Map.of(), ElementState.REMOVED);
        var inElMigratedRemove = new InstanceElementDto(
            INSTANCE_ELEMENT.instanceId(), INSTANCE_ELEMENT.elementId(),
            Map.of("key", "value"), Map.of(), ElementState.REMOVED);
        acElementHandler
            .migrate(COMPOSITION_ELEMENT, compoElTargetRemove, INSTANCE_ELEMENT, inElMigratedRemove, 0);
        verify(intermediaryApi).deleteAutomationCompositionElementState(
            INSTANCE_ELEMENT.instanceId(), INSTANCE_ELEMENT.elementId());
    }

    @Test
    void testMigratePrecheck() {
        var config = CommonTestData.createSimConfig();
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandler(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        var compositionElementTarget = new CompositionElementDto(UUID.randomUUID(), new ToscaConceptIdentifier(),
            Map.of(), Map.of());
        var instanceElementMigrated = new InstanceElementDto(
            INSTANCE_ELEMENT.instanceId(), INSTANCE_ELEMENT.elementId(),
            Map.of("key", "value"), Map.of());
        acElementHandler.migratePrecheck(COMPOSITION_ELEMENT, compositionElementTarget,
            INSTANCE_ELEMENT, instanceElementMigrated);
        verify(intermediaryApi).updateAutomationCompositionElementState(new ElementStateDto(
            INSTANCE_ELEMENT.instanceId(), INSTANCE_ELEMENT.elementId(), DeployState.DEPLOYED,
            StateChangeResult.NO_ERROR, "Migration precheck completed",
            null, null, instanceElementMigrated.outProperties()));

        config.setMigratePrecheck(false);
        acElementHandler.migratePrecheck(COMPOSITION_ELEMENT, compositionElementTarget,
            INSTANCE_ELEMENT, instanceElementMigrated);
        verify(intermediaryApi).updateAutomationCompositionElementState(new ElementStateDto(
            INSTANCE_ELEMENT.instanceId(), INSTANCE_ELEMENT.elementId(), DeployState.DEPLOYED,
            StateChangeResult.FAILED, "Migration precheck failed",
            null, null, instanceElementMigrated.outProperties()));
    }

    @Test
    void testPrepare() {
        var config = CommonTestData.createSimConfig();
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandler(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        acElementHandler.prepare(COMPOSITION_ELEMENT, INSTANCE_ELEMENT, 0);
        verify(intermediaryApi).updateAutomationCompositionElementState(new ElementStateDto(
            INSTANCE_ELEMENT.instanceId(), INSTANCE_ELEMENT.elementId(), DeployState.UNDEPLOYED,
            StateChangeResult.NO_ERROR, "Prepare completed",
            null, null, INSTANCE_ELEMENT.outProperties()));

        config.setPrepare(false);
        acElementHandler.prepare(COMPOSITION_ELEMENT, INSTANCE_ELEMENT, 0);
        verify(intermediaryApi).updateAutomationCompositionElementState(new ElementStateDto(
            INSTANCE_ELEMENT.instanceId(), INSTANCE_ELEMENT.elementId(), DeployState.UNDEPLOYED,
            StateChangeResult.FAILED, "Prepare failed",
            null, null, INSTANCE_ELEMENT.outProperties()));
    }

    @Test
    void testPrepareStage() {
        var config = CommonTestData.createSimConfig();
        var intermediaryApi = spy(new ParticipantIntermediaryApiImpl(mock(), mock()));
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandler(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        var prepareMap = Map.of("prepare", List.of(1, 2));
        var compositionElement = new CompositionElementDto(UUID.randomUUID(), new ToscaConceptIdentifier(),
                Map.of("stage", prepareMap), Map.of());
        var instanceElement = new InstanceElementDto(INSTANCE_ELEMENT.instanceId(),
                INSTANCE_ELEMENT.elementId(), Map.of(), new HashMap<>());
        acElementHandler.prepare(compositionElement, instanceElement, 1);
        verify(intermediaryApi).updateAutomationCompositionElementStage(new ElementStageDto(
                instanceElement.instanceId(), instanceElement.elementId(), "stage 1 Prepared", 2,
                null, null, instanceElement.outProperties()));
    }

    @Test
    void testReview() {
        var config = CommonTestData.createSimConfig();
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandler(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        acElementHandler.review(COMPOSITION_ELEMENT, INSTANCE_ELEMENT);
        verify(intermediaryApi).updateAutomationCompositionElementState(new ElementStateDto(
            INSTANCE_ELEMENT.instanceId(), INSTANCE_ELEMENT.elementId(), DeployState.DEPLOYED,
            StateChangeResult.NO_ERROR, "Review completed", null, null, INSTANCE_ELEMENT.outProperties()));

        config.setReview(false);
        acElementHandler.review(COMPOSITION_ELEMENT, INSTANCE_ELEMENT);
        verify(intermediaryApi).updateAutomationCompositionElementState(new ElementStateDto(
            INSTANCE_ELEMENT.instanceId(), INSTANCE_ELEMENT.elementId(), DeployState.DEPLOYED,
            StateChangeResult.FAILED, "Review failed", null, null, INSTANCE_ELEMENT.outProperties()));
    }

    @Test
    void testRollback() {
        var config = CommonTestData.createSimConfig();
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandler(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        var compositionElementRollback = new CompositionElementDto(UUID.randomUUID(), new ToscaConceptIdentifier(),
                Map.of(), Map.of());
        var instanceElementRollback = new InstanceElementDto(
                INSTANCE_ELEMENT.instanceId(), INSTANCE_ELEMENT.elementId(),
                Map.of("key", "value"), new HashMap<>());
        acElementHandler.rollbackMigration(COMPOSITION_ELEMENT, compositionElementRollback, INSTANCE_ELEMENT,
                instanceElementRollback, 0);
        verify(intermediaryApi).updateAutomationCompositionElementState(new ElementStateDto(
                INSTANCE_ELEMENT.instanceId(), INSTANCE_ELEMENT.elementId(), DeployState.DEPLOYED,
                StateChangeResult.NO_ERROR, "Migration rollback done",
                null, null, instanceElementRollback.outProperties()));

        config.setRollback(false);
        acElementHandler.rollbackMigration(COMPOSITION_ELEMENT, compositionElementRollback, INSTANCE_ELEMENT,
                instanceElementRollback, 0);
        verify(intermediaryApi).updateAutomationCompositionElementState(new ElementStateDto(
            INSTANCE_ELEMENT.instanceId(), INSTANCE_ELEMENT.elementId(), DeployState.DEPLOYED,
            StateChangeResult.FAILED, "Migration rollback failed",
                null, null, instanceElementRollback.outProperties()));
    }

    @Test
    void testRollbackStage() {
        var config = CommonTestData.createSimConfig();
        var intermediaryApi = spy(new ParticipantIntermediaryApiImpl(mock(), mock()));
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandler(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        var compositionElementRollback = new CompositionElementDto(UUID.randomUUID(), new ToscaConceptIdentifier(),
                Map.of("stage", List.of(1, 2)), Map.of());
        var instanceElementRollback = new InstanceElementDto(INSTANCE_ELEMENT.instanceId(),
                INSTANCE_ELEMENT.elementId(), Map.of(), new HashMap<>());
        acElementHandler.rollbackMigration(COMPOSITION_ELEMENT, compositionElementRollback, INSTANCE_ELEMENT,
                instanceElementRollback, 2);
        verify(intermediaryApi).updateAutomationCompositionElementStage(new ElementStageDto(
                INSTANCE_ELEMENT.instanceId(), INSTANCE_ELEMENT.elementId(), "stage 2 Migration rollback", 1,
                null, null, instanceElementRollback.outProperties()));
    }

    @Test
    void testRollbackAdd() {
        var config = CommonTestData.createSimConfig();
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandler(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        var compositionElement = new CompositionElementDto(
                UUID.randomUUID(), new ToscaConceptIdentifier(), Map.of(), Map.of(), ElementState.NOT_PRESENT);

        var instanceElement = new InstanceElementDto(
                UUID.randomUUID(), UUID.randomUUID(), Map.of(), Map.of(), ElementState.NOT_PRESENT);

        var compoElRollbackAdd = new CompositionElementDto(
                UUID.randomUUID(), new ToscaConceptIdentifier(), Map.of(), Map.of(), ElementState.NEW);
        var inElRollbackAdd = new InstanceElementDto(instanceElement.instanceId(), instanceElement.elementId(),
                Map.of(), new HashMap<>(), ElementState.NEW);
        acElementHandler
                .rollbackMigration(compositionElement, compoElRollbackAdd, instanceElement, inElRollbackAdd, 0);
        verify(intermediaryApi).updateAutomationCompositionElementState(new ElementStateDto(
                instanceElement.instanceId(), instanceElement.elementId(),
                DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Migration rollback done",
                null, null, inElRollbackAdd.outProperties()));
    }

    @Test
    void testRollbackRemove() {
        var config = CommonTestData.createSimConfig();
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandler(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);

        var compoElRollbackRemove = new CompositionElementDto(UUID.randomUUID(), new ToscaConceptIdentifier(),
                Map.of(), Map.of(), ElementState.REMOVED);
        var inElRollbackRemove = new InstanceElementDto(
                INSTANCE_ELEMENT.instanceId(), INSTANCE_ELEMENT.elementId(),
                Map.of("key", "value"), Map.of(), ElementState.REMOVED);
        acElementHandler
                .rollbackMigration(COMPOSITION_ELEMENT, compoElRollbackRemove, INSTANCE_ELEMENT, inElRollbackRemove, 0);
        verify(intermediaryApi).deleteAutomationCompositionElementState(
                INSTANCE_ELEMENT.instanceId(), INSTANCE_ELEMENT.elementId());
    }
}
