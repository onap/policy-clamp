/*-
 * ============LICENSE_START=======================================================
 * ONAP POLICY-CLAMP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights
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

package org.onap.policy.clamp.policy.operational;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.onap.policy.clamp.clds.util.JsonUtils;
import org.onap.policy.clamp.clds.util.ResourceFileUtils;
import org.onap.policy.clamp.loop.service.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OperationalPolicyRepresentationBuilder {

    private static final Logger logger =
            LoggerFactory.getLogger(OperationalPolicyRepresentationBuilder.class);

    public static final String PROPERTIES = "properties";
    public static final String ITEMS = "items";
    public static final String ANY_OF = "anyOf";
    public static final String TITLE = "title";
    public static final String RECIPE = "recipe";
    public static final String DEFAULT = "default";
    public static final String STRING = "string";
    public static final String TYPE = "type";
    public static final String TYPE_LIST = "list";
    public static final String TYPE_OBJECT = "object";
    public static final String TYPE_ARRAY = "array";

    private OperationalPolicyRepresentationBuilder() {
        throw new IllegalStateException("This is Utility class, not supposed to be initiated.");
    }

    /**
     * This method generates the operational policy json representation that will be
     * used by ui for rendering. It uses the model (VF and VFModule) defined in the
     * loop object to do so, so it's dynamic. It also uses the operational policy
     * schema template defined in the resource folder.
     *
     * @param modelJson The loop model json
     * @return The json representation
     */
    public static JsonObject generateOperationalPolicySchema(Service modelJson) {

        JsonObject jsonSchema = null;
        try {
            jsonSchema = JsonUtils.GSON.fromJson(
                    ResourceFileUtils
                            .getResourceAsString("clds/json-schema/operational_policies/operational_policy.json"),
                    JsonObject.class);
            jsonSchema.get(PROPERTIES).getAsJsonObject()
                    .get("operational_policy").getAsJsonObject().get(PROPERTIES).getAsJsonObject().get("policies")
                    .getAsJsonObject().get(ITEMS).getAsJsonObject().get(PROPERTIES).getAsJsonObject().get("target")
                    .getAsJsonObject().get(ANY_OF).getAsJsonArray().addAll(createAnyOfArray(modelJson, true));

            // update CDS recipe and payload information to schema
            for (JsonElement actor : jsonSchema.get(PROPERTIES).getAsJsonObject()
                    .get("operational_policy").getAsJsonObject().get(PROPERTIES).getAsJsonObject().get("policies")
                    .getAsJsonObject().get(ITEMS).getAsJsonObject().get(PROPERTIES).getAsJsonObject().get("actor")
                    .getAsJsonObject().get(ANY_OF).getAsJsonArray()) {
                if ("CDS".equalsIgnoreCase(actor.getAsJsonObject().get(TITLE).getAsString())) {
                    actor.getAsJsonObject().get(PROPERTIES).getAsJsonObject().get(RECIPE).getAsJsonObject()
                            .get(ANY_OF).getAsJsonArray()
                            .addAll(createAnyOfArrayForCdsRecipe(modelJson));
                }
            }
            return jsonSchema;
        } catch (IOException e) {
            logger.error("Unable to generate the json schema because of an exception", e);
            return new JsonObject();
        }
    }

    private static JsonObject createSchemaProperty(String title, String type, String defaultValue, String readOnlyFlag,
                                                   String[] enumArray) {
        var property = new JsonObject();
        property.addProperty(TITLE, title);
        property.addProperty(TYPE, type);
        property.addProperty(DEFAULT, defaultValue);
        property.addProperty("readOnly", readOnlyFlag);

        if (enumArray != null) {
            var jsonArray = new JsonArray();
            property.add("enum", jsonArray);
            for (String val : enumArray) {
                jsonArray.add(val);
            }
        }
        return property;
    }

    private static JsonArray createVnfSchema(Service modelService, boolean generateType) {
        var vnfSchemaArray = new JsonArray();
        JsonObject modelVnfs = modelService.getResourceByType("VF");

        for (Entry<String, JsonElement> entry : modelVnfs.entrySet()) {
            var vnfOneOfSchema = new JsonObject();
            vnfOneOfSchema.addProperty(TITLE, "VNF" + "-" + entry.getKey());
            var properties = new JsonObject();
            if (generateType) {
                properties.add(TYPE, createSchemaProperty("Type", STRING, "VNF", "True", null));
            }
            properties.add("resourceID", createSchemaProperty("Resource ID", STRING,
                    modelVnfs.get(entry.getKey()).getAsJsonObject().get("invariantUUID").getAsString(), "True", null));

            vnfOneOfSchema.add(PROPERTIES, properties);
            vnfSchemaArray.add(vnfOneOfSchema);
        }
        return vnfSchemaArray;
    }

    private static JsonArray createBlankEntry() {
        var result = new JsonArray();
        var blankObject = new JsonObject();
        blankObject.addProperty(TITLE, "User defined");
        blankObject.add(PROPERTIES, new JsonObject());
        result.add(blankObject);
        return result;
    }

    private static JsonArray createVfModuleSchema(Service modelService, boolean generateType) {
        var vfModuleOneOfSchemaArray = new JsonArray();
        JsonObject modelVfModules = modelService.getResourceByType("VFModule");

        for (Entry<String, JsonElement> entry : modelVfModules.entrySet()) {
            var vfModuleOneOfSchema = new JsonObject();
            vfModuleOneOfSchema.addProperty(TITLE, "VFMODULE" + "-" + entry.getKey());
            var properties = new JsonObject();
            if (generateType) {
                properties.add(TYPE, createSchemaProperty("Type", STRING, "VFMODULE", "True", null));
            }
            properties.add("resourceID",
                    createSchemaProperty("Resource ID", STRING,
                            modelVfModules.get(entry.getKey()).getAsJsonObject().get("vfModuleModelName").getAsString(),
                            "True", null));
            properties.add("modelInvariantId",
                    createSchemaProperty("Model Invariant Id (ModelInvariantUUID)", STRING,
                            modelVfModules.get(entry.getKey()).getAsJsonObject().get("vfModuleModelInvariantUUID")
                                    .getAsString(),
                            "True", null));
            properties.add("modelVersionId",
                    createSchemaProperty("Model Version Id (ModelUUID)", STRING,
                            modelVfModules.get(entry.getKey()).getAsJsonObject().get("vfModuleModelUUID").getAsString(),
                            "True", null));
            properties.add("modelName",
                    createSchemaProperty("Model Name", STRING,
                            modelVfModules.get(entry.getKey()).getAsJsonObject().get("vfModuleModelName").getAsString(),
                            "True", null));
            properties.add("modelVersion", createSchemaProperty("Model Version", STRING,
                    modelVfModules.get(entry.getKey()).getAsJsonObject().get("vfModuleModelVersion").getAsString(),
                    "True", null));
            properties
                    .add("modelCustomizationId",
                            createSchemaProperty("Customization ID", STRING,
                                    modelVfModules.get(entry.getKey()).getAsJsonObject()
                                            .get("vfModuleModelCustomizationUUID").getAsString(), "True",
                                    null));

            vfModuleOneOfSchema.add(PROPERTIES, properties);
            vfModuleOneOfSchemaArray.add(vfModuleOneOfSchema);
        }
        return vfModuleOneOfSchemaArray;
    }

    /**
     * Create an anyOf array of possible structure we may have for Target.
     *
     * @param modelJson The service object
     * @return A JsonArray with everything inside
     */
    public static JsonArray createAnyOfArray(Service modelJson, boolean generateType) {
        var targetOneOfStructure = new JsonArray();
        // First entry must be user defined
        targetOneOfStructure.addAll(createBlankEntry());
        targetOneOfStructure.addAll(createVnfSchema(modelJson, generateType));
        targetOneOfStructure.addAll(createVfModuleSchema(modelJson, generateType));
        return targetOneOfStructure;
    }

    private static JsonArray createAnyOfArrayForCdsRecipe(Service modelJson) {
        var anyOfStructure = new JsonArray();
        anyOfStructure.addAll(createAnyOfCdsRecipe(modelJson.getResourceDetails().getAsJsonObject("VF")));
        anyOfStructure.addAll(createAnyOfCdsRecipe(modelJson.getResourceDetails().getAsJsonObject("PNF")));
        return anyOfStructure;
    }

    private static JsonArray createAnyOfCdsRecipe(JsonObject jsonObject) {
        var schemaArray = new JsonArray();
        for (Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            var controllerProperties = entry.getValue().getAsJsonObject()
                    .getAsJsonObject("controllerProperties");

            if (controllerProperties != null && controllerProperties.getAsJsonObject("workflows") != null) {
                var workflows = controllerProperties.getAsJsonObject("workflows");
                for (Entry<String, JsonElement> workflowsEntry : workflows.entrySet()) {
                    var obj = new JsonObject();
                    obj.addProperty(TITLE, workflowsEntry.getKey());
                    obj.addProperty(TYPE, TYPE_OBJECT);
                    obj.add(PROPERTIES, createPayloadProperty(workflowsEntry.getValue().getAsJsonObject(),
                            controllerProperties, workflowsEntry.getKey()));
                    schemaArray.add(obj);
                }

            }
        }
        return schemaArray;
    }

    private static JsonObject createPayloadProperty(JsonObject workFlow,
                                                    JsonObject controllerProperties, String workFlowName) {
        var payload = new JsonObject();
        payload.addProperty(TITLE, "Payload");
        payload.addProperty(TYPE, TYPE_OBJECT);
        payload.add(PROPERTIES, createInputPropertiesForPayload(workFlow, controllerProperties,
                                                                workFlowName));
        var properties = new JsonObject();
        properties.add(RECIPE, createRecipeForCdsWorkflow(workFlowName));
        properties.add("payload", payload);
        return properties;
    }

    private static JsonObject createRecipeForCdsWorkflow(String workflow) {
        var recipe = new JsonObject();
        recipe.addProperty(TITLE, RECIPE);
        recipe.addProperty(TYPE, STRING);
        recipe.addProperty(DEFAULT, workflow);
        var options = new JsonObject();
        options.addProperty("hidden", true);
        recipe.add("options", options);
        return recipe;
    }

    /**
     * Returns the properties of payload based on the cds work flows.
     *
     * @param workFlow             cds work flows to update payload
     * @param controllerProperties cds properties to get blueprint name and
     *                             version
     * @param workFlowName         work flow name
     * @return returns the properties of payload
     */
    public static JsonObject createInputPropertiesForPayload(JsonObject workFlow,
                                                             JsonObject controllerProperties,
                                                             String workFlowName) {
        var artifactName = controllerProperties.get("sdnc_model_name").getAsString();
        var artifactVersion = controllerProperties.get("sdnc_model_version").getAsString();
        var inputs = workFlow.getAsJsonObject("inputs");
        var jsonObject = new JsonObject();
        jsonObject.add("artifact_name", createSchemaProperty(
                "artifact name", STRING, artifactName, "True", null));
        jsonObject.add("artifact_version", createSchemaProperty(
                "artifact version", STRING, artifactVersion, "True", null));
        jsonObject.add("mode", createCdsInputProperty(
                "mode", STRING, "async", null));
        jsonObject.add("data", createDataProperty(inputs, workFlowName));
        return jsonObject;
    }

    private static JsonObject createDataProperty(JsonObject inputs, String workflowName) {
        var data = new JsonObject();
        data.addProperty(TITLE, "data");
        var dataObj = new JsonObject();
        addDataFields(inputs, dataObj, workflowName);
        data.add(PROPERTIES, dataObj);
        return data;
    }

    private static void addDataFields(JsonObject inputs,
                                      JsonObject dataObj,
                                      String workFlowName) {
        Set<Map.Entry<String, JsonElement>> entrySet = inputs.entrySet();
        for (Map.Entry<String, JsonElement> entry : entrySet) {
            String key = entry.getKey();
            var inputProperty = inputs.getAsJsonObject(key);
            if (key.equalsIgnoreCase(workFlowName + "-properties")) {
                addDataFields(entry.getValue().getAsJsonObject().get(PROPERTIES).getAsJsonObject(),
                        dataObj, workFlowName);
            } else {
                dataObj.add(entry.getKey(),
                        createCdsInputProperty(key, inputProperty.get(TYPE).getAsString(), null,
                                entry.getValue().getAsJsonObject()));
            }
        }
    }

    private static JsonObject createCdsInputProperty(String title,
                                                     String type,
                                                     String defaultValue,
                                                     JsonObject cdsProperty) {
        var property = new JsonObject();
        property.addProperty(TITLE, title);

        if (TYPE_LIST.equalsIgnoreCase(type)) {
            property.addProperty(TYPE, TYPE_ARRAY);
            if (cdsProperty != null && cdsProperty.get(PROPERTIES) != null) {
                var listProperties = new JsonObject();
                listProperties.add(PROPERTIES, getProperties(cdsProperty.get(PROPERTIES).getAsJsonObject()));
                property.add(ITEMS, listProperties);
            }
        } else if (cdsProperty != null && TYPE_OBJECT.equalsIgnoreCase(type)) {
            property.addProperty(TYPE, TYPE_OBJECT);
            property.add(PROPERTIES, getProperties(cdsProperty.get(PROPERTIES).getAsJsonObject()));
        } else {
            property.addProperty(TYPE, type);
        }

        if (defaultValue != null) {
            property.addProperty(DEFAULT, defaultValue);
        }
        return property;
    }

    private static JsonObject getProperties(JsonObject inputProperties) {
        if (inputProperties == null) {
            return null;
        }
        var dataObject = new JsonObject();
        addDataFields(inputProperties, dataObject, null);
        return dataObject;
    }
}
