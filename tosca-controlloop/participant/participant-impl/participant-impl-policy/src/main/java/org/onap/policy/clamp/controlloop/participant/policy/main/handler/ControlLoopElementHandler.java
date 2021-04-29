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

package org.onap.policy.clamp.controlloop.participant.policy.main.handler;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ClElementStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ControlLoopElementListener;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles implementation of controlLoopElement updates.
 */
public class ControlLoopElementHandler implements ControlLoopElementListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControlLoopElementHandler.class);
    private static final Map<String, String> policyTypeMap = new LinkedHashMap<>();
    private static final Map<String, String> policyMap = new LinkedHashMap<>();

    /**
     * Callback method to handle a control loop element state change.
     *
     * @param controlLoopElementId the ID of the control loop element
     * @param currentState the current state of the control loop element
     * @param newState the state to which the control loop element is changing to
     * @throws PfModelException in case of an exception
    */
    @Override
    public void controlLoopElementStateChange(UUID controlLoopElementId,
            ControlLoopState currentState,
            ControlLoopOrderedState newState) throws PfModelException {
        switch (newState) {
            case UNINITIALISED:
                try {
                    deletePolicyData(controlLoopElementId, newState);
                } catch (PfModelRuntimeException e) {
                    LOGGER.debug("Delete policytpes failed", e);
                }
                break;
            case PASSIVE:
                PolicyHandler.getInstance().getPolicyProvider().getIntermediaryApi()
                    .updateControlLoopElementState(controlLoopElementId, newState,
                            ControlLoopState.PASSIVE);
                break;
            case RUNNING:
                PolicyHandler.getInstance().getPolicyProvider().getIntermediaryApi()
                    .updateControlLoopElementState(controlLoopElementId, newState,
                            ControlLoopState.RUNNING);
                break;
            default:
                LOGGER.debug("Unknown orderedstate {}", newState);
                break;
        }
    }

    private void deletePolicyData(UUID controlLoopElementId,
            ControlLoopOrderedState newState) throws PfModelException {
        if (policyMap != null) {
            // Delete all policies of this controlLoop from policy framework
            for (Entry<String, String> policy : policyMap.entrySet()) {
                PolicyHandler.getInstance().getDatabaseProvider().deletePolicy(
                       policy.getKey(), policy.getValue());
            }
        }
        if (policyTypeMap != null) {
            // Delete all policy types of this control loop from policy framework
            for (Entry<String, String> policy : policyTypeMap.entrySet()) {
                PolicyHandler.getInstance().getDatabaseProvider().deletePolicyType(
                        policy.getKey(), policy.getValue());
            }
        }
        PolicyHandler.getInstance().getPolicyProvider().getIntermediaryApi()
            .updateControlLoopElementState(controlLoopElementId, newState,
                    ControlLoopState.UNINITIALISED);
    }

    /**
     * Callback method to handle an update on a control loop element.
     *
     * @param element the information on the control loop element
     * @param controlLoopDefinition toscaServiceTemplate
     * @throws PfModelException in case of an exception
     */
    @Override
    public void controlLoopElementUpdate(ControlLoopElement element,
            ToscaServiceTemplate controlLoopDefinition) throws PfModelException {
        PolicyHandler.getInstance().getPolicyProvider().getIntermediaryApi()
            .updateControlLoopElementState(element.getId(), element.getOrderedState(), ControlLoopState.PASSIVE);
        if (controlLoopDefinition.getPolicyTypes() != null) {
            for (ToscaPolicyType policyType : controlLoopDefinition.getPolicyTypes().values()) {
                policyTypeMap.put(policyType.getName(), policyType.getVersion());
            }
            PolicyHandler.getInstance().getDatabaseProvider().createPolicyTypes(controlLoopDefinition);
        }
        if (controlLoopDefinition.getToscaTopologyTemplate().getPolicies() != null) {
            for (Map<String, ToscaPolicy> foundPolicyMap : controlLoopDefinition
                            .getToscaTopologyTemplate().getPolicies()) {
                for (Entry<String, ToscaPolicy> policyEntry : foundPolicyMap.entrySet()) {
                    ToscaPolicy policy = policyEntry.getValue();
                    policyMap.put(policy.getName(), policy.getVersion());
                }
            }
            PolicyHandler.getInstance().getDatabaseProvider().createPolicies(controlLoopDefinition);
        }
    }

    /**
     * Get controlLoopElement statistics.
     *
     * @param controlLoopElementId controlloop element id
     */
    @Override
    public void getClElementStatistics(UUID controlLoopElementId) {
        ControlLoopElement clElement = PolicyHandler.getInstance().getPolicyProvider()
                .getIntermediaryApi().getControlLoopElement(controlLoopElementId);
        if (clElement != null) {
            ClElementStatistics clElementStatistics = new ClElementStatistics();
            clElementStatistics.setControlLoopState(clElement.getState());
            clElementStatistics.setTimeStamp(Instant.now());
            PolicyHandler.getInstance().getPolicyProvider().getIntermediaryApi()
                .updateControlLoopElementStatistics(controlLoopElementId, clElementStatistics);
        }
    }
}
