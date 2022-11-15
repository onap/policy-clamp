/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2022 Nordix Foundation.
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import javax.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaAutomationComposition;
import org.onap.policy.clamp.models.acm.persistence.repository.AutomationCompositionRepository;
import org.onap.policy.clamp.models.acm.persistence.repository.ToscaNodeTemplateRepository;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaTypedEntityFilter;
import org.onap.policy.models.tosca.simple.concepts.JpaToscaNodeTemplate;

class AutomationCompositionProviderTest {

    private static final String LIST_IS_NULL = "automationCompositions is marked .*ull but is null";
    private static final String OBJECT_IS_NULL = "automationComposition is marked non-null but is null";

    private static final String ID_NAME = "PMSHInstance1";
    private static final String ID_VERSION = "1.0.1";
    private static final String ID_NAME_NOT_EXTST = "not_exist";
    private static final String ID_NAME_NOT_VALID = "not_valid";

    private static final Coder CODER = new StandardCoder();
    private static final String AUTOMATION_COMPOSITION_JSON =
        "src/test/resources/providers/TestAutomationCompositions.json";

    private AutomationCompositions inputAutomationCompositions;
    private List<JpaAutomationComposition> inputAutomationCompositionsJpa;
    private final String originalJson = ResourceUtils.getResourceAsString(AUTOMATION_COMPOSITION_JSON);

    @BeforeEach
    void beforeSetupDao() throws Exception {
        inputAutomationCompositions = CODER.decode(originalJson, AutomationCompositions.class);
        inputAutomationCompositionsJpa =
            ProviderUtils.getJpaAndValidateList(inputAutomationCompositions.getAutomationCompositionList(),
                JpaAutomationComposition::new, "automation compositions");
    }

    @Test
    void testAutomationCompositionsSave() throws Exception {
        var automationCompositionRepository = mock(AutomationCompositionRepository.class);
        var automationCompositionProvider = new AutomationCompositionProvider(automationCompositionRepository,
            mock(ToscaNodeTemplateRepository.class));

        assertThatThrownBy(() -> automationCompositionProvider.saveAutomationCompositions(null))
            .hasMessageMatching(LIST_IS_NULL);

        when(automationCompositionRepository.saveAll(inputAutomationCompositionsJpa))
            .thenReturn(inputAutomationCompositionsJpa);

        var createdAutomationCompositions = new AutomationCompositions();
        createdAutomationCompositions.setAutomationCompositionList(automationCompositionProvider
            .saveAutomationCompositions(inputAutomationCompositions.getAutomationCompositionList()));

        assertEquals(inputAutomationCompositions, createdAutomationCompositions);

        when(automationCompositionRepository.saveAll(any())).thenThrow(IllegalArgumentException.class);

        assertThatThrownBy(() -> automationCompositionProvider
            .saveAutomationCompositions(inputAutomationCompositions.getAutomationCompositionList()))
            .hasMessageMatching("Error in save AutomationCompositions");
    }

    @Test
    void testAutomationCompositionSave() throws Exception {
        var automationCompositionRepository = mock(AutomationCompositionRepository.class);
        var automationCompositionProvider = new AutomationCompositionProvider(automationCompositionRepository,
            mock(ToscaNodeTemplateRepository.class));

        assertThatThrownBy(() -> automationCompositionProvider.saveAutomationComposition(null))
            .hasMessageMatching(OBJECT_IS_NULL);

        when(automationCompositionRepository.save(inputAutomationCompositionsJpa.get(0)))
            .thenReturn(inputAutomationCompositionsJpa.get(0));

        var createdAutomationComposition = automationCompositionProvider
            .saveAutomationComposition(inputAutomationCompositions.getAutomationCompositionList().get(0));

        assertEquals(inputAutomationCompositions.getAutomationCompositionList().get(0), createdAutomationComposition);

        when(automationCompositionRepository.save(any())).thenThrow(IllegalArgumentException.class);

        assertThatThrownBy(() -> automationCompositionProvider
            .saveAutomationComposition(inputAutomationCompositions.getAutomationCompositionList().get(0)))
            .hasMessageMatching("Error in save automationComposition");
    }

