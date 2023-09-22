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
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUtils;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
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

    private final TimeoutHandler<UUID> acTimeout = new TimeoutHandler<>();
    private final Map<UUID, Integer> phaseMap = new HashMap<>();

    private final AutomationCompositionProvider automationCompositionProvider;
    private final AcDefinitionProvider acDefinitionProvider;
    private final AutomationCompositionStateChangePublisher automationCompositionStateChangePublisher;
    private final AutomationCompositionDeployPublisher automationCompositionDeployPublisher;
    private final AcRuntimeParameterGroup acRuntimeParameterGroup;

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
        this.acRuntimeParameterGroup = acRuntimeParameterGroup;

        acTimeout.setMaxWaitMs(acRuntimeParameterGroup.getParticipantParameters().getMaxStatusWaitMs());
    }

    /**
     * Run Scanning.
     */
    public void run() {
        LOGGER.debug("Scanning automation compositions in the database . . .");

        var list = acDefinitionProvider.getAllAcDefinitions();
        for (var acDefinition : list) {
            if (AcTypeState.PRIMING.equals(acDefinition.getState())
                    || AcTypeState.DEPRIMING.equals(acDefinition.getState())) {
                scanAutomationCompositionDefinition(acDefinition);
            } else {
                acTimeout.clear(acDefinition.getCompositionId());
                var acList =
                        automationCompositionProvider.getAcInstancesByCompositionId(acDefinition.getCompositionId());
                for (var automationComposition : acList) {
                    scanAutomationComposition(automationComposition, acDefinition.getServiceTemplate());
                }
            }
        }

        LOGGER.debug("Automation composition scan complete . . .");
    }

    private void scanAutomationCompositionDefinition(AutomationCompositionDefinition acDefinition) {
        if (StateChangeResult.FAILED.equals(acDefinition.getStateChangeResult())) {
            LOGGER.debug("automation definition {} scanned, OK", acDefinition.getCompositionId());

            // Clear Timeout on ac Definition
            acTimeout.clear(acDefinition.getCompositionId());
            return;
        }

        if (acTimeout.isTimeout(acDefinition.getCompositionId())
                && StateChangeResult.NO_ERROR.equals(acDefinition.getStateChangeResult())) {
            // retry by the user
            LOGGER.debug("clearing Timeout for the ac definition");
            acTimeout.clear(acDefinition.getCompositionId());
        }

        handleTimeout(acDefinition);
    }

    private void scanAutomationComposition(final AutomationComposition automationComposition,
            ToscaServiceTemplate serviceTemplate) {
        LOGGER.debug("scanning automation composition {} . . .", automationComposition.getInstanceId());

        if (!AcmUtils.isInTransitionalState(automationComposition.getDeployState(),
                automationComposition.getLockState())
                || StateChangeResult.FAILED.equals(automationComposition.getStateChangeResult())) {
            LOGGER.debug("automation composition {} scanned, OK", automationComposition.getInstanceId());

            // Clear Timeout on automation composition
            clearTimeout(automationComposition, true);
            return;
        }

        if (acTimeout.isTimeout(automationComposition.getInstanceId())
                && StateChangeResult.NO_ERROR.equals(automationComposition.getStateChangeResult())) {
            // retry by the user
            LOGGER.debug("clearing Timeout for the ac instance");
            clearTimeout(automationComposition, true);
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

            complete(automationComposition);
        } else {
            LOGGER.debug("automation composition scan: transition from state {} to {} not completed",
                    automationComposition.getDeployState(), automationComposition.getLockState());

            if (DeployState.UPDATING.equals(automationComposition.getDeployState())) {
                // UPDATING do not need phases
                handleTimeout(automationComposition);
                return;
            }

            var isForward =
                    AcmUtils.isForward(automationComposition.getDeployState(), automationComposition.getLockState());

            var nextSpNotCompleted = isForward ? minSpNotCompleted : maxSpNotCompleted;
            var firstStartPhase = isForward ? defaultMin : defaultMax;

            if (nextSpNotCompleted != phaseMap.getOrDefault(automationComposition.getInstanceId(), firstStartPhase)) {
                phaseMap.put(automationComposition.getInstanceId(), nextSpNotCompleted);
                sendAutomationCompositionMsg(automationComposition, serviceTemplate, nextSpNotCompleted,
                        firstStartPhase == nextSpNotCompleted);
            } else {
                handleTimeout(automationComposition);
            }
        }
    }

    private void complete(final AutomationComposition automationComposition) {
        var deployState = automationComposition.getDeployState();
        automationComposition.setDeployState(AcmUtils.deployCompleted(deployState));
        automationComposition.setLockState(AcmUtils.lockCompleted(deployState, automationComposition.getLockState()));
        if (StateChangeResult.TIMEOUT.equals(automationComposition.getStateChangeResult())) {
            automationComposition.setStateChangeResult(StateChangeResult.NO_ERROR);
        }
        if (DeployState.DELETED.equals(automationComposition.getDeployState())) {
            automationCompositionProvider.deleteAutomationComposition(automationComposition.getInstanceId());
        } else {
            automationCompositionProvider.updateAutomationComposition(automationComposition);
        }

        // Clear timeout on automation composition
        clearTimeout(automationComposition, true);
    }

    private void clearTimeout(AutomationComposition automationComposition, boolean cleanPhase) {
        acTimeout.clear(automationComposition.getInstanceId());
        if (cleanPhase) {
            phaseMap.remove(automationComposition.getInstanceId());
        }
    }

    private void handleTimeout(AutomationCompositionDefinition acDefinition) {
        var compositionId = acDefinition.getCompositionId();
        if (acTimeout.isTimeout(compositionId)) {
            LOGGER.debug("The ac definition is in timeout {}", acDefinition.getCompositionId());
            return;
        }

        if (acTimeout.getDuration(compositionId) > acTimeout.getMaxWaitMs()) {
            LOGGER.debug("Report timeout for the ac definition {}", acDefinition.getCompositionId());
            acTimeout.setTimeout(compositionId);
            acDefinition.setStateChangeResult(StateChangeResult.TIMEOUT);
            acDefinitionProvider.updateAcDefinition(acDefinition,
                    acRuntimeParameterGroup.getAcmParameters().getToscaCompositionName());
        }
    }

    private void handleTimeout(AutomationComposition automationComposition) {
        var instanceId = automationComposition.getInstanceId();
        if (acTimeout.isTimeout(instanceId)) {
            LOGGER.debug("The ac instance is in timeout {}", automationComposition.getInstanceId());
            return;
        }

        if (acTimeout.getDuration(instanceId) > acTimeout.getMaxWaitMs()) {
            LOGGER.debug("Report timeout for the ac instance {}", automationComposition.getInstanceId());
            acTimeout.setTimeout(instanceId);
            automationComposition.setStateChangeResult(StateChangeResult.TIMEOUT);
            automationCompositionProvider.updateAutomationComposition(automationComposition);
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
        // Clear timeout on automation composition
        clearTimeout(automationComposition, false);
    }
}
