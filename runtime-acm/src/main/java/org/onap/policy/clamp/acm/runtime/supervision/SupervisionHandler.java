/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2025 Nordix Foundation.
 *  Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrimeAck;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.MessageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class handles supervision of automation composition definition, so only one object of this type should be built
 * at a time.
 */
@Component
@AllArgsConstructor
public class SupervisionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SupervisionHandler.class);

    private final AcDefinitionProvider acDefinitionProvider;
    private final MessageProvider messageProvider;

    /**
     * Handle a ParticipantPrimeAck message from a participant.
     *
     * @param participantPrimeAckMessage the ParticipantPrimeAck message received from a participant
     */
    @MessageIntercept
    @Timed(value = "listener.participant_prime_ack", description = "PARTICIPANT_PRIME_ACK messages received")
    public void handleParticipantMessage(ParticipantPrimeAck participantPrimeAckMessage) {
        if (participantPrimeAckMessage.getCompositionId() == null
                || participantPrimeAckMessage.getCompositionState() == null
                || participantPrimeAckMessage.getStateChangeResult() == null) {
            LOGGER.error("Not valid ParticipantPrimeAck message");
            return;
        }
        if (AcTypeState.PRIMING.equals(participantPrimeAckMessage.getCompositionState())
                || AcTypeState.DEPRIMING.equals(participantPrimeAckMessage.getCompositionState())) {
            LOGGER.error("Not valid state {}", participantPrimeAckMessage.getCompositionState());
            return;
        }
        if (!StateChangeResult.NO_ERROR.equals(participantPrimeAckMessage.getStateChangeResult())
                && !StateChangeResult.FAILED.equals(participantPrimeAckMessage.getStateChangeResult())) {
            LOGGER.error("Vot valid stateChangeResult {} ", participantPrimeAckMessage.getStateChangeResult());
            return;
        }
        var acDefinitionOpt = acDefinitionProvider.findAcDefinition(participantPrimeAckMessage.getCompositionId());
        if (acDefinitionOpt.isEmpty()) {
            LOGGER.warn("AC Definition not found in database {}", participantPrimeAckMessage.getCompositionId());
            return;
        }
        var acDefinition = acDefinitionOpt.get();
        if (!AcTypeState.PRIMING.equals(acDefinition.getState())
                && !AcTypeState.DEPRIMING.equals(acDefinition.getState())) {
            LOGGER.error("AC Definition {} already primed/deprimed with participant {}",
                    participantPrimeAckMessage.getCompositionId(), participantPrimeAckMessage.getParticipantId());
            return;
        }
        messageProvider.save(participantPrimeAckMessage);
    }
}
