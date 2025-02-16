/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 Nordix Foundation.
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
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup;
import org.onap.policy.clamp.acm.runtime.main.utils.EncryptionUtils;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionMigrationPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantSyncPublisher;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUtils;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.springframework.stereotype.Component;

@Component
public class StageScanner extends AbstractScanner {

    private final AutomationCompositionMigrationPublisher acMigrationPublisher;

    /**
     * Constructor for instantiating StageScanner.
     *
     * @param acProvider the provider to use to read automation compositions from the database
     * @param participantSyncPublisher the Participant Sync Publisher
     * @param acMigrationPublisher the AutomationComposition Migration Publisher
     * @param acRuntimeParameterGroup the parameters for the automation composition runtime
     */
    public StageScanner(final AutomationCompositionProvider acProvider,
                        final ParticipantSyncPublisher participantSyncPublisher,
                        final AutomationCompositionMigrationPublisher acMigrationPublisher,
                        final AcRuntimeParameterGroup acRuntimeParameterGroup,
                        final EncryptionUtils encryptionUtils) {
        super(acProvider, participantSyncPublisher, acRuntimeParameterGroup, encryptionUtils);
        this.acMigrationPublisher = acMigrationPublisher;
    }

    /**
     * Scan with stage: MIGRATE.
     *
     * @param automationComposition the AutomationComposition
     * @param serviceTemplate the ToscaServiceTemplate
     * @param updateSync the update/sync information
     */
    public void scanStage(final AutomationComposition automationComposition, ToscaServiceTemplate serviceTemplate,
            UpdateSync updateSync) {
        var completed = true;
        var minStageNotCompleted = 1000; // min stage not completed
        for (var element : automationComposition.getElements().values()) {
            if (AcmUtils.isInTransitionalState(element.getDeployState(), element.getLockState(),
                    element.getSubState())) {
                var toscaNodeTemplate = serviceTemplate.getToscaTopologyTemplate().getNodeTemplates()
                        .get(element.getDefinition().getName());
                var stageSet = ParticipantUtils.findStageSet(toscaNodeTemplate.getProperties());
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
                acMigrationPublisher.send(automationComposition, minStageNotCompleted);
            } else {
                handleTimeout(automationComposition, updateSync);
            }
        }
    }
}
