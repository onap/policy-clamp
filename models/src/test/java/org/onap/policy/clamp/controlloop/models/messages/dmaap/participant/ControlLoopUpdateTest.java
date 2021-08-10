/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.models.messages.dmaap.participant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantMessageUtils.removeVariableFields;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Test the copy constructor.
 */
class ControlLoopUpdateTest {
    @Test
    void testCopyConstructor() {
        assertThatThrownBy(() -> new ControlLoopUpdate(null)).isInstanceOf(NullPointerException.class);

        ControlLoopUpdate orig = new ControlLoopUpdate();
        // verify with all values
        ToscaConceptIdentifier id = new ToscaConceptIdentifier("id", "1.2.3");
        orig.setControlLoopId(id);
        orig.setParticipantId(null);
        orig.setMessageId(UUID.randomUUID());
        orig.setTimestamp(Instant.ofEpochMilli(3000));

        ControlLoopElement clElement = new ControlLoopElement();
        clElement.setId(UUID.randomUUID());
        clElement.setDefinition(id);
        clElement.setDescription("Description");
        clElement.setOrderedState(ControlLoopOrderedState.PASSIVE);
        clElement.setState(ControlLoopState.PASSIVE);
        clElement.setParticipantId(id);
        clElement.setParticipantType(id);

        Map<String, String> commonPropertiesMap = Map.of("Prop1", "PropValue");
        clElement.setCommonPropertiesMap(commonPropertiesMap);

        Map<ToscaConceptIdentifier, ControlLoopElement> controlLoopElementMap = Map.of(id, clElement);
        Map<ToscaConceptIdentifier, Map<ToscaConceptIdentifier, ControlLoopElement>>
            participantUpdateMap = Map.of(id, controlLoopElementMap);
        orig.setParticipantUpdateMap(participantUpdateMap);

        ControlLoopUpdate other = new ControlLoopUpdate(orig);

        assertEquals(removeVariableFields(orig.toString()), removeVariableFields(other.toString()));
    }
}
