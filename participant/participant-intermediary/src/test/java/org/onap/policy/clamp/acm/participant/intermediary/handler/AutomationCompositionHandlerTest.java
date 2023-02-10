/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation.
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.policy.clamp.acm.participant.intermediary.api.AutomationCompositionElementListener;
import org.onap.policy.clamp.acm.participant.intermediary.main.parameters.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDeploy;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionDeploy;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionStateChange;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.LockOrder;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class AutomationCompositionHandlerTest {

    private final CommonTestData commonTestData = new CommonTestData();

    @Test
    void automationCompositionHandlerTest() {
        var ach = commonTestData.getMockAutomationCompositionHandler();
        assertNotNull(ach.getAutomationCompositionMap());
        assertNotNull(ach.getElementsOnThisParticipant());

        var listener = mock(AutomationCompositionElementListener.class);
        ach.registerAutomationCompositionElementListener(listener);
        assertThat(ach.getListeners()).contains(listener);
    }

    @Test
    void updateNullAutomationCompositionHandlerTest() {
        var id = UUID.randomUUID();

        var ach = commonTestData.getMockAutomationCompositionHandler();
        assertDoesNotThrow(
                () -> ach.updateAutomationCompositionElementState(null, null, DeployState.UNDEPLOYED, LockState.NONE));

        assertDoesNotThrow(
                () -> ach.updateAutomationCompositionElementState(null, id, DeployState.UNDEPLOYED, LockState.NONE));
    }

    @Test
    void updateAutomationCompositionHandlerTest() {
        var uuid = UUID.randomUUID();
        var partecipantId = CommonTestData.getParticipantId();
        var definition = CommonTestData.getDefinition();

        var ach = commonTestData.setTestAutomationCompositionHandler(definition, uuid, partecipantId);
        var key = ach.getElementsOnThisParticipant().keySet().iterator().next();
        var value = ach.getElementsOnThisParticipant().get(key);
        assertEquals(DeployState.UNDEPLOYED, value.getDeployState());
        assertEquals(LockState.LOCKED, value.getLockState());
        ach.updateAutomationCompositionElementState(CommonTestData.AC_ID_1, uuid, DeployState.DEPLOYED,
                LockState.UNLOCKED);
        assertEquals(DeployState.DEPLOYED, value.getDeployState());

        ach.getAutomationCompositionMap().values().iterator().next().getElements().putIfAbsent(key, value);
        ach.updateAutomationCompositionElementState(CommonTestData.AC_ID_1, key, DeployState.DEPLOYED,
                LockState.UNLOCKED);
        assertEquals(DeployState.DEPLOYED, value.getDeployState());

        ach.getElementsOnThisParticipant().remove(key, value);
        ach.getAutomationCompositionMap().values().iterator().next().getElements().clear();
        assertDoesNotThrow(() -> ach.updateAutomationCompositionElementState(CommonTestData.AC_ID_1, key,
                DeployState.DEPLOYED, LockState.UNLOCKED));
    }

    @Test
    void handleAutomationCompositionStateChangeTest() {
        var uuid = UUID.randomUUID();
        var partecipantId = CommonTestData.getParticipantId();
        var definition = CommonTestData.getDefinition();
        var stateChange = commonTestData.getStateChange(partecipantId, uuid, DeployOrder.NONE, LockOrder.UNLOCK);
        var ach = commonTestData.setTestAutomationCompositionHandler(definition, uuid, partecipantId);
        assertDoesNotThrow(() -> ach
                .handleAutomationCompositionStateChange(mock(AutomationCompositionStateChange.class), List.of()));

        ach.handleAutomationCompositionStateChange(stateChange, List.of());
        var newPartecipantId = CommonTestData.getRndParticipantId();
        stateChange.setAutomationCompositionId(UUID.randomUUID());
        stateChange.setParticipantId(newPartecipantId);
        assertDoesNotThrow(() -> ach.handleAutomationCompositionStateChange(stateChange, List.of()));
    }

    @Test
    void handleAutomationCompositionDeployTest() {
        var acd = new AutomationCompositionElementDefinition();
        var definition = CommonTestData.getDefinition();
        acd.setAcElementDefinitionId(definition);
        var updateMsg = new AutomationCompositionDeploy();
        updateMsg.setAutomationCompositionId(UUID.randomUUID());
        var uuid = UUID.randomUUID();
        updateMsg.setMessageId(uuid);
        var partecipantId = CommonTestData.getParticipantId();
        updateMsg.setParticipantId(partecipantId);
        updateMsg.setFirstStartPhase(true);
        updateMsg.setStartPhase(0);
        var acElementDefinitions = List.of(acd);
        var ach = commonTestData.setTestAutomationCompositionHandler(definition, uuid, partecipantId);
        assertDoesNotThrow(() -> ach.handleAutomationCompositionDeploy(updateMsg, acElementDefinitions));
        updateMsg.setFirstStartPhase(false);
        updateMsg.setStartPhase(1);
        assertDoesNotThrow(() -> ach.handleAutomationCompositionDeploy(updateMsg, acElementDefinitions));

        ach.getAutomationCompositionMap().clear();
        updateMsg.setFirstStartPhase(true);
        updateMsg.setStartPhase(0);
        assertDoesNotThrow(() -> ach.handleAutomationCompositionDeploy(updateMsg, acElementDefinitions));

        updateMsg.setAutomationCompositionId(UUID.randomUUID());
        updateMsg.setParticipantUpdatesList(List.of(mock(ParticipantDeploy.class)));
        assertDoesNotThrow(() -> ach.handleAutomationCompositionDeploy(updateMsg, acElementDefinitions));

        updateMsg.setStartPhase(1);
        var participantDeploy = new ParticipantDeploy();
        participantDeploy.setParticipantId(partecipantId);
        var element = new AcElementDeploy();
        element.setDefinition(definition);
        participantDeploy.setAcElementList(List.of(element));
        updateMsg.setParticipantUpdatesList(List.of(participantDeploy));

        var acd2 = new AutomationCompositionElementDefinition();
        acd2.setAcElementDefinitionId(definition);
        acd2.setAutomationCompositionElementToscaNodeTemplate(mock(ToscaNodeTemplate.class));
        assertDoesNotThrow(() -> ach.handleAutomationCompositionDeploy(updateMsg, List.of(acd2)));

    }

    @Test
    void acUndeployTest() {
        var uuid = UUID.randomUUID();
        var partecipantId = CommonTestData.getParticipantId();
        var definition = CommonTestData.getDefinition();

        var stateChangeUndeploy =
                commonTestData.getStateChange(partecipantId, uuid, DeployOrder.UNDEPLOY, LockOrder.NONE);

        var ach = commonTestData.setTestAutomationCompositionHandler(definition, uuid, partecipantId);
        stateChangeUndeploy
                .setAutomationCompositionId(ach.getAutomationCompositionMap().entrySet().iterator().next().getKey());
        ach.handleAutomationCompositionStateChange(stateChangeUndeploy, List.of());
        stateChangeUndeploy.setAutomationCompositionId(UUID.randomUUID());
        stateChangeUndeploy.setParticipantId(CommonTestData.getRndParticipantId());
        assertDoesNotThrow(() -> ach.handleAutomationCompositionStateChange(stateChangeUndeploy, List.of()));
    }

    @Test
    void automationCompositionStateUnlock() {
        var uuid = UUID.randomUUID();
        var partecipantId = CommonTestData.getParticipantId();
        var definition = CommonTestData.getDefinition();

        var stateChangeUnlock =
                commonTestData.getStateChange(partecipantId, uuid, DeployOrder.NONE, LockOrder.UNLOCK);

        var ach = commonTestData.setTestAutomationCompositionHandler(definition, uuid, partecipantId);
        stateChangeUnlock
                .setAutomationCompositionId(ach.getAutomationCompositionMap().entrySet().iterator().next().getKey());
        ach.handleAutomationCompositionStateChange(stateChangeUnlock, List.of());
        stateChangeUnlock.setAutomationCompositionId(UUID.randomUUID());
        stateChangeUnlock.setParticipantId(CommonTestData.getRndParticipantId());
        assertDoesNotThrow(() -> ach.handleAutomationCompositionStateChange(stateChangeUnlock, List.of()));
    }
}
