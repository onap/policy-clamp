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

package org.onap.policy.clamp.controlloop.runtime.supervision.comm;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElementDefinition;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantDefinition;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantUtils;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantUpdate;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
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

    private final PolicyModelsProvider modelsProvider;

    /**
     * Send ParticipantUpdate to Participant.
     *
     */
    public void send(Map<String, ToscaNodeType> commonPropertiesMap, boolean commissionFlag) {
        var message = new ParticipantUpdate();
        message.setTimestamp(Instant.now());

        ToscaServiceTemplate toscaServiceTemplate = null;
        try {
            var list = modelsProvider.getServiceTemplateList(null, null);
            if (!list.isEmpty()) {
                toscaServiceTemplate = list.get(0);
            }
        } catch (PfModelException pfme) {
            LOGGER.warn("Get of tosca service template failed, cannot send participantupdate", pfme);
            return;
        }

        List<ParticipantDefinition> participantDefinitionUpdates = new ArrayList<>();
        if (toscaServiceTemplate != null) {
            for (var toscaInputEntry : toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates().entrySet()) {
                if (ParticipantUtils.checkIfNodeTemplateIsControlLoopElement(toscaInputEntry.getValue(),
                        toscaServiceTemplate)) {
                    var clParticipantType =
                            ParticipantUtils.findParticipantType(toscaInputEntry.getValue().getProperties());
                    prepareParticipantDefinitionUpdate(clParticipantType, toscaInputEntry.getKey(),
                            toscaInputEntry.getValue(), participantDefinitionUpdates, commonPropertiesMap);
                }
            }
        }

        if (commissionFlag) {
            // Commission the controlloop but sending participantdefinitions to participants
            message.setParticipantDefinitionUpdates(participantDefinitionUpdates);
        } else {
            // DeCommission the controlloop but deleting participantdefinitions on participants
            message.setParticipantDefinitionUpdates(null);
        }
        LOGGER.debug("Participant Update sent {}", message);
        super.send(message);
    }

    private void prepareParticipantDefinitionUpdate(ToscaConceptIdentifier clParticipantType, String entryKey,
            ToscaNodeTemplate entryValue, List<ParticipantDefinition> participantDefinitionUpdates,
            Map<String, ToscaNodeType> commonPropertiesMap) {

        var clDefinition = new ControlLoopElementDefinition();
        clDefinition.setClElementDefinitionId(new ToscaConceptIdentifier(entryKey, entryValue.getVersion()));
        clDefinition.setControlLoopElementToscaNodeTemplate(entryValue);
        ToscaNodeType nodeType = commonPropertiesMap.get(entryValue.getType());
        if (nodeType != null) {
            clDefinition.setCommonPropertiesMap(nodeType.getProperties());
        }

        List<ControlLoopElementDefinition> controlLoopElementDefinitionList = new ArrayList<>();

        if (participantDefinitionUpdates.isEmpty()) {
            participantDefinitionUpdates
                    .add(getParticipantDefinition(clDefinition, clParticipantType, controlLoopElementDefinitionList));
        } else {
            var participantExists = false;
            for (ParticipantDefinition participantDefinitionUpdate : participantDefinitionUpdates) {
                if (participantDefinitionUpdate.getParticipantType().equals(clParticipantType)) {
                    participantDefinitionUpdate.getControlLoopElementDefinitionList().add(clDefinition);
                    participantExists = true;
                }
            }
            if (!participantExists) {
                participantDefinitionUpdates.add(
                        getParticipantDefinition(clDefinition, clParticipantType, controlLoopElementDefinitionList));
            }
        }
    }

    private ParticipantDefinition getParticipantDefinition(ControlLoopElementDefinition clDefinition,
            ToscaConceptIdentifier clParticipantType,
            List<ControlLoopElementDefinition> controlLoopElementDefinitionList) {
        var participantDefinition = new ParticipantDefinition();
        participantDefinition.setParticipantType(clParticipantType);
        controlLoopElementDefinitionList.add(clDefinition);
        participantDefinition.setControlLoopElementDefinitionList(controlLoopElementDefinitionList);
        return participantDefinition;
    }
}
