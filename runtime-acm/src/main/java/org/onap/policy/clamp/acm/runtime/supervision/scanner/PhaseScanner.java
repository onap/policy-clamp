/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 Nordix OpenInfra Foundation Europe. All rights reserved.
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

import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup;
import org.onap.policy.clamp.acm.runtime.main.utils.EncryptionUtils;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionDeployPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionStateChangePublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantSyncPublisher;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.utils.AcmStageUtils;
import org.onap.policy.clamp.models.acm.utils.AcmStateUtils;
import org.springframework.stereotype.Component;

@Component
public class PhaseScanner extends AbstractScanner {

    private final AutomationCompositionStateChangePublisher acStateChangePublisher;
    private final AutomationCompositionDeployPublisher acDeployPublisher;


    /**
     * Constructor for instantiating PhaseScanner.
     *
     * @param acProvider the provider to use to read automation compositions from the database
     * @param participantSyncPublisher the Participant Sync Publisher
     * @param acStateChangePublisher the automation composition StateChange Publisher
     * @param acDeployPublisher the automation composition Deploy Publisher
     * @param acRuntimeParameterGroup the parameters for the automation composition runtime
     */
    public PhaseScanner(final AutomationCompositionProvider acProvider,
            final ParticipantSyncPublisher participantSyncPublisher,
            final AutomationCompositionStateChangePublisher acStateChangePublisher,
            final AutomationCompositionDeployPublisher acDeployPublisher,
            final AcRuntimeParameterGroup acRuntimeParameterGroup,
            final EncryptionUtils encryptionUtils) {
        super(acProvider, participantSyncPublisher, acRuntimeParameterGroup, encryptionUtils);
        this.acStateChangePublisher = acStateChangePublisher;
        this.acDeployPublisher = acDeployPublisher;
    }

    /**
     * Scan with startPhase: DEPLOY, UNDEPLOY, LOCK and UNLOCK.
     *
     * @param automationComposition the AutomationComposition
     * @param acDefinition the AutomationComposition Definition
     * @param updateSync the update/sync information
     */
    public void scanWithPhase(final AutomationComposition automationComposition,
            AutomationCompositionDefinition acDefinition, UpdateSync updateSync) {
        var completed = true;
        var minSpNotCompleted = 1000; // min startPhase not completed
        var maxSpNotCompleted = 0; // max startPhase not completed
        var defaultMin = 1000; // min startPhase
        var defaultMax = 0; // max startPhase
        for (var element : automationComposition.getElements().values()) {
            var toscaNodeTemplate = acDefinition.getServiceTemplate().getToscaTopologyTemplate().getNodeTemplates()
                    .get(element.getDefinition().getName());
            int startPhase = toscaNodeTemplate != null
                    && element.getDefinition().getVersion().equals(toscaNodeTemplate.getVersion())
                    ? AcmStageUtils.findStartPhase(toscaNodeTemplate.getProperties()) : 0;
            defaultMin = Math.min(defaultMin, startPhase);
            defaultMax = Math.max(defaultMax, startPhase);
            if (AcmStateUtils.isInTransitionalState(element.getDeployState(), element.getLockState(),
                    element.getSubState())) {
                completed = false;
                minSpNotCompleted = Math.min(minSpNotCompleted, startPhase);
                maxSpNotCompleted = Math.max(maxSpNotCompleted, startPhase);
            }
        }

        if (completed) {
            complete(automationComposition, updateSync);
        } else {
            LOGGER.debug("automation composition scan: transition state {} {} not completed",
                    automationComposition.getDeployState(), automationComposition.getLockState());

            var isForward = AcmStateUtils
                    .isForward(automationComposition.getDeployState(), automationComposition.getLockState());

            var nextSpNotCompleted = isForward ? minSpNotCompleted : maxSpNotCompleted;

            if (nextSpNotCompleted != automationComposition.getPhase()) {
                sendAutomationCompositionMsg(automationComposition, nextSpNotCompleted, updateSync, acDefinition);
            } else {
                handleTimeout(automationComposition, updateSync);
            }
        }
    }

    private void sendAutomationCompositionMsg(AutomationComposition automationComposition, int startPhase,
            UpdateSync updateSync, AutomationCompositionDefinition acDefinition) {
        savePhase(automationComposition, startPhase);
        updateSync.setUpdated(true);
        saveAndSync(automationComposition, updateSync);

        if (DeployState.DEPLOYING.equals(automationComposition.getDeployState())) {
            LOGGER.debug("retry message AutomationCompositionDeploy");
            var acToSend = new AutomationComposition(automationComposition);
            decryptInstanceProperties(acToSend);
            acDeployPublisher.send(acToSend, startPhase, false, acDefinition.getRevisionId());
        } else {
            LOGGER.debug("retry message AutomationCompositionStateChange");
            acStateChangePublisher.send(automationComposition, startPhase, false, acDefinition.getRevisionId());
        }
    }
}
