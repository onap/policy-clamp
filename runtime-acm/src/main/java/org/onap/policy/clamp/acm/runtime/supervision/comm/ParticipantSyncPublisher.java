/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation.
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
import java.util.Optional;
import java.util.UUID;
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup;
import org.onap.policy.clamp.acm.runtime.main.parameters.Topics;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantRestartAc;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantSync;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
public class ParticipantSyncPublisher extends ParticipantRestartPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantSyncPublisher.class);

    private final AcRuntimeParameterGroup acRuntimeParameterGroup;

    public ParticipantSyncPublisher(AcRuntimeParameterGroup acRuntimeParameterGroup) {
        super(acRuntimeParameterGroup);
        this.acRuntimeParameterGroup = acRuntimeParameterGroup;
    }


    /**
     * Send sync msg to Participant.
     *
     * @param participantId the ParticipantId
     * @param acmDefinition the AutomationComposition Definition
     * @param automationCompositions the list of automationCompositions
     */
    @Override
    @Timed(value = "publisher.participant_sync_msg", description = "Participant Sync published")
    public void send(UUID participantId, AutomationCompositionDefinition acmDefinition,
                     List<AutomationComposition> automationCompositions) {

        var message = new ParticipantSync();
        message.setParticipantId(participantId);
        message.setCompositionId(acmDefinition.getCompositionId());
        message.setMessageId(UUID.randomUUID());
        message.setTimestamp(Instant.now());
        message.setState(acmDefinition.getState());
        message.setParticipantDefinitionUpdates(prepareParticipantRestarting(participantId, acmDefinition));
        var toscaServiceTemplateFragment = AcmUtils.getToscaServiceTemplateFragment(acmDefinition.getServiceTemplate());

        for (var automationComposition : automationCompositions) {
            var syncAc = new ParticipantRestartAc();
            syncAc.setAutomationCompositionId(automationComposition.getInstanceId());
            for (var element : automationComposition.getElements().values()) {
                if (participantId.equals(element.getParticipantId())) {
                    var acElementSync = AcmUtils.createAcElementRestart(element);
                    acElementSync.setToscaServiceTemplateFragment(toscaServiceTemplateFragment);
                    syncAc.getAcElementList().add(acElementSync);
                }
            }
            message.getAutomationcompositionList().add(syncAc);
        }

        LOGGER.debug("Participant Sync sent {}", message);
        super.send(message);
    }

    /**
     * Is default topic.
     * @return true if default
     */
    @Override
    public boolean isDefaultTopic() {
        return false;
    }

}
