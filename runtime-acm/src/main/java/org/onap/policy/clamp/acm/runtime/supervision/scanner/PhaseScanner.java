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

import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionDeployPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionStateChangePublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantSyncPublisher;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUtils;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
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
            final AcRuntimeParameterGroup acRuntimeParameterGroup) {
        super(acProvider, participantSyncPublisher, acRuntimeParameterGroup);
        this.acStateChangePublisher = acStateChangePublisher;
        this.acDeployPublisher = acDeployPublisher;
    }

    /**
     * Scan with startPhase: DEPLOY, UNDEPLOY, LOCK and UNLOCK.
     *
     * @param automationComposition the AutomationComposition
     * @param serviceTemplate the ToscaServiceTemplate
     * @param updateSync the update/sync information
     */
    public void scanWithPhase(final AutomationComposition automationComposition,
            ToscaServiceTemplate serviceTemplate, UpdateSync updateSync) {
        var completed = true;
        var minSpNotCompleted = 1000; // min startPhase not completed
        var maxSpNotCompleted = 0; // max startPhase not completed
        var defaultMin = 1000; // min startPhase
        var defaultMax = 0; // max startPhase
        for (var element : automationComposition.getElements().values()) {
            var toscaNodeTemplate = serviceTemplate.getToscaTopologyTemplate().getNodeTemplates()
                    .get(element.getDefinition().getName());
            int startPhase = toscaNodeTemplate != null
                    && element.getDefinition().getVersion().equals(toscaNodeTemplate.getVersion())
                    ? ParticipantUtils.findStartPhase(toscaNodeTemplate.getProperties()) : 0;
            defaultMin = Math.min(defaultMin, startPhase);
            defaultMax = Math.max(defaultMax, startPhase);
            if (AcmUtils.isInTransitionalState(element.getDeployState(), element.getLockState(),
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

            var isForward =
                    AcmUtils.isForward(automationComposition.getDeployState(), automationComposition.getLockState());

            var nextSpNotCompleted = isForward ? minSpNotCompleted : maxSpNotCompleted;

            if (nextSpNotCompleted != automationComposition.getPhase()) {
                sendAutomationCompositionMsg(automationComposition, nextSpNotCompleted, updateSync);
            } else {
                handleTimeout(automationComposition, updateSync);
            }
        }
    }

    private void sendAutomationCompositionMsg(AutomationComposition automationComposition, int startPhase,
            UpdateSync updateSync) {
        savePhase(automationComposition, startPhase);
        updateSync.setUpdated(true);
        saveAndSync(automationComposition, updateSync);

        if (DeployState.DEPLOYING.equals(automationComposition.getDeployState())) {
            LOGGER.debug("retry message AutomationCompositionDeploy");
            acDeployPublisher.send(automationComposition, startPhase, false);
        } else {
            LOGGER.debug("retry message AutomationCompositionStateChange");
            acStateChangePublisher.send(automationComposition, startPhase, false);
        }
    }
}
