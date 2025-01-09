/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2025 Nordix Foundation.
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
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.acm.participant.sim.comm.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;

class AutomationCompositionElementHandlerV1Test {

    private static final UUID COMPOSITION_ID = UUID.randomUUID();
    private static final UUID INSTANCE_ID = UUID.randomUUID();
    private static final UUID ELEMENT_ID = UUID.randomUUID();

    private AcElementDeploy createAcElementDeploy() {
        var element = new AcElementDeploy();
        element.setId(ELEMENT_ID);
        return element;
    }

    @Test
    void testDeploy() {
        var config = CommonTestData.createSimConfig();
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandlerV1(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        var element = createAcElementDeploy();
        acElementHandler.deploy(INSTANCE_ID, element, Map.of());
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, element.getId(),
                DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Deployed");

        config.setDeploySuccess(false);
        acElementHandler.deploy(INSTANCE_ID, element, Map.of());
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, element.getId(),
                DeployState.UNDEPLOYED, null, StateChangeResult.FAILED, "Deploy failed!");
    }

    @Test
    void testUndeploy() {
        var config = CommonTestData.createSimConfig();
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandlerV1(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        acElementHandler.undeploy(INSTANCE_ID, ELEMENT_ID);
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, ELEMENT_ID, DeployState.UNDEPLOYED,
                null, StateChangeResult.NO_ERROR, "Undeployed");

        config.setUndeploySuccess(false);
        acElementHandler.undeploy(INSTANCE_ID, ELEMENT_ID);
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, ELEMENT_ID, DeployState.DEPLOYED,
                null, StateChangeResult.FAILED, "Undeploy failed!");
    }

    @Test
    void testLock() {
        var config = CommonTestData.createSimConfig();
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandlerV1(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        acElementHandler.lock(INSTANCE_ID, ELEMENT_ID);
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, ELEMENT_ID,
                null, LockState.LOCKED, StateChangeResult.NO_ERROR, "Locked");

        config.setLockSuccess(false);
        acElementHandler.lock(INSTANCE_ID, ELEMENT_ID);
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, ELEMENT_ID,
                null, LockState.UNLOCKED, StateChangeResult.FAILED, "Lock failed!");
    }

    @Test
    void testUnlock() {
        var config = CommonTestData.createSimConfig();
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandlerV1(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        acElementHandler.unlock(INSTANCE_ID, ELEMENT_ID);
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, ELEMENT_ID,
                null, LockState.UNLOCKED, StateChangeResult.NO_ERROR, "Unlocked");

        config.setUnlockSuccess(false);
        acElementHandler.unlock(INSTANCE_ID, ELEMENT_ID);
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, ELEMENT_ID,
                null, LockState.LOCKED, StateChangeResult.FAILED, "Unlock failed!");
    }

    @Test
    void testUpdate() {
        var config = CommonTestData.createSimConfig();
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandlerV1(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        var element = createAcElementDeploy();
        acElementHandler.update(INSTANCE_ID, element, Map.of());
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, element.getId(),
                DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Updated");

        config.setUpdateSuccess(false);
        acElementHandler.update(INSTANCE_ID, element, Map.of());
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, element.getId(),
                DeployState.DEPLOYED, null, StateChangeResult.FAILED, "Update failed!");
    }

    @Test
    void testDelete() {
        var config = CommonTestData.createSimConfig();
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandlerV1(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        acElementHandler.delete(INSTANCE_ID, ELEMENT_ID);
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, ELEMENT_ID, DeployState.DELETED,
                null, StateChangeResult.NO_ERROR, "Deleted");

        config.setDeleteSuccess(false);
        acElementHandler.delete(INSTANCE_ID, ELEMENT_ID);
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, ELEMENT_ID, DeployState.UNDEPLOYED,
                null, StateChangeResult.FAILED, "Delete failed!");
    }

    @Test
    void testPrime() {
        var config = CommonTestData.createSimConfig();
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandlerV1(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        acElementHandler.prime(COMPOSITION_ID, List.of(createAutomationCompositionElementDefinition()));
        verify(intermediaryApi).updateCompositionState(COMPOSITION_ID, AcTypeState.PRIMED, StateChangeResult.NO_ERROR,
                "Primed");

        config.setPrimeSuccess(false);
        acElementHandler.prime(COMPOSITION_ID, List.of(createAutomationCompositionElementDefinition()));
        verify(intermediaryApi).updateCompositionState(COMPOSITION_ID, AcTypeState.COMMISSIONED,
                StateChangeResult.FAILED, "Prime failed!");
    }

    @Test
    void testDeprime() {
        var config = CommonTestData.createSimConfig();
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var acElementDefinition = createAutomationCompositionElementDefinition();
        when(intermediaryApi.getAcElementsDefinitions(COMPOSITION_ID))
                .thenReturn(Map.of(acElementDefinition.getAcElementDefinitionId(), acElementDefinition));
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandlerV1(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        acElementHandler.deprime(COMPOSITION_ID);
        verify(intermediaryApi).updateCompositionState(COMPOSITION_ID, AcTypeState.COMMISSIONED,
                StateChangeResult.NO_ERROR, "Deprimed");

        config.setDeprimeSuccess(false);
        acElementHandler.deprime(COMPOSITION_ID);
        verify(intermediaryApi).updateCompositionState(COMPOSITION_ID, AcTypeState.PRIMED, StateChangeResult.FAILED,
                "Deprime failed!");
    }

    private AutomationCompositionElementDefinition createAutomationCompositionElementDefinition() {
        var acElementDefinition = new AutomationCompositionElementDefinition();
        acElementDefinition.setAutomationCompositionElementToscaNodeTemplate(new ToscaNodeTemplate());
        acElementDefinition.getAutomationCompositionElementToscaNodeTemplate().setProperties(Map.of());
        acElementDefinition.setAcElementDefinitionId(new ToscaConceptIdentifier("name", "1.0.0"));
        acElementDefinition.setOutProperties(new HashMap<>());
        return acElementDefinition;
    }

    @Test
    void testMigrate() {
        var config = CommonTestData.createSimConfig();
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var simulatorService = new SimulatorService(intermediaryApi);
        var acElementHandler = new AutomationCompositionElementHandlerV1(intermediaryApi, simulatorService);
        simulatorService.setConfig(config);
        var element = createAcElementDeploy();
        acElementHandler.migrate(INSTANCE_ID, element, COMPOSITION_ID, Map.of());
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, element.getId(),
                DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Migrated");

        config.setMigrateSuccess(false);
        acElementHandler.migrate(INSTANCE_ID, element, COMPOSITION_ID, Map.of());
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, element.getId(),
                DeployState.DEPLOYED, null, StateChangeResult.FAILED, "Migrate failed!");
    }
}
