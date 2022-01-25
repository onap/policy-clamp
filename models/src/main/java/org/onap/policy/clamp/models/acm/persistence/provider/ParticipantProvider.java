/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 Nordix Foundation.
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

import java.util.List;
import java.util.Optional;
import javax.ws.rs.core.Response.Status;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaParticipant;
import org.onap.policy.clamp.models.acm.persistence.repository.ParticipantRepository;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaTypedEntityFilter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class provides information on participant concepts in the database to callers.
 */
@Service
@Transactional
@AllArgsConstructor
public class ParticipantProvider {

    private ParticipantRepository participantRepository;

    /**
     * Get participants.
     *
     * @param name the name of the participant to get, null to get all participants
     * @param version the version of the participant to get, null to get all participants
     * @return the participants found
     * @throws PfModelException on errors getting participants
     */
    @Transactional(readOnly = true)
    public List<Participant> getParticipants(final String name, final String version) throws PfModelException {

        return ProviderUtils.asEntityList(participantRepository.getFiltered(JpaParticipant.class, name, version));
    }

    /**
     * Get all participants.
     *
     * @return the participants found
     * @throws PfModelException on errors getting policies
     */
    @Transactional(readOnly = true)
    public List<Participant> getParticipants() throws PfModelException {
        return ProviderUtils.asEntityList(participantRepository.findAll());
    }

    /**
     * Get participant.
     *
     * @param name the name of the participant to get
     * @param version the version of the participant to get
     * @return the participant found
     * @throws PfModelException on errors getting participant
     */
    @Transactional(readOnly = true)
    public Optional<Participant> findParticipant(@NonNull final String name, @NonNull final String version)
            throws PfModelException {
        try {
            return participantRepository.findById(new PfConceptKey(name, version)).map(JpaParticipant::toAuthorative);
        } catch (IllegalArgumentException e) {
            throw new PfModelException(Status.BAD_REQUEST, "Error in find Participant", e);
        }
    }

    /**
     * Get filtered participants.
     *
     * @param filter the filter for the participants to get
     * @return the participants found
     * @throws PfModelException on errors getting policies
     */
    @Transactional(readOnly = true)
    public List<Participant> getFilteredParticipants(@NonNull final ToscaTypedEntityFilter<Participant> filter)
            throws PfModelException {

        return filter.filter(ProviderUtils.asEntityList(
                participantRepository.getFiltered(JpaParticipant.class, filter.getName(), filter.getVersion())));
    }

    /**
     * Saves participant.
     *
     * @param participant participant to save
     * @return the participant created
     * @throws PfModelException on errors creating participants
     */
    public Participant saveParticipant(@NonNull final Participant participant) throws PfModelException {
        try {
            var result = participantRepository
                    .save(ProviderUtils.getJpaAndValidate(participant, JpaParticipant::new, "participant"));

            // Return the saved participant
            return result.toAuthorative();
        } catch (IllegalArgumentException e) {
            throw new PfModelException(Status.BAD_REQUEST, "Error in save Participant", e);
        }
    }

    /**
     * Delete a participant.
     *
     * @param name the name of the participant to delete
     * @param version the version of the participant to get
     * @return the participant deleted
     * @throws PfModelRuntimeException on errors deleting participants
     */
    public Participant deleteParticipant(@NonNull final String name, @NonNull final String version)
            throws PfModelException {
        try {
            var participantKey = new PfConceptKey(name, version);

            var jpaDeleteParticipantOpt = participantRepository.findById(participantKey);

            if (jpaDeleteParticipantOpt.isEmpty()) {
                String errorMessage =
                        "delete of participant \"" + participantKey.getId() + "\" failed, participant does not exist";
                throw new PfModelRuntimeException(Status.BAD_REQUEST, errorMessage);
            }
            participantRepository.delete(jpaDeleteParticipantOpt.get());

            return jpaDeleteParticipantOpt.get().toAuthorative();
        } catch (IllegalArgumentException e) {
            throw new PfModelException(Status.BAD_REQUEST, "Error in delete Participant", e);
        }
    }
}
