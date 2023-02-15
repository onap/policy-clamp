/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2023 Nordix Foundation.
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

package org.onap.policy.clamp.acm.runtime.supervision;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionDeployPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionStateChangePublisher;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUtils;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class is used to scan the automation compositions in the database and check if they are in the correct state.
 */
@Component
public class SupervisionScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(SupervisionScanner.class);

    private final HandleCounter<UUID> automationCompositionCounter = new HandleCounter<>();
    private final Map<UUID, Integer> phaseMap = new HashMap<>();

    private final AutomationCompositionProvider automationCompositionProvider;
    private final AcDefinitionProvider acDefinitionProvider;
    private final AutomationCompositionStateChangePublisher automationCompositionStateChangePublisher;
    private final AutomationCompositionDeployPublisher automationCompositionDeployPublisher;

    /**
     * Constructor for instantiating SupervisionScanner.
     *
     * @param automationCompositionProvider the provider to use to read automation compositions from the database
     * @param acDefinitionProvider the Policy Models Provider
     * @param automationCompositionStateChangePublisher the AutomationComposition StateChange Publisher
     * @param automationCompositionDeployPublisher the AutomationCompositionUpdate Publisher
     * @param acRuntimeParameterGroup the parameters for the automation composition runtime
     */
    public SupervisionScanner(final AutomationCompositionProvider automationCompositionProvider,
            final AcDefinitionProvider acDefinitionProvider,
            final AutomationCompositionStateChangePublisher automationCompositionStateChangePublisher,
            final AutomationCompositionDeployPublisher automationCompositionDeployPublisher,
            final AcRuntimeParameterGroup acRuntimeParameterGroup) {
        this.automationCompositionProvider = automationCompositionProvider;
        this.acDefinitionProvider = acDefinitionProvider;
        this.automationCompositionStateChangePublisher = automationCompositionStateChangePublisher;
        this.automationCompositionDeployPublisher = automationCompositionDeployPublisher;

        automationCompositionCounter.setMaxRetryCount(
                acRuntimeParameterGroup.getParticipantParameters().getUpdateParameters().getMaxRetryCount());
        automationCompositionCounter
                .setMaxWaitMs(acRuntimeParameterGroup.getParticipantParameters().getMaxStatusWaitMs());
    }

    /**
     * Run Scanning.
     *
     * @param counterCheck if true activate counter and retry
     */
    public void run(boolean counterCheck) {
        LOGGER.debug("Scanning automation compositions in the database . . .");

        var list = acDefinitionProvider.getAllAcDefinitions();
        for (var acDefinition : list) {
            var acList = automationCompositionProvider.getAcInstancesByCompositionId(acDefinition.getCompositionId());
            for (var automationComposition : acList) {
                scanAutomationComposition(automationComposition, acDefinition.getServiceTemplate(), counterCheck);
            }
        }

        LOGGER.debug("Automation composition scan complete . . .");
    }

    private void scanAutomationComposition(final AutomationComposition automationComposition,
            ToscaServiceTemplate serviceTemplate, boolean counterCheck) {
        LOGGER.debug("scanning automation composition {} . . .", automationComposition.getInstanceId());

        if (!AcmUtils.isInTransitionalState(automationComposition.getDeployState(),
                automationComposition.getLockState())) {
            LOGGER.debug("automation composition {} scanned, OK", automationComposition.getInstanceId());

            // Clear missed report counter on automation composition
            clearFaultAndCounter(automationComposition);
            return;
        }

        var completed = true;
        var minSpNotCompleted = 1000; // min startPhase not completed
        var maxSpNotCompleted = 0; // max startPhase not completed
        var defaultMin = 1000; // min startPhase
        var defaultMax = 0; // max startPhase
        for (var element : automationComposition.getElements().values()) {
            var toscaNodeTemplate = serviceTemplate.getToscaTopologyTemplate().getNodeTemplates()
                    .get(element.getDefinition().getName());
            int startPhase = ParticipantUtils.findStartPhase(toscaNodeTemplate.getProperties());
            defaultMin = Math.min(defaultMin, startPhase);
            defaultMax = Math.max(defaultMax, startPhase);
            if (AcmUtils.isInTransitionalState(element.getDeployState(), element.getLockState())) {
                completed = false;
                minSpNotCompleted = Math.min(minSpNotCompleted, startPhase);
                maxSpNotCompleted = Math.max(maxSpNotCompleted, startPhase);
            }
        }

        if (completed) {
            LOGGER.debug("automation composition scan: transition state {} {} ", automationComposition.getDeployState(),
                    automationComposition.getLockState());

            var deployState = automationComposition.getDeployState();
            automationComposition.setDeployState(AcmUtils.deployCompleted(deployState));
            automationComposition
                    .setLockState(AcmUtils.lockCompleted(deployState, automationComposition.getLockState()));
            automationCompositionProvider.updateAutomationComposition(automationComposition);

            // Clear missed report counter on automation composition
            clearFaultAndCounter(automationComposition);
        } else {
            LOGGER.debug("automation composition scan: transition from state {} to {} not completed",
                    automationComposition.getDeployState(), automationComposition.getLockState());

            var isForward =
                    AcmUtils.isForward(automationComposition.getDeployState(), automationComposition.getLockState());

            var nextSpNotCompleted = isForward ? minSpNotCompleted : maxSpNotCompleted;
            var firstStartPhase = isForward ? defaultMin : defaultMax;

            if (nextSpNotCompleted != phaseMap.getOrDefault(automationComposition.getInstanceId(), firstStartPhase)) {
                phaseMap.put(automationComposition.getInstanceId(), nextSpNotCompleted);
                sendAutomationCompositionMsg(automationComposition, serviceTemplate, nextSpNotCompleted,
                        firstStartPhase == nextSpNotCompleted);
            } else if (counterCheck) {
                phaseMap.put(automationComposition.getInstanceId(), nextSpNotCompleted);
                handleCounter(automationComposition, serviceTemplate, nextSpNotCompleted,
                        firstStartPhase == nextSpNotCompleted);
            }
        }
    }

    private void clearFaultAndCounter(AutomationComposition automationComposition) {
        automationCompositionCounter.clear(automationComposition.getInstanceId());
        phaseMap.remove(automationComposition.getInstanceId());
    }

    private void handleCounter(AutomationComposition automationComposition, ToscaServiceTemplate serviceTemplate,
            int startPhase, boolean firstStartPhase) {
        var instanceId = automationComposition.getInstanceId();
        if (automationCompositionCounter.isFault(instanceId)) {
            LOGGER.debug("report AutomationComposition fault");
            return;
        }

        if (automationCompositionCounter.getDuration(instanceId) > automationCompositionCounter.getMaxWaitMs()) {
            if (automationCompositionCounter.count(instanceId)) {
                phaseMap.put(instanceId, startPhase);
                sendAutomationCompositionMsg(automationComposition, serviceTemplate, startPhase, firstStartPhase);
            } else {
                LOGGER.debug("report AutomationComposition fault");
                automationCompositionCounter.setFault(instanceId);
            }
        }
    }

    private void sendAutomationCompositionMsg(AutomationComposition automationComposition,
            ToscaServiceTemplate serviceTemplate, int startPhase, boolean firstStartPhase) {
        if (DeployState.DEPLOYING.equals(automationComposition.getDeployState())) {
            LOGGER.debug("retry message AutomationCompositionUpdate");
            automationCompositionDeployPublisher.send(automationComposition, serviceTemplate, startPhase,
                    firstStartPhase);
        } else {
            LOGGER.debug("retry message AutomationCompositionStateChange");
            automationCompositionStateChangePublisher.send(automationComposition, startPhase, firstStartPhase);
        }
    }
}
