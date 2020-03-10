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
import org.onap.clamp.clds.util.ResourceFileUtil;

public class ToscaConverterManagerTest extends TestCase {

    /**
     * Test the launch translation wit operational policies.
     *
     * @throws IOException               In case of failure
     * @throws UnknownComponentException In case of failure
     */
    public void testLaunchTranslationTca() throws IOException, UnknownComponentException {
        ToscaConverterManager toscaConverterManager =
                new ToscaConverterManager(
                        ResourceFileUtil.getResourceAsString("http-cache/example/policy/api/v1/policytypes/onap"
                                + ".policies.monitoring.cdap.tca.hi.lo.app/versions/1.0.0&#63;"
                                + "connectionTimeToLive=5000/.file"), ResourceFileUtil.getResourceAsString(
                        "clds/tosca_update/default-tosca-types.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca_update/templates.json"));
        assertNull(toscaConverterManager.getParseToJson());
        String componentName = "onap.policies.monitoring.cdap.tca.hi.lo.app";
        toscaConverterManager.startConversionToJson(componentName);
        assertNotNull(toscaConverterManager.getParseToJson());
    }

    /**
     * Test the launch translation wit operational policies.
     *
     * @throws IOException               In case of failure
     * @throws UnknownComponentException In case of failure
     */
    public void testLaunchTranslationFrequencyLimiter() throws IOException, UnknownComponentException {
        ToscaConverterManager toscaConverterManager =
                new ToscaConverterManager(
                        ResourceFileUtil.getResourceAsString("http-cache/example/policy/api/v1/policytypes/onap"
                                + ".policies.controlloop.guard.common.FrequencyLimiter/versions/1.0.0&#63;"
                                + "connectionTimeToLive=5000/.file"), ResourceFileUtil.getResourceAsString(
                        "clds/tosca_update/default-tosca-types.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca_update/templates.json"));
        assertNull(toscaConverterManager.getParseToJson());
        String componentName = "onap.policies.controlloop.guard.common.FrequencyLimiter";
        toscaConverterManager.startConversionToJson(componentName);
        assertNotNull(toscaConverterManager.getParseToJson());
    }

    /**
     * Test the launch translation wit operational policies.
     *
     * @throws IOException               In case of failure
     * @throws UnknownComponentException In case of failure
     */
    public void testLaunchTranslationApex() throws IOException, UnknownComponentException {
        ToscaConverterManager toscaConverterManager =
                new ToscaConverterManager(
                        ResourceFileUtil.getResourceAsString("http-cache/example/policy/api/v1/policytypes/onap"
                                + ".policies.controlloop.operational.common.Apex/versions/1.0.0&#63;"
                                + "connectionTimeToLive=5000/.file"), ResourceFileUtil.getResourceAsString(
                        "clds/tosca_update/default-tosca-types.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca_update/templates.json"));
        assertNull(toscaConverterManager.getParseToJson());
        String componentName = "onap.policies.controlloop.operational.common.Apex";
        toscaConverterManager.startConversionToJson(componentName);
        assertNotNull(toscaConverterManager.getParseToJson());
    }

    /**
     * Test the launch translation wit operational policies.
     *
     * @throws IOException               In case of failure
     * @throws UnknownComponentException In case of failure
     */
    public void testLaunchTranslationDrools() throws IOException, UnknownComponentException {
        ToscaConverterManager toscaConverterManager =
                new ToscaConverterManager(
                        ResourceFileUtil.getResourceAsString("http-cache/example/policy/api/v1/policytypes/onap"
                                + ".policies.controlloop.operational.common.Drools/versions/1.0.0&#63;"
                                + "connectionTimeToLive=5000/.file"), ResourceFileUtil.getResourceAsString(
                        "clds/tosca_update/default-tosca-types.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca_update/templates.json"));
        assertNull(toscaConverterManager.getParseToJson());
        String componentName = "onap.policies.controlloop.operational.common.Drools";
        toscaConverterManager.startConversionToJson(componentName);
        assertNotNull(toscaConverterManager.getParseToJson());
    }

    /**
     * Test the launch translation.
     *
     * @throws IOException               In case of failure
     * @throws UnknownComponentException In case of failure
     */
    public void testLaunchTranslation() throws IOException, UnknownComponentException {
        ToscaConverterManager toscaConverterManager =
                new ToscaConverterManager(
                        ResourceFileUtil.getResourceAsString("tosca/new-converter/sampleOperationalPolicies.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca_update/default-tosca-types.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca_update/templates.json"));
        assertNull(toscaConverterManager.getParseToJson());
        String componentName = "onap.policies.controlloop.operational.common.Drools";
        toscaConverterManager.startConversionToJson(componentName);
        assertNotNull(toscaConverterManager.getParseToJson());
    }

    /**
     * Test addTemplate.
     *
     * @throws IOException In case of failure
     */
    public void testAddTemplate() throws IOException {
        ToscaConverterManager toscaConverterManager =
                new ToscaConverterManager(
                        ResourceFileUtil.getResourceAsString("tosca/new-converter/sampleOperationalPolicies.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca_update/default-tosca-types.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca_update/templates.json"));
        int count = toscaConverterManager.getTemplates().size();
        List<TemplateField> templateFields = new ArrayList<>(Arrays.asList(new TemplateField("type"), new TemplateField("description"),
                new TemplateField(
                "required"),
                new TemplateField("metadata"), new TemplateField("constraints")));
        toscaConverterManager.addTemplate("test", templateFields);
        assertNotSame(count, toscaConverterManager.getTemplates().size());
    }

    /**
     * test Remove template.
     *
     * @throws IOException In case of failure
     */
    public void testRemoveTemplate() throws IOException {
        ToscaConverterManager toscaConverterManager =
                new ToscaConverterManager(
                        ResourceFileUtil.getResourceAsString("tosca/new-converter/sampleOperationalPolicies.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca_update/default-tosca-types.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca_update/templates.json"));
        int count = toscaConverterManager.getTemplates().size();
        toscaConverterManager.removeTemplate("string");
        assertNotSame(count, toscaConverterManager.getTemplates().size());
    }

    /**
     * Test update template.
     *
     * @throws IOException In case of failure
     */
    public void testUpdateTemplate() throws IOException {
        ToscaConverterManager toscaConverterManager =
                new ToscaConverterManager(
                        ResourceFileUtil.getResourceAsString("tosca/new-converter/sampleOperationalPolicies.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca_update/default-tosca-types.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca_update/templates.json"));
        int count = toscaConverterManager.getTemplates().get("integer").getTemplateFields().size();
        toscaConverterManager.updateTemplate("integer", new TemplateField("type"), false);
        assertNotSame(count, toscaConverterManager.getTemplates().get("integer").getTemplateFields().size());
    }

    /**
     * Test has template.
     *
     * @throws IOException In case of failure
     */
    public void testHasTemplate() throws IOException {
        ToscaConverterManager toscaConverterManager =
                new ToscaConverterManager(
                        ResourceFileUtil.getResourceAsString("tosca/new-converter/sampleOperationalPolicies.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca_update/default-tosca-types.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca_update/templates.json"));
        boolean has = true;
        List<TemplateField> templateFieldsString =
                new ArrayList<>(Arrays.asList(new TemplateField("type"), new TemplateField("description"), new TemplateField("required"),
                        new TemplateField("metadata"), new TemplateField("constraints")));
        Template templateTest = new Template("String", templateFieldsString);
        has = toscaConverterManager.hasTemplate(templateTest);
        assertEquals(false, has);
    }

}
