/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2024 Nordix Foundation.
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

import java.util.Comparator;
import java.util.HashMap;
import java.util.UUID;
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionDeployPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionMigrationPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionStateChangePublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantSyncPublisher;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUtils;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.clamp.models.acm.utils.TimestampHelper;
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

    private final long maxStatusWaitMs;

    private final AutomationCompositionProvider automationCompositionProvider;
    private final AcDefinitionProvider acDefinitionProvider;
    private final AutomationCompositionStateChangePublisher automationCompositionStateChangePublisher;
    private final AutomationCompositionDeployPublisher automationCompositionDeployPublisher;
    private final ParticipantSyncPublisher participantSyncPublisher;
    private final AutomationCompositionMigrationPublisher automationCompositionMigrationPublisher;

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
            final ParticipantSyncPublisher participantSyncPublisher,
            final AutomationCompositionMigrationPublisher automationCompositionMigrationPublisher,
            final AcRuntimeParameterGroup acRuntimeParameterGroup) {
        this.automationCompositionProvider = automationCompositionProvider;
        this.acDefinitionProvider = acDefinitionProvider;
        this.automationCompositionStateChangePublisher = automationCompositionStateChangePublisher;
        this.automationCompositionDeployPublisher = automationCompositionDeployPublisher;
        this.participantSyncPublisher = participantSyncPublisher;
        this.automationCompositionMigrationPublisher = automationCompositionMigrationPublisher;
        this.maxStatusWaitMs = acRuntimeParameterGroup.getParticipantParameters().getMaxStatusWaitMs();
    }

    /**
     * Run Scanning.
     */
    public void run() {
        LOGGER.debug("Scanning automation compositions in the database . . .");

        var acDefinitionList = acDefinitionProvider.getAllAcDefinitionsInTransition();
        for (var acDefinition : acDefinitionList) {
            scanAutomationCompositionDefinition(acDefinition);
        }

        var acList = automationCompositionProvider.getAcInstancesInTransition();
        HashMap<UUID, AutomationCompositionDefinition> acDefinitionMap = new HashMap<>();
        for (var automationComposition : acList) {
            var compositionId = automationComposition.getCompositionTargetId() != null
                    ? automationComposition.getCompositionTargetId() : automationComposition.getCompositionId();
            var acDefinition = acDefinitionMap.computeIfAbsent(compositionId, acDefinitionProvider::getAcDefinition);
            scanAutomationComposition(automationComposition, acDefinition.getServiceTemplate());
        }
        LOGGER.debug("Automation composition scan complete . . .");
    }

    private void scanAutomationCompositionDefinition(AutomationCompositionDefinition acDefinition) {
        if (StateChangeResult.FAILED.equals(acDefinition.getStateChangeResult())) {
            LOGGER.debug("automation definition {} scanned, OK", acDefinition.getCompositionId());
            return;
        }

        boolean completed = true;
        var finalState = AcTypeState.PRIMING.equals(acDefinition.getState())
            || AcTypeState.PRIMED.equals(acDefinition.getState()) ? AcTypeState.PRIMED : AcTypeState.COMMISSIONED;
        for (var element : acDefinition.getElementStateMap().values()) {
            if (!finalState.equals(element.getState())) {
                completed = false;
                break;
            }
        }
        if (completed) {
            acDefinitionProvider.updateAcDefinitionState(acDefinition.getCompositionId(), finalState,
                StateChangeResult.NO_ERROR, null);
            participantSyncPublisher.sendSync(acDefinition, null);
        } else {
            handleTimeout(acDefinition);
        }
    }

    private void scanAutomationComposition(final AutomationComposition automationComposition,
            ToscaServiceTemplate serviceTemplate) {
        LOGGER.debug("scanning automation composition {} . . .", automationComposition.getInstanceId());

        if (!AcmUtils.isInTransitionalState(automationComposition.getDeployState(),
                automationComposition.getLockState(), automationComposition.getSubState())
                || StateChangeResult.FAILED.equals(automationComposition.getStateChangeResult())) {
            LOGGER.debug("automation composition {} scanned, OK", automationComposition.getInstanceId());

            return;
        }

        if (DeployState.MIGRATING.equals(automationComposition.getDeployState())) {
            scanStage(automationComposition, serviceTemplate);
        } else if (DeployState.UPDATING.equals(automationComposition.getDeployState())
                || SubState.PREPARING.equals(automationComposition.getSubState())
                || SubState.REVIEWING.equals(automationComposition.getSubState())
                || SubState.MIGRATION_PRECHECKING.equals(automationComposition.getSubState())) {
            simpleScan(automationComposition, serviceTemplate);
        } else {
            scanWithPhase(automationComposition, serviceTemplate);
        }
    }

    /**
     * Scan with startPhase: DEPLOY, UNDEPLOY, LOCK and UNLOCK.
     *
     * @param automationComposition the AutomationComposition
     * @param serviceTemplate the ToscaServiceTemplate
     */
    private void scanWithPhase(final AutomationComposition automationComposition,
            ToscaServiceTemplate serviceTemplate) {
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
            if (AcmUtils.isInTransitionalState(element.getDeployState(), element.getLockState(),
                    element.getSubState())) {
                completed = false;
                minSpNotCompleted = Math.min(minSpNotCompleted, startPhase);
                maxSpNotCompleted = Math.max(maxSpNotCompleted, startPhase);
            }
        }

        if (completed) {
            complete(automationComposition, serviceTemplate);
        } else {
            LOGGER.debug("automation composition scan: transition state {} {} not completed",
                    automationComposition.getDeployState(), automationComposition.getLockState());

            var isForward =
                    AcmUtils.isForward(automationComposition.getDeployState(), automationComposition.getLockState());

            var nextSpNotCompleted = isForward ? minSpNotCompleted : maxSpNotCompleted;

            if (nextSpNotCompleted != automationComposition.getPhase()) {
                sendAutomationCompositionMsg(automationComposition, serviceTemplate, nextSpNotCompleted);
            } else {
                handleTimeout(automationComposition);
            }
        }
    }

    /**
     * Simple scan: UPDATE, PREPARE, REVIEW, MIGRATE_PRECHECKING.
     *
     * @param automationComposition the AutomationComposition
     * @param serviceTemplate the ToscaServiceTemplate
     */
    private void simpleScan(final AutomationComposition automationComposition, ToscaServiceTemplate serviceTemplate) {
        var completed = automationComposition.getElements().values().stream()
                .filter(element -> AcmUtils.isInTransitionalState(element.getDeployState(), element.getLockState(),
                        element.getSubState())).findFirst().isEmpty();

        if (completed) {
            complete(automationComposition, serviceTemplate);
        } else {
            handleTimeout(automationComposition);
        }
    }

    /**
     * Scan with stage: MIGRATE.
     *
     * @param automationComposition the AutomationComposition
     * @param serviceTemplate the ToscaServiceTemplate
     */
    private void scanStage(final AutomationComposition automationComposition, ToscaServiceTemplate serviceTemplate) {
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
            complete(automationComposition, serviceTemplate);
        } else {
            LOGGER.debug("automation composition scan: transition from state {} to {} not completed",
                    automationComposition.getDeployState(), automationComposition.getLockState());

            if (minStageNotCompleted != automationComposition.getPhase()) {
                savePahese(automationComposition, minStageNotCompleted);
                LOGGER.debug("retry message AutomationCompositionMigration");
                automationCompositionMigrationPublisher.send(automationComposition, minStageNotCompleted);
            } else {
                handleTimeout(automationComposition);
            }
        }
    }

    private void complete(final AutomationComposition automationComposition,
            ToscaServiceTemplate serviceTemplate) {
        LOGGER.debug("automation composition scan: transition state {} {} {} completed",
                automationComposition.getDeployState(), automationComposition.getLockState(),
                automationComposition.getSubState());

        var deployState = automationComposition.getDeployState();
        if (DeployState.MIGRATING.equals(automationComposition.getDeployState())) {
            // migration scenario
            automationComposition.setCompositionId(automationComposition.getCompositionTargetId());
            automationComposition.setCompositionTargetId(null);
        }
        automationComposition.setDeployState(AcmUtils.deployCompleted(deployState));
        automationComposition.setLockState(AcmUtils.lockCompleted(deployState, automationComposition.getLockState()));
        automationComposition.setPhase(null);
        automationComposition.setSubState(SubState.NONE);
        automationComposition.setPrecheck(null);
        if (StateChangeResult.TIMEOUT.equals(automationComposition.getStateChangeResult())) {
            automationComposition.setStateChangeResult(StateChangeResult.NO_ERROR);
        }
        var acToUpdate = automationComposition;
        if (DeployState.DELETED.equals(automationComposition.getDeployState())) {
            automationCompositionProvider.deleteAutomationComposition(automationComposition.getInstanceId());
        } else {
            acToUpdate = automationCompositionProvider.updateAcState(acToUpdate);
        }
        participantSyncPublisher.sendSync(serviceTemplate, acToUpdate);
    }

    private void handleTimeout(AutomationCompositionDefinition acDefinition) {
        if (StateChangeResult.TIMEOUT.equals(acDefinition.getStateChangeResult())) {
            LOGGER.debug("The ac definition is in timeout {}", acDefinition.getCompositionId());
            return;
        }
        var now = TimestampHelper.nowEpochMilli();
        var lastMsg = TimestampHelper.toEpochMilli(acDefinition.getLastMsg());
        if ((now - lastMsg) > maxStatusWaitMs) {
            LOGGER.debug("Report timeout for the ac definition {}", acDefinition.getCompositionId());
            acDefinition.setStateChangeResult(StateChangeResult.TIMEOUT);
            acDefinitionProvider.updateAcDefinitionState(acDefinition.getCompositionId(),
                acDefinition.getState(), acDefinition.getStateChangeResult(), acDefinition.getRestarting());
        }
    }

    private void handleTimeout(AutomationComposition automationComposition) {
        LOGGER.debug("automation composition scan: transition from state {} to {} {} not completed",
                automationComposition.getDeployState(), automationComposition.getLockState(),
                automationComposition.getSubState());

        if (StateChangeResult.TIMEOUT.equals(automationComposition.getStateChangeResult())) {
            LOGGER.debug("The ac instance is in timeout {}", automationComposition.getInstanceId());
            return;
        }
        var now = TimestampHelper.nowEpochMilli();
        var lastMsg = TimestampHelper.toEpochMilli(automationComposition.getLastMsg());
        if ((now - lastMsg) > maxStatusWaitMs) {
            LOGGER.debug("Report timeout for the ac instance {}", automationComposition.getInstanceId());
            automationComposition.setStateChangeResult(StateChangeResult.TIMEOUT);
            automationCompositionProvider.updateAcState(automationComposition);
        }
    }

    private void savePahese(AutomationComposition automationComposition, int startPhase) {
        automationComposition.setLastMsg(TimestampHelper.now());
        automationComposition.setPhase(startPhase);
        automationCompositionProvider.updateAcState(automationComposition);
    }

    private void sendAutomationCompositionMsg(AutomationComposition automationComposition,
            ToscaServiceTemplate serviceTemplate, int startPhase) {
        savePahese(automationComposition, startPhase);

        if (DeployState.DEPLOYING.equals(automationComposition.getDeployState())) {
            LOGGER.debug("retry message AutomationCompositionDeploy");
            automationCompositionDeployPublisher.send(automationComposition, serviceTemplate, startPhase,
                    false);
        } else {
            LOGGER.debug("retry message AutomationCompositionStateChange");
            automationCompositionStateChangePublisher.send(automationComposition, startPhase, false);
        }
    }
}
