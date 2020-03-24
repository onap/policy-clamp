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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import org.onap.clamp.clds.tosca.update.execution.ToscaMetadataProcess;
import org.onap.clamp.loop.service.Service;
import org.onap.clamp.tosca.DictionaryService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class is there to add the JsonObject for CDS in the json Schema according to what is found in the Tosca model.
 */
public class ToscaMetadataCdsProcess extends ToscaMetadataProcess {

    @Autowired
    private DictionaryService dictionaryService;

    @Override
    public void executeProcess(String parameters, JsonObject childObject, Service serviceModel) {
        switch (parameters) {
            case "payload":
                childObject.add("anyOf", generatePayload(serviceModel));
                break;
            case "operation":
                childObject.add("enum", generateOperation(serviceModel));
                break;
        }
    }

    private static JsonArray generatePayload(Service serviceModel) {
        JsonArray schemaAnyOf = new JsonArray();
        schemaAnyOf.addAll(generatePayloadPerResource("VF", serviceModel));
        schemaAnyOf.addAll(generatePayloadPerResource("PNF", serviceModel));
        return schemaAnyOf;
    }

    private static JsonArray generateOperation(Service serviceModel) {
        JsonArray schemaEnum = new JsonArray();
        schemaEnum.addAll(generateOperationPerResource("VF", serviceModel));
        schemaEnum.addAll(generateOperationPerResource("PNF", serviceModel));
        return schemaEnum;
    }

    private static JsonArray generateOperationPerResource(String resourceName, Service serviceModel) {
        JsonArray schemaEnum = new JsonArray();
        for (Map.Entry<String, JsonElement> entry : serviceModel.getResourceDetails().getAsJsonObject(resourceName)
                .entrySet()) {
            JsonObject controllerProperties = entry.getValue().getAsJsonObject()
                    .getAsJsonObject("controllerProperties");
            if (controllerProperties != null) {
                for (String workflowsEntry : controllerProperties.getAsJsonObject("workflows").keySet()) {
                    schemaEnum.add(workflowsEntry);
                }
            }
        }
        return schemaEnum;
    }

    private static JsonArray generatePayloadPerResource(String resourceName, Service serviceModel) {
        JsonArray schemaAnyOf = new JsonArray();
        for (Map.Entry<String, JsonElement> entry : serviceModel.getResourceDetails().getAsJsonObject(resourceName)
                .entrySet()) {
            JsonObject controllerProperties = entry.getValue().getAsJsonObject()
                    .getAsJsonObject("controllerProperties");
            if (controllerProperties != null) {
                for (Map.Entry<String, JsonElement> workflowsEntry : controllerProperties.getAsJsonObject("workflows")
                        .entrySet()) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("title", workflowsEntry.getKey());
                    obj.add("properties", createPayloadProperty(workflowsEntry.getValue().getAsJsonObject(),
                            controllerProperties));
                    schemaAnyOf.add(obj);
                }
            }
        }
        return schemaAnyOf;
    }

    private static JsonObject createPayloadProperty(JsonObject workFlow, JsonObject controllerProperties) {
        JsonObject payloadResult = new JsonObject();

        payloadResult.addProperty("artifact_name", controllerProperties.get("sdnc_model_name").getAsString());
        payloadResult.addProperty("artifact_version", controllerProperties.get("sdnc_model_version").getAsString());
        payloadResult.addProperty("mode", "async");
        payloadResult.add("data", workFlow.getAsJsonObject("inputs"));
        return payloadResult;
    }
}
