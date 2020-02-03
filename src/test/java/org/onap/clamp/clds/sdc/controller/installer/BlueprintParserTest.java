/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 Nokia Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 * Modifications Copyright (c) 2019 Samsung
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

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.clamp.clds.exception.sdc.controller.BlueprintParserException;
import org.onap.clamp.clds.util.ResourceFileUtil;
import org.yaml.snakeyaml.Yaml;

public class BlueprintParserTest {
    private static final Gson GSON = new Gson();
    private static final String FIRST_APPP = "first_app";
    private static final String SECOND_APPP = "second_app";
    private static final String THIRD_APPP = "third_app";
    private static final String MODEL_TYPE1 = "type1";
    private static final String MODEL_TYPE_TCA = "onap.policies.monitoring.cdap.tca.hi.lo.app";
    private static final String VERSION = "1.0.0";

    private static String microServiceTheWholeBlueprintValid;
    private static String newMicroServiceBlueprint;
    private static JsonObject jsonObjectBlueprintInvalid;
    private static JsonObject jsonObjectBlueprintWithoutName;
    private static JsonObject jsonObjectBlueprintWithoutProperties;
    private static JsonObject jsonObjectBlueprintWithoutRelationships;
    private static JsonObject jsonObjectBlueprintValidWithVersion;

    /**
     * Method to load Blueprints before all test.
     *
     * @throws IOException In case of issues when opening the files
     */
    @BeforeClass
    public static void loadBlueprints() throws IOException {
        microServiceTheWholeBlueprintValid = ResourceFileUtil
                .getResourceAsString("clds/blueprint-with-microservice-chain.yaml");

        newMicroServiceBlueprint = ResourceFileUtil.getResourceAsString("clds/new-microservice.yaml");

        String microServiceBlueprintInvalid = ResourceFileUtil
                .getResourceAsString("clds/single-microservice-fragment-invalid.yaml");
        jsonObjectBlueprintInvalid = yamlToJson(microServiceBlueprintInvalid);
        String microServiceBlueprintWithoutName = ResourceFileUtil
                .getResourceAsString("clds/single-microservice-fragment-without-name.yaml");
        jsonObjectBlueprintWithoutName = yamlToJson(microServiceBlueprintWithoutName);
        String microServiceBlueprintWithoutProperties = ResourceFileUtil
                .getResourceAsString("clds/single-microservice-fragment-without-properties.yaml");
        jsonObjectBlueprintWithoutProperties = yamlToJson(microServiceBlueprintWithoutProperties);
        String microServiceBlueprintValidWithVersion = ResourceFileUtil
                .getResourceAsString("clds/single-microservice-fragment-valid-with-version.yaml");
        jsonObjectBlueprintValidWithVersion = yamlToJson(microServiceBlueprintValidWithVersion);

        String microServiceBlueprintWithoutRelationships = ResourceFileUtil
                .getResourceAsString("clds/single-microservice-fragment-without-relationships.yaml");
        jsonObjectBlueprintWithoutRelationships = yamlToJson(microServiceBlueprintWithoutRelationships);

    }

    @Test
    public void getNameShouldReturnDefinedName() {
        final JsonObject jsonObject = jsonObjectBlueprintInvalid;
        String expectedName = jsonObject.get(jsonObject.keySet().iterator().next()).getAsJsonObject().get("properties")
                .getAsJsonObject().get("name").getAsString();
        Entry<String, JsonElement> entry = jsonObject.entrySet().iterator().next();
        String actualName = BlueprintParser.getName(entry);

        Assert.assertEquals(expectedName, actualName);
    }

    @Test
    public void getNameShouldReturnServiceNameWhenNoNameDefined() {
        final JsonObject jsonObject = jsonObjectBlueprintWithoutName;

        String expectedName = jsonObject.keySet().iterator().next();
        Entry<String, JsonElement> entry = jsonObject.entrySet().iterator().next();
        String actualName = BlueprintParser.getName(entry);

        Assert.assertEquals(expectedName, actualName);
    }

