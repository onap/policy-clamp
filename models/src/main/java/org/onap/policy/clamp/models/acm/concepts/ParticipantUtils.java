/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation.
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
    public static int getFirstStage(
        AutomationComposition automationComposition, ToscaServiceTemplate toscaServiceTemplate) {
        Set<Integer> minStage = new HashSet<>();
        for (var element : automationComposition.getElements().values()) {
            var toscaNodeTemplate = toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates()
                .get(element.getDefinition().getName());
            var stage = ParticipantUtils.findStageSet(toscaNodeTemplate.getProperties());
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
     * Finds stage from a map of properties.
     *
     * @param properties Map of properties
     * @return stage
     */
    public static Set<Integer> findStageSet(Map<String, Object> properties) {
        var objStage = properties.get("stage");
        if (objStage instanceof List<?> stageSet) {
            return stageSet.stream()
                .map(obj -> Integer.valueOf(obj.toString()))
                .collect(Collectors.toSet());
        }
        return Set.of(0);
    }
}
