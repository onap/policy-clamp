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

package org.onap.policy.clamp.controlloop.models.controlloop.persistence.concepts;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ClElementStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.models.base.PfAuthorative;
import org.onap.policy.models.base.PfConcept;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfKey;
import org.onap.policy.models.base.PfReferenceTimestampKey;
import org.onap.policy.models.base.validation.annotations.VerifyKey;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Class to represent a controlloop element statistics in the database.
 *
 * @author Ramesh Murugan Iyer (ramesh.murugan.iyer@est.tech)
 */
@Entity
@Table(name = "ClElementStatistics")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class JpaClElementStatistics extends PfConcept implements PfAuthorative<ClElementStatistics>, Serializable {

    private static final long serialVersionUID = 621426717868738629L;

    @EmbeddedId
    @VerifyKey
    @NotNull
    private PfReferenceTimestampKey key = new PfReferenceTimestampKey();


    @VerifyKey
    @NotNull
    // @formatter:off
    @AttributeOverride(name = "name",    column = @Column(name = "participant_name"))
    @AttributeOverride(name = "version", column = @Column(name = "participant_version"))
    private PfConceptKey participantId;
    // @formatter: on

    @Column
    @NotNull
    private ControlLoopState state;

    @Column
    private long clElementUptime;


    /**
     * The Default Constructor creates a {@link JpaClElementStatistics} object with a null key.
     */
    public JpaClElementStatistics() {
        this(new PfReferenceTimestampKey());
    }


    /**
     * The Key Constructor creates a {@link JpaClElementStatistics} object with the given Reference Timestamp key.
     *
     * @param key the key
     */
    public JpaClElementStatistics(@NonNull final PfReferenceTimestampKey key) {
        this(key, new PfConceptKey(), ControlLoopState.PASSIVE, 0L);
    }

    /**
     * The Key Constructor creates a {@link JpaClElementStatistics} object with all mandatory fields.
     *
     * @param key the key
     * @param participantId the TOSCA definition of the control loop element
     */
    public JpaClElementStatistics(@NonNull final PfReferenceTimestampKey key,
                                  @NonNull final PfConceptKey participantId) {
        this.key = key;
        this.participantId = participantId;
    }

    /**
     * Copy constructor.
     *
     * @param copyConcept the concept to copy from
     */
    public JpaClElementStatistics(@NonNull final JpaClElementStatistics copyConcept) {
        super(copyConcept);
        this.key = new PfReferenceTimestampKey(copyConcept.key);
        this.participantId = new PfConceptKey(copyConcept.participantId);
        this.state = copyConcept.state;
        this.clElementUptime = copyConcept.clElementUptime;
    }


    /**
     * Authorative constructor.
     *
     * @param authorativeConcept the authorative concept to copy from
     */
    public JpaClElementStatistics(@NonNull final ClElementStatistics authorativeConcept) {
        this.fromAuthorative(authorativeConcept);
    }



    @Override
    public ClElementStatistics toAuthorative() {
        ClElementStatistics clElementStatistics = new ClElementStatistics();
        clElementStatistics.setId(UUID.fromString(getKey().getReferenceKey().getLocalName()));
        clElementStatistics.setTimeStamp(key.getInstant());
        clElementStatistics.setParticipantId(new ToscaConceptIdentifier(participantId));
        clElementStatistics.setControlLoopState(state);
        clElementStatistics.setClElementUptime(clElementUptime);

        return clElementStatistics;
    }

    @Override
    public void fromAuthorative(@NonNull ClElementStatistics clElementStatistics) {
        // @formatter:off
        if (this.key == null || this.getKey().isNullKey()) {
            this.setKey(new PfReferenceTimestampKey(clElementStatistics.getParticipantId().getName(),
                clElementStatistics.getParticipantId().getVersion(), clElementStatistics.getId().toString(),
                clElementStatistics.getTimeStamp()));
        }
        // @formatter:on
        this.setParticipantId(clElementStatistics.getParticipantId().asConceptKey());
        this.setState(clElementStatistics.getControlLoopState());
        this.setClElementUptime(clElementStatistics.getClElementUptime());
    }

    @Override
    public List<PfKey> getKeys() {
        return getKey().getKeys();
    }

    @Override
    public void clean() {
        key.clean();
        participantId.clean();
    }


    @Override
    public int compareTo(PfConcept otherConcept) {
        if (otherConcept == null) {
            return -1;
        }
        if (this == otherConcept) {
            return 0;
        }
        if (getClass() != otherConcept.getClass()) {
            return getClass().getName().compareTo(otherConcept.getClass().getName());
        }

        final JpaClElementStatistics other = (JpaClElementStatistics) otherConcept;
        return new CompareToBuilder().append(this.key, other.key).append(this.state, other.state)
                .append(this.clElementUptime, other.clElementUptime).toComparison();
    }
}
