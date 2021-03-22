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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This is an utility class that build the PDP group policy payload.
 * This is used when policies have to be deployed to PDP group/subgroups on the Policy Engine.
 */
public class PdpGroupPayload {

    private Map<String, Map<String, List<JsonObject>>> pdpGroupMap = new HashMap<>();

    /**
     * This method updates the pdpGroupMap structure for a specific policy/version/pdpdGroup/PdpSubGroup.
     *
     * @param pdpGroup The pdp Group in String
     * @param pdpSubGroup The pdp Sub Group in String
     * @param policyName The policy name
     * @param policyVersion The policy Version
     */
    public void updatePdpGroupMap(String pdpGroup,
                                          String pdpSubGroup,
                                          String policyName,
                                          String policyVersion) {
        JsonObject policyJson = new JsonObject();
        policyJson.addProperty("name", policyName);
        policyJson.addProperty("version", policyVersion);
        Map<String, List<JsonObject>> pdpSubGroupMap;
        List<JsonObject> policyList;
        if (pdpGroupMap.get(pdpGroup) == null) {
            pdpSubGroupMap = new HashMap<>();
            policyList = new LinkedList<>();
        } else {
            pdpSubGroupMap = pdpGroupMap.get(pdpGroup);
            if (pdpSubGroupMap.get(pdpSubGroup) == null) {
                policyList = new LinkedList<>();
            } else {
                policyList = (List<JsonObject>) pdpSubGroupMap.get(pdpSubGroup);
            }
        }
        policyList.add(policyJson);
        pdpSubGroupMap.put(pdpSubGroup, policyList);
        pdpGroupMap.put(pdpGroup, pdpSubGroupMap);
    }

    /**
     * This method generates the Payload in Json from the pdp Group structure containing the policies/versions
     * that must be sent to the policy framework.
     *
     * @param action The action to do, either a POST or a DELETE
     * @return The Json that can be sent to policy framework as JsonObject
     */
    public JsonObject generateActivatePdpGroupPayload(String action) {
        JsonArray payloadArray = new JsonArray();
        for (Map.Entry<String, Map<String, List<JsonObject>>> pdpGroupInfo : pdpGroupMap.entrySet()) {
            JsonObject pdpGroupNode = new JsonObject();
            JsonArray subPdpArray = new JsonArray();
            pdpGroupNode.addProperty("name", pdpGroupInfo.getKey());
            pdpGroupNode.add("deploymentSubgroups", subPdpArray);

            for (Map.Entry<String, List<JsonObject>> pdpSubGroupInfo : pdpGroupInfo.getValue().entrySet()) {
                JsonObject pdpSubGroupNode = new JsonObject();
                subPdpArray.add(pdpSubGroupNode);
                pdpSubGroupNode.addProperty("pdpType", pdpSubGroupInfo.getKey());
                pdpSubGroupNode.addProperty("action", action);

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
}
