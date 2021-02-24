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
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoops;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantHealthStatus;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantState;

/**
 * Class to represent the PARTICIPANT_STATUS message that all the participants send to the control loop runtime.
 */
@Getter
@Setter
@ToString(callSuper = true)
public class ParticipantStatus extends ParticipantMessage {
    // The response should be completed if this message is a response to a request from the Control Loop Runtime
    private ParticipantResponseDetails response;

    // State and health status of the participant
    private ParticipantState state;
    private ParticipantHealthStatus healthStatus;

    // Control Loops on the participant
    private ControlLoops controlLoops;

    // Description. May be left {@code null}.
    private String message;

    /**
     * Constructor for instantiating ParticipantStatus class with message name.
     *
     */
    public ParticipantStatus() {
        super(ParticipantMessageType.PARTICIPANT_STATUS);
    }

    /**
     * Constructs the object, making a deep copy.
     *
     * @param source source from which to copy
     */
    public ParticipantStatus(final ParticipantStatus source) {
        super(source);

        this.state = source.state;
        this.healthStatus = source.healthStatus;
        this.message = source.message;
        this.controlLoops = (source.controlLoops == null ? null : new ControlLoops(source.controlLoops));
        this.response = (source.response == null ? null : new ParticipantResponseDetails(source.response));
    }
}
