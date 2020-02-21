/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END============================================
 * ===================================================================
 *
 */

package org.onap.clamp.loop.components.external;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Transient;

import org.apache.camel.Exchange;
import org.onap.clamp.clds.util.JsonUtils;
import org.onap.clamp.loop.Loop;
import org.onap.clamp.policy.microservice.MicroServicePolicy;
import org.onap.clamp.policy.operational.OperationalPolicy;

public class PolicyComponent extends ExternalComponent {

    @Transient
    private static final EELFLogger logger = EELFManager.getInstance().getLogger(PolicyComponent.class);

    public static final ExternalComponentState IN_ERROR = new ExternalComponentState("IN_ERROR",
            "There was an error during the sending to policy, the policy engine may be corrupted or inconsistent", 100);
    public static final ExternalComponentState NOT_SENT = new ExternalComponentState("NOT_SENT",
            "The policies defined have NOT yet been created on the policy engine", 90);
    public static final ExternalComponentState SENT = new ExternalComponentState("SENT",
            "The policies defined have been created but NOT deployed on the policy engine", 50);
    public static final ExternalComponentState SENT_AND_DEPLOYED = new ExternalComponentState("SENT_AND_DEPLOYED",
            "The policies defined have been created and deployed on the policy engine", 10);
    public static final ExternalComponentState UNKNOWN = new ExternalComponentState("UNKNOWN",
            "The current status is not clear. Need to regresh the status to get the current status.", 0);

    /**
     * Default constructor.
     */
    public PolicyComponent() {
        /*
         * We assume it's good by default as we will receive the state for each policy
         * on by one, each time we increase the level we can't decrease it anymore.
         * That's why it starts with the lowest one SENT_AND_DEPLOYED.
         */
        super(UNKNOWN);
    }

    @Override
    public String getComponentName() {
        return "POLICY";
    }

    /**
     * Generates the Json that must be sent to policy to add all policies to Active
     * PDP group.
     *
     * @return The json, payload to send
     */
    public static String createPoliciesPayloadPdpGroup(Loop loop) {
        JsonObject jsonObject = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        jsonObject.add("groups", jsonArray);

        for (OperationalPolicy opPolicy : loop.getOperationalPolicies()) {
            jsonArray.add(createPdpDeploymentPayload(opPolicy.getPdpGroup(), opPolicy.getPdpSubGroup(),
                    opPolicy.getPolicyModel().getPolicyModelType(), opPolicy.getPolicyModel().getVersion()));
        }

        for (MicroServicePolicy msPolicy : loop.getMicroServicePolicies()) {
            jsonArray.add(createPdpDeploymentPayload(msPolicy.getPdpGroup(), msPolicy.getPdpSubGroup(),
                    msPolicy.getPolicyModel().getPolicyModelType(), msPolicy.getPolicyModel().getVersion()));
        }

        String payload = new GsonBuilder().setPrettyPrinting().create().toJson(jsonObject);
        logger.info("PdpGroup policy payload: " + payload);
        return new GsonBuilder().setPrettyPrinting().create().toJson(jsonObject);
    }

    private static JsonObject createPdpDeploymentPayload(String pdpGroup, String pdpSubGroup,
            String policyType, String version) {
        JsonObject pdpGroupNode = new JsonObject();
        JsonArray subPdpArray = new JsonArray();
        pdpGroupNode.addProperty("name", pdpGroup);
        pdpGroupNode.add("deploymentSubgroups", subPdpArray);

        JsonObject pdpSubGroupNode = new JsonObject();
        subPdpArray.add(pdpSubGroupNode);
        pdpSubGroupNode.addProperty("pdpType", pdpSubGroup);
        pdpSubGroupNode.addProperty("action", "POST");

        JsonArray policyArray = new JsonArray();
        pdpSubGroupNode.add("policies", policyArray);
        JsonObject policyNode = new JsonObject();
        policyNode.addProperty("name", policyType);
        policyNode.addProperty("version", version);
        policyArray.add(policyNode);
        return pdpGroupNode;
    }

    /**
     * Generates the list of policy names that must be send/remove to/from active
     * PDP group.
     *
     * @return A list of policy names
     */
    public static List<String> listPolicyNamesPdpGroup(Loop loop) {
        List<String> policyNamesList = new ArrayList<>();
        for (OperationalPolicy opPolicy : loop.getOperationalPolicies()) {
            policyNamesList.add(opPolicy.getName());
            for (String guardName : opPolicy.createGuardPolicyPayloads().keySet()) {
                policyNamesList.add(guardName);
            }
        }
        for (MicroServicePolicy microServicePolicy : loop.getMicroServicePolicies()) {
            policyNamesList.add(microServicePolicy.getName());
        }
        return policyNamesList;
    }

    private static ExternalComponentState findNewState(boolean found, boolean deployed) {

        ExternalComponentState newState = NOT_SENT;
        if (found && deployed) {
            newState = SENT_AND_DEPLOYED;
        } else if (found) {
            newState = SENT;
        } else if (deployed) {
            newState = IN_ERROR;
        }
        return newState;
    }

    private static ExternalComponentState mergeStates(ExternalComponentState oldState,
            ExternalComponentState newState) {
        return (oldState.compareTo(newState) < 0) ? newState : oldState;
    }

    /**
     * This is a method that expect the results of the queries getPolicy and
     * getPolicyDeployed for a unique policy (op,guard, config, etc ...). It
     * re-computes the global policy state for each policy results given. Therefore
     * this method is called multiple times from the camel route and must be reset
     * for a new global policy state retrieval. The state to compute the global
     * policy state is stored in this class.
     * 
     */
    @Override
    public ExternalComponentState computeState(Exchange camelExchange) {
        this.setState(mergeStates(this.getState(),
                findNewState((boolean) camelExchange.getIn().getExchange().getProperty("policyFound"),
                        (boolean) camelExchange.getIn().getExchange().getProperty("policyDeployed"))));
        return this.getState();
    }
}
