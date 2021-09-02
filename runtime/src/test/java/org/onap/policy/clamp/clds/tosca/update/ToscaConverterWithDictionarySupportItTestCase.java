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

package org.onap.policy.clamp.clds.tosca.update;

import com.google.gson.JsonObject;
import java.io.IOException;
import javax.transaction.Transactional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.policy.clamp.clds.Application;
import org.onap.policy.clamp.clds.tosca.update.parser.metadata.ToscaMetadataParserWithDictionarySupport;
import org.onap.policy.clamp.clds.tosca.update.templates.JsonTemplateManager;
import org.onap.policy.clamp.clds.util.JsonUtils;
import org.onap.policy.clamp.clds.util.ResourceFileUtils;
import org.onap.policy.clamp.loop.service.Service;
import org.onap.policy.clamp.tosca.Dictionary;
import org.onap.policy.clamp.tosca.DictionaryElement;
import org.onap.policy.clamp.tosca.DictionaryService;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class)
@ActiveProfiles({"clamp-default", "clamp-default-user", "default-dictionary-elements"})
public class ToscaConverterWithDictionarySupportItTestCase {

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private ToscaMetadataParserWithDictionarySupport toscaMetadataParserWithDictionarySupport;

    /**
     * This Test validates Tosca yaml with metadata tag that contains policy_model_type and acronym
     * parameters which defines the Tosca Policy name and its short name.
     *
     * @throws IOException In case of issue when opening the tosca yaml file and
     *                     converted json file
     */
    @Test
    @Transactional
    public final void testMetadataClampPossibleValues() throws IOException, UnknownComponentException {

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

        dictionaryService.saveOrUpdateDictionary(dictionaryTest1);

        Dictionary dictionaryTest2 = new Dictionary();
        dictionaryTest2.setName("Operators");
        dictionaryTest2.setSecondLevelDictionary(0);

        DictionaryElement element2 = new DictionaryElement();
        element2.setName("equals");
        element2.setShortName("equals");
        element2.setType("string");
        element2.setDescription("equals");
        dictionaryTest2.addDictionaryElements(element2);
        dictionaryService.saveOrUpdateDictionary(dictionaryTest2);

        JsonTemplateManager jsonTemplateManager =
                new JsonTemplateManager(
                        ResourceFileUtils
                                .getResourceAsString("tosca/new-converter/tosca_metadata_clamp_possible_values.yaml"),
                        ResourceFileUtils.getResourceAsString("clds/tosca-converter/default-tosca-types.yaml"),
                        ResourceFileUtils.getResourceAsString("clds/tosca-converter/templates.json"));

        JsonObject jsonSchema = jsonTemplateManager.getJsonSchemaForPolicyType(
                "onap.policies.monitoring.cdap.tca.hi.lo.app", toscaMetadataParserWithDictionarySupport, null);

        JSONAssert.assertEquals(
                ResourceFileUtils
                        .getResourceAsString("tosca/new-converter/tca-with-metadata.json"),
                JsonUtils.GSON.toJson(jsonSchema), true);
    }

    @Test
    @Transactional
    public final void testMetadataClampPossibleValueWithExecutor() throws IOException, UnknownComponentException {
        Service service = new Service(ResourceFileUtils.getResourceAsString("tosca/service-details.json"),
                ResourceFileUtils.getResourceAsString("tosca/resource-details-cds.json"));
        JsonTemplateManager jsonTemplateManager =
                new JsonTemplateManager(
                        ResourceFileUtils.getResourceAsString("http-cache/example/policy/api/v1/policytypes/onap"
                                + ".policies.controlloop.operational.common.Apex/versions/1.0.0/.file"),
                        ResourceFileUtils.getResourceAsString("clds/tosca-converter/default-tosca-types.yaml"),
                        ResourceFileUtils.getResourceAsString("clds/tosca-converter/templates.json"));

        JsonObject jsonSchema = jsonTemplateManager.getJsonSchemaForPolicyType(
                "onap.policies.controlloop.operational.common.Apex", toscaMetadataParserWithDictionarySupport, service);

        JSONAssert.assertEquals(
                ResourceFileUtils
                        .getResourceAsString("tosca/new-converter/tosca_apex_with_metadata.json"),
                JsonUtils.GSON.toJson(jsonSchema), true);
    }
}
