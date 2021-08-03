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

import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class ControlLoopAckTest {

    @Test
    void testCopyConstructor() {
        assertThatThrownBy(() -> new ControlLoopAck((ControlLoopAck) null))
            .isInstanceOf(NullPointerException.class);

        final ControlLoopAck orig = new ControlLoopAck(ParticipantMessageType.CONTROL_LOOP_UPDATE);

        // verify with null values
        assertEquals(removeVariableFields(orig.toString()),
                removeVariableFields(new ControlLoopAck(orig).toString()));

        // verify with all values
        ToscaConceptIdentifier id = new ToscaConceptIdentifier("id", "1.2.3");
        orig.setControlLoopId(id);
        orig.setParticipantId(id);
        orig.setParticipantType(id);

        Pair<Boolean, String> clElementResult = Pair.of(true, "ControlLoopElement result");
        final Map<UUID, Pair<Boolean, String>> controlLoopResultMap = Map.of(UUID.randomUUID(), clElementResult);
        orig.setControlLoopResultMap(controlLoopResultMap);

        orig.setResponseTo(UUID.randomUUID());
        orig.setResult(true);
        orig.setMessage("Successfully processed ControlLoopUpdate message");

        assertEquals(removeVariableFields(orig.toString()),
                removeVariableFields(new ControlLoopAck(orig).toString()));
    }
}
