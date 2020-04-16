/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights
 *                             reserved.
 * Modifications Copyright (C) 2019 Huawei Technologies Co., Ltd.
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

package org.onap.clamp.clds.tosca;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonObject;
import java.io.IOException;
import javax.transaction.Transactional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.clamp.clds.Application;
import org.onap.clamp.clds.util.ResourceFileUtil;
import org.onap.clamp.tosca.Dictionary;
import org.onap.clamp.tosca.DictionaryElement;
import org.onap.clamp.tosca.DictionaryService;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class)
public class ToscaYamlToJsonConvertorTestItCase {

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private ToscaYamlToJsonConvertor toscaYamlToJsonConvertor;

    /**
     * This Test validates TOSCA yaml to JSON Schema conversion based on JSON Editor
     * Schema.
     *
     * @throws IOException In case of issue when opening the tosca yaml file and
     *         converted json file
     */
    @Test
    public final void testParseToscaYaml() throws IOException {
        String toscaModelYaml = ResourceFileUtil.getResourceAsString("tosca/tosca_example.yaml");
        ToscaYamlToJsonConvertor convertor = new ToscaYamlToJsonConvertor();

        String parsedJsonSchema =
            convertor.parseToscaYaml(toscaModelYaml, "onap.policies.monitoring.cdap.tca.hi.lo.app");
        assertNotNull(parsedJsonSchema);
        JSONAssert.assertEquals(
            ResourceFileUtil.getResourceAsString("tosca/policy-yaml-to-json.json"),
            parsedJsonSchema, true);
    }

    /**
     * This Test validates TOSCA yaml with constraints to JSON Schema conversion
     * based on JSON Editor Schema.
     *
     * @throws IOException In case of issue when opening the tosca yaml file and
     *         converted json file
     */
    @Test
    public final void testParseToscaYamlWithConstraints() throws IOException {
        String toscaModelYaml =
            ResourceFileUtil.getResourceAsString("tosca/tosca-with-constraints.yaml");
        ToscaYamlToJsonConvertor convertor = new ToscaYamlToJsonConvertor();

        String parsedJsonSchema =
            convertor.parseToscaYaml(toscaModelYaml, "onap.policies.monitoring.example.app");
        assertNotNull(parsedJsonSchema);
        JSONAssert.assertEquals(
            ResourceFileUtil.getResourceAsString("tosca/policy-yaml-to-json-with-constraints.json"),
            parsedJsonSchema, true);
    }

    /**
     * This Test validates TOSCA yaml with different datatypes to JSON Schema
     * conversion based on JSON Editor Schema.
     *
     * @throws IOException In case of issue when opening the tosca yaml file and
     *         converted json file
     */
    @Test
    public final void testParseToscaYamlWithTypes() throws IOException {
        String toscaModelYaml =
            ResourceFileUtil.getResourceAsString("tosca/tosca-with-datatypes.yaml");
        ToscaYamlToJsonConvertor convertor = new ToscaYamlToJsonConvertor();

        String parsedJsonSchema =
            convertor.parseToscaYaml(toscaModelYaml, "onap.policies.monitoring.example.app");
        assertNotNull(parsedJsonSchema);
        JSONAssert.assertEquals(
            ResourceFileUtil.getResourceAsString("tosca/policy-yaml-to-json-with-datatypes.json"),
            parsedJsonSchema, true);
    }

    /**
     * This Test validates Tosca yaml with metadata tag that contains policy_model_type and acronym
     * parameters which defines the Tosca Policy name and its short name.
     *
     * @throws IOException In case of issue when opening the tosca yaml file and
     *         converted json file
     */
    @Test
    @Transactional
    public final void testMetadataClampPossibleValues() throws IOException {
        setupDictionary();
        String toscaModelYaml =
            ResourceFileUtil.getResourceAsString("tosca/tosca_metadata_clamp_possible_values.yaml");

        JsonObject jsonObject = toscaYamlToJsonConvertor.validateAndConvertToJson(toscaModelYaml);
        assertNotNull(jsonObject);
        String policyModelType = toscaYamlToJsonConvertor.getValueFromMetadata(jsonObject,
            ToscaSchemaConstants.METADATA_POLICY_MODEL_TYPE);
        String acronym = toscaYamlToJsonConvertor.getValueFromMetadata(jsonObject,
            ToscaSchemaConstants.METADATA_ACRONYM);
        String parsedJsonSchema =
            toscaYamlToJsonConvertor.parseToscaYaml(toscaModelYaml, policyModelType);

        assertNotNull(parsedJsonSchema);
        assertEquals("onap.policies.monitoring.cdap.tca.hi.lo.app", policyModelType);
        assertEquals("tca", acronym);
        JSONAssert.assertEquals(
            ResourceFileUtil
                .getResourceAsString("tosca/tosca_metadata_clamp_possible_values_json_schema.json"),
            parsedJsonSchema, true);

    }

    private void setupDictionary() {

        // Set up dictionary elements
        Dictionary dictionaryTest = new Dictionary();
        dictionaryTest.setName("Context");
        dictionaryTest.setSecondLevelDictionary(0);

        DictionaryElement element = new DictionaryElement();
        element.setName("PROD");
        element.setShortName("PROD");
        element.setType("string");
        element.setDescription("Production");
        dictionaryTest.addDictionaryElements(element);

        dictionaryService.saveOrUpdateDictionary(dictionaryTest);

        Dictionary dictionaryTest1 = new Dictionary();
        dictionaryTest1.setName("EventDictionary");
        dictionaryTest1.setSecondLevelDictionary(0);

        DictionaryElement element1 = new DictionaryElement();
        element1.setName("alarmCondition");
        element1.setShortName("alarmCondition");
        element1.setType("string");
        element1.setDescription("Alarm Condition");
        dictionaryTest1.addDictionaryElements(element1);

        dictionaryTest1 = dictionaryService.saveOrUpdateDictionary(dictionaryTest1);

        DictionaryElement element3 = new DictionaryElement();
        element3.setName("timeEpoch");
        element3.setShortName("timeEpoch");
        element3.setType("datetime");
        element3.setDescription("Time Epoch");
        dictionaryTest1.addDictionaryElements(element3);

        dictionaryService.saveOrUpdateDictionary(dictionaryTest1);

        Dictionary dictionaryTest2 = new Dictionary();
        dictionaryTest2.setName("Operators");
        dictionaryTest2.setSecondLevelDictionary(0);

        DictionaryElement element2 = new DictionaryElement();
        element2.setName("equals");
        element2.setShortName("equals");
        element2.setType("string|datetime");
        element2.setDescription("equals");
        dictionaryTest2.addDictionaryElements(element2);
        dictionaryService.saveOrUpdateDictionary(dictionaryTest2);
    }

}
