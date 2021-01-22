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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.onap.policy.clamp.clds.util.JsonUtils;
import org.onap.policy.models.pdp.concepts.PdpGroups;

/**
 * This is an utility class that contains methods to work on the different results provided by the PEF.
 * Mainly used to aggregate the results.
 */
public class PoliciesPdpMerger {

    /**
     * This method extract the content of a policy without knowing the key (policy Id).
     * This JsonElement normally contains only the policy ID then the content,
     * there is only one member in the Json element.
     *
     * @param policyJsonElement The policy as JsonElement
     * @return It return the content as JsonObject
     */
    public static JsonObject getPolicyContentOutOfJsonElement(JsonElement policyJsonElement) {
        return policyJsonElement.getAsJsonObject()
                .get(((String) policyJsonElement.getAsJsonObject().keySet().toArray()[0])).getAsJsonObject();
    }

    /**
     * This method merges 2 JsonElement together.
     *
     * @param json
     * @param jsonToMerge
     * @return
     */
    public static void mergeJsonElement(JsonObject json, JsonObject jsonToMerge) {
        if (jsonToMerge != null) {
            jsonToMerge.entrySet().stream().forEach(entry -> json.add(entry.getKey(), entry.getValue()));
        }
    }

    /**
     * This method merges the result of the policy listing and the associated Pdp Group info.
     * It can be seen as an enrichment of the policy listing.
     *
     * @param jsonPoliciesList The Json containing the policies from the PEF
     * @param pdpGroupsJson    The json containing the PDP groups info from the PEF
     * @return It returns a String containing the policies list enriched with PdpGroup info
     */
    public static String mergePoliciesAndPdpGroupStates(String jsonPoliciesList, String pdpGroupsJson) {
        PdpGroups pdpGroups = JsonUtils.GSON.fromJson(pdpGroupsJson, PdpGroups.class);
        JsonObject policiesListJson =
                JsonUtils.GSON.fromJson(jsonPoliciesList, JsonObject.class).get("topology_template")
                        .getAsJsonObject();
        StreamSupport.stream(policiesListJson.get("policies").getAsJsonArray().spliterator(), true)
                .forEach(policyJson -> enrichOnePolicy(pdpGroups, getPolicyContentOutOfJsonElement(policyJson)));
        return policiesListJson.toString();
    }

    /**
     * Enrich one policy json node object with pdpGroup info.
     *
     * @param pdpGroups      The pdpGroups from PEF to search the policy
     * @param policyJsonNode The policy json node that must be enriched
     */
    private static void enrichOnePolicy(PdpGroups pdpGroups, JsonObject policyJsonNode) {
        JsonObject deploymentPdpJson = PdpGroupsAnalyzer
                .getPdpGroupDeploymentOfOnePolicy(pdpGroups, policyJsonNode.get("name").getAsString(),
                        policyJsonNode.get("version").getAsString());
        mergeJsonElement(policyJsonNode, deploymentPdpJson);

        JsonObject supportedPdpGroupsJson = PdpGroupsAnalyzer
                .getSupportedPdpGroupsForModelType(pdpGroups, policyJsonNode.get("type").getAsString(),
                        policyJsonNode.get("type_version").getAsString());
        mergeJsonElement(policyJsonNode, supportedPdpGroupsJson);
    }
}
