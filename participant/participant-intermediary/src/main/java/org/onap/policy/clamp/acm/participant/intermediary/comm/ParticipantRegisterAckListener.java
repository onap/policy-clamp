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

package org.onap.policy.clamp.acm.participant.intermediary.comm;

import org.onap.policy.clamp.acm.participant.intermediary.handler.ParticipantHandler;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantRegisterAck;
import org.springframework.stereotype.Component;

/**
 * Listener for Participant Register Ack messages sent by runtime.
 *
 */
@Component
public class ParticipantRegisterAckListener extends ParticipantAckListener<ParticipantRegisterAck> {

    /**
     * Constructs the object.
     *
     * @param participantHandler the handler for managing the state of the participant
     */
    public ParticipantRegisterAckListener(final ParticipantHandler participantHandler) {
        super(ParticipantRegisterAck.class, participantHandler, participantHandler::handleParticipantRegisterAck);
    }

    @Override
    public String getType() {
        return ParticipantMessageType.PARTICIPANT_REGISTER_ACK.name();
    }
}
