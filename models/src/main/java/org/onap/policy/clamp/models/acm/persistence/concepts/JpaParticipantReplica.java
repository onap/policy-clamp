/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2024 Nordix Foundation.
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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.onap.policy.clamp.models.acm.concepts.ParticipantReplica;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.utils.TimestampHelper;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.models.base.PfAuthorative;
import org.onap.policy.models.base.Validated;

@Entity
@Table(name = "ParticipantReplica")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Data
@EqualsAndHashCode(callSuper = false)
public class JpaParticipantReplica extends Validated implements PfAuthorative<ParticipantReplica> {

    @Id
    @NotNull
    private String replicaId;

    @NotNull
    @Column
    private String participantId;

    @Column
    @NotNull
    private ParticipantState participantState;

    @Column
    @NotNull
    private Timestamp lastMsg;

    public JpaParticipantReplica() {
        this(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

    public JpaParticipantReplica(@NonNull String replicaId, @NonNull String participantId) {
        this.replicaId = replicaId;
        this.participantId = participantId;
    }

    @Override
    public ParticipantReplica toAuthorative() {
        var participantReplica = new ParticipantReplica();
        participantReplica.setReplicaId(UUID.fromString(replicaId));
        participantReplica.setParticipantState(participantState);
        participantReplica.setLastMsg(lastMsg.toString());
        return participantReplica;
    }

    @Override
    public void fromAuthorative(@NonNull ParticipantReplica participantReplica) {
        this.replicaId = participantReplica.getReplicaId().toString();
        this.participantState = participantReplica.getParticipantState();
        this.lastMsg = TimestampHelper.toTimestamp(participantReplica.getLastMsg());
    }
}
