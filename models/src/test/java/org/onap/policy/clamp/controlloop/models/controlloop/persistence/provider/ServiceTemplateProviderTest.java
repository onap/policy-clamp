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
import org.junit.jupiter.api.Test;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardYamlCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

class ServiceTemplateProviderTest {

    private static final String TOSCA_SERVICE_TEMPLATE_YAML =
            "src/test/resources/providers/tosca-for-smoke-testing.yaml";

    private static final StandardYamlCoder YAML_TRANSLATOR = new StandardYamlCoder();

    @Test
    void testGetCommonOrInstancePropertiesFromNodeTypes() throws PfModelException {
        var serviceTemplateProvider = new ServiceTemplateProvider(mock(PolicyModelsProvider.class));

        var serviceTemplate = getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);

        var result = serviceTemplateProvider.getCommonOrInstancePropertiesFromNodeTypes(true, serviceTemplate);
        assertNotNull(result);
        assertThat(result).hasSize(8);
    }

    @Test
    void testGetDerivedCommonOrInstanceNodeTemplates() throws PfModelException {
        var serviceTemplateProvider = new ServiceTemplateProvider(mock(PolicyModelsProvider.class));

        var serviceTemplate = getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);

        var commonOrInstanceNodeTypeProps =
                serviceTemplateProvider.getCommonOrInstancePropertiesFromNodeTypes(true, serviceTemplate);

        var result = serviceTemplateProvider.getDerivedCommonOrInstanceNodeTemplates(
                serviceTemplate.getToscaTopologyTemplate().getNodeTemplates(), commonOrInstanceNodeTypeProps);

        assertNotNull(result);
        assertThat(result).hasSize(8);
    }

    @Test
    void testCreateServiceTemplate() throws PfModelException {
        var modelsProvider = mock(PolicyModelsProvider.class);
        var serviceTemplateProvider = new ServiceTemplateProvider(modelsProvider);

        var serviceTemplate = new ToscaServiceTemplate();
        when(modelsProvider.createServiceTemplate(serviceTemplate)).thenReturn(serviceTemplate);

        var result = serviceTemplateProvider.createServiceTemplate(serviceTemplate);

        assertThat(result).isEqualTo(serviceTemplate);
    }

    @Test
    void testDeleteServiceTemplate() throws PfModelException {
        var serviceTemplate = new ToscaServiceTemplate();
        serviceTemplate.setName("Name");
        serviceTemplate.setVersion("1.0.0");
        var modelsProvider = mock(PolicyModelsProvider.class);
        when(modelsProvider.deleteServiceTemplate(serviceTemplate.getName(), serviceTemplate.getVersion()))
                .thenReturn(serviceTemplate);

        var serviceTemplateProvider = new ServiceTemplateProvider(modelsProvider);
        var result =
                serviceTemplateProvider.deleteServiceTemplate(serviceTemplate.getName(), serviceTemplate.getVersion());

        assertThat(result).isEqualTo(serviceTemplate);
    }

    @Test
    void testGetServiceTemplateListEmpty() throws PfModelException {
        var modelsProvider = mock(PolicyModelsProvider.class);
        when(modelsProvider.getServiceTemplateList(any(String.class), any(String.class))).thenReturn(List.of());

        var serviceTemplateProvider = new ServiceTemplateProvider(modelsProvider);
        assertThatThrownBy(() -> serviceTemplateProvider.getToscaServiceTemplate("Name", "1.0.0"))
                .hasMessage("Control Loop definitions not found");
    }

    @Test
    void testGetServiceTemplateList() throws PfModelException {
        var serviceTemplate = new ToscaServiceTemplate();
        serviceTemplate.setName("Name");
        serviceTemplate.setVersion("1.0.0");
        var modelsProvider = mock(PolicyModelsProvider.class);
        when(modelsProvider.getServiceTemplateList(serviceTemplate.getName(), serviceTemplate.getVersion()))
                .thenReturn(List.of(serviceTemplate));

        var serviceTemplateProvider = new ServiceTemplateProvider(modelsProvider);
        var result = serviceTemplateProvider.getToscaServiceTemplate(serviceTemplate.getName(),
                serviceTemplate.getVersion());

        assertThat(result).isEqualTo(serviceTemplate);
    }

    @Test
    void testGetServiceTemplate() throws PfModelException {
        var serviceTemplate = new ToscaServiceTemplate();
        serviceTemplate.setName("Name");
        serviceTemplate.setVersion("1.0.0");
        var modelsProvider = mock(PolicyModelsProvider.class);
        when(modelsProvider.getServiceTemplateList(serviceTemplate.getName(), serviceTemplate.getVersion()))
                .thenReturn(List.of(serviceTemplate));

        var serviceTemplateProvider = new ServiceTemplateProvider(modelsProvider);
        var result =
                serviceTemplateProvider.getServiceTemplateList(serviceTemplate.getName(), serviceTemplate.getVersion());

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(serviceTemplate);
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
