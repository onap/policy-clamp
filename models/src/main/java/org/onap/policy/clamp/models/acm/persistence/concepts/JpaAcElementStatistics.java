/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 Nordix Foundation.
 * ================================================================================
 * Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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
import org.onap.policy.clamp.models.acm.concepts.AcElementStatistics;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.models.base.PfAuthorative;
import org.onap.policy.models.base.PfConcept;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfKey;
import org.onap.policy.models.base.PfReferenceTimestampKey;
import org.onap.policy.models.base.validation.annotations.VerifyKey;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Class to represent automation composition element statistics in the database.
 *
 * @author Ramesh Murugan Iyer (ramesh.murugan.iyer@est.tech)
 */
@Entity
@Table(name = "AcElementStatistics")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class JpaAcElementStatistics extends PfConcept implements PfAuthorative<AcElementStatistics>, Serializable {

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
    private AutomationCompositionState state;

    @Column
    private long acElementUptime;


    /**
     * The Default Constructor creates a {@link JpaAcElementStatistics} object with a null key.
     */
    public JpaAcElementStatistics() {
        this(new PfReferenceTimestampKey());
    }


    /**
     * The Key Constructor creates a {@link JpaAcElementStatistics} object with the given Reference Timestamp key.
     *
     * @param key the key
     */
    public JpaAcElementStatistics(@NonNull final PfReferenceTimestampKey key) {
        this(key, new PfConceptKey(), AutomationCompositionState.PASSIVE, 0L);
    }

    /**
     * The Key Constructor creates a {@link JpaAcElementStatistics} object with all mandatory fields.
     *
     * @param key the key
     * @param participantId the TOSCA definition of the automation composition element
     */
    public JpaAcElementStatistics(@NonNull final PfReferenceTimestampKey key,
                                  @NonNull final PfConceptKey participantId) {
        this.key = key;
        this.participantId = participantId;
    }

    /**
     * Copy constructor.
     *
     * @param copyConcept the concept to copy from
     */
    public JpaAcElementStatistics(@NonNull final JpaAcElementStatistics copyConcept) {
        super(copyConcept);
        this.key = new PfReferenceTimestampKey(copyConcept.key);
        this.participantId = new PfConceptKey(copyConcept.participantId);
        this.state = copyConcept.state;
        this.acElementUptime = copyConcept.acElementUptime;
    }


    /**
     * Authorative constructor.
     *
     * @param authorativeConcept the authorative concept to copy from
     */
    public JpaAcElementStatistics(@NonNull final AcElementStatistics authorativeConcept) {
        this.fromAuthorative(authorativeConcept);
    }



    @Override
    public AcElementStatistics toAuthorative() {
        var acElementStatistics = new AcElementStatistics();
        acElementStatistics.setId(UUID.fromString(getKey().getReferenceKey().getLocalName()));
        acElementStatistics.setTimeStamp(key.getInstant());
        acElementStatistics.setParticipantId(new ToscaConceptIdentifier(participantId));
        acElementStatistics.setState(state);
        acElementStatistics.setAcElementUptime(acElementUptime);

        return acElementStatistics;
    }

    @Override
    public void fromAuthorative(@NonNull AcElementStatistics acElementStatistics) {
        // @formatter:off
        if (this.key == null || this.getKey().isNullKey()) {
            this.setKey(new PfReferenceTimestampKey(acElementStatistics.getParticipantId().getName(),
                acElementStatistics.getParticipantId().getVersion(), acElementStatistics.getId().toString(),
                acElementStatistics.getTimeStamp()));
        }
        // @formatter:on
        this.setParticipantId(acElementStatistics.getParticipantId().asConceptKey());
        this.setState(acElementStatistics.getState());
        this.setAcElementUptime(acElementStatistics.getAcElementUptime());
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

        final JpaAcElementStatistics other = (JpaAcElementStatistics) otherConcept;
        return new CompareToBuilder().append(this.key, other.key).append(this.state, other.state)
                .append(this.acElementUptime, other.acElementUptime).toComparison();
    }
}
