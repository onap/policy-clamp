/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation.
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.document.concepts.DocToscaServiceTemplate;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaAutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.persistence.repository.AutomationCompositionDefinitionRepository;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardYamlCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.springframework.data.domain.Example;

class AcDefinitionProviderTest {

    private static final String TOSCA_SERVICE_TEMPLATE_YAML = "clamp/acm/pmsh/funtional-pmsh-usecase.yaml";
    private static final String TOSCA_SERVICE_TEMPLATE_YAML_PROP =
            "clamp/acm/test/tosca-template-additional-properties.yaml";

    private static final StandardYamlCoder YAML_TRANSLATOR = new StandardYamlCoder();

    private static ToscaServiceTemplate inputServiceTemplate;

    @BeforeAll
    static void loadServiceTemplate() {
        inputServiceTemplate = getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
    }

    @Test
    void testDocCopyCompare() {

        var inputServiceTemplateProperties = getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML_PROP);
        var docServiceTemplate = new DocToscaServiceTemplate(inputServiceTemplateProperties);
        var docServiceTemplateCopy = new DocToscaServiceTemplate(docServiceTemplate);

        assertNotEquals(0, docServiceTemplate.compareTo(docServiceTemplateCopy));
        assertThat(docServiceTemplate.compareToWithoutEntities(docServiceTemplateCopy)).isZero();

        var acmDefinition = getAcDefinition(docServiceTemplate);
        var acmDefinitionCopy = getAcDefinition(docServiceTemplateCopy);

        assertThat(acmDefinition.getServiceTemplate().getName()).isEqualTo(
                acmDefinitionCopy.getServiceTemplate().getName());

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

    private AutomationCompositionDefinition getAcDefinition(DocToscaServiceTemplate docServiceTemplate) {
        var acmDefinition = new AutomationCompositionDefinition();
        acmDefinition.setCompositionId(UUID.randomUUID());
        acmDefinition.setServiceTemplate(docServiceTemplate.toAuthorative());
        return acmDefinition;
    }

    /**
     * Get ToscaServiceTemplate from resource.
     *
     * @param path path of the resource
     */
    public static ToscaServiceTemplate getToscaServiceTemplate(String path) {

        try {
            return YAML_TRANSLATOR.decode(ResourceUtils.getResourceAsStream(path), ToscaServiceTemplate.class);
        } catch (CoderException e) {
            fail("Cannot read or decode " + path);
            return null;
        }
    }
}
