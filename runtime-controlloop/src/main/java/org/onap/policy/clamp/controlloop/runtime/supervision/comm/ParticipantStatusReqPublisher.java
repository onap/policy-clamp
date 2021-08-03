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

package org.onap.policy.clamp.controlloop.runtime.supervision.comm;

import java.time.Instant;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantStatusReq;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ParticipantStatusReqPublisher extends AbstractParticipantPublisher<ParticipantStatusReq> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantStatusReqPublisher.class);

    /**
     * Send ParticipantStatusReq to Participant.
     *
     * @param participantId the participant Id
     */
    public void send(ToscaConceptIdentifier participantId) {
        ParticipantStatusReq message = new ParticipantStatusReq();
        message.setParticipantId(participantId);
        message.setTimestamp(Instant.now());

        LOGGER.debug("Participant StatusReq sent {}", message);
        super.send(message);
    }
}
