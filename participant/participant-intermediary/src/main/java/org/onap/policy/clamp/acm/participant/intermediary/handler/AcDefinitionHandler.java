/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.participant.intermediary.handler;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.acm.participant.intermediary.handler.cache.CacheProvider;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDefinition;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.dto.CompositionDto;
import org.onap.policy.clamp.models.acm.dto.ParticipantPrimeDto;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrime;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrimeAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantSync;
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.PrimeOrder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AcDefinitionHandler {

    private final CacheProvider cacheProvider;
    private final ParticipantMessagePublisher publisher;
    private final ThreadHandler listener;

    /**
     * Handle a participant Prime message.
     *
     * @param participantPrimeMsg the ParticipantPrime message
     */
    public void handlePrime(ParticipantPrime participantPrimeMsg) {
        if (PrimeOrder.PRIME.equals(participantPrimeMsg.getPrimeOrder())) {
            prime(participantPrimeMsg);
        } else {
            deprime(participantPrimeMsg);
        }
    }

    private void prime(ParticipantPrime participantPrimeMsg) {
        var compositionDto = getCompositionDto(participantPrimeMsg.getPrimeDtoList());
        if (compositionDto != null) {
            cacheProvider.addElementDefinition(compositionDto,
                    participantPrimeMsg.getRevisionIdComposition());
            listener.prime(participantPrimeMsg.getMessageId(), compositionDto);
        }
    }

    private void deprime(ParticipantPrime participantPrimeMsg) {
        var compositionDto = getCompositionDto(participantPrimeMsg.getPrimeDtoList());
        if (compositionDto != null) {
            listener.deprime(participantPrimeMsg.getMessageId(), compositionDto);
            return;
        }

        // this participant does not handle this composition
        var participantPrimeAck = new ParticipantPrimeAck();
        participantPrimeAck.setCompositionId(participantPrimeMsg.getCompositionId());
        participantPrimeAck.setMessage("Already deprimed or never primed");
        participantPrimeAck.setResponseTo(participantPrimeMsg.getMessageId());
        participantPrimeAck.setCompositionState(AcTypeState.COMMISSIONED);
        participantPrimeAck.setStateChangeResult(StateChangeResult.NO_ERROR);
        participantPrimeAck.setParticipantId(cacheProvider.getParticipantId());
        participantPrimeAck.setReplicaId(cacheProvider.getReplicaId());
        publisher.sendParticipantPrimeAck(participantPrimeAck);
    }

    private CompositionDto getCompositionDto(List<ParticipantPrimeDto> primeDtoList) {
        if (primeDtoList == null || primeDtoList.isEmpty()) {
            return null;
        }
        return primeDtoList.stream()
                .filter(p -> cacheProvider.getParticipantId().equals(p.getParticipantId()))
                .map(ParticipantPrimeDto::getCompositionDto)
                .findFirst()
                .orElse(null);
    }

    private List<AutomationCompositionElementDefinition> collectAcElementDefinition(
            List<ParticipantDefinition> participantDefinitionList) {
        return participantDefinitionList.stream()
                .filter(participantDefinition -> participantDefinition.getParticipantId()
                        .equals(cacheProvider.getParticipantId()))
                .map(ParticipantDefinition::getAutomationCompositionElementDefinitionList)
                .flatMap(List::stream)
                .toList();
    }

    /**
     * Handle a Participant Sync message.
     *
     * @param participantSyncMsg the participantRestart message
     */
    public void handleParticipantSync(ParticipantSync participantSyncMsg) {

        if (participantSyncMsg.isDelete()) {
            deleteScenario(participantSyncMsg);
            return;
        }

        if (!participantSyncMsg.getParticipantDefinitionUpdates().isEmpty()) {
            if (StateChangeResult.TIMEOUT.equals(participantSyncMsg.getStateChangeResult())) {
                listener.cleanExecution(participantSyncMsg.getCompositionId(), participantSyncMsg.getMessageId());
            }

            var list = collectAcElementDefinition(participantSyncMsg.getParticipantDefinitionUpdates());
            if (!list.isEmpty()) {
                cacheProvider.addElementDefinition(participantSyncMsg.getCompositionId(), list,
                        participantSyncMsg.getRevisionIdComposition());
            }
        } else if (participantSyncMsg.isRestarting()) {
            checkComposition(participantSyncMsg);
        }

        for (var automationcomposition : participantSyncMsg.getAutomationcompositionList()) {
            cacheProvider.initializeAutomationComposition(
                    participantSyncMsg.getCompositionId(), automationcomposition);
            if (StateChangeResult.TIMEOUT.equals(automationcomposition.getStateChangeResult())) {
                for (var element : automationcomposition.getAcElementList()) {
                    listener.cleanExecution(element.getId(), participantSyncMsg.getMessageId());
                }
            }
        }
    }

    private void checkComposition(ParticipantSync participantSyncMsg) {
        // edge case scenario in migration with remove/add elements,
        // when composition or target composition doesn't contain elements from this participant
        for (var msg : cacheProvider.getMessagesOnHold().values()) {
            if (participantSyncMsg.getCompositionId().equals(msg.getCompositionTargetId())) {
                msg.setCompositionTargetId(null);
            }
            if (participantSyncMsg.getCompositionId().equals(msg.getCompositionId())) {
                msg.setCompositionId(null);
            }
        }
    }

    private void deleteScenario(ParticipantSync participantSyncMsg) {
        if (AcTypeState.COMMISSIONED.equals(participantSyncMsg.getState())) {
            cacheProvider.removeElementDefinition(participantSyncMsg.getCompositionId());
        }
        for (var automationcomposition : participantSyncMsg.getAutomationcompositionList()) {
            cacheProvider.removeAutomationComposition(automationcomposition.getAutomationCompositionId());
        }
    }
}
