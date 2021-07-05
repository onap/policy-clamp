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

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Class to represent the PARTICIPANT_DEREGISTER_ACK message that control loop runtime sends to the participant.
 */
@Getter
@Setter
@ToString(callSuper = true)
public class ParticipantDeregisterAck extends ParticipantAckMessage {

    /**
     * Constructor for instantiating ParticipantDeregisterAck class with message name.
     *
     */
    public ParticipantDeregisterAck() {
        super(ParticipantMessageType.PARTICIPANT_DEREGISTER_ACK);
    }

    /**
     * Constructs the object, making a deep copy.
     *
     * @param source source from which to copy
     */
    public ParticipantDeregisterAck(final ParticipantDeregisterAck source) {
        super(source);
    }
}
