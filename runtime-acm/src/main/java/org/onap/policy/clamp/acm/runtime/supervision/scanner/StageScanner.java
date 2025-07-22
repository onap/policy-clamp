/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.runtime.supervision.scanner;

import java.util.Comparator;
import java.util.UUID;
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup;
import org.onap.policy.clamp.acm.runtime.main.utils.EncryptionUtils;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AcPreparePublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionMigrationPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantSyncPublisher;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUtils;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.springframework.stereotype.Component;

@Component
public class StageScanner extends AbstractScanner {

    private final AutomationCompositionMigrationPublisher acMigrationPublisher;
    private final AcPreparePublisher acPreparePublisher;

    /**
     * Constructor for instantiating StageScanner.
     *
     * @param acProvider the provider to use to read automation compositions from the database
     * @param participantSyncPublisher the Participant Sync Publisher
     * @param acMigrationPublisher the AutomationComposition Migration Publisher
     * @param acRuntimeParameterGroup the parameters for the automation composition runtime
     * @param encryptionUtils the EncryptionUtils
     */
    public StageScanner(
            final AutomationCompositionProvider acProvider,
            final ParticipantSyncPublisher participantSyncPublisher,
            final AutomationCompositionMigrationPublisher acMigrationPublisher,
            final AcPreparePublisher acPreparePublisher,
            final AcRuntimeParameterGroup acRuntimeParameterGroup,
            final EncryptionUtils encryptionUtils) {
        super(acProvider, participantSyncPublisher, acRuntimeParameterGroup, encryptionUtils);
        this.acMigrationPublisher = acMigrationPublisher;
        this.acPreparePublisher = acPreparePublisher;
    }

    /**
     * Scan with stage: MIGRATE.
     *
     * @param automationComposition the AutomationComposition
     * @param acDefinition the Composition Definition and for migration is the Composition target
     * @param updateSync the update/sync information
     * @param revisionIdComposition the last Update from Composition
     */
    public void scanStage(final AutomationComposition automationComposition,
            AutomationCompositionDefinition acDefinition, UpdateSync updateSync, UUID revisionIdComposition) {
        var completed = true;
        var minStageNotCompleted = 1000; // min stage not completed
        for (var element : automationComposition.getElements().values()) {
            if (AcmUtils.isInTransitionalState(element.getDeployState(), element.getLockState(),
                    element.getSubState())) {
                var toscaNodeTemplate = acDefinition.getServiceTemplate().getToscaTopologyTemplate().getNodeTemplates()
                        .get(element.getDefinition().getName());
                var stageSet = DeployState.MIGRATING.equals(automationComposition.getDeployState())
                            || DeployState.MIGRATION_REVERTING.equals(automationComposition.getDeployState())
                        ? ParticipantUtils.findStageSetMigrate(toscaNodeTemplate.getProperties())
                        : ParticipantUtils.findStageSetPrepare(toscaNodeTemplate.getProperties());
                var minStage = stageSet.stream().min(Comparator.comparing(Integer::valueOf)).orElse(0);
                int stage = element.getStage() != null ? element.getStage() : minStage;
                minStageNotCompleted = Math.min(minStageNotCompleted, stage);
                completed = false;
            }
        }

        if (completed) {
            complete(automationComposition, updateSync);
        } else {
            LOGGER.debug("automation composition scan: transition from state {} to {} not completed",
                    automationComposition.getDeployState(), automationComposition.getLockState());

            if (minStageNotCompleted != automationComposition.getPhase()) {
                savePhase(automationComposition, minStageNotCompleted);
                updateSync.setUpdated(true);
                saveAndSync(automationComposition, updateSync);
                LOGGER.debug("retry message AutomationCompositionMigration");
                var acToSend = new AutomationComposition(automationComposition);
                decryptInstanceProperties(acToSend);
                sendNextStage(acToSend, minStageNotCompleted, revisionIdComposition, acDefinition);
            } else {
                handleTimeout(automationComposition, updateSync);
            }
        }
    }

    private void sendNextStage(final AutomationComposition automationComposition, int minStageNotCompleted,
            UUID revisionIdComposition, AutomationCompositionDefinition acDefinition) {
        if (DeployState.MIGRATING.equals(automationComposition.getDeployState())
                || DeployState.MIGRATION_REVERTING.equals(automationComposition.getDeployState())) {
            LOGGER.debug("retry message AutomationCompositionMigration");
            // acDefinition for migration is the Composition target
            acMigrationPublisher.send(automationComposition, minStageNotCompleted, revisionIdComposition,
                    acDefinition.getRevisionId());
        }
        if (SubState.PREPARING.equals(automationComposition.getSubState())) {
            LOGGER.debug("retry message AutomationCompositionPrepare");
            acPreparePublisher.sendPrepare(automationComposition, minStageNotCompleted, acDefinition.getRevisionId());
        }
    }
}
