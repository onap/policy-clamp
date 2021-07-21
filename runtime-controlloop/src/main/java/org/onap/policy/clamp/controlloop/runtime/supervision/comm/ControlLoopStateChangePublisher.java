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

package org.onap.policy.clamp.controlloop.runtime.supervision.comm;

import java.util.UUID;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ControlLoopStateChange;
import org.springframework.stereotype.Component;

/**
 * This class is used to send ControlLoopStateChangePublisher messages to participants on DMaaP.
 */
@Component
public class ControlLoopStateChangePublisher
        extends AbstractParticipantPublisher<ControlLoopStateChange> {

    /**
     * Send ControlLoopStateChange to Participant.
     *
     * @param controlLoop the ControlLoop
     */
    public void send(ControlLoop controlLoop) {
        var clsc = new ControlLoopStateChange();
        clsc.setControlLoopId(controlLoop.getKey().asIdentifier());
        clsc.setMessageId(UUID.randomUUID());
        clsc.setOrderedState(controlLoop.getOrderedState());

        super.send(clsc);
    }
}
