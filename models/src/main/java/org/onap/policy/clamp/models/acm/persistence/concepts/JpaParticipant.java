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

package org.onap.policy.clamp.models.acm.persistence.concepts;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import javax.persistence.AttributeOverride;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.apache.commons.lang3.ObjectUtils;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.common.parameters.annotations.Valid;
import org.onap.policy.models.base.PfAuthorative;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.Validated;
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
public class JpaParticipant extends Validated
        implements PfAuthorative<Participant>, Comparable<JpaParticipant>, Serializable {
    private static final long serialVersionUID = -4697758484642403483L;

    @Id
    @NotNull
    private String participantId;

    @NotNull
    @AttributeOverride(name = "name", column = @Column(name = "participant_type_name"))
    @AttributeOverride(name = "version", column = @Column(name = "participant_type_version"))
    private PfConceptKey participantType;

    @Column
    @NotNull
    private ParticipantState participantState;

    @Column
    private String description;

    @NotNull
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "participantId", referencedColumnName = "participantId",
        foreignKey = @ForeignKey(name = "supported_element_fk"))
    private List<@NotNull @Valid JpaParticipantSupportedElementType> supportedElements;

    /**
     * The Default Constructor creates a {@link JpaParticipant} object with a null key.
     */
    public JpaParticipant() {
        this(UUID.randomUUID().toString(), ParticipantState.ON_LINE, new ArrayList<>());
    }

    /**
     * The Key Constructor creates a {@link JpaParticipant} object with all mandatory fields.
     *
     * @param participantId the participant id
     * @param participantState the state of the participant
     */
    public JpaParticipant(@NonNull String participantId, @NonNull final ParticipantState participantState,
            @NonNull final List<JpaParticipantSupportedElementType> supportedElements) {
        this.participantId = participantId;
        this.participantState = participantState;
        this.supportedElements = supportedElements;
    }

    /**
     * Copy constructor.
     *
     * @param copyConcept the concept to copy from
     */
    public JpaParticipant(@NonNull final JpaParticipant copyConcept) {
        this.participantState = copyConcept.participantState;
        this.description = copyConcept.description;
        this.participantType = copyConcept.participantType;
        this.participantId = copyConcept.participantId;
        this.supportedElements = copyConcept.supportedElements;
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

        participant.setParticipantState(participantState);
        participant.setParticipantType(new ToscaConceptIdentifier(participantType));
        participant.setParticipantId(UUID.fromString(participantId));
        participant.setParticipantSupportedElementTypes(new LinkedHashMap<>(this.supportedElements.size()));
        for (var element : this.supportedElements) {
            participant.getParticipantSupportedElementTypes()
                .put(UUID.fromString(element.getId()), element.toAuthorative());
        }

        return participant;
    }

    @Override
    public void fromAuthorative(@NonNull final Participant participant) {
        this.setParticipantState(participant.getParticipantState());
        this.participantType = participant.getParticipantType().asConceptKey();
        this.participantId = participant.getParticipantId().toString();
        this.supportedElements = new ArrayList<>(participant.getParticipantSupportedElementTypes().size());

        for (var elementEntry : participant.getParticipantSupportedElementTypes().entrySet()) {
            var jpaParticipantSupportedElementType = new JpaParticipantSupportedElementType();
            jpaParticipantSupportedElementType.setParticipantId(this.participantId);
            jpaParticipantSupportedElementType.fromAuthorative(elementEntry.getValue());
            this.supportedElements.add(jpaParticipantSupportedElementType);
        }
    }

    @Override
    public int compareTo(final JpaParticipant other) {
        if (other == null) {
            return -1;
        }
        if (this == other) {
            return 0;
        }

        var result = participantId.compareTo(other.participantId);
        if (result != 0) {
            return result;
        }

        result = ObjectUtils.compare(participantState, other.participantState);
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
