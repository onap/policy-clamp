/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation.
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
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpStatus;
import org.onap.policy.clamp.acm.participant.intermediary.api.AutomationCompositionElementListener;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.acm.participant.policy.client.PolicyApiHttpClient;
import org.onap.policy.clamp.acm.participant.policy.client.PolicyPapHttpClient;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
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
@RequiredArgsConstructor
public class AutomationCompositionElementHandler implements AutomationCompositionElementListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutomationCompositionElementHandler.class);

    private final Map<UUID, ToscaServiceTemplate> serviceTemplateMap = new ConcurrentHashMap<>();

    private final PolicyApiHttpClient apiHttpClient;
    private final PolicyPapHttpClient papHttpClient;
    private final ParticipantIntermediaryApi intermediaryApi;

    /**
     * Callback method to handle a automation composition element state change.
     *
     * @param automationCompositionId the ID of the automation composition
     * @param automationCompositionElementId the ID of the automation composition element
     */
    @Override
    public void undeploy(UUID automationCompositionId, UUID automationCompositionElementId) throws PfModelException {
        var automationCompositionDefinition = serviceTemplateMap.get(automationCompositionElementId);
        if (automationCompositionDefinition == null) {
            LOGGER.debug("No policies to undeploy to {}", automationCompositionElementId);
            intermediaryApi.updateAutomationCompositionElementState(automationCompositionId,
                    automationCompositionElementId, DeployState.UNDEPLOYED, null, StateChangeResult.NO_ERROR,
                    "Undeployed");
            return;
        }
        var policyList = getPolicyList(automationCompositionDefinition);
        undeployPolicies(policyList, automationCompositionElementId);
        var policyTypeList = getPolicyTypeList(automationCompositionDefinition);
        deletePolicyData(policyTypeList, policyList);
        serviceTemplateMap.remove(automationCompositionElementId);
        intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, automationCompositionElementId,
                DeployState.UNDEPLOYED, null, StateChangeResult.NO_ERROR, "Undeployed");
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
     * @param automationCompositionId the automationComposition Id
     * @param element the information on the automation composition element
     * @param properties properties Map
     * @throws PfModelException in case of an exception
     */
    @Override
    public void deploy(UUID automationCompositionId, AcElementDeploy element, Map<String, Object> properties)
            throws PfModelException {
        var createPolicyTypeResp = HttpStatus.SC_OK;
        var createPolicyResp = HttpStatus.SC_OK;

        var automationCompositionDefinition = element.getToscaServiceTemplateFragment();
        if (automationCompositionDefinition.getToscaTopologyTemplate() == null) {
            intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, element.getId(),
                    DeployState.UNDEPLOYED, null, StateChangeResult.FAILED, "ToscaTopologyTemplate not defined");
            return;
        }
        serviceTemplateMap.put(element.getId(), automationCompositionDefinition);
        if (automationCompositionDefinition.getPolicyTypes() != null) {
            LOGGER.info("Found Policy Types in automation composition definition: {} , Creating Policy Types",
                    automationCompositionDefinition.getName());
            createPolicyTypeResp = apiHttpClient.createPolicyType(automationCompositionDefinition).getStatus();
        }
        if (automationCompositionDefinition.getToscaTopologyTemplate().getPolicies() != null) {
            LOGGER.info("Found Policies in automation composition definition: {} , Creating Policies",
                    automationCompositionDefinition.getName());
            createPolicyResp = apiHttpClient.createPolicy(automationCompositionDefinition).getStatus();
        }
        if (createPolicyTypeResp == HttpStatus.SC_OK && createPolicyResp == HttpStatus.SC_OK) {
            LOGGER.info(
                    "PolicyTypes/Policies for the automation composition element : {} are created " + "successfully",
                    element.getId());
            var policyList = getPolicyList(automationCompositionDefinition);
            deployPolicies(policyList, automationCompositionId, element.getId());
        } else {
            intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, element.getId(),
                    DeployState.UNDEPLOYED, null, StateChangeResult.FAILED,
                    "Creation of PolicyTypes/Policies failed. Policies will not be deployed.");
        }
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

    @Override
    public void lock(UUID instanceId, UUID elementId) throws PfModelException {
        intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId, null, LockState.LOCKED,
                StateChangeResult.NO_ERROR, "Locked");
    }

    @Override
    public void unlock(UUID instanceId, UUID elementId) throws PfModelException {
        intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId, null, LockState.UNLOCKED,
                StateChangeResult.NO_ERROR, "Unlocked");
    }

    @Override
    public void delete(UUID instanceId, UUID elementId) throws PfModelException {
        intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId, DeployState.DELETED, null,
                StateChangeResult.NO_ERROR, "Deleted");
    }

    @Override
    public void update(UUID instanceId, AcElementDeploy element, Map<String, Object> properties)
            throws PfModelException {
        intermediaryApi.updateAutomationCompositionElementState(instanceId, element.getId(), DeployState.DEPLOYED, null,
                StateChangeResult.NO_ERROR, "Update not supported");
    }

    @Override
    public void prime(UUID compositionId, List<AutomationCompositionElementDefinition> elementDefinitionList)
            throws PfModelException {
        intermediaryApi.updateCompositionState(compositionId, AcTypeState.PRIMED, StateChangeResult.NO_ERROR, "Primed");
    }

    @Override
    public void deprime(UUID compositionId) throws PfModelException {
        intermediaryApi.updateCompositionState(compositionId, AcTypeState.COMMISSIONED, StateChangeResult.NO_ERROR,
                "Deprimed");
    }

    @Override
    public void handleRestartComposition(UUID compositionId,
            List<AutomationCompositionElementDefinition> elementDefinitionList, AcTypeState state)
            throws PfModelException {
        var finalState = AcTypeState.PRIMED.equals(state) || AcTypeState.PRIMING.equals(state) ? AcTypeState.PRIMED
                : AcTypeState.COMMISSIONED;
        intermediaryApi.updateCompositionState(compositionId, finalState, StateChangeResult.NO_ERROR, "Restarted");
    }

    @Override
    public void handleRestartInstance(UUID automationCompositionId, AcElementDeploy element,
            Map<String, Object> properties, DeployState deployState, LockState lockState) throws PfModelException {
        if (DeployState.DEPLOYING.equals(deployState)) {
            deploy(automationCompositionId, element, properties);
            return;
        }
        if (DeployState.UNDEPLOYING.equals(deployState) || DeployState.DEPLOYED.equals(deployState)
                || DeployState.UPDATING.equals(deployState)) {
            var automationCompositionDefinition = element.getToscaServiceTemplateFragment();
            serviceTemplateMap.put(element.getId(), automationCompositionDefinition);
        }
        if (DeployState.UNDEPLOYING.equals(deployState)) {
            undeploy(automationCompositionId, element.getId());
            return;
        }
        deployState = AcmUtils.deployCompleted(deployState);
        lockState = AcmUtils.lockCompleted(deployState, lockState);
        intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, element.getId(), deployState,
                lockState, StateChangeResult.NO_ERROR, "Restarted");
    }

    @Override
    public void migrate(UUID automationCompositionId, AcElementDeploy element, UUID compositionTargetId,
            Map<String, Object> properties) throws PfModelException {
        intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, element.getId(),
                DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Migrated");
    }
}
