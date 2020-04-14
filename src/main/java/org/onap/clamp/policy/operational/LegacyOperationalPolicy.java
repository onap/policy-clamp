/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights
 *                             reserved.
 * Modifications Copyright (C) 2020 Huawei Technologies Co., Ltd.
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

package org.onap.clamp.policy.operational;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.lang3.math.NumberUtils;
import org.onap.clamp.loop.Loop;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.ScalarStyle;
import org.yaml.snakeyaml.Yaml;

/**
 * This class contains the code required to support the sending of Legacy
 * operational payload to policy engine. This will probably disappear in El
 * Alto.
 */
public class LegacyOperationalPolicy {

    private static final String ACTOR = "actor";
    private static final String RECIPE = "recipe";
    private static final String POLICIES = "policies";
    private static final String PAYLOAD = "payload";

    private LegacyOperationalPolicy() {

    }

    private static void translateStringValues(String jsonKey, String stringValue, JsonElement parentJsonElement) {
        if (stringValue.equalsIgnoreCase("true") || stringValue.equalsIgnoreCase("false")) {
            parentJsonElement.getAsJsonObject().addProperty(jsonKey, Boolean.valueOf(stringValue));

        } else if (NumberUtils.isParsable(stringValue)) {
            parentJsonElement.getAsJsonObject().addProperty(jsonKey, Long.parseLong(stringValue));
        }
    }

