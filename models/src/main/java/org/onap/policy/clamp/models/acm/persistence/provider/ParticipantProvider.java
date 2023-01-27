/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2023 Nordix Foundation.
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
import java.util.UUID;
import javax.ws.rs.core.Response.Status;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.NodeTemplateState;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaParticipant;
import org.onap.policy.clamp.models.acm.persistence.repository.AutomationCompositionElementRepository;
import org.onap.policy.clamp.models.acm.persistence.repository.NodeTemplateStateRepository;
import org.onap.policy.clamp.models.acm.persistence.repository.ParticipantRepository;
import org.onap.policy.models.base.PfModelRuntimeException;
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
    public Participant deleteParticipant(@NonNull final UUID participantId) {
        var jpaDeleteParticipantOpt = participantRepository.findById(participantId.toString());

        if (jpaDeleteParticipantOpt.isEmpty()) {
            String errorMessage =
                "delete of participant \"" + participantId + "\" failed, participant does not exist";
            throw new PfModelRuntimeException(Status.BAD_REQUEST, errorMessage);
        }
        participantRepository.delete(jpaDeleteParticipantOpt.get());

        return jpaDeleteParticipantOpt.get().toAuthorative();
    }

    /**
     * Retrieve a list of automation composition elements associated with a participantId.
     *
     * @param participantId the participant id associated with the automation composition elements
     * @return the list of associated elements
     */
    public List<AutomationCompositionElement> getAutomationCompositionElements(@NonNull final UUID participantId) {
        return ProviderUtils.asEntityList(automationCompositionElementRepository
            .findByParticipantId(participantId.toString()));
    }

    /**
     * Retrieve a list of node template states elements associated with a participantId from ac definitions.
     *
     * @param participantId the participant id associated with the automation composition elements
     * @return the list of associated elements
     */
    public List<NodeTemplateState> getAcNodeTemplateStates(@NonNull final UUID participantId) {
        return ProviderUtils.asEntityList(nodeTemplateStateRepository
            .findByParticipantId(participantId.toString()));
    }
}
