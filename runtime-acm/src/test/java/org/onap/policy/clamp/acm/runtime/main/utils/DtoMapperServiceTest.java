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

package org.onap.policy.clamp.acm.runtime.main.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionRollback;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.MigrationState;
import org.onap.policy.clamp.models.acm.concepts.NodeTemplateState;
import org.onap.policy.clamp.models.acm.dto.AcElementDto;
import org.onap.policy.clamp.models.acm.dto.ElementState;
import org.onap.policy.clamp.models.acm.dto.ParticipantDto;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaTopologyTemplate;

class DtoMapperServiceTest {

    private static final UUID INSTANCE_ID = UUID.randomUUID();
    private static final UUID COMPOSITION_ID = UUID.randomUUID();
    private static final UUID COMPOSITION_TARGET_ID = UUID.randomUUID();
    private static final UUID PARTICIPANT_ID_1 = UUID.randomUUID();
    private static final UUID PARTICIPANT_ID_2 = UUID.randomUUID();
    private static final UUID PARTICIPANT_ID_3 = UUID.randomUUID();
    private static final ToscaConceptIdentifier ELEMENT_DEF_1 = new ToscaConceptIdentifier("element1", "1.0.0");
    private static final ToscaConceptIdentifier ELEMENT_DEF_2 = new ToscaConceptIdentifier("element2", "1.0.0");
    private static final ToscaConceptIdentifier ELEMENT_DEF_3 = new ToscaConceptIdentifier("element3", "1.0.0");

    private final DtoMapperService dtoMapperService = new DtoMapperService();

    @Test
    void testCreateDtoList() {
        var element = createAcElement(PARTICIPANT_ID_1, ELEMENT_DEF_1);
        var ac = createAutomationComposition(Map.of(element.getId(), element));
        var acDefinition = createAcDefinition(COMPOSITION_ID, ELEMENT_DEF_1);
        var acPriorUpdate = createAcRollback(element);

        var result = dtoMapperService.createDtoList(acPriorUpdate, ac, acDefinition, null);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getParticipantId()).isEqualTo(PARTICIPANT_ID_1);

