/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2024-2026 OpenInfra Foundation Europe. All rights reserved.
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
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.onap.policy.clamp.models.acm.base.PfAuthorative;
import org.onap.policy.clamp.models.acm.concepts.ParticipantReplica;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.utils.TimestampHelper;

@Entity
@Table(name = "ParticipantReplica")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class JpaParticipantReplica implements PfAuthorative<ParticipantReplica> {

    @Id
    @NotNull
    @EqualsAndHashCode.Include
    @Column(nullable = false)
    private String replicaId;

    @NotNull
    @Column(nullable = false)
    private String participantId;

    @NotNull
    @Column(nullable = false)
    private ParticipantState participantState;

    @NotNull
    @Column(nullable = false)
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
