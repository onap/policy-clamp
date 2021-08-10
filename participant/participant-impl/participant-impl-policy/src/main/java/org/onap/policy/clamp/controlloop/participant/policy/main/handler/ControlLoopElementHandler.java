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
import lombok.Setter;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ClElementStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ControlLoopElementListener;
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.controlloop.participant.policy.client.PolicyApiHttpClient;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class handles implementation of controlLoopElement updates.
 */
@Component
public class ControlLoopElementHandler implements ControlLoopElementListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControlLoopElementHandler.class);
    private final Map<String, String> policyTypeMap = new LinkedHashMap<>();
    private final Map<String, String> policyMap = new LinkedHashMap<>();

    private final PolicyApiHttpClient apiHttpClient;

    @Setter
    private ParticipantIntermediaryApi intermediaryApi;

    /**
     * constructor.
     *
     * @param apiHttpClient the Policy Api Http Client
     */
    public ControlLoopElementHandler(PolicyApiHttpClient apiHttpClient) {
        this.apiHttpClient = apiHttpClient;
    }

    /**
     * Callback method to handle a control loop element state change.
     *
     * @param controlLoopElementId the ID of the control loop element
     * @param currentState the current state of the control loop element
     * @param newState the state to which the control loop element is changing to
     * @throws PfModelException in case of an exception
     */
    @Override
    public void controlLoopElementStateChange(UUID controlLoopElementId, ControlLoopState currentState,
            ControlLoopOrderedState newState) throws PfModelException {
        switch (newState) {
            case UNINITIALISED:
                try {
                    deletePolicyData(controlLoopElementId, newState);
                } catch (PfModelRuntimeException e) {
                    LOGGER.debug("Deleting policy data failed", e);
                }
                break;
            case PASSIVE:
                intermediaryApi.updateControlLoopElementState(controlLoopElementId, newState, ControlLoopState.PASSIVE);
                break;
            case RUNNING:
                intermediaryApi.updateControlLoopElementState(controlLoopElementId, newState, ControlLoopState.RUNNING);
                break;
            default:
                LOGGER.debug("Unknown orderedstate {}", newState);
                break;
        }
    }

    private void deletePolicyData(UUID controlLoopElementId, ControlLoopOrderedState newState) throws PfModelException {
        // Delete all policies of this controlLoop from policy framework
        for (Entry<String, String> policy : policyMap.entrySet()) {
            apiHttpClient.deletePolicy(policy.getKey(), policy.getValue());
        }
        policyMap.clear();
        // Delete all policy types of this control loop from policy framework
        for (Entry<String, String> policyType : policyTypeMap.entrySet()) {
            apiHttpClient.deletePolicyType(policyType.getKey(), policyType.getValue());
        }
        policyTypeMap.clear();
        intermediaryApi.updateControlLoopElementState(controlLoopElementId, newState, ControlLoopState.UNINITIALISED);
    }

    /**
     * Callback method to handle an update on a control loop element.
     *
     * @param element the information on the control loop element
     * @param clElementDefinition toscaNodeTemplate
     * @throws PfModelException in case of an exception
     */
    @Override
    public void controlLoopElementUpdate(ControlLoopElement element, ToscaNodeTemplate clElementDefinition)
            throws PfModelException {
        intermediaryApi.updateControlLoopElementState(element.getId(), element.getOrderedState(),
                ControlLoopState.PASSIVE);
        ToscaServiceTemplate controlLoopDefinition = intermediaryApi.getToscaServiceTemplate();
        if (controlLoopDefinition.getToscaTopologyTemplate() != null) {
            if (controlLoopDefinition.getPolicyTypes() != null) {
                for (ToscaPolicyType policyType : controlLoopDefinition.getPolicyTypes().values()) {
                    policyTypeMap.put(policyType.getName(), policyType.getVersion());
                }
                LOGGER.debug("Found Policy Types in control loop definition: {} , Creating Policy Types",
                        controlLoopDefinition.getName());
                apiHttpClient.createPolicyType(controlLoopDefinition);
            }
            if (controlLoopDefinition.getToscaTopologyTemplate().getPolicies() != null) {
                for (Map<String, ToscaPolicy> foundPolicyMap : controlLoopDefinition.getToscaTopologyTemplate()
                        .getPolicies()) {
                    for (ToscaPolicy policy : foundPolicyMap.values()) {
                        policyMap.put(policy.getName(), policy.getVersion());
                    }
                }
                LOGGER.debug("Found Policies in control loop definition: {} , Creating Policies",
                        controlLoopDefinition.getName());
                apiHttpClient.createPolicy(controlLoopDefinition);
            }
        }
    }

    /**
     * Handle controlLoopElement statistics.
     *
     * @param controlLoopElementId controlloop element id
     */
    @Override
    public void handleStatistics(UUID controlLoopElementId) throws PfModelException {
        ControlLoopElement clElement = intermediaryApi.getControlLoopElement(controlLoopElementId);
        if (clElement != null) {
            ClElementStatistics clElementStatistics = new ClElementStatistics();
            clElementStatistics.setControlLoopState(clElement.getState());
            clElementStatistics.setTimeStamp(Instant.now());
            intermediaryApi.updateControlLoopElementStatistics(controlLoopElementId, clElementStatistics);
        }
    }
}
