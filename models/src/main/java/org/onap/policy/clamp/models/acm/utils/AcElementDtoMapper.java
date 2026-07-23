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

import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.dto.CompositionElementDto;
import org.onap.policy.clamp.models.acm.dto.ElementState;
import org.onap.policy.clamp.models.acm.dto.InstanceElementDto;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Mapper interface that constructs the DTOs for participant use.
 */
@Mapper(componentModel = "spring")
public interface AcElementDtoMapper {

    @Mapping(target = "elementId", source = "element.id")
    @Mapping(target = "inProperties", source = "element.properties")
    @Mapping(target = "outProperties", source = "element.outProperties")
    @Mapping(target = "state", source = "state")
    InstanceElementDto toInstanceElementDto(UUID instanceId, AutomationCompositionElement element, ElementState state);

    default InstanceElementDto toInstanceElementDto(UUID instanceId, AutomationCompositionElement element) {
        return toInstanceElementDto(instanceId, element, ElementState.PRESENT);
    }

    /**
     * Construct compositionElementDto without mapstruct.
     * @param acDefinition AutomationCompositionDefinition
     * @param elementDefinitionId ToscaConceptIdentifier
     * @return CompositionElementDto
     */
    default CompositionElementDto toCompositionElementDto(AutomationCompositionDefinition acDefinition,
                                                          ToscaConceptIdentifier elementDefinitionId,
                                                          ElementState state) {
        var nodeTemplateState = acDefinition.getElementStateMap().get(elementDefinitionId.getName());
        var nodeTemplate = acDefinition.getServiceTemplate()
                .getToscaTopologyTemplate().getNodeTemplates().get(elementDefinitionId.getName());
        return new CompositionElementDto(acDefinition.getCompositionId(), elementDefinitionId,
                nodeTemplate.getProperties(), nodeTemplateState.getOutProperties(), state);
    }

    default CompositionElementDto toCompositionElementDto(AutomationCompositionDefinition acDefinition,
                                                          ToscaConceptIdentifier elementDefinitionId) {
        return toCompositionElementDto(acDefinition, elementDefinitionId, ElementState.PRESENT);
    }

}
