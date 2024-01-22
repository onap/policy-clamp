/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021,2022,2024 Nordix Foundation.
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
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantDeregisterAck;
import org.springframework.stereotype.Component;

/**
 * This class is used to send ParticipantDeregisterAck messages to participants on Kafka.
 */
@Component
public class ParticipantDeregisterAckPublisher extends AbstractParticipantAckPublisher<ParticipantDeregisterAck> {

    /**
     * Sent ParticipantDeregisterAck to Participant.
     *
     * @param responseTo the original request id in the request.
     */
    @Timed(value = "publisher.participant_deregister_ack",
            description = "PARTICIPANT_DEREGISTER_ACK messages published")
    public void send(UUID responseTo) {
        var message = new ParticipantDeregisterAck();
        message.setResponseTo(responseTo);
        message.setMessage("Participant Deregister Ack");
        message.setResult(true);
        super.send(message);
    }
}
