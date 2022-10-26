/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021,2022 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.policy.main.handler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import lombok.Setter;
import org.apache.http.HttpStatus;
import org.onap.policy.clamp.acm.participant.intermediary.api.AutomationCompositionElementListener;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.acm.participant.policy.client.PolicyApiHttpClient;
import org.onap.policy.clamp.acm.participant.policy.client.PolicyPapHttpClient;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.pdp.concepts.DeploymentSubGroup;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class handles implementation of automationCompositionElement updates.
 */
@Component
public class AutomationCompositionElementHandler implements AutomationCompositionElementListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutomationCompositionElementHandler.class);
    private final Map<String, String> policyTypeMap = new LinkedHashMap<>();
    private final Map<String, String> policyMap = new LinkedHashMap<>();

    private final PolicyApiHttpClient apiHttpClient;
    private final PolicyPapHttpClient papHttpClient;

    @Setter
    private ParticipantIntermediaryApi intermediaryApi;

    /**
     * constructor.
     *
     * @param apiHttpClient the Policy Api Http Client
     * @param papHttpClient the Policy Pap Http Client
     */
    public AutomationCompositionElementHandler(PolicyApiHttpClient apiHttpClient, PolicyPapHttpClient papHttpClient) {
        this.papHttpClient = papHttpClient;
        this.apiHttpClient = apiHttpClient;
    }

    /**
     * Callback method to handle a automation composition element state change.
     *
     * @param automationCompositionId        the ID of the automation composition
     * @param automationCompositionElementId the ID of the automation composition element
     * @param currentState                   the current state of the automation composition element
     * @param orderedState                   the state to which the automation composition element is changing to
     */
    @Override
    public void automationCompositionElementStateChange(ToscaConceptIdentifier automationCompositionId,
                                                        UUID automationCompositionElementId,
                                                        AutomationCompositionState currentState,
                                                        AutomationCompositionOrderedState orderedState) {
        switch (orderedState) {
            case UNINITIALISED:
                try {
                    undeployPolicies(automationCompositionElementId);
                    deletePolicyData(automationCompositionId, automationCompositionElementId, orderedState);
                    intermediaryApi.updateAutomationCompositionElementState(automationCompositionId,
                        automationCompositionElementId, orderedState, AutomationCompositionState.UNINITIALISED,
                        ParticipantMessageType.AUTOMATION_COMPOSITION_STATE_CHANGE);
                } catch (PfModelRuntimeException e) {
                    LOGGER.error("Undeploying/Deleting policy failed {}", automationCompositionElementId, e);
                }
                break;
            case PASSIVE:
                try {
                    undeployPolicies(automationCompositionElementId);
                } catch (PfModelRuntimeException e) {
                    LOGGER.error("Undeploying policies failed - no policies to undeploy {}",
                        automationCompositionElementId);
                }
                intermediaryApi.updateAutomationCompositionElementState(automationCompositionId,
                    automationCompositionElementId, orderedState, AutomationCompositionState.PASSIVE,
                    ParticipantMessageType.AUTOMATION_COMPOSITION_STATE_CHANGE);
                break;
            case RUNNING:
                LOGGER.info("Running state is not supported");
                break;
            default:
                LOGGER.debug("Unknown orderedstate {}", orderedState);
                break;
        }
    }

    private void deletePolicyData(ToscaConceptIdentifier automationCompositionId,
                                  UUID automationCompositionElementId, AutomationCompositionOrderedState newState) {
        // Delete all policies of this automationComposition from policy framework
        for (Entry<String, String> policy : policyMap.entrySet()) {
            apiHttpClient.deletePolicy(policy.getKey(), policy.getValue());
        }
        policyMap.clear();
        // Delete all policy types of this automation composition from policy framework
        for (Entry<String, String> policyType : policyTypeMap.entrySet()) {
            apiHttpClient.deletePolicyType(policyType.getKey(), policyType.getValue());
        }
        policyTypeMap.clear();
    }

    private void deployPolicies(ToscaConceptIdentifier automationCompositionId, UUID automationCompositionElementId,
                                AutomationCompositionOrderedState newState) {
        var deployFailure = false;
        // Deploy all policies of this automationComposition from Policy Framework
        if (!policyMap.entrySet().isEmpty()) {
            for (Entry<String, String> policy : policyMap.entrySet()) {
                var deployPolicyResp = papHttpClient.handlePolicyDeployOrUndeploy(policy.getKey(), policy.getValue(),
                        DeploymentSubGroup.Action.POST).getStatus();
                if (deployPolicyResp != HttpStatus.SC_ACCEPTED) {
                    deployFailure = true;
                }
            }
            LOGGER.info("Policies deployed to {} successfully", automationCompositionElementId);
        } else {
            LOGGER.debug("No policies to deploy to {}", automationCompositionElementId);
        }
        if (! deployFailure) {
            // Update the AC element state
            intermediaryApi.updateAutomationCompositionElementState(automationCompositionId,
                    automationCompositionElementId, newState, AutomationCompositionState.PASSIVE,
                    ParticipantMessageType.AUTOMATION_COMPOSITION_STATE_CHANGE);
        }
    }

    private void undeployPolicies(UUID automationCompositionElementId) {
        // Undeploy all policies of this automation composition from Policy Framework
        if (!policyMap.entrySet().isEmpty()) {
            for (Entry<String, String> policy : policyMap.entrySet()) {
                papHttpClient.handlePolicyDeployOrUndeploy(policy.getKey(), policy.getValue(),
                    DeploymentSubGroup.Action.DELETE);
            }
            LOGGER.debug("Undeployed policies from {} successfully", automationCompositionElementId);
        } else {
            LOGGER.debug("No policies are deployed to {}", automationCompositionElementId);
        }
    }

    /**
     * Callback method to handle an update on automation composition element.
     *
     * @param element the information on the automation composition element
     * @param acElementDefinition toscaNodeTemplate
     * @throws PfModelException in case of an exception
     */
    @Override
    public void automationCompositionElementUpdate(ToscaConceptIdentifier automationCompositionId,
                                                   AutomationCompositionElement element,
                                                   ToscaNodeTemplate acElementDefinition)
        throws PfModelException {
        var createPolicyTypeResp = HttpStatus.SC_OK;
        var createPolicyResp = HttpStatus.SC_OK;

        ToscaServiceTemplate automationCompositionDefinition = element.getToscaServiceTemplateFragment();
        if (automationCompositionDefinition.getToscaTopologyTemplate() != null) {
            if (automationCompositionDefinition.getPolicyTypes() != null) {
                for (ToscaPolicyType policyType : automationCompositionDefinition.getPolicyTypes().values()) {
                    policyTypeMap.put(policyType.getName(), policyType.getVersion());
                }
                LOGGER.info("Found Policy Types in automation composition definition: {} , Creating Policy Types",
                    automationCompositionDefinition.getName());
                createPolicyTypeResp = apiHttpClient.createPolicyType(automationCompositionDefinition).getStatus();
            }
            if (automationCompositionDefinition.getToscaTopologyTemplate().getPolicies() != null) {
                for (Map<String, ToscaPolicy> gotPolicyMap : automationCompositionDefinition.getToscaTopologyTemplate()
                    .getPolicies()) {
                    for (ToscaPolicy policy : gotPolicyMap.values()) {
                        policyMap.put(policy.getName(), policy.getVersion());
                    }
                }
                LOGGER.info("Found Policies in automation composition definition: {} , Creating Policies",
                    automationCompositionDefinition.getName());
                createPolicyResp = apiHttpClient.createPolicy(automationCompositionDefinition).getStatus();
            }
            if (createPolicyTypeResp == HttpStatus.SC_OK && createPolicyResp == HttpStatus.SC_OK) {
                LOGGER.info("PolicyTypes/Policies for the automation composition element : {} are created "
                        + "successfully", element.getId());
                deployPolicies(automationCompositionId, element.getId(), element.getOrderedState());
            } else {
                LOGGER.error("Creation of PolicyTypes/Policies failed. Policies will not be deployed.");
            }
        }
    }
}