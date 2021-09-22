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

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantUpdates;
import org.onap.policy.models.base.PfUtils;

/**
 * Class to represent the CONTROL_LOOP_UPDATE message that the control loop runtime sends to a participant.
 * When a participant receives this message, it creates the control loop elements contained in the message and sets them
 * to state PASSIVE. subsequent CONTROL_LOOP_STATE_CHANGE messages are used to activate the control loops.
 */
@Getter
@Setter
@ToString(callSuper = true)
public class ControlLoopUpdate extends ParticipantMessage {

    // A list of ParticipantUpdates instances which carries details of an updated participant.
    private List<ParticipantUpdates> participantUpdatesList = new ArrayList<>();
    private Integer startPhase = 0;

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

        this.participantUpdatesList = PfUtils.mapList(source.participantUpdatesList,
            ParticipantUpdates::new);
    }
}
