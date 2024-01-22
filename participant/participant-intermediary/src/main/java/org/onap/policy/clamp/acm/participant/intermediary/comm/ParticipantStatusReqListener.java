/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021,2024 Nordix Foundation.
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
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatusReq;
import org.springframework.stereotype.Component;

/**
 * Listener for Participant status request messages sent by runtime to all/one participant.
 */
@Component
public class ParticipantStatusReqListener extends ParticipantListener<ParticipantStatusReq> {

    /**
     * Constructs the object.
     *
     * @param participantHandler the handler for managing the state and health of the participant
     */
    public ParticipantStatusReqListener(final ParticipantHandler participantHandler) {
        super(ParticipantStatusReq.class, participantHandler, participantHandler::handleParticipantStatusReq);
    }

    @Override
    public String getType() {
        return ParticipantMessageType.PARTICIPANT_STATUS_REQ.name();
    }
}
