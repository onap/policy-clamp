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

package org.onap.policy.clamp.acm.participant.intermediary.handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.dto.AcElementDto;
import org.onap.policy.clamp.models.acm.dto.CompositionDto;
import org.onap.policy.clamp.models.acm.dto.CompositionElementDto;
import org.onap.policy.clamp.models.acm.dto.InstanceElementDto;
import org.onap.policy.clamp.models.acm.dto.ParticipantDto;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ParticipantDtoUtils {

    /**
     * Resolve the effective instance element from a DTO.
     * For non-update operations, instanceElement may be null and instanceElementTarget holds the current state.
     *
     * @param dto the AcElementDto
     * @return the effective InstanceElementDto, or null if both are null
     */
    public static InstanceElementDto resolveInstanceElement(AcElementDto dto) {
        return dto.getInstanceElement() != null
                ? dto.getInstanceElement() : dto.getInstanceElementTarget();
    }

    /**
     * Extract a map of element ID to AcElementDto for the given participant from the message's participantDtoList.
     *
     * @param participantDtoList the list of ParticipantDto from the Kafka message
     * @param participantId the current participant's ID
     * @return map of element ID to AcElementDto, or empty map if no pre-built DTOs available
     */
    public static Map<UUID, AcElementDto> getElementDtoMap(List<ParticipantDto> participantDtoList,
            UUID participantId) {
        if (participantDtoList == null || participantDtoList.isEmpty()) {
            return Map.of();
        }
        return getAcElementDtos(participantDtoList, participantId)
                .filter(dto -> resolveInstanceElement(dto) != null)
                .collect(Collectors.toMap(
                        dto -> resolveInstanceElement(dto).elementId(),
                        dto -> dto,
                        (a, b) -> b));
    }

    /**
     * Extract the CompositionDto for the given participant from the message's participantDtoList.
     *
     * @param participantDtoList the list of ParticipantDto from the Kafka message
     * @param participantId the current participant's ID
     * @return the CompositionDto
     */
    public static CompositionDto getCompositionDto(List<ParticipantDto> participantDtoList,
            UUID participantId) {
        var list = getAcElementDtos(participantDtoList, participantId)
                .map(AcElementDto::getCompositionElement)
                .filter(Objects::nonNull).toList();
        var inPropertiesMap = list.stream().collect(Collectors.toMap(
                        CompositionElementDto::elementDefinitionId,
                        CompositionElementDto::inProperties));
        var outPropertiesMap = list.stream()
                .collect(Collectors.toMap(
                        CompositionElementDto::elementDefinitionId,
                        CompositionElementDto::outProperties));
        return new CompositionDto(list.getFirst().compositionId(), inPropertiesMap, outPropertiesMap);
    }

    /**
     * Extract the CompositionDto target for the given participant from the message's participantDtoList.
     *
     * @param participantDtoList the list of ParticipantDto from the Kafka message
     * @param participantId the current participant's ID
     * @return the CompositionDto
     */
    public static CompositionDto getCompositionTargetDto(List<ParticipantDto> participantDtoList,
            UUID participantId) {
        var list = getAcElementDtos(participantDtoList, participantId)
                .map(AcElementDto::getCompositionElementTarget)
                .filter(Objects::nonNull).toList();
        var inPropertiesMap = list.stream()
                .collect(Collectors.toMap(
                        CompositionElementDto::elementDefinitionId,
                        CompositionElementDto::inProperties));
        var outPropertiesMap = list.stream()
                .collect(Collectors.toMap(
                        CompositionElementDto::elementDefinitionId,
                        CompositionElementDto::outProperties));
        if (list.isEmpty()) {
            return null;
        }
        return new CompositionDto(list.getFirst().compositionId(), inPropertiesMap, outPropertiesMap);
    }

    private static Stream<AcElementDto> getAcElementDtos(List<ParticipantDto> participantDtoList, UUID participantId) {
        return participantDtoList.stream()
                .filter(p -> participantId.equals(p.getParticipantId()))
                .flatMap(p -> p.getElementDtos().stream());
    }

    /**
     * Extract a map of element ID to AutomationCompositionElementDefinition for the given compositionDto.
     *
     * @param compositionDto the CompositionDto
     * @return the map of element ID to AutomationCompositionElementDefinition
     */
    public static Map<ToscaConceptIdentifier, AutomationCompositionElementDefinition>
            getAutomationCompositionElementDefinitionMap(CompositionDto compositionDto) {
        Map<ToscaConceptIdentifier, AutomationCompositionElementDefinition> result = new HashMap<>();
        var outPropertiesMap = compositionDto.outPropertiesMap();
        compositionDto.inPropertiesMap().forEach((elementId, map) -> {
            var elementDefinition = new AutomationCompositionElementDefinition();
            elementDefinition.setAcElementDefinitionId(elementId);
            elementDefinition.setAutomationCompositionElementToscaNodeTemplate(new ToscaNodeTemplate());
            elementDefinition.getAutomationCompositionElementToscaNodeTemplate().setProperties(new HashMap<>(map));
            var outProperties = outPropertiesMap.containsKey(elementId)
                    ? new HashMap<>(outPropertiesMap.get(elementId)) : Map.<String, Object>of();
            elementDefinition.setOutProperties(outProperties);
            result.put(elementId, elementDefinition);
        });
        return result;
    }

    /**
     * Extract an AutomationCompositionElementDefinition for the given compositionDto and elementId.
     *
     * @param compositionDto the CompositionDto
     * @param elementId the elementId
     * @return the AutomationCompositionElementDefinition
     */
    public static AutomationCompositionElementDefinition getAutomationCompositionElementDefinition(
            CompositionDto compositionDto, ToscaConceptIdentifier elementId) {
        if (!compositionDto.inPropertiesMap().containsKey(elementId)) {
            return null;
        }
        var elementDefinition = new AutomationCompositionElementDefinition();
        elementDefinition.setAcElementDefinitionId(elementId);
        elementDefinition.setAutomationCompositionElementToscaNodeTemplate(new ToscaNodeTemplate());
        elementDefinition.getAutomationCompositionElementToscaNodeTemplate()
                .setProperties(new HashMap<>(compositionDto.inPropertiesMap().get(elementId)));
        elementDefinition.setOutProperties(new HashMap<>(compositionDto.outPropertiesMap().get(elementId)));
        return elementDefinition;
    }

}
