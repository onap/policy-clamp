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

package org.onap.policy.clamp.models.acm.messages.dmaap.participant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageUtils.assertSerializable;
import static org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageUtils.removeVariableFields;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.AcElementRestart;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantRestartAc;
import org.onap.policy.clamp.models.acm.utils.CommonTestData;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class ParticipantRestartTest {

    @Test
    void testCopyConstructor() throws CoderException {
        assertThatThrownBy(() -> new ParticipantRestart(null)).isInstanceOf(NullPointerException.class);

        final var orig = new ParticipantRestart();
        // verify with null values
        assertEquals(removeVariableFields(orig.toString()),
                removeVariableFields(new ParticipantRestart(orig).toString()));

        orig.setMessageId(UUID.randomUUID());
        orig.setCompositionId(UUID.randomUUID());
        orig.setTimestamp(Instant.ofEpochMilli(3000));
        orig.setParticipantId(CommonTestData.getParticipantId());

        var participantDefinitionUpdate = new ParticipantDefinition();
        var type = new ToscaConceptIdentifier("id", "1.2.3");
        var acDefinition = CommonTestData.getAcElementDefinition(type);
        participantDefinitionUpdate.setAutomationCompositionElementDefinitionList(List.of(acDefinition));
        orig.setParticipantDefinitionUpdates(List.of(participantDefinitionUpdate));

        var acElement = new AcElementRestart();
        acElement.setId(UUID.randomUUID());
        var id = new ToscaConceptIdentifier("id", "1.2.3");
        acElement.setDefinition(id);

        var acRestart = new ParticipantRestartAc();
        acRestart.setAcElementList(List.of(acElement));
        acRestart.setAutomationCompositionId(UUID.randomUUID());

        orig.setAutomationcompositionList(List.of(acRestart));

        assertEquals(removeVariableFields(orig.toString()),
                removeVariableFields(new ParticipantRestart(orig).toString()));

        assertSerializable(orig, ParticipantRestart.class);
    }
}
