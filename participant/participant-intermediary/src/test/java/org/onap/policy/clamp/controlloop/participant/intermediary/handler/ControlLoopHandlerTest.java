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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ClElementStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ControlLoopElementListener;
import org.onap.policy.clamp.controlloop.participant.intermediary.main.parameters.CommonTestData;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class ControlLoopHandlerTest {

    private CommonTestData commonTestData = new CommonTestData();

    @Test
    void controlLoopHandlerTest() {
        ControlLoopHandler clh = commonTestData.getMockControlLoopHandler();
        assertNotNull(clh.getControlLoops());

        assertNotNull(clh.getControlLoopMap());
        assertNotNull(clh.getElementsOnThisParticipant());

        UUID elementId1 = UUID.randomUUID();
        ControlLoopElement element = new ControlLoopElement();
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
        UUID id = UUID.randomUUID();

        ControlLoopHandler clh = commonTestData.getMockControlLoopHandler();
        assertNull(clh.updateControlLoopElementState(null, null, ControlLoopOrderedState.UNINITIALISED,
                ControlLoopState.PASSIVE));

        assertNull(clh.updateControlLoopElementState(null, id, ControlLoopOrderedState.UNINITIALISED,
                ControlLoopState.PASSIVE));

        ClElementStatistics clElementStatistics = new ClElementStatistics();
        ToscaConceptIdentifier controlLoopId = new ToscaConceptIdentifier("defName", "0.0.1");
        clElementStatistics.setParticipantId(controlLoopId);
        clElementStatistics.setControlLoopState(ControlLoopState.RUNNING);
        clElementStatistics.setTimeStamp(Instant.now());

        clh.updateControlLoopElementStatistics(id, clElementStatistics);
        assertNull(clh.updateControlLoopElementState(controlLoopId, id, ControlLoopOrderedState.UNINITIALISED,
                ControlLoopState.PASSIVE));



    }

}