    @Test
    void testGetAutomationCompositions() throws Exception {
        var automationCompositionRepository = mock(AutomationCompositionRepository.class);
        var automationCompositionProvider = new AutomationCompositionProvider(automationCompositionRepository,
            mock(ToscaNodeTemplateRepository.class));

        // Return empty list when no data present in db
        List<AutomationComposition> getResponse = automationCompositionProvider.getAutomationCompositions();
        assertThat(getResponse).isEmpty();

        automationCompositionProvider
            .saveAutomationCompositions(inputAutomationCompositions.getAutomationCompositionList());

        var automationComposition0 = inputAutomationCompositions.getAutomationCompositionList().get(1);
        var name = automationComposition0.getName();
        var version = automationComposition0.getVersion();
        var automationComposition1 = inputAutomationCompositions.getAutomationCompositionList().get(1);

        when(automationCompositionRepository.getFiltered(eq(JpaAutomationComposition.class), any(), any()))
            .thenReturn(List.of(new JpaAutomationComposition(automationComposition0),
                new JpaAutomationComposition(automationComposition1)));
        when(automationCompositionRepository.findById(automationComposition0.getKey().asIdentifier().asConceptKey()))
            .thenReturn(Optional.of(new JpaAutomationComposition(automationComposition0)));
        when(automationCompositionRepository.getById(automationComposition0.getKey().asIdentifier().asConceptKey()))
            .thenReturn(new JpaAutomationComposition(automationComposition0));
        when(automationCompositionRepository.getFiltered(JpaAutomationComposition.class, name, version))
            .thenReturn(List.of(new JpaAutomationComposition(automationComposition0)));
        when(automationCompositionRepository.findById(automationComposition1.getKey().asIdentifier().asConceptKey()))
            .thenReturn(Optional.of(new JpaAutomationComposition(automationComposition1)));

        assertEquals(1, automationCompositionProvider.getAutomationCompositions(name, version).size());

        var ac = automationCompositionProvider
            .findAutomationComposition(new ToscaConceptIdentifier(ID_NAME, ID_VERSION))
            .orElse(new AutomationComposition());
        assertEquals(inputAutomationCompositions.getAutomationCompositionList().get(1), ac);

        ac = automationCompositionProvider.getAutomationComposition(new ToscaConceptIdentifier(ID_NAME, ID_VERSION));
        assertEquals(inputAutomationCompositions.getAutomationCompositionList().get(1), ac);

        when(automationCompositionRepository.getById(any())).thenThrow(EntityNotFoundException.class);

        assertThatThrownBy(() -> automationCompositionProvider
            .getAutomationComposition(new ToscaConceptIdentifier(ID_NAME_NOT_EXTST, ID_VERSION)))
            .hasMessageMatching("AutomationComposition not found");

        ac = automationCompositionProvider.findAutomationComposition(ID_NAME, ID_VERSION)
            .orElse(new AutomationComposition());
        assertEquals(inputAutomationCompositions.getAutomationCompositionList().get(1), ac);

        assertThat(automationCompositionProvider
            .findAutomationComposition(new ToscaConceptIdentifier(ID_NAME_NOT_EXTST, ID_VERSION))).isEmpty();

        when(automationCompositionRepository.findById(any())).thenThrow(IllegalArgumentException.class);

        assertThatThrownBy(() -> automationCompositionProvider.findAutomationComposition(ID_NAME_NOT_VALID, ID_VERSION))
            .hasMessageMatching("Not valid parameter");
    }

    @Test
    void testDeleteAutomationComposition() throws Exception {
        var automationCompositionRepository = mock(AutomationCompositionRepository.class);
        var automationCompositionProvider = new AutomationCompositionProvider(automationCompositionRepository,
            mock(ToscaNodeTemplateRepository.class));

        assertThatThrownBy(() -> automationCompositionProvider
            .deleteAutomationComposition(ID_NAME_NOT_EXTST, ID_VERSION))
            .hasMessageMatching(".*.failed, automation composition does not exist");

        var automationComposition = inputAutomationCompositions.getAutomationCompositionList().get(0);
        var name = automationComposition.getName();
        var version = automationComposition.getVersion();

        when(automationCompositionRepository.findById(new PfConceptKey(name, version)))
            .thenReturn(Optional.of(inputAutomationCompositionsJpa.get(0)));

        AutomationComposition deletedAc = automationCompositionProvider.deleteAutomationComposition(name, version);
        assertEquals(automationComposition, deletedAc);
    }

    @Test
    void testGetNodeTemplates() {
        var toscaNodeTemplateRepository = mock(ToscaNodeTemplateRepository.class);
        var automationCompositionProvider =
            new AutomationCompositionProvider(mock(AutomationCompositionRepository.class), toscaNodeTemplateRepository);

        var toscaNodeTemplate0 = new JpaToscaNodeTemplate(new PfConceptKey(ID_NAME, ID_VERSION));
        var toscaNodeTemplate1 = new JpaToscaNodeTemplate(new PfConceptKey("PMSHInstance2", ID_VERSION));

        when(toscaNodeTemplateRepository.getFiltered(JpaToscaNodeTemplate.class, null, null))
            .thenReturn(List.of(toscaNodeTemplate0, toscaNodeTemplate1));
        when(toscaNodeTemplateRepository.findAll()).thenReturn(List.of(toscaNodeTemplate0, toscaNodeTemplate1));
        when(toscaNodeTemplateRepository.getFiltered(JpaToscaNodeTemplate.class, ID_NAME, ID_VERSION))
            .thenReturn(List.of(toscaNodeTemplate0));

        // Getting all nodes
        var listNodes = automationCompositionProvider.getAllNodeTemplates();
        assertNotNull(listNodes);
        assertThat(listNodes).hasSize(2);

        listNodes = automationCompositionProvider.getNodeTemplates(ID_NAME, ID_VERSION);
        assertNotNull(listNodes);
        assertThat(listNodes).hasSize(1);

        listNodes = automationCompositionProvider.getAllNodeTemplates();
        assertNotNull(listNodes);
        assertThat(listNodes).hasSize(2);

        var nodeTemplateFilter =
            ToscaTypedEntityFilter.<ToscaNodeTemplate>builder().name(ID_NAME).version(ID_VERSION).build();

        listNodes = automationCompositionProvider.getFilteredNodeTemplates(nodeTemplateFilter);
        assertNotNull(listNodes);
        assertThat(listNodes).hasSize(1);

        assertThatThrownBy(() -> automationCompositionProvider.getFilteredNodeTemplates(null))
            .hasMessageMatching("filter is marked non-null but is null");
    }
}
