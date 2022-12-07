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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaAutomationComposition;
import org.onap.policy.clamp.models.acm.persistence.repository.AutomationCompositionRepository;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class AutomationCompositionProviderTest {

    private static final String OBJECT_IS_NULL = "automationComposition is marked non-null but is null";

    private static final String ID_NAME = "PMSHInstance1";
    private static final String ID_VERSION = "1.0.1";
    private static final String ID_NAME_NOT_EXTST = "not_exist";

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
    void testAutomationCompositionCreate() {
        var automationCompositionRepository = mock(AutomationCompositionRepository.class);
        var automationCompositionProvider = new AutomationCompositionProvider(automationCompositionRepository);

        when(automationCompositionRepository.save(any(JpaAutomationComposition.class)))
                .thenReturn(inputAutomationCompositionsJpa.get(0));
        var inputAc = inputAutomationCompositions.getAutomationCompositionList().get(0);

        var createdAutomationComposition = automationCompositionProvider.createAutomationComposition(inputAc);
        inputAc.setInstanceId(createdAutomationComposition.getInstanceId());
        assertEquals(inputAc, createdAutomationComposition);
    }

    @Test
    void testAutomationCompositionUpdate() {
        var automationCompositionRepository = mock(AutomationCompositionRepository.class);
        var automationCompositionProvider = new AutomationCompositionProvider(automationCompositionRepository);

        assertThatThrownBy(() -> automationCompositionProvider.updateAutomationComposition(null))
                .hasMessageMatching(OBJECT_IS_NULL);

        when(automationCompositionRepository.save(inputAutomationCompositionsJpa.get(0)))
                .thenReturn(inputAutomationCompositionsJpa.get(0));

        var createdAutomationComposition = automationCompositionProvider
                .updateAutomationComposition(inputAutomationCompositions.getAutomationCompositionList().get(0));

        assertEquals(inputAutomationCompositions.getAutomationCompositionList().get(0), createdAutomationComposition);
    }

    @Test
    void testGetAutomationCompositions() throws Exception {
        var automationComposition0 = inputAutomationCompositions.getAutomationCompositionList().get(1);
        var name = automationComposition0.getName();
        var version = automationComposition0.getVersion();
        var automationComposition1 = inputAutomationCompositions.getAutomationCompositionList().get(1);

        var automationCompositionRepository = mock(AutomationCompositionRepository.class);
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

        var automationCompositionProvider = new AutomationCompositionProvider(automationCompositionRepository);
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

        assertThat(automationCompositionProvider
                .findAutomationComposition(new ToscaConceptIdentifier(ID_NAME_NOT_EXTST, ID_VERSION))).isEmpty();
    }

    @Test
    void testGetAutomationComposition() {
        var automationCompositionRepository = mock(AutomationCompositionRepository.class);
        var automationCompositionProvider = new AutomationCompositionProvider(automationCompositionRepository);

        var automationComposition = inputAutomationCompositions.getAutomationCompositionList().get(0);
        when(automationCompositionRepository.findByInstanceId(automationComposition.getInstanceId().toString()))
                .thenReturn(Optional.of(inputAutomationCompositionsJpa.get(0)));
        var ac = automationCompositionProvider.getAutomationComposition(automationComposition.getInstanceId());
        assertEquals(inputAutomationCompositions.getAutomationCompositionList().get(0), ac);
    }

    @Test
    void testGetAcInstancesByCompositionId() {
        var automationCompositionRepository = mock(AutomationCompositionRepository.class);
        var automationCompositionProvider = new AutomationCompositionProvider(automationCompositionRepository);

        var automationComposition = inputAutomationCompositions.getAutomationCompositionList().get(0);
        when(automationCompositionRepository.findByCompositionId(automationComposition.getCompositionId().toString()))
                .thenReturn(inputAutomationCompositionsJpa);
        var acList =
                automationCompositionProvider.getAcInstancesByCompositionId(automationComposition.getCompositionId());
        assertEquals(inputAutomationCompositions.getAutomationCompositionList(), acList);
    }

    @Test
    void testDeleteAutomationComposition() {
        var automationCompositionRepository = mock(AutomationCompositionRepository.class);
        var automationCompositionProvider = new AutomationCompositionProvider(automationCompositionRepository);

        assertThatThrownBy(() -> automationCompositionProvider.deleteAutomationComposition(UUID.randomUUID()))
                .hasMessageMatching(".*.failed, automation composition does not exist");

        var automationComposition = inputAutomationCompositions.getAutomationCompositionList().get(0);
        when(automationCompositionRepository.findByInstanceId(automationComposition.getInstanceId().toString()))
                .thenReturn(Optional.of(inputAutomationCompositionsJpa.get(0)));

        var deletedAc =
                automationCompositionProvider.deleteAutomationComposition(automationComposition.getInstanceId());
        assertEquals(automationComposition, deletedAc);
    }
}
