/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDefinition;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrime;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.clamp.models.acm.utils.TimestampHelper;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class is used to send ParticipantPrime messages to participants on Kafka.
 */
@Component
@AllArgsConstructor
public class ParticipantPrimePublisher extends AbstractParticipantPublisher<ParticipantPrime> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantPrimePublisher.class);

    private final ParticipantProvider participantProvider;
    private final AcRuntimeParameterGroup acRuntimeParameterGroup;

    /**
     * Send ParticipantPrime to Participant
     * if participantId is null then message is broadcast.
     *
     * @param participantDefinitions the list of ParticipantDefinition to send
     * @param compositionId the compositionId
     * @param revisionId last update
     */
    @Timed(value = "publisher.participant_update", description = "PARTICIPANT_UPDATE messages published")
    public void sendPriming(List<ParticipantDefinition> participantDefinitions, UUID compositionId,
            UUID revisionId) {
        var message = new ParticipantPrime();
        message.setCompositionId(compositionId);
        message.setParticipantIdList(participantDefinitions.stream()
                .map(ParticipantDefinition::getParticipantId).collect(Collectors.toSet()));
        message.setTimestamp(Instant.now());
        message.setRevisionIdComposition(revisionId);
        message.setParticipantDefinitionUpdates(participantDefinitions);
        LOGGER.debug("Participant Update sent {}", message.getMessageId());
        super.send(message);
    }

    /**
     * Prepare the Priming message creating the list of ParticipantDefinition to send
     * and fill the ElementState map of the AC Definition.
     *
     * @param acmDefinition the AutomationComposition Definition
     * @return list of ParticipantDefinition
     */
    public List<ParticipantDefinition> prepareParticipantPriming(AutomationCompositionDefinition acmDefinition) {
        acmDefinition.setStateChangeResult(StateChangeResult.NO_ERROR);
        acmDefinition.setState(AcTypeState.PRIMING);
        acmDefinition.setRevisionId(UUID.randomUUID());
        acmDefinition.setLastMsg(TimestampHelper.now());
        var acElements = AcmUtils.extractAcElementsFromServiceTemplate(acmDefinition.getServiceTemplate(),
                acRuntimeParameterGroup.getAcmParameters().getToscaElementName());
        Map<ToscaConceptIdentifier, UUID> supportedElementMap = new HashMap<>();
        var participantIds = new HashSet<UUID>();
        if (AcTypeState.PRIMED.equals(acmDefinition.getState())) {
            // scenario Prime again, participants already assigned
            for (var elementEntry : acElements) {
                var elementState = acmDefinition.getElementStateMap().get(elementEntry.getKey());
                elementState.setState(AcTypeState.PRIMING);
                participantIds.add(elementState.getParticipantId());
                supportedElementMap.put(AcmUtils.getType(elementEntry.getValue()), elementState.getParticipantId());
            }
        } else {
            // scenario Prime participants not assigned yet
            supportedElementMap = participantProvider.getSupportedElementMap();
            for (var elementEntry : acElements) {
                var elementState = acmDefinition.getElementStateMap().get(elementEntry.getKey());
                elementState.setState(AcTypeState.PRIMING);
                var participantId = supportedElementMap.get(AcmUtils.getType(elementEntry.getValue()));
                if (participantId != null) {
                    elementState.setParticipantId(participantId);
                    participantIds.add(participantId);
                }
            }
        }
        participantProvider.verifyParticipantState(participantIds);
        return AcmUtils.prepareParticipantPriming(acElements, supportedElementMap);
    }

    /**
     * Send ParticipantPrime to Participant after that commissioning has been removed.
     */
    @Timed(value = "publisher.participant_update", description = "PARTICIPANT_UPDATE messages published")
    public void sendDepriming(UUID compositionId, Set<UUID> participantIds, UUID revisionId) {
        var message = new ParticipantPrime();
        message.setCompositionId(compositionId);
        message.setParticipantIdList(participantIds);
        message.setTimestamp(Instant.now());
        message.setRevisionIdComposition(revisionId);
        // DeCommission the automation composition but deleting participantdefinitions on participants
        message.setParticipantDefinitionUpdates(null);

        LOGGER.debug("Participant Update sent {}", message.getMessageId());
        super.send(message);
    }
}
