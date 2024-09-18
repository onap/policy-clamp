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
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantRestartAc;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantSync;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ParticipantSyncPublisher extends AbstractParticipantPublisher<ParticipantSync> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantSyncPublisher.class);
    private final AcRuntimeParameterGroup acRuntimeParameterGroup;

    /**
     * Send Restart sync msg to Participant by participantId.
     *
     * @param participantId the participantId
     * @param replicaId the replicaId
     * @param acmDefinition the AutomationComposition Definition
     * @param automationCompositions the list of automationCompositions
     */
    @Timed(value = "publisher.participant_sync_msg", description = "Participant Sync published")
    public void sendRestartMsg(UUID participantId, UUID replicaId, AutomationCompositionDefinition acmDefinition,
                     List<AutomationComposition> automationCompositions) {

        var message = new ParticipantSync();
        message.setParticipantId(participantId);
        message.setReplicaId(replicaId);
        message.setRestarting(true);
        message.setCompositionId(acmDefinition.getCompositionId());
        message.setMessageId(UUID.randomUUID());
        message.setTimestamp(Instant.now());
        message.setState(acmDefinition.getState());
        message.setParticipantDefinitionUpdates(AcmUtils.prepareParticipantRestarting(participantId, acmDefinition,
                acRuntimeParameterGroup.getAcmParameters().getToscaElementName()));

        for (var automationComposition : automationCompositions) {
            var syncAc = AcmUtils.createAcRestart(automationComposition, participantId);
            message.getAutomationcompositionList().add(syncAc);
        }

        LOGGER.debug("Participant Restarting Sync sent {}", message);
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

    /**
     * Send AutomationCompositionDefinition sync msg to all Participants.
     *
     * @param acDefinition the AutomationComposition Definition
     * @param excludeReplicaId the replica to be excluded
     */
    @Timed(value = "publisher.participant_sync_msg", description = "Participant Sync published")
    public void sendSync(AutomationCompositionDefinition acDefinition, UUID excludeReplicaId) {
        var message = new ParticipantSync();
        message.setCompositionId(acDefinition.getCompositionId());
        if (excludeReplicaId != null) {
            message.getExcludeReplicas().add(excludeReplicaId);
        }
        message.setState(acDefinition.getState());
        message.setStateChangeResult(acDefinition.getStateChangeResult());
        message.setMessageId(UUID.randomUUID());
        message.setTimestamp(Instant.now());
        if (AcTypeState.COMMISSIONED.equals(acDefinition.getState())) {
            message.setDelete(true);
        } else {
            message.setParticipantDefinitionUpdates(AcmUtils.prepareParticipantRestarting(null, acDefinition,
                    acRuntimeParameterGroup.getAcmParameters().getToscaElementName()));
        }
        LOGGER.debug("Participant AutomationCompositionDefinition Sync sent {}", message);
        super.send(message);
    }

    /**
     * Send AutomationComposition sync msg to all Participants.
     *
     * @param automationComposition the automationComposition
     */
    @Timed(value = "publisher.participant_sync_msg", description = "Participant Sync published")
    public void sendSync(AutomationComposition automationComposition) {
        var message = new ParticipantSync();
        message.setCompositionId(automationComposition.getCompositionId());
        message.setAutomationCompositionId(automationComposition.getInstanceId());
        message.setState(AcTypeState.PRIMED);
        message.setMessageId(UUID.randomUUID());
        message.setTimestamp(Instant.now());
        var syncAc = new ParticipantRestartAc();
        syncAc.setAutomationCompositionId(automationComposition.getInstanceId());
        syncAc.setDeployState(automationComposition.getDeployState());
        syncAc.setLockState(automationComposition.getLockState());
        syncAc.setStateChangeResult(automationComposition.getStateChangeResult());
        if (DeployState.DELETED.equals(automationComposition.getDeployState())) {
            message.setDelete(true);
        } else {
            for (var element : automationComposition.getElements().values()) {
                var acElementSync = AcmUtils.createAcElementRestart(element);
                syncAc.getAcElementList().add(acElementSync);

            }
        }
        message.getAutomationcompositionList().add(syncAc);

        LOGGER.debug("Participant AutomationComposition Sync sent {}", message.getMessageId());
        super.send(message);
    }
}
