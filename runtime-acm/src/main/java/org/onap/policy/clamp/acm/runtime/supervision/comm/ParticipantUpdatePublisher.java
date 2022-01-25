/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 Nordix Foundation.
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUtils;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantUpdate;
import org.onap.policy.clamp.models.acm.persistence.provider.ServiceTemplateProvider;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
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

    private final ServiceTemplateProvider serviceTemplateProvider;

    /**
     * Send ParticipantUpdate to all Participants.
     *
     * @param name the ToscaServiceTemplate name
     * @param version the ToscaServiceTemplate version
     */
    public void sendComissioningBroadcast(String name, String version) {
        sendCommissioning(name, version, null, null);
    }

    /**
     * Send ParticipantUpdate to Participant
     * if participantType and participantId are null then message is broadcast.
     *
     * @param name the ToscaServiceTemplate name
     * @param version the ToscaServiceTemplate version
     * @param participantType the ParticipantType
     * @param participantId the ParticipantId
     */
    public boolean sendCommissioning(String name, String version, ToscaConceptIdentifier participantType,
            ToscaConceptIdentifier participantId) {
        var message = new ParticipantUpdate();
        message.setParticipantType(participantType);
        message.setParticipantId(participantId);
        message.setTimestamp(Instant.now());

        ToscaServiceTemplate toscaServiceTemplate = null;
        Map<String, ToscaNodeType> commonPropertiesMap = null;
        try {
            var list = serviceTemplateProvider.getServiceTemplateList(name, version);
            if (!list.isEmpty()) {
                toscaServiceTemplate = list.get(0);
                commonPropertiesMap =
                        serviceTemplateProvider.getCommonOrInstancePropertiesFromNodeTypes(true, toscaServiceTemplate);
            } else {
                LOGGER.warn("No tosca service template found, cannot send participantupdate {} {}", name, version);
                return false;
            }
        } catch (PfModelException pfme) {
            LOGGER.warn("Get of tosca service template failed, cannot send participantupdate", pfme);
            return false;
        }

        List<ParticipantDefinition> participantDefinitionUpdates = new ArrayList<>();
        for (var toscaInputEntry : toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates().entrySet()) {
            if (ParticipantUtils.checkIfNodeTemplateIsAutomationCompositionElement(toscaInputEntry.getValue(),
                    toscaServiceTemplate)) {
                AcmUtils.prepareParticipantDefinitionUpdate(
                    ParticipantUtils.findParticipantType(toscaInputEntry.getValue().getProperties()),
                    toscaInputEntry.getKey(), toscaInputEntry.getValue(),
                    participantDefinitionUpdates, commonPropertiesMap);
            }
        }

        // Commission the automation composition but sending participantdefinitions to participants
        message.setParticipantDefinitionUpdates(participantDefinitionUpdates);
        LOGGER.debug("Participant Update sent {}", message);
        super.send(message);
        return true;
    }

    /**
     * Send ParticipantUpdate to Participant after that commissioning has been removed.
     */
    public void sendDecomisioning() {
        var message = new ParticipantUpdate();
        message.setTimestamp(Instant.now());
        // DeCommission the automation composition but deleting participantdefinitions on participants
        message.setParticipantDefinitionUpdates(null);

        LOGGER.debug("Participant Update sent {}", message);
        super.send(message);
    }
}
