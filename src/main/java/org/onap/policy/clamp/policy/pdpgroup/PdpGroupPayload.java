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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.StreamSupport;
import org.onap.policy.clamp.clds.util.JsonUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * This is an utility class that build the PDP group policy payload.
 * This is used when policies have to be deployed to PDP group/subgroups on the Policy Engine.
 */
public class PdpGroupPayload {

    /**
     * The default node that will contain the operations array.
     */
    public static final String PDP_OPERATIONS = "PdpOperations";

    // First level is the PDPGroup, the second is the PdpSubGroup, the third is the operation (POST or DELETE)
    private Map<String, Map<String, Map<String, List<ToscaConceptIdentifier>>>> pdpGroupMap = new ConcurrentHashMap<>();

    /**
     * Default constructor.
     */
    public PdpGroupPayload() {
    }

    /**
     * This method converts the list of operations directly to the pdp payload query as String.
     *
     * @param listOfPdpOperations The list of operations that needs to be done.
     *                            e.g: {"PdpOperations":["DELETE/PdpGroup1/PdpSubGroup1/PolicyName1/1.0.0",....]}
     * @return The string containing the PDP payload that can be sent directly
     */
    public static String generatePdpGroupPayloadFromList(final JsonElement listOfPdpOperations) {
        return new PdpGroupPayload(listOfPdpOperations).generatePdpGroupPayload();
    }

    /**
     * Constructor that takes a list of Operations in input.
     *
     * @param listOfPdpOperations The list of operations that needs to be done.
     *                            e.g: {"PdpOperations":["DELETE/PdpGroup1/PdpSubGroup1/PolicyName1/1.0.0",....]}
     */
    public PdpGroupPayload(final JsonElement listOfPdpOperations) {
        this.readListOfOperations(listOfPdpOperations);
    }

    private void readListOfOperations(final JsonElement listOfPdpOperations) {
        StreamSupport.stream(listOfPdpOperations.getAsJsonObject().getAsJsonArray(PDP_OPERATIONS).spliterator(), true)
                .forEach(operation -> {
                    String[] opParams = operation.getAsString().split("/");
                    this.updatePdpGroupMap(opParams[1], opParams[2], opParams[3], opParams[4], opParams[0]);
                });
    }

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
        pdpGroupMap.computeIfAbsent(pdpGroup, key -> new ConcurrentHashMap<>());
        pdpGroupMap.get(pdpGroup).computeIfAbsent(pdpSubGroup, key -> new ConcurrentHashMap<>());
        pdpGroupMap.get(pdpGroup).get(pdpSubGroup)
                .computeIfAbsent(operation, key -> Collections.synchronizedList(new ArrayList<>()));
        pdpGroupMap.get(pdpGroup).get(pdpSubGroup).get(operation)
                .add(new ToscaConceptIdentifier(policyName, policyVersion));
    }

    /**
     * This method generates the Payload in Json from the pdp Group structure containing the policies/versions
     * that must be sent to the policy framework.
     *
     * @return The Json that can be sent to policy framework as String
     */
    public String generatePdpGroupPayload() {
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
        return JsonUtils.GSON.toJson(jsonObject);
    }

    private JsonArray buildPdpSubGroupsArray(
            final Map<String, Map<String, List<ToscaConceptIdentifier>>> mapOfPdpSubGroups) {
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
                    newPolicy.addProperty("name", policy.getName());
                    newPolicy.addProperty("version", policy.getVersion());
                    pdpSubGroupNode.getAsJsonArray("policies").add(newPolicy);
                }
            }
        }
        return subGroupsArray;
    }
}
