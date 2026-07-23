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

package org.onap.policy.clamp.models.acm.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.dto.AcElementDto;
import org.onap.policy.clamp.models.acm.dto.CompositionElementDto;
import org.onap.policy.clamp.models.acm.dto.ElementState;
import org.onap.policy.clamp.models.acm.dto.InstanceElementDto;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;


class AcElementDtoMapperTest {

    private static final String TOSCA_TEMPLATE_YAML = "clamp/acm/pmsh/funtional-pmsh-usecase.yaml";
    private static final String AUTOMATION_COMPOSITION_ELEMENT =
            "org.onap.policy.clamp.acm.AutomationCompositionElement";

    @Test
    void testToInstanceElementDto() {

        var dtoMapper = Mappers.getMapper(AcElementDtoMapper.class);
        assertDoesNotThrow(
            () -> dtoMapper.toInstanceElementDto(null, null));

        var acElement = new AutomationCompositionElement();
        acElement.setDeployState(DeployState.DEPLOYED);
        acElement.setDefinition(new ToscaConceptIdentifier("testElement", "1.0.0"));
        acElement.setStage(0);
        Map<String, Object> inProperties = Map.of("key1", "val1", "key2", "val2");
        acElement.setProperties(inProperties);
        var outProperties = Map.of("outKey1", "outVal1", "outKey2", Map.of("nestedKey1", "nestedVal1"));
        acElement.setOutProperties(outProperties);
        var instanceId = UUID.randomUUID();
        var instanceElementDto = dtoMapper.toInstanceElementDto(instanceId, acElement);
        assertThat(instanceElementDto.instanceId()).isEqualTo(instanceId);
        assertThat(instanceElementDto.elementId()).isEqualTo(acElement.getId());
        assertThat(instanceElementDto.inProperties()).isEqualTo(inProperties);
        assertThat(instanceElementDto.outProperties()).isEqualTo(outProperties);
        assertThat(instanceElementDto.state()).isEqualTo(ElementState.PRESENT);

        instanceElementDto = dtoMapper.toInstanceElementDto(instanceId, acElement, ElementState.NEW);
        assertThat(instanceElementDto.state()).isEqualTo(ElementState.NEW);
    }

    @Test
    void testToCompositionElementDto() {
        var serviceTemplate = CommonTestData.getToscaServiceTemplate(TOSCA_TEMPLATE_YAML);

        assert serviceTemplate != null;
        var acElements =
                AcmUtils.extractAcElementsFromServiceTemplate(serviceTemplate, AUTOMATION_COMPOSITION_ELEMENT);
        var acDefinition = new AutomationCompositionDefinition();
        var compositionId = UUID.randomUUID();
        acDefinition.setState(AcTypeState.PRIMED);
        acDefinition.setServiceTemplate(serviceTemplate);
        acDefinition.setCompositionId(compositionId);
        acDefinition.setElementStateMap(AcmUtils.createElementStateMap(acElements, AcTypeState.PRIMED));
        acDefinition.getElementStateMap().values().forEach(nodeTemplateState
                -> nodeTemplateState.setOutProperties(Map.of("outProperty", "testProperty")));

        var testElementName = acElements.getFirst().getKey();
        var dtoMapper = Mappers.getMapper(AcElementDtoMapper.class);
        var compositionElementDto = dtoMapper.toCompositionElementDto(acDefinition,
                new ToscaConceptIdentifier(testElementName, "1.0.0"));
        assertThat(compositionElementDto.compositionId()).isEqualTo(compositionId);
        assertThat(compositionElementDto.state()).isEqualTo(ElementState.PRESENT);
        assertThat(compositionElementDto.elementDefinitionId().getName()).isEqualTo(testElementName);
        assertThat(compositionElementDto.outProperties()).containsEntry("outProperty", "testProperty");

        compositionElementDto = dtoMapper.toCompositionElementDto(acDefinition,
                new ToscaConceptIdentifier(testElementName, "1.0.0"), ElementState.NEW);
        assertThat(compositionElementDto.state()).isEqualTo(ElementState.NEW);
    }

    @Test
    void testBackwardCompatibility() throws Exception {
        var coder = new StandardCoder();

        var compositionId = UUID.randomUUID();
        var elementDefId = new ToscaConceptIdentifier("testElement", "1.0.0");
        var inProps = Map.<String, Object>of("key1", "val1");
        var outProps = Map.<String, Object>of("outKey1", "outVal1");

        var compositionElement = new CompositionElementDto(compositionId, elementDefId, inProps, outProps);
        var instanceId = UUID.randomUUID();
        var elementId = UUID.randomUUID();
        var instanceElement = new InstanceElementDto(instanceId, elementId, inProps, outProps);
        var dto = new AcElementDto();
        dto.setInstanceElement(instanceElement);
        dto.setCompositionElement(compositionElement);

        // Serialize and inject unknown fields (simulating runtime sending new fields)
        var json = coder.encode(dto);
        var jsonNode = new ObjectMapper().readTree(json);
        ((ObjectNode) jsonNode.get("compositionElement")).put("newFutureField", "futureValue");
        ((ObjectNode) jsonNode.get("instanceElement")).put("anotherNewField", 12345);
        var jsonWithUnknownFields = jsonNode.toString();

        // Deserialize with old model — should not fail with StandardCoder
        var deserialized = coder.decode(jsonWithUnknownFields, AcElementDto.class);
        assertThat(deserialized.getCompositionElement().compositionId()).isEqualTo(compositionId);
        assertThat(deserialized.getCompositionElement().elementDefinitionId()).isEqualTo(elementDefId);
        assertThat(deserialized.getCompositionElement().inProperties()).isEqualTo(inProps);
        assertThat(deserialized.getInstanceElement().instanceId()).isEqualTo(instanceId);
        assertThat(deserialized.getInstanceElement().elementId()).isEqualTo(elementId);
        assertThat(deserialized.getInstanceElement().outProperties()).isEqualTo(outProps);

        // Should also work with a plain ObjectMapper with @JsonIgnoreProperties
        var plainMapper = new ObjectMapper();
        var deserializedPlain = plainMapper.readValue(jsonWithUnknownFields, AcElementDto.class);
        assertThat(deserializedPlain.getCompositionElement().compositionId()).isEqualTo(compositionId);
        assertThat(deserializedPlain.getInstanceElement().instanceId()).isEqualTo(instanceId);

        // Simulate sender removing a field (e.g. compositionElement removed in future version)
        var jsonNodeForRemoval = new ObjectMapper().readTree(json);
        ((ObjectNode) jsonNodeForRemoval).remove("compositionElement");
        var jsonWithRemovedField = jsonNodeForRemoval.toString();

        var deserializedWithRemoval = coder.decode(jsonWithRemovedField, AcElementDto.class);
        assertThat(deserializedWithRemoval.getCompositionElement()).isNull();
        assertThat(deserializedWithRemoval.getInstanceElement().instanceId()).isEqualTo(instanceId);
    }

}
