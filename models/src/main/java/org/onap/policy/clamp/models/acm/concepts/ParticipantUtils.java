/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.models.acm.concepts;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ParticipantUtils {
    private static final String STAGE_MIGRATE = "migrate";
    private static final String STAGE_PREPARE = "prepare";
    public static final String DEFAULT_TIMEOUT = "maxOperationWaitMs";
    public static final String PRIME_TIMEOUT = "primeTimeoutMs";
    public static final String DEPRIME_TIMEOUT = "deprimeTimeoutMs";
    public static final String DEPLOY_TIMEOUT = "deployTimeoutMs";
    public static final String UNDEPLOY_TIMEOUT = "undeployTimeoutMs";
    public static final String UPDATE_TIMEOUT = "updateTimeoutMs";
    public static final String MIGRATE_TIMEOUT = "migrateTimeoutMs";
    public static final String DELETE_TIMEOUT = "deleteTimeoutMs";

    public static final Map<DeployState, String> MAP_TIMEOUT = Map.of(DeployState.DEPLOYING, DEPLOY_TIMEOUT,
            DeployState.UNDEPLOYING, UNDEPLOY_TIMEOUT,
            DeployState.UPDATING, UPDATE_TIMEOUT,
            DeployState.MIGRATING, MIGRATE_TIMEOUT,
            DeployState.DELETING, DELETE_TIMEOUT);

    /**
     * Get the First StartPhase.
     *
     * <p>This depends on the state of the automation composition and also on all start phases defined in the
     * ToscaServiceTemplate.
     *
     * @param automationComposition the automation composition
     * @param toscaServiceTemplate the ToscaServiceTemplate
     * @return the First StartPhase
     */
    public static int getFirstStartPhase(
        AutomationComposition automationComposition, ToscaServiceTemplate toscaServiceTemplate) {
        var minStartPhase = 1000;
        var maxStartPhase = 0;
        for (var element : automationComposition.getElements().values()) {
            var toscaNodeTemplate = toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates()
                .get(element.getDefinition().getName());
            int startPhase = toscaNodeTemplate != null
                    && element.getDefinition().getVersion().equals(toscaNodeTemplate.getVersion())
                    ? ParticipantUtils.findStartPhase(toscaNodeTemplate.getProperties()) : 0;
            minStartPhase = Math.min(minStartPhase, startPhase);
            maxStartPhase = Math.max(maxStartPhase, startPhase);
        }

        return DeployState.DEPLOYING.equals(automationComposition.getDeployState())
            || LockState.UNLOCKING.equals(automationComposition.getLockState()) ? minStartPhase : maxStartPhase;
    }

    /**
     * Get the First Stage.
     *
     * @param automationComposition the automation composition
     * @param toscaServiceTemplate the ToscaServiceTemplate
     * @return the First stage
     */
    public static int getFirstStage(AutomationComposition automationComposition,
            ToscaServiceTemplate toscaServiceTemplate) {
        Set<Integer> minStage = new HashSet<>();
        for (var element : automationComposition.getElements().values()) {
            var toscaNodeTemplate = toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates()
                .get(element.getDefinition().getName());
            var stage = DeployState.MIGRATING.equals(automationComposition.getDeployState())
                    ? ParticipantUtils.findStageSetMigrate(toscaNodeTemplate.getProperties())
                    : ParticipantUtils.findStageSetPrepare(toscaNodeTemplate.getProperties());
            minStage.addAll(stage);
        }
        return minStage.stream().min(Integer::compare).orElse(0);
    }

    /**
     * Finds startPhase from a map of properties.
     *
     * @param properties Map of properties
     * @return startPhase
     */
    public static int findStartPhase(Map<String, Object> properties) {
        var objStartPhase = properties.get("startPhase");
        if (objStartPhase != null) {
            return Integer.parseInt(objStartPhase.toString());
        }
        return 0;
    }

    /**
     * Finds stage from a map of properties for Prepare.
     *
     * @param properties Map of properties
     * @return stage
     */
    public static Set<Integer> findStageSetPrepare(Map<String, Object> properties) {
        var objStage = properties.get("stage");
        if (objStage instanceof Map<?, ?>) {
            objStage = ((Map<?, ?>) objStage).get(STAGE_PREPARE);
            return findStageSet(objStage);
        }
        return Set.of(0);
    }

    /**
     * Get timeout value from properties by name operation, return default value if not present.
     *
     * @param properties instance properties
     * @param name the operation name
     * @param defaultValue the default value
     * @return the timeout value
     */
    public static long getTimeout(Map<String, Object> properties, String name, long defaultValue) {
        var objTimeout = properties.get(name);
        if (objTimeout != null) {
            return Long.parseLong(objTimeout.toString());
        }
        return defaultValue;
    }

    /**
     * Get operation name of a composition definition.
     *
     * @param state the state of the composition definition
     * @return the operation name
     */
    public static String getOpName(AcTypeState state) {
        return AcTypeState.PRIMING.equals(state) ? PRIME_TIMEOUT : DEPRIME_TIMEOUT;
    }

    /**
     * Get operation name of a AutomationComposition.
     *
     * @param deployState the state of the AutomationComposition
     * @return the operation name
     */
    public static String getOpName(DeployState deployState) {
        return MAP_TIMEOUT.getOrDefault(deployState, DEFAULT_TIMEOUT);
    }

    /**
     * Finds stage from a map of properties for Migrate.
     *
     * @param properties Map of properties
     * @return stage
     */
    public static Set<Integer> findStageSetMigrate(Map<String, Object> properties) {
        var objStage = properties.get("stage");
        if (objStage instanceof Map<?, ?>) {
            objStage = ((Map<?, ?>) objStage).get(STAGE_MIGRATE);
        }
        return findStageSet(objStage);
    }

    private static Set<Integer> findStageSet(Object objStage) {
        if (objStage instanceof List<?> stageSet) {
            return stageSet.stream()
                    .map(obj -> Integer.valueOf(obj.toString()))
                    .collect(Collectors.toSet());
        }
        return Set.of(0);
    }
}
