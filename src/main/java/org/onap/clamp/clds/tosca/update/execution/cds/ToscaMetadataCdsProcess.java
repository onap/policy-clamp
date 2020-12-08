/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights
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

package org.onap.clamp.clds.tosca.update.execution.cds;

import static org.onap.clamp.clds.tosca.ToscaSchemaConstants.TYPE;
import static org.onap.clamp.clds.tosca.ToscaSchemaConstants.TYPE_LIST;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Set;
import org.onap.clamp.clds.tosca.update.execution.ToscaMetadataProcess;
import org.onap.clamp.loop.service.Service;

/**
 * This class is there to add the JsonObject for CDS in the json Schema according to what is found in the Tosca model.
 */
public class ToscaMetadataCdsProcess extends ToscaMetadataProcess {

    @Override
    public void executeProcess(String parameters, JsonObject childObject, Service serviceModel) {
        switch (parameters) {
            case "actor":
                JsonArray jsonArray = new JsonArray();
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
        JsonArray schemaAnyOf = new JsonArray();
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
        JsonArray schemaEnum = new JsonArray();
        JsonArray schemaTitle = new JsonArray();
        for (Map.Entry<String, JsonElement> entry : serviceModel.getResourceDetails().getAsJsonObject(resourceName)
                .entrySet()) {
            JsonObject controllerProperties = entry.getValue().getAsJsonObject()
                    .getAsJsonObject("controllerProperties");
            if (controllerProperties != null && controllerProperties.getAsJsonObject("workflows") != null) {
                for (String workflowsEntry : controllerProperties.getAsJsonObject("workflows").keySet()) {
                    schemaEnum.add(workflowsEntry);
                    schemaTitle.add(workflowsEntry + " (CDS operation)");
                }
            }
        }
        addToJsonArray(childObject, "enum", schemaEnum);
        if (childObject.get("options") == null) {
            JsonObject optionsSection = new JsonObject();
            childObject.add("options", optionsSection);
        }
        addToJsonArray(childObject.getAsJsonObject("options"), "enum_titles", schemaTitle);

    }

    private static JsonArray generatePayloadPerResource(String resourceName,
                                                        Service serviceModel) {
        JsonArray schemaAnyOf = new JsonArray();

        for (Map.Entry<String, JsonElement> entry : serviceModel.getResourceDetails().getAsJsonObject(resourceName)
                .entrySet()) {
            JsonObject controllerProperties = entry.getValue().getAsJsonObject()
                    .getAsJsonObject("controllerProperties");
            if (controllerProperties != null && controllerProperties.getAsJsonObject("workflows") != null) {
                for (Map.Entry<String, JsonElement> workflowsEntry : controllerProperties.getAsJsonObject("workflows")
                        .entrySet()) {
                    JsonObject obj = new JsonObject();
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
        JsonArray result = new JsonArray();
        JsonObject blankObject = new JsonObject();
        blankObject.addProperty("title", "User defined");
        blankObject.add("properties", new JsonObject());
        result.add(blankObject);
        return result;
    }

    private static JsonObject createAnyOfJsonProperty(String name,
                                                      String defaultValue,
                                                      boolean readOnlyFlag) {
        JsonObject result = new JsonObject();
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
        String artifactName = controllerProperties.get("sdnc_model_name").getAsString();
        String artifactVersion = controllerProperties.get("sdnc_model_version").getAsString();
        JsonObject inputs = workFlow.getAsJsonObject("inputs");
        JsonObject jsonObject = new JsonObject();
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
        JsonObject data = new JsonObject();
        data.addProperty("title", "data");
        data.addProperty("type", "string");
        data.addProperty("format", "textarea");
        JsonObject defaultValue = new JsonObject();
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
            JsonObject inputProperty = inputs.getAsJsonObject(key);
            if (key.equalsIgnoreCase(workFlowName + "-properties")) {
                addDefaultValueForData(entry.getValue().getAsJsonObject().get("properties")
                        .getAsJsonObject(), defaultValue, workFlowName);
            } else if ("object".equalsIgnoreCase(inputProperty.get(TYPE).getAsString())) {
                JsonObject object = new JsonObject();
                addDefaultValueForData(entry.getValue().getAsJsonObject().get("properties")
                        .getAsJsonObject(), object, workFlowName);
                defaultValue.add(entry.getKey(), object);
            } else if (TYPE_LIST.equalsIgnoreCase(inputProperty.get(TYPE).getAsString())) {
                defaultValue.add(entry.getKey(), handleListType(entry.getValue().getAsJsonObject(), workFlowName));
            } else {
                defaultValue.addProperty(entry.getKey(), "");
            }
        }
    }

    private static JsonArray handleListType(JsonObject inputs,
                                            String workFlowName) {

        JsonObject object = new JsonObject();
        if (inputs.get("properties") != null) {
            addDefaultValueForData(inputs.get("properties").getAsJsonObject(), object, workFlowName);
        }
        JsonArray arr = new JsonArray();
        arr.add(object);
        return arr;
    }
}
