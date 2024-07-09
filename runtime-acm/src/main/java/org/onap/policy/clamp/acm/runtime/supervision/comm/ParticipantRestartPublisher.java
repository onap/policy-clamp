/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation.
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
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantRestart;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ParticipantRestartPublisher extends AbstractParticipantPublisher<ParticipantRestart> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantRestartPublisher.class);
    private final AcRuntimeParameterGroup acRuntimeParameterGroup;

    /**
     * Send Restart to Participant.
     *
     * @param participantId the ParticipantId
     * @param acmDefinition the AutomationComposition Definition
     * @param automationCompositions the list of automationCompositions
     */
    @Timed(value = "publisher.participant_restart", description = "Participant Restart published")
    public void send(UUID participantId, AutomationCompositionDefinition acmDefinition,
            List<AutomationComposition> automationCompositions) {

        var message = new ParticipantRestart();
        message.setParticipantId(participantId);
        message.setCompositionId(acmDefinition.getCompositionId());
        message.setMessageId(UUID.randomUUID());
        message.setTimestamp(Instant.now());
        message.setState(acmDefinition.getState());
        message.setParticipantDefinitionUpdates(
                AcmUtils.prepareParticipantRestarting(participantId, acmDefinition,
                        acRuntimeParameterGroup.getAcmParameters().getToscaElementName()));
        var toscaServiceTemplateFragment = AcmUtils.getToscaServiceTemplateFragment(acmDefinition.getServiceTemplate());

        for (var automationComposition : automationCompositions) {
            var restartAc = AcmUtils
                    .createAcRestart(automationComposition, participantId, toscaServiceTemplateFragment);
            message.getAutomationcompositionList().add(restartAc);
        }

        LOGGER.debug("Participant Restart sent {}", message.getMessageId());
        super.send(message);
    }
}
