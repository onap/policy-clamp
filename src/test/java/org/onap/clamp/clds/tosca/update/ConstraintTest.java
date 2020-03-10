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

import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import junit.framework.TestCase;
import org.onap.clamp.clds.util.ResourceFileUtil;

public class ConstraintTest extends TestCase {

    ToscaConverterManager toscaConverterManager = new ToscaConverterManager(
            ResourceFileUtil.getResourceAsString("tosca/new-converter/constraints.yaml"),
            ResourceFileUtil.getResourceAsString("clds/tosca_update/default-tosca-types.yaml"),
            ResourceFileUtil.getResourceAsString("clds/tosca_update/templates.json"));

    ToscaElement toscaElement = toscaConverterManager.getComponents().get("onap.datatype.controlloop.Operation");

    public ConstraintTest() throws IOException {
    }

    /**
     * Test get value array.
     */
    public void testGetValuesArray() {
        Property property = toscaElement.getProperties().get("timeout");
        Template template = toscaConverterManager.getTemplates().get("integer");
        JsonObject resultProcess = new JsonObject();
        property.addConstraintsAsJson(resultProcess, (ArrayList<Object>) property.getItems().get("constraints"),
                template);
        String reference = "{\"enum\":[3,4,5.5,6,10]}";
        assertEquals(reference, String.valueOf(resultProcess));
        property = toscaElement.getProperties().get("success");
        template = toscaConverterManager.getTemplates().get("string");
        resultProcess = new JsonObject();
        property.addConstraintsAsJson(resultProcess, (ArrayList<Object>) property.getItems().get("constraints"),
                template);
        reference = "{\"enum\":[\"VALID\",\"TERMINATED\"]}";
        assertEquals(reference, String.valueOf(resultProcess));
    }

    /**
     * Test get Specific length.
     */
    public void testGetSpecificLength() {
        //Test for string type, same process for array
        Property property = toscaElement.getProperties().get("id");
        Template template = toscaConverterManager.getTemplates().get("string");
        JsonObject resultProcess = new JsonObject();
        property.addConstraintsAsJson(resultProcess, (ArrayList<Object>) property.getItems().get("constraints"),
                template);
        int specificLength = 8;
        int toTest = resultProcess.get("minLength").getAsInt();
        assertEquals(specificLength, toTest);
        toTest = resultProcess.get("maxLength").getAsInt();
        assertEquals(specificLength, toTest);
    }

    /**
     * Test get limit value.
     */
    public void testGetLimitValue() {
        //Test for array type, same process for string
        Property property = toscaElement.getProperties().get("description");
        Template template = toscaConverterManager.getTemplates().get("array");
        JsonObject resultProcess = new JsonObject();
        property.addConstraintsAsJson(resultProcess, (ArrayList<Object>) property.getItems().get("constraints"),
                template);

        int toTest = resultProcess.get("minItems").getAsInt();
        assertEquals(5, toTest);
        toTest = resultProcess.get("maxItems").getAsInt();
        assertEquals(7, toTest);
    }

}