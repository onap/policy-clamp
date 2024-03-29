/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation.
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

import java.util.Map;
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
            int startPhase = ParticipantUtils.findStartPhase(toscaNodeTemplate.getProperties());
            minStartPhase = Math.min(minStartPhase, startPhase);
            maxStartPhase = Math.max(maxStartPhase, startPhase);
        }

        return DeployState.DEPLOYING.equals(automationComposition.getDeployState())
            || LockState.UNLOCKING.equals(automationComposition.getLockState()) ? minStartPhase
                : maxStartPhase;
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
            return Integer.valueOf(objStartPhase.toString());
        }
        return 0;
    }
}
