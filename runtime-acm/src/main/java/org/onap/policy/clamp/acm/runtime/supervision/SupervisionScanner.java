/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2025 Nordix Foundation.
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
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.runtime.supervision.scanner.AcDefinitionScanner;
import org.onap.policy.clamp.acm.runtime.supervision.scanner.PhaseScanner;
import org.onap.policy.clamp.acm.runtime.supervision.scanner.SimpleScanner;
import org.onap.policy.clamp.acm.runtime.supervision.scanner.StageScanner;
import org.onap.policy.clamp.acm.runtime.supervision.scanner.UpdateSync;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.concepts.SubState;
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
@RequiredArgsConstructor
public class SupervisionScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(SupervisionScanner.class);

    private final AutomationCompositionProvider automationCompositionProvider;
    private final AcDefinitionProvider acDefinitionProvider;
    private final AcDefinitionScanner acDefinitionScanner;
    private final StageScanner stageScanner;
    private final SimpleScanner simpleScanner;
    private final PhaseScanner phaseScanner;

    /**
     * Run Scanning.
     */
    public void run() {
        LOGGER.debug("Scanning automation compositions in the database . . .");

        var acDefinitions = acDefinitionProvider.getAllAcDefinitionsInTransition();
        for (var acDefinition : acDefinitions) {
            scanAcDefinition(acDefinition.getCompositionId());
        }

        var instances = automationCompositionProvider.getAcInstancesInTransition();
        Map<UUID, AutomationCompositionDefinition> acDefinitionMap = new HashMap<>();
        for (var instance : instances) {
            scanAutomationComposition(instance.getInstanceId(), acDefinitionMap);
        }
        LOGGER.debug("Automation composition scan complete . . .");
    }

    private void scanAcDefinition(UUID compositionId) {
        var acDefinitionOpt = acDefinitionProvider.findAcDefinition(compositionId);
        var updateSync = new UpdateSync();
        acDefinitionOpt.ifPresent(acDefinition ->
                acDefinitionScanner.scanAutomationCompositionDefinition(acDefinition, updateSync));
    }

    private void scanAutomationComposition(UUID instanceId,
            Map<UUID, AutomationCompositionDefinition> acDefinitionMap) {
        var automationCompositionOpt = automationCompositionProvider.findAutomationComposition(instanceId);
        var updateSync = new UpdateSync();
        if (automationCompositionOpt.isPresent()) {
            var automationComposition = automationCompositionOpt.get();
            var compositionId = automationComposition.getCompositionTargetId() != null
                    ? automationComposition.getCompositionTargetId() : automationComposition.getCompositionId();
            var acDefinition = acDefinitionMap.computeIfAbsent(compositionId, acDefinitionProvider::getAcDefinition);
            scanAutomationComposition(automationComposition, acDefinition.getServiceTemplate(), updateSync);
        }
    }

    private void scanAutomationComposition(final AutomationComposition automationComposition,
            ToscaServiceTemplate serviceTemplate, UpdateSync updateSync) {
        LOGGER.debug("scanning automation composition {} . . .", automationComposition.getInstanceId());

        if (!AcmUtils.isInTransitionalState(automationComposition.getDeployState(),
                automationComposition.getLockState(), automationComposition.getSubState())
                || StateChangeResult.FAILED.equals(automationComposition.getStateChangeResult())) {
            LOGGER.debug("automation composition {} scanned, OK", automationComposition.getInstanceId());
            simpleScanner.saveAndSync(automationComposition, updateSync);
        }

        if (DeployState.MIGRATING.equals(automationComposition.getDeployState())) {
            stageScanner.scanStage(automationComposition, serviceTemplate, updateSync);
        } else if (DeployState.UPDATING.equals(automationComposition.getDeployState())
                || SubState.REVIEWING.equals(automationComposition.getSubState())
                || SubState.PREPARING.equals(automationComposition.getSubState())
                || SubState.MIGRATION_PRECHECKING.equals(automationComposition.getSubState())) {
            simpleScanner.simpleScan(automationComposition, updateSync);
        } else {
            phaseScanner.scanWithPhase(automationComposition, serviceTemplate, updateSync);
        }
    }
}
