/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 Nordix Foundation.
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
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElementDefinition;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantUpdate;
import org.onap.policy.clamp.controlloop.runtime.commissioning.CommissioningProvider;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.base.PfModelException;
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
    private final CommissioningProvider commissioningProvider;
    private static final Coder CODER = new StandardCoder();

    /**
     * Send ParticipantUpdate to Participant.
     *
     * @param participantId the participant Id
     * @param participantType the participant Type
     */
    public void send(ToscaConceptIdentifier participantId, ToscaConceptIdentifier participantType) {
        var message = new ParticipantUpdate();
        message.setParticipantId(participantId);
        message.setParticipantType(participantType);
        message.setTimestamp(Instant.now());

        ToscaServiceTemplate toscaServiceTemplate;
        try {
            toscaServiceTemplate = commissioningProvider.getToscaServiceTemplate(null, null);
        } catch (PfModelException pfme) {
            LOGGER.warn("Get of tosca service template failed, cannot send participantupdate", pfme);
            return;
        }

        Map<ToscaConceptIdentifier, Map<ToscaConceptIdentifier, ControlLoopElementDefinition>>
            participantDefinitionUpdateMap = new LinkedHashMap<>();
        for (Map.Entry<String, ToscaNodeTemplate> toscaInputEntry :
            toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates().entrySet()) {
            if (toscaInputEntry.getValue().getType().contains(CONTROL_LOOP_ELEMENT)) {
                Map<ToscaConceptIdentifier, ControlLoopElementDefinition> controlLoopElementDefinitionMap =
                    new LinkedHashMap<>();
                ToscaConceptIdentifier clParticipantId;
                try {
                    clParticipantId = CODER.decode(
                            toscaInputEntry.getValue().getProperties().get("participant_id").toString(),
                            ToscaConceptIdentifier.class);
                } catch (CoderException e) {
                    throw new RuntimeException("cannot get ParticipantId from toscaNodeTemplate", e);
                }

                var clDefinition = new ControlLoopElementDefinition();
                clDefinition.setControlLoopElementToscaNodeTemplate(toscaInputEntry.getValue());

                if (!participantDefinitionUpdateMap.containsKey(clParticipantId)) {
                    controlLoopElementDefinitionMap.put(
                        new ToscaConceptIdentifier(toscaInputEntry.getKey(), toscaInputEntry.getValue().getVersion()),
                        clDefinition);
                    participantDefinitionUpdateMap.put(clParticipantId, controlLoopElementDefinitionMap);
                } else {
                    controlLoopElementDefinitionMap = participantDefinitionUpdateMap.get(clParticipantId);
                    controlLoopElementDefinitionMap.put(
                        new ToscaConceptIdentifier(toscaInputEntry.getKey(), toscaInputEntry.getValue().getVersion()),
                        clDefinition);
                }
            }
        }

        message.setParticipantDefinitionUpdateMap(participantDefinitionUpdateMap);
        message.setToscaServiceTemplate(toscaServiceTemplate);
        LOGGER.debug("Participant Update sent {}", message);
        super.send(message);
    }
}
