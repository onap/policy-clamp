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
import org.onap.clamp.clds.util.ResourceFileUtil;
import org.yaml.snakeyaml.Yaml;

public class BlueprintParserTest {
    private static final Gson GSON = new Gson();
    private static final String FIRST_APPP = "first_app";
    private static final String SECOND_APPP = "second_app";
    private static final String THIRD_APPP = "third_app";

    private static String microServiceTheWholeBlueprintValid;
    private static String microServiceBlueprintOldStyleTCA;
    private static String microServiceBlueprintOldStyleHolmes;

    private static JsonObject jsonObjectBlueprintValid;
    private static JsonObject jsonObjectBlueprintWithoutName;
    private static JsonObject jsonObjectBlueprintWithoutProperties;
    private static JsonObject jsonObjectBlueprintWithoutRelationships;

    @BeforeClass
    public static void loadBlueprints() throws IOException {
        microServiceTheWholeBlueprintValid = ResourceFileUtil
            .getResourceAsString("clds/blueprint-with-microservice-chain.yaml");
        microServiceBlueprintOldStyleTCA = ResourceFileUtil
            .getResourceAsString("clds/tca-old-style-ms.yaml");
        microServiceBlueprintOldStyleHolmes = ResourceFileUtil
            .getResourceAsString("clds/holmes-old-style-ms.yaml");

        String microServiceBlueprintValid = ResourceFileUtil
            .getResourceAsString("clds/single-microservice-fragment-valid.yaml");
        String microServiceBlueprintWithoutName = ResourceFileUtil
            .getResourceAsString("clds/single-microservice-fragment-without-name.yaml");
        String microServiceBlueprintWithoutProperties = ResourceFileUtil
            .getResourceAsString("clds/single-microservice-fragment-without-properties.yaml");
        String microServiceBlueprintWithoutRelationships = ResourceFileUtil
            .getResourceAsString("clds/single-microservice-fragment-without-relationships.yaml");

        jsonObjectBlueprintValid = yamlToJson(microServiceBlueprintValid);
        jsonObjectBlueprintWithoutName = yamlToJson(microServiceBlueprintWithoutName);
        jsonObjectBlueprintWithoutProperties = yamlToJson(microServiceBlueprintWithoutProperties);
        jsonObjectBlueprintWithoutRelationships = yamlToJson(microServiceBlueprintWithoutRelationships);

    }

    @Test
    public void getNameShouldReturnDefinedName() {
        final JsonObject jsonObject = jsonObjectBlueprintValid;
        String expectedName = jsonObject.get(jsonObject.keySet().iterator().next())
            .getAsJsonObject().get("properties")
            .getAsJsonObject().get("name")
            .getAsString();
        Entry<String, JsonElement> entry = jsonObject.entrySet().iterator().next();
        String actualName = new BlueprintParser().getName(entry);

        Assert.assertEquals(expectedName, actualName);
    }

    @Test
    public void getNameShouldReturnServiceNameWhenNoNameDefined() {
        final JsonObject jsonObject = jsonObjectBlueprintWithoutName;

        String expectedName = jsonObject.keySet().iterator().next();
        Entry<String, JsonElement> entry = jsonObject.entrySet().iterator().next();
        String actualName = new BlueprintParser().getName(entry);

        Assert.assertEquals(expectedName, actualName);
    }

    @Test
    public void getNameShouldReturnServiceNameWhenNoPropertiesDefined() {
        final JsonObject jsonObject = jsonObjectBlueprintWithoutProperties;

        String expectedName = jsonObject.keySet().iterator().next();
        Entry<String, JsonElement> entry = jsonObject.entrySet().iterator().next();
        String actualName = new BlueprintParser().getName(entry);

        Assert.assertEquals(expectedName, actualName);
    }

    @Test
    public void getInputShouldReturnInputWhenPresent() {
        final JsonObject jsonObject = jsonObjectBlueprintValid;

        String expected = FIRST_APPP;
        Entry<String, JsonElement> entry = jsonObject.entrySet().iterator().next();
        String actual = new BlueprintParser().getInput(entry);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void getInputShouldReturnEmptyStringWhenAbsent() {
        final JsonObject jsonObject = jsonObjectBlueprintWithoutRelationships;

        String expected = "";
        Entry<String, JsonElement> entry = jsonObject.entrySet().iterator().next();
        String actual = new BlueprintParser().getInput(entry);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void getNodeRepresentationFromCompleteYaml() {
        final JsonObject jsonObject = jsonObjectBlueprintValid;

        MicroService expected = new MicroService(SECOND_APPP, FIRST_APPP);
        Entry<String, JsonElement> entry = jsonObject.entrySet().iterator().next();
        MicroService actual = new BlueprintParser().getNodeRepresentation(entry);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void getMicroServicesFromBlueprintTest() {
        MicroService thirdApp = new MicroService(THIRD_APPP, "");
        MicroService firstApp = new MicroService(FIRST_APPP, THIRD_APPP);
        MicroService secondApp = new MicroService(SECOND_APPP, FIRST_APPP);

        Set<MicroService> expected = new HashSet<>(Arrays.asList(firstApp, secondApp, thirdApp));
        Set<MicroService> actual = new BlueprintParser().getMicroServices(microServiceTheWholeBlueprintValid);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void fallBackToOneMicroServiceTCATest() {
        MicroService tcaMS = new MicroService(BlueprintParser.TCA, "");

        List<MicroService> expected = Collections.singletonList(tcaMS);
        List<MicroService> actual = new BlueprintParser().fallbackToOneMicroService(microServiceBlueprintOldStyleTCA);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void fallBackToOneMicroServiceHolmesTest() {
        MicroService holmesMS = new MicroService(BlueprintParser.HOLMES, "");

        List<MicroService> expected = Collections.singletonList(holmesMS);
        List<MicroService> actual =
            new BlueprintParser().fallbackToOneMicroService(microServiceBlueprintOldStyleHolmes);

        Assert.assertEquals(expected, actual);
    }

    private static JsonObject yamlToJson(String yamlString) {
        Yaml yaml = new Yaml();
        Map<String, Object> map = yaml.load(yamlString);
        JSONObject jsonObject = new JSONObject(map);
        return GSON.fromJson(jsonObject.toString(), JsonObject.class);
    }
}
