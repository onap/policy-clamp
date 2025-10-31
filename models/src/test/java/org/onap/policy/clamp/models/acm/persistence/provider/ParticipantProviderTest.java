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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions;
import org.onap.policy.clamp.models.acm.concepts.NodeTemplateState;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaAutomationComposition;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaNodeTemplateState;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaParticipant;
import org.onap.policy.clamp.models.acm.persistence.repository.AutomationCompositionElementRepository;
import org.onap.policy.clamp.models.acm.persistence.repository.NodeTemplateStateRepository;
import org.onap.policy.clamp.models.acm.persistence.repository.ParticipantReplicaRepository;
import org.onap.policy.clamp.models.acm.persistence.repository.ParticipantRepository;
import org.onap.policy.clamp.models.acm.utils.CommonTestData;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class ParticipantProviderTest {

    private static final Coder CODER = new StandardCoder();
    private static final String PARTICIPANT_JSON = "src/test/resources/providers/TestParticipant.json";

    private static final String AUTOMATION_COMPOSITION_JSON =
        "src/test/resources/providers/TestAutomationCompositions.json";

    private static final String NODE_TEMPLATE_STATE_JSON = "src/test/resources/providers/NodeTemplateState.json";
    private static final String LIST_IS_NULL = ".*. is marked .*ull but is null";
    private static final UUID INVALID_ID = UUID.randomUUID();

    private final List<Participant> inputParticipants = new ArrayList<>();
    private List<JpaParticipant> jpaParticipantList;
    private List<JpaAutomationComposition> inputAutomationCompositionsJpa;

    private final List<NodeTemplateState> nodeTemplateStateList = new ArrayList<>();
    private List<JpaNodeTemplateState> jpaNodeTemplateStateList;

    @BeforeEach
    void beforeSetup() throws Exception {
        var originalJson = ResourceUtils.getResourceAsString(PARTICIPANT_JSON);
        inputParticipants.add(CODER.decode(originalJson, Participant.class));
        jpaParticipantList = ProviderUtils.getJpaAndValidateList(inputParticipants, JpaParticipant::new, "participant");

        var originalAcJson = ResourceUtils.getResourceAsString(AUTOMATION_COMPOSITION_JSON);
        var inputAutomationCompositions = CODER.decode(originalAcJson, AutomationCompositions.class);
        inputAutomationCompositionsJpa =
            ProviderUtils.getJpaAndValidateList(inputAutomationCompositions.getAutomationCompositionList(),
                JpaAutomationComposition::new, "automation compositions");

        var nodeTemplateStatesJson = ResourceUtils.getResourceAsString(NODE_TEMPLATE_STATE_JSON);
        nodeTemplateStateList.add(CODER.decode(nodeTemplateStatesJson, NodeTemplateState.class));
        nodeTemplateStateList.get(0).setState(AcTypeState.COMMISSIONED);
        jpaNodeTemplateStateList = ProviderUtils.getJpaAndValidateList(nodeTemplateStateList,
            JpaNodeTemplateState::new, "node template state");
    }

    @Test
    void testParticipantSave() {
        var participantRepository = mock(ParticipantRepository.class);
        var automationCompositionElementRepository = mock(AutomationCompositionElementRepository.class);
        var nodeTemplateStateRepository = mock(NodeTemplateStateRepository.class);

        var participantProvider = new ParticipantProvider(participantRepository,
            automationCompositionElementRepository, nodeTemplateStateRepository,
            mock(ParticipantReplicaRepository.class));

        assertThatThrownBy(() -> participantProvider.saveParticipant(null)).hasMessageMatching(LIST_IS_NULL);

        when(participantRepository.save(any())).thenReturn(jpaParticipantList.get(0));

        var savedParticipant = participantProvider.saveParticipant(inputParticipants.get(0));
        savedParticipant.setParticipantId(inputParticipants.get(0).getParticipantId());

        assertThat(savedParticipant).usingRecursiveComparison().isEqualTo(inputParticipants.get(0));
    }

    @Test
    void testGetAutomationCompositions() {
        var participantRepository = mock(ParticipantRepository.class);
        var automationCompositionElementRepository = mock(AutomationCompositionElementRepository.class);
        var nodeTemplateStateRepository = mock(NodeTemplateStateRepository.class);
        var participantProvider = new ParticipantProvider(participantRepository,
            automationCompositionElementRepository, nodeTemplateStateRepository,
            mock(ParticipantReplicaRepository.class));

        assertThat(participantProvider.findParticipant(INVALID_ID)).isEmpty();

        when(participantRepository.findAll()).thenReturn(jpaParticipantList);
        assertThat(participantProvider.getParticipants()).hasSize(inputParticipants.size());

        assertThatThrownBy(() -> participantProvider.getParticipantById(inputParticipants.get(0).getParticipantId()))
                .hasMessageMatching("Participant Not Found with ID: " + inputParticipants.get(0).getParticipantId());

        when(participantRepository.findById(any())).thenReturn(Optional.ofNullable(jpaParticipantList.get(0)));

        var participant = participantProvider.getParticipantById(inputParticipants.get(0).getParticipantId());

        assertThat(inputParticipants.get(0)).usingRecursiveComparison().isEqualTo(participant);
    }

    @Test
    void testEmptyParticipant() {
        var participantRepository = mock(ParticipantRepository.class);
        var automationCompositionElementRepository = mock(AutomationCompositionElementRepository.class);
        var nodeTemplateStateRepository = mock(NodeTemplateStateRepository.class);
        var participantProvider = new ParticipantProvider(participantRepository,
            automationCompositionElementRepository, nodeTemplateStateRepository,
            mock(ParticipantReplicaRepository.class));

        assertThatThrownBy(() -> participantProvider.getParticipantById(INVALID_ID)).isInstanceOf(
            PfModelRuntimeException.class).hasMessageMatching("Participant Not Found with ID:.*.");
    }

    @Test
    void testGetAutomationCompositionElements() {
        var participantRepository = mock(ParticipantRepository.class);
        var automationCompositionElementRepository = mock(AutomationCompositionElementRepository.class);
        var nodeTemplateStateRepository = mock(NodeTemplateStateRepository.class);
        var participantProvider = new ParticipantProvider(participantRepository,
            automationCompositionElementRepository, nodeTemplateStateRepository,
            mock(ParticipantReplicaRepository.class));

        var acElementList = inputAutomationCompositionsJpa.get(0).getElements();

        var participantId = UUID.randomUUID();
        var pageable = PageRequest.of(0, 5);
        when(automationCompositionElementRepository.findByParticipantId(participantId.toString(), pageable))
                .thenReturn(acElementList);

        var listOfAcElements = participantProvider.getAutomationCompositionElements(participantId, pageable);

        assertThat(listOfAcElements).hasSameSizeAs(acElementList);
        assertEquals(UUID.fromString(acElementList.get(0).getElementId()), listOfAcElements.get(0).getId());
    }

    @Test
    void testGetAcNodeTemplateState() {
        var participantRepository = mock(ParticipantRepository.class);
        var automationCompositionElementRepository = mock(AutomationCompositionElementRepository.class);
        var nodeTemplateStateRepository = mock(NodeTemplateStateRepository.class);
        var participantId = jpaParticipantList.get(0).getParticipantId();
        var pageable = PageRequest.of(0, 5);
        when(nodeTemplateStateRepository
            .findByParticipantId(participantId, pageable)).thenReturn(jpaNodeTemplateStateList);

        var participantProvider = new ParticipantProvider(participantRepository,
            automationCompositionElementRepository, nodeTemplateStateRepository,
            mock(ParticipantReplicaRepository.class));

        var listOfNodeTemplateState =
            participantProvider.getAcNodeTemplateStates(UUID.fromString(participantId), pageable);

        assertEquals(listOfNodeTemplateState, nodeTemplateStateList);
    }

    @Test
    void testNotNullExceptions() {
        var participantRepository = mock(ParticipantRepository.class);
        var automationCompositionElementRepository = mock(AutomationCompositionElementRepository.class);
        var nodeTemplateStateRepository = mock(NodeTemplateStateRepository.class);

        var participantProvider = new ParticipantProvider(participantRepository,
            automationCompositionElementRepository, nodeTemplateStateRepository,
            mock(ParticipantReplicaRepository.class));

        assertThrows(NullPointerException.class, () -> participantProvider.getParticipantById(null));
        assertThrows(NullPointerException.class, () -> participantProvider.findParticipant(null));
        assertThrows(NullPointerException.class, () -> participantProvider.saveParticipant(null));

        var pageable = Pageable.unpaged();
        assertThrows(NullPointerException.class, () ->
                participantProvider.getAutomationCompositionElements(null, pageable));
        var participantId = UUID.randomUUID();
        assertThrows(NullPointerException.class, () ->
                participantProvider.getAutomationCompositionElements(participantId, null));
        assertThrows(NullPointerException.class, () ->
                participantProvider.getAcNodeTemplateStates(null, pageable));
        assertThrows(NullPointerException.class, () ->
                participantProvider.getAcNodeTemplateStates(participantId, null));

        assertThrows(NullPointerException.class, () -> participantProvider.findParticipantReplica(null));
        assertThrows(NullPointerException.class, () -> participantProvider.saveParticipantReplica(null));
        assertThrows(NullPointerException.class, () -> participantProvider.deleteParticipantReplica(null));
        assertThrows(NullPointerException.class, () ->
                participantProvider.getAutomationCompositionElements(null, pageable));
        assertThrows(NullPointerException.class, () ->
                participantProvider.getAutomationCompositionElements(participantId, null));
        assertThrows(NullPointerException.class, () -> participantProvider.getAcNodeTemplateStates(null, null));
    }

    @Test
    void testGetSupportedElementMap() {
        var participantRepository = mock(ParticipantRepository.class);
        var automationCompositionElementRepository = mock(AutomationCompositionElementRepository.class);
        var nodeTemplateStateRepository = mock(NodeTemplateStateRepository.class);
        when(participantRepository.findAll()).thenReturn(jpaParticipantList);
        var participantProvider = new ParticipantProvider(participantRepository,
            automationCompositionElementRepository, nodeTemplateStateRepository,
            mock(ParticipantReplicaRepository.class));

        var result = participantProvider.getSupportedElementMap();
        assertThat(result).hasSize(2);
    }

    @Test
    void testGetCompositionIds() {
        var nodeTemplateStateRepository = mock(NodeTemplateStateRepository.class);
        var participantId = UUID.randomUUID();
        when(nodeTemplateStateRepository.findByParticipantId(participantId.toString()))
                .thenReturn(jpaNodeTemplateStateList);
        var participantRepository = mock(ParticipantRepository.class);
        var automationCompositionElementRepository = mock(AutomationCompositionElementRepository.class);

        var participantProvider = new ParticipantProvider(participantRepository,
            automationCompositionElementRepository, nodeTemplateStateRepository,
            mock(ParticipantReplicaRepository.class));

        assertThatThrownBy(() -> participantProvider.getCompositionIds(null)).hasMessageMatching(LIST_IS_NULL);

        var result = participantProvider.getCompositionIds(participantId);
        assertThat(result).hasSize(1);
    }

    @Test
    void testFindParticipantReplica() {
        var replicaRepository = mock(ParticipantReplicaRepository.class);
        var replica = inputParticipants.get(0).getReplicas().values().iterator().next();
        var jpaReplica = jpaParticipantList.get(0).getReplicas().get(0);
        when(replicaRepository.findById(replica.getReplicaId().toString())).thenReturn(Optional.of(jpaReplica));
        var participantProvider = new ParticipantProvider(mock(ParticipantRepository.class),
                mock(AutomationCompositionElementRepository.class), mock(NodeTemplateStateRepository.class),
                replicaRepository);

        var result = participantProvider.findParticipantReplica(replica.getReplicaId());
        assertThat(result).isNotEmpty();
        assertEquals(replica.getReplicaId(), result.get().getReplicaId());
    }

    @Test
    void testFindReplicasOnLine() {
        var replicaRepository = mock(ParticipantReplicaRepository.class);
        var replica = inputParticipants.get(0).getReplicas().values().iterator().next();
        var jpaReplica = jpaParticipantList.get(0).getReplicas().get(0);
        jpaReplica.fromAuthorative(replica);
        when(replicaRepository.findByParticipantState(ParticipantState.ON_LINE)).thenReturn(List.of(jpaReplica));
        var participantProvider = new ParticipantProvider(mock(ParticipantRepository.class),
                mock(AutomationCompositionElementRepository.class), mock(NodeTemplateStateRepository.class),
                replicaRepository);

        var result = participantProvider.findReplicasOnLine();
        assertThat(result).hasSize(1);
        assertEquals(replica.getReplicaId(), result.get(0).getReplicaId());
    }

    @Test
    void testSaveParticipantReplica() {
        var jpaReplica = jpaParticipantList.get(0).getReplicas().get(0);
        var replicaRepository = mock(ParticipantReplicaRepository.class);
        when(replicaRepository.getReferenceById(jpaReplica.getReplicaId())).thenReturn(jpaReplica);
        var participantProvider = new ParticipantProvider(mock(ParticipantRepository.class),
                mock(AutomationCompositionElementRepository.class), mock(NodeTemplateStateRepository.class),
                replicaRepository);

        var replica = inputParticipants.get(0).getReplicas().values().iterator().next();
        participantProvider.saveParticipantReplica(replica);
        verify(replicaRepository).save(any());
    }

    @Test
    void testDeleteParticipantReplica() {
        var replicaRepository = mock(ParticipantReplicaRepository.class);
        var participantProvider = new ParticipantProvider(mock(ParticipantRepository.class),
                mock(AutomationCompositionElementRepository.class), mock(NodeTemplateStateRepository.class),
                replicaRepository);
        participantProvider.deleteParticipantReplica(CommonTestData.getReplicaId());
        verify(replicaRepository).deleteById(CommonTestData.getReplicaId().toString());
    }

    @Test
    void testVerifyParticipantState() {
        var jpaParticipant = new JpaParticipant(jpaParticipantList.get(0));
        var participantId = jpaParticipant.getParticipantId();
        var participantRepository = mock(ParticipantRepository.class);
        when(participantRepository.getReferenceById(participantId)).thenReturn(jpaParticipant);

        var replicaRepository = mock(ParticipantReplicaRepository.class);
        var participantProvider = new ParticipantProvider(participantRepository,
                mock(AutomationCompositionElementRepository.class), mock(NodeTemplateStateRepository.class),
                replicaRepository);

        jpaParticipant.setReplicas(List.of());
        var set = Set.of(UUID.fromString(participantId));
        assertThatThrownBy(() -> participantProvider.verifyParticipantState(set))
                .hasMessageMatching("Participant: " + participantId + " is OFFLINE");

        when(participantRepository.getReferenceById(participantId)).thenReturn(jpaParticipantList.get(0));
        participantProvider.verifyParticipantState(set);
        verify(participantRepository, times(2)).getReferenceById(participantId);
    }

    @Test
    void testCheckRegisteredParticipant() {
        var jpaParticipant = new JpaParticipant(jpaParticipantList.get(0));
        var participantId = jpaParticipant.getParticipantId();
        var participantRepository = mock(ParticipantRepository.class);
        when(participantRepository.getReferenceById(participantId)).thenReturn(jpaParticipant);

        var acDefinition = new AutomationCompositionDefinition();
        var nodeTemplateState = new NodeTemplateState();
        nodeTemplateState.setNodeTemplateId(new ToscaConceptIdentifier("name", "0.0.0"));
        nodeTemplateState.setParticipantId(UUID.fromString(participantId));
        acDefinition.setElementStateMap(Map.of(nodeTemplateState.getNodeTemplateId().getName(), nodeTemplateState));

        var replicaRepository = mock(ParticipantReplicaRepository.class);
        var participantProvider = new ParticipantProvider(participantRepository,
                mock(AutomationCompositionElementRepository.class), mock(NodeTemplateStateRepository.class),
                replicaRepository);
        participantProvider.checkRegisteredParticipant(acDefinition);
        verify(participantRepository).getReferenceById(participantId);
    }
}
