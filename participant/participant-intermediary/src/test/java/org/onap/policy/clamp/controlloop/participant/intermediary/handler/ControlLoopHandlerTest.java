/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.participant.intermediary.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ClElementStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElementDefinition;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantUpdates;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ControlLoopStateChange;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ControlLoopUpdate;
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ControlLoopElementListener;
import org.onap.policy.clamp.controlloop.participant.intermediary.main.parameters.CommonTestData;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class ControlLoopHandlerTest {

    private CommonTestData commonTestData = new CommonTestData();

    @Test
    void controlLoopHandlerTest() {
        var clh = commonTestData.getMockControlLoopHandler();
        assertNotNull(clh.getControlLoops());

        assertNotNull(clh.getControlLoopMap());
        assertNotNull(clh.getElementsOnThisParticipant());

        var elementId1 = UUID.randomUUID();
        var element = new ControlLoopElement();
        element.setId(elementId1);
        element.setDefinition(new ToscaConceptIdentifier(
                "org.onap.policy.controlloop.PolicyControlLoopParticipant", "1.0.1"));

        element.setOrderedState(ControlLoopOrderedState.PASSIVE);

        ControlLoopElementListener listener = mock(ControlLoopElementListener.class);
        clh.registerControlLoopElementListener(listener);
        assertThat(clh.getListeners()).contains(listener);
    }

    @Test
    void updateNullControlLoopHandlerTest() {
        var id = UUID.randomUUID();

        var clh = commonTestData.getMockControlLoopHandler();
        assertNull(clh.updateControlLoopElementState(null, null, ControlLoopOrderedState.UNINITIALISED,
                ControlLoopState.PASSIVE));

        assertNull(clh.updateControlLoopElementState(null, id, ControlLoopOrderedState.UNINITIALISED,
                ControlLoopState.PASSIVE));

        var clElementStatistics = new ClElementStatistics();
        var controlLoopId = new ToscaConceptIdentifier("defName", "0.0.1");
        clElementStatistics.setParticipantId(controlLoopId);
        clElementStatistics.setControlLoopState(ControlLoopState.RUNNING);
        clElementStatistics.setTimeStamp(Instant.now());

        clh.updateControlLoopElementStatistics(id, clElementStatistics);
        assertNull(clh.updateControlLoopElementState(controlLoopId, id, ControlLoopOrderedState.UNINITIALISED,
                ControlLoopState.PASSIVE));
    }

    @Test
    void updateControlLoopHandlerTest() throws CoderException {
        var uuid = UUID.randomUUID();
        var id = CommonTestData.getParticipantId();

        var clh = commonTestData.setTestControlLoopHandler(id, uuid);
        var key = clh.getElementsOnThisParticipant().keySet().iterator().next();
        var value = clh.getElementsOnThisParticipant().get(key);
        assertEquals(ControlLoopState.UNINITIALISED, value.getState());
        clh.updateControlLoopElementState(id, uuid, ControlLoopOrderedState.UNINITIALISED,
                ControlLoopState.PASSIVE);
        assertEquals(ControlLoopState.PASSIVE, value.getState());

        clh.getControlLoopMap().values().iterator().next().getElements().putIfAbsent(key, value);
        clh.updateControlLoopElementState(id, key, ControlLoopOrderedState.PASSIVE,
                ControlLoopState.RUNNING);
        assertEquals(ControlLoopState.RUNNING, value.getState());

        var clElementStatistics = new ClElementStatistics();
        clElementStatistics.setParticipantId(id);
        clElementStatistics.setControlLoopState(ControlLoopState.RUNNING);
        clElementStatistics.setTimeStamp(Instant.now());

        assertNotEquals(uuid, value.getClElementStatistics().getId());
        clh.updateControlLoopElementStatistics(uuid, clElementStatistics);
        assertEquals(uuid, value.getClElementStatistics().getId());

        clh.getElementsOnThisParticipant().remove(key, value);
        clh.getControlLoopMap().values().iterator().next().getElements().clear();
        assertNull(clh.updateControlLoopElementState(id, key, ControlLoopOrderedState.PASSIVE,
                ControlLoopState.RUNNING));

    }

    @Test
    void handleControlLoopUpdateExceptionTest() throws CoderException {
        var uuid = UUID.randomUUID();
        var id = CommonTestData.getParticipantId();
        var stateChange = getStateChange(id, uuid, ControlLoopOrderedState.RUNNING);
        var clh = commonTestData.setTestControlLoopHandler(id, uuid);
        assertDoesNotThrow(() -> clh.handleControlLoopStateChange(mock(ControlLoopStateChange.class), List.of()));

        clh.handleControlLoopStateChange(stateChange, List.of());
        var newid = new ToscaConceptIdentifier("id", "1.2.3");
        stateChange.setControlLoopId(newid);
        stateChange.setParticipantId(newid);
        assertDoesNotThrow(() -> clh.handleControlLoopStateChange(stateChange, List.of()));

        var cld = new ControlLoopElementDefinition();
        cld.setClElementDefinitionId(id);
        var updateMsg = new ControlLoopUpdate();
        updateMsg.setControlLoopId(id);
        updateMsg.setMessageId(uuid);
        updateMsg.setParticipantId(id);
        updateMsg.setStartPhase(0);
        var clElementDefinitions = List.of(cld);
        assertDoesNotThrow(() -> clh.handleControlLoopUpdate(updateMsg, clElementDefinitions));
        updateMsg.setStartPhase(1);
        assertDoesNotThrow(() -> clh.handleControlLoopUpdate(updateMsg, clElementDefinitions));
        assertThat(clh.getClElementInstanceProperties(uuid)).isEmpty();

        clh.getControlLoopMap().clear();
        updateMsg.setStartPhase(0);
        assertDoesNotThrow(() -> clh.handleControlLoopUpdate(updateMsg, clElementDefinitions));

        updateMsg.setControlLoopId(new ToscaConceptIdentifier("new", "0.0.1"));
        updateMsg.setParticipantUpdatesList(List.of(mock(ParticipantUpdates.class)));
        assertDoesNotThrow(() -> clh.handleControlLoopUpdate(updateMsg, clElementDefinitions));

        updateMsg.setStartPhase(1);
        var participantUpdate = new ParticipantUpdates();
        participantUpdate.setParticipantId(id);
        var element = new ControlLoopElement();
        element.setParticipantType(id);
        element.setDefinition(id);
        participantUpdate.setControlLoopElementList(List.of(element));
        updateMsg.setParticipantUpdatesList(List.of(participantUpdate));

        var cld2 = new ControlLoopElementDefinition();
        cld2.setClElementDefinitionId(id);
        cld2.setControlLoopElementToscaNodeTemplate(mock(ToscaNodeTemplate.class));
        assertDoesNotThrow(() -> clh.handleControlLoopUpdate(updateMsg, List.of(cld2)));

    }

    @Test
    void controlLoopStateChangeUninitialisedTest() throws CoderException {
        var uuid = UUID.randomUUID();
        var id = CommonTestData.getParticipantId();

        var stateChangeUninitialised = getStateChange(id, uuid, ControlLoopOrderedState.UNINITIALISED);

        var clh = commonTestData.setTestControlLoopHandler(id, uuid);
        clh.handleControlLoopStateChange(stateChangeUninitialised, List.of());
        var newid = new ToscaConceptIdentifier("id", "1.2.3");
        stateChangeUninitialised.setControlLoopId(newid);
        stateChangeUninitialised.setParticipantId(newid);
        assertDoesNotThrow(() -> clh.handleControlLoopStateChange(stateChangeUninitialised, List.of()));
    }

    @Test
    void controlLoopStateChangePassiveTest() throws CoderException {
        var uuid = UUID.randomUUID();
        var id = CommonTestData.getParticipantId();

        var stateChangePassive = getStateChange(id, uuid, ControlLoopOrderedState.PASSIVE);

        var clh = commonTestData.setTestControlLoopHandler(id, uuid);
        clh.handleControlLoopStateChange(stateChangePassive, List.of());
        var newid = new ToscaConceptIdentifier("id", "1.2.3");
        stateChangePassive.setControlLoopId(newid);
        stateChangePassive.setParticipantId(newid);
        assertDoesNotThrow(() -> clh.handleControlLoopStateChange(stateChangePassive, List.of()));
    }


    private ControlLoopStateChange getStateChange(ToscaConceptIdentifier id, UUID uuid, ControlLoopOrderedState state) {
        var stateChange = new ControlLoopStateChange();
        stateChange.setControlLoopId(id);
        stateChange.setParticipantId(id);
        stateChange.setMessageId(uuid);
        stateChange.setOrderedState(state);
        stateChange.setCurrentState(ControlLoopState.UNINITIALISED);
        stateChange.setTimestamp(Instant.ofEpochMilli(3000));
        return stateChange;
    }

}
