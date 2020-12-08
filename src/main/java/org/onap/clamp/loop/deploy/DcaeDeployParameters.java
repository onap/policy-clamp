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

package org.onap.clamp.loop.deploy;

import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.onap.clamp.clds.util.JsonUtils;
import org.onap.clamp.loop.Loop;
import org.onap.clamp.loop.components.external.DcaeComponent;
import org.onap.clamp.policy.microservice.MicroServicePolicy;
import org.yaml.snakeyaml.Yaml;

/**
 * To decode the bluprint input parameters.
 */
public class DcaeDeployParameters {

    private static LinkedHashMap<String, JsonObject> init(Loop loop) {
        LinkedHashMap<String, JsonObject> deploymentParamMap = new LinkedHashMap<>();
        Set<MicroServicePolicy> microServiceList = loop.getMicroServicePolicies();

        for (MicroServicePolicy microService : microServiceList) {
            deploymentParamMap.put(microService.getName(),
                    generateDcaeDeployParameter(microService));
        }
        return deploymentParamMap;
    }

    private static JsonObject generateDcaeDeployParameter(MicroServicePolicy microService) {
        return generateDcaeDeployParameter(microService.getLoopElementModel().getBlueprint(),
                microService.getName());
    }

    private static JsonObject generateDcaeDeployParameter(String blueprint, String policyId) {
        JsonObject deployJsonBody = new JsonObject();
        Yaml yaml = new Yaml();
        Map<String, Object> inputsNodes = ((Map<String, Object>) ((Map<String, Object>) yaml
                .load(blueprint)).get("inputs"));
        inputsNodes.entrySet().stream().filter(e -> !e.getKey().contains("policy_id")).forEach(elem -> {
            Object defaultValue = ((Map<String, Object>) elem.getValue()).get("default");
            if (defaultValue != null) {
                addPropertyToNode(deployJsonBody, elem.getKey(), defaultValue);
            }
            else {
                deployJsonBody.addProperty(elem.getKey(), "");
            }
        });
        deployJsonBody.addProperty("policy_id", policyId);
        return deployJsonBody;
    }

    private static void addPropertyToNode(JsonObject node, String key, Object value) {
        if (value instanceof String) {
            node.addProperty(key, (String) value);
        }
        else if (value instanceof Number) {
            node.addProperty(key, (Number) value);
        }
        else if (value instanceof Boolean) {
            node.addProperty(key, (Boolean) value);
        }
        else if (value instanceof Character) {
            node.addProperty(key, (Character) value);
        }
        else {
            node.addProperty(key, JsonUtils.GSON.toJson(value));
        }
    }

    /**
     * Convert the object in Json.
     *
     * @return The deploymentParameters in Json
     */
    public static JsonObject getDcaeDeploymentParametersInJson(Loop loop) {
        JsonObject globalProperties = new JsonObject();
        JsonObject deployParamJson = new JsonObject();
        if (loop.getLoopTemplate().getUniqueBlueprint()) {
            // Normally the unique blueprint could contain multiple microservices but then we can't guess
            // the policy id params that will be used, so here we expect only one by default.
            deployParamJson.add(DcaeComponent.UNIQUE_BLUEPRINT_PARAMETERS,
                    generateDcaeDeployParameter(loop.getLoopTemplate().getBlueprint(),
                            ((MicroServicePolicy) loop.getMicroServicePolicies().toArray()[0]).getName()));

        }
        else {
            LinkedHashMap<String, JsonObject> deploymentParamMap = init(loop);
            for (Map.Entry<String, JsonObject> mapElement : deploymentParamMap.entrySet()) {
                deployParamJson.add(mapElement.getKey(), mapElement.getValue());
            }
        }
        globalProperties.add("dcaeDeployParameters", deployParamJson);
        return globalProperties;
    }

}
