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
import junit.framework.TestCase;
import org.onap.clamp.clds.util.ResourceFileUtil;

public class TemplateManagementTest extends TestCase {

    /**
     * Test the launch translation.
     *
     * @throws IOException               In case of failure
     * @throws UnknownComponentException In case of failure
     */
    public void testLaunchTranslation() throws IOException, UnknownComponentException {
        TemplateManagement templateManagement =
                new TemplateManagement(
                        ResourceFileUtil.getResourceAsString("tosca/new-converter/sampleOperationalPolicies.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca_update/templates.properties"));
        assertNull(templateManagement.getParseToJson());
        String componentName = "onap.policies.controlloop.operational.common.Drools";
        templateManagement.launchTranslation(componentName);
        assertNotNull(templateManagement.getParseToJson());
    }

    /**
     * Test addTemplate.
     *
     * @throws IOException In case of failure
     */
    public void testAddTemplate() throws IOException {
        TemplateManagement templateManagement =
                new TemplateManagement(
                        ResourceFileUtil.getResourceAsString("tosca/new-converter/sampleOperationalPolicies.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca_update/templates.properties"));
        int count = templateManagement.getTemplates().size();
        ArrayList<String> templateFields =
                new ArrayList<>(Arrays.asList("type", "description", "required", "metadata", "constraints"));
        templateManagement.addTemplate("test", templateFields);
        assertNotSame(count, templateManagement.getTemplates().size());
    }

    /**
     * test Remove template.
     *
     * @throws IOException In case of failure
     */
    public void testRemoveTemplate() throws IOException {
        TemplateManagement templateManagement =
                new TemplateManagement(
                        ResourceFileUtil.getResourceAsString("tosca/new-converter/sampleOperationalPolicies.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca_update/templates.properties"));
        int count = templateManagement.getTemplates().size();
        templateManagement.removeTemplate("string");
        assertNotSame(count, templateManagement.getTemplates().size());
    }

    /**
     * Test update template.
     *
     * @throws IOException In case of failure
     */
    public void testUpdateTemplate() throws IOException {
        TemplateManagement templateManagement =
                new TemplateManagement(
                        ResourceFileUtil.getResourceAsString("tosca/new-converter/sampleOperationalPolicies.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca_update/templates.properties"));
        int count = templateManagement.getTemplates().get("integer").getFields().size();
        templateManagement.updateTemplate("integer", "type", false);
        assertNotSame(count, templateManagement.getTemplates().get("integer").getFields().size());
    }

    /**
     * Test has template.
     *
     * @throws IOException In case of failure
     */
    public void testHasTemplate() throws IOException {
        TemplateManagement templateManagement =
                new TemplateManagement(
                        ResourceFileUtil.getResourceAsString("tosca/new-converter/sampleOperationalPolicies.yaml"),
                        ResourceFileUtil.getResourceAsString("clds/tosca_update/templates.properties"));
        boolean has = true;
        ArrayList<String> templateFieldsString =
                new ArrayList<>(Arrays.asList("type", "description", "required", "metadata", "constraints"));
        Template templateTest = new Template("String", templateFieldsString);
        has = templateManagement.hasTemplate(templateTest);
        assertEquals(false, has);
    }

}
