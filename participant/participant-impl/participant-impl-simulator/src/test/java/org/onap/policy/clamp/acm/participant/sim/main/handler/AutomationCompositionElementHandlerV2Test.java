/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation.
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
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.InstanceElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.acm.participant.sim.model.SimConfig;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class AutomationCompositionElementHandlerV2Test {

    @Test
    void testDeploy() throws PfModelException {
        var config = new SimConfig();
        config.setDeployTimerMs(1);
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandlerV2(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        var compositionElement = new CompositionElementDto(UUID.randomUUID(), new ToscaConceptIdentifier(),
                Map.of(), Map.of());
        var instanceId = UUID.randomUUID();
        var elementId = UUID.randomUUID();
        var instanceElement = new InstanceElementDto(instanceId, elementId, null, Map.of(), Map.of());
        acElementHandler.deploy(compositionElement, instanceElement);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceId, elementId, DeployState.DEPLOYED,
                null, StateChangeResult.NO_ERROR, "Deployed");

        config.setDeploySuccess(false);
        acElementHandler.deploy(compositionElement, instanceElement);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceId, elementId, DeployState.UNDEPLOYED,
                null, StateChangeResult.FAILED, "Deploy failed!");
    }

    @Test
    void testUndeploy() throws PfModelException {
        var config = new SimConfig();
        config.setUndeployTimerMs(1);
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandlerV2(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        var compositionElement = new CompositionElementDto(UUID.randomUUID(), new ToscaConceptIdentifier(),
                Map.of(), Map.of());
        var instanceId = UUID.randomUUID();
        var elementId = UUID.randomUUID();
        var instanceElement = new InstanceElementDto(instanceId, elementId, null, Map.of(), Map.of());
        acElementHandler.undeploy(compositionElement, instanceElement);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceId, elementId, DeployState.UNDEPLOYED,
                null, StateChangeResult.NO_ERROR, "Undeployed");

        config.setUndeploySuccess(false);
        acElementHandler.undeploy(compositionElement, instanceElement);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceId, elementId, DeployState.DEPLOYED,
                null, StateChangeResult.FAILED, "Undeploy failed!");
    }

    @Test
    void testLock() throws PfModelException {
        var config = new SimConfig();
        config.setLockTimerMs(1);
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandlerV2(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        var compositionElement = new CompositionElementDto(UUID.randomUUID(), new ToscaConceptIdentifier(),
                Map.of(), Map.of());
        var instanceId = UUID.randomUUID();
        var elementId = UUID.randomUUID();
        var instanceElement = new InstanceElementDto(instanceId, elementId, null, Map.of(), Map.of());
        acElementHandler.lock(compositionElement, instanceElement);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceId, elementId, null, LockState.LOCKED,
                StateChangeResult.NO_ERROR, "Locked");

        config.setLockSuccess(false);
        acElementHandler.lock(compositionElement, instanceElement);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceId, elementId, null, LockState.UNLOCKED,
                StateChangeResult.FAILED, "Lock failed!");
    }

    @Test
    void testUnlock() throws PfModelException {
        var config = new SimConfig();
        config.setUnlockTimerMs(1);
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandlerV2(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        var compositionElement = new CompositionElementDto(UUID.randomUUID(), new ToscaConceptIdentifier(),
                Map.of(), Map.of());
        var instanceId = UUID.randomUUID();
        var elementId = UUID.randomUUID();
        var instanceElement = new InstanceElementDto(instanceId, elementId, null, Map.of(), Map.of());
        acElementHandler.unlock(compositionElement, instanceElement);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceId, elementId, null, LockState.UNLOCKED,
                StateChangeResult.NO_ERROR, "Unlocked");

        config.setUnlockSuccess(false);
        acElementHandler.unlock(compositionElement, instanceElement);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceId, elementId, null, LockState.LOCKED,
                StateChangeResult.FAILED, "Unlock failed!");
    }

    @Test
    void testUpdate() throws PfModelException {
        var config = new SimConfig();
        config.setUpdateTimerMs(1);
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandlerV2(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        var compositionElement = new CompositionElementDto(UUID.randomUUID(), new ToscaConceptIdentifier(),
                Map.of(), Map.of());
        var instanceId = UUID.randomUUID();
        var element = new AcElementDeploy();
        element.setId(UUID.randomUUID());
        var instanceElement = new InstanceElementDto(instanceId, element.getId(), null, Map.of(), Map.of());
        var instanceElementUpdated = new InstanceElementDto(instanceId, element.getId(), null,
                Map.of("key", "value"), Map.of());
        acElementHandler.update(compositionElement, instanceElement, instanceElementUpdated);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceId, element.getId(),
                DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Updated");

        config.setUpdateSuccess(false);
        acElementHandler.update(compositionElement, instanceElement, instanceElementUpdated);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceId, element.getId(),
                DeployState.DEPLOYED, null, StateChangeResult.FAILED, "Update failed!");
    }

    @Test
    void testDelete() throws PfModelException {
        var config = new SimConfig();
        config.setDeleteTimerMs(1);
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandlerV2(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        var compositionElement = new CompositionElementDto(UUID.randomUUID(), new ToscaConceptIdentifier(),
                Map.of(), Map.of());
        var instanceId = UUID.randomUUID();
        var elementId = UUID.randomUUID();
        var instanceElement = new InstanceElementDto(instanceId, elementId, null, Map.of(), Map.of());
        acElementHandler.delete(compositionElement, instanceElement);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceId, elementId, DeployState.DELETED,
                null, StateChangeResult.NO_ERROR, "Deleted");

        config.setDeleteSuccess(false);
        acElementHandler.delete(compositionElement, instanceElement);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceId, elementId, DeployState.UNDEPLOYED,
                null, StateChangeResult.FAILED, "Delete failed!");
    }

    @Test
    void testPrime() throws PfModelException {
        var config = new SimConfig();
        config.setPrimeTimerMs(1);
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandlerV2(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        var compositionId = UUID.randomUUID();
        var composition = new CompositionDto(compositionId, Map.of(), Map.of());
        acElementHandler.prime(composition);
        verify(intermediaryApi).updateCompositionState(compositionId, AcTypeState.PRIMED, StateChangeResult.NO_ERROR,
                "Primed");

        config.setPrimeSuccess(false);
        acElementHandler.prime(composition);
        verify(intermediaryApi).updateCompositionState(compositionId, AcTypeState.COMMISSIONED,
                StateChangeResult.FAILED, "Prime failed!");
    }

    @Test
    void testDeprime() throws PfModelException {
        var config = new SimConfig();
        config.setDeprimeTimerMs(1);
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandlerV2(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        var compositionId = UUID.randomUUID();
        var composition = new CompositionDto(compositionId, Map.of(), Map.of());
        acElementHandler.deprime(composition);
        verify(intermediaryApi).updateCompositionState(compositionId, AcTypeState.COMMISSIONED,
                StateChangeResult.NO_ERROR, "Deprimed");

        config.setDeprimeSuccess(false);
        acElementHandler.deprime(composition);
        verify(intermediaryApi).updateCompositionState(compositionId, AcTypeState.PRIMED, StateChangeResult.FAILED,
                "Deprime failed!");
    }

    @Test
    void testMigrate() throws PfModelException {
        var config = new SimConfig();
        config.setUpdateTimerMs(1);
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandlerV2(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        var compositionElement = new CompositionElementDto(UUID.randomUUID(), new ToscaConceptIdentifier(),
                Map.of(), Map.of());
        var compositionElementTraget = new CompositionElementDto(UUID.randomUUID(), new ToscaConceptIdentifier(),
                Map.of(), Map.of());
        var instanceId = UUID.randomUUID();
        var element = new AcElementDeploy();
        element.setId(UUID.randomUUID());
        var instanceElement = new InstanceElementDto(instanceId, element.getId(), null, Map.of(), Map.of());
        var instanceElementMigrated = new InstanceElementDto(instanceId, element.getId(),
                null, Map.of("key", "value"), Map.of());
        acElementHandler
                .migrate(compositionElement, compositionElementTraget, instanceElement, instanceElementMigrated);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceId, element.getId(),
                DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Migrated");

        config.setMigrateSuccess(false);
        acElementHandler
                .migrate(compositionElement, compositionElementTraget, instanceElement, instanceElementMigrated);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceId, element.getId(),
                DeployState.DEPLOYED, null, StateChangeResult.FAILED, "Migrate failed!");
    }
}
