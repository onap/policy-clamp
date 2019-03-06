/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 Nokia Intellectual Property. All rights
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
package org.onap.clamp.clds.sdc.controller.installer;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

@Component
public class BlueprintParser {

    static final String TCA = "TCA";
    static final String HOLMES = "Holmes";
    private static final String TCA_POLICY = "tca_policy";
    private static final String HOLMES_PREFIX = "holmes";
    private static final String NODE_TEMPLATES = "node_templates";
    private static final String DCAE_NODES = "dcae.nodes.";
    private static final String TYPE = "type";
    private static final String PROPERTIES = "properties";
    private static final String NAME = "name";
    private static final String RELATIONSHIPS = "relationships";
    private static final String CLAMP_NODE_RELATIONSHIPS_GETS_INPUT_FROM = "clamp_node.relationships.gets_input_from";
    private static final String TARGET = "target";

    public Set<MicroService> getMicroServices(String blueprintString) {
        Set<MicroService> microServices = new HashSet<>();
        JsonObject jsonObject = BlueprintParser.convertToJson(blueprintString);
        JsonObject results = jsonObject.get(NODE_TEMPLATES).getAsJsonObject();

        for (Entry<String, JsonElement> entry : results.entrySet()) {
            JsonObject nodeTemplate = entry.getValue().getAsJsonObject();
            if (nodeTemplate.get(TYPE).getAsString().contains(DCAE_NODES)) {
                MicroService microService = getNodeRepresentation(entry);
                microServices.add(microService);
            }
        }
        microServices.removeIf(ms -> TCA_POLICY.equals(ms.getName()));
        return microServices;
    }

    public List<MicroService> fallbackToOneMicroService(String blueprintString) {
        JsonObject jsonObject = BlueprintParser.convertToJson(blueprintString);
        JsonObject results = jsonObject.get(NODE_TEMPLATES).getAsJsonObject();
        String theBiggestMicroServiceContent = "";
        String theBiggestMicroServiceKey = "";
        for (Entry<String, JsonElement> entry : results.entrySet()) {
            String msAsString = entry.getValue().toString();
            int len = msAsString.length();
            if (len > theBiggestMicroServiceContent.length()) {
                theBiggestMicroServiceContent = msAsString;
                theBiggestMicroServiceKey = entry.getKey();
            }
        }
        String msName = theBiggestMicroServiceKey.toLowerCase().contains(HOLMES_PREFIX) ? HOLMES : TCA;
        return Collections.singletonList(new MicroService(msName, ""));
    }

    String getName(Entry<String, JsonElement> entry) {
        String microServiceYamlName = entry.getKey();
        JsonObject ob = entry.getValue().getAsJsonObject();
        if (ob.has(PROPERTIES)) {
            JsonObject properties = ob.get(PROPERTIES).getAsJsonObject();
            if (properties.has(NAME)) {
                return properties.get(NAME).getAsString();
            }
        }
        return microServiceYamlName;
    }

    String getInput(Entry<String, JsonElement> entry) {
        JsonObject ob = entry.getValue().getAsJsonObject();
        if (ob.has(RELATIONSHIPS)) {
            JsonArray relationships = ob.getAsJsonArray(RELATIONSHIPS);
            for (JsonElement element : relationships) {
                String target = getTarget(element.getAsJsonObject());
                if (!target.isEmpty()) {
                    return target;
                }
            }
        }
        return "";
    }

    MicroService getNodeRepresentation(Entry<String, JsonElement> entry) {
        String name = getName(entry);
        String getInputFrom = getInput(entry);
        return new MicroService(name, getInputFrom);
    }

    private String getTarget(JsonObject elementObject) {
        if (elementObject.has(TYPE) && elementObject.has(TARGET)
            && elementObject.get(TYPE).getAsString().equals(CLAMP_NODE_RELATIONSHIPS_GETS_INPUT_FROM)) {
            return elementObject.get(TARGET).getAsString();
        }
        return "";
    }

    private static JsonObject convertToJson(String yamlString) {
        Yaml yaml = new Yaml();
        Map<String, Object> map = yaml.load(yamlString);

        JSONObject jsonObject = new JSONObject(map);
        return new Gson().fromJson(jsonObject.toString(), JsonObject.class);
    }
}
