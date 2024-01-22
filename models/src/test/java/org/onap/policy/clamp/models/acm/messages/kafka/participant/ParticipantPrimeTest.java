/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2024 Nordix Foundation.
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

package org.onap.policy.clamp.models.acm.messages.kafka.participant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessageUtils.assertSerializable;
import static org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessageUtils.removeVariableFields;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDefinition;
import org.onap.policy.clamp.models.acm.utils.CommonTestData;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Test the copy constructor.
 */
class ParticipantPrimeTest {
    @Test
    void testCopyConstructor() throws CoderException {
        assertThatThrownBy(() -> new ParticipantPrime(null)).isInstanceOf(NullPointerException.class);

        var orig = new ParticipantPrime();
        // verify with all values
        orig.setAutomationCompositionId(UUID.randomUUID());
        orig.setParticipantId(CommonTestData.getParticipantId());
        orig.setMessageId(UUID.randomUUID());
        orig.setTimestamp(Instant.ofEpochMilli(3000));

        var participantDefinitionUpdate = new ParticipantDefinition();
        var type = new ToscaConceptIdentifier("id", "1.2.3");
        var acDefinition = CommonTestData.getAcElementDefinition(type);
        participantDefinitionUpdate.setAutomationCompositionElementDefinitionList(List.of(acDefinition));
        orig.setParticipantDefinitionUpdates(List.of(participantDefinitionUpdate));

        var other = new ParticipantPrime(orig);

        assertEquals(removeVariableFields(orig.toString()), removeVariableFields(other.toString()));

        assertSerializable(orig, ParticipantPrime.class);
    }
}
