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

package org.onap.policy.clamp.models.acm.messages.dmaap.participant;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Class to represent the PARTICIPANT_STATUS_REQ message that the ACM runtime
 * sends to participants for an immediate ParticipantStatus from participants.
 */
@Getter
@Setter
@ToString(callSuper = true)
public class ParticipantStatusReq extends ParticipantMessage {

    /**
     * Constructor for instantiating a participant status request class.
     */
    public ParticipantStatusReq() {
        super(ParticipantMessageType.PARTICIPANT_STATUS_REQ);
    }

    /**
     * Constructs the object, making a deep copy.
     *
     * @param source source from which to copy
     */
    public ParticipantStatusReq(final ParticipantStatusReq source) {
        super(source);
    }
}
