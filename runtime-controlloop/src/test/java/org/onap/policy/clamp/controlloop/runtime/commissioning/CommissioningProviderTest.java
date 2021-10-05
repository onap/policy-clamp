/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
 *  Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ControlLoopProvider;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.controlloop.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.controlloop.runtime.main.parameters.ClRuntimeParameterGroup;
import org.onap.policy.clamp.controlloop.runtime.util.CommonTestData;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaCapabilityType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaDataType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaRelationshipType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaTopologyTemplate;

class CommissioningProviderTest {
    private static final String TOSCA_SERVICE_TEMPLATE_YAML =
            "src/test/resources/rest/servicetemplates/pmsh_multiple_cl_tosca.yaml";
    private static final String COMMON_TOSCA_SERVICE_TEMPLATE_YAML =
            "src/test/resources/rest/servicetemplates/full-tosca-with-common-properties.yaml";
    private static final String TEMPLATE_IS_NULL = ".*serviceTemplate is marked non-null but is null";

    private PolicyModelsProvider modelsProvider = null;
    private ControlLoopProvider clProvider = null;
    private static final Coder CODER = new StandardCoder();
    private final ObjectMapper mapper = new ObjectMapper();
    private ParticipantProvider participantProvider;

    @AfterEach
    void close() throws Exception {
        if (modelsProvider != null) {
            modelsProvider.close();
        }
        if (clProvider != null) {
            clProvider.close();
        }
        if (participantProvider != null) {
            participantProvider.close();
        }
    }

    /**
     * Test the fetching of control loop definitions (ToscaServiceTemplates).
     *
     * @throws Exception .
     */
    @Test
    void testGetControlLoopDefinitions() throws Exception {
        ClRuntimeParameterGroup clRuntimeParameterGroup = CommonTestData.geParameterGroup("getCLDefinitions");
        modelsProvider =
                CommonTestData.getPolicyModelsProvider(clRuntimeParameterGroup.getDatabaseProviderParameters());
        clProvider = new ControlLoopProvider(clRuntimeParameterGroup.getDatabaseProviderParameters());
        participantProvider = new ParticipantProvider(clRuntimeParameterGroup.getDatabaseProviderParameters());

        CommissioningProvider provider =
                new CommissioningProvider(modelsProvider, clProvider, null, participantProvider);
        ToscaServiceTemplate serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);

        List<ToscaNodeTemplate> listOfTemplates = provider.getControlLoopDefinitions(null, null);
        assertThat(listOfTemplates).isEmpty();

        provider.createControlLoopDefinitions(serviceTemplate);
        listOfTemplates = provider.getControlLoopDefinitions(null, null);
        assertThat(listOfTemplates).hasSize(2);

        // Test Filtering
        listOfTemplates = provider.getControlLoopDefinitions("org.onap.domain.pmsh.PMSHControlLoopDefinition", "1.2.3");
        assertThat(listOfTemplates).hasSize(1);
        for (ToscaNodeTemplate template : listOfTemplates) {
            // Other CL elements contain PMSD instead of PMSH in their name
            assertThat(template.getName()).doesNotContain("PMSD");
        }

