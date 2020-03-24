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
 * Modifications copyright (c) 2019 AT&T
 * ===================================================================
 *
 */

package org.onap.clamp.clds.sdc.controller.installer;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONObject;
import org.onap.clamp.clds.exception.sdc.controller.BlueprintParserException;
import org.yaml.snakeyaml.Yaml;

public class BlueprintParser {

    static final String TCA = "TCA";
    private static final String NODE_TEMPLATES = "node_templates";
    private static final String DCAE_NODES = "dcae.nodes.";
    private static final String DCAE_NODES_POLICY = ".nodes.policy";
    private static final String TYPE = "type";
    private static final String PROPERTIES = "properties";
    private static final String NAME = "name";
    private static final String INPUT = "inputs";
    private static final String GET_INPUT = "get_input";
    private static final String POLICY_MODEL_ID = "policy_model_id";
    private static final String POLICY_MODEL_VERSION = "policy_model_version";
    private static final String RELATIONSHIPS = "relationships";
    private static final String CLAMP_NODE_RELATIONSHIPS_GETS_INPUT_FROM = "clamp_node.relationships.gets_input_from";
    private static final String TARGET = "target";
    public static final String DEFAULT_VERSION = "1.0.0";

    private static final EELFLogger logger = EELFManager.getInstance().getLogger(BlueprintParser.class);

    private BlueprintParser() {

    }

    /**
     * Get all micro services from blueprint.
     * 
     * @param blueprintString the blueprint in a String
     * @return A set of MircoService
     * @throws BlueprintParserException In case of issues with the parsing
     */
    public static Set<BlueprintMicroService> getMicroServices(String blueprintString) throws BlueprintParserException {
        Set<BlueprintMicroService> microServices = new HashSet<>();
        JsonObject blueprintJson = BlueprintParser.convertToJson(blueprintString);
        JsonObject nodeTemplateList = blueprintJson.get(NODE_TEMPLATES).getAsJsonObject();
        JsonObject inputList = blueprintJson.get(INPUT).getAsJsonObject();

        for (Entry<String, JsonElement> entry : nodeTemplateList.entrySet()) {
            JsonObject nodeTemplate = entry.getValue().getAsJsonObject();
            if (!nodeTemplate.get(TYPE).getAsString().contains(DCAE_NODES_POLICY)
                    && nodeTemplate.get(TYPE).getAsString().contains(DCAE_NODES)) {
                BlueprintMicroService microService = getNodeRepresentation(entry, nodeTemplateList, inputList);
                if (!microService.getModelType().isBlank()) {
                    microServices.add(microService);
                } else {
                    logger.warn("Microservice " + microService.getName()
                            + " will NOT be used by CLAMP as the model type is not defined or has not been found");
                }
            }
        }
        logger.debug("Those microservices have been found in the blueprint:" + microServices);
        return microServices;
    }

    /**
     * Does a fallback to TCA.
     * 
     * @return The list of microservices
     */
    public static List<BlueprintMicroService> fallbackToOneMicroService() {
        return Collections.singletonList(
                new BlueprintMicroService(TCA, "onap.policies.monitoring.cdap.tca.hi.lo.app", "", DEFAULT_VERSION));
    }

    static String getName(Entry<String, JsonElement> entry) {
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

    static String getInput(Entry<String, JsonElement> entry) {
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

    static String findPropertyInRelationshipsArray(String propertyName, JsonArray relationshipsArray,
            JsonObject blueprintNodeTemplateList, JsonObject blueprintInputList) throws BlueprintParserException {
        for (JsonElement elem : relationshipsArray) {
            if (blueprintNodeTemplateList.get(elem.getAsJsonObject().get(TARGET).getAsString()) == null) {
                throw new BlueprintParserException(
                        "The Target mentioned in the blueprint is not a known entry in the blueprint: "
                                + elem.getAsJsonObject().get(TARGET).getAsString());
            } else {
                String property = getPropertyValue(propertyName,
                        new AbstractMap.SimpleEntry<String, JsonElement>(
                                elem.getAsJsonObject().get(TARGET).getAsString(), blueprintNodeTemplateList
                                        .get(elem.getAsJsonObject().get(TARGET).getAsString()).getAsJsonObject()),
                        blueprintNodeTemplateList, blueprintInputList);
                if (!property.isEmpty()) {
                    return property;
                }
            }
        }
        return "";
    }

    static String getDirectOrInputPropertyValue(String propertyName, JsonObject blueprintInputList,
            JsonObject nodeTemplateContent) {
        JsonObject properties = nodeTemplateContent.get(PROPERTIES).getAsJsonObject();
        if (properties.has(propertyName)) {
            if (properties.get(propertyName).isJsonObject()) {
                // it's a blueprint parameter
                return blueprintInputList
                        .get(properties.get(propertyName).getAsJsonObject().get(GET_INPUT).getAsString())
                        .getAsJsonObject().get("default").getAsString();
            } else {
                // It's a direct value
                return properties.get(propertyName).getAsString();
            }
        }
        return "";
    }

    static String getPropertyValue(String propertyName, Entry<String, JsonElement> nodeTemplateEntry,
            JsonObject blueprintNodeTemplateList, JsonObject blueprintIputList) throws BlueprintParserException {
        JsonObject nodeTemplateContent = nodeTemplateEntry.getValue().getAsJsonObject();
        // Search first in this node template
        if (nodeTemplateContent.has(PROPERTIES)) {
            String propValue = getDirectOrInputPropertyValue(propertyName, blueprintIputList, nodeTemplateContent);
            if (!propValue.isBlank()) {
                return propValue;
            }
        }
        // Or it's may be defined in a relationship
        if (nodeTemplateContent.has(RELATIONSHIPS)) {
            return findPropertyInRelationshipsArray(propertyName,
                    nodeTemplateContent.get(RELATIONSHIPS).getAsJsonArray(), blueprintNodeTemplateList,
                    blueprintIputList);
        }
        return "";
    }

    static BlueprintMicroService getNodeRepresentation(Entry<String, JsonElement> nodeTemplateEntry,
            JsonObject blueprintNodeTemplateList, JsonObject blueprintInputList) throws BlueprintParserException {
        String modelIdFound = getPropertyValue(POLICY_MODEL_ID, nodeTemplateEntry, blueprintNodeTemplateList,
                blueprintInputList);
        String versionFound = getPropertyValue(POLICY_MODEL_VERSION, nodeTemplateEntry, blueprintNodeTemplateList,
                blueprintInputList);
        if (modelIdFound.isBlank()) {
            logger.warn("policy_model_id is not defined for the node template:" + nodeTemplateEntry.getKey());
        }
        if (versionFound.isBlank()) {
            logger.warn("policy_model_version is not defined (setting it to a default value) for the node template:"
                    + nodeTemplateEntry.getKey());
        }
        return new BlueprintMicroService(getName(nodeTemplateEntry), modelIdFound, getInput(nodeTemplateEntry),
                !versionFound.isBlank() ? versionFound : DEFAULT_VERSION);
    }

    private static String getTarget(JsonObject elementObject) {
        if (elementObject.has(TYPE) && elementObject.has(TARGET)
                && elementObject.get(TYPE).getAsString().equals(CLAMP_NODE_RELATIONSHIPS_GETS_INPUT_FROM)) {
            return elementObject.get(TARGET).getAsString();
        }
        return "";
    }

    private static JsonObject convertToJson(String yamlString) {
        Map<String, Object> map = new Yaml().load(yamlString);
        return new Gson().fromJson(new JSONObject(map).toString(), JsonObject.class);
    }
}
