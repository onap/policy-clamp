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

package org.onap.clamp.clds.tosca.update;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;
import org.onap.clamp.clds.tosca.update.templates.JsonTemplate;
import org.onap.clamp.clds.tosca.update.templates.JsonTemplateField;
import org.onap.clamp.clds.tosca.update.templates.JsonTemplateManager;
import org.onap.clamp.clds.util.ResourceFileUtil;

public class JsonTemplateManagerTest extends TestCase {

    /**
     * Test the launch translation wit operational policies.
     *
     * @throws IOException               In case of failure
     * @throws UnknownComponentException In case of failure
     */
    public void testLaunchTranslationTca() throws IOException, UnknownComponentException {
        JsonTemplateManager jsonTemplateManager =
                new JsonTemplateManager(
                        ResourceFileUtil.getResourceAsString("http-cache/example/policy/api/v1/policytypes/onap"
                                + ".policies.monitoring.cdap.tca.hi.lo.app/versions/1.0.0&#63;"
                                + "connectionTimeToLive=5000/.file"), ResourceFileUtil.getResourceAsString(
                        "clds/tosca-converter/default-tosca-types.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca-converter/templates.json"));
        String componentName = "onap.policies.monitoring.cdap.tca.hi.lo.app";
        jsonTemplateManager.getJsonSchemaForPolicyType(componentName, null, null);
    }

    /**
     * Test the launch translation wit operational policies.
     *
     * @throws IOException               In case of failure
     * @throws UnknownComponentException In case of failure
     */
    public void testLaunchTranslationFrequencyLimiter() throws IOException, UnknownComponentException {
        JsonTemplateManager jsonTemplateManager =
                new JsonTemplateManager(
                        ResourceFileUtil.getResourceAsString("http-cache/example/policy/api/v1/policytypes/onap"
                                + ".policies.controlloop.guard.common.FrequencyLimiter/versions/1.0.0&#63;"
                                + "connectionTimeToLive=5000/.file"), ResourceFileUtil.getResourceAsString(
                        "clds/tosca-converter/default-tosca-types.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca-converter/templates.json"));
        String componentName = "onap.policies.controlloop.guard.common.FrequencyLimiter";
        jsonTemplateManager.getJsonSchemaForPolicyType(componentName, null, null);
    }

    /**
     * Test the launch translation wit operational policies.
     *
     * @throws IOException               In case of failure
     * @throws UnknownComponentException In case of failure
     */
    public void testLaunchTranslationApex() throws IOException, UnknownComponentException {
        JsonTemplateManager jsonTemplateManager =
                new JsonTemplateManager(
                        ResourceFileUtil.getResourceAsString("http-cache/example/policy/api/v1/policytypes/onap"
                                + ".policies.controlloop.operational.common.Apex/versions/1.0.0&#63;"
                                + "connectionTimeToLive=5000/.file"), ResourceFileUtil.getResourceAsString(
                        "clds/tosca-converter/default-tosca-types.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca-converter/templates.json"));
        String componentName = "onap.policies.controlloop.operational.common.Apex";
        jsonTemplateManager.getJsonSchemaForPolicyType(componentName, null, null);
    }

    /**
     * Test the launch translation wit operational policies.
     *
     * @throws IOException               In case of failure
     * @throws UnknownComponentException In case of failure
     */
    public void testLaunchTranslationDrools() throws IOException, UnknownComponentException {
        JsonTemplateManager jsonTemplateManager =
                new JsonTemplateManager(
                        ResourceFileUtil.getResourceAsString("http-cache/example/policy/api/v1/policytypes/onap"
                                + ".policies.controlloop.operational.common.Drools/versions/1.0.0&#63;"
                                + "connectionTimeToLive=5000/.file"), ResourceFileUtil.getResourceAsString(
                        "clds/tosca-converter/default-tosca-types.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca-converter/templates.json"));
        String componentName = "onap.policies.controlloop.operational.common.Drools";
        jsonTemplateManager.getJsonSchemaForPolicyType(componentName, null, null);
    }


