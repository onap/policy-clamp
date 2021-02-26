/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
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

package org.onap.policy.clamp.policy;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import javax.persistence.Transient;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

/**
 * This class is a utility class to create the policy payload.
 */
public class PolicyPayload {

    private static final EELFLogger logger = EELFManager.getInstance().getLogger(PolicyPayload.class);

    private static JsonObject createJsonFromPolicyTosca(String toscaContent) {
        Map<String, Object> map =
                new Yaml().load(toscaContent != null && !toscaContent.isEmpty() ? toscaContent : "");
        return new Gson().fromJson(new JSONObject(map).toString(), JsonObject.class);
    }

    /**
     * This method create the policy payload that must be sent to PEF.
     *
     * @return A String containing the payload
     * @throws UnsupportedEncodingException In case of failure
     */
    public static String createPolicyPayload(String policyModelType, String policyModelVersion, String policyName,
                                      String policyVersion, JsonObject policyProperties, String toscaContent)
            throws UnsupportedEncodingException {
        JsonObject policyPayloadResult = new JsonObject();

        policyPayloadResult.add("tosca_definitions_version",
                createJsonFromPolicyTosca(toscaContent).get("tosca_definitions_version"));

        JsonObject topologyTemplateNode = new JsonObject();
        policyPayloadResult.add("topology_template", topologyTemplateNode);

        JsonArray policiesArray = new JsonArray();
        topologyTemplateNode.add("policies", policiesArray);

        JsonObject thisPolicy = new JsonObject();
        policiesArray.add(thisPolicy);

        JsonObject policyDetails = new JsonObject();
        thisPolicy.add(policyName, policyDetails);
        policyDetails.addProperty("type", policyModelType);
        policyDetails.addProperty("type_version", policyModelVersion);
        policyDetails.addProperty("version", policyVersion);

        JsonObject policyMetadata = new JsonObject();
        policyDetails.add("metadata", policyMetadata);
        policyMetadata.addProperty("policy-id", policyName);

        policyDetails.add("properties", policyProperties);

        String policyPayload = new GsonBuilder().setPrettyPrinting().create().toJson(policyPayloadResult);
        logger.info("Policy payload: " + policyPayload);
        return policyPayload;
    }
}