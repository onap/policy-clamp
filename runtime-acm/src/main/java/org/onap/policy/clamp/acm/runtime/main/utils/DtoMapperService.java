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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionRollback;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.MigrationState;
import org.onap.policy.clamp.models.acm.dto.AcElementDto;
import org.onap.policy.clamp.models.acm.dto.CompositionElementDto;
import org.onap.policy.clamp.models.acm.dto.ElementState;
import org.onap.policy.clamp.models.acm.dto.InstanceElementDto;
import org.onap.policy.clamp.models.acm.dto.ParticipantDto;
import org.onap.policy.clamp.models.acm.utils.AcElementDtoMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DtoMapperService {

    /**
     * Create a list of ParticipantDTO.
     * @param acPriorUpdate AutomationComposition prior to update
     * @param automationComposition target AutomationComposition
     * @param acDefinition AutomationCompositionDefinition
     * @return ParticipantDto
     */
    public List<ParticipantDto> createDtoList(AutomationCompositionRollback acPriorUpdate,
                                              AutomationComposition automationComposition,
                                              AutomationCompositionDefinition acDefinition,
                                              AutomationCompositionDefinition acDefinitionTarget) {
        Map<UUID, List<AcElementDto>> participantDtoMap = new HashMap<>();
        var instanceId = automationComposition.getInstanceId();
        var acElementList = automationComposition.getElements();

        for (var element : acElementList.values()) {
            var instanceElementDto = acPriorUpdate != null ? AcElementDtoMapper.toInstanceElementDto(instanceId,
                    acPriorUpdate.getElements().get(element.getId())) : null;
            var instanceElementDtoTarget = AcElementDtoMapper.toInstanceElementDto(instanceId, element);

            var preferredDefinition = element.getMigrationState()
                    .equals(MigrationState.NEW) ? acDefinitionTarget : acDefinition;

            var compositionElementDto = AcElementDtoMapper.toCompositionElementDto(preferredDefinition,
                    element.getDefinition());

            var acElementDto = buildAcElementDto(compositionElementDto, instanceElementDto, null,
                    instanceElementDtoTarget, element);

            var participantId = element.getParticipantId();
            participantDtoMap.computeIfAbsent(participantId, k -> new ArrayList<>()).add(acElementDto);
        }
        return buildParticipantDtoList(participantDtoMap);
    }

    /**
     * Create a List of ParticipantDto for Migration.
     * @param acPriorUpdate AutomationComposition prior to update
     * @param acDefinition AutomationCompositionDefinition
     * @param automationComposition AutomationComposition target
     * @param acDefinitionTarget AutomationCompositionDefinition target
     * @return List of ParticipantDto
     */
    public List<ParticipantDto> createMigrationDto(AutomationCompositionRollback acPriorUpdate,
                                                   AutomationCompositionDefinition acDefinition,
                                                   AutomationComposition automationComposition,
                                                   AutomationCompositionDefinition acDefinitionTarget) {
        if (DeployState.MIGRATION_REVERTING.equals(automationComposition.getDeployState())) {
            return createRollbackDto(acPriorUpdate, acDefinition, automationComposition, acDefinitionTarget);
        } else {
            return createMigrateDto(acPriorUpdate, acDefinition, automationComposition, acDefinitionTarget);
        }
    }

    /**
     * Create a List of ParticipantDto for Migration.
     * @param acPriorUpdate AutomationComposition prior to update
     * @param acDefinition AutomationCompositionDefinition
     * @param automationComposition AutomationComposition target
     * @param acDefinitionTarget AutomationCompositionDefinition target
     * @return List of ParticipantDto
     */
    public List<ParticipantDto> createMigrateDto(AutomationCompositionRollback acPriorUpdate,
                                                   AutomationCompositionDefinition acDefinition,
                                                   AutomationComposition automationComposition,
                                                   AutomationCompositionDefinition acDefinitionTarget) {
        var instanceId = automationComposition.getInstanceId();
        var acElementList = automationComposition.getElements();
        InstanceElementDto instanceElementDto;
        CompositionElementDto compositionElementDto;
        InstanceElementDto instanceElementTarget;
        CompositionElementDto compositionElementTarget;
        Map<UUID, List<AcElementDto>> participantDtoMap = new HashMap<>();

        for (var element : acElementList.values()) {
            if (MigrationState.NEW.equals(element.getMigrationState())) {
                instanceElementDto = new InstanceElementDto(instanceId, element.getId(), Map.of(), Map.of(),
                        ElementState.NOT_PRESENT);
                compositionElementDto = new CompositionElementDto(automationComposition.getCompositionId(),
                        element.getDefinition(), Map.of(), Map.of(), ElementState.NOT_PRESENT);
                instanceElementTarget = AcElementDtoMapper.toInstanceElementDto(instanceId, element, ElementState.NEW);
                compositionElementTarget = AcElementDtoMapper.toCompositionElementDto(acDefinitionTarget,
                        element.getDefinition(), ElementState.NEW);
            } else if (MigrationState.REMOVED.equals(element.getMigrationState())) {
                compositionElementTarget = new CompositionElementDto(automationComposition.getCompositionTargetId(),
                        element.getDefinition(), Map.of(), Map.of(), ElementState.REMOVED);
                instanceElementTarget = new InstanceElementDto(instanceId,
                        element.getId(), Map.of(), Map.of(), ElementState.REMOVED);
                instanceElementDto = AcElementDtoMapper.toInstanceElementDto(instanceId, element);
                compositionElementDto = AcElementDtoMapper.toCompositionElementDto(acDefinition,
                        element.getDefinition());
            } else  { // default scenario
                compositionElementTarget = AcElementDtoMapper.toCompositionElementDto(acDefinitionTarget,
                        element.getDefinition());
                instanceElementTarget = AcElementDtoMapper.toInstanceElementDto(instanceId, element);
                var elementPrior = acPriorUpdate.getElements().get(element.getId());
                instanceElementDto = AcElementDtoMapper.toInstanceElementDto(instanceId, elementPrior);
                compositionElementDto = AcElementDtoMapper.toCompositionElementDto(acDefinition,
                        elementPrior.getDefinition());

            }
            var acElementDto = buildAcElementDto(compositionElementDto, instanceElementDto, compositionElementTarget,
                    instanceElementTarget, element);

            var participantId = element.getParticipantId();
            participantDtoMap.computeIfAbsent(participantId, k -> new ArrayList<>()).add(acElementDto);
        }
        return buildParticipantDtoList(participantDtoMap);

    }


    /**
     * Create a List of ParticipantDto for Rollback.
     * @param acPriorUpdate AutomationComposition prior to update
     * @param acDefinition AutomationCompositionDefinition
     * @param automationComposition AutomationComposition target
     * @param acDefinitionTarget AutomationCompositionDefinition target
     * @return List of ParticipantDto
     */
    public List<ParticipantDto> createRollbackDto(AutomationCompositionRollback acPriorUpdate,
                                                   AutomationCompositionDefinition acDefinition,
                                                   AutomationComposition automationComposition,
                                                   AutomationCompositionDefinition acDefinitionTarget) {

        var instanceId = automationComposition.getInstanceId();
        var acElementList = automationComposition.getElements();
        InstanceElementDto instanceElementDto;
        CompositionElementDto compositionElementDto;
        InstanceElementDto instanceElementTarget;
        CompositionElementDto compositionElementTarget;
        Map<UUID, List<AcElementDto>> participantDtoMap = new HashMap<>();

        for (var element : acElementList.values()) {
            if (MigrationState.NEW.equals(element.getMigrationState())) {
                compositionElementTarget = new CompositionElementDto(automationComposition.getCompositionId(),
                        element.getDefinition(), Map.of(), Map.of(), ElementState.REMOVED);
                instanceElementTarget = new InstanceElementDto(automationComposition.getInstanceId(),
                        element.getId(), Map.of(), Map.of(), ElementState.REMOVED);
                instanceElementDto = AcElementDtoMapper.toInstanceElementDto(instanceId, element);
                compositionElementDto = AcElementDtoMapper.toCompositionElementDto(acDefinitionTarget,
                        element.getDefinition());

            } else if (MigrationState.REMOVED.equals(element.getMigrationState())) {
                compositionElementDto = new CompositionElementDto(automationComposition.getCompositionTargetId(),
                                element.getDefinition(), Map.of(), Map.of(), ElementState.NOT_PRESENT);
                instanceElementDto = new InstanceElementDto(instanceId, element.getId(), Map.of(), Map.of(),
                        ElementState.NOT_PRESENT);
                compositionElementTarget = AcElementDtoMapper.toCompositionElementDto(acDefinition,
                        element.getDefinition(),
                        ElementState.NEW);
                instanceElementTarget = AcElementDtoMapper.toInstanceElementDto(instanceId, element, ElementState.NEW);

            } else  { // default scenario
                compositionElementTarget = AcElementDtoMapper.toCompositionElementDto(acDefinition,
                        element.getDefinition());
                instanceElementTarget = AcElementDtoMapper.toInstanceElementDto(instanceId, element);
                var elementPrior = acPriorUpdate.getElements().get(element.getId());
                instanceElementDto = AcElementDtoMapper.toInstanceElementDto(instanceId, elementPrior);
                compositionElementDto = AcElementDtoMapper.toCompositionElementDto(acDefinitionTarget,
                        elementPrior.getDefinition());

            }
            var acElementDto = buildAcElementDto(compositionElementDto, instanceElementDto, compositionElementTarget,
                    instanceElementTarget, element);

            var participantId = element.getParticipantId();
            participantDtoMap.computeIfAbsent(participantId, k -> new ArrayList<>()).add(acElementDto);
        }
        return buildParticipantDtoList(participantDtoMap);
    }

    private List<ParticipantDto> buildParticipantDtoList(Map<UUID, List<AcElementDto>> participantDtoMap) {

        return participantDtoMap.entrySet().stream().map(entry -> {
            var participantDto = new ParticipantDto();
            participantDto.setParticipantId(entry.getKey());
            participantDto.setElementDtos(entry.getValue());
            return participantDto;
        }).toList();
    }

    private AcElementDto buildAcElementDto(
            CompositionElementDto compositionElementDto, InstanceElementDto instanceElementDto,
            CompositionElementDto compositionElementTarget, InstanceElementDto instanceElementTarget,
            AutomationCompositionElement element) {
        var acElementDto = new AcElementDto();
        acElementDto.setCompositionElement(compositionElementDto);
        acElementDto.setInstanceElement(instanceElementDto);
        acElementDto.setCompositionElementTarget(compositionElementTarget);
        acElementDto.setInstanceElementTarget(instanceElementTarget);
        acElementDto.setDeployState(element.getDeployState());
        acElementDto.setLockState(element.getLockState());
        acElementDto.setSubState(element.getSubState());
        acElementDto.setMigrationState(element.getMigrationState());
        acElementDto.setOperationalState(element.getOperationalState());
        acElementDto.setUseState(element.getUseState());

        return acElementDto;
    }

}
