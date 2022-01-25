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

package org.onap.policy.clamp.models.acm.persistence.concepts;

import java.io.Serializable;
import java.util.List;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.apache.commons.lang3.ObjectUtils;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.concepts.ParticipantHealthStatus;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.models.base.PfAuthorative;
import org.onap.policy.models.base.PfConcept;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfKey;
import org.onap.policy.models.base.validation.annotations.VerifyKey;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Class to represent a participant in the database.
 *
 * @author Liam Fallon (liam.fallon@est.tech)
 */
@Entity
@Table(name = "Participant")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Data
@EqualsAndHashCode(callSuper = false)
public class JpaParticipant extends PfConcept implements PfAuthorative<Participant>, Serializable {
    private static final long serialVersionUID = -4697758484642403483L;

    @EmbeddedId
    @VerifyKey
    @NotNull
    private PfConceptKey key;

    // @formatter:off
    @VerifyKey
    @NotNull
    @AttributeOverride(name = "name",    column = @Column(name = "definition_name"))
    @AttributeOverride(name = "version", column = @Column(name = "definition_version"))
    private PfConceptKey definition;
    // @formatter:on

    @NotNull
    @AttributeOverride(name = "name",    column = @Column(name = "participant_type_name"))
    @AttributeOverride(name = "version", column = @Column(name = "participant_type_version"))
    private PfConceptKey participantType;

    @Column
    @NotNull
    private ParticipantState participantState;

    @Column
    @NotNull
    private ParticipantHealthStatus healthStatus;

    @Column
    private String description;

    /**
     * The Default Constructor creates a {@link JpaParticipant} object with a null key.
     */
    public JpaParticipant() {
        this(new PfConceptKey());
    }

    /**
     * The Key Constructor creates a {@link JpaParticipant} object with the given concept key.
     *
     * @param key the key
     */
    public JpaParticipant(@NonNull final PfConceptKey key) {
        this(key, new PfConceptKey(), ParticipantState.PASSIVE, ParticipantHealthStatus.UNKNOWN);
    }

    /**
     * The Key Constructor creates a {@link JpaParticipant} object with all mandatory fields.
     *
     * @param key the key
     * @param definition the TOSCA definition of the participant
     * @param participantState the state of the participant
     * @param healthStatus the health state of the participant
     */
    public JpaParticipant(@NonNull final PfConceptKey key, @NonNull final PfConceptKey definition,
            @NonNull final ParticipantState participantState, @NonNull ParticipantHealthStatus healthStatus) {
        this.key = key;
        this.definition = definition;
        this.participantState = participantState;
        this.healthStatus = healthStatus;
    }

    /**
     * Copy constructor.
     *
     * @param copyConcept the concept to copy from
     */
    public JpaParticipant(@NonNull final JpaParticipant copyConcept) {
        super(copyConcept);
        this.key = new PfConceptKey(copyConcept.key);
        this.definition = new PfConceptKey(copyConcept.definition);
        this.participantState = copyConcept.participantState;
        this.healthStatus = copyConcept.healthStatus;
        this.description = copyConcept.description;
        this.participantType = copyConcept.participantType;
    }

    /**
     * Authorative constructor.
     *
     * @param authorativeConcept the authorative concept to copy from
     */
    public JpaParticipant(@NonNull final Participant authorativeConcept) {
        this.fromAuthorative(authorativeConcept);
    }

    @Override
    public Participant toAuthorative() {
        var participant = new Participant();

        participant.setName(key.getName());
        participant.setVersion(key.getVersion());
        participant.setDefinition(new ToscaConceptIdentifier(definition));
        participant.setParticipantState(participantState);
        participant.setHealthStatus(healthStatus);
        participant.setDescription(description);
        participant.setParticipantType(new ToscaConceptIdentifier(participantType));

        return participant;
    }

    @Override
    public void fromAuthorative(@NonNull final Participant participant) {
        if (this.key == null || this.getKey().isNullKey()) {
            this.setKey(new PfConceptKey(participant.getName(), participant.getVersion()));
        }

        this.definition = participant.getDefinition().asConceptKey();
        this.setParticipantState(participant.getParticipantState());
        this.setHealthStatus(participant.getHealthStatus());
        this.setDescription(participant.getDescription());
        this.participantType = participant.getParticipantType().asConceptKey();
    }

    @Override
    public List<PfKey> getKeys() {
        List<PfKey> keyList = getKey().getKeys();

        keyList.add(definition);
        keyList.add(participantType);

        return keyList;
    }

    @Override
    public void clean() {
        key.clean();
        definition.clean();
        description = (description == null ? null : description.trim());
        participantType.clean();
    }

    @Override
    public int compareTo(final PfConcept otherConcept) {
        if (otherConcept == null) {
            return -1;
        }
        if (this == otherConcept) {
            return 0;
        }
        if (getClass() != otherConcept.getClass()) {
            return getClass().getName().compareTo(otherConcept.getClass().getName());
        }

        final JpaParticipant other = (JpaParticipant) otherConcept;
        int result = key.compareTo(other.key);
        if (result != 0) {
            return result;
        }

        result = definition.compareTo(other.definition);
        if (result != 0) {
            return result;
        }

        result = ObjectUtils.compare(participantState, other.participantState);
        if (result != 0) {
            return result;
        }

        result = ObjectUtils.compare(healthStatus, other.healthStatus);
        if (result != 0) {
            return result;
        }

        result = ObjectUtils.compare(participantType, other.participantType);
        if (result != 0) {
            return result;
        }

        return ObjectUtils.compare(description, other.description);
    }
}
