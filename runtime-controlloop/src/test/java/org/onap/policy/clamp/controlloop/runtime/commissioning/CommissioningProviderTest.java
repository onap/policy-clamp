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

package org.onap.policy.clamp.controlloop.runtime.commissioning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.controlloop.runtime.main.parameters.ClRuntimeParameterGroup;
import org.onap.policy.clamp.controlloop.runtime.util.CommonTestData;
import org.onap.policy.common.utils.coder.YamlJsonTranslator;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

class CommissioningProviderTest {
    private static final String TOSCA_SERVICE_TEMPLATE_YAML =
            "src/test/resources/rest/servicetemplates/pmsh_multiple_cl_tosca.yaml";
    private static final String TEMPLATE_IS_NULL = ".*serviceTemplate is marked non-null but is null";
    private static final YamlJsonTranslator yamlTranslator = new YamlJsonTranslator();

    /**
     * Test the fetching of control loop definitions (ToscaServiceTemplates).
     *
     * @throws Exception .
     */
    @Test
    void testGetControlLoopDefinitions() throws Exception {
        List<ToscaNodeTemplate> listOfTemplates;
        ClRuntimeParameterGroup clRuntimeParameterGroup = CommonTestData.geParameterGroup("getCLDefinitions");

        try (var provider = new CommissioningProvider(clRuntimeParameterGroup)) {
            ToscaServiceTemplate serviceTemplate = yamlTranslator.fromYaml(
                    ResourceUtils.getResourceAsString(TOSCA_SERVICE_TEMPLATE_YAML), ToscaServiceTemplate.class);

            listOfTemplates = provider.getControlLoopDefinitions(null, null);
            assertThat(listOfTemplates).isEmpty();

            provider.createControlLoopDefinitions(serviceTemplate);
            listOfTemplates = provider.getControlLoopDefinitions(null, null);
            assertThat(listOfTemplates).hasSize(2);

            // Test Filtering
            listOfTemplates =
                    provider.getControlLoopDefinitions("org.onap.domain.pmsh.PMSHControlLoopDefinition", "1.2.3");
            assertThat(listOfTemplates).hasSize(1);
            for (ToscaNodeTemplate template : listOfTemplates) {
                // Other CL elements contain PMSD instead of PMSH in their name
                assertThat(template.getName()).doesNotContain("PMSD");
            }

            // Test Wrong Name
            listOfTemplates = provider.getControlLoopDefinitions("WrongControlLoopName", "0.0.0");
            assertThat(listOfTemplates).isEmpty();
        }
    }

    /**
     * Test the creation of control loop definitions (ToscaServiceTemplates).
     *
     * @throws Exception .
     */
    @Test
    void testCreateControlLoopDefinitions() throws Exception {
        List<ToscaNodeTemplate> listOfTemplates;
        ClRuntimeParameterGroup clRuntimeParameterGroup = CommonTestData.geParameterGroup("createCLDefinitions");

        try (var provider = new CommissioningProvider(clRuntimeParameterGroup)) {
            // Test Service template is null
            assertThatThrownBy(() -> provider.createControlLoopDefinitions(null)).hasMessageMatching(TEMPLATE_IS_NULL);
            listOfTemplates = provider.getControlLoopDefinitions(null, null);
            assertThat(listOfTemplates).isEmpty();

            ToscaServiceTemplate serviceTemplate = yamlTranslator.fromYaml(
                    ResourceUtils.getResourceAsString(TOSCA_SERVICE_TEMPLATE_YAML), ToscaServiceTemplate.class);

            // Response should return the number of node templates present in the service template
            List<ToscaConceptIdentifier> affectedDefinitions =
                    provider.createControlLoopDefinitions(serviceTemplate).getAffectedControlLoopDefinitions();
            assertThat(affectedDefinitions).hasSize(13);
            listOfTemplates = provider.getControlLoopDefinitions(null, null);
            assertThat(listOfTemplates).hasSize(2);
        }
    }

    /**
     * Test the deletion of control loop definitions (ToscaServiceTemplate).
     *
     * @throws Exception .
     */
    @Test
    void testDeleteControlLoopDefinitions() throws Exception {
        List<ToscaNodeTemplate> listOfTemplates;
        ClRuntimeParameterGroup clRuntimeParameterGroup = CommonTestData.geParameterGroup("deleteCLDefinitions");

        try (var provider = new CommissioningProvider(clRuntimeParameterGroup)) {
            ToscaServiceTemplate serviceTemplate = yamlTranslator.fromYaml(
                    ResourceUtils.getResourceAsString(TOSCA_SERVICE_TEMPLATE_YAML), ToscaServiceTemplate.class);

            listOfTemplates = provider.getControlLoopDefinitions(null, null);
            assertThat(listOfTemplates).isEmpty();

            provider.createControlLoopDefinitions(serviceTemplate);
            listOfTemplates = provider.getControlLoopDefinitions(null, null);
            assertThat(listOfTemplates).hasSize(2);

            provider.deleteControlLoopDefinition(serviceTemplate.getName(), serviceTemplate.getVersion());
            listOfTemplates = provider.getControlLoopDefinitions(null, null);
            assertThat(listOfTemplates).isEmpty();
        }
    }

    /**
     * Test the fetching of control loop element definitions.
     *
     * @throws Exception .
     */
    @Test
    void testGetControlLoopElementDefinitions() throws Exception {
        ClRuntimeParameterGroup clRuntimeParameterGroup = CommonTestData.geParameterGroup("getCLElDefinitions");
        try (var provider = new CommissioningProvider(clRuntimeParameterGroup)) {
            ToscaServiceTemplate serviceTemplate = yamlTranslator.fromYaml(
                    ResourceUtils.getResourceAsString(TOSCA_SERVICE_TEMPLATE_YAML), ToscaServiceTemplate.class);

            provider.getControlLoopDefinitions(null, null);

            provider.createControlLoopDefinitions(serviceTemplate);
            List<ToscaNodeTemplate> controlLoopDefinitionList =
                    provider.getControlLoopDefinitions("org.onap.domain.pmsh.PMSHControlLoopDefinition", "1.2.3");

            List<ToscaNodeTemplate> controlLoopElementNodeTemplates =
                    provider.getControlLoopElementDefinitions(controlLoopDefinitionList.get(0));

            // 4 PMSH control loop elements definitions.
            assertThat(controlLoopElementNodeTemplates).hasSize(4);

            List<ToscaNodeType> derivedTypes = getDerivedNodeTypes(serviceTemplate);
            for (ToscaNodeTemplate template : controlLoopElementNodeTemplates) {
                assertTrue(checkNodeType(template, derivedTypes));
            }
        }
    }

    private boolean checkNodeType(ToscaNodeTemplate template, List<ToscaNodeType> derivedNodeTypes) {
        String controlLoopElementType = "org.onap.policy.clamp.controlloop.ControlLoopElement";
        for (ToscaNodeType derivedType : derivedNodeTypes) {
            if (template.getType().equals(derivedType.getName()) || template.getType().equals(controlLoopElementType)) {
                return true;
            }
        }
        return false;
    }

    private List<ToscaNodeType> getDerivedNodeTypes(ToscaServiceTemplate serviceTemplate) {
        String type = "org.onap.policy.clamp.controlloop.ControlLoopElement";
        List<ToscaNodeType> nodeTypes = new ArrayList<>();
        for (ToscaNodeType nodeType : serviceTemplate.getNodeTypes().values()) {
            if (nodeType.getDerivedFrom().equals(type)) {
                nodeTypes.add(nodeType);
            }
        }
        return nodeTypes;
    }
}

