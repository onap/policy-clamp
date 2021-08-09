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
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElementDefinition;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantUpdate;
import org.onap.policy.clamp.controlloop.runtime.commissioning.CommissioningProvider;
import org.onap.policy.models.base.PfModelException;
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

    private final CommissioningProvider commissioningProvider;

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

        var clDefinition = new ControlLoopElementDefinition();
        clDefinition.setId(UUID.randomUUID());

        try {
            clDefinition.setControlLoopElementToscaServiceTemplate(
                    commissioningProvider.getToscaServiceTemplate(null, null));
        } catch (PfModelException pfme) {
            LOGGER.warn("Get of tosca service template failed, cannot send participantupdate", pfme);
            return;
        }

        Map<UUID, ControlLoopElementDefinition> controlLoopElementDefinitionMap = new LinkedHashMap<>();
        controlLoopElementDefinitionMap.put(UUID.randomUUID(), clDefinition);

        Map<String, Map<UUID, ControlLoopElementDefinition>> participantDefinitionUpdateMap = new LinkedHashMap<>();
        participantDefinitionUpdateMap.put(participantId.toString(), controlLoopElementDefinitionMap);
        message.setParticipantDefinitionUpdateMap(participantDefinitionUpdateMap);

        LOGGER.debug("Participant Update sent {}", message);
        super.send(message);
    }
}
