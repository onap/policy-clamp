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

package org.onap.policy.clamp.acm.participant.sim.main.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.acm.participant.sim.comm.CommonTestData;
import org.onap.policy.clamp.acm.participant.sim.model.SimConfig;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.models.base.PfModelException;

class AutomationCompositionElementHandlerTest {

    @Test
    void testDeploy() throws PfModelException {
        var config = new SimConfig();
        config.setDeployTimerMs(1);
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var acElementHandler = new AutomationCompositionElementHandler(intermediaryApi);
        acElementHandler.setConfig(config);
        var instanceId = UUID.randomUUID();
        var element = new AcElementDeploy();
        element.setId(UUID.randomUUID());
        acElementHandler.deploy(instanceId, element, Map.of());
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceId, element.getId(),
                DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Deployed");

        config.setDeploySuccess(false);
        acElementHandler.deploy(instanceId, element, Map.of());
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceId, element.getId(),
                DeployState.UNDEPLOYED, null, StateChangeResult.FAILED, "Deploy failed!");
    }

    @Test
    void testUndeploy() throws PfModelException {
        var config = new SimConfig();
        config.setUndeployTimerMs(1);
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var acElementHandler = new AutomationCompositionElementHandler(intermediaryApi);
        acElementHandler.setConfig(config);
        var instanceId = UUID.randomUUID();
        var elementId = UUID.randomUUID();
        acElementHandler.undeploy(instanceId, elementId);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceId, elementId, DeployState.UNDEPLOYED,
                null, StateChangeResult.NO_ERROR, "Undeployed");

        config.setUndeploySuccess(false);
        acElementHandler.undeploy(instanceId, elementId);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceId, elementId, DeployState.DEPLOYED,
                null, StateChangeResult.FAILED, "Undeploy failed!");
    }

    @Test
    void testLock() throws PfModelException {
        var config = new SimConfig();
        config.setLockTimerMs(1);
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var acElementHandler = new AutomationCompositionElementHandler(intermediaryApi);
        acElementHandler.setConfig(config);
        var instanceId = UUID.randomUUID();
        var elementId = UUID.randomUUID();
        acElementHandler.lock(instanceId, elementId);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceId, elementId, null, LockState.LOCKED,
                StateChangeResult.NO_ERROR, "Locked");

        config.setLockSuccess(false);
        acElementHandler.lock(instanceId, elementId);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceId, elementId, null, LockState.UNLOCKED,
                StateChangeResult.FAILED, "Lock failed!");
    }

    @Test
    void testUnlock() throws PfModelException {
        var config = new SimConfig();
        config.setUnlockTimerMs(1);
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var acElementHandler = new AutomationCompositionElementHandler(intermediaryApi);
        acElementHandler.setConfig(config);
        var instanceId = UUID.randomUUID();
        var elementId = UUID.randomUUID();
        acElementHandler.unlock(instanceId, elementId);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceId, elementId, null, LockState.UNLOCKED,
                StateChangeResult.NO_ERROR, "Unlocked");

        config.setUnlockSuccess(false);
        acElementHandler.unlock(instanceId, elementId);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceId, elementId, null, LockState.LOCKED,
                StateChangeResult.FAILED, "Unlock failed!");
    }

    @Test
    void testUpdate() throws PfModelException {
        var config = new SimConfig();
        config.setUpdateTimerMs(1);
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var acElementHandler = new AutomationCompositionElementHandler(intermediaryApi);
        acElementHandler.setConfig(config);
        var instanceId = UUID.randomUUID();
        var element = new AcElementDeploy();
        element.setId(UUID.randomUUID());
        acElementHandler.update(instanceId, element, Map.of());
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceId, element.getId(),
                DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Updated");

        config.setUpdateSuccess(false);
        acElementHandler.update(instanceId, element, Map.of());
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceId, element.getId(),
                DeployState.DEPLOYED, null, StateChangeResult.FAILED, "Update failed!");
    }

    @Test
    void testDelete() throws PfModelException {
        var config = new SimConfig();
        config.setDeleteTimerMs(1);
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var acElementHandler = new AutomationCompositionElementHandler(intermediaryApi);
        acElementHandler.setConfig(config);
        var instanceId = UUID.randomUUID();
        var elementId = UUID.randomUUID();
        acElementHandler.delete(instanceId, elementId);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceId, elementId, DeployState.DELETED,
                null, StateChangeResult.NO_ERROR, "Deleted");

        config.setDeleteSuccess(false);
        acElementHandler.delete(instanceId, elementId);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceId, elementId, DeployState.UNDEPLOYED,
                null, StateChangeResult.FAILED, "Delete failed!");
    }

    @Test
    void testgetAutomationComposition() throws PfModelException {
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var acElementHandler = new AutomationCompositionElementHandler(intermediaryApi);

        var map = CommonTestData.getTestAutomationCompositionMap();
        when(intermediaryApi.getAutomationCompositions()).thenReturn(map);
        var result = acElementHandler.getAutomationCompositions();
        assertEquals(map.values().iterator().next(), result.getAutomationCompositionList().get(0));
    }

    @Test
    void testsetOutProperties() {
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var acElementHandler = new AutomationCompositionElementHandler(intermediaryApi);

        var instanceId = UUID.randomUUID();
        var elementId = UUID.randomUUID();
        var useState = "useState";
        var operationalState = "operationalState";
        Map<String, Object> map = Map.of("id", "1234");

        acElementHandler.setOutProperties(instanceId, elementId, useState, operationalState, map);
        verify(intermediaryApi).sendAcElementInfo(instanceId, elementId, useState, operationalState, map);
    }

    @Test
    void testgetDataList() {
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var acElementHandler = new AutomationCompositionElementHandler(intermediaryApi);

        var map = CommonTestData.getTestAutomationCompositionMap();
        when(intermediaryApi.getAutomationCompositions()).thenReturn(map);
        var result = acElementHandler.getDataList();
        var data = result.getList().get(0);
        var autocomposition = map.values().iterator().next();
        assertEquals(autocomposition.getInstanceId(), data.getAutomationCompositionId());
        var element = autocomposition.getElements().values().iterator().next();
        assertEquals(element.getId(), data.getAutomationCompositionElementId());
    }

    @Test
    void testPrime() throws PfModelException {
        var config = new SimConfig();
        config.setPrimeTimerMs(1);
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var acElementHandler = new AutomationCompositionElementHandler(intermediaryApi);
        acElementHandler.setConfig(config);
        var compositionId = UUID.randomUUID();
        acElementHandler.prime(compositionId, List.of());
        verify(intermediaryApi).updateCompositionState(compositionId, AcTypeState.PRIMED, StateChangeResult.NO_ERROR,
                "Primed");

        config.setPrimeSuccess(false);
        acElementHandler.prime(compositionId, List.of());
        verify(intermediaryApi).updateCompositionState(compositionId, AcTypeState.COMMISSIONED,
                StateChangeResult.FAILED, "Prime failed!");
    }

    @Test
    void testDeprime() throws PfModelException {
        var config = new SimConfig();
        config.setDeprimeTimerMs(1);
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var acElementHandler = new AutomationCompositionElementHandler(intermediaryApi);
        acElementHandler.setConfig(config);
        var compositionId = UUID.randomUUID();
        acElementHandler.deprime(compositionId);
        verify(intermediaryApi).updateCompositionState(compositionId, AcTypeState.COMMISSIONED,
                StateChangeResult.NO_ERROR, "Deprimed");

        config.setDeprimeSuccess(false);
        acElementHandler.deprime(compositionId);
        verify(intermediaryApi).updateCompositionState(compositionId, AcTypeState.PRIMED, StateChangeResult.FAILED,
                "Deprime failed!");
    }
}
