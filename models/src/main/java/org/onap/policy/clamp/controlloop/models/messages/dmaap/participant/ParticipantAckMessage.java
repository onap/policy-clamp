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

package org.onap.policy.clamp.controlloop.models.messages.dmaap.participant;

import java.util.UUID;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Class to represent participant Ack message.
 */
@Getter
@Setter
@ToString
public class ParticipantAckMessage {

    // The responseTo field should match the original request id in the request.
    private UUID responseTo;

    // Result: Success/Fail.
    private Boolean result;

    // Message indicating reason for failure
    private String message;

    private ParticipantMessageType messageType;

    /**
     * Participant Type, or {@code null} for messages from participants.
     */
    private ToscaConceptIdentifier participantType;

    /**
     * Participant ID, or {@code null} for messages from participants.
     */
    private ToscaConceptIdentifier participantId;

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
        this.message = source.message;
        this.messageType = source.messageType;
        this.participantType = source.participantType;
        this.participantId = source.participantId;
        this.state = source.state;
    }

    /**
     * Determines if this message applies to this participant type.
     *
     * @param participantType type of the participant to match against
     * @param participantId id of the participant to match against
     * @return {@code true} if this message applies to this participant, {@code false} otherwise
     */
    public boolean appliesTo(@NonNull final ToscaConceptIdentifier participantType,
            @NonNull final ToscaConceptIdentifier participantId) {
        // Broadcast message to all participants
        if (this.participantType == null) {
            return true;
        }

        if (!participantType.equals(this.participantType)) {
            return false;
        }

        // Broadcast message to all control loop elements on this participant
        if (this.participantId == null) {
            return true;
        }

        // Targeted message at this specific participant
        return participantId.equals(this.participantId);
    }
}
