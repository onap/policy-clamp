/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.models.acm.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class AcDtoClassesTest {

    @Test
    void testCompositionDtos() {
        var compositionDtos = new CompositionDtos(List.of());
        assertNotNull(compositionDtos.compositions());
    }

    @Test
    void testAcElementDtoDefaultConstructor() {
        var dto = new AcElementDto();
        assertNull(dto.getCompositionElement());
        assertNull(dto.getInstanceElement());
        assertNull(dto.getCompositionElementTarget());
        assertNull(dto.getInstanceElementTarget());
    }

    @Test
    void testParticipantDtoDefaultConstructor() {
        var dto = new ParticipantDto();
        assertNull(dto.getParticipantId());
        assertThat(dto.getElementDtos()).isEmpty();
    }

    @Test
    void testParticipantPrimeDto() {
        var dto = new ParticipantPrimeDto();
        assertNull(dto.getParticipantId());
        assertNull(dto.getCompositionDto());

        var participantId = UUID.randomUUID();
        var compositionId = UUID.randomUUID();
        var elementId = new ToscaConceptIdentifier("element", "1.0.0");
        var compositionDto = new CompositionDto(compositionId,
                Map.of(elementId, Map.of("key", "val")),
                Map.of(elementId, Map.of("outKey", "outVal")));

        dto.setParticipantId(participantId);
        dto.setCompositionDto(compositionDto);

        assertEquals(participantId, dto.getParticipantId());
        assertEquals(compositionId, dto.getCompositionDto().compositionId());
        assertThat(dto.getCompositionDto().inPropertiesMap()).containsKey(elementId);
    }

    @Test
    void testCompositionElementDtoDefaultState() {
        var compositionId = UUID.randomUUID();
        var elementDefId = new ToscaConceptIdentifier("element", "1.0.0");

        var dto = new CompositionElementDto(compositionId, elementDefId, Map.of(), Map.of());
        assertEquals(ElementState.PRESENT, dto.state());
        assertEquals(compositionId, dto.compositionId());
        assertEquals(elementDefId, dto.elementDefinitionId());
    }

    @Test
    void testInstanceElementDtoDefaultState() {
        var instanceId = UUID.randomUUID();
        var elementId = UUID.randomUUID();

        var dto = new InstanceElementDto(instanceId, elementId, Map.of("k", "v"), Map.of());
        assertEquals(ElementState.PRESENT, dto.state());
        assertEquals(instanceId, dto.instanceId());
        assertEquals(elementId, dto.elementId());
        assertThat(dto.inProperties()).containsEntry("k", "v");
    }

    @Test
    void testInstanceElementDtoWithState() {
        var instanceId = UUID.randomUUID();
        var elementId = UUID.randomUUID();

        var dto = new InstanceElementDto(instanceId, elementId, Map.of(), Map.of(), ElementState.REMOVED);
        assertEquals(ElementState.REMOVED, dto.state());
    }

    @Test
    void testCompositionDto() {
        var compositionId = UUID.randomUUID();
        var elementId = new ToscaConceptIdentifier("el", "1.0.0");
        var inProps = Map.<ToscaConceptIdentifier, Map<String, Object>>of(
                elementId, Map.of("stage", List.of(0, 1)));
        var outProps = Map.<ToscaConceptIdentifier, Map<String, Object>>of(
                elementId, Map.of("InternalState", "PRIMED"));

        var dto = new CompositionDto(compositionId, inProps, outProps);
        assertEquals(compositionId, dto.compositionId());
        assertThat(dto.inPropertiesMap()).containsKey(elementId);
        assertThat(dto.outPropertiesMap()).containsKey(elementId);
    }

    @Test
    void testElementStateValues() {
        assertThat(ElementState.values()).containsExactly(
                ElementState.PRESENT, ElementState.NOT_PRESENT,
                ElementState.REMOVED, ElementState.NEW);
    }

    @Test
    void testCompositionElementDtoToStringSanitizesProperties() {
        var compositionId = UUID.randomUUID();
        var elementDefId = new ToscaConceptIdentifier("element", "1.0.0");
        var dto = new CompositionElementDto(compositionId, elementDefId,
                Map.of("inKey", "inValue"), Map.of("outKey", "outValue"), ElementState.PRESENT);

        assertThat(dto.toString())
                .contains(compositionId.toString(), elementDefId.toString(), ElementState.PRESENT.toString(),
                        "inKey", "outKey")
                .doesNotContain("inValue", "outValue");
    }

    @Test
    void testInstanceElementDtoToStringSanitizesProperties() {
        var instanceId = UUID.randomUUID();
        var elementId = UUID.randomUUID();
        var dto = new InstanceElementDto(instanceId, elementId,
                Map.of("inKey", "inValue"), Map.of("outKey", "outValue"), ElementState.PRESENT);

        assertThat(dto.toString())
                .contains(instanceId.toString(), elementId.toString(), ElementState.PRESENT.toString(),
                        "inKey", "outKey")
                .doesNotContain("inValue", "outValue");
    }

    @Test
    void testCompositionDtoToStringSanitizesProperties() {
        var compositionId = UUID.randomUUID();
        var elementId = new ToscaConceptIdentifier("el", "1.0.0");
        var dto = new CompositionDto(compositionId,
                Map.of(elementId, Map.of("inKey", "inValue")),
                Map.of(elementId, Map.of("outKey", "outValue")));

        assertThat(dto.toString())
                .contains(compositionId.toString(), "inKey", "outKey")
                .doesNotContain("inValue", "outValue");
    }
}
