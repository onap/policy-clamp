/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2026 OpenInfra Foundation Europe. All rights reserved.
 * ================================================================================
 * Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDefinition;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.dto.ParticipantPrimeDto;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrime;
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.PrimeOrder;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.clamp.models.acm.utils.TimestampHelper;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.springframework.stereotype.Component;

/**
 * This class is used to send ParticipantPrime messages to participants on Kafka.
 */
@Component
@RequiredArgsConstructor
public class ParticipantPrimePublisher {

    private final ParticipantProvider participantProvider;
    private final AcRuntimeParameterGroup acRuntimeParameterGroup;

    private final ParticipantPublisher participantPublisher;

    /**
     * Send ParticipantPrime to Participant
     * if participantId is null then message is broadcast.
     *
     * @param elementMap the map of participantId with associated elements
     * @param compositionId the compositionId
     * @param revisionId last update
     */
    @Timed(value = "publisher.participant_update", description = "PARTICIPANT_UPDATE messages published")
    public void sendPriming(Map<UUID, List<AutomationCompositionElementDefinition>> elementMap, UUID compositionId,
                            UUID revisionId) {
        var message = new ParticipantPrime();
        message.setCompositionId(compositionId);
        var participantDefinitions = AcmUtils.prepareParticipantDefinitions(elementMap);
        message.setParticipantIdList(participantDefinitions.stream()
                .map(ParticipantDefinition::getParticipantId).collect(Collectors.toSet()));
        message.setTimestamp(Instant.now());
        message.setRevisionIdComposition(revisionId);
        message.setParticipantDefinitionUpdates(participantDefinitions);
        message.setPrimeDtoList(AcmUtils.preparePrimeDtoList(elementMap, compositionId));
        message.setPrimeOrder(PrimeOrder.PRIME);
        participantPublisher.send(message);
    }

    /**
     * Prepare the Priming message creating the list of ParticipantDefinition to send
     * and fill the ElementState map of the AC Definition.
     *
     * @param acmDefinition the AutomationComposition Definition
     * @return Map of participantId and the associated elementDefinitions
     */
    public Map<UUID, List<AutomationCompositionElementDefinition>> prepareParticipantPriming(
            AutomationCompositionDefinition acmDefinition, AcTypeState acTypeState) {
        var acElements = AcmUtils.extractAcElementsFromServiceTemplate(acmDefinition.getServiceTemplate(),
                acRuntimeParameterGroup.getAcmParameters().getToscaElementName());
        Map<ToscaConceptIdentifier, UUID> supportedElementMap = new HashMap<>();
        var participantIds = new HashSet<UUID>();
        if (AcTypeState.COMMISSIONED.equals(acmDefinition.getState())) {
            // scenario Prime, participants not assigned yet
            supportedElementMap = participantProvider.getSupportedElementMap();
            for (var elementEntry : acElements) {
                var elementState = acmDefinition.getElementStateMap().get(elementEntry.getKey());
                elementState.setState(acTypeState);
                var participantId = supportedElementMap.get(AcmUtils.getType(elementEntry.getValue()));
                if (participantId != null) {
                    elementState.setParticipantId(participantId);
                    participantIds.add(participantId);
                }
            }
        } else {
            // scenario Prime again/Deprime, participants already assigned
            for (var elementEntry : acElements) {
                var elementState = acmDefinition.getElementStateMap().get(elementEntry.getKey());
                elementState.setState(acTypeState);
                participantIds.add(elementState.getParticipantId());
                supportedElementMap.put(AcmUtils.getType(elementEntry.getValue()), elementState.getParticipantId());
            }
        }
        acmDefinition.setStateChangeResult(StateChangeResult.NO_ERROR);
        acmDefinition.setState(acTypeState);
        acmDefinition.setRevisionId(UUID.randomUUID());
        acmDefinition.setLastMsg(TimestampHelper.now());
        participantProvider.verifyParticipantState(participantIds);
        return AcmUtils.prepareParticipantPriming(acElements, supportedElementMap, acmDefinition);
    }

    /**
     * Send ParticipantPrime to Participant after that commissioning has been removed.
     */
    @Timed(value = "publisher.participant_update", description = "PARTICIPANT_UPDATE messages published")
    public void sendDepriming(Map<UUID, List<AutomationCompositionElementDefinition>> elementMap, UUID compositionId,
                              UUID revisionId) {
        var message = new ParticipantPrime();
        message.setCompositionId(compositionId);
        message.setTimestamp(Instant.now());
        message.setRevisionIdComposition(revisionId);
        message.setPrimeOrder(PrimeOrder.DEPRIME);
        var primeDtoList = AcmUtils.preparePrimeDtoList(elementMap, compositionId);
        message.setPrimeDtoList(primeDtoList);
        message.setParticipantIdList(primeDtoList.stream()
                .map(ParticipantPrimeDto::getParticipantId).collect(Collectors.toSet()));

        participantPublisher.send(message);
    }
}
