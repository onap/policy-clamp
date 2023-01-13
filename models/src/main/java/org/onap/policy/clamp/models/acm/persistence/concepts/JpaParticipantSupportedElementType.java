/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023 Nordix Foundation.
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

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.apache.commons.lang3.ObjectUtils;
import org.onap.policy.clamp.models.acm.concepts.ParticipantSupportedElementType;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.models.base.PfAuthorative;
import org.onap.policy.models.base.Validated;

@Entity
@Table(name = "ParticipantSupportedAcElements")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Data
@EqualsAndHashCode(callSuper = false)
public class JpaParticipantSupportedElementType extends Validated
    implements PfAuthorative<ParticipantSupportedElementType>, Comparable<JpaParticipantSupportedElementType> {

    @NotNull
    @Column
    @Id
    private String id;
    @NotNull
    @Column
    private String participantId;

    @NotNull
    @Column
    private String typeName;

    @NotNull
    @Column
    private String typeVersion;

    /**
     * The default Constructor creates a {@link JpaParticipantSupportedElementType} with a generated id.
     *
     */
    public JpaParticipantSupportedElementType() {
        this(UUID.randomUUID().toString());
    }

    /**
     * The Key Constructor creates a {@link JpaParticipantSupportedElementType} with just a generated id parameter.
     *
     * @param id the main id of the element type
     */
    public JpaParticipantSupportedElementType(@NonNull String id) {
        this(id, UUID.randomUUID().toString());
    }

    /**
     * The Key Constructor creates a {@link JpaParticipantSupportedElementType} with id and participantId.
     *
     * @param id the main id of the element type
     * @param participantId the participant id
     */
    public JpaParticipantSupportedElementType(@NonNull String id, @NonNull String participantId) {
        this(id, participantId, "", "");
    }

    /**
     * The Key Constructor creates a {@link JpaParticipantSupportedElementType} object with all mandatory fields.
     *
     * @param id the main id of the element type
     * @param participantId the participant id
     * @param typeName the type name of the supported element
     * @param typeVersion the type version of the supported element
     */
    public JpaParticipantSupportedElementType(@NonNull String id,
                                              @NonNull String participantId,
                                              @NonNull String typeName,
                                              @NonNull String typeVersion) {
        this.id = id;
        this.participantId = participantId;
        this.typeName = typeName;
        this.typeVersion = typeVersion;
    }

    /**
     * Copy constructor.
     *
     * @param copyConcept the concept to copy from
     */
    public JpaParticipantSupportedElementType(@NonNull final JpaParticipantSupportedElementType copyConcept) {
        this.id = copyConcept.id;
        this.participantId = copyConcept.participantId;
        this.typeName = copyConcept.typeName;
        this.typeVersion = copyConcept.typeVersion;
    }

    /**
     * Authorative constructor.
     *
     * @param authorativeConcept the authorative concept to copy from
     */
    public JpaParticipantSupportedElementType(@NonNull final ParticipantSupportedElementType authorativeConcept) {
        this.fromAuthorative(authorativeConcept);
    }

    @Override
    public int compareTo(final JpaParticipantSupportedElementType other) {
        if (other == null) {
            return -1;
        }
        if (this == other) {
            return 0;
        }

        var result = ObjectUtils.compare(participantId, other.participantId);
        if (result != 0) {
            return result;
        }

        result = ObjectUtils.compare(typeName, other.typeName);
        if (result != 0) {
            return result;
        }

        result = ObjectUtils.compare(id, other.id);
        if (result != 0) {
            return result;
        }

        result = typeVersion.compareTo(other.typeVersion);
        if (result != 0) {
            return result;
        }

        return 0;
    }

    @Override
    public ParticipantSupportedElementType toAuthorative() {
        var element = new ParticipantSupportedElementType();
        element.setId(UUID.fromString(id));
        element.setTypeName(typeName);
        element.setTypeVersion(typeVersion);
        return element;
    }

    @Override
    public void fromAuthorative(@NonNull ParticipantSupportedElementType participantSupportedElementType) {
        this.id = participantSupportedElementType.getId().toString();
        this.typeName = participantSupportedElementType.getTypeName();
        this.typeVersion = participantSupportedElementType.getTypeVersion();
    }
}
