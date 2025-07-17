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

package org.onap.policy.clamp.models.acm.messages.kafka.participant;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * Class to represent the base class for various messages that will be exchanged between the ACM runtime and
 * participants.
 */
@Getter
@Setter
@ToString
public class ParticipantMessage {
    @Setter(AccessLevel.NONE)
    private ParticipantMessageType messageType;

    private UUID messageId = UUID.randomUUID();

    /**
     * Time-stamp, in milliseconds, when the message was created. Defaults to the current time.
     */
    private Instant timestamp = Instant.now();

    /**
     * Participant ID, or {@code null} for messages from participants.
     */
    private UUID participantId;

    private UUID replicaId;

    /**
     * Automation Composition ID, or {@code null} for messages to participants.
     */
    private UUID automationCompositionId;

    private UUID compositionId;

    private UUID revisionIdComposition;
    private UUID revisionIdInstance;

    /**
     * List of participantId that should receive the message.
     */
    private Set<UUID> participantIdList = new HashSet<>();

    /**
     * Constructor for instantiating a participant message class.
     *
     * @param messageType the message type
     */
    public ParticipantMessage(final ParticipantMessageType messageType) {
        this.messageType = messageType;
    }

    /**
     * Constructs the object, making a deep copy. Does <i>not</i> copy the request id or the time stamp.
     *
     * @param source source from which to copy
     */
    public ParticipantMessage(final ParticipantMessage source) {
        this.messageId = source.messageId;
        this.timestamp = source.timestamp;
        this.messageType = source.messageType;
        this.participantId = source.participantId;
        this.replicaId = source.replicaId;
        this.automationCompositionId = source.automationCompositionId;
        this.compositionId = source.compositionId;
        this.revisionIdComposition = source.revisionIdComposition;
        this.revisionIdInstance = source.revisionIdInstance;
        this.participantIdList = new HashSet<>(source.participantIdList);
    }

    /**
     * Determines if this message applies to this participant type.
     *
     * @param refParticipantId id of the participant from properties file to match against
     * @param refReplicaId id of the replica from properties file to match against
     * @return {@code true} if this message applies to this participant, {@code false} otherwise
     */
    public boolean appliesTo(@NonNull final UUID refParticipantId, @NonNull final UUID refReplicaId) {
        // Broadcast message to specific list of participants
        // or all participants when participantIdList is empty
        // filter backward compatible with old ACM-r
        if (participantIdList != null && !participantIdList.isEmpty()
                && !participantIdList.contains(refParticipantId)) {
            return false;
        }
        // Broadcast message to all participants and all replicas or specific participant and all replicas,
        // filter backward compatible with old ACM-r
        if ((this.participantId == null)
                || (refParticipantId.equals(this.participantId) && this.replicaId == null)) {
            return true;
        }
        // Targeted message at a specific participant and replica
        return refReplicaId.equals(this.replicaId);
    }
}
