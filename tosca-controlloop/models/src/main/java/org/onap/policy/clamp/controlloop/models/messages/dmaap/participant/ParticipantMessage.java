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

import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Class to represent the base class for various messages that will be exchanged between
 * the control loop runtime and participants.
 */
@Getter
@Setter
@ToString
public class ParticipantMessage {
    @Setter(AccessLevel.NONE)
    private ParticipantMessageType messageType;

    private UUID messageId = UUID.randomUUID();

    /**
     * Time-stamp, in milliseconds, when the message was created. Defaults to the current
     * time.
     */
    private Instant timestamp = Instant.now();

    /**
     * Participant ID, or {@code null} for state-change broadcast messages.
     */
    private ToscaConceptIdentifier participantId;

    /**
     * Control loop ID. For state-change messages, this may be {@code null}.
     */
    private ToscaConceptIdentifier controlLoopId;

    /**
     * Constructor for instantiating a participant message class.
     *
     * @param messageType the message type
     */
    public ParticipantMessage(final ParticipantMessageType messageType) {
        this.messageType = messageType;
    }

    /**
     * Constructs the object, making a deep copy. Does <i>not</i> copy the request id or
     * the time stamp.
     *
     * @param source source from which to copy
     */
    public ParticipantMessage(final ParticipantMessage source) {
        this.messageType = source.messageType;
        this.participantId = source.participantId;
        this.controlLoopId = source.controlLoopId;
    }

    /**
     * Determines if this message applies to this participant.
     *
     * @param participantId id of the participant to match against
     * @return {@code true} if this message applies to this participant, {@code false} otherwise
     */
    public boolean appliesTo(@NonNull final ToscaConceptIdentifier participantId) {
        if (participantId.equals(this.participantId)) {
            return true;
        }

        // false: message included a participant ID, but it does not match
        // true: its a broadcast message
        return this.participantId == null;
    }
}
