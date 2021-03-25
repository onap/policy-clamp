/*-
 * ============LICENSE_START=======================================================
 * ONAP POLICY-CLAMP
 * ================================================================================
 * Copyright (C) 2021 AT&T Intellectual Property. All rights
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

package org.onap.policy.clamp.policy.pdpgroup;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * This is an utility class that build the PDP group policy payload.
 * This is used when policies have to be deployed to PDP group/subgroups on the Policy Engine.
 */
public class PdpGroupPayload {

    // First level is the PDPGroup, the second is the PdpSubGroup, the third is the operation (POST or DELETE)
    private Map<String, Map<String, Map<String, List<ToscaConceptIdentifier>>>> pdpGroupMap = new HashMap<>();

    /**
     * This method updates the pdpGroupMap structure for a specific policy/version/pdpdGroup/PdpSubGroup.
     *
     * @param pdpGroup      The pdp Group in String
     * @param pdpSubGroup   The pdp Sub Group in String
     * @param policyName    The policy name
     * @param policyVersion The policy Version
     * @param operation     DELETE or POST
     */
    public void updatePdpGroupMap(String pdpGroup,
                                  String pdpSubGroup,
                                  String policyName,
                                  String policyVersion, String operation) {
        ToscaConceptIdentifier newPolicyToAdd = new ToscaConceptIdentifier(policyName, policyVersion);
        pdpGroupMap.computeIfAbsent(pdpGroup, key -> new HashMap<>());
        pdpGroupMap.get(pdpGroup).computeIfAbsent(pdpSubGroup, key -> new HashMap<>());
        pdpGroupMap.get(pdpGroup).get(pdpSubGroup).computeIfAbsent(operation, key -> new ArrayList<>());
        pdpGroupMap.get(pdpGroup).get(pdpSubGroup).get(operation).add(newPolicyToAdd);
    }

    /**
     * This method generates the Payload in Json from the pdp Group structure containing the policies/versions
     * that must be sent to the policy framework.
     *
     * @return The Json that can be sent to policy framework as JsonObject
     */
    public JsonObject generateActivatePdpGroupPayload() {
        JsonArray payloadArray = new JsonArray();
        for (Map.Entry<String, Map<String, Map<String, List<ToscaConceptIdentifier>>>> pdpGroupInfo : pdpGroupMap
                .entrySet()) {
            JsonObject pdpGroupNode = new JsonObject();
            pdpGroupNode.addProperty("name", pdpGroupInfo.getKey());
            pdpGroupNode.add("deploymentSubgroups", buildPdpSubGroupsArray(pdpGroupInfo.getValue()));
            payloadArray.add(pdpGroupNode);
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("groups", payloadArray);
        return jsonObject;
    }

    private JsonArray buildPdpSubGroupsArray (Map<String, Map<String, List<ToscaConceptIdentifier>>> mapOfPdpSubGroups) {
        JsonArray subGroupsArray = new JsonArray();
        for (Map.Entry<String, Map<String, List<ToscaConceptIdentifier>>> pdpSubGroupInfo : mapOfPdpSubGroups
                .entrySet()) {
            for (Map.Entry<String, List<ToscaConceptIdentifier>> operation : pdpSubGroupInfo
                    .getValue().entrySet()) {
                JsonObject pdpSubGroupNode = new JsonObject();
                subGroupsArray.add(pdpSubGroupNode);
                pdpSubGroupNode.addProperty("pdpType", pdpSubGroupInfo.getKey());
                pdpSubGroupNode.addProperty("action", operation.getKey());
                pdpSubGroupNode.add("policies", new JsonArray());
                for (ToscaConceptIdentifier policy : operation.getValue()) {
                    JsonObject newPolicy = new JsonObject();
                    newPolicy.addProperty("name",policy.getName());
                    newPolicy.addProperty("version",policy.getVersion());
                    pdpSubGroupNode.getAsJsonArray("policies").add(newPolicy);
                }
            }
        }
        return subGroupsArray;
    }
}
