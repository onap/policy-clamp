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

package org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;
import lombok.NonNull;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.Participant;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantFilter;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.concepts.JpaParticipant;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.provider.PolicyModelsProviderParameters;
import org.onap.policy.models.provider.impl.AbstractModelsProvider;

/**
 * This class provides information on participant concepts in the database to callers.
 */
public class ParticipantProvider extends AbstractModelsProvider {
    /**
     * Create a provider for participantss.
     *
     * @param parameters the parameters for database access
     * @throws PfModelException on initiation errors
     */
    public ParticipantProvider(@NonNull PolicyModelsProviderParameters parameters) throws PfModelException {
        super(parameters);
        this.init();
    }

    /**
     * Get participants.
     *
     * @param name the name of the participant to get, null to get all participants
     * @param version the version of the participant to get, null to get all participants
     * @return the participants found
     * @throws PfModelException on errors getting participants
     */
    public List<Participant> getParticipants(final String name, final String version) throws PfModelException {

        return asParticipantList(getPfDao().getFiltered(JpaParticipant.class, name, version));
    }

    /**
     * Get filtered participants.
     *
     * @param filter the filter for the participants to get
     * @return the participants found
     * @throws PfModelException on errors getting policies
     */
    public List<Participant> getFilteredParticipants(@NonNull final ParticipantFilter filter) {

        return filter.filter(asParticipantList(
                getPfDao().getFiltered(JpaParticipant.class, filter.getName(), filter.getVersion())));
    }

    /**
     * Creates participants.
     *
     * @param participants a specification of the participants to create
     * @return the participants created
     * @throws PfModelException on errors creating participants
     */
    public List<Participant> createParticipants(@NonNull final List<Participant> participants) throws PfModelException {

        for (Participant participant : participants) {
            JpaParticipant jpaParticipant = new JpaParticipant();
            jpaParticipant.fromAuthorative(participant);

            BeanValidationResult validationResult = jpaParticipant.validate("participant");
            if (!validationResult.isValid()) {
                throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, validationResult.getResult());
            }

            getPfDao().create(jpaParticipant);
        }

        // Return the created participants
        List<Participant> returnParticipants = new ArrayList<>();

        for (Participant participant : participants) {
            JpaParticipant jpaParticipant = getPfDao().get(JpaParticipant.class,
                    new PfConceptKey(participant.getName(), participant.getVersion()));
            returnParticipants.add(jpaParticipant.toAuthorative());
        }

        return returnParticipants;
    }

    /**
     * Updates participants.
     *
     * @param participants a specification of the participants to update
     * @return the participants updated
     * @throws PfModelException on errors updating participants
     */
    public List<Participant> updateParticipants(@NonNull final List<Participant> participants) throws PfModelException {

        for (Participant participant : participants) {
            JpaParticipant jpaParticipant = new JpaParticipant();
            jpaParticipant.fromAuthorative(participant);

            BeanValidationResult validationResult = jpaParticipant.validate("participant");
            if (!validationResult.isValid()) {
                throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, validationResult.getResult());
            }

            getPfDao().update(jpaParticipant);
        }

        // Return the created participants
        List<Participant> returnParticipants = new ArrayList<>();

        for (Participant participant : participants) {
            JpaParticipant jpaParticipant = getPfDao().get(JpaParticipant.class,
                    new PfConceptKey(participant.getName(), participant.getVersion()));
            returnParticipants.add(jpaParticipant.toAuthorative());
        }

        return returnParticipants;
    }

    /**
     * Delete a participant.
     *
     * @param name the name of the participant to delete
     * @param version the version of the participant to get
     * @return the participant deleted
     * @throws PfModelException on errors deleting participants
     */
    public Participant deleteParticipant(@NonNull final String name, @NonNull final String version) {

        PfConceptKey participantKey = new PfConceptKey(name, version);

        JpaParticipant jpaDeleteParticipant = getPfDao().get(JpaParticipant.class, participantKey);

        if (jpaDeleteParticipant == null) {
            String errorMessage =
                    "delete of participant \"" + participantKey.getId() + "\" failed, participant does not exist";
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, errorMessage);
        }

        getPfDao().delete(jpaDeleteParticipant);

        return jpaDeleteParticipant.toAuthorative();
    }

    /**
     * Convert JPA participant list to an authorative participant list.
     *
     * @param foundParticipants the list to convert
     * @return the authorative list
     */
    private List<Participant> asParticipantList(List<JpaParticipant> jpaParticipantList) {
        List<Participant> participantList = new ArrayList<>(jpaParticipantList.size());

        for (JpaParticipant jpaParticipant : jpaParticipantList) {
            participantList.add(jpaParticipant.toAuthorative());
        }

        return participantList;
    }
}
