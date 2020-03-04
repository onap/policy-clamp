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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import junit.framework.TestCase;
import org.onap.clamp.clds.util.ResourceFileUtil;

public class PropertyTest extends TestCase {
    public PropertyTest() throws IOException {
    }

    /**
     * Test Parse array.
     *
     * @throws IOException In case of failure
     */
    public void testParseArray() throws IOException {
        TemplateManagement templateManagement = new TemplateManagement(
                ResourceFileUtil.getResourceAsString("tosca/new-converter/sampleOperationalPoliciesEXTENTED.yaml"),
                ResourceFileUtil.getResourceAsString("clds/tosca_update/defaultToscaTypes.yaml"),
                ResourceFileUtil.getResourceAsString("clds/tosca_update/templates.properties"));
        Component component = templateManagement.getComponents().get("onap.datatype.controlloop.Actor");
        Property property = component.getProperties().get("actor");
        JsonArray toTest = property.parseArray((ArrayList<Object>) property.getItems().get("default"));
        assertNotNull(toTest);
    }

    /**
     * Test add constraint as json.
     *
     * @throws IOException In case of failure
     */
    public void testAddConstraintsAsJson() throws IOException {
        TemplateManagement templateManagement = new TemplateManagement(
                ResourceFileUtil.getResourceAsString("tosca/new-converter/sampleOperationalPolicies.yaml"),
                ResourceFileUtil.getResourceAsString("clds/tosca_update/defaultToscaTypes.yaml"),
                ResourceFileUtil.getResourceAsString("clds/tosca_update/templates.properties"));
        Component component = templateManagement.getComponents().get("onap.datatype.controlloop.operation.Failure");
        Property property = component.getProperties().get("category");
        Template template = templateManagement.getTemplates().get("string");
        JsonObject toTest = new JsonObject();
        property.addConstraintsAsJson(toTest, (ArrayList<Object>) property.getItems().get("constraints"), template);
        String test = "{\"enum\":[\"error\",\"timeout\",\"retries\",\"guard\",\"exception\"]}";
        assertEquals(test, String.valueOf(toTest));
    }
}