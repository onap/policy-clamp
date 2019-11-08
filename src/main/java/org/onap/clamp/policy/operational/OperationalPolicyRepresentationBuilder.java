/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights
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

package org.onap.clamp.policy.operational;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.Map.Entry;

import org.onap.clamp.clds.util.JsonUtils;
import org.onap.clamp.clds.util.ResourceFileUtil;
import org.onap.clamp.loop.service.Service;

public class OperationalPolicyRepresentationBuilder {

    /**
     * This method generates the operational policy json representation that will be
     * used by ui for rendering. It uses the model (VF and VFModule) defined in the
     * loop object to do so, so it's dynamic. It also uses the operational policy
     * schema template defined in the resource folder.
     * 
     * @param modelJson The loop model json
     * @return The json representation
     * @throws JsonSyntaxException If the schema template cannot be parsed
     * @throws IOException         In case of issue when opening the schema template
     */
    public static JsonObject generateOperationalPolicySchema(Service modelJson)
            throws JsonSyntaxException, IOException {
        JsonObject jsonSchema = JsonUtils.GSON.fromJson(
                ResourceFileUtil.getResourceAsString("clds/json-schema/operational_policies/operational_policy.json"),
                JsonObject.class);
        jsonSchema.get("schema").getAsJsonObject().get("items").getAsJsonObject().get("properties").getAsJsonObject()
                .get("configurationsJson").getAsJsonObject().get("properties").getAsJsonObject()
                .get("operational_policy").getAsJsonObject().get("properties").getAsJsonObject().get("policies")
                .getAsJsonObject().get("items").getAsJsonObject().get("properties").getAsJsonObject().get("target")
                .getAsJsonObject().get("anyOf").getAsJsonArray().addAll(createAnyOfArray(modelJson));
        return jsonSchema;
    }

    private static JsonObject createSchemaProperty(String title, String type, String defaultValue, String readOnlyFlag,
            String[] enumArray) {
        JsonObject property = new JsonObject();
        property.addProperty("title", title);
        property.addProperty("type", type);
        property.addProperty("default", defaultValue);
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

    private static JsonArray createVnfSchema(Service modelService) {
        JsonArray vnfSchemaArray = new JsonArray();
        JsonObject modelVnfs = modelService.getResourceByType("VF");

        for (Entry<String, JsonElement> entry : modelVnfs.entrySet()) {
            JsonObject vnfOneOfSchema = new JsonObject();
            vnfOneOfSchema.addProperty("title", "VNF" + "-" + entry.getKey());
            JsonObject properties = new JsonObject();
            properties.add("type", createSchemaProperty("Type", "string", "VNF", "True", null));
            properties.add("resourceID", createSchemaProperty("Resource ID", "string",
                    modelVnfs.get(entry.getKey()).getAsJsonObject().get("name").getAsString(), "True", null));

            vnfOneOfSchema.add("properties", properties);
            vnfSchemaArray.add(vnfOneOfSchema);
        }
        return vnfSchemaArray;
    }

    private static JsonArray createVfModuleSchema(Service modelService) {
        JsonArray vfModuleOneOfSchemaArray = new JsonArray();
        JsonObject modelVfModules = modelService.getResourceByType("VFModule");

        for (Entry<String, JsonElement> entry : modelVfModules.entrySet()) {
            JsonObject vfModuleOneOfSchema = new JsonObject();
            vfModuleOneOfSchema.addProperty("title", "VFMODULE" + "-" + entry.getKey());
            JsonObject properties = new JsonObject();
            properties.add("type", createSchemaProperty("Type", "string", "VFMODULE", "True", null));
            properties.add("resourceID",
                    createSchemaProperty("Resource ID", "string",
                            modelVfModules.get(entry.getKey()).getAsJsonObject().get("vfModuleModelName").getAsString(),
                            "True", null));
            properties.add("modelInvariantId",
                    createSchemaProperty("Model Invariant Id (ModelInvariantUUID)", "string", modelVfModules
                            .get(entry.getKey()).getAsJsonObject().get("vfModuleModelInvariantUUID").getAsString(),
                            "True", null));
            properties.add("modelVersionId",
                    createSchemaProperty("Model Version Id (ModelUUID)", "string",
                            modelVfModules.get(entry.getKey()).getAsJsonObject().get("vfModuleModelUUID").getAsString(),
                            "True", null));
            properties.add("modelName",
                    createSchemaProperty("Model Name", "string",
                            modelVfModules.get(entry.getKey()).getAsJsonObject().get("vfModuleModelName").getAsString(),
                            "True", null));
            properties.add("modelVersion", createSchemaProperty("Model Version", "string",
                    modelVfModules.get(entry.getKey()).getAsJsonObject().get("vfModuleModelVersion").getAsString(),
                    "True", null));
            properties
                    .add("modelCustomizationId",
                            createSchemaProperty("Customization ID", "string", modelVfModules.get(entry.getKey())
                                    .getAsJsonObject().get("vfModuleModelCustomizationUUID").getAsString(), "True",
                                    null));

            vfModuleOneOfSchema.add("properties", properties);
            vfModuleOneOfSchemaArray.add(vfModuleOneOfSchema);
        }
        return vfModuleOneOfSchemaArray;
    }

    private static JsonArray createAnyOfArray(Service modelJson) {
        JsonArray targetOneOfStructure = new JsonArray();
        targetOneOfStructure.addAll(createVnfSchema(modelJson));
        targetOneOfStructure.addAll(createVfModuleSchema(modelJson));
        return targetOneOfStructure;
    }
}
