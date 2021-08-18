/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights
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

package org.onap.policy.clamp.clds.tosca.update.execution.cds;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Set;
import org.onap.policy.clamp.clds.tosca.ToscaSchemaConstants;
import org.onap.policy.clamp.clds.tosca.update.execution.ToscaMetadataProcess;
import org.onap.policy.clamp.loop.service.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class is there to add the JsonObject for CDS in the json Schema according to what is found in the Tosca model.
 */
public class ToscaMetadataCdsProcess extends ToscaMetadataProcess {

    private static final Logger logger =
            LoggerFactory.getLogger(ToscaMetadataCdsProcess.class);

    @Override
    public void executeProcess(String parameters, JsonObject childObject, Service serviceModel) {
        if (serviceModel == null) {
            logger.info("serviceModel is null, therefore the ToscaMetadataCdsProcess is skipped");
            return;
        }
        switch (parameters) {
            case "actor":
                var jsonArray = new JsonArray();
                jsonArray.add("CDS");
                addToJsonArray(childObject, "enum", jsonArray);
                break;
            case "payload":
                generatePayload(childObject, serviceModel);
                break;
            case "operation":
                generateOperation(childObject, serviceModel);
                break;
            default:
        }
    }

    private static void generatePayload(JsonObject childObject, Service serviceModel) {
        var schemaAnyOf = new JsonArray();
        schemaAnyOf.addAll(createBlankEntry());
        schemaAnyOf.addAll(generatePayloadPerResource("VF", serviceModel));
        schemaAnyOf.addAll(generatePayloadPerResource("PNF", serviceModel));
        addToJsonArray(childObject, "anyOf", schemaAnyOf);
    }

    private static void generateOperation(JsonObject childObject, Service serviceModel) {
        generateOperationPerResource(childObject, "VF", serviceModel);
        generateOperationPerResource(childObject, "PNF", serviceModel);
    }

    private static void generateOperationPerResource(JsonObject childObject, String resourceName,
                                                     Service serviceModel) {
        var schemaEnum = new JsonArray();
        var schemaTitle = new JsonArray();
        for (Map.Entry<String, JsonElement> entry : serviceModel.getResourceDetails().getAsJsonObject(resourceName)
                .entrySet()) {
            var controllerProperties = entry.getValue().getAsJsonObject()
                    .getAsJsonObject("controllerProperties");
            if (controllerProperties != null && controllerProperties.getAsJsonObject("workflows") != null) {
                for (var workflowsEntry : controllerProperties.getAsJsonObject("workflows").keySet()) {
                    schemaEnum.add(workflowsEntry);
                    schemaTitle.add(workflowsEntry + " (CDS operation)");
                }
            }
        }
        addToJsonArray(childObject, "enum", schemaEnum);
        if (childObject.get("options") == null) {
            var optionsSection = new JsonObject();
            childObject.add("options", optionsSection);
        }
        addToJsonArray(childObject.getAsJsonObject("options"), "enum_titles", schemaTitle);

    }

    private static JsonArray generatePayloadPerResource(String resourceName,
                                                        Service serviceModel) {
        var schemaAnyOf = new JsonArray();

        for (Map.Entry<String, JsonElement> entry : serviceModel.getResourceDetails().getAsJsonObject(resourceName)
                .entrySet()) {
            var controllerProperties = entry.getValue().getAsJsonObject()
                    .getAsJsonObject("controllerProperties");
            if (controllerProperties != null && controllerProperties.getAsJsonObject("workflows") != null) {
                for (Map.Entry<String, JsonElement> workflowsEntry : controllerProperties.getAsJsonObject("workflows")
                        .entrySet()) {
                    var obj = new JsonObject();
                    obj.addProperty("title", workflowsEntry.getKey());
                    obj.add("properties",
                            createInputPropertiesForPayload(workflowsEntry.getValue().getAsJsonObject(),
                                                            controllerProperties,
                                                            workflowsEntry.getKey()));
                    schemaAnyOf.add(obj);
                }
            }
        }
        return schemaAnyOf;
    }

