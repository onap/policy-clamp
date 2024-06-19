/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation.
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
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantSyncPublisher;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.NodeTemplateState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrimeAck;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
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
    private final ParticipantSyncPublisher participantSyncPublisher;

    /**
     * Handle a ParticipantPrimeAck message from a participant.
     *
     * @param participantPrimeAckMessage the ParticipantPrimeAck message received from a participant
     */
    @Timed(value = "listener.participant_prime_ack", description = "PARTICIPANT_PRIME_ACK messages received")
    public void handleParticipantMessage(ParticipantPrimeAck participantPrimeAckMessage) {
        var acDefinitionOpt = acDefinitionProvider.findAcDefinition(participantPrimeAckMessage.getCompositionId());
        if (acDefinitionOpt.isEmpty()) {
            LOGGER.warn("AC Definition not found in database {}", participantPrimeAckMessage.getCompositionId());
            return;
        }
        var acDefinition = acDefinitionOpt.get();
        if (!AcTypeState.PRIMING.equals(acDefinition.getState())
                && !AcTypeState.DEPRIMING.equals(acDefinition.getState()) && acDefinition.getRestarting() == null) {
            LOGGER.error("AC Definition {} already primed/deprimed with participant {}",
                    participantPrimeAckMessage.getCompositionId(), participantPrimeAckMessage.getParticipantId());
            return;
        }
        handleParticipantPrimeAck(participantPrimeAckMessage, acDefinition);
    }

    private void handleParticipantPrimeAck(ParticipantPrimeAck participantPrimeAckMessage,
            AutomationCompositionDefinition acDefinition) {
        var finalState = AcTypeState.PRIMING.equals(acDefinition.getState())
                || AcTypeState.PRIMED.equals(acDefinition.getState()) ? AcTypeState.PRIMED : AcTypeState.COMMISSIONED;
        var msgInErrors = StateChangeResult.FAILED.equals(participantPrimeAckMessage.getStateChangeResult());
        boolean inProgress = !StateChangeResult.FAILED.equals(acDefinition.getStateChangeResult());
        boolean toUpdate = false;
        if (inProgress && msgInErrors) {
            acDefinition.setStateChangeResult(StateChangeResult.FAILED);
            toUpdate = true;
        }

        boolean completed = true;
        boolean restarting = false;
        for (var element : acDefinition.getElementStateMap().values()) {
            handlePrimeAckElement(participantPrimeAckMessage, element);
            if (!finalState.equals(element.getState())) {
                completed = false;
            }
            if (element.getRestarting() != null) {
                restarting = true;
            }
        }

        if (inProgress && !msgInErrors && completed) {
            toUpdate = true;
            acDefinition.setState(finalState);
            if (StateChangeResult.TIMEOUT.equals(acDefinition.getStateChangeResult())) {
                acDefinition.setStateChangeResult(StateChangeResult.NO_ERROR);
            }
        }
        if (!restarting && acDefinition.getRestarting() != null) {
            toUpdate = true;
            acDefinition.setRestarting(null);
        }
        if (toUpdate) {
            acDefinitionProvider.updateAcDefinitionState(acDefinition.getCompositionId(), acDefinition.getState(),
                acDefinition.getStateChangeResult(), acDefinition.getRestarting());
            if (!participantPrimeAckMessage.getParticipantId().equals(participantPrimeAckMessage.getReplicaId())) {
                participantSyncPublisher.sendSync(acDefinition, participantPrimeAckMessage.getReplicaId());
            }
        }
    }

    private void handlePrimeAckElement(ParticipantPrimeAck participantPrimeAckMessage, NodeTemplateState element) {
        if (participantPrimeAckMessage.getParticipantId().equals(element.getParticipantId())) {
            element.setMessage(participantPrimeAckMessage.getMessage());
            element.setState(participantPrimeAckMessage.getCompositionState());
            element.setRestarting(null);
            acDefinitionProvider.updateAcDefinitionElement(element, participantPrimeAckMessage.getCompositionId());
        }
    }
}
