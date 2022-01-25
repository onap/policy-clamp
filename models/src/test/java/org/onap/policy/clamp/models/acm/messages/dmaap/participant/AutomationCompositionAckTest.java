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

package org.onap.policy.clamp.models.acm.messages.dmaap.participant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageUtils.assertSerializable;
import static org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageUtils.removeVariableFields;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementAck;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class AutomationCompositionAckTest {

    @Test
    void testCopyConstructor() throws CoderException {
        assertThatThrownBy(() -> new AutomationCompositionAck((AutomationCompositionAck) null))
            .isInstanceOf(NullPointerException.class);

        final var orig = new AutomationCompositionAck(ParticipantMessageType.AUTOMATION_COMPOSITION_UPDATE);

        // verify with null values
        assertEquals(removeVariableFields(orig.toString()),
                removeVariableFields(new AutomationCompositionAck(orig).toString()));

        // verify with all values
        var id = new ToscaConceptIdentifier("id", "1.2.3");
        orig.setAutomationCompositionId(id);
        orig.setParticipantId(id);
        orig.setParticipantType(id);
        var acElementResult = new AutomationCompositionElementAck(AutomationCompositionState.UNINITIALISED,
            true, "AutomationCompositionElement result");
        final var automationCompositionResultMap = Map.of(UUID.randomUUID(), acElementResult);
        orig.setAutomationCompositionResultMap(automationCompositionResultMap);

        orig.setResponseTo(UUID.randomUUID());
        orig.setResult(true);
        orig.setMessage("Successfully processed AutomationCompositionUpdate message");

        assertEquals(removeVariableFields(orig.toString()),
                removeVariableFields(new AutomationCompositionAck(orig).toString()));

        assertSerializable(orig, AutomationCompositionAck.class);
    }
}
