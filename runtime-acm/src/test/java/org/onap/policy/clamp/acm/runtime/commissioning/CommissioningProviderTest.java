/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2025 Nordix Foundation.
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.onap.policy.clamp.acm.runtime.util.CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantPrimePublisher;
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.AcTypeStateUpdate;
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.PrimeOrder;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.AcTypeStateResolver;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.springframework.data.domain.Pageable;

class CommissioningProviderTest {

    /**
     * Test the fetching of automation composition definitions (ToscaServiceTemplates).
     */
    @Test
    void testGetAutomationCompositionDefinitions() {
        var acProvider = mock(AutomationCompositionProvider.class);
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var acRuntimeParameterGroup = mock(AcRuntimeParameterGroup.class);

        var provider = new CommissioningProvider(acDefinitionProvider, acProvider, null, null, null,
            acRuntimeParameterGroup);

        var serviceTemplates = provider.getAutomationCompositionDefinitions(null, null, Pageable.unpaged());
        assertThat(serviceTemplates.getServiceTemplates()).isEmpty();

        when(acDefinitionProvider.getServiceTemplateList(null, null, Pageable.unpaged()))
            .thenReturn(List.of(new ToscaServiceTemplate()));
        serviceTemplates = provider.getAutomationCompositionDefinitions(null, null, Pageable.unpaged());
        assertThat(serviceTemplates.getServiceTemplates()).hasSize(1);

        assertThatThrownBy(() -> provider.getAutomationCompositionDefinitions(null, null, null))
                .hasMessage("pageable is marked non-null but is null");
    }

    /**
     * Test the creation of automation composition definitions (ToscaServiceTemplates).
     */
    @Test
    void testCreateAutomationCompositionDefinitions() {
        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        assertNotNull(serviceTemplate);
        serviceTemplate.setName("Name");
        serviceTemplate.setVersion("1.0.0");
        var acmDefinition = new AutomationCompositionDefinition();
        acmDefinition.setCompositionId(UUID.randomUUID());
        acmDefinition.setServiceTemplate(serviceTemplate);
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        when(acDefinitionProvider.createAutomationCompositionDefinition(serviceTemplate,
            CommonTestData.TOSCA_ELEMENT_NAME, CommonTestData.TOSCA_COMP_NAME)).thenReturn(acmDefinition);

        var acProvider = mock(AutomationCompositionProvider.class);
        var provider = new CommissioningProvider(acDefinitionProvider, acProvider, null, null, null,
            CommonTestData.getTestParamaterGroup());
        var affectedDefinitions = provider.createAutomationCompositionDefinition(serviceTemplate)
            .getAffectedAutomationCompositionDefinitions();
        verify(acDefinitionProvider).createAutomationCompositionDefinition(serviceTemplate,
            CommonTestData.TOSCA_ELEMENT_NAME, CommonTestData.TOSCA_COMP_NAME);
        // Response should return the number of node templates present in the service template
        assertThat(affectedDefinitions).hasSize(7);
    }

    /**
     * Test the fetching of a full ToscaServiceTemplate object - as opposed to the reduced template that is being
     * tested in the testGetToscaServiceTemplateReduced() test.
     */
    @Test
    void testGetToscaServiceTemplateList() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var acProvider = mock(AutomationCompositionProvider.class);

        var provider = new CommissioningProvider(acDefinitionProvider, acProvider, null, null, null, null);
        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        assertNotNull(serviceTemplate);
        when(acDefinitionProvider
                .getServiceTemplateList(null, null, Pageable.unpaged())).thenReturn(List.of(serviceTemplate));

