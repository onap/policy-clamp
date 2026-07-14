/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.models.acm.persistence.provider;

import static org.onap.policy.clamp.models.acm.persistence.provider.CompositionSpecs.hasParticipantId;
import static org.springframework.data.jpa.domain.Specification.where;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.models.acm.dto.CompositionDto;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaAutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.persistence.repository.AutomationCompositionDefinitionRepository;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class CompositionDtoProvider {

    private final AutomationCompositionDefinitionRepository acmDefinitionRepository;

    /**
     * Get CompositionDto by compositionId  with elements filtered by participantId.
     *
     * @param participantId the participantId
     * @param compositionId the compositionId
     * @return the CompositionDto
     */
    public CompositionDto getCompositionDto(UUID participantId, UUID compositionId) {
        var jap = acmDefinitionRepository.getReferenceById(compositionId.toString());
        return toCompositionDto(jap, participantId);

    }

    private CompositionDto toCompositionDto(JpaAutomationCompositionDefinition jpa, UUID participantId) {
        Map<ToscaConceptIdentifier, Map<String, Object>> inPropertiesMap = new HashMap<>();
        Map<ToscaConceptIdentifier, Map<String, Object>> outPropertiesMap = new HashMap<>();
        var strParticipantId = participantId.toString();
        var nodeTemplates = jpa.getServiceTemplate().getToscaTopologyTemplate().getNodeTemplates();
        for (var element : jpa.getElements()) {
            if (strParticipantId.equals(element.getParticipantId())) {
                var key = new ToscaConceptIdentifier(element.getNodeTemplateName(), element.getNodeTemplateVersion());
                outPropertiesMap.put(key, element.getOutProperties());
                var keyStr = PfUtils.getId(key.asConceptKey());
                inPropertiesMap.put(key, nodeTemplates.get(keyStr).getProperties());
            }
        }
        return new CompositionDto(UUID.fromString(jpa.getCompositionId()), inPropertiesMap, outPropertiesMap);
    }

    /**
     * get a list of CompositionDto filtered by participantId.
     *
     * @param participantId the participantId
     * @param pageable the Pageable
     * @return a list of CompositionDto
     */
    public List<CompositionDto> getCompositionDtoList(UUID participantId, Pageable pageable) {
        var query = where(hasParticipantId(participantId));
        var page = acmDefinitionRepository.findAll(query, pageable);
        return page.stream().map(jpa -> toCompositionDto(jpa, participantId)).toList();
    }
}
