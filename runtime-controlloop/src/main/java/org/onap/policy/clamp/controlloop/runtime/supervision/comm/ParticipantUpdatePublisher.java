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
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantUpdate;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
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
    private static final String CONTROL_LOOP_ELEMENT = "org.onap.policy.clamp.controlloop.ControlLoopElement";
    private static final Coder CODER = new StandardCoder();
    private final PolicyModelsProvider modelsProvider;

    /**
     * Send ParticipantUpdate to Participant.
     *
     * @param participantId the participant Id
     * @param participantType the participant Type
     */
    public void send(ToscaConceptIdentifier participantId, ToscaConceptIdentifier participantType,
            boolean commissionFlag) {
        var message = new ParticipantUpdate();
        message.setParticipantId(participantId);
        message.setParticipantType(participantType);
        message.setTimestamp(Instant.now());

        ToscaServiceTemplate toscaServiceTemplate;
        try {
            toscaServiceTemplate = modelsProvider.getServiceTemplateList(null, null).get(0);
        } catch (PfModelException pfme) {
            LOGGER.warn("Get of tosca service template failed, cannot send participantupdate", pfme);
            return;
        }

        List<ParticipantDefinition> participantDefinitionUpdates = new ArrayList<>();
        for (Map.Entry<String, ToscaNodeTemplate> toscaInputEntry : toscaServiceTemplate.getToscaTopologyTemplate()
                .getNodeTemplates().entrySet()) {
            if (toscaInputEntry.getValue().getType().contains(CONTROL_LOOP_ELEMENT)) {
                ToscaConceptIdentifier clParticipantType;
                try {
                    clParticipantType =
                            CODER.decode(toscaInputEntry.getValue().getProperties().get("participantType").toString(),
                                    ToscaConceptIdentifier.class);
                } catch (CoderException e) {
                    throw new RuntimeException("cannot get ParticipantType from toscaNodeTemplate", e);
                }
                prepareParticipantDefinitionUpdate(clParticipantType, toscaInputEntry.getKey(),
                        toscaInputEntry.getValue(), participantDefinitionUpdates);
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
            ToscaNodeTemplate entryValue, List<ParticipantDefinition> participantDefinitionUpdates) {

        var clDefinition = new ControlLoopElementDefinition();
        clDefinition.setClElementDefinitionId(new ToscaConceptIdentifier(entryKey, entryValue.getVersion()));
        clDefinition.setControlLoopElementToscaNodeTemplate(entryValue);
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
