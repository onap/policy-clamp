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
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUpdates;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionUpdate;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class is used to send AutomationCompositionUpdate messages to participants on DMaaP.
 */
@Component
@AllArgsConstructor
public class AutomationCompositionUpdatePublisher extends AbstractParticipantPublisher<AutomationCompositionUpdate> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutomationCompositionUpdatePublisher.class);
    private final AcDefinitionProvider acDefinitionProvider;

    /**
     * Send AutomationCompositionUpdate to Participant.
     *
     * @param automationComposition the AutomationComposition
     */
    @Timed(value = "publisher.automation_composition_update",
            description = "AUTOMATION_COMPOSITION_UPDATE messages published")
    public void send(AutomationComposition automationComposition) {
        send(automationComposition, 0);
    }

    /**
     * Send AutomationCompositionUpdate to Participant.
     *
     * @param automationComposition the AutomationComposition
     * @param startPhase the Start Phase
     */
    @Timed(value = "publisher.automation_composition_update",
            description = "AUTOMATION_COMPOSITION_UPDATE messages published")
    public void send(AutomationComposition automationComposition, int startPhase) {
        var automationCompositionUpdateMsg = new AutomationCompositionUpdate();
        automationCompositionUpdateMsg.setCompositionId(automationComposition.getCompositionId());
        automationCompositionUpdateMsg.setStartPhase(startPhase);
        automationCompositionUpdateMsg.setAutomationCompositionId(automationComposition.getInstanceId());
        automationCompositionUpdateMsg.setMessageId(UUID.randomUUID());
        automationCompositionUpdateMsg.setTimestamp(Instant.now());
        var toscaServiceTemplate = acDefinitionProvider.getAcDefinition(automationComposition.getCompositionId());

        List<ParticipantUpdates> participantUpdates = new ArrayList<>();
        for (var element : automationComposition.getElements().values()) {
            AcmUtils.setAcPolicyInfo(element, toscaServiceTemplate);
            AcmUtils.prepareParticipantUpdate(element, participantUpdates);
        }
        automationCompositionUpdateMsg.setParticipantUpdatesList(participantUpdates);

        LOGGER.debug("AutomationCompositionUpdate message sent {}", automationCompositionUpdateMsg);
        super.send(automationCompositionUpdateMsg);
    }
}
