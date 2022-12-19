/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021,2022 Nordix Foundation.
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUtils;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantUpdate;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
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

    private final AcDefinitionProvider acDefinitionProvider;

    /**
     * Send ParticipantUpdate to all Participants.
     *
     * @param acmDefinition the AutomationComposition Definition
     */
    @Timed(value = "publisher.participant_update", description = "PARTICIPANT_UPDATE messages published")
    public void sendComissioningBroadcast(AutomationCompositionDefinition acmDefinition) {
        sendCommissioning(acmDefinition, null, null);
    }

    /**
     * Send ParticipantUpdate to Participant
     * if participantType and participantId are null then message is broadcast.
     *
     * @param participantType the ParticipantType
     * @param participantId the ParticipantId
     */
    @Timed(value = "publisher.participant_update", description = "PARTICIPANT_UPDATE messages published")
    public void sendCommissioning(ToscaConceptIdentifier participantType, ToscaConceptIdentifier participantId) {
        var list = acDefinitionProvider.getAllAcDefinitions();
        if (list.isEmpty()) {
            LOGGER.warn("No tosca service template found, cannot send participantupdate");
        }
        for (var acmDefinition : list) {
            sendCommissioning(acmDefinition, participantType, participantId);
        }
    }

    /**
     * Send ParticipantUpdate to Participant
     * if participantType and participantId are null then message is broadcast.
     *
     * @param acmDefinition the AutomationComposition Definition
     * @param participantType the ParticipantType
     * @param participantId the ParticipantId
     */
    @Timed(value = "publisher.participant_update", description = "PARTICIPANT_UPDATE messages published")
    public void sendCommissioning(AutomationCompositionDefinition acmDefinition,
            ToscaConceptIdentifier participantType, ToscaConceptIdentifier participantId) {
        var message = new ParticipantUpdate();
        message.setCompositionId(acmDefinition.getCompositionId());
        message.setParticipantType(participantType);
        message.setParticipantId(participantId);
        message.setTimestamp(Instant.now());

        var toscaServiceTemplate = acmDefinition.getServiceTemplate();
        List<ParticipantDefinition> participantDefinitionUpdates = new ArrayList<>();
        for (var toscaInputEntry : toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates().entrySet()) {
            if (ParticipantUtils.checkIfNodeTemplateIsAutomationCompositionElement(toscaInputEntry.getValue(),
                    toscaServiceTemplate)) {
                AcmUtils.prepareParticipantDefinitionUpdate(
                        ParticipantUtils.findParticipantType(toscaInputEntry.getValue().getProperties()),
                        toscaInputEntry.getKey(), toscaInputEntry.getValue(), participantDefinitionUpdates);
            }
        }

        // Commission the automation composition but sending participantdefinitions to participants
        message.setParticipantDefinitionUpdates(participantDefinitionUpdates);
        LOGGER.debug("Participant Update sent {}", message);
        super.send(message);
    }

    /**
     * Send ParticipantUpdate to Participant after that commissioning has been removed.
     */
    @Timed(value = "publisher.participant_update", description = "PARTICIPANT_UPDATE messages published")
    public void sendDecomisioning(UUID compositionId) {
        var message = new ParticipantUpdate();
        message.setCompositionId(compositionId);
        message.setTimestamp(Instant.now());
        // DeCommission the automation composition but deleting participantdefinitions on participants
        message.setParticipantDefinitionUpdates(null);

        LOGGER.debug("Participant Update sent {}", message);
        super.send(message);
    }
}
