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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ClElementStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElementDefinition;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ControlLoopStateChange;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ControlLoopUpdate;
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ControlLoopElementListener;
import org.onap.policy.clamp.controlloop.participant.intermediary.main.parameters.CommonTestData;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
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

        var clh = setTestControlLoopHandler(id, uuid);
        var key = clh.getElementsOnThisParticipant().keySet().iterator().next();
        var value = clh.getElementsOnThisParticipant().get(key);
        assertEquals(ControlLoopState.UNINITIALISED, value.getState());
        clh.updateControlLoopElementState(id, uuid, ControlLoopOrderedState.UNINITIALISED,
                ControlLoopState.PASSIVE);
        assertEquals(ControlLoopState.PASSIVE, value.getState());

        var clElementStatistics = new ClElementStatistics();
        clElementStatistics.setParticipantId(id);
        clElementStatistics.setControlLoopState(ControlLoopState.RUNNING);
        clElementStatistics.setTimeStamp(Instant.now());

        assertNotEquals(uuid, value.getClElementStatistics().getId());
        clh.updateControlLoopElementStatistics(uuid, clElementStatistics);
        assertEquals(uuid, value.getClElementStatistics().getId());
    }

    @Test
    void handleControlLoopUpdateExceptionTest() throws CoderException {
        var uuid = UUID.randomUUID();
        var id = CommonTestData.getParticipantId();

        var stateChange = new ControlLoopStateChange();
        stateChange.setControlLoopId(id);
        stateChange.setParticipantId(id);
        stateChange.setMessageId(uuid);
        stateChange.setOrderedState(ControlLoopOrderedState.RUNNING);
        stateChange.setCurrentState(ControlLoopState.UNINITIALISED);
        stateChange.setTimestamp(Instant.ofEpochMilli(3000));

        var clh = setTestControlLoopHandler(id, uuid);
        clh.handleControlLoopStateChange(stateChange);
        var newid = new ToscaConceptIdentifier("id", "1.2.3");
        stateChange.setControlLoopId(newid);
        stateChange.setParticipantId(newid);
        assertDoesNotThrow(() -> clh.handleControlLoopStateChange(stateChange));

        List<ControlLoopElementDefinition> clElementDefinitions = new ArrayList<>();
        var cld = new ControlLoopElementDefinition();
        cld.setClElementDefinitionId(id);
        clElementDefinitions.add(cld);
        var updateMsg = new ControlLoopUpdate();
        updateMsg.setControlLoopId(id);
        updateMsg.setMessageId(uuid);
        updateMsg.setParticipantId(id);
        updateMsg.setStartPhase(0);
        assertDoesNotThrow(() -> clh.handleControlLoopUpdate(updateMsg, clElementDefinitions));
        updateMsg.setStartPhase(1);
        assertDoesNotThrow(() -> clh.handleControlLoopUpdate(updateMsg, clElementDefinitions));
    }

    private ControlLoopHandler setTestControlLoopHandler(ToscaConceptIdentifier id, UUID uuid) throws CoderException {
        var clh = commonTestData.getMockControlLoopHandler();

        var key = commonTestData.getTestControlLoopMap().keySet().iterator().next();
        var value = commonTestData.getTestControlLoopMap().get(key);
        clh.getControlLoopMap().put(key, value);

        var keyElem = commonTestData.setControlLoopElementTest(uuid, id).keySet().iterator().next();
        var valueElem = commonTestData.setControlLoopElementTest(uuid, id).get(keyElem);
        clh.getElementsOnThisParticipant().put(keyElem, valueElem);

        return clh;
    }

}
