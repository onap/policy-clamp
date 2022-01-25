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
import org.onap.policy.clamp.models.acm.concepts.ParticipantHealthStatus;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantStatistics;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.models.base.PfAuthorative;
import org.onap.policy.models.base.PfConcept;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfKey;
import org.onap.policy.models.base.PfTimestampKey;
import org.onap.policy.models.base.validation.annotations.VerifyKey;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Class to represent a participant statistics in the database.
 *
 * @author Ramesh Murugan Iyer (ramesh.murugan.iyer@est.tech)
 */
@Entity
@Table(name = "ParticipantStatistics")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class JpaParticipantStatistics extends PfConcept implements PfAuthorative<ParticipantStatistics>, Serializable {

    private static final long serialVersionUID = -5992214428190133190L;

    @EmbeddedId
    @VerifyKey
    @NotNull
    private PfTimestampKey key;

    @VerifyKey
    @NotNull
    @AttributeOverride(name = "name", column = @Column(name = "participant_name"))
    @AttributeOverride(name = "version", column = @Column(name = "participant_version"))
    private PfConceptKey participantId;

    @Column
    @NotNull
    private ParticipantState state;

    @Column
    @NotNull
    private ParticipantHealthStatus healthStatus;

    @Column
    private long eventCount;

    @Column
    private long lastExecutionTime;

    @Column
    private double averageExecutionTime;

    @Column
    private long upTime;

    @Column
    private long lastEnterTime;

    @Column
    private long lastStart;


    /**
     * The Default Constructor creates a {@link JpaParticipantStatistics} object with a null key.
     */
    public JpaParticipantStatistics() {
        this(new PfTimestampKey());
    }

    /**
     * The Key Constructor creates a {@link JpaParticipantStatistics} object with the given Timestamp key.
     *
     * @param key the key
     */
    public JpaParticipantStatistics(@NonNull final PfTimestampKey key) {
        this(key, new PfConceptKey(), ParticipantState.PASSIVE, ParticipantHealthStatus.HEALTHY, 0L, 0L, 0.0d, 0L, 0L,
                0L);
    }


    /**
     * The Key Constructor creates a {@link JpaParticipantStatistics} object with all mandatory fields.
     *
     * @param key the key
     * @param participantId the TOSCA definition of the participant
     */
    public JpaParticipantStatistics(@NonNull final PfTimestampKey key, @NonNull final PfConceptKey participantId) {
        this.key = key;
        this.participantId = participantId;
    }


    /**
     * Copy constructor.
     *
     * @param copyConcept the concept to copy from
     */
    public JpaParticipantStatistics(@NonNull final JpaParticipantStatistics copyConcept) {
        super(copyConcept);
        this.key = new PfTimestampKey(copyConcept.key);
        this.participantId = new PfConceptKey(copyConcept.participantId);
        this.state = copyConcept.state;
        this.healthStatus = copyConcept.healthStatus;
        this.eventCount = copyConcept.eventCount;
        this.lastExecutionTime = copyConcept.lastExecutionTime;
        this.averageExecutionTime = copyConcept.averageExecutionTime;
        this.upTime = copyConcept.upTime;
        this.lastEnterTime = copyConcept.lastEnterTime;
        this.lastStart = copyConcept.lastStart;
    }

    /**
     * Authorative constructor.
     *
     * @param authorativeConcept the authorative concept to copy from
     */
    public JpaParticipantStatistics(@NonNull final ParticipantStatistics authorativeConcept) {
        this.fromAuthorative(authorativeConcept);
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

        final JpaParticipantStatistics other = (JpaParticipantStatistics) otherConcept;
        // @formatter:off
        return new CompareToBuilder()
                .append(this.key, other.key)
                .append(this.participantId, other.participantId)
                .append(this.state, other.state)
                .append(this.healthStatus, other.healthStatus)
                .append(this.eventCount, other.eventCount)
                .append(this.lastExecutionTime, other.lastExecutionTime)
                .append(this.averageExecutionTime, other.averageExecutionTime)
                .append(this.upTime, other.upTime)
                .append(this.lastEnterTime, other.lastEnterTime)
                .append(this.lastStart, other.lastStart).toComparison();
        // @formatter:on
    }

    @Override
    public ParticipantStatistics toAuthorative() {
        var participantStatistics = new ParticipantStatistics();
        participantStatistics.setTimeStamp(key.getTimeStamp().toInstant());
        participantStatistics.setParticipantId(new ToscaConceptIdentifier(participantId));
        participantStatistics.setState(state);
        participantStatistics.setHealthStatus(healthStatus);
        participantStatistics.setAverageExecutionTime(averageExecutionTime);
        participantStatistics.setEventCount(eventCount);
        participantStatistics.setLastExecutionTime(lastExecutionTime);
        participantStatistics.setUpTime(upTime);
        participantStatistics.setLastEnterTime(lastEnterTime);
        participantStatistics.setLastStart(lastStart);

        return participantStatistics;
    }

    @Override
    public void fromAuthorative(@NonNull final ParticipantStatistics participantStatistics) {
        if (this.key == null || this.getKey().isNullKey()) {
            this.setKey(new PfTimestampKey(participantStatistics.getParticipantId().getName(),
                    participantStatistics.getParticipantId().getVersion(), participantStatistics.getTimeStamp()));
        }
        this.setParticipantId(participantStatistics.getParticipantId().asConceptKey());
        this.setState(participantStatistics.getState());
        this.setHealthStatus(participantStatistics.getHealthStatus());
        this.setAverageExecutionTime(participantStatistics.getAverageExecutionTime());
        this.setEventCount(participantStatistics.getEventCount());
        this.setLastExecutionTime(participantStatistics.getLastExecutionTime());
        this.setUpTime(participantStatistics.getUpTime());
        this.setLastEnterTime(participantStatistics.getLastEnterTime());
        this.setLastStart(participantStatistics.getLastStart());

    }

    @Override
    public List<PfKey> getKeys() {
        List<PfKey> keyList = getKey().getKeys();
        keyList.addAll(participantId.getKeys());
        return keyList;
    }

    @Override
    public void clean() {
        key.clean();
        participantId.clean();
    }
}
