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

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.NodeTemplateState;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.concepts.ParticipantReplica;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaParticipant;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaParticipantReplica;
import org.onap.policy.clamp.models.acm.persistence.repository.AutomationCompositionElementRepository;
import org.onap.policy.clamp.models.acm.persistence.repository.NodeTemplateStateRepository;
import org.onap.policy.clamp.models.acm.persistence.repository.ParticipantReplicaRepository;
import org.onap.policy.clamp.models.acm.persistence.repository.ParticipantRepository;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class provides information on participant concepts in the database to callers.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ParticipantProvider {

    private final ParticipantRepository participantRepository;

    private final AutomationCompositionElementRepository automationCompositionElementRepository;

    private final NodeTemplateStateRepository nodeTemplateStateRepository;

    private final ParticipantReplicaRepository replicaRepository;

    /**
     * Get all participants.
     *
     * @return the participants found
     */
    @Transactional(readOnly = true)
    public List<Participant> getParticipants() {
        return ProviderUtils.asEntityList(participantRepository.findAll());
    }

    /**
     * Get participant.
     *
     * @param participantId the id of the participant to get
     * @return the participant found
     */
    @Transactional(readOnly = true)
    public Participant getParticipantById(UUID participantId) {
        var participant = participantRepository.findById(participantId.toString());
        if (participant.isEmpty()) {
            throw new PfModelRuntimeException(Status.NOT_FOUND,
                "Participant Not Found with ID: " + participantId);
        } else {
            return participant.get().toAuthorative();
        }
    }

    /**
     * Get participant.
     *
     * @param participantId the Id of the participant to get
     * @return the participant found
     */
    @Transactional(readOnly = true)
    public Optional<Participant> findParticipant(@NonNull final UUID participantId) {
        return participantRepository.findById(participantId.toString()).map(JpaParticipant::toAuthorative);
    }

    /**
     * Saves participant.
     *
     * @param participant participant to save
     * @return the participant created
     */
    public Participant saveParticipant(@NonNull final Participant participant) {
        var result = participantRepository
            .save(ProviderUtils.getJpaAndValidate(participant, JpaParticipant::new, "participant"));

        // Return the saved participant
        return result.toAuthorative();
    }

    /**
     * Get a map with SupportedElement as key and the participantId as value.
     *
     * @return a map
     */
    public Map<ToscaConceptIdentifier, UUID> getSupportedElementMap() {
        var list = participantRepository.findAll();
        Map<ToscaConceptIdentifier, UUID> map = new HashMap<>();
        for (var participant : list) {
            for (var element : participant.getSupportedElements()) {
                var supportedElement = new ToscaConceptIdentifier(element.getTypeName(), element.getTypeVersion());
                map.put(supportedElement, UUID.fromString(participant.getParticipantId()));
            }
        }
        return map;
    }

    /**
     * Retrieve a list of automation composition elements associated with a participantId.
     *
     * @param participantId the participant id associated with the automation composition elements
     * @param pageable the Pageable
     * @return the list of associated elements
     */
    public List<AutomationCompositionElement> getAutomationCompositionElements(
        @NonNull final UUID participantId, @NonNull final Pageable pageable) {
        return ProviderUtils.asEntityList(automationCompositionElementRepository
            .findByParticipantId(participantId.toString(), pageable));
    }

    /**
     * Retrieve a list of node template states elements associated with a participantId from ac definitions.
     *
     * @param participantId the participant id associated with the automation composition elements
     * @param pageable the Pageable
     * @return the list of associated elements
     */
    public List<NodeTemplateState> getAcNodeTemplateStates(
            @NonNull final UUID participantId, @NonNull final Pageable pageable) {
        return ProviderUtils.asEntityList(nodeTemplateStateRepository
            .findByParticipantId(participantId.toString(), pageable));
    }

    /**
     * Get a list of compositionId associated with a participantId from ac definitions.
     * @param participantId the participant id associated with the automation composition elements
     * @return the set of compositionId
     */
    public Set<UUID> getCompositionIds(@NonNull final UUID participantId) {
        return nodeTemplateStateRepository.findByParticipantId(participantId.toString()).stream()
                .map(nodeTemplateState -> UUID.fromString(nodeTemplateState.getCompositionId()))
                .collect(Collectors.toSet());
    }

    /**
     * Get participant replica.
     *
     * @param replicaId the Id of the replica to get
     * @return the replica found
     */
    @Transactional(readOnly = true)
    public Optional<ParticipantReplica> findParticipantReplica(@NonNull final UUID replicaId) {
        return replicaRepository.findById(replicaId.toString()).map(JpaParticipantReplica::toAuthorative);
    }

    /**
     * Save participant replica.
     *
     * @param replica replica to save
     */
    public void saveParticipantReplica(@NonNull final ParticipantReplica replica) {
        var jpa = replicaRepository.getReferenceById(replica.getReplicaId().toString());
        jpa.fromAuthorative(replica);
        replicaRepository.save(jpa);
    }

    /**
     * Delete participant replica.
     *
     * @param replicaId the Id of the replica to delete
     */
    public void deleteParticipantReplica(@NonNull UUID replicaId) {
        replicaRepository.deleteById(replicaId.toString());
    }

    public List<ParticipantReplica> findReplicasOnLine() {
        return ProviderUtils.asEntityList(replicaRepository.findByParticipantState(ParticipantState.ON_LINE));
    }

    /**
     * Verify Participant state.
     *
     * @param participantIds The list of UUIDs of the participants to get
     * @throws  PfModelRuntimeException in case the participant is offline
     */
    public void verifyParticipantState(Set<UUID> participantIds) {
        for (UUID participantId : participantIds) {
            var jpaParticipant = participantRepository.getReferenceById(participantId.toString());
            var replicaOnline = jpaParticipant.getReplicas().stream()
                    .filter(replica -> ParticipantState.ON_LINE.equals(replica.getParticipantState())).findFirst();
            if (replicaOnline.isEmpty()) {
                throw new PfModelRuntimeException(Response.Status.CONFLICT,
                        "Participant: " + participantId + " is OFFLINE");
            }
        }
    }

    /**
     * Check if the Participant defined into an AutomationCompositionDefinition has been registered.
     *
     * @param acDefinition the AutomationCompositionDefinition
     */
    public void checkRegisteredParticipant(AutomationCompositionDefinition acDefinition) {
        var participantIds = acDefinition.getElementStateMap().values().stream()
                .map(NodeTemplateState::getParticipantId).collect(Collectors.toSet());
        verifyParticipantState(participantIds);
    }
}
