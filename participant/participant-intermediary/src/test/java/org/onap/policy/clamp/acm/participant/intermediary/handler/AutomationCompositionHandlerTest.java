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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.policy.clamp.acm.participant.intermediary.api.AutomationCompositionElementListener;
import org.onap.policy.clamp.acm.participant.intermediary.main.parameters.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDeploy;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionDeploy;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionStateChange;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
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

        var elementId1 = UUID.randomUUID();
        var element = new AutomationCompositionElement();
        element.setId(elementId1);
        element.setDefinition(
                new ToscaConceptIdentifier("org.onap.policy.acm.PolicyAutomationCompositionParticipant", "1.0.1"));

        element.setOrderedState(AutomationCompositionOrderedState.PASSIVE);

        AutomationCompositionElementListener listener = mock(AutomationCompositionElementListener.class);
        ach.registerAutomationCompositionElementListener(listener);
        assertThat(ach.getListeners()).contains(listener);
    }

    @Test
    void updateNullAutomationCompositionHandlerTest() {
        var id = UUID.randomUUID();

        var ach = commonTestData.getMockAutomationCompositionHandler();
        assertNull(ach.updateAutomationCompositionElementState(null, null,
                AutomationCompositionOrderedState.UNINITIALISED, AutomationCompositionState.PASSIVE));

        assertNull(ach.updateAutomationCompositionElementState(null, id,
                AutomationCompositionOrderedState.UNINITIALISED, AutomationCompositionState.PASSIVE));
    }

    @Test
    void updateAutomationCompositionHandlerTest() {
        var uuid = UUID.randomUUID();
        var partecipantId = CommonTestData.getParticipantId();
        var definition = CommonTestData.getDefinition();

        var ach = commonTestData.setTestAutomationCompositionHandler(definition, uuid, partecipantId);
        var key = ach.getElementsOnThisParticipant().keySet().iterator().next();
        var value = ach.getElementsOnThisParticipant().get(key);
        assertEquals(AutomationCompositionState.UNINITIALISED, value.getState());
        ach.updateAutomationCompositionElementState(CommonTestData.AC_ID_1, uuid,
                AutomationCompositionOrderedState.UNINITIALISED, AutomationCompositionState.PASSIVE);
        assertEquals(AutomationCompositionState.PASSIVE, value.getState());

        ach.getAutomationCompositionMap().values().iterator().next().getElements().putIfAbsent(key, value);
        ach.updateAutomationCompositionElementState(CommonTestData.AC_ID_1, key,
                AutomationCompositionOrderedState.PASSIVE, AutomationCompositionState.RUNNING);
        assertEquals(AutomationCompositionState.RUNNING, value.getState());

        ach.getElementsOnThisParticipant().remove(key, value);
        ach.getAutomationCompositionMap().values().iterator().next().getElements().clear();
        assertNull(ach.updateAutomationCompositionElementState(CommonTestData.AC_ID_1, key,
                AutomationCompositionOrderedState.PASSIVE, AutomationCompositionState.RUNNING));

    }

    @Test
    void handleAutomationCompositionUpdateExceptionTest() {
        var uuid = UUID.randomUUID();
        var partecipantId = CommonTestData.getParticipantId();
        var definition = CommonTestData.getDefinition();
        var stateChange = commonTestData.getStateChange(partecipantId, uuid, AutomationCompositionOrderedState.RUNNING);
        var ach = commonTestData.setTestAutomationCompositionHandler(definition, uuid, partecipantId);
        assertDoesNotThrow(() -> ach
                .handleAutomationCompositionStateChange(mock(AutomationCompositionStateChange.class), List.of()));

        ach.handleAutomationCompositionStateChange(stateChange, List.of());
        var newPartecipantId = CommonTestData.getRndParticipantId();
        stateChange.setAutomationCompositionId(UUID.randomUUID());
        stateChange.setParticipantId(newPartecipantId);
        assertDoesNotThrow(() -> ach.handleAutomationCompositionStateChange(stateChange, List.of()));

        var acd = new AutomationCompositionElementDefinition();
        acd.setAcElementDefinitionId(definition);
        var updateMsg = new AutomationCompositionDeploy();
        updateMsg.setAutomationCompositionId(UUID.randomUUID());
        updateMsg.setMessageId(uuid);
        updateMsg.setParticipantId(partecipantId);
        updateMsg.setStartPhase(0);
        var acElementDefinitions = List.of(acd);
        assertDoesNotThrow(() -> ach.handleAutomationCompositionDeploy(updateMsg, acElementDefinitions));
        updateMsg.setStartPhase(1);
        assertDoesNotThrow(() -> ach.handleAutomationCompositionDeploy(updateMsg, acElementDefinitions));

        ach.getAutomationCompositionMap().clear();
        updateMsg.setStartPhase(0);
        assertDoesNotThrow(() -> ach.handleAutomationCompositionDeploy(updateMsg, acElementDefinitions));

        updateMsg.setAutomationCompositionId(UUID.randomUUID());
        updateMsg.setParticipantUpdatesList(List.of(mock(ParticipantDeploy.class)));
        assertDoesNotThrow(() -> ach.handleAutomationCompositionDeploy(updateMsg, acElementDefinitions));

        updateMsg.setStartPhase(1);
        var participantDeploy = new ParticipantDeploy();
        participantDeploy.setParticipantId(partecipantId);
        var element = new AutomationCompositionElement();
        element.setDefinition(definition);
        participantDeploy.setAutomationCompositionElementList(List.of(element));
        updateMsg.setParticipantUpdatesList(List.of(participantDeploy));

        var acd2 = new AutomationCompositionElementDefinition();
        acd2.setAcElementDefinitionId(definition);
        acd2.setAutomationCompositionElementToscaNodeTemplate(mock(ToscaNodeTemplate.class));
        assertDoesNotThrow(() -> ach.handleAutomationCompositionDeploy(updateMsg, List.of(acd2)));

    }

    @Test
    void automationCompositionStateChangeUninitialisedTest() {
        var uuid = UUID.randomUUID();
        var partecipantId = CommonTestData.getParticipantId();
        var definition = CommonTestData.getDefinition();

        var stateChangeUninitialised =
                commonTestData.getStateChange(partecipantId, uuid, AutomationCompositionOrderedState.UNINITIALISED);

        var ach = commonTestData.setTestAutomationCompositionHandler(definition, uuid, partecipantId);
        ach.handleAutomationCompositionStateChange(stateChangeUninitialised, List.of());
        stateChangeUninitialised.setAutomationCompositionId(UUID.randomUUID());
        stateChangeUninitialised.setParticipantId(CommonTestData.getRndParticipantId());
        assertDoesNotThrow(() -> ach.handleAutomationCompositionStateChange(stateChangeUninitialised, List.of()));
    }

    @Test
    void automationCompositionStateChangePassiveTest() {
        var uuid = UUID.randomUUID();
        var partecipantId = CommonTestData.getParticipantId();
        var definition = CommonTestData.getDefinition();

        var stateChangePassive =
                commonTestData.getStateChange(partecipantId, uuid, AutomationCompositionOrderedState.PASSIVE);

        var ach = commonTestData.setTestAutomationCompositionHandler(definition, uuid, partecipantId);
        ach.handleAutomationCompositionStateChange(stateChangePassive, List.of());
        stateChangePassive.setAutomationCompositionId(UUID.randomUUID());
        stateChangePassive.setParticipantId(CommonTestData.getRndParticipantId());
        assertDoesNotThrow(() -> ach.handleAutomationCompositionStateChange(stateChangePassive, List.of()));
    }
}
