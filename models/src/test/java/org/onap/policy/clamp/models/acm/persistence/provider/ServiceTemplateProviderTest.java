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
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaAutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.persistence.repository.AutomationCompositionDefinitionRepository;
import org.onap.policy.clamp.models.acm.persistence.repository.ToscaServiceTemplateRepository;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardYamlCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.simple.concepts.JpaToscaServiceTemplate;

class ServiceTemplateProviderTest {

    private static final String TOSCA_SERVICE_TEMPLATE_YAML = "clamp/acm/pmsh/funtional-pmsh-usecase.yaml";

    private static final StandardYamlCoder YAML_TRANSLATOR = new StandardYamlCoder();

    private static ToscaServiceTemplate inputServiceTemplate;

    @BeforeAll
    static void loadServiceTemplate() {
        inputServiceTemplate = getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
    }

    @Test
    void testCreateServiceTemplate() throws PfModelException {
        var acmDefinitionRepository = mock(AutomationCompositionDefinitionRepository.class);

        var jpaServiceTemplate = ProviderUtils.getJpaAndValidate(inputServiceTemplate, JpaToscaServiceTemplate::new,
                "toscaServiceTemplate");
        var acmDefinition = new AutomationCompositionDefinition();
        acmDefinition.setCompositionId(UUID.randomUUID());
        acmDefinition.setServiceTemplate(jpaServiceTemplate.toAuthorative());

        when(acmDefinitionRepository.save(any(JpaAutomationCompositionDefinition.class)))
                .thenReturn(new JpaAutomationCompositionDefinition(acmDefinition));

        var serviceTemplateRepository = mock(ToscaServiceTemplateRepository.class);
        var serviceTemplateProvider = new ServiceTemplateProvider(serviceTemplateRepository, acmDefinitionRepository);
        var result = serviceTemplateProvider.createAutomationCompositionDefinition(inputServiceTemplate);

        assertThat(result.getServiceTemplate()).isEqualTo(jpaServiceTemplate.toAuthorative());
    }

    @Test
    void testDeleteServiceTemplate() throws PfModelException {
        var jpaServiceTemplate = ProviderUtils.getJpaAndValidate(inputServiceTemplate, JpaToscaServiceTemplate::new,
                "toscaServiceTemplate");
        var acmDefinition = new AutomationCompositionDefinition();
        acmDefinition.setCompositionId(UUID.randomUUID());
        acmDefinition.setServiceTemplate(jpaServiceTemplate.toAuthorative());

        var serviceTemplateRepository = mock(ToscaServiceTemplateRepository.class);
        var acmDefinitionRepository = mock(AutomationCompositionDefinitionRepository.class);
        when(acmDefinitionRepository.findById(acmDefinition.getCompositionId().toString()))
                .thenReturn(Optional.of(new JpaAutomationCompositionDefinition(acmDefinition)));

        var serviceTemplateProvider = new ServiceTemplateProvider(serviceTemplateRepository, acmDefinitionRepository);
        var result = serviceTemplateProvider.deleteServiceTemplate(acmDefinition.getCompositionId());

        assertThat(result).isEqualTo(jpaServiceTemplate.toAuthorative());
    }

    @Test
    void testDeleteServiceTemplateEmpty() throws PfModelException {
        var serviceTemplateRepository = mock(ToscaServiceTemplateRepository.class);
        when(serviceTemplateRepository.findAll()).thenReturn(List.of());

        var compositionId = UUID.randomUUID();
        var acmDefinitionRepository = mock(AutomationCompositionDefinitionRepository.class);
        var serviceTemplateProvider = new ServiceTemplateProvider(serviceTemplateRepository, acmDefinitionRepository);
        assertThatThrownBy(() -> serviceTemplateProvider.deleteServiceTemplate(compositionId)).hasMessage(
                "delete of serviceTemplate \"" + compositionId + "\" failed, serviceTemplate does not exist");
    }

    @Test
    void testGetServiceTemplate() throws PfModelException {
        var jpaServiceTemplate = ProviderUtils.getJpaAndValidate(inputServiceTemplate, JpaToscaServiceTemplate::new,
                "toscaServiceTemplate");
        var serviceTemplateRepository = mock(ToscaServiceTemplateRepository.class);
        when(serviceTemplateRepository.getFiltered(JpaToscaServiceTemplate.class, inputServiceTemplate.getName(),
                inputServiceTemplate.getVersion())).thenReturn(List.of(jpaServiceTemplate));

        var acmDefinitionRepository = mock(AutomationCompositionDefinitionRepository.class);
        var serviceTemplateProvider = new ServiceTemplateProvider(serviceTemplateRepository, acmDefinitionRepository);
        var result = serviceTemplateProvider.getServiceTemplateList(inputServiceTemplate.getName(),
                inputServiceTemplate.getVersion());

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(jpaServiceTemplate.toAuthorative());
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
