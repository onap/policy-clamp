/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.participant.intermediary.comm;

import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ControlLoopUpdate;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.clamp.controlloop.participant.intermediary.handler.ParticipantHandler;
import org.springframework.stereotype.Component;

/**
 * Listener for Control Loop Update messages sent by CLAMP.
 */
@Component
public class ControlLoopUpdateListener extends ParticipantListener<ControlLoopUpdate> {

    /**
     * Constructs the object.
     *
     * @param participantHandler the handler for managing the state of the participant
     */
    public ControlLoopUpdateListener(final ParticipantHandler participantHandler) {
        super(ControlLoopUpdate.class, participantHandler, participantHandler::handleControlLoopUpdate);
    }

    @Override
    public String getType() {
        return ParticipantMessageType.CONTROL_LOOP_UPDATE.name();
    }
}
