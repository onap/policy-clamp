/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation.
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionDto;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrime;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrimeAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantSync;
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
        if (!participantPrimeMsg.getParticipantDefinitionUpdates().isEmpty()) {
            // prime
            List<AutomationCompositionElementDefinition> list = new ArrayList<>();
            for (var participantDefinition : participantPrimeMsg.getParticipantDefinitionUpdates()) {
                if (participantDefinition.getParticipantId().equals(cacheProvider.getParticipantId())) {
                    list.addAll(participantDefinition.getAutomationCompositionElementDefinitionList());
                }
            }
            if (!list.isEmpty()) {
                cacheProvider.addElementDefinition(participantPrimeMsg.getCompositionId(), list);
                prime(participantPrimeMsg.getMessageId(), participantPrimeMsg.getCompositionId(), list);
            }
        } else {
            // deprime
            deprime(participantPrimeMsg.getMessageId(), participantPrimeMsg.getCompositionId());
        }
    }

    private void prime(UUID messageId, UUID compositionId, List<AutomationCompositionElementDefinition> list) {
        var inPropertiesMap = list.stream().collect(Collectors.toMap(
                AutomationCompositionElementDefinition::getAcElementDefinitionId,
                el -> el.getAutomationCompositionElementToscaNodeTemplate().getProperties()));
        var outPropertiesMap = list.stream().collect(Collectors.toMap(
                AutomationCompositionElementDefinition::getAcElementDefinitionId,
                AutomationCompositionElementDefinition::getOutProperties));
        listener.prime(messageId, new CompositionDto(compositionId, inPropertiesMap, outPropertiesMap));
    }

    private void deprime(UUID messageId, UUID compositionId) {
        var acElementsDefinitions = cacheProvider.getAcElementsDefinitions().get(compositionId);
        if (acElementsDefinitions == null) {
            // this participant does not handle this composition
            var participantPrimeAck = new ParticipantPrimeAck();
            participantPrimeAck.setCompositionId(compositionId);
            participantPrimeAck.setMessage("Already deprimed or never primed");
            participantPrimeAck.setResult(true);
            participantPrimeAck.setResponseTo(messageId);
            participantPrimeAck.setCompositionState(AcTypeState.COMMISSIONED);
            participantPrimeAck.setStateChangeResult(StateChangeResult.NO_ERROR);
            participantPrimeAck.setParticipantId(cacheProvider.getParticipantId());
            participantPrimeAck.setReplicaId(cacheProvider.getReplicaId());
            participantPrimeAck.setState(ParticipantState.ON_LINE);
            publisher.sendParticipantPrimeAck(participantPrimeAck);
            return;
        }
        var list = new ArrayList<>(acElementsDefinitions.values());
        var inPropertiesMap = list.stream().collect(Collectors.toMap(
                AutomationCompositionElementDefinition::getAcElementDefinitionId,
                el -> el.getAutomationCompositionElementToscaNodeTemplate().getProperties()));
        var outPropertiesMap = list.stream().collect(Collectors.toMap(
                AutomationCompositionElementDefinition::getAcElementDefinitionId,
                AutomationCompositionElementDefinition::getOutProperties));
        listener.deprime(messageId, new CompositionDto(compositionId, inPropertiesMap, outPropertiesMap));
    }

    /**
     * Handle a Participant Sync message.
     *
     * @param participantSyncMsg the participantRestart message
     */
    public void handleParticipantSync(ParticipantSync participantSyncMsg) {

        if (participantSyncMsg.isDelete()) {
            if (AcTypeState.COMMISSIONED.equals(participantSyncMsg.getState())) {
                cacheProvider.removeElementDefinition(participantSyncMsg.getCompositionId());
            }
            for (var automationcomposition : participantSyncMsg.getAutomationcompositionList()) {
                cacheProvider.removeAutomationComposition(automationcomposition.getAutomationCompositionId());
            }
            return;
        }

        if (!participantSyncMsg.getParticipantDefinitionUpdates().isEmpty()) {
            List<AutomationCompositionElementDefinition> list = new ArrayList<>();
            for (var participantDefinition : participantSyncMsg.getParticipantDefinitionUpdates()) {
                list.addAll(participantDefinition.getAutomationCompositionElementDefinitionList());
            }
            cacheProvider.addElementDefinition(participantSyncMsg.getCompositionId(), list);
        }

        for (var automationcomposition : participantSyncMsg.getAutomationcompositionList()) {
            cacheProvider
                    .initializeAutomationComposition(participantSyncMsg.getCompositionId(), automationcomposition);
        }
    }
}
