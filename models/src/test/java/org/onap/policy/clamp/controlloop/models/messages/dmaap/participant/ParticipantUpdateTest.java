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
import static org.junit.Assert.assertNotSame;
import static org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantMessageUtils.removeVariableFields;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElementDefinition;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

/**
 * Test the copy constructor.
 */
class ParticipantUpdateTest {
    @Test
    void testCopyConstructor() {
        assertThatThrownBy(() -> new ParticipantUpdate(null)).isInstanceOf(NullPointerException.class);

        ParticipantUpdate orig = new ParticipantUpdate();
        // verify with all values
        ToscaConceptIdentifier id = new ToscaConceptIdentifier();
        id.setName("id");
        id.setVersion("1.2.3");
        orig.setControlLoopId(id);
        orig.setParticipantId(id);
        orig.setParticipantType(id);
        orig.setMessageId(UUID.randomUUID());
        orig.setTimestamp(Instant.ofEpochMilli(3000));

        ToscaServiceTemplate toscaServiceTemplate = new ToscaServiceTemplate();
        toscaServiceTemplate.setName("serviceTemplate");
        toscaServiceTemplate.setDerivedFrom("parentServiceTemplate");
        toscaServiceTemplate.setDescription("Description of serviceTemplate");
        toscaServiceTemplate.setVersion("1.2.3");

        ControlLoopElementDefinition clDefinition = new ControlLoopElementDefinition();
        clDefinition.setId(UUID.randomUUID());
        clDefinition.setControlLoopElementToscaServiceTemplate(toscaServiceTemplate);
        Map<String, String> commonPropertiesMap = new LinkedHashMap<>();
        commonPropertiesMap.put("Prop1", "PropValue");
        clDefinition.setCommonPropertiesMap(commonPropertiesMap);

        Map<UUID, ControlLoopElementDefinition> controlLoopElementDefinitionMap = new LinkedHashMap<>();
        controlLoopElementDefinitionMap.put(UUID.randomUUID(), clDefinition);

        Map<ToscaConceptIdentifier, Map<UUID, ControlLoopElementDefinition>>
            participantDefinitionUpdateMap = new LinkedHashMap<>();
        participantDefinitionUpdateMap.put(id, controlLoopElementDefinitionMap);
        orig.setParticipantDefinitionUpdateMap(participantDefinitionUpdateMap);

        ParticipantUpdate other = new ParticipantUpdate(orig);

        assertEquals(removeVariableFields(orig.toString()), removeVariableFields(other.toString()));
    }
}
