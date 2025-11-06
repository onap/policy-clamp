/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.models.acm.utils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AcmStageUtils {
    private static final String STAGE_MIGRATE = "migrate";
    private static final String STAGE_PREPARE = "prepare";
    private static final int MAX_STAGE = 1000;

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
        var minStartPhase = MAX_STAGE;
        var maxStartPhase = 0;
        for (var element : automationComposition.getElements().values()) {
            var toscaNodeTemplate = toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates()
                    .get(element.getDefinition().getName());
            int startPhase = toscaNodeTemplate != null
                    && element.getDefinition().getVersion().equals(toscaNodeTemplate.getVersion())
                    ? findStartPhase(toscaNodeTemplate.getProperties()) : 0;
            minStartPhase = Math.min(minStartPhase, startPhase);
            maxStartPhase = Math.max(maxStartPhase, startPhase);
        }

        return AcmStateUtils.isForward(automationComposition.getDeployState(), automationComposition.getLockState())
                ? minStartPhase : maxStartPhase;
    }

    /**
     * Get the First Stage from AutomationComposition.
     *
     * @param automationComposition the automation composition
     * @param toscaServiceTemplate the ToscaServiceTemplate
     * @return the First stage
     */
    public static int getFirstStage(AutomationComposition automationComposition,
            ToscaServiceTemplate toscaServiceTemplate) {
        var stages = automationComposition.getElements().values().stream()
                .map(element -> getFirstStage(element, toscaServiceTemplate));
        return stages.min(Integer::compare).orElse(0);
    }

    /**
     * Get the First Stage from AutomationCompositionElement.
     *
     * @param element the automation composition element
     * @param toscaServiceTemplate the ToscaServiceTemplate
     * @return the First stage
     */
    public static int getFirstStage(AutomationCompositionElement element, ToscaServiceTemplate toscaServiceTemplate) {
        var toscaNodeTemplate = toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates()
                .get(element.getDefinition().getName());
        if (toscaNodeTemplate == null) {
            return 0;
        }
        return getFirstStage(element.getDeployState(), toscaNodeTemplate.getProperties());
    }

    /**
     * Get the First Stage.
     *
     * @param deployState the DeployState
     * @param properties Map of properties
     * @return the First stage
     */
    public static int getFirstStage(DeployState deployState, Map<String, Object> properties) {
        var stageSet = AcmStateUtils.isMigrating(deployState)
                ? findStageSetMigrate(properties)
                : findStageSetPrepare(properties);
        return stageSet.stream().min(Integer::compare).orElse(0);
    }

    /**
     * Get the Last Stage from AutomationComposition.
     *
     * @param automationComposition the automation composition
     * @param toscaServiceTemplate the ToscaServiceTemplate
     * @return the Last stage
     */
    public static int getLastStage(AutomationComposition automationComposition,
            ToscaServiceTemplate toscaServiceTemplate) {
        var stages = automationComposition.getElements().values().stream()
                .map(element -> getLastStage(element, toscaServiceTemplate, 0));
        return stages.max(Integer::compare).orElse(0);
    }

    /**
     * Get the Last Stage from AutomationCompositionElement.
     *
     * @param element the automation composition element
     * @param toscaServiceTemplate the ToscaServiceTemplate
     * @param defaultValue default Value is not present
     * @return the Last stage
     */
    public static int getLastStage(
            AutomationCompositionElement element, ToscaServiceTemplate toscaServiceTemplate, int defaultValue) {
        var toscaNodeTemplate = toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates()
                .get(element.getDefinition().getName());
        if (toscaNodeTemplate == null) {
            return defaultValue;
        }
        return getLastStage(toscaNodeTemplate.getProperties(), defaultValue);
    }

    /**
     * Get the Last Stage.
     *
     * @param properties Map of properties
     * @param defaultValue default Value is not present
     * @return the Last stage
     */
    public static int getLastStage(Map<String, Object> properties, int defaultValue) {
        var stageSet = findStageSetMigrate(properties);
        return stageSet.stream().max(Integer::compare).orElse(defaultValue);
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
