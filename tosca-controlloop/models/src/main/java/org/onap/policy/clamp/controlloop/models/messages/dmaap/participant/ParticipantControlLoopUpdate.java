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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;

/**
 * Class to represent the PARTICIPANT_CONTROL_LOOP_UPDATE message that the control loop runtime sends to a participant.
 * When a participant receives this message, it creates the control loop elements contained in the message and sets them
 * to state PASSIVE. subsequent PARTICIPANT_CONTROL_LOOP_STATE_CHANGE messages are used to activate the control loops.
 */
@Getter
@Setter
@ToString(callSuper = true)
public class ParticipantControlLoopUpdate extends ParticipantMessage {
    /**
     * Definition of the control loop.
     */
    private ToscaNodeTemplate controlLoopDefinition;

    /**
     * Control loops that should be updated on the participant. This is a complete list, so participants should be
     * prepared to create new control loop elements listed and delete control loop elements that are no longer listed.
     * Note: this list may be empty, as a participant may remain attached to the control loop runtime even if all of the
     * control loop elements are removed from it.
     */
    private Map<UUID, ToscaNodeTemplate> controlLoopElementMap = new HashMap<>();
    // Add the parameter values for each participant
    // Add the participant to ToscaNodeTemplate mapping

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

        this.controlLoopDefinition = new ToscaNodeTemplate(controlLoopDefinition);
        this.controlLoopElementMap = PfUtils.mapMap(controlLoopElementMap, ToscaNodeTemplate::new);
    }
}
