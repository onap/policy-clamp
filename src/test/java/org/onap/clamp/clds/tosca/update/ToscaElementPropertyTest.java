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
import org.onap.clamp.clds.tosca.update.elements.ToscaElement;
import org.onap.clamp.clds.tosca.update.elements.ToscaElementProperty;
import org.onap.clamp.clds.tosca.update.templates.JsonTemplate;
import org.onap.clamp.clds.tosca.update.templates.JsonTemplateManager;
import org.onap.clamp.clds.util.ResourceFileUtil;

public class ToscaElementPropertyTest extends TestCase {
    public ToscaElementPropertyTest() throws IOException {
    }

    /**
     * Test Parse array.
     *
     * @throws IOException In case of failure
     */
    public void testParseArray() throws IOException {
        JsonTemplateManager jsonTemplateManager = new JsonTemplateManager(
                ResourceFileUtil.getResourceAsString("tosca/new-converter/sampleOperationalPoliciesEXTENTED.yaml"),
                ResourceFileUtil.getResourceAsString("clds/tosca-converter/default-tosca-types.yaml"),
                ResourceFileUtil.getResourceAsString("clds/tosca-converter/templates.json"));
        ToscaElement toscaElement = jsonTemplateManager.getToscaElements().get("onap.datatype.controlloop.Actor");
        ToscaElementProperty toscaElementProperty = toscaElement.getProperties().get("actor");
        JsonArray toTest =
                toscaElementProperty.parseArray((ArrayList<Object>) toscaElementProperty.getItems().get("default"));
        assertNotNull(toTest);
    }

    /**
     * Test add constraint as json.
     *
     * @throws IOException In case of failure
     */
    public void testAddConstraintsAsJson() throws IOException {
        JsonTemplateManager jsonTemplateManager = new JsonTemplateManager(
                ResourceFileUtil.getResourceAsString("tosca/new-converter/sampleOperationalPolicies.yaml"),
                ResourceFileUtil.getResourceAsString("clds/tosca-converter/default-tosca-types.yaml"),
                ResourceFileUtil.getResourceAsString("clds/tosca-converter/templates.json"));
        ToscaElement toscaElement =
                jsonTemplateManager.getToscaElements().get("onap.datatype.controlloop.operation.Failure");
        ToscaElementProperty toscaElementProperty = toscaElement.getProperties().get("category");
        JsonTemplate jsonTemplate = jsonTemplateManager.getJsonSchemaTemplates().get("string");
        JsonObject toTest = new JsonObject();
        toscaElementProperty
                .addConstraintsAsJson(toTest, (ArrayList<Object>) toscaElementProperty.getItems().get("constraints"),
                        jsonTemplate);
        String test = "{\"enum\":[\"error\",\"timeout\",\"retries\",\"guard\",\"exception\"]}";
        assertEquals(test, String.valueOf(toTest));
    }
}