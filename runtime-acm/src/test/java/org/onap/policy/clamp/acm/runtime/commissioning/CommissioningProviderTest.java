/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation.
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

package org.onap.policy.clamp.acm.runtime.commissioning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.onap.policy.clamp.acm.runtime.util.CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML;
import static org.onap.policy.clamp.acm.runtime.util.CommonTestData.TOSCA_ST_TEMPLATE_YAML;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ServiceTemplateProvider;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.RecordApplicationEvents;

@SpringBootTest
@RecordApplicationEvents
class CommissioningProviderTest {

    private static final Coder CODER = new StandardCoder();

    /**
     * Test the fetching of automation composition definitions (ToscaServiceTemplates).
     *
     * @throws Exception .
     */
    @Test
    void testGetAutomationCompositionDefinitions() throws Exception {
        var acProvider = mock(AutomationCompositionProvider.class);
        var participantProvider = mock(ParticipantProvider.class);
        var serviceTemplateProvider = mock(ServiceTemplateProvider.class);

        CommissioningProvider provider =
            new CommissioningProvider(serviceTemplateProvider, acProvider, null, participantProvider);

        List<ToscaNodeTemplate> listOfTemplates = provider.getAutomationCompositionDefinitions(null, null);
        assertThat(listOfTemplates).isEmpty();

        when(acProvider.getFilteredNodeTemplates(any()))
            .thenReturn(List.of(new ToscaNodeTemplate(), new ToscaNodeTemplate()));
        listOfTemplates = provider.getAutomationCompositionDefinitions(null, null);
        assertThat(listOfTemplates).hasSize(2);
    }

    /**
     * Test the creation of automation composition definitions (ToscaServiceTemplates).
     *
     * @throws Exception .
     */
    @Test
    void testCreateAutomationCompositionDefinitions() throws Exception {
        var serviceTemplateProvider = mock(ServiceTemplateProvider.class);
        var acProvider = mock(AutomationCompositionProvider.class);
        var participantProvider = mock(ParticipantProvider.class);

        CommissioningProvider provider =
            new CommissioningProvider(serviceTemplateProvider, acProvider, null, participantProvider);

        List<ToscaNodeTemplate> listOfTemplates = provider.getAutomationCompositionDefinitions(null, null);
        assertThat(listOfTemplates).isEmpty();

        ToscaServiceTemplate serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        when(serviceTemplateProvider.createServiceTemplate(serviceTemplate)).thenReturn(serviceTemplate);

        // Response should return the number of node templates present in the service template
        List<ToscaConceptIdentifier> affectedDefinitions = provider
            .createAutomationCompositionDefinitions(serviceTemplate).getAffectedAutomationCompositionDefinitions();
        assertThat(affectedDefinitions).hasSize(13);

        when(acProvider.getFilteredNodeTemplates(any()))
            .thenReturn(List.of(new ToscaNodeTemplate(), new ToscaNodeTemplate()));

        listOfTemplates = provider.getAutomationCompositionDefinitions(null, null);
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
        var acProvider = mock(AutomationCompositionProvider.class);
        var participantProvider = mock(ParticipantProvider.class);

        CommissioningProvider provider =
            new CommissioningProvider(serviceTemplateProvider, acProvider, null, participantProvider);
        ToscaServiceTemplate serviceTemplate =
            InstantiationUtils.getToscaServiceTemplate(TOSCA_ST_TEMPLATE_YAML);
        when(serviceTemplateProvider.createServiceTemplate(serviceTemplate)).thenReturn(serviceTemplate);

        provider.createAutomationCompositionDefinitions(serviceTemplate);
        verify(serviceTemplateProvider).createServiceTemplate(serviceTemplate);

        when(serviceTemplateProvider.getToscaServiceTemplate(null, null)).thenReturn(serviceTemplate);

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
        var acProvider = mock(AutomationCompositionProvider.class);
        var participantProvider = mock(ParticipantProvider.class);

        CommissioningProvider provider =
            new CommissioningProvider(serviceTemplateProvider, acProvider, null, participantProvider);
        ToscaServiceTemplate serviceTemplate =
            InstantiationUtils.getToscaServiceTemplate(TOSCA_ST_TEMPLATE_YAML);
        when(serviceTemplateProvider.createServiceTemplate(serviceTemplate)).thenReturn(serviceTemplate);

        provider.createAutomationCompositionDefinitions(serviceTemplate);

        when(serviceTemplateProvider.getServiceTemplateList(any(), any())).thenReturn(List.of(
            Objects.requireNonNull(serviceTemplate)));

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
        var acProvider = mock(AutomationCompositionProvider.class);
        var participantProvider = mock(ParticipantProvider.class);

        CommissioningProvider provider =
            new CommissioningProvider(serviceTemplateProvider, acProvider, null, participantProvider);
        ToscaServiceTemplate serviceTemplate =
            InstantiationUtils.getToscaServiceTemplate(TOSCA_ST_TEMPLATE_YAML);
        when(serviceTemplateProvider.createServiceTemplate(serviceTemplate)).thenReturn(serviceTemplate);

        provider.createAutomationCompositionDefinitions(serviceTemplate);

        final Map<String, Class<?>> sections = Map.of("all", ToscaServiceTemplate.class,
            "data_types", ToscaDataType.class, "capability_types", ToscaCapabilityType.class,
            "node_types", ToscaNodeType.class, "relationship_types",
            ToscaRelationshipType.class, "policy_types", ToscaPolicyType.class, "topology_template",
            ToscaTopologyTemplate.class, "node_templates", List.class);

        String localServiceTemplateSchema = new String();
        Gson gson = new Gson();
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(ToscaTopologyTemplate.class,
                (JsonSerializer<ToscaTopologyTemplate>) (src, typeOfSrc, context) -> new JsonPrimitive(src.toString()));
        builder.setPrettyPrinting();
        gson = builder.create();

        for (Map.Entry<String, Class<?>> entry : sections.entrySet()) {
            String returnedServiceTemplateSchema = provider.getToscaServiceTemplateSchema(entry.getKey());
            assertThat(returnedServiceTemplateSchema).isNotNull();

            if (entry.getKey().equals("topology_template")) {
                localServiceTemplateSchema = gson.toJson(new ToscaTopologyTemplate());
                assertThat(localServiceTemplateSchema).isEqualTo(returnedServiceTemplateSchema);
            }
        }
    }
}
