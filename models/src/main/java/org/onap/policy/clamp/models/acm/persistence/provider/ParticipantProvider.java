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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response.Status;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaAutomationComposition;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaParticipant;
import org.onap.policy.clamp.models.acm.persistence.repository.ParticipantRepository;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
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

    /**
     * Get participants.
     *
     * @param name the name of the participant to get, null to get all participants
     * @param version the version of the participant to get, null to get all participants
     * @return the participants found
     */
    @Transactional(readOnly = true)
    public List<Participant> getParticipants(final String name, final String version) {

        return ProviderUtils.asEntityList(participantRepository.getFiltered(JpaParticipant.class, name, version));
    }

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
     * @throws PfModelException on errors getting participant
     */
    @Transactional(readOnly = true)
    public Participant getParticipantById(String participantId) {
        var participant = participantRepository.findByParticipantId(participantId);
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
    public Optional<Participant> findParticipant(@NonNull final ToscaConceptIdentifier participantId) {
        return participantRepository.findById(participantId.asConceptKey()).map(JpaParticipant::toAuthorative);
    }

    /**
     * Saves participant.
     *
     * @param participant participant to save
     * @return the participant created
     */
    public Participant saveParticipant(@NonNull final Participant participant) {
        participant.setParticipantId(UUID.randomUUID());
        var result = participantRepository
            .save(ProviderUtils.getJpaAndValidate(participant, JpaParticipant::new, "participant"));

        // Return the saved participant
        return result.toAuthorative();
    }

    /**
     * Updates an existing participant.
     *
     * @param participant participant to update
     * @return the participant updated
     */
    public Participant updateParticipant(@NonNull final Participant participant) {
        var result = participantRepository
            .save(ProviderUtils.getJpaAndValidate(participant, JpaParticipant::new, "participant"));

        // Return the saved participant
        return result.toAuthorative();
    }

    /**
     * Delete a participant.
     *
     * @param participantId the Id of the participant to delete
     * @return the participant deleted
     */
    public Participant deleteParticipant(@NonNull final ToscaConceptIdentifier participantId) {
        var jpaDeleteParticipantOpt = participantRepository.findById(participantId.asConceptKey());

        if (jpaDeleteParticipantOpt.isEmpty()) {
            String errorMessage =
                "delete of participant \"" + participantId + "\" failed, participant does not exist";
            throw new PfModelRuntimeException(Status.BAD_REQUEST, errorMessage);
        }
        participantRepository.delete(jpaDeleteParticipantOpt.get());

        return jpaDeleteParticipantOpt.get().toAuthorative();
    }
}