        var dto = result.getFirst().getElementDtos().getFirst();
        assertThat(dto.getCompositionElement()).isNotNull();
        assertThat(dto.getCompositionElement().compositionId()).isEqualTo(COMPOSITION_ID);
        assertThat(dto.getInstanceElement()).isNotNull();
        assertThat(dto.getInstanceElementTarget()).isNotNull();
        assertThat(dto.getInstanceElementTarget().instanceId()).isEqualTo(INSTANCE_ID);
        assertThat(dto.getCompositionElementTarget()).isNull();
    }

    @Test
    void testCreateDtoListWithNullRollback() {
        var element = createAcElement(PARTICIPANT_ID_1, ELEMENT_DEF_1);
        var ac = createAutomationComposition(Map.of(element.getId(), element));
        var acDefinition = createAcDefinition(COMPOSITION_ID, ELEMENT_DEF_1);

        var result = dtoMapperService.createDtoList(null, ac, acDefinition, null);

        var dto = result.getFirst().getElementDtos().getFirst();
        assertThat(dto.getInstanceElement()).isNull();
        assertThat(dto.getInstanceElementTarget()).isNotNull();
    }

    @Test
    void testCreateDtoListMultipleParticipants() {
        var element1 = createAcElement(PARTICIPANT_ID_1, ELEMENT_DEF_1);
        var element2 = createAcElement(PARTICIPANT_ID_2, ELEMENT_DEF_1);
        var elements = new HashMap<UUID, AutomationCompositionElement>();
        elements.put(element1.getId(), element1);
        elements.put(element2.getId(), element2);

        var ac = createAutomationComposition(elements);
        var acDefinition = createAcDefinition(COMPOSITION_ID, ELEMENT_DEF_1);
        var rollback = createAcRollback(element1, element2);

        var result = dtoMapperService.createDtoList(rollback, ac, acDefinition, null);

        assertThat(result).hasSize(2);
        assertThat(result).extracting("participantId")
                .containsExactlyInAnyOrder(PARTICIPANT_ID_1, PARTICIPANT_ID_2);
    }

    @Test
    void testCreateMigrateDtoAllStates() {
        var newElement = createAcElement(PARTICIPANT_ID_1, ELEMENT_DEF_1, MigrationState.NEW);
        var removedElement = createAcElement(PARTICIPANT_ID_2, ELEMENT_DEF_2, MigrationState.REMOVED);
        var defaultElement = createAcElement(PARTICIPANT_ID_3, ELEMENT_DEF_3, MigrationState.DEFAULT);

        var elements = new LinkedHashMap<UUID, AutomationCompositionElement>();
        elements.put(newElement.getId(), newElement);
        elements.put(removedElement.getId(), removedElement);
        elements.put(defaultElement.getId(), defaultElement);

        var ac = createAutomationComposition(elements);
        ac.setCompositionTargetId(COMPOSITION_TARGET_ID);
        ac.setDeployState(DeployState.MIGRATING);

        var acDefinition = createAcDefinition(COMPOSITION_ID, ELEMENT_DEF_1, ELEMENT_DEF_2, ELEMENT_DEF_3);
        var acDefinitionTarget = createAcDefinition(COMPOSITION_TARGET_ID, ELEMENT_DEF_1, ELEMENT_DEF_2, ELEMENT_DEF_3);
        var rollback = createAcRollback(defaultElement);

        var result = dtoMapperService.createMigrationDto(rollback, acDefinition, ac, acDefinitionTarget);

        assertThat(result).hasSize(3);
        assertThat(result).extracting("participantId")
                .containsExactlyInAnyOrder(PARTICIPANT_ID_1, PARTICIPANT_ID_2, PARTICIPANT_ID_3);

        // NEW element: source is NOT_PRESENT, target is NEW
        var newDto = getElementDtoForParticipant(result, PARTICIPANT_ID_1);
        assertThat(newDto.getInstanceElement().state()).isEqualTo(ElementState.NOT_PRESENT);
        assertThat(newDto.getCompositionElement().state()).isEqualTo(ElementState.NOT_PRESENT);
        assertThat(newDto.getInstanceElementTarget().state()).isEqualTo(ElementState.NEW);
        assertThat(newDto.getCompositionElementTarget().state()).isEqualTo(ElementState.NEW);

        // REMOVED element: source is PRESENT, target is REMOVED
        var removedDto = getElementDtoForParticipant(result, PARTICIPANT_ID_2);
        assertThat(removedDto.getInstanceElement().state()).isEqualTo(ElementState.PRESENT);
        assertThat(removedDto.getCompositionElement().state()).isEqualTo(ElementState.PRESENT);
        assertThat(removedDto.getInstanceElementTarget().state()).isEqualTo(ElementState.REMOVED);
        assertThat(removedDto.getCompositionElementTarget().state()).isEqualTo(ElementState.REMOVED);

        // DEFAULT element: both source and target are PRESENT
        var defaultDto = getElementDtoForParticipant(result, PARTICIPANT_ID_3);
        assertThat(defaultDto.getCompositionElement().state()).isEqualTo(ElementState.PRESENT);
        assertThat(defaultDto.getInstanceElement().state()).isEqualTo(ElementState.PRESENT);
        assertThat(defaultDto.getCompositionElementTarget().state()).isEqualTo(ElementState.PRESENT);
        assertThat(defaultDto.getInstanceElementTarget().state()).isEqualTo(ElementState.PRESENT);
    }

    @Test
    void testCreateRollbackDto() {
        var newElement = createAcElement(PARTICIPANT_ID_1, ELEMENT_DEF_1, MigrationState.NEW);
        var removedElement = createAcElement(PARTICIPANT_ID_2, ELEMENT_DEF_2, MigrationState.REMOVED);
        var defaultElement = createAcElement(PARTICIPANT_ID_3, ELEMENT_DEF_3, MigrationState.DEFAULT);

        var elements = new LinkedHashMap<UUID, AutomationCompositionElement>();
        elements.put(newElement.getId(), newElement);
        elements.put(removedElement.getId(), removedElement);
        elements.put(defaultElement.getId(), defaultElement);

        var ac = createAutomationComposition(elements);
        ac.setCompositionTargetId(COMPOSITION_TARGET_ID);
        ac.setDeployState(DeployState.MIGRATION_REVERTING);

        var acDefinition = createAcDefinition(COMPOSITION_ID, ELEMENT_DEF_1, ELEMENT_DEF_2, ELEMENT_DEF_3);
        var acDefinitionTarget = createAcDefinition(COMPOSITION_TARGET_ID, ELEMENT_DEF_1, ELEMENT_DEF_2, ELEMENT_DEF_3);
        var rollback = createAcRollback(defaultElement);

        var result = dtoMapperService.createMigrationDto(rollback, acDefinition, ac, acDefinitionTarget);

        assertThat(result).hasSize(3);

        // NEW element in rollback: target is REMOVED (rolling back a new element removes it)
        var newDto = getElementDtoForParticipant(result, PARTICIPANT_ID_1);
        assertThat(newDto.getInstanceElementTarget().state()).isEqualTo(ElementState.REMOVED);
        assertThat(newDto.getCompositionElementTarget().state()).isEqualTo(ElementState.REMOVED);

        // REMOVED element in rollback: target is NEW (rolling back a removal re-adds it)
        var removedDto = getElementDtoForParticipant(result, PARTICIPANT_ID_2);
        assertThat(removedDto.getInstanceElement().state()).isEqualTo(ElementState.NOT_PRESENT);
        assertThat(removedDto.getCompositionElement().state()).isEqualTo(ElementState.NOT_PRESENT);
        assertThat(removedDto.getInstanceElementTarget().state()).isEqualTo(ElementState.NEW);
        assertThat(removedDto.getCompositionElementTarget().state()).isEqualTo(ElementState.NEW);
    }

    private AcElementDto getElementDtoForParticipant(List<ParticipantDto> result, UUID participantId) {
        return result.stream()
                .filter(p -> participantId.equals(p.getParticipantId()))
                .findFirst().orElseThrow()
                .getElementDtos().getFirst();
    }

    private AutomationCompositionElement createAcElement(UUID participantId, ToscaConceptIdentifier definition) {
        return createAcElement(participantId, definition, MigrationState.DEFAULT);
    }

    private AutomationCompositionElement createAcElement(UUID participantId, ToscaConceptIdentifier definition,
                                                         MigrationState migrationState) {
        var element = new AutomationCompositionElement();
        element.setId(UUID.randomUUID());
        element.setParticipantId(participantId);
        element.setDefinition(definition);
        element.setMigrationState(migrationState);
        element.setProperties(Map.of("key", "value"));
        element.setOutProperties(Map.of("outKey", "outValue"));
        return element;
    }

    private AutomationComposition createAutomationComposition(
            Map<UUID, AutomationCompositionElement> elements) {
        var ac = new AutomationComposition();
        ac.setInstanceId(INSTANCE_ID);
        ac.setCompositionId(COMPOSITION_ID);
        ac.setDeployState(DeployState.MIGRATING);
        ac.setElements(new HashMap<>(elements));
        return ac;
    }

    private AutomationCompositionDefinition createAcDefinition(UUID compositionId,
                                                                ToscaConceptIdentifier... elementDefs) {
        var acDefinition = new AutomationCompositionDefinition();
        acDefinition.setCompositionId(compositionId);
        acDefinition.setState(AcTypeState.PRIMED);

        var nodeTemplates = new HashMap<String, ToscaNodeTemplate>();
        var elementStateMap = new HashMap<String, NodeTemplateState>();

        for (var def : elementDefs) {
            var nodeTemplate = new ToscaNodeTemplate();
            nodeTemplate.setProperties(Map.of("compositionProp", "compositionValue"));
            nodeTemplates.put(def.getName(), nodeTemplate);

            var state = new NodeTemplateState();
            state.setNodeTemplateStateId(UUID.randomUUID());
            state.setOutProperties(Map.of("outProp", "outVal"));
            elementStateMap.put(def.getName(), state);
        }

        var topologyTemplate = new ToscaTopologyTemplate();
        topologyTemplate.setNodeTemplates(nodeTemplates);
        var serviceTemplate = new ToscaServiceTemplate();
        serviceTemplate.setToscaTopologyTemplate(topologyTemplate);

        acDefinition.setServiceTemplate(serviceTemplate);
        acDefinition.setElementStateMap(elementStateMap);
        return acDefinition;
    }

    private AutomationCompositionRollback createAcRollback(AutomationCompositionElement... elements) {
        var rollback = new AutomationCompositionRollback();
        rollback.setInstanceId(INSTANCE_ID);
        rollback.setCompositionId(COMPOSITION_ID);
        var elementMap = new HashMap<UUID, AutomationCompositionElement>();
        for (var element : elements) {
            var rollbackElement = new AutomationCompositionElement();
            rollbackElement.setId(element.getId());
            rollbackElement.setDefinition(element.getDefinition());
            rollbackElement.setProperties(Map.of("oldKey", "oldValue"));
            rollbackElement.setOutProperties(Map.of("oldOut", "oldOutValue"));
            elementMap.put(element.getId(), rollbackElement);
        }
        rollback.setElements(elementMap);
        return rollback;
    }
}
