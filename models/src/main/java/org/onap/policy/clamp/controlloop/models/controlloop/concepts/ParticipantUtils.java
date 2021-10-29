/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.models.controlloop.concepts;

import java.util.Map;
import javax.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ParticipantUtils {

    private static final Coder CODER = new StandardCoder();
    private static final String CONTROL_LOOP_ELEMENT = "org.onap.policy.clamp.controlloop.ControlLoopElement";

    /**
     * Finds participantType from a map of properties.
     *
     * @param properties Map of properties
     * @return participantType
     */
    public static ToscaConceptIdentifier findParticipantType(Map<String, Object> properties) {
        var objParticipantType = properties.get("participantType");
        if (objParticipantType != null) {
            try {
                return CODER.decode(objParticipantType.toString(), ToscaConceptIdentifier.class);
            } catch (CoderException e) {
                throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * Get the First StartPhase
     * it depend of the state of the Control Loop
     * and also from the all startPhase defined into the ToscaServiceTemplate.
     * @param controlLoop the ControlLoop
     * @param toscaServiceTemplate the ToscaServiceTemplate
     * @return the First StartPhase
     */
    public static int getFirstStartPhase(ControlLoop controlLoop, ToscaServiceTemplate toscaServiceTemplate) {
        var minStartPhase = 1000;
        var maxStartPhase = 0;
        for (var element : controlLoop.getElements().values()) {
            ToscaNodeTemplate toscaNodeTemplate = toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates()
                    .get(element.getDefinition().getName());
            int startPhase = ParticipantUtils.findStartPhase(toscaNodeTemplate.getProperties());
            minStartPhase = Math.min(minStartPhase, startPhase);
            maxStartPhase = Math.max(maxStartPhase, startPhase);
        }

        return ControlLoopState.UNINITIALISED2PASSIVE.equals(controlLoop.getState())
                || ControlLoopState.PASSIVE2RUNNING.equals(controlLoop.getState()) ? minStartPhase
                        : maxStartPhase;
    }

    /**
     * Finds startPhase from a map of properties.
     *
     * @param properties Map of properties
     * @return startPhase
     */
    public static int findStartPhase(Map<String, Object> properties) {
        var objParticipantType = properties.get("startPhase");
        if (objParticipantType != null) {
            return Integer.valueOf(objParticipantType.toString());
        }
        return 0;
    }

    /**
     * Checks If NodeTemplate Is ControlLoopElement.
     *
     * @param nodeTemplate the ToscaNodeTemplate
     * @param toscaServiceTemplate the ToscaServiceTemplate
     * @return true if NodeTemplate Is ControlLoopElement
     */
    public static boolean checkIfNodeTemplateIsControlLoopElement(ToscaNodeTemplate nodeTemplate,
            ToscaServiceTemplate toscaServiceTemplate) {
        if (nodeTemplate.getType().contains(CONTROL_LOOP_ELEMENT)) {
            return true;
        } else {
            var nodeType = toscaServiceTemplate.getNodeTypes().get(nodeTemplate.getType());
            if (nodeType != null) {
                var derivedFrom = nodeType.getDerivedFrom();
                if (derivedFrom != null) {
                    return derivedFrom.contains(CONTROL_LOOP_ELEMENT);
                }
            }
        }
        return false;
    }
}
