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
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ControlLoopUpdate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class is used to send ControlLoopUpdate messages to participants on DMaaP.
 */
@Component
@AllArgsConstructor
public class ControlLoopUpdatePublisher extends AbstractParticipantPublisher<ControlLoopUpdate> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControlLoopUpdatePublisher.class);

    /**
     * Send ControlLoopUpdate to Participant.
     *
     * @param controlLoop the ControlLoop
     */
    public void send(ControlLoop controlLoop) {
        var controlLoopUpdateMsg = new ControlLoopUpdate();
        controlLoopUpdateMsg.setControlLoopId(controlLoop.getKey().asIdentifier());
        controlLoopUpdateMsg.setMessageId(UUID.randomUUID());
        controlLoopUpdateMsg.setTimestamp(Instant.now());

        Map<ToscaConceptIdentifier, Map<ToscaConceptIdentifier, ControlLoopElement>> participantUpdateMap =
                new LinkedHashMap<>();
        for (ControlLoopElement element : controlLoop.getElements().values()) {
            Map<ToscaConceptIdentifier, ControlLoopElement> clElementMap = new LinkedHashMap<>();

            if (!participantUpdateMap.containsKey(element.getParticipantId())) {
                clElementMap.put(element.getDefinition(), element);
                participantUpdateMap.put(element.getParticipantId(), clElementMap);
            } else {
                clElementMap = participantUpdateMap.get(element.getParticipantId());
                clElementMap.put(element.getDefinition(), element);
            }
        }
        controlLoopUpdateMsg.setParticipantUpdateMap(participantUpdateMap);

        LOGGER.debug("ControlLoopUpdate message sent", controlLoopUpdateMsg);
        super.send(controlLoopUpdateMsg);
    }
}
