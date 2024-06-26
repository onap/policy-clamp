/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2024 Nordix Foundation.
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

import java.util.UUID;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;

/**
 * Class to represent participant Ack message.
 */
@Getter
@Setter
@ToString
public class ParticipantAckMessage {

    // The responseTo field should match the original request id in the request.
    private UUID responseTo;

    // Intermediary result: Success/Fail.
    private Boolean result;

    // Indicating participant failure
    private StateChangeResult stateChangeResult;

    // Message indicating reason for failure
    private String message;

    private ParticipantMessageType messageType;

    private UUID compositionId;

    /**
     * Participant ID, or {@code null} for messages from participants.
     */
    private UUID participantId;

    private UUID replicaId;

    /**
     * Participant State, or {@code null} for messages from participants.
     */
    private ParticipantState state;

    /**
     * Constructor for instantiating a participant ack message class.
     *
     * @param messageType the message type
     */
    public ParticipantAckMessage(final ParticipantMessageType messageType) {
        this.messageType = messageType;
    }

    /**
     * Constructs the object, making a deep copy.
     *
     * @param source source from which to copy
     */
    public ParticipantAckMessage(ParticipantAckMessage source) {
        this.responseTo = source.responseTo;
        this.result = source.result;
        this.stateChangeResult = source.stateChangeResult;
        this.message = source.message;
        this.messageType = source.messageType;
        this.compositionId = source.compositionId;
        this.participantId = source.participantId;
        this.replicaId = source.replicaId;
        this.state = source.state;
    }

    /**
     * Determines if this message applies to this participant type.
     *
     * @param participantId id of the participant to match against
     * @param replicaId id of the participant to match against
     * @return {@code true} if this message applies to this participant, {@code false} otherwise
     */
    public boolean appliesTo(@NonNull final UUID participantId, @NonNull final UUID replicaId) {
        // Broadcast message to all participants
        if ((this.participantId == null)
                || (participantId.equals(this.participantId) && this.replicaId == null)) {
            return true;
        }

        // Targeted message at this specific participant
        return replicaId.equals(this.replicaId);
    }
}