    private static JsonElement removeAllQuotes(JsonElement jsonElement) {
        if (jsonElement.isJsonArray()) {
            for (JsonElement element : jsonElement.getAsJsonArray()) {
                removeAllQuotes(element);
            }
        } else if (jsonElement.isJsonObject()) {
            for (Entry<String, JsonElement> entry : jsonElement.getAsJsonObject().entrySet()) {
                if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()) {
                    translateStringValues(entry.getKey(), entry.getValue().getAsString(), jsonElement);
                } else {
                    removeAllQuotes(entry.getValue());
                }
            }
        }
        return jsonElement;
    }

    /**
     * This method rework the payload attribute (yaml) that is normally wrapped in a
     * string when coming from the UI.
     *
     * @param policyJson The operational policy json config
     * @return The same object reference but modified
     */
    public static JsonElement reworkPayloadAttributes(JsonElement policyJson) {
        for (JsonElement policy : policyJson.getAsJsonObject().get("policies").getAsJsonArray()) {
            JsonElement payloadElem = policy.getAsJsonObject().get("payload");
            String payloadString = payloadElem != null ? payloadElem.getAsString() : "";
            if (!payloadString.isEmpty()) {
                Map<String, String> testMap = new Yaml().load(payloadString);
                String json = new GsonBuilder().create().toJson(testMap);
                policy.getAsJsonObject().add("payload", new GsonBuilder().create().fromJson(json, JsonElement.class));
            }
        }
        return policyJson;
    }

    private static void replacePropertiesIfEmpty(JsonElement policy, String key, String valueIfEmpty) {
        JsonElement payloadElem = policy.getAsJsonObject().get(key);
        String payloadString = payloadElem != null ? payloadElem.getAsString() : "";
        if (payloadString.isEmpty()) {
            policy.getAsJsonObject().addProperty(key, valueIfEmpty);
        }
    }

    private static JsonElement fulfillPoliciesTreeField(JsonElement policyJson) {
        for (JsonElement policy : policyJson.getAsJsonObject().get("policies").getAsJsonArray()) {
            replacePropertiesIfEmpty(policy, "success", "final_success");
            replacePropertiesIfEmpty(policy, "failure", "final_failure");
            replacePropertiesIfEmpty(policy, "failure_timeout", "final_failure_timeout");
            replacePropertiesIfEmpty(policy, "failure_retries", "final_failure_retries");
            replacePropertiesIfEmpty(policy, "failure_exception", "final_failure_exception");
            replacePropertiesIfEmpty(policy, "failure_guard", "final_failure_guard");
            // Again special case for payload, should remove it if it's there but empty
            // otherwise policy crashes
            JsonElement payloadElem = policy.getAsJsonObject().get("payload");
            if (payloadElem != null && payloadElem.isJsonPrimitive() && payloadElem.getAsString().isEmpty()) {
                policy.getAsJsonObject().remove("payload");
            }
        }
        return policyJson;
    }

    private static Map<String, Object> createMap(JsonElement jsonElement) {
        Map<String, Object> mapResult = new TreeMap<>();

        if (jsonElement.isJsonObject()) {
            for (Entry<String, JsonElement> entry : jsonElement.getAsJsonObject().entrySet()) {
                if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()) {
                    mapResult.put(entry.getKey(), entry.getValue().getAsString());
                } else if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isBoolean()) {
                    mapResult.put(entry.getKey(), entry.getValue().getAsBoolean());
                } else if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isNumber()) {
                    // Only int ro long normally, we don't need float here
                    mapResult.put(entry.getKey(), entry.getValue().getAsLong());
                } else if (entry.getValue().isJsonArray()) {
                    List<Map<String, Object>> newArray = new ArrayList<>();
                    mapResult.put(entry.getKey(), newArray);
                    for (JsonElement element : entry.getValue().getAsJsonArray()) {
                        newArray.add(createMap(element));
                    }
                } else if (entry.getValue().isJsonObject()) {
                    mapResult.put(entry.getKey(), createMap(entry.getValue()));
                }
            }
        }
        return mapResult;
    }

    /**
     * This method transforms the configuration json to a Yaml format.
     *
     * @param operationalPolicyJsonElement The operational policy json config
     * @return The Yaml as string
     */
    public static String createPolicyPayloadYamlLegacy(JsonElement operationalPolicyJsonElement) {
        DumperOptions options = new DumperOptions();
        options.setDefaultScalarStyle(ScalarStyle.PLAIN);
        options.setIndent(2);
        options.setPrettyFlow(true);
        // Policy can't support { } in the yaml
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        return (new Yaml(options)).dump(createMap(fulfillPoliciesTreeField(
                removeAllQuotes(reworkActorAttributes(operationalPolicyJsonElement.getAsJsonObject().deepCopy())))));
    }

    /**
     * This method load mandatory field in the operational policy configuration
     * JSON.
     *
     * @param configurationsJson The operational policy JSON
     * @param loop               The parent loop object
     */
    public static void preloadConfiguration(JsonObject configurationsJson, Loop loop) {
        if (configurationsJson != null && configurationsJson.entrySet().isEmpty()) {
            JsonObject controlLoopName = new JsonObject();
            controlLoopName.addProperty("controlLoopName",
                    loop != null ? loop.getName() : "Empty (NO loop loaded yet)");
            JsonObject controlLoop = new JsonObject();
            controlLoop.add("controlLoop", controlLoopName);
            configurationsJson.add("operational_policy", controlLoop);
        }
    }

    /**
     * This method rework on the actor/recipe and payload attribute.
     *
     * @param policyJson The operational policy json config
     * @return The same object reference but modified
     */
    public static JsonElement reworkActorAttributes(JsonElement policyJson) {
        for (JsonElement policy : policyJson.getAsJsonObject().get(POLICIES).getAsJsonArray()) {
            JsonObject actor = policy.getAsJsonObject().get(ACTOR).getAsJsonObject();
            policy.getAsJsonObject().remove(ACTOR);
            String actorStr = actor.getAsJsonObject().get(ACTOR).getAsString();
            policy.getAsJsonObject().addProperty(ACTOR, actorStr);

            if ("CDS".equalsIgnoreCase(actorStr)) {
                policy.getAsJsonObject().addProperty(RECIPE, getRecipe(actor));
                addCdsPayloadAttributes(actor.getAsJsonObject(RECIPE), policy);
            } else {
                policy.getAsJsonObject().addProperty(RECIPE,
                                                     actor.getAsJsonObject().get(RECIPE).getAsString());
                addPayloadAttributes(actor, policy);
            }
        }
        return policyJson;
    }

    private static void addPayloadAttributes(JsonObject jsonObject,
                                             JsonElement policy) {
        JsonElement payloadElem = jsonObject.getAsJsonObject().get(PAYLOAD);
        String payloadString = payloadElem != null ? payloadElem.getAsString() : "";
        if (!payloadString.isEmpty()) {
            Map<String, String> testMap = new Yaml().load(payloadString);
            String json = new GsonBuilder().create().toJson(testMap);
            policy.getAsJsonObject().add(PAYLOAD,
                                         new GsonBuilder().create().fromJson(json, JsonElement.class));
        } else {
            policy.getAsJsonObject().addProperty(PAYLOAD, "");
        }
    }

    private static void addCdsPayloadAttributes(JsonObject jsonObject,
                                             JsonElement policy) {
        JsonElement payloadElem = jsonObject.getAsJsonObject().get(PAYLOAD);
        JsonObject payloadObject = payloadElem != null ?
                payloadElem.getAsJsonObject() : null;
        if (payloadObject != null) {
            policy.getAsJsonObject().add(PAYLOAD,
                                         payloadObject);
        } else {
            policy.getAsJsonObject().addProperty(PAYLOAD, "");
        }
    }

    private static String getRecipe(JsonObject actor) {
        return actor.getAsJsonObject().get("recipe").getAsJsonObject().get("recipe").getAsString();
    }
}
