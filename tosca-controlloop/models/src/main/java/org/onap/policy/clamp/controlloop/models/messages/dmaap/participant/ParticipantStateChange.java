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
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantState;

/**
 * Class to represent the PARTICIPANT_STATE_CHANGE message that the control loop runtime will send to participants
 * to change their state.
 */
@Getter
@Setter
@ToString(callSuper = true)
public class ParticipantStateChange extends ParticipantMessage {
    private ParticipantState state;

    /**
     * Constructor for instantiating ParticipantStateChange class with message name.
     *
     */
    public ParticipantStateChange() {
        super(ParticipantMessageType.PARTICIPANT_STATE_CHANGE);
    }

    /**
     * Constructs the object, making a deep copy.
     *
     * @param source source from which to copy
     */
    public ParticipantStateChange(ParticipantStateChange source) {
        super(source);

        this.state = source.state;
    }
}
