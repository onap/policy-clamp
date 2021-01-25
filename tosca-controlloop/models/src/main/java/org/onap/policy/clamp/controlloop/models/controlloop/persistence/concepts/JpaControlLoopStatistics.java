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
import java.util.Date;
import java.util.List;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
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
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopStatistics;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.models.base.PfAuthorative;
import org.onap.policy.models.base.PfConcept;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfKey;
import org.onap.policy.models.base.PfTimestampKey;
import org.onap.policy.models.base.validation.annotations.VerifyKey;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Class to represent a control loop statistics in the database.
 *
 * @author Ramesh Murugan Iyer (ramesh.murugan.iyer@est.tech)
 */
@Entity
@Table(name = "ControlLoopStatistics")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class JpaControlLoopStatistics extends PfConcept
    implements PfAuthorative<ControlLoopStatistics>, Serializable {

    private static final long serialVersionUID = -5992214428190133190L;

    @EmbeddedId
    @VerifyKey
    @NotNull
    private PfTimestampKey key;

    @VerifyKey
    @NotNull
    @AttributeOverrides({
        @AttributeOverride(name = "name",    column = @Column(name = "participant_name")),
        @AttributeOverride(name = "version", column = @Column(name = "participant_version"))
        }
    )
    private PfConceptKey participantId;

    @Column
    private ControlLoopState state;

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
     * The Default Constructor creates a {@link JpaControlLoopStatistics} object with a null key.
     */
    public JpaControlLoopStatistics() {
        this(new PfTimestampKey());
    }

    /**
     * The Key Constructor creates a {@link JpaControlLoopStatistics} object with the given Timestamp key.
     *
     * @param key the key
     */
    public JpaControlLoopStatistics(@NonNull final PfTimestampKey key) {
        this(key, new PfConceptKey(), ControlLoopState.UNINITIALISED, 0L, 0L,
            0.0d, 0L, 0L, 0L);
    }


    /**
     * The Key Constructor creates a {@link JpaControlLoopStatistics} object with all mandatory fields.
     *
     * @param key the key
     * @param participantId the TOSCA definition of the control loop participant
     */
    public JpaControlLoopStatistics(@NonNull final PfTimestampKey key, @NonNull final PfConceptKey participantId) {
        this.key = key;
        this.participantId = participantId;
    }


    /**
     * Copy constructor.
     *
     * @param copyConcept the concept to copy from
     */
    public JpaControlLoopStatistics(@NonNull final JpaControlLoopStatistics copyConcept) {
        super(copyConcept);
        this.key = new PfTimestampKey(copyConcept.key);
        this.participantId = new PfConceptKey(copyConcept.participantId);
        this.state = copyConcept.state;
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
    public JpaControlLoopStatistics(@NonNull final ControlLoopStatistics authorativeConcept) {
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

        final JpaControlLoopStatistics other = (JpaControlLoopStatistics) otherConcept;
        return new CompareToBuilder().append(this.key, other.key)
            .append(this.state, other.state)
            .append(this.eventCount, other.eventCount)
            .append(this.lastExecutionTime, other.lastExecutionTime)
            .append(this.averageExecutionTime, other.averageExecutionTime)
            .append(this.upTime, other.upTime)
            .append(this.lastEnterTime, other.lastEnterTime)
            .append(this.lastStart, other.lastStart).toComparison();
    }

    @Override
    public ControlLoopStatistics toAuthorative() {
        ControlLoopStatistics controlLoopStatistics = new ControlLoopStatistics();
        controlLoopStatistics.setParticipantTimeStamp(new Date(key.getTimeStamp().getTime()));
        controlLoopStatistics.setParticipantId(new ToscaConceptIdentifier(participantId));
        controlLoopStatistics.setState(state);
        controlLoopStatistics.setAverageExecutionTime(averageExecutionTime);
        controlLoopStatistics.setEventCount(eventCount);
        controlLoopStatistics.setLastExecutionTime(lastExecutionTime);
        controlLoopStatistics.setUpTime(upTime);
        controlLoopStatistics.setLastEnterTime(lastEnterTime);
        controlLoopStatistics.setLastStart(lastStart);

        return controlLoopStatistics;
    }

    @Override
    public void fromAuthorative(@NonNull final ControlLoopStatistics controlLoopStatistics) {
        if (this.key == null || this.getKey().isNullKey()) {
            this.setKey(new PfTimestampKey(controlLoopStatistics.getParticipantId().getName(),
                controlLoopStatistics.getParticipantId().getVersion(),
                new Date(controlLoopStatistics.getParticipantTimeStamp() == null ? 0
                    : controlLoopStatistics.getParticipantTimeStamp().getTime())));
        }
        this.setParticipantId(controlLoopStatistics.getParticipantId().asConceptKey());
        this.setState(controlLoopStatistics.getState());
        this.setAverageExecutionTime(controlLoopStatistics.getAverageExecutionTime());
        this.setEventCount(controlLoopStatistics.getEventCount());
        this.setLastExecutionTime(controlLoopStatistics.getLastExecutionTime());
        this.setUpTime(controlLoopStatistics.getUpTime());
        this.setLastEnterTime(controlLoopStatistics.getLastEnterTime());
        this.setLastStart(controlLoopStatistics.getLastStart());

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
}