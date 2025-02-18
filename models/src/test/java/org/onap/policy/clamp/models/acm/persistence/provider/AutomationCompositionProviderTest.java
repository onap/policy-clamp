/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2025 Nordix Foundation.
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
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaAutomationComposition;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaAutomationCompositionElement;
import org.onap.policy.clamp.models.acm.persistence.repository.AutomationCompositionElementRepository;
import org.onap.policy.clamp.models.acm.persistence.repository.AutomationCompositionRepository;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.springframework.data.domain.Example;

class AutomationCompositionProviderTest {

    private static final String AC_IS_NULL = "automationComposition is marked non-null but is null";
    private static final String ACELEMENT_ID_IS_NULL = "elementId is marked non-null but is null";

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
        var automationCompositionProvider = new AutomationCompositionProvider(automationCompositionRepository,
                mock(AutomationCompositionElementRepository.class));

        when(automationCompositionRepository.save(any(JpaAutomationComposition.class)))
                .thenReturn(inputAutomationCompositionsJpa.get(0));
        var inputAc = inputAutomationCompositions.getAutomationCompositionList().get(0);

        var createdAutomationComposition = automationCompositionProvider.createAutomationComposition(inputAc);
        inputAc.setInstanceId(createdAutomationComposition.getInstanceId());
        inputAc.setLastMsg(createdAutomationComposition.getLastMsg());
        assertEquals(inputAc, createdAutomationComposition);
    }

    @Test
    void testAutomationCompositionUpdate() {
        var automationCompositionRepository = mock(AutomationCompositionRepository.class);
        var automationCompositionProvider = new AutomationCompositionProvider(automationCompositionRepository,
                mock(AutomationCompositionElementRepository.class));

        assertThatThrownBy(() -> automationCompositionProvider.updateAutomationComposition(null))
                .hasMessageMatching(AC_IS_NULL);

        when(automationCompositionRepository.save(inputAutomationCompositionsJpa.get(0)))
                .thenReturn(inputAutomationCompositionsJpa.get(0));

        var createdAutomationComposition = automationCompositionProvider
                .updateAutomationComposition(inputAutomationCompositions.getAutomationCompositionList().get(0));

        assertEquals(inputAutomationCompositions.getAutomationCompositionList().get(0), createdAutomationComposition);
    }

    @Test
    void testGetAutomationCompositions() {
        var automationCompositionRepository = mock(AutomationCompositionRepository.class);
        var automationCompositionProvider = new AutomationCompositionProvider(automationCompositionRepository,
                mock(AutomationCompositionElementRepository.class));

        var automationComposition = inputAutomationCompositions.getAutomationCompositionList().get(0);
        var acList = automationCompositionProvider.getAutomationCompositions(UUID.randomUUID(),
                automationComposition.getName(), automationComposition.getVersion());
        assertThat(acList).isEmpty();

        when(automationCompositionRepository.findAll(Mockito.<Example<JpaAutomationComposition>>any()))
                .thenReturn(inputAutomationCompositionsJpa);
        acList = automationCompositionProvider.getAutomationCompositions(automationComposition.getCompositionId(), null,
                null);
        assertThat(acList).hasSize(2);
    }

    @Test
    void testGetAutomationComposition() {
        var automationCompositionRepository = mock(AutomationCompositionRepository.class);
        var automationCompositionProvider = new AutomationCompositionProvider(automationCompositionRepository,
                mock(AutomationCompositionElementRepository.class));

        var automationComposition = inputAutomationCompositions.getAutomationCompositionList().get(0);
        assertThatThrownBy(
                () -> automationCompositionProvider.getAutomationComposition(automationComposition.getInstanceId()))
                        .hasMessageMatching("AutomationComposition not found");

        when(automationCompositionRepository.findById(automationComposition.getInstanceId().toString()))
                .thenReturn(Optional.of(inputAutomationCompositionsJpa.get(0)));
        var ac = automationCompositionProvider.getAutomationComposition(automationComposition.getInstanceId());
        assertEquals(automationComposition, ac);
    }

    @Test
    void testFindAutomationComposition() {
        var automationCompositionRepository = mock(AutomationCompositionRepository.class);
        var automationCompositionProvider = new AutomationCompositionProvider(automationCompositionRepository,
                mock(AutomationCompositionElementRepository.class));

        var automationComposition = inputAutomationCompositions.getAutomationCompositionList().get(0);
        var acOpt = automationCompositionProvider.findAutomationComposition(automationComposition.getInstanceId());
        assertThat(acOpt).isEmpty();

        acOpt = automationCompositionProvider.findAutomationComposition(automationComposition.getKey().asIdentifier());
        assertThat(acOpt).isEmpty();

        when(automationCompositionRepository.findById(automationComposition.getInstanceId().toString()))
                .thenReturn(Optional.of(inputAutomationCompositionsJpa.get(0)));
        acOpt = automationCompositionProvider.findAutomationComposition(automationComposition.getInstanceId());
        assertEquals(automationComposition, acOpt.get());

        when(automationCompositionRepository.findOne(Mockito.<Example<JpaAutomationComposition>>any()))
                .thenReturn(Optional.of(inputAutomationCompositionsJpa.get(0)));
        acOpt = automationCompositionProvider.findAutomationComposition(automationComposition.getKey().asIdentifier());
        assertEquals(automationComposition, acOpt.get());
    }

    @Test
    void testGetAcInstancesByCompositionId() {
        var automationCompositionRepository = mock(AutomationCompositionRepository.class);
        var automationCompositionProvider = new AutomationCompositionProvider(automationCompositionRepository,
                mock(AutomationCompositionElementRepository.class));

        var automationComposition = inputAutomationCompositions.getAutomationCompositionList().get(0);
        when(automationCompositionRepository.findByCompositionId(automationComposition.getCompositionId().toString()))
                .thenReturn(inputAutomationCompositionsJpa);
        var acList =
                automationCompositionProvider.getAcInstancesByCompositionId(automationComposition.getCompositionId());
        assertEquals(inputAutomationCompositions.getAutomationCompositionList(), acList);
    }

    @Test
    void testGetAcInstancesInTransition() {
        inputAutomationCompositions.getAutomationCompositionList().get(0).setDeployState(DeployState.DEPLOYING);
        inputAutomationCompositions.getAutomationCompositionList().get(1).setLockState(LockState.LOCKING);
        inputAutomationCompositionsJpa.get(0).setDeployState(DeployState.DEPLOYING);
        inputAutomationCompositionsJpa.get(1).setLockState(LockState.LOCKING);

        List<JpaAutomationComposition> res1 = new ArrayList<>();
        res1.add(inputAutomationCompositionsJpa.get(0));
        var automationCompositionRepository = mock(AutomationCompositionRepository.class);
        var automationCompositionProvider = new AutomationCompositionProvider(automationCompositionRepository,
            mock(AutomationCompositionElementRepository.class));
        when(automationCompositionRepository.findByDeployStateIn(List.of(DeployState.DEPLOYING,
            DeployState.UNDEPLOYING, DeployState.DELETING, DeployState.UPDATING, DeployState.MIGRATING)))
            .thenReturn(res1);
        when(automationCompositionRepository.findByLockStateIn(List.of(LockState.LOCKING, LockState.UNLOCKING)))
            .thenReturn(List.of(inputAutomationCompositionsJpa.get(1)));
        var acList = automationCompositionProvider.getAcInstancesInTransition();
        assertThat(acList).hasSize(2)
                .contains(inputAutomationCompositions.getAutomationCompositionList().get(0).getInstanceId())
                .contains(inputAutomationCompositions.getAutomationCompositionList().get(1).getInstanceId());
    }

    @Test
    void testDeleteAutomationComposition() {
        var automationCompositionRepository = mock(AutomationCompositionRepository.class);
        var automationCompositionProvider = new AutomationCompositionProvider(automationCompositionRepository,
                mock(AutomationCompositionElementRepository.class));

        assertThatThrownBy(() -> automationCompositionProvider.deleteAutomationComposition(UUID.randomUUID()))
                .hasMessageMatching(".*.failed, automation composition does not exist");

        var automationComposition = inputAutomationCompositions.getAutomationCompositionList().get(0);
        when(automationCompositionRepository.findById(automationComposition.getInstanceId().toString()))
                .thenReturn(Optional.of(inputAutomationCompositionsJpa.get(0)));

        var deletedAc =
                automationCompositionProvider.deleteAutomationComposition(automationComposition.getInstanceId());
        assertEquals(automationComposition, deletedAc);
    }

    @Test
    void testDeleteElementById() {
        var acElementRepository = mock(AutomationCompositionElementRepository.class);
        var automationCompositionProvider = new AutomationCompositionProvider(
                mock(AutomationCompositionRepository.class), acElementRepository);
        assertThatThrownBy(() -> automationCompositionProvider.deleteAutomationCompositionElement(null))
                .hasMessageMatching(ACELEMENT_ID_IS_NULL);
        var elementId = UUID.randomUUID();
        automationCompositionProvider.deleteAutomationCompositionElement(elementId);
        verify(acElementRepository).deleteById(elementId.toString());
    }

    @Test
    void testValidateElementIds() {
        var acElementRepository = mock(AutomationCompositionElementRepository.class);
        var automationCompositionProvider = new AutomationCompositionProvider(
            mock(AutomationCompositionRepository.class), acElementRepository);

        var ac = inputAutomationCompositions.getAutomationCompositionList().get(0);

        var result = automationCompositionProvider.validateElementIds(ac);
        assertThat(result.isValid()).isTrue();

        var jpaElement = new JpaAutomationCompositionElement(ac.getElements().values().iterator().next());
        when(acElementRepository.findAllById(anyIterable()))
            .thenReturn(List.of(jpaElement));

        ac.setInstanceId(null);
        result = automationCompositionProvider.validateElementIds(ac);
        assertThat(result.isValid()).isFalse();

        ac.setInstanceId(UUID.randomUUID());
        jpaElement.setInstanceId(UUID.randomUUID().toString());
        result = automationCompositionProvider.validateElementIds(ac);
        assertThat(result.isValid()).isFalse();

        ac.setInstanceId(UUID.randomUUID());
        jpaElement.setInstanceId(ac.getInstanceId().toString());
        result = automationCompositionProvider.validateElementIds(ac);
        assertThat(result.isValid()).isTrue();
    }
}
