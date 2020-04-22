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

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.onap.clamp.clds.util.JsonUtils;
import org.onap.clamp.clds.util.ResourceFileUtil;
import org.onap.clamp.loop.service.Service;

public class OperationalPolicyRepresentationBuilder {

    private static final EELFLogger logger =
            EELFManager.getInstance().getLogger(OperationalPolicyRepresentationBuilder.class);

    public static final String PROPERTIES = "properties";
    public static final String ITEMS = "items";
    public static final String ANY_OF = "anyOf";
    public static final String TITLE = "title";
    public static final String RECIPE = "recipe";
    public static final String DEFAULT = "default";
    public static final String STRING = "string";
    public static final String TYPE = "type";
    public static final String TYPE_LIST = "list";

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
                    ResourceFileUtil
                            .getResourceAsString("clds/json-schema/operational_policies/operational_policy.json"),
                    JsonObject.class);
            jsonSchema.get(PROPERTIES).getAsJsonObject()
                    .get("operational_policy").getAsJsonObject().get(PROPERTIES).getAsJsonObject().get("policies")
                    .getAsJsonObject().get(ITEMS).getAsJsonObject().get(PROPERTIES).getAsJsonObject().get("target")
                    .getAsJsonObject().get(ANY_OF).getAsJsonArray().addAll(createAnyOfArray(modelJson, true));

            // update CDS recipe and payload information to schema
            JsonArray actors = jsonSchema.get(PROPERTIES).getAsJsonObject()
                    .get("operational_policy").getAsJsonObject().get(PROPERTIES).getAsJsonObject().get("policies")
                    .getAsJsonObject().get(ITEMS).getAsJsonObject().get(PROPERTIES).getAsJsonObject().get("actor")
                    .getAsJsonObject().get(ANY_OF).getAsJsonArray();

            for (JsonElement actor : actors) {
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
        JsonObject property = new JsonObject();
        property.addProperty(TITLE, title);
        property.addProperty(TYPE, type);
        property.addProperty(DEFAULT, defaultValue);
        property.addProperty("readOnly", readOnlyFlag);

        if (enumArray != null) {
            JsonArray jsonArray = new JsonArray();
            property.add("enum", jsonArray);
            for (String val : enumArray) {
                jsonArray.add(val);
            }
        }
        return property;
    }

    private static JsonArray createVnfSchema(Service modelService, boolean generateType) {
        JsonArray vnfSchemaArray = new JsonArray();
        JsonObject modelVnfs = modelService.getResourceByType("VF");

        for (Entry<String, JsonElement> entry : modelVnfs.entrySet()) {
            JsonObject vnfOneOfSchema = new JsonObject();
            vnfOneOfSchema.addProperty(TITLE, "VNF" + "-" + entry.getKey());
            JsonObject properties = new JsonObject();
            if (generateType) {
                properties.add(TYPE, createSchemaProperty("Type", STRING, "VNF", "True", null));
            }
            properties.add("resourceID", createSchemaProperty("Resource ID", STRING,
                    modelVnfs.get(entry.getKey()).getAsJsonObject().get("name").getAsString(), "True", null));

            vnfOneOfSchema.add(PROPERTIES, properties);
            vnfSchemaArray.add(vnfOneOfSchema);
        }
        return vnfSchemaArray;
    }

    private static JsonArray createVfModuleSchema(Service modelService, boolean generateType) {
        JsonArray vfModuleOneOfSchemaArray = new JsonArray();
        JsonObject modelVfModules = modelService.getResourceByType("VFModule");

        for (Entry<String, JsonElement> entry : modelVfModules.entrySet()) {
            JsonObject vfModuleOneOfSchema = new JsonObject();
            vfModuleOneOfSchema.addProperty(TITLE, "VFMODULE" + "-" + entry.getKey());
            JsonObject properties = new JsonObject();
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
        JsonArray targetOneOfStructure = new JsonArray();
        targetOneOfStructure.addAll(createVnfSchema(modelJson, generateType));
        targetOneOfStructure.addAll(createVfModuleSchema(modelJson, generateType));
        return targetOneOfStructure;
    }

    private static JsonArray createAnyOfArrayForCdsRecipe(Service modelJson) {
        JsonArray anyOfStructure = new JsonArray();
        anyOfStructure.addAll(createAnyOfCdsRecipe(modelJson.getResourceDetails().getAsJsonObject("VF")));
        anyOfStructure.addAll(createAnyOfCdsRecipe(modelJson.getResourceDetails().getAsJsonObject("PNF")));
        return anyOfStructure;
    }

    private static JsonArray createAnyOfCdsRecipe(JsonObject jsonObject) {
        JsonArray schemaArray = new JsonArray();
        for (Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            JsonObject controllerProperties = entry.getValue().getAsJsonObject()
                    .getAsJsonObject("controllerProperties");

            if (controllerProperties != null) {
                JsonObject workflows = controllerProperties.getAsJsonObject("workflows");
                for (Entry<String, JsonElement> workflowsEntry : workflows.entrySet()) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty(TITLE, workflowsEntry.getKey());
                    obj.addProperty(TYPE, "object");
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
        JsonObject payload = new JsonObject();
        payload.addProperty(TITLE, "Payload");
        payload.addProperty(TYPE, "object");
        payload.add(PROPERTIES, createInputPropertiesForPayload(workFlow,
                                                                  controllerProperties));
        JsonObject properties = new JsonObject();
        properties.add(RECIPE, createRecipeForCdsWorkflow(workFlowName));
        properties.add("payload", payload);
        return properties;
    }

    private static JsonObject createRecipeForCdsWorkflow(String workflow) {
        JsonObject recipe = new JsonObject();
        recipe.addProperty(TITLE, RECIPE);
        recipe.addProperty(TYPE, STRING);
        recipe.addProperty(DEFAULT, workflow);
        JsonObject options = new JsonObject();
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
     * @return returns the properties of payload
     */
    public static JsonObject createInputPropertiesForPayload(JsonObject workFlow,
                                                             JsonObject controllerProperties) {
        String artifactName = controllerProperties.get("sdnc_model_name").getAsString();
        String artifactVersion = controllerProperties.get("sdnc_model_version").getAsString();
        JsonObject inputs = workFlow.getAsJsonObject("inputs");
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("artifact_name", createSchemaProperty(
                "artifact name", STRING, artifactName, "True", null));
        jsonObject.add("artifact_version", createSchemaProperty(
                "artifact version", STRING, artifactVersion, "True", null));
        jsonObject.add("mode", createCdsInputProperty(
                "mode", STRING, "async" ,null));
        jsonObject.add("data", createDataProperty(inputs));
        return jsonObject;
    }

    private static JsonObject createDataProperty(JsonObject inputs) {
        JsonObject data = new JsonObject();
        data.addProperty(TITLE, "data");
        JsonObject dataObj = new JsonObject();
        addDataFields(inputs, dataObj);
        data.add(PROPERTIES, dataObj);
        return data;
    }

    private static void addDataFields(JsonObject inputs,
                                      JsonObject dataObj) {
        Set<Map.Entry<String, JsonElement>> entrySet = inputs.entrySet();
        for (Map.Entry<String, JsonElement> entry : entrySet) {
            String key = entry.getKey();
            JsonObject inputProperty = inputs.getAsJsonObject(key);
            if (inputProperty.get(TYPE) == null) {
                addDataFields(entry.getValue().getAsJsonObject(), dataObj);
            } else {
                dataObj.add(entry.getKey(),
                            createCdsInputProperty(key,
                                                   inputProperty.get(TYPE).getAsString(),
                                                   null,
                                                   entry.getValue().getAsJsonObject()));
            }
        }
    }

    private static JsonObject createCdsInputProperty(String title,
                                                     String type,
                                                     String defaultValue,
                                                     JsonObject cdsProperty) {
        JsonObject property = new JsonObject();
        property.addProperty(TITLE, title);

        if (TYPE_LIST.equalsIgnoreCase(type)) {
            property.addProperty(TYPE, "array");
            if (cdsProperty.get(PROPERTIES) != null) {
                JsonObject dataObject = new JsonObject();
                addDataFields(cdsProperty.get(PROPERTIES).getAsJsonObject(),
                              dataObject);
                JsonObject listProperties = new JsonObject();
                listProperties.add(PROPERTIES, dataObject);
                property.add(ITEMS, listProperties);
            }
        } else {
            property.addProperty(TYPE, type);
        }

        if (defaultValue != null) {
            property.addProperty(DEFAULT, defaultValue);
        }
        return property;
    }
}