    /**
     * Test the launch translation.
     *
     * @throws IOException               In case of failure
     * @throws UnknownComponentException In case of failure
     */
    public void testLaunchTranslation() throws IOException, UnknownComponentException {
        JsonTemplateManager jsonTemplateManager =
                new JsonTemplateManager(
                        ResourceFileUtil.getResourceAsString("tosca/new-converter/sampleOperationalPolicies.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca-converter/default-tosca-types.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca-converter/templates.json"));
        String componentName = "onap.policies.controlloop.operational.common.Drools";
        jsonTemplateManager.getJsonSchemaForPolicyType(componentName, null, null);
    }

    /**
     * Test addTemplate.
     *
     * @throws IOException In case of failure
     */
    public void testAddTemplate() throws IOException {
        JsonTemplateManager jsonTemplateManager =
                new JsonTemplateManager(
                        ResourceFileUtil.getResourceAsString("tosca/new-converter/sampleOperationalPolicies.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca-converter/default-tosca-types.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca-converter/templates.json"));
        int count = jsonTemplateManager.getJsonSchemaTemplates().size();
        List<JsonTemplateField>
                jsonTemplateFields =
                new ArrayList<>(Arrays.asList(new JsonTemplateField("type"), new JsonTemplateField("description"),
                        new JsonTemplateField(
                                "required"),
                        new JsonTemplateField("metadata"), new JsonTemplateField("constraints")));
        jsonTemplateManager.addTemplate("test", jsonTemplateFields);
        assertNotSame(count, jsonTemplateManager.getJsonSchemaTemplates().size());
    }

    /**
     * test Remove template.
     *
     * @throws IOException In case of failure
     */
    public void testRemoveTemplate() throws IOException {
        JsonTemplateManager jsonTemplateManager =
                new JsonTemplateManager(
                        ResourceFileUtil.getResourceAsString("tosca/new-converter/sampleOperationalPolicies.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca-converter/default-tosca-types.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca-converter/templates.json"));
        int count = jsonTemplateManager.getJsonSchemaTemplates().size();
        jsonTemplateManager.removeTemplate("string");
        assertNotSame(count, jsonTemplateManager.getJsonSchemaTemplates().size());
    }

    /**
     * Test update template.
     *
     * @throws IOException In case of failure
     */
    public void testUpdateTemplate() throws IOException {
        JsonTemplateManager jsonTemplateManager =
                new JsonTemplateManager(
                        ResourceFileUtil.getResourceAsString("tosca/new-converter/sampleOperationalPolicies.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca-converter/default-tosca-types.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca-converter/templates.json"));
        int count = jsonTemplateManager.getJsonSchemaTemplates().get("integer").getJsonTemplateFields().size();
        jsonTemplateManager.updateTemplate("integer", new JsonTemplateField("type"), false);
        assertNotSame(count,
                jsonTemplateManager.getJsonSchemaTemplates().get("integer").getJsonTemplateFields().size());
    }

    /**
     * Test has template.
     *
     * @throws IOException In case of failure
     */
    public void testHasTemplate() throws IOException {
        JsonTemplateManager jsonTemplateManager =
                new JsonTemplateManager(
                        ResourceFileUtil.getResourceAsString("tosca/new-converter/sampleOperationalPolicies.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca-converter/default-tosca-types.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca-converter/templates.json"));
        boolean has = true;
        List<JsonTemplateField> jsonTemplateFieldsString =
                new ArrayList<>(Arrays.asList(new JsonTemplateField("type"), new JsonTemplateField("description"),
                        new JsonTemplateField("required"),
                        new JsonTemplateField("metadata"), new JsonTemplateField("constraints")));
        JsonTemplate jsonTemplateTest = new JsonTemplate("String", jsonTemplateFieldsString);
        has = jsonTemplateManager.hasTemplate(jsonTemplateTest);
        assertEquals(false, has);
    }

}