        // Test Wrong Name
        listOfTemplates = provider.getControlLoopDefinitions("WrongControlLoopName", "0.0.0");
        assertThat(listOfTemplates).isEmpty();

    }

    /**
     * Test the creation of control loop definitions (ToscaServiceTemplates).
     *
     * @throws Exception .
     */
    @Test
    void testCreateControlLoopDefinitions() throws Exception {
        ClRuntimeParameterGroup clRuntimeParameterGroup = CommonTestData.geParameterGroup("createCLDefinitions");
        modelsProvider =
                CommonTestData.getPolicyModelsProvider(clRuntimeParameterGroup.getDatabaseProviderParameters());
        clProvider = new ControlLoopProvider(clRuntimeParameterGroup.getDatabaseProviderParameters());
        participantProvider = new ParticipantProvider(clRuntimeParameterGroup.getDatabaseProviderParameters());

        CommissioningProvider provider =
                new CommissioningProvider(modelsProvider, clProvider, null, participantProvider);
        // Test Service template is null
        assertThatThrownBy(() -> provider.createControlLoopDefinitions(null)).hasMessageMatching(TEMPLATE_IS_NULL);
        List<ToscaNodeTemplate> listOfTemplates = provider.getControlLoopDefinitions(null, null);
        assertThat(listOfTemplates).isEmpty();

        ToscaServiceTemplate serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);

        // Response should return the number of node templates present in the service template
        List<ToscaConceptIdentifier> affectedDefinitions =
                provider.createControlLoopDefinitions(serviceTemplate).getAffectedControlLoopDefinitions();
        assertThat(affectedDefinitions).hasSize(13);
        listOfTemplates = provider.getControlLoopDefinitions(null, null);
        assertThat(listOfTemplates).hasSize(2);
    }

    /**
     * Test the fetching of a full ToscaServiceTemplate object - as opposed to the reduced template that is being
     * tested in the testGetToscaServiceTemplateReduced() test.
     *
     */
    @Test
    void testGetToscaServiceTemplate() throws Exception {
        ClRuntimeParameterGroup clRuntimeParameterGroup = CommonTestData.geParameterGroup("getCLDefinitions");
        modelsProvider =
                CommonTestData.getPolicyModelsProvider(clRuntimeParameterGroup.getDatabaseProviderParameters());
        clProvider = new ControlLoopProvider(clRuntimeParameterGroup.getDatabaseProviderParameters());
        participantProvider = new ParticipantProvider(clRuntimeParameterGroup.getDatabaseProviderParameters());

        CommissioningProvider provider =
                new CommissioningProvider(modelsProvider, clProvider, null, participantProvider);
        ToscaServiceTemplate serviceTemplate =
                InstantiationUtils.getToscaServiceTemplate(COMMON_TOSCA_SERVICE_TEMPLATE_YAML);

        provider.createControlLoopDefinitions(serviceTemplate);

        ToscaServiceTemplate returnedServiceTemplate = provider.getToscaServiceTemplate(null, null);
        assertThat(returnedServiceTemplate).isNotNull();

        Map<String, ToscaNodeTemplate> nodeTemplates =
                returnedServiceTemplate.getToscaTopologyTemplate().getNodeTemplates();

        assertThat(nodeTemplates).hasSize(8);
    }

    /**
     * Test the fetching of a reduced ToscaServiceTemplate with only some of the objects from the full template.
     * The reduced template does not contain: DataTypesAsMap or PolicyTypesAsMap.
     *
     */
    @Test
    void testGetToscaServiceTemplateReduced() throws Exception {
        ClRuntimeParameterGroup clRuntimeParameterGroup = CommonTestData.geParameterGroup("getCLDefinitions");
        modelsProvider =
                CommonTestData.getPolicyModelsProvider(clRuntimeParameterGroup.getDatabaseProviderParameters());
        clProvider = new ControlLoopProvider(clRuntimeParameterGroup.getDatabaseProviderParameters());
        participantProvider = new ParticipantProvider(clRuntimeParameterGroup.getDatabaseProviderParameters());

        CommissioningProvider provider =
                new CommissioningProvider(modelsProvider, clProvider, null, participantProvider);
        ToscaServiceTemplate serviceTemplate =
                InstantiationUtils.getToscaServiceTemplate(COMMON_TOSCA_SERVICE_TEMPLATE_YAML);

        provider.createControlLoopDefinitions(serviceTemplate);

        String returnedServiceTemplate = provider.getToscaServiceTemplateReduced(null, null);
        assertThat(returnedServiceTemplate).isNotNull();
        ToscaServiceTemplate parsedServiceTemplate = CODER.decode(returnedServiceTemplate, ToscaServiceTemplate.class);

        assertThat(parsedServiceTemplate.getToscaTopologyTemplate().getNodeTemplates()).hasSize(8);
    }

    /**
     * Tests the different schemas being returned from the schema endpoint. As schemas of the different
     * sections of the Tosca Service Templates can be returned by the API, this test must cover all of the
     * different sections.
     *
     */
    @Test
    void testGetToscaServiceTemplateSchema() throws Exception {

        ClRuntimeParameterGroup clRuntimeParameterGroup = CommonTestData.geParameterGroup("getCLDefinitions");
        modelsProvider =
                CommonTestData.getPolicyModelsProvider(clRuntimeParameterGroup.getDatabaseProviderParameters());
        clProvider = new ControlLoopProvider(clRuntimeParameterGroup.getDatabaseProviderParameters());
        participantProvider = new ParticipantProvider(clRuntimeParameterGroup.getDatabaseProviderParameters());

        CommissioningProvider provider =
                new CommissioningProvider(modelsProvider, clProvider, null, participantProvider);
        ToscaServiceTemplate serviceTemplate =
                InstantiationUtils.getToscaServiceTemplate(COMMON_TOSCA_SERVICE_TEMPLATE_YAML);

        provider.createControlLoopDefinitions(serviceTemplate);

        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        Map<String, Class<?>> sections = Map.of("all", ToscaServiceTemplate.class, "data_types", ToscaDataType.class,
                "capability_types", ToscaCapabilityType.class, "node_types", ToscaNodeType.class, "relationship_types",
                ToscaRelationshipType.class, "policy_types", ToscaPolicyType.class, "topology_template",
                ToscaTopologyTemplate.class, "node_templates", List.class);

        for (Map.Entry<String, Class<?>> entry : sections.entrySet()) {
            String returnedServiceTemplateSchema = provider.getToscaServiceTemplateSchema(entry.getKey());
            assertThat(returnedServiceTemplateSchema).isNotNull();

            var visitor = new SchemaFactoryWrapper();

            if (entry.getKey().equals("node_templates")) {
                mapper.acceptJsonFormatVisitor(
                        mapper.getTypeFactory().constructCollectionType(List.class, ToscaNodeTemplate.class), visitor);
            } else {
                mapper.acceptJsonFormatVisitor(mapper.constructType(entry.getValue()), visitor);
            }

            var jsonSchema = visitor.finalSchema();
            String localServiceTemplateSchema = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonSchema);
            assertThat(localServiceTemplateSchema).isEqualTo(returnedServiceTemplateSchema);
        }
    }

    /**
     * Test the deletion of control loop definitions (ToscaServiceTemplate).
     *
     * @throws Exception .
     */
    @Test
    void testDeleteControlLoopDefinitions() throws Exception {
        ClRuntimeParameterGroup clRuntimeParameterGroup = CommonTestData.geParameterGroup("deleteCLDefinitions");
        modelsProvider =
                CommonTestData.getPolicyModelsProvider(clRuntimeParameterGroup.getDatabaseProviderParameters());
        clProvider = new ControlLoopProvider(clRuntimeParameterGroup.getDatabaseProviderParameters());
        participantProvider = new ParticipantProvider(clRuntimeParameterGroup.getDatabaseProviderParameters());

        CommissioningProvider provider =
                new CommissioningProvider(modelsProvider, clProvider, null, participantProvider);
        ToscaServiceTemplate serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);

        List<ToscaNodeTemplate> listOfTemplates = provider.getControlLoopDefinitions(null, null);
        assertThat(listOfTemplates).isEmpty();

        provider.createControlLoopDefinitions(serviceTemplate);
        listOfTemplates = provider.getControlLoopDefinitions(null, null);
        assertThat(listOfTemplates).hasSize(2);

        provider.deleteControlLoopDefinition(serviceTemplate.getName(), serviceTemplate.getVersion());
        listOfTemplates = provider.getControlLoopDefinitions(null, null);
        assertThat(listOfTemplates).isEmpty();
    }

    /**
     * Test the fetching of control loop element definitions.
     *
     * @throws Exception .
     */
    @Test
    void testGetControlLoopElementDefinitions() throws Exception {
        ClRuntimeParameterGroup clRuntimeParameterGroup = CommonTestData.geParameterGroup("getCLElDefinitions");
        modelsProvider =
                CommonTestData.getPolicyModelsProvider(clRuntimeParameterGroup.getDatabaseProviderParameters());
        clProvider = new ControlLoopProvider(clRuntimeParameterGroup.getDatabaseProviderParameters());
        participantProvider = new ParticipantProvider(clRuntimeParameterGroup.getDatabaseProviderParameters());

        CommissioningProvider provider =
                new CommissioningProvider(modelsProvider, clProvider, null, participantProvider);
        ToscaServiceTemplate serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);

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
