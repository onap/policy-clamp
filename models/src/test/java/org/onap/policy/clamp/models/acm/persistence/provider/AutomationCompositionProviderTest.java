/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaAutomationComposition;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaAutomationCompositionElement;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaAutomationCompositionRollback;
import org.onap.policy.clamp.models.acm.persistence.repository.AutomationCompositionElementRepository;
import org.onap.policy.clamp.models.acm.persistence.repository.AutomationCompositionRepository;
import org.onap.policy.clamp.models.acm.persistence.repository.AutomationCompositionRollbackRepository;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
        when(automationCompositionRepository.save(any(JpaAutomationComposition.class)))
            .thenReturn(inputAutomationCompositionsJpa.get(0));
        var inputAc = inputAutomationCompositions.getAutomationCompositionList().get(0);

        var automationCompositionProvider = new AutomationCompositionProvider(
                automationCompositionRepository, mock(AutomationCompositionElementRepository.class),
                mock(AutomationCompositionRollbackRepository.class));
        var createdAutomationComposition = automationCompositionProvider.createAutomationComposition(inputAc);
        inputAc.setInstanceId(createdAutomationComposition.getInstanceId());
        inputAc.setLastMsg(createdAutomationComposition.getLastMsg());
        assertEquals(inputAc, createdAutomationComposition);
    }

    @Test
    void testAutomationCompositionUpdate() {
        var automationCompositionRepository = mock(AutomationCompositionRepository.class);
        var automationCompositionProvider = new AutomationCompositionProvider(
                automationCompositionRepository, mock(AutomationCompositionElementRepository.class),
                mock(AutomationCompositionRollbackRepository.class));
        assertThatThrownBy(() -> automationCompositionProvider.updateAutomationComposition(null))
            .hasMessageMatching(AC_IS_NULL);

        when(automationCompositionRepository.save(inputAutomationCompositionsJpa.get(0)))
            .thenReturn(inputAutomationCompositionsJpa.get(0));

        var createdAutomationComposition = automationCompositionProvider
            .updateAutomationComposition(inputAutomationCompositions.getAutomationCompositionList().get(0));

        assertEquals(inputAutomationCompositions.getAutomationCompositionList().get(0), createdAutomationComposition);
    }

    @Test
    void testGetAutomationCompositionsWithNull() {
        var automationCompositionProvider = new AutomationCompositionProvider(
                mock(AutomationCompositionRepository.class), mock(AutomationCompositionElementRepository.class),
                mock(AutomationCompositionRollbackRepository.class));
        assertThatThrownBy(() -> automationCompositionProvider
            .getAutomationCompositions(UUID.randomUUID(), null, null, null))
            .hasMessage("pageable is marked non-null but is null");

        assertThatThrownBy(() -> automationCompositionProvider
            .getAutomationCompositions(null, null, null, Pageable.unpaged()))
            .hasMessage("compositionId is marked non-null but is null");
    }

    @Test
    void testGetAutomationCompositions() {
        var automationCompositionRepository = mock(AutomationCompositionRepository.class);
        var automationCompositionProvider = new AutomationCompositionProvider(
                automationCompositionRepository, mock(AutomationCompositionElementRepository.class),
                mock(AutomationCompositionRollbackRepository.class));
        var automationComposition = inputAutomationCompositions.getAutomationCompositionList().get(0);
        when(automationCompositionRepository
            .findAll(Mockito.any(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(inputAutomationCompositionsJpa));
        var acList = automationCompositionProvider.getAutomationCompositions(UUID.randomUUID(),
            automationComposition.getName(), automationComposition.getVersion(), Pageable.unpaged());
        assertThat(acList).hasSize(2);

        acList = automationCompositionProvider.getAutomationCompositions(automationComposition.getCompositionId(), null,
            null, Pageable.unpaged());
        assertThat(acList).hasSize(2);

        when(automationCompositionRepository
            .findAll(Mockito.any(), Mockito.any(Pageable.class)))
            .thenReturn(new PageImpl<>(inputAutomationCompositionsJpa));
        acList = automationCompositionProvider.getAutomationCompositions(automationComposition.getCompositionId(), null,
            null, PageRequest.of(0, 10));
        assertThat(acList).hasSize(2);
    }

    @Test
    void testGetAutomationComposition() {
        var automationComposition = inputAutomationCompositions.getAutomationCompositionList().get(0);
        var automationCompositionRepository = mock(AutomationCompositionRepository.class);
        var automationCompositionProvider = new AutomationCompositionProvider(
                automationCompositionRepository, mock(AutomationCompositionElementRepository.class),
                mock(AutomationCompositionRollbackRepository.class));
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
        var automationComposition = inputAutomationCompositions.getAutomationCompositionList().get(0);
        var automationCompositionRepository = mock(AutomationCompositionRepository.class);
        var automationCompositionProvider = new AutomationCompositionProvider(
                automationCompositionRepository, mock(AutomationCompositionElementRepository.class),
                mock(AutomationCompositionRollbackRepository.class));
        var acOpt = automationCompositionProvider.findAutomationComposition(automationComposition.getInstanceId());
        assertThat(acOpt).isEmpty();

        when(automationCompositionRepository.findById(automationComposition.getInstanceId().toString()))
                .thenReturn(Optional.of(inputAutomationCompositionsJpa.get(0)));
        acOpt = automationCompositionProvider.findAutomationComposition(automationComposition.getInstanceId());
        assertTrue(acOpt.isPresent());
        assertEquals(automationComposition, acOpt.get());
    }

    @Test
    void testGetAcInstancesByCompositionId() {
        var automationComposition = inputAutomationCompositions.getAutomationCompositionList().get(0);
        var automationCompositionRepository = mock(AutomationCompositionRepository.class);
        when(automationCompositionRepository.findByCompositionId(automationComposition.getCompositionId().toString()))
            .thenReturn(inputAutomationCompositionsJpa);
        var automationCompositionProvider = new AutomationCompositionProvider(
                automationCompositionRepository, mock(AutomationCompositionElementRepository.class),
                mock(AutomationCompositionRollbackRepository.class));
        var acList =
            automationCompositionProvider.getAcInstancesByCompositionId(automationComposition.getCompositionId());
        assertEquals(inputAutomationCompositions.getAutomationCompositionList(), acList);
    }

    @Test
    void testGetAcInstancesByTargetCompositionId() {
        var automationComposition = inputAutomationCompositions.getAutomationCompositionList().get(0);
        var automationCompositionRepository = mock(AutomationCompositionRepository.class);
        when(automationCompositionRepository.findByCompositionTargetId(any()))
                .thenReturn(List.of(inputAutomationCompositionsJpa.get(0)));
        var automationCompositionProvider = new AutomationCompositionProvider(
                automationCompositionRepository, mock(AutomationCompositionElementRepository.class),
                mock(AutomationCompositionRollbackRepository.class));
        var acList = automationCompositionProvider
                .getAcInstancesByTargetCompositionId(automationComposition.getCompositionTargetId());
        assertEquals(inputAutomationCompositions.getAutomationCompositionList().get(0), acList.get(0));
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
        when(automationCompositionRepository.findByDeployStateIn(List.of(DeployState.DEPLOYING,
            DeployState.UNDEPLOYING, DeployState.DELETING, DeployState.UPDATING, DeployState.MIGRATING,
                DeployState.MIGRATION_REVERTING)))
            .thenReturn(res1);
        when(automationCompositionRepository.findByLockStateIn(List.of(LockState.LOCKING, LockState.UNLOCKING)))
            .thenReturn(List.of(inputAutomationCompositionsJpa.get(1)));
        var automationCompositionProvider = new AutomationCompositionProvider(
                automationCompositionRepository, mock(AutomationCompositionElementRepository.class),
                mock(AutomationCompositionRollbackRepository.class));
        var acList = automationCompositionProvider.getAcInstancesInTransition();
        assertThat(acList).hasSize(2)
            .contains(inputAutomationCompositions.getAutomationCompositionList().get(0).getInstanceId())
            .contains(inputAutomationCompositions.getAutomationCompositionList().get(1).getInstanceId());
    }

    @Test
    void testDeleteAutomationComposition() {
        var automationCompositionRepository = mock(AutomationCompositionRepository.class);
        var acRollbackRepository = mock(AutomationCompositionRollbackRepository.class);
        var automationCompositionProvider = new AutomationCompositionProvider(
                automationCompositionRepository, mock(AutomationCompositionElementRepository.class),
                acRollbackRepository);
        assertThatThrownBy(() -> automationCompositionProvider.deleteAutomationComposition(UUID.randomUUID()))
            .hasMessageMatching(".*.failed, automation composition does not exist");

        assertThatThrownBy(() -> automationCompositionProvider.deleteAutomationComposition(null))
                .hasMessageMatching("instanceId is marked non-null but is null");

        var automationComposition = inputAutomationCompositions.getAutomationCompositionList().get(0);
        when(automationCompositionRepository.findById(automationComposition.getInstanceId().toString()))
            .thenReturn(Optional.of(inputAutomationCompositionsJpa.get(0)));

        var deletedAc =
            automationCompositionProvider.deleteAutomationComposition(automationComposition.getInstanceId());
        assertEquals(automationComposition, deletedAc);

        when(acRollbackRepository.existsById(automationComposition.getInstanceId().toString())).thenReturn(true);

        deletedAc =
                automationCompositionProvider.deleteAutomationComposition(automationComposition.getInstanceId());
        assertEquals(automationComposition, deletedAc);
        verify(acRollbackRepository).deleteById(automationComposition.getInstanceId().toString());
    }

    @Test
    void testDeleteElementById() {
        var automationCompositionRepository = mock(AutomationCompositionRepository.class);
        var acElementRepository = mock(AutomationCompositionElementRepository.class);
        var automationCompositionProvider = new AutomationCompositionProvider(
                automationCompositionRepository, acElementRepository,
                mock(AutomationCompositionRollbackRepository.class));
        assertThatThrownBy(() -> automationCompositionProvider.deleteAutomationCompositionElement(null))
            .hasMessageMatching(ACELEMENT_ID_IS_NULL);
        var elementId = UUID.randomUUID();
        automationCompositionProvider.deleteAutomationCompositionElement(elementId);
        verify(acElementRepository).deleteById(elementId.toString());
    }

    @Test
    void testValidateElementIds() {
        var ac = inputAutomationCompositions.getAutomationCompositionList().get(0);

        var acElementRepository = mock(AutomationCompositionElementRepository.class);
        var automationCompositionProvider = new AutomationCompositionProvider(
                mock(AutomationCompositionRepository.class), acElementRepository,
                mock(AutomationCompositionRollbackRepository.class));
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

    @Test
    void testCopyAcElements() {
        var ac = inputAutomationCompositions.getAutomationCompositionList().get(0);
        var automationCompositionRepository = mock(AutomationCompositionRepository.class);
        var acRollbackRepository = mock(AutomationCompositionRollbackRepository.class);
        var automationCompositionProvider = new AutomationCompositionProvider(
                automationCompositionRepository, mock(AutomationCompositionElementRepository.class),
                acRollbackRepository);
        automationCompositionProvider.copyAcElementsBeforeUpdate(ac);

        verify(acRollbackRepository).save(any(JpaAutomationCompositionRollback.class));
        var instanceId = ac.getInstanceId();
        assertThrows(PfModelRuntimeException.class, () -> automationCompositionProvider
            .getAutomationCompositionRollback(instanceId));
    }

    @Test
    void testGetRollbackSuccess() {
        var ac = inputAutomationCompositions.getAutomationCompositionList().get(0);
        var rollback = new JpaAutomationCompositionRollback();
        rollback.setInstanceId(ac.getInstanceId().toString());
        rollback.setCompositionId(ac.getCompositionId().toString());

        var acRollbackRepository = mock(AutomationCompositionRollbackRepository.class);
        when(acRollbackRepository.findById(anyString())).thenReturn(Optional.of(rollback));

        var automationCompositionProvider = new AutomationCompositionProvider(
                mock(AutomationCompositionRepository.class), mock(AutomationCompositionElementRepository.class),
                acRollbackRepository);
        var rbFromDb = automationCompositionProvider.getAutomationCompositionRollback(ac.getInstanceId());
        assertNotNull(rbFromDb);
    }

    @Test
    void testGetRollbackEmpty() {
        var acRollbackRepository = mock(AutomationCompositionRollbackRepository.class);
        var automationCompositionProvider = new AutomationCompositionProvider(
                mock(AutomationCompositionRepository.class), mock(AutomationCompositionElementRepository.class),
                acRollbackRepository);
        when(acRollbackRepository.findById(anyString())).thenReturn(Optional.empty());
        var compositionId = UUID.randomUUID();
        assertThrows(PfModelRuntimeException.class, () -> automationCompositionProvider
            .getAutomationCompositionRollback(compositionId));
    }

    @Test
    void testVersionCompatibility() {
        // Identical
        var newDefinition = new PfConceptKey("policy.clamp.element", "1.2.3");
        var oldDefinition = new PfConceptKey("policy.clamp.element", "1.2.3");

        var instanceId = UUID.randomUUID();
        assertDoesNotThrow(() ->
                AutomationCompositionProvider.checkCompatibility(newDefinition, oldDefinition, instanceId));

        // Patch
        newDefinition.setVersion("1.2.4");
        assertDoesNotThrow(() ->
                AutomationCompositionProvider.checkCompatibility(newDefinition, oldDefinition, instanceId));

        // Minor
        newDefinition.setVersion("1.3.1");
        assertDoesNotThrow(() ->
                AutomationCompositionProvider.checkCompatibility(newDefinition, oldDefinition, instanceId));

        // Major
        newDefinition.setVersion("2.1.1");
        assertDoesNotThrow(() ->
                AutomationCompositionProvider.checkCompatibility(newDefinition, oldDefinition, instanceId));

        // Not compatible
        newDefinition.setName("policy.clamp.newElement");
        newDefinition.setVersion("2.2.4");
        assertThatThrownBy(() -> AutomationCompositionProvider
                .checkCompatibility(newDefinition, oldDefinition, instanceId))
                .hasMessageContaining("is not compatible");
    }

    @Test
    void testValidateNameVersion() {
        var automationCompositionRepository = mock(AutomationCompositionRepository.class);
        var automationCompositionProvider = new AutomationCompositionProvider(
                automationCompositionRepository, mock(AutomationCompositionElementRepository.class),
                mock(AutomationCompositionRollbackRepository.class));

        var acIdentifier = new ToscaConceptIdentifier();
        assertDoesNotThrow(() -> {
            automationCompositionProvider.validateNameVersion(acIdentifier);
        });

        when(automationCompositionRepository.findOne(Mockito.any()))
                .thenReturn(Optional.of(inputAutomationCompositionsJpa.get(0)));
        assertThatThrownBy(() -> {
            automationCompositionProvider.validateNameVersion(acIdentifier);
        }).hasMessageContaining("already defined");
    }

    @Test
    void testValidateInstanceEndpoint() {
        var automationComposition = new AutomationComposition();
        automationComposition.setCompositionId(UUID.randomUUID());

        var compositionId = automationComposition.getCompositionId();
        assertDoesNotThrow(() -> AutomationCompositionProvider
                .validateInstanceEndpoint(compositionId, automationComposition));

        var wrongCompositionId = UUID.randomUUID();
        assertThatThrownBy(() -> AutomationCompositionProvider
                .validateInstanceEndpoint(wrongCompositionId, automationComposition))
                .hasMessageContaining("do not match with");
    }

    @Test
    void testGetAcInstancesByFilter_WithoutInstanceIds() {
        Page<JpaAutomationComposition> mockPage = new PageImpl<>(inputAutomationCompositionsJpa);
        var acRepository = mock(AutomationCompositionRepository.class);
        when(acRepository.findByStateChangeResultIn(anyCollection(), any(Pageable.class)))
            .thenReturn(mockPage);
        when(acRepository.findByDeployStateIn(anyCollection(), any(Pageable.class)))
            .thenReturn(mockPage);

        var acIds = new ArrayList<String>();
        var stateChangeResults = new ArrayList<StateChangeResult>();
        var deployStates = new ArrayList<DeployState>();
        var pageable = Pageable.unpaged();
        when(acRepository.findByStateChangeResultInAndDeployStateIn(
            stateChangeResults, deployStates, pageable)).thenReturn(mockPage);
        when(acRepository.findAll(any(Pageable.class))).thenReturn(mockPage);
        var acProvider = new AutomationCompositionProvider(acRepository,
            mock(AutomationCompositionElementRepository.class), mock(AutomationCompositionRollbackRepository.class));

        var acInstances = acProvider.getAcInstancesByFilter(acIds, stateChangeResults, deployStates, pageable);
        assertNotNull(acInstances);
        assertEquals(2, acInstances.size());

        stateChangeResults.add(StateChangeResult.NO_ERROR);
        acInstances = acProvider.getAcInstancesByFilter(acIds, stateChangeResults, deployStates, pageable);
        assertNotNull(acInstances);

        deployStates.add(DeployState.DEPLOYED);
        acInstances = acProvider.getAcInstancesByFilter(acIds, stateChangeResults, deployStates, pageable);
        assertNotNull(acInstances);

        stateChangeResults.clear();
        acInstances = acProvider.getAcInstancesByFilter(acIds, stateChangeResults, deployStates, pageable);
        assertNotNull(acInstances);
    }

    @Test
    void testGetAcInstancesByFilter_WithInstanceIds() {
        Page<JpaAutomationComposition> mockPage = new PageImpl<>(inputAutomationCompositionsJpa);
        var acRepository = mock(AutomationCompositionRepository.class);

        var acIds = new ArrayList<String>();
        var stateChangeResults = new ArrayList<StateChangeResult>();
        var deployStates = new ArrayList<DeployState>();
        var pageable = Pageable.unpaged();

        when(acRepository.findByInstanceIdInAndStateChangeResultInAndDeployStateIn(
            acIds, stateChangeResults, deployStates, pageable)).thenReturn(mockPage);
        when(acRepository.findByInstanceIdInAndStateChangeResultIn(acIds, stateChangeResults, pageable))
            .thenReturn(mockPage);
        when(acRepository.findByInstanceIdInAndDeployStateIn(acIds, deployStates, pageable)).thenReturn(mockPage);
        when(acRepository.findByInstanceIdIn(acIds, pageable)).thenReturn(mockPage);

        var acProvider = new AutomationCompositionProvider(acRepository,
            mock(AutomationCompositionElementRepository.class), mock(AutomationCompositionRollbackRepository.class));

        acIds.add(inputAutomationCompositions.getAutomationCompositionList().get(0).getCompositionId().toString());

        var acInstances = acProvider.getAcInstancesByFilter(acIds, stateChangeResults, deployStates, pageable);
        assertNotNull(acInstances);
        assertEquals(2, acInstances.size());

        stateChangeResults.add(StateChangeResult.NO_ERROR);
        acInstances = acProvider.getAcInstancesByFilter(acIds, stateChangeResults, deployStates, pageable);
        assertNotNull(acInstances);

        deployStates.add(DeployState.DEPLOYED);
        acInstances = acProvider.getAcInstancesByFilter(acIds, stateChangeResults, deployStates, pageable);
        assertNotNull(acInstances);

        stateChangeResults.clear();
        acInstances = acProvider.getAcInstancesByFilter(acIds, stateChangeResults, deployStates, pageable);
        assertNotNull(acInstances);
    }

    @Test
    void testGetAcInstancesByFilterWithNull() {
        var provider = new AutomationCompositionProvider(mock(AutomationCompositionRepository.class),
            mock(AutomationCompositionElementRepository.class), mock(AutomationCompositionRollbackRepository.class));
        var acIds = new ArrayList<String>();
        var stateChangeResults = new ArrayList<StateChangeResult>();
        var deployStates = new ArrayList<DeployState>();
        var pageable = Pageable.unpaged();
        assertThrows(NullPointerException.class, () ->
            provider.getAcInstancesByFilter(null, stateChangeResults, deployStates, pageable));
        assertThrows(NullPointerException.class, () ->
            provider.getAcInstancesByFilter(acIds, null, deployStates, pageable));
        assertThrows(NullPointerException.class, () ->
            provider.getAcInstancesByFilter(acIds, stateChangeResults, null, pageable));
        assertThrows(NullPointerException.class, () ->
            provider.getAcInstancesByFilter(acIds, stateChangeResults, deployStates, null));
    }
}