    @Test
    public void getNameShouldReturnServiceNameWhenNoPropertiesDefined() {
        final JsonObject jsonObject = jsonObjectBlueprintWithoutProperties;

        String expectedName = jsonObject.keySet().iterator().next();
        Entry<String, JsonElement> entry = jsonObject.entrySet().iterator().next();
        String actualName = BlueprintParser.getName(entry);

        Assert.assertEquals(expectedName, actualName);
    }

    @Test
    public void getInputShouldReturnInputWhenPresent() {
        final JsonObject jsonObject = jsonObjectBlueprintInvalid;

        String expected = FIRST_APPP;
        Entry<String, JsonElement> entry = jsonObject.entrySet().iterator().next();
        String actual = BlueprintParser.getInput(entry);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void getInputShouldReturnEmptyStringWhenAbsent() {
        final JsonObject jsonObject = jsonObjectBlueprintWithoutRelationships;

        String expected = "";
        Entry<String, JsonElement> entry = jsonObject.entrySet().iterator().next();
        String actual = BlueprintParser.getInput(entry);

        Assert.assertEquals(expected, actual);
    }

    @Test(expected = BlueprintParserException.class)
    public void getNodeRepresentationFromIncompleteYaml() throws BlueprintParserException {
        BlueprintParser.getNodeRepresentation(jsonObjectBlueprintInvalid.entrySet().iterator().next(),
                jsonObjectBlueprintInvalid, null);
    }

    @Test
    public void getNodeRepresentationFromCompleteYamlWithModelVersion() throws BlueprintParserException {
        final JsonObject jsonObject = jsonObjectBlueprintValidWithVersion;

        BlueprintMicroService expected = new BlueprintMicroService(SECOND_APPP, MODEL_TYPE1, "", "10.0.0");
        Entry<String, JsonElement> entry = jsonObject.entrySet().iterator().next();
        BlueprintMicroService actual = BlueprintParser.getNodeRepresentation(entry, jsonObject, null);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void getMicroServicesFromBlueprintTest() throws BlueprintParserException {
        BlueprintMicroService thirdApp = new BlueprintMicroService(THIRD_APPP, MODEL_TYPE_TCA, SECOND_APPP, VERSION);
        BlueprintMicroService firstApp = new BlueprintMicroService(FIRST_APPP, MODEL_TYPE_TCA, "", VERSION);
        BlueprintMicroService secondApp = new BlueprintMicroService(SECOND_APPP, MODEL_TYPE_TCA, FIRST_APPP, VERSION);

        Set<BlueprintMicroService> expected = new HashSet<>(Arrays.asList(firstApp, secondApp, thirdApp));
        Set<BlueprintMicroService> actual = BlueprintParser.getMicroServices(microServiceTheWholeBlueprintValid);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void fallBackToOneMicroServiceTcaTest() {
        BlueprintMicroService tcaMs = new BlueprintMicroService(BlueprintParser.TCA,
                "onap.policies.monitoring.cdap.tca.hi.lo.app", "", VERSION);
        List<BlueprintMicroService> expected = Collections.singletonList(tcaMs);
        List<BlueprintMicroService> actual = BlueprintParser.fallbackToOneMicroService();

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void newMicroServiceTest() throws BlueprintParserException {
        List<BlueprintMicroService> microServicesChain = new ChainGenerator()
                .getChainOfMicroServices(BlueprintParser.getMicroServices(newMicroServiceBlueprint));
        if (microServicesChain.isEmpty()) {
            microServicesChain = BlueprintParser.fallbackToOneMicroService();
        }
        assertThat(microServicesChain.size()).isEqualTo(1);
        assertThat(microServicesChain.get(0).getName()).isEqualTo("pmsh");
    }

    private static JsonObject yamlToJson(String yamlString) {
        Yaml yaml = new Yaml();
        Map<String, Object> map = yaml.load(yamlString);
        JSONObject jsonObject = new JSONObject(map);
        return GSON.fromJson(jsonObject.toString(), JsonObject.class);
    }
}