        var returnedServiceTemplate = provider.getAutomationCompositionDefinitions(null, null, Pageable.unpaged());
        assertThat(returnedServiceTemplate).isNotNull();
        assertThat(returnedServiceTemplate.getServiceTemplates()).isNotEmpty();
    }

    @Test
    void testDeleteAcDefinitionDabRequest() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var acProvider = mock(AutomationCompositionProvider.class);

        var compositionId = UUID.randomUUID();
        when(acProvider.getAcInstancesByCompositionId(compositionId)).thenReturn(List.of(new AutomationComposition()));

        var provider = new CommissioningProvider(acDefinitionProvider, acProvider, null, null, null, null);

        assertThatThrownBy(() -> provider.deleteAutomationCompositionDefinition(compositionId))
            .hasMessageMatching("Delete instances, to commission automation composition definitions");
    }

    @Test
    void testDeleteAutomationCompositionDefinition() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var compositionId = UUID.randomUUID();
        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        assertNotNull(serviceTemplate);
        when(acDefinitionProvider.deleteAcDefinition(compositionId)).thenReturn(serviceTemplate);

        var acmDefinition = new AutomationCompositionDefinition();
        acmDefinition.setCompositionId(compositionId);
        acmDefinition.setServiceTemplate(serviceTemplate);
        acmDefinition.setState(AcTypeState.COMMISSIONED);
        when(acDefinitionProvider.getAcDefinition(compositionId)).thenReturn(acmDefinition);

        var acProvider = mock(AutomationCompositionProvider.class);
        var provider = new CommissioningProvider(acDefinitionProvider, acProvider, null, null, null, null);

        provider.deleteAutomationCompositionDefinition(compositionId);

        verify(acDefinitionProvider).deleteAcDefinition(compositionId);
    }

    @Test
    void testPriming() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var acmDefinition = CommonTestData.createAcDefinition(
            InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML), AcTypeState.COMMISSIONED);
        var compositionId = acmDefinition.getCompositionId();
        when(acDefinitionProvider.getAcDefinition(compositionId)).thenReturn(acmDefinition);

        var participantPrimePublisher = mock(ParticipantPrimePublisher.class);
        var provider = new CommissioningProvider(acDefinitionProvider, mock(AutomationCompositionProvider.class),
            mock(ParticipantProvider.class), new AcTypeStateResolver(), participantPrimePublisher,
            CommonTestData.getTestParamaterGroup());

        var acTypeStateUpdate = new AcTypeStateUpdate();
        acTypeStateUpdate.setPrimeOrder(PrimeOrder.PRIME);
        provider.compositionDefinitionPriming(compositionId, acTypeStateUpdate);
        verify(acDefinitionProvider).updateAcDefinition(acmDefinition, CommonTestData.TOSCA_COMP_NAME);
        verify(participantPrimePublisher, timeout(1000).times(1)).sendPriming(any(), any(), any());
    }

    @Test
    void testDepriming() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var acmDefinition = CommonTestData.createAcDefinition(
            InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML), AcTypeState.PRIMED);
        var compositionId = acmDefinition.getCompositionId();
        when(acDefinitionProvider.getAcDefinition(compositionId)).thenReturn(acmDefinition);

        var participantPrimePublisher = mock(ParticipantPrimePublisher.class);
        var participantProvider = mock(ParticipantProvider.class);
        var provider = new CommissioningProvider(acDefinitionProvider, mock(AutomationCompositionProvider.class),
            participantProvider, new AcTypeStateResolver(), participantPrimePublisher,
            CommonTestData.getTestParamaterGroup());

        var acTypeStateUpdate = new AcTypeStateUpdate();
        acTypeStateUpdate.setPrimeOrder(PrimeOrder.DEPRIME);

        doNothing().when(participantProvider).verifyParticipantState(any());
        provider.compositionDefinitionPriming(compositionId, acTypeStateUpdate);
        verify(participantPrimePublisher, timeout(1000).times(1)).sendDepriming(compositionId);
    }

    @Test
    void testBadRequest() {
        var acProvider = mock(AutomationCompositionProvider.class);
        var provider = new CommissioningProvider(mock(AcDefinitionProvider.class), acProvider,
            mock(ParticipantProvider.class), new AcTypeStateResolver(), mock(ParticipantPrimePublisher.class),
            mock(AcRuntimeParameterGroup.class));

        var compositionId = UUID.randomUUID();
        when(acProvider.getAcInstancesByCompositionId(compositionId)).thenReturn(List.of(new AutomationComposition()));

        var toscaServiceTemplate = new ToscaServiceTemplate();
        assertThatThrownBy(() -> provider.updateCompositionDefinition(compositionId, toscaServiceTemplate))
            .hasMessageMatching("There are ACM instances, Update of ACM Definition not allowed");

        var acTypeStateUpdate = new AcTypeStateUpdate();
        assertThatThrownBy(() -> provider.compositionDefinitionPriming(compositionId, acTypeStateUpdate))
            .hasMessageMatching("There are instances, Priming/Depriming not allowed");
    }

    @Test
    void testPrimedBadRequest() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var toscaServiceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        var acmDefinition = CommonTestData.createAcDefinition(toscaServiceTemplate, AcTypeState.PRIMED);
        var compositionId = acmDefinition.getCompositionId();
        when(acDefinitionProvider.getAcDefinition(compositionId)).thenReturn(acmDefinition);

        var provider = new CommissioningProvider(acDefinitionProvider, mock(AutomationCompositionProvider.class),
            mock(ParticipantProvider.class), new AcTypeStateResolver(), mock(ParticipantPrimePublisher.class),
            mock(AcRuntimeParameterGroup.class));

        assertThatThrownBy(() -> provider.updateCompositionDefinition(compositionId, toscaServiceTemplate))
            .hasMessageMatching("ACM not in COMMISSIONED state, Update of ACM Definition not allowed");

        assertThatThrownBy(() -> provider.deleteAutomationCompositionDefinition(compositionId))
            .hasMessageMatching("ACM not in COMMISSIONED state, Delete of ACM Definition not allowed");
    }
}
