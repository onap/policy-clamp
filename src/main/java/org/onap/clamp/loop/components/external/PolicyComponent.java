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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import javax.persistence.Transient;

import org.apache.camel.Exchange;
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
        HashMap<String,HashMap<String, List<JsonObject>>> pdpGroupMap = new HashMap <String,HashMap<String, List<JsonObject>>>();
        for (OperationalPolicy opPolicy : loop.getOperationalPolicies()) {
            pdpGroupMap = updatePdpGroupMap(opPolicy.getPdpGroup(), opPolicy.getPdpSubgroup(),
                  opPolicy.getName(),
                  opPolicy.getPolicyModel().getVersion(), pdpGroupMap);
        }

        for (MicroServicePolicy msPolicy : loop.getMicroServicePolicies()) {
            pdpGroupMap = updatePdpGroupMap(msPolicy.getPdpGroup(), msPolicy.getPdpSubgroup(),
                    msPolicy.getName(),
                    msPolicy.getPolicyModel().getVersion(), pdpGroupMap);
        }

        String payload = new GsonBuilder().setPrettyPrinting().create()
              .toJson(generateActivatePdpGroupPayload(pdpGroupMap));
        logger.info("PdpGroup policy payload: " + payload);
        return payload;
    }

    private static HashMap<String,HashMap<String, List<JsonObject>>> updatePdpGroupMap (String pdpGroup, String pdpSubGroup, String policyName,
        String policyModelVersion, HashMap<String,HashMap<String, List<JsonObject>>> pdpGroupMap){

        JsonObject policyJson = new JsonObject();
        policyJson.addProperty("name", policyName);
        policyJson.addProperty("version", policyModelVersion);
        HashMap<String, List<JsonObject>> pdpSubGroupMap;
        List<JsonObject> policyList;
        if (pdpGroupMap.get(pdpGroup) == null) {
            pdpSubGroupMap = new HashMap <String, List<JsonObject>>();
            policyList = new LinkedList<JsonObject>();
        } else {
            pdpSubGroupMap = pdpGroupMap.get(pdpGroup);
            if (pdpSubGroupMap.get(pdpSubGroup) == null) {
                policyList = new LinkedList<JsonObject>();
            } else {
                policyList = (List<JsonObject>)pdpSubGroupMap.get(pdpSubGroup);
            }
        }
        policyList.add(policyJson);
        pdpSubGroupMap.put(pdpSubGroup, policyList);
        pdpGroupMap.put(pdpGroup, pdpSubGroupMap);

        return pdpGroupMap;
    }

    private static JsonObject generateActivatePdpGroupPayload(HashMap<String,HashMap<String, List<JsonObject>>> pdpGroupMap) {
        JsonArray payloadArray = new JsonArray();
        for (Entry<String, HashMap<String, List<JsonObject>>> pdpGroupInfo : pdpGroupMap.entrySet()) {
            JsonObject pdpGroupNode = new JsonObject();
            JsonArray subPdpArray = new JsonArray();
            pdpGroupNode.addProperty("name", pdpGroupInfo.getKey());
            pdpGroupNode.add("deploymentSubgroups", subPdpArray);

            JsonObject pdpSubGroupNode = new JsonObject();
            subPdpArray.add(pdpSubGroupNode);

            for (Entry<String, List<JsonObject>> pdpSubGroupInfo : pdpGroupInfo.getValue().entrySet()) {
                pdpSubGroupNode.addProperty("pdpType", pdpSubGroupInfo.getKey());
                pdpSubGroupNode.addProperty("action", "POST");

                JsonArray policyArray = new JsonArray();
                pdpSubGroupNode.add("policies", policyArray);

                for (JsonObject policy : pdpSubGroupInfo.getValue()) {
                    policyArray.add(policy);
                }
            }
            payloadArray.add(pdpGroupNode);
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("groups", payloadArray);
        return jsonObject;
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
