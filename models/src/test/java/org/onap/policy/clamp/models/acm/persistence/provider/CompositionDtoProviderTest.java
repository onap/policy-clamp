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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.document.concepts.DocToscaServiceTemplate;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaAutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaNodeTemplateState;
import org.onap.policy.clamp.models.acm.persistence.repository.AutomationCompositionDefinitionRepository;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.clamp.models.acm.utils.CommonTestData;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

class CompositionDtoProviderTest {

    private static final String TOSCA_TEMPLATE_YAML = "clamp/acm/pmsh/funtional-pmsh-usecase.yaml";

    public static final String AUTOMATION_COMPOSITION_ELEMENT =
            "org.onap.policy.clamp.acm.AutomationCompositionElement";

    @Test
    void testGetCompositionDto() {
        var acmDefinitionRepository = mock(AutomationCompositionDefinitionRepository.class);
        var compositionId = UUID.randomUUID();
        var jpa = createJpaAutomationCompositionDefinition(compositionId);
        when(acmDefinitionRepository.getReferenceById(jpa.getCompositionId())).thenReturn(jpa);
        var compositionDtoProvider = new CompositionDtoProvider(acmDefinitionRepository);
        var result = compositionDtoProvider.getCompositionDto(CommonTestData.getParticipantId(), compositionId);
        assertEquals(compositionId, result.compositionId());
        assertThat(result.inPropertiesMap()).hasSize(3);
        assertThat(result.outPropertiesMap()).hasSize(3);
    }

    private JpaAutomationCompositionDefinition createJpaAutomationCompositionDefinition(UUID compositionId) {
        var jpa = new JpaAutomationCompositionDefinition();
        jpa.setCompositionId(compositionId.toString());
        var serviceTemplate = CommonTestData.getToscaServiceTemplate(TOSCA_TEMPLATE_YAML);
        var acElements =
                AcmUtils.extractAcElementsFromServiceTemplate(serviceTemplate, AUTOMATION_COMPOSITION_ELEMENT);
        var nodeTemplateStateMap = AcmUtils.createElementStateMap(acElements, AcTypeState.PRIMED);
        for (var element : nodeTemplateStateMap.values()) {
            var nodeTemplateStateId = element.getNodeTemplateStateId().toString();
            var jpaNodeTemplateState = new JpaNodeTemplateState(nodeTemplateStateId, jpa.getCompositionId());
            jpaNodeTemplateState.fromAuthorative(element);
            jpaNodeTemplateState.setParticipantId(CommonTestData.getParticipantId().toString());
            jpa.getElements().add(jpaNodeTemplateState);
        }
        jpa.setServiceTemplate(new DocToscaServiceTemplate(serviceTemplate));
        return jpa;
    }

    @Test
    void testGetCompositionDtoList() {
        var acmDefinitionRepository = mock(AutomationCompositionDefinitionRepository.class);
        var compositionId = UUID.randomUUID();
        var jpa = createJpaAutomationCompositionDefinition(compositionId);
        when(acmDefinitionRepository
                .findAll(Mockito.<Specification<JpaAutomationCompositionDefinition>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(jpa)));
        var compositionDtoProvider = new CompositionDtoProvider(acmDefinitionRepository);
        var result = compositionDtoProvider
                .getCompositionDtoList(CommonTestData.getParticipantId(), Pageable.unpaged());
        assertThat(result).hasSize(1);
        assertEquals(compositionId, result.getFirst().compositionId());
    }
}
