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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ControlLoopProvider;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ServiceTemplateProvider;
import org.onap.policy.clamp.controlloop.runtime.instantiation.InstantiationUtils;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.StandardCoder;
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

    private static final Coder CODER = new StandardCoder();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Test the fetching of control loop definitions (ToscaServiceTemplates).
     *
     * @throws Exception .
     */
    @Test
    void testGetControlLoopDefinitions() throws Exception {
        var clProvider = mock(ControlLoopProvider.class);
        var participantProvider = mock(ParticipantProvider.class);
        var serviceTemplateProvider = mock(ServiceTemplateProvider.class);

        CommissioningProvider provider =
                new CommissioningProvider(serviceTemplateProvider, clProvider, null, participantProvider);

        List<ToscaNodeTemplate> listOfTemplates = provider.getControlLoopDefinitions(null, null);
        assertThat(listOfTemplates).isEmpty();

        when(clProvider.getFilteredNodeTemplates(any()))
                .thenReturn(List.of(new ToscaNodeTemplate(), new ToscaNodeTemplate()));
        listOfTemplates = provider.getControlLoopDefinitions(null, null);
        assertThat(listOfTemplates).hasSize(2);
    }

    /**
     * Test the creation of control loop definitions (ToscaServiceTemplates).
     *
     * @throws Exception .
     */
    @Test
    void testCreateControlLoopDefinitions() throws Exception {
        var serviceTemplateProvider = mock(ServiceTemplateProvider.class);
        var clProvider = mock(ControlLoopProvider.class);
        var participantProvider = mock(ParticipantProvider.class);

        CommissioningProvider provider =
                new CommissioningProvider(serviceTemplateProvider, clProvider, null, participantProvider);

        List<ToscaNodeTemplate> listOfTemplates = provider.getControlLoopDefinitions(null, null);
        assertThat(listOfTemplates).isEmpty();

        ToscaServiceTemplate serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        when(serviceTemplateProvider.createServiceTemplate(serviceTemplate)).thenReturn(serviceTemplate);

        // Response should return the number of node templates present in the service template
        List<ToscaConceptIdentifier> affectedDefinitions =
                provider.createControlLoopDefinitions(serviceTemplate).getAffectedControlLoopDefinitions();
        assertThat(affectedDefinitions).hasSize(13);

        when(clProvider.getFilteredNodeTemplates(any()))
                .thenReturn(List.of(new ToscaNodeTemplate(), new ToscaNodeTemplate()));

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
        var serviceTemplateProvider = mock(ServiceTemplateProvider.class);
        var clProvider = mock(ControlLoopProvider.class);
        var participantProvider = mock(ParticipantProvider.class);

        CommissioningProvider provider =
                new CommissioningProvider(serviceTemplateProvider, clProvider, null, participantProvider);
        ToscaServiceTemplate serviceTemplate =
                InstantiationUtils.getToscaServiceTemplate(COMMON_TOSCA_SERVICE_TEMPLATE_YAML);
        when(serviceTemplateProvider.createServiceTemplate(serviceTemplate)).thenReturn(serviceTemplate);

        provider.createControlLoopDefinitions(serviceTemplate);
        verify(serviceTemplateProvider).createServiceTemplate(serviceTemplate);

        when(serviceTemplateProvider.getToscaServiceTemplate(eq(null), eq(null))).thenReturn(serviceTemplate);

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
        var serviceTemplateProvider = mock(ServiceTemplateProvider.class);
        var clProvider = mock(ControlLoopProvider.class);
        var participantProvider = mock(ParticipantProvider.class);

        CommissioningProvider provider =
                new CommissioningProvider(serviceTemplateProvider, clProvider, null, participantProvider);
        ToscaServiceTemplate serviceTemplate =
                InstantiationUtils.getToscaServiceTemplate(COMMON_TOSCA_SERVICE_TEMPLATE_YAML);
        when(serviceTemplateProvider.createServiceTemplate(serviceTemplate)).thenReturn(serviceTemplate);

        provider.createControlLoopDefinitions(serviceTemplate);

        when(serviceTemplateProvider.getServiceTemplateList(any(), any())).thenReturn(List.of(serviceTemplate));

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
        var serviceTemplateProvider = mock(ServiceTemplateProvider.class);
        var clProvider = mock(ControlLoopProvider.class);
        var participantProvider = mock(ParticipantProvider.class);

        CommissioningProvider provider =
                new CommissioningProvider(serviceTemplateProvider, clProvider, null, participantProvider);
        ToscaServiceTemplate serviceTemplate =
                InstantiationUtils.getToscaServiceTemplate(COMMON_TOSCA_SERVICE_TEMPLATE_YAML);
        when(serviceTemplateProvider.createServiceTemplate(serviceTemplate)).thenReturn(serviceTemplate);

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
}
