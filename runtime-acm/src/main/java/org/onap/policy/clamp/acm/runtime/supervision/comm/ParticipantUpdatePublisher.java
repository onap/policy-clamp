/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2023 Nordix Foundation.
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDefinition;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantUpdate;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class is used to send ParticipantUpdate messages to participants on DMaaP.
 */
@Component
@AllArgsConstructor
public class ParticipantUpdatePublisher extends AbstractParticipantPublisher<ParticipantUpdate> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantUpdatePublisher.class);

    private final ParticipantProvider participantProvider;

    /**
     * Send ParticipantUpdate to Participant
     * if participantId is null then message is broadcast.
     *
     * @param participantDefinitions the list of ParticipantDefinition to send
     * @param compositionId the compositionId
     * @param participantId the ParticipantId
     */
    @Timed(value = "publisher.participant_update", description = "PARTICIPANT_UPDATE messages published")
    public void sendPriming(List<ParticipantDefinition> participantDefinitions, UUID compositionId,
            UUID participantId) {
        var message = new ParticipantUpdate();
        message.setCompositionId(compositionId);
        message.setParticipantId(participantId);
        message.setTimestamp(Instant.now());
        message.setParticipantDefinitionUpdates(participantDefinitions);
        LOGGER.debug("Participant Update sent {}", message);
        super.send(message);
    }

    /**
     * Pepare the Priming message creating the list of ParticipantDefinition to send
     * and fill the ElementState map of the AC Definition.
     *
     * @param acmDefinition the AutomationComposition Definition
     * @return list of ParticipantDefinition
     */
    public List<ParticipantDefinition> prepareParticipantPriming(AutomationCompositionDefinition acmDefinition) {
        acmDefinition.setState(AcTypeState.PRIMING);
        var acElements = AcmUtils.extractAcElementsFromServiceTemplate(acmDefinition.getServiceTemplate());
        Map<ToscaConceptIdentifier, UUID> supportedElementMap = new HashMap<>();
        if (AcTypeState.PRIMED.equals(acmDefinition.getState())) {
            // scenario Prime again, participants already assigned
            for (var elementEntry : acElements) {
                var elementState = acmDefinition.getElementStateMap().get(elementEntry.getKey());
                elementState.setState(AcTypeState.PRIMING);
                var type = new ToscaConceptIdentifier(elementEntry.getValue().getType(),
                        elementEntry.getValue().getTypeVersion());
                supportedElementMap.put(type, elementState.getParticipantId());
            }
        } else {
            // scenario Prime participants not assigned yet
            supportedElementMap = participantProvider.getSupportedElementMap();
            for (var elementEntry : acElements) {
                var elementState = acmDefinition.getElementStateMap().get(elementEntry.getKey());
                elementState.setState(AcTypeState.PRIMING);
                var type = new ToscaConceptIdentifier(elementEntry.getValue().getType(),
                        elementEntry.getValue().getTypeVersion());
                elementState.setParticipantId(supportedElementMap.get(type));
            }
        }
        return AcmUtils.prepareParticipantPriming(acElements, supportedElementMap);
    }

    /**
     * Send ParticipantUpdate to Participant after that commissioning has been removed.
     */
    @Timed(value = "publisher.participant_update", description = "PARTICIPANT_UPDATE messages published")
    public void sendDepriming(UUID compositionId) {
        var message = new ParticipantUpdate();
        message.setCompositionId(compositionId);
        message.setTimestamp(Instant.now());
        // DeCommission the automation composition but deleting participantdefinitions on participants
        message.setParticipantDefinitionUpdates(null);

        LOGGER.debug("Participant Update sent {}", message);
        super.send(message);
    }
}
