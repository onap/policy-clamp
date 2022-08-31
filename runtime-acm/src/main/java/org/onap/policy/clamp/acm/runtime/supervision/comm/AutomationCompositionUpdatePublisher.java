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
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUpdates;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionUpdate;
import org.onap.policy.clamp.models.acm.persistence.provider.ServiceTemplateProvider;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
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
    private final ServiceTemplateProvider serviceTemplateProvider;

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
        automationCompositionUpdateMsg.setStartPhase(startPhase);
        automationCompositionUpdateMsg.setAutomationCompositionId(automationComposition.getKey().asIdentifier());
        automationCompositionUpdateMsg.setMessageId(UUID.randomUUID());
        automationCompositionUpdateMsg.setTimestamp(Instant.now());
        ToscaServiceTemplate toscaServiceTemplate;
        try {
            toscaServiceTemplate = serviceTemplateProvider.getAllServiceTemplates().get(0);
        } catch (PfModelException pfme) {
            LOGGER.warn("Get of tosca service template failed, cannot send participantupdate", pfme);
            return;
        }

        List<ParticipantUpdates> participantUpdates = new ArrayList<>();
        for (AutomationCompositionElement element : automationComposition.getElements().values()) {
            AcmUtils.setServiceTemplatePolicyInfo(element, toscaServiceTemplate);
            AcmUtils.prepareParticipantUpdate(element, participantUpdates);
        }
        automationCompositionUpdateMsg.setParticipantUpdatesList(participantUpdates);

        LOGGER.debug("AutomationCompositionUpdate message sent {}", automationCompositionUpdateMsg);
        super.send(automationCompositionUpdateMsg);
    }
}
