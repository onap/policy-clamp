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
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

/**
 * Class to represent the PARTICIPANT_CONTROL_LOOP_UPDATE message that the control loop runtime sends to a participant.
 * When a participant receives this message, it creates the control loop elements contained in the message and sets them
 * to state PASSIVE. subsequent PARTICIPANT_CONTROL_LOOP_STATE_CHANGE messages are used to activate the control loops.
 */
@Getter
@Setter
@ToString(callSuper = true)
public class ParticipantControlLoopUpdate extends ParticipantMessage {
    // The control loop
    private ControlLoop controlLoop;

    // A service template containing a complete definition of the control loop
    private ToscaServiceTemplate controlLoopDefinition;

    /**
     * Constructor for instantiating ParticipantControlLoopUpdate class with message name.
     *
     */
    public ParticipantControlLoopUpdate() {
        super(ParticipantMessageType.PARTICIPANT_CONTROL_LOOP_UPDATE);
    }

    /**
     * Constructs the object, making a deep copy.
     *
     * @param source source from which to copy
     */
    public ParticipantControlLoopUpdate(ParticipantControlLoopUpdate source) {
        super(source);

        this.controlLoopDefinition = new ToscaServiceTemplate(source.controlLoopDefinition);
    }
}
