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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.onap.policy.clamp.acm.runtime.util.CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

class CommissioningProviderTest {

    /**
     * Test the fetching of automation composition definitions (ToscaServiceTemplates).
     *
     * @throws Exception .
     */
    @Test
    void testGetAutomationCompositionDefinitions() throws Exception {
        var acProvider = mock(AutomationCompositionProvider.class);
        var participantProvider = mock(ParticipantProvider.class);
        var acDefinitionProvider = mock(AcDefinitionProvider.class);

        var provider = new CommissioningProvider(acDefinitionProvider, acProvider, null, participantProvider);

        var serviceTemplates = provider.getAutomationCompositionDefinitions(null, null);
        assertThat(serviceTemplates.getServiceTemplates()).isEmpty();

        when(acDefinitionProvider.getServiceTemplateList(null, null))
                .thenReturn(List.of(new ToscaServiceTemplate()));
        serviceTemplates = provider.getAutomationCompositionDefinitions(null, null);
        assertThat(serviceTemplates.getServiceTemplates()).hasSize(1);
    }

    /**
     * Test the creation of automation composition definitions (ToscaServiceTemplates).
     *
     * @throws Exception .
     */
    @Test
    void testCreateAutomationCompositionDefinitions() throws Exception {
        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        var acmDefinition = new AutomationCompositionDefinition();
        acmDefinition.setCompositionId(UUID.randomUUID());
        acmDefinition.setServiceTemplate(serviceTemplate);
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        when(acDefinitionProvider.createAutomationCompositionDefinition(serviceTemplate)).thenReturn(acmDefinition);

        // Response should return the number of node templates present in the service template
        var acProvider = mock(AutomationCompositionProvider.class);
        var participantProvider = mock(ParticipantProvider.class);
        var provider = new CommissioningProvider(acDefinitionProvider, acProvider, null, participantProvider);
        List<ToscaConceptIdentifier> affectedDefinitions = provider
                .createAutomationCompositionDefinitions(serviceTemplate).getAffectedAutomationCompositionDefinitions();
        verify(acDefinitionProvider).createAutomationCompositionDefinition(serviceTemplate);
        assertThat(affectedDefinitions).hasSize(7);
    }

    /**
     * Test the fetching of a full ToscaServiceTemplate object - as opposed to the reduced template that is being
     * tested in the testGetToscaServiceTemplateReduced() test.
     *
     */
    @Test
    void testGetToscaServiceTemplateList() throws Exception {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var acProvider = mock(AutomationCompositionProvider.class);
        var participantProvider = mock(ParticipantProvider.class);

        var provider =
                new CommissioningProvider(acDefinitionProvider, acProvider, null, participantProvider);
        ToscaServiceTemplate serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        when(acDefinitionProvider.getServiceTemplateList(null, null)).thenReturn(List.of(serviceTemplate));

        var returnedServiceTemplate = provider.getAutomationCompositionDefinitions(null, null);
        assertThat(returnedServiceTemplate).isNotNull();
        assertThat(returnedServiceTemplate.getServiceTemplates()).isNotEmpty();
    }
}
