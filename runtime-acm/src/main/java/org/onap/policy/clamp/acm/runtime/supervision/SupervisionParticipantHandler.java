/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
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

package org.onap.policy.clamp.acm.runtime.supervision;

import io.micrometer.core.annotation.Timed;
import lombok.AllArgsConstructor;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantDeregisterAckPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantRegisterAckPublisher;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantDeregister;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessage;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantRegister;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantStatus;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class handles supervision of participant status.
 */
@Component
@AllArgsConstructor
public class SupervisionParticipantHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SupervisionParticipantHandler.class);

    private final ParticipantProvider participantProvider;
    private final ParticipantRegisterAckPublisher participantRegisterAckPublisher;
    private final ParticipantDeregisterAckPublisher participantDeregisterAckPublisher;

    /**
     * Handle a ParticipantRegister message from a participant.
     *
     * @param participantRegisterMsg the ParticipantRegister message received from a participant
     */
    @MessageIntercept
    @Timed(value = "listener.participant_register", description = "PARTICIPANT_REGISTER messages received")
    public void handleParticipantMessage(ParticipantRegister participantRegisterMsg) {
        LOGGER.debug("Participant Register received {}", participantRegisterMsg);
        saveParticipantStatus(participantRegisterMsg);

        participantRegisterAckPublisher.send(participantRegisterMsg.getMessageId(),
                participantRegisterMsg.getParticipantId(), participantRegisterMsg.getParticipantType());
    }

    /**
     * Handle a ParticipantDeregister message from a participant.
     *
     * @param participantDeregisterMsg the ParticipantDeregister message received from a participant
     */
    @MessageIntercept
    @Timed(value = "listener.participant_deregister", description = "PARTICIPANT_DEREGISTER messages received")
    public void handleParticipantMessage(ParticipantDeregister participantDeregisterMsg) {
        LOGGER.debug("Participant Deregister received {}", participantDeregisterMsg);
        var participantOpt = participantProvider.findParticipant(participantDeregisterMsg.getParticipantId());

        if (participantOpt.isPresent()) {
            var participant = participantOpt.get();
            participant.setParticipantState(ParticipantState.OFF_LINE);
            participantProvider.saveParticipant(participant);
        }

        participantDeregisterAckPublisher.send(participantDeregisterMsg.getMessageId());
    }

    /**
     * Handle a ParticipantStatus message from a participant.
     *
     * @param participantStatusMsg the ParticipantStatus message received from a participant
     */
    @MessageIntercept
    @Timed(value = "listener.participant_status", description = "PARTICIPANT_STATUS messages received")
    public void handleParticipantMessage(ParticipantStatus participantStatusMsg) {
        LOGGER.debug("Participant Status received {}", participantStatusMsg);
        saveParticipantStatus(participantStatusMsg);
    }

    private void saveParticipantStatus(ParticipantMessage participantMessage) {
        var participantOpt = participantProvider.findParticipant(participantMessage.getParticipantId());

        if (participantOpt.isEmpty()) {
            var participant = new Participant();
            participant.setName(participantMessage.getParticipantId().getName());
            participant.setVersion(participantMessage.getParticipantId().getVersion());
            participant.setDefinition(participantMessage.getParticipantId());
            participant.setParticipantType(participantMessage.getParticipantType());
            participant.setParticipantState(ParticipantState.ON_LINE);

            participantProvider.saveParticipant(participant);
        } else {
            var participant = participantOpt.get();
            participant.setParticipantState(ParticipantState.ON_LINE);

            participantProvider.saveParticipant(participant);
        }
    }
}
