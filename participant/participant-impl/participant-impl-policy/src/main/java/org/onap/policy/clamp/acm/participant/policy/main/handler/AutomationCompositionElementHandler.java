/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation.
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

import jakarta.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.hc.core5.http.HttpStatus;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.InstanceElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.acm.participant.intermediary.api.impl.AcElementListenerV2;
import org.onap.policy.clamp.acm.participant.policy.client.PolicyApiHttpClient;
import org.onap.policy.clamp.acm.participant.policy.client.PolicyPapHttpClient;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.DeploymentSubGroup;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class handles implementation of automationCompositionElement updates.
 */
@Component
public class AutomationCompositionElementHandler extends AcElementListenerV2 {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutomationCompositionElementHandler.class);
    private static final Coder CODER = new StandardCoder();

    private final PolicyApiHttpClient apiHttpClient;
    private final PolicyPapHttpClient papHttpClient;

    /**
     * Constructor.
     *
     * @param apiHttpClient the PolicyApi Http Client
     * @param papHttpClient the Policy Pap Http Client
     * @param intermediaryApi the Participant Intermediary Api
     */
    public AutomationCompositionElementHandler(PolicyApiHttpClient apiHttpClient, PolicyPapHttpClient papHttpClient,
        ParticipantIntermediaryApi intermediaryApi) {
        super(intermediaryApi);
        this.apiHttpClient = apiHttpClient;
        this.papHttpClient = papHttpClient;
    }

    /**
     * Callback method to handle a automation composition element state change.
     *
     * @param compositionElement the information of the Automation Composition Definition Element
     * @param instanceElement the information of the Automation Composition Instance Element
     * @throws PfModelException in case of a model exception
     */
    @Override
    public void undeploy(CompositionElementDto compositionElement, InstanceElementDto instanceElement)
            throws PfModelException {
        var automationCompositionDefinition = getToscaServiceTemplate(instanceElement.inProperties());
        if (automationCompositionDefinition.getToscaTopologyTemplate() == null) {
            LOGGER.debug("No policies to undeploy to {}", instanceElement.elementId());
            intermediaryApi.updateAutomationCompositionElementState(instanceElement.instanceId(),
                    instanceElement.elementId(), DeployState.UNDEPLOYED, null, StateChangeResult.NO_ERROR,
                    "Undeployed");
            return;
        }
        var policyList = getPolicyList(automationCompositionDefinition);
        undeployPolicies(policyList, instanceElement.elementId());
        var policyTypeList = getPolicyTypeList(automationCompositionDefinition);
        deletePolicyData(policyTypeList, policyList);
        intermediaryApi.updateAutomationCompositionElementState(instanceElement.instanceId(),
                instanceElement.elementId(), DeployState.UNDEPLOYED, null, StateChangeResult.NO_ERROR,
                "Undeployed");
    }

    private void deletePolicyData(List<ToscaConceptIdentifier> policyTypeList,
            List<ToscaConceptIdentifier> policyList) {
        // Delete all policies of this automationComposition from policy framework
        for (var policy : policyList) {
            apiHttpClient.deletePolicy(policy.getName(), policy.getVersion());
        }
        // Delete all policy types of this automation composition from policy framework
        for (var policyType : policyTypeList) {
            apiHttpClient.deletePolicyType(policyType.getName(), policyType.getVersion());
        }
    }

    private void deployPolicies(List<ToscaConceptIdentifier> policyList, UUID automationCompositionId,
            UUID automationCompositionElementId) throws PfModelException {
        var deployFailure = false;
        // Deploy all policies of this automationComposition from Policy Framework
        if (!policyList.isEmpty()) {
            for (var policy : policyList) {
                var deployPolicyResp = papHttpClient.handlePolicyDeployOrUndeploy(policy.getName(), policy.getVersion(),
                        DeploymentSubGroup.Action.POST).getStatus();
                if (deployPolicyResp != HttpStatus.SC_ACCEPTED) {
                    deployFailure = true;
                }
            }
            LOGGER.info("Policies deployed to {} successfully", automationCompositionElementId);
        } else {
            LOGGER.debug("No policies to deploy to {}", automationCompositionElementId);
        }
        if (!deployFailure) {
            // Update the AC element state
            intermediaryApi.sendAcElementInfo(automationCompositionId, automationCompositionElementId, "IDLE",
                    "ENABLED", Map.of());
            intermediaryApi.updateAutomationCompositionElementState(automationCompositionId,
                    automationCompositionElementId, DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Deployed");
        } else {
            throw new PfModelException(Status.BAD_REQUEST, "Deploy of Policy failed.");
        }
    }

    private void undeployPolicies(List<ToscaConceptIdentifier> policyList, UUID automationCompositionElementId) {
        // Undeploy all policies of this automation composition from Policy Framework
        if (!policyList.isEmpty()) {
            for (var policy : policyList) {
                papHttpClient.handlePolicyDeployOrUndeploy(policy.getName(), policy.getVersion(),
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
     * @param compositionElement the information of the Automation Composition Definition Element
     * @param instanceElement the information of the Automation Composition Instance Element
     * @throws PfModelException from Policy framework
     */
    @Override
    public void deploy(CompositionElementDto compositionElement, InstanceElementDto instanceElement)
            throws PfModelException {
        var createPolicyTypeResp = HttpStatus.SC_OK;
        var createPolicyResp = HttpStatus.SC_OK;

        var automationCompositionDefinition = getToscaServiceTemplate(instanceElement.inProperties());
        if (automationCompositionDefinition.getToscaTopologyTemplate() == null) {
            intermediaryApi.updateAutomationCompositionElementState(instanceElement.instanceId(),
                    instanceElement.elementId(), DeployState.UNDEPLOYED, null, StateChangeResult.FAILED,
                    "ToscaTopologyTemplate not defined");
            return;
        }
        if (automationCompositionDefinition.getPolicyTypes() != null) {
            LOGGER.info("Found Policy Types in automation composition definition: {} , Creating Policy Types",
                    automationCompositionDefinition.getName());
            try (var response = apiHttpClient.createPolicyType(automationCompositionDefinition)) {
                createPolicyTypeResp = response.getStatus();
            }
        }
        if (automationCompositionDefinition.getToscaTopologyTemplate().getPolicies() != null) {
            LOGGER.info("Found Policies in automation composition definition: {} , Creating Policies",
                    automationCompositionDefinition.getName());
            try (var response = apiHttpClient.createPolicy(automationCompositionDefinition)) {
                createPolicyResp = response.getStatus();
            }
        }
        if (isSuccess(createPolicyTypeResp) && isSuccess(createPolicyResp)) {
            LOGGER.info(
                    "PolicyTypes/Policies for the automation composition element : {} are created " + "successfully",
                    instanceElement.elementId());
            var policyList = getPolicyList(automationCompositionDefinition);
            deployPolicies(policyList, instanceElement.instanceId(), instanceElement.elementId());
        } else {
            intermediaryApi.updateAutomationCompositionElementState(instanceElement.instanceId(),
                    instanceElement.elementId(), DeployState.UNDEPLOYED, null, StateChangeResult.FAILED,
                    "Creation of PolicyTypes/Policies failed. Policies will not be deployed.");
        }
    }

    private boolean isSuccess(int status) {
        return status == HttpStatus.SC_OK || status == HttpStatus.SC_CREATED;
    }

    private List<ToscaConceptIdentifier> getPolicyTypeList(ToscaServiceTemplate serviceTemplate) {
        List<ToscaConceptIdentifier> policyTypeList = new ArrayList<>();
        if (serviceTemplate.getPolicyTypes() != null) {
            for (var policyType : serviceTemplate.getPolicyTypes().values()) {
                policyTypeList.add(policyType.getKey().asIdentifier());
            }
        }

        return policyTypeList;
    }

    private List<ToscaConceptIdentifier> getPolicyList(ToscaServiceTemplate serviceTemplate) {
        List<ToscaConceptIdentifier> policyList = new ArrayList<>();
        if (serviceTemplate.getToscaTopologyTemplate().getPolicies() != null) {
            for (var gotPolicyMap : serviceTemplate.getToscaTopologyTemplate().getPolicies()) {
                for (var policy : gotPolicyMap.values()) {
                    policyList.add(policy.getKey().asIdentifier());
                }
            }
        }

        return policyList;
    }

    private ToscaServiceTemplate getToscaServiceTemplate(Map<String, Object> properties) throws PfModelException {
        try {
            return  CODER.convert(properties, ToscaServiceTemplate.class);
        } catch (CoderException e) {
            throw new PfModelException(Status.BAD_REQUEST, e.getMessage());
        }
    }
}
