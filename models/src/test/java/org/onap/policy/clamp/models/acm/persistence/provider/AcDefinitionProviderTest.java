/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation.
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.NodeTemplateState;
import org.onap.policy.clamp.models.acm.document.concepts.DocToscaServiceTemplate;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaAutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.persistence.repository.AutomationCompositionDefinitionRepository;
import org.onap.policy.clamp.models.acm.utils.CommonTestData;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.springframework.data.domain.Example;

class AcDefinitionProviderTest {

    private static final String TOSCA_SERVICE_TEMPLATE_YAML = "clamp/acm/pmsh/funtional-pmsh-usecase.yaml";
    private static final String TOSCA_SERVICE_TEMPLATE_YAML_PROP =
            "clamp/acm/test/tosca-template-additional-properties.yaml";

    private static ToscaServiceTemplate inputServiceTemplate;

    @BeforeAll
    static void loadServiceTemplate() {
        inputServiceTemplate = CommonTestData.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
    }

    @Test
    void testDocCopyCompare() {

        var inputServiceTemplateProperties = CommonTestData.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML_PROP);
        var docServiceTemplate = new DocToscaServiceTemplate(inputServiceTemplateProperties);
        var docServiceTemplateCopy = new DocToscaServiceTemplate(docServiceTemplate);

        assertThat(docServiceTemplate.compareTo(docServiceTemplateCopy)).isEqualByComparingTo(0);
        assertThat(docServiceTemplate.compareToWithoutEntities(docServiceTemplateCopy)).isZero();

        var acmDefinition = getAcDefinition(docServiceTemplate);
        var acmDefinitionCopy = getAcDefinition(docServiceTemplateCopy);

        assertThat(acmDefinition.getServiceTemplate().getName())
                .isEqualTo(acmDefinitionCopy.getServiceTemplate().getName());

    }

    @Test
    void testCreateServiceTemplate() {
        var docServiceTemplate = new DocToscaServiceTemplate(inputServiceTemplate);
        var acmDefinition = getAcDefinition(docServiceTemplate);

        var acmDefinitionRepository = mock(AutomationCompositionDefinitionRepository.class);
        when(acmDefinitionRepository.save(any(JpaAutomationCompositionDefinition.class)))
                .thenReturn(new JpaAutomationCompositionDefinition(acmDefinition));

        var acDefinitionProvider = new AcDefinitionProvider(acmDefinitionRepository);
        var result = acDefinitionProvider.createAutomationCompositionDefinition(inputServiceTemplate);

        assertThat(result.getServiceTemplate()).isEqualTo(docServiceTemplate.toAuthorative());
        assertThat(result.getServiceTemplate().getMetadata() != null);
    }

    @Test
    void testCreateServiceTemplateWithMetadata() {
        var docServiceTemplate = new DocToscaServiceTemplate(inputServiceTemplate);
        var acmDefinition = getAcDefinition(docServiceTemplate);

        var acmDefinitionRepository = mock(AutomationCompositionDefinitionRepository.class);
        when(acmDefinitionRepository.save(any(JpaAutomationCompositionDefinition.class)))
            .thenReturn(new JpaAutomationCompositionDefinition(acmDefinition));

        var acDefinitionProvider = new AcDefinitionProvider(acmDefinitionRepository);
        inputServiceTemplate.setMetadata(new HashMap<>());
        var result = acDefinitionProvider.createAutomationCompositionDefinition(inputServiceTemplate);

        assertThat(result.getServiceTemplate()).isEqualTo(docServiceTemplate.toAuthorative());
        assertThat(result.getServiceTemplate().getMetadata() != null);
    }

    @Test
    void testUpdateServiceTemplate() {
        var acmDefinitionRepository = mock(AutomationCompositionDefinitionRepository.class);
        var acDefinitionProvider = new AcDefinitionProvider(acmDefinitionRepository);
        acDefinitionProvider.updateServiceTemplate(UUID.randomUUID(), inputServiceTemplate);
        verify(acmDefinitionRepository).save(any(JpaAutomationCompositionDefinition.class));
    }

    @Test
    void testUpdateAcDefinition() {
        var acmDefinitionRepository = mock(AutomationCompositionDefinitionRepository.class);
        var acDefinitionProvider = new AcDefinitionProvider(acmDefinitionRepository);
        var acmDefinition = getAcDefinition(new DocToscaServiceTemplate(inputServiceTemplate));
        acDefinitionProvider.updateAcDefinition(acmDefinition);
        verify(acmDefinitionRepository).save(any(JpaAutomationCompositionDefinition.class));
    }

    @Test
    void testGetAcDefinition() {
        var jpa = new JpaAutomationCompositionDefinition();
        jpa.fromAuthorative(getAcDefinition(new DocToscaServiceTemplate(inputServiceTemplate)));
        var acmDefinitionRepository = mock(AutomationCompositionDefinitionRepository.class);
        when(acmDefinitionRepository.findById(jpa.getCompositionId())).thenReturn(Optional.of(jpa));
        var acDefinitionProvider = new AcDefinitionProvider(acmDefinitionRepository);
        var result = acDefinitionProvider.getAcDefinition(UUID.fromString(jpa.getCompositionId()));
        assertThat(result).isNotNull();
    }

    @Test
    void testGetAcDefinitionNotFound() {
        var acmDefinitionRepository = mock(AutomationCompositionDefinitionRepository.class);
        var acDefinitionProvider = new AcDefinitionProvider(acmDefinitionRepository);
        var compositionId = UUID.randomUUID();
        assertThatThrownBy(() -> acDefinitionProvider.getAcDefinition(compositionId))
                .hasMessage("Get serviceTemplate \"" + compositionId + "\" failed, serviceTemplate does not exist");
    }

    @Test
    void testFindAcDefinition() {
        var jpa = new JpaAutomationCompositionDefinition();
        jpa.fromAuthorative(getAcDefinition(new DocToscaServiceTemplate(inputServiceTemplate)));
        var acmDefinitionRepository = mock(AutomationCompositionDefinitionRepository.class);
        when(acmDefinitionRepository.findById(jpa.getCompositionId())).thenReturn(Optional.of(jpa));
        var acDefinitionProvider = new AcDefinitionProvider(acmDefinitionRepository);
        var result = acDefinitionProvider.findAcDefinition(UUID.fromString(jpa.getCompositionId()));
        assertThat(result).isNotNull();
    }

    @Test
    void testGetAllAcDefinitions() {
        var jpa = new JpaAutomationCompositionDefinition();
        jpa.fromAuthorative(getAcDefinition(new DocToscaServiceTemplate(inputServiceTemplate)));
        var acmDefinitionRepository = mock(AutomationCompositionDefinitionRepository.class);
        when(acmDefinitionRepository.findAll()).thenReturn(List.of(jpa));
        var acDefinitionProvider = new AcDefinitionProvider(acmDefinitionRepository);
        var result = acDefinitionProvider.getAllAcDefinitions();
        assertThat(result).hasSize(1);
    }

    @Test
    void testDeleteAcDefintion() {
        var docServiceTemplate = new DocToscaServiceTemplate(inputServiceTemplate);
        var acmDefinition = getAcDefinition(docServiceTemplate);

        var acmDefinitionRepository = mock(AutomationCompositionDefinitionRepository.class);
        when(acmDefinitionRepository.findById(acmDefinition.getCompositionId().toString()))
                .thenReturn(Optional.of(new JpaAutomationCompositionDefinition(acmDefinition)));

        var acDefinitionProvider = new AcDefinitionProvider(acmDefinitionRepository);
        var result = acDefinitionProvider.deleteAcDefintion(acmDefinition.getCompositionId());

        assertThat(result).isEqualTo(docServiceTemplate.toAuthorative());
    }

    @Test
    void testDeleteServiceTemplateEmpty() {
        var compositionId = UUID.randomUUID();
        var acmDefinitionRepository = mock(AutomationCompositionDefinitionRepository.class);
        var acDefinitionProvider = new AcDefinitionProvider(acmDefinitionRepository);
        assertThatThrownBy(() -> acDefinitionProvider.deleteAcDefintion(compositionId))
                .hasMessage("delete of Automation Composition Definition \"" + compositionId
                        + "\" failed, Automation Composition Definition does not exist");
    }

    @Test
    void testGetServiceTemplate() {
        var docServiceTemplate = new DocToscaServiceTemplate(inputServiceTemplate);
        var acmDefinition = getAcDefinition(docServiceTemplate);
        var acmDefinitionRepository = mock(AutomationCompositionDefinitionRepository.class);
        when(acmDefinitionRepository.findAll(Mockito.<Example<JpaAutomationCompositionDefinition>>any()))
                .thenReturn(List.of(new JpaAutomationCompositionDefinition(acmDefinition)));

        var acDefinitionProvider = new AcDefinitionProvider(acmDefinitionRepository);
        var result = acDefinitionProvider.getServiceTemplateList(inputServiceTemplate.getName(),
                inputServiceTemplate.getVersion());

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(acmDefinition.getServiceTemplate());
    }

    @Test
    void testGetServiceTemplateNulls() {
        var docServiceTemplate = new DocToscaServiceTemplate(inputServiceTemplate);
        var acmDefinition = getAcDefinition(docServiceTemplate);
        var acmDefinitionRepository = mock(AutomationCompositionDefinitionRepository.class);
        when(acmDefinitionRepository.findAll(Mockito.<Example<JpaAutomationCompositionDefinition>>any()))
            .thenReturn(List.of(new JpaAutomationCompositionDefinition(acmDefinition)));

        var acDefinitionProvider = new AcDefinitionProvider(acmDefinitionRepository);
        var result = acDefinitionProvider.getServiceTemplateList(null,
            inputServiceTemplate.getVersion());

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(acmDefinition.getServiceTemplate());

        result = acDefinitionProvider.getServiceTemplateList(inputServiceTemplate.getName(),
            null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(acmDefinition.getServiceTemplate());

        result = acDefinitionProvider.getServiceTemplateList(null,
            null);

        assertThat(result).hasSize(0);
        assertThat(result.isEmpty());
    }

    private AutomationCompositionDefinition getAcDefinition(DocToscaServiceTemplate docServiceTemplate) {
        var acmDefinition = new AutomationCompositionDefinition();
        acmDefinition.setCompositionId(UUID.randomUUID());
        acmDefinition.setState(AcTypeState.COMMISSIONED);
        acmDefinition.setServiceTemplate(docServiceTemplate.toAuthorative());
        var nodeTemplateState = new NodeTemplateState();
        nodeTemplateState.setNodeTemplateStateId(UUID.randomUUID());
        nodeTemplateState.setNodeTemplateId(new ToscaConceptIdentifier("name", "1.0.0"));
        nodeTemplateState.setState(AcTypeState.COMMISSIONED);
        acmDefinition.setElementStateMap(Map.of(nodeTemplateState.getNodeTemplateId().getName(), nodeTemplateState));
        return acmDefinition;
    }
}
