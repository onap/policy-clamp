/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.repository.ToscaServiceTemplateRepository;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardYamlCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.simple.concepts.JpaToscaServiceTemplate;

class ServiceTemplateProviderTest {

    private static final String TOSCA_SERVICE_TEMPLATE_YAML =
            "src/test/resources/providers/tosca-for-smoke-testing.yaml";

    private static final StandardYamlCoder YAML_TRANSLATOR = new StandardYamlCoder();

    private static ToscaServiceTemplate inputServiceTemplate;

    @BeforeAll
    static void loadServiceTemplate() {
        inputServiceTemplate = getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
    }

    @Test
    void testGetCommonOrInstancePropertiesFromNodeTypes() throws PfModelException {
        var serviceTemplateRepository = mock(ToscaServiceTemplateRepository.class);
        var serviceTemplateProvider = new ServiceTemplateProvider(serviceTemplateRepository);

        var result = serviceTemplateProvider.getCommonOrInstancePropertiesFromNodeTypes(true, inputServiceTemplate);
        assertNotNull(result);
        assertThat(result).hasSize(8);
    }

    @Test
    void testGetDerivedCommonOrInstanceNodeTemplates() throws PfModelException {
        var serviceTemplateRepository = mock(ToscaServiceTemplateRepository.class);
        var serviceTemplateProvider = new ServiceTemplateProvider(serviceTemplateRepository);

        var commonOrInstanceNodeTypeProps =
                serviceTemplateProvider.getCommonOrInstancePropertiesFromNodeTypes(true, inputServiceTemplate);

        var result = serviceTemplateProvider.getDerivedCommonOrInstanceNodeTemplates(
                inputServiceTemplate.getToscaTopologyTemplate().getNodeTemplates(), commonOrInstanceNodeTypeProps);

        assertNotNull(result);
        assertThat(result).hasSize(8);
    }

    @Test
    void testCreateServiceTemplate() throws PfModelException {
        var serviceTemplateRepository = mock(ToscaServiceTemplateRepository.class);
        var serviceTemplateProvider = new ServiceTemplateProvider(serviceTemplateRepository);

        var jpaServiceTemplate = ProviderUtils.getJpaAndValidate(inputServiceTemplate, JpaToscaServiceTemplate::new,
                "toscaServiceTemplate");
        when(serviceTemplateRepository.save(jpaServiceTemplate)).thenReturn(jpaServiceTemplate);

        var result = serviceTemplateProvider.createServiceTemplate(inputServiceTemplate);

        assertThat(result).isEqualTo(jpaServiceTemplate.toAuthorative());
    }

    @Test
    void testDeleteServiceTemplate() throws PfModelException {
        var jpaServiceTemplate = ProviderUtils.getJpaAndValidate(inputServiceTemplate, JpaToscaServiceTemplate::new,
                "toscaServiceTemplate");
        var serviceTemplateRepository = mock(ToscaServiceTemplateRepository.class);
        when(serviceTemplateRepository
                .findById(new PfConceptKey(inputServiceTemplate.getName(), inputServiceTemplate.getVersion())))
                        .thenReturn(Optional.of(jpaServiceTemplate));

        var serviceTemplateProvider = new ServiceTemplateProvider(serviceTemplateRepository);
        var result = serviceTemplateProvider.deleteServiceTemplate(inputServiceTemplate.getName(),
                inputServiceTemplate.getVersion());

        assertThat(result).isEqualTo(jpaServiceTemplate.toAuthorative());
    }

    @Test
    void testGetServiceTemplateListEmpty() throws PfModelException {
        var serviceTemplateRepository = mock(ToscaServiceTemplateRepository.class);
        when(serviceTemplateRepository.findById(any())).thenReturn(Optional.empty());

        var serviceTemplateProvider = new ServiceTemplateProvider(serviceTemplateRepository);
        assertThatThrownBy(() -> serviceTemplateProvider.getToscaServiceTemplate("Name", "1.0.0"))
                .hasMessage("Control Loop definitions not found");
    }

    @Test
    void testGetServiceTemplateList() throws PfModelException {
        var jpaServiceTemplate = ProviderUtils.getJpaAndValidate(inputServiceTemplate, JpaToscaServiceTemplate::new,
                "toscaServiceTemplate");
        var serviceTemplateRepository = mock(ToscaServiceTemplateRepository.class);
        when(serviceTemplateRepository
                .findById(new PfConceptKey(inputServiceTemplate.getName(), inputServiceTemplate.getVersion())))
                        .thenReturn(Optional.of(jpaServiceTemplate));

        var serviceTemplateProvider = new ServiceTemplateProvider(serviceTemplateRepository);
        var result = serviceTemplateProvider.getToscaServiceTemplate(inputServiceTemplate.getName(),
                inputServiceTemplate.getVersion());

        assertThat(result).isEqualTo(jpaServiceTemplate.toAuthorative());
    }

    @Test
    void testGetServiceTemplate() throws PfModelException {
        var jpaServiceTemplate = ProviderUtils.getJpaAndValidate(inputServiceTemplate, JpaToscaServiceTemplate::new,
                "toscaServiceTemplate");
        var serviceTemplateRepository = mock(ToscaServiceTemplateRepository.class);
        when(serviceTemplateRepository.getFiltered(JpaToscaServiceTemplate.class, inputServiceTemplate.getName(),
                inputServiceTemplate.getVersion())).thenReturn(List.of(jpaServiceTemplate));

        var serviceTemplateProvider = new ServiceTemplateProvider(serviceTemplateRepository);
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
