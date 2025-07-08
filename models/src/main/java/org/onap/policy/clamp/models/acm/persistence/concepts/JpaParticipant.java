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

package org.onap.policy.clamp.models.acm.persistence.concepts;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.apache.commons.lang3.ObjectUtils;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.common.parameters.annotations.Valid;
import org.onap.policy.models.base.PfAuthorative;
import org.onap.policy.models.base.Validated;

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

    @Serial
    private static final long serialVersionUID = -4697758484642403483L;

    @Id
    @NotNull
    private String participantId;

    @Column
    private String description;

    @NotNull
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "participantId", referencedColumnName = "participantId",
        foreignKey = @ForeignKey(name = "supported_element_fk"))
    @SuppressWarnings("squid:S1948")
    private List<@NotNull @Valid JpaParticipantSupportedElementType> supportedElements;

    @NotNull
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "participantId", referencedColumnName = "participantId",
            foreignKey = @ForeignKey(name = "participant_replica_fk"))
    @SuppressWarnings("squid:S1948")
    private List<@NotNull @Valid JpaParticipantReplica> replicas;

    /**
     * The Default Constructor creates a {@link JpaParticipant} object with a null key.
     */
    public JpaParticipant() {
        this(UUID.randomUUID().toString(), new ArrayList<>(), new ArrayList<>());
    }

    /**
     * The Key Constructor creates a {@link JpaParticipant} object with all mandatory fields.
     *
     * @param participantId the participant id
     * @param supportedElements the list of supported Element Type
     * @param replicas the list of replica
     */
    public JpaParticipant(@NonNull String participantId,
            @NonNull final List<JpaParticipantSupportedElementType> supportedElements,
            @NonNull final List<JpaParticipantReplica> replicas) {
        this.participantId = participantId;
        this.supportedElements = supportedElements;
        this.replicas = replicas;
    }

    /**
     * Copy constructor.
     *
     * @param copyConcept the concept to copy from
     */
    public JpaParticipant(@NonNull final JpaParticipant copyConcept) {
        this.description = copyConcept.description;
        this.participantId = copyConcept.participantId;
        this.supportedElements = copyConcept.supportedElements;
        this.replicas = copyConcept.replicas;
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

        participant.setParticipantId(UUID.fromString(participantId));
        participant.setParticipantSupportedElementTypes(new LinkedHashMap<>(this.supportedElements.size()));
        for (var element : this.supportedElements) {
            participant.getParticipantSupportedElementTypes()
                .put(UUID.fromString(element.getId()), element.toAuthorative());
        }
        for (var replica : this.replicas) {
            participant.getReplicas().put(UUID.fromString(replica.getReplicaId()), replica.toAuthorative());
        }
        return participant;
    }

    @Override
    public void fromAuthorative(@NonNull final Participant participant) {
        this.participantId = participant.getParticipantId().toString();

        this.supportedElements = new ArrayList<>(participant.getParticipantSupportedElementTypes().size());
        for (var elementEntry : participant.getParticipantSupportedElementTypes().entrySet()) {
            var jpaParticipantSupportedElementType = new JpaParticipantSupportedElementType();
            jpaParticipantSupportedElementType.setParticipantId(this.participantId);
            jpaParticipantSupportedElementType.fromAuthorative(elementEntry.getValue());
            this.supportedElements.add(jpaParticipantSupportedElementType);
        }

        this.replicas = new ArrayList<>(participant.getReplicas().size());
        for (var replicaEntry : participant.getReplicas().entrySet()) {
            var jpaReplica = new JpaParticipantReplica();
            jpaReplica.setParticipantId(this.participantId);
            jpaReplica.fromAuthorative(replicaEntry.getValue());
            this.replicas.add(jpaReplica);
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

        return ObjectUtils.compare(description, other.description);
    }
}
