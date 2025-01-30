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
import org.onap.policy.clamp.acm.runtime.supervision.scanner.MonitoringScanner;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.MessageProvider;
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
    private final MessageProvider messageProvider;
    private final MonitoringScanner monitoringScanner;

    /**
     * Run Scanning.
     */
    public void run() {
        LOGGER.debug("Scanning automation compositions in the database . . .");

        messageProvider.removeOldJobs();

        var compositionIds = acDefinitionProvider.getAllAcDefinitionsInTransition();
        compositionIds.addAll(messageProvider.findCompositionMessages());
        for (var compositionId : compositionIds) {
            scanAcDefinition(compositionId);
        }

        var instanceIds = automationCompositionProvider.getAcInstancesInTransition();
        instanceIds.addAll(messageProvider.findInstanceMessages());
        Map<UUID, AutomationCompositionDefinition> acDefinitionMap = new HashMap<>();
        for (var instanceId : instanceIds) {
            scanAutomationComposition(instanceId, acDefinitionMap);
        }
        LOGGER.debug("Automation composition scan complete . . .");
    }

    private void scanAcDefinition(UUID compositionId) {
        var optJobId = messageProvider.createJob(compositionId);
        if (optJobId.isEmpty()) {
            return;
        }
        monitoringScanner.scanAcDefinition(compositionId);
        messageProvider.removeJob(optJobId.get());
    }

    private void scanAutomationComposition(UUID instanceId,
            Map<UUID, AutomationCompositionDefinition> acDefinitionMap) {
        var optJobId = messageProvider.createJob(instanceId);
        if (optJobId.isEmpty()) {
            return;
        }
        monitoringScanner.scanAutomationComposition(instanceId, acDefinitionMap);
        messageProvider.removeJob(optJobId.get());
    }
}