    private static JsonArray createBlankEntry() {
        var result = new JsonArray();
        var blankObject = new JsonObject();
        blankObject.addProperty("title", "User defined");
        blankObject.add("properties", new JsonObject());
        result.add(blankObject);
        return result;
    }

    private static JsonObject createAnyOfJsonProperty(String name,
                                                      String defaultValue,
                                                      boolean readOnlyFlag) {
        var result = new JsonObject();
        result.addProperty("title", name);
        result.addProperty("type", "string");
        result.addProperty("default", defaultValue);
        result.addProperty("readOnly", readOnlyFlag);
        return result;
    }

    private static void addToJsonArray(JsonObject childObject, String section, JsonArray value) {
        if (childObject.getAsJsonArray(section) != null) {
            childObject.getAsJsonArray(section).addAll(value);
        } else {
            childObject.add(section, value);
        }
    }

    /**
     * Returns the properties of payload based on the cds work flows.
     *
     * @param workFlow cds work flows to update payload
     * @param controllerProperties cds properties to get blueprint name and
     *                            version
     * @param workFlowName work flow name
     * @return returns the properties of payload
     */
    public static JsonObject createInputPropertiesForPayload(JsonObject workFlow,
                                                             JsonObject controllerProperties,
                                                             String workFlowName) {
        var artifactName = controllerProperties.get("sdnc_model_name").getAsString();
        var artifactVersion = controllerProperties.get("sdnc_model_version").getAsString();
        var inputs = workFlow.getAsJsonObject("inputs");
        var jsonObject = new JsonObject();
        jsonObject.add("artifact_name", createAnyOfJsonProperty(
                "artifact name", artifactName, true));
        jsonObject.add("artifact_version", createAnyOfJsonProperty(
                "artifact version", artifactVersion, true));
        jsonObject.add("mode", createAnyOfJsonProperty(
                "mode", "async", false));
        jsonObject.add("data", createDataProperty(inputs, workFlowName));
        return jsonObject;
    }

    private static JsonObject createDataProperty(JsonObject inputs, String workFlowName) {
        var data = new JsonObject();
        data.addProperty("title", "data");
        data.addProperty("type", "string");
        data.addProperty("format", "textarea");
        var defaultValue = new JsonObject();
        addDefaultValueForData(inputs, defaultValue, workFlowName);
        data.addProperty("default", defaultValue.toString());
        return data;
    }

    private static void addDefaultValueForData(JsonObject inputs,
                                               JsonObject defaultValue,
                                               String workFlowName) {
        Set<Map.Entry<String, JsonElement>> entrySet = inputs.entrySet();
        for (Map.Entry<String, JsonElement> entry : entrySet) {
            String key = entry.getKey();
            var inputProperty = inputs.getAsJsonObject(key);
            if (key.equalsIgnoreCase(workFlowName + "-properties")) {
                addDefaultValueForData(entry.getValue().getAsJsonObject().get("properties")
                        .getAsJsonObject(), defaultValue, workFlowName);
            } else if ("object".equalsIgnoreCase(inputProperty.get(ToscaSchemaConstants.TYPE).getAsString())) {
                var object = new JsonObject();
                addDefaultValueForData(entry.getValue().getAsJsonObject().get("properties")
                        .getAsJsonObject(), object, workFlowName);
                defaultValue.add(entry.getKey(), object);
            } else if (ToscaSchemaConstants.TYPE_LIST.equalsIgnoreCase(inputProperty.get(ToscaSchemaConstants.TYPE)
                    .getAsString())) {
                defaultValue.add(entry.getKey(), handleListType(entry.getValue().getAsJsonObject(), workFlowName));
            } else {
                defaultValue.addProperty(entry.getKey(), "");
            }
        }
    }

    private static JsonArray handleListType(JsonObject inputs,
                                            String workFlowName) {

        var object = new JsonObject();
        if (inputs.get("properties") != null) {
            addDefaultValueForData(inputs.get("properties").getAsJsonObject(), object, workFlowName);
        }
        var arr = new JsonArray();
        arr.add(object);
        return arr;
    }
}
