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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Class to represent the CONTROL_LOOP_UPDATE message that the control loop runtime sends to a participant.
 * When a participant receives this message, it creates the control loop elements contained in the message and sets them
 * to state PASSIVE. subsequent CONTROL_LOOP_STATE_CHANGE messages are used to activate the control loops.
 */
@Getter
@Setter
@ToString(callSuper = true)
public class ControlLoopUpdate extends ParticipantMessage {

    // The control loop
    private ControlLoop controlLoop;

    // A map with Participant ID as its key, and a map of ControlLoopElements as value.
    private Map<ToscaConceptIdentifier, Map<UUID, ControlLoopElement>>
            participantUpdateMap = new LinkedHashMap<>();

    /**
     * Constructor for instantiating ControlLoopUpdate class with message name.
     *
     */
    public ControlLoopUpdate() {
        super(ParticipantMessageType.CONTROL_LOOP_UPDATE);
    }

    /**
     * Constructs the object, making a deep copy.
     *
     * @param source source from which to copy
     */
    public ControlLoopUpdate(ControlLoopUpdate source) {
        super(source);

        this.controlLoop = source.controlLoop;
        this.participantUpdateMap = PfUtils.mapMap(source.participantUpdateMap,
                clElementMap -> PfUtils.mapMap(clElementMap, ControlLoopElement::new));
    }
}
