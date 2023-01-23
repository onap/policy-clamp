/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021,2023 Nordix Foundation.
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

package org.onap.policy.clamp.acm.runtime.supervision.comm;

import io.micrometer.core.annotation.Timed;
import java.util.UUID;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantRegisterAck;
import org.springframework.stereotype.Component;

/**
 * This class is used to send ParticipantRegisterAck messages to participants on DMaaP.
 */
@Component
public class ParticipantRegisterAckPublisher extends AbstractParticipantAckPublisher<ParticipantRegisterAck> {

    /**
     * Send ParticipantRegisterAck to Participant.
     *
     * @param responseTo the original request id in the request.
     * @param participantId the participant Id
     */
    @Timed(value = "publisher.participant_register_ack", description = "PARTICIPANT_REGISTER_ACK messages published")
    public void send(UUID responseTo, UUID participantId) {
        var message = new ParticipantRegisterAck();
        message.setParticipantId(participantId);
        message.setResponseTo(responseTo);
        message.setMessage("Participant Register Ack");
        message.setResult(true);
        super.send(message);
    }
}
