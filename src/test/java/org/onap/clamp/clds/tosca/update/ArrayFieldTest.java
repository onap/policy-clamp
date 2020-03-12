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
import java.io.IOException;
import java.util.ArrayList;
import junit.framework.TestCase;
import org.onap.clamp.clds.tosca.update.elements.ArrayField;
import org.onap.clamp.clds.tosca.update.elements.ToscaElement;
import org.onap.clamp.clds.tosca.update.elements.ToscaElementProperty;
import org.onap.clamp.clds.tosca.update.templates.JsonTemplateManager;
import org.onap.clamp.clds.util.ResourceFileUtil;

public class ArrayFieldTest extends TestCase {

    /**
     * Test the deploy method.
     *
     * @throws IOException in case of failure
     */
    public void testDeploy() throws IOException {
        JsonTemplateManager jsonTemplateManager = new JsonTemplateManager(ResourceFileUtil.getResourceAsString(
                "tosca/new-converter/sampleOperationalPoliciesEXTENTED.yaml"),ResourceFileUtil.getResourceAsString(
                "clds/tosca-converter/default-tosca-types.yaml"),
                ResourceFileUtil.getResourceAsString("clds/tosca-converter/templates.json"));
        ToscaElement toscaElement = jsonTemplateManager.getToscaElements().get("onap.datatype.controlloop.Actor");
        ToscaElementProperty toscaElementProperty = toscaElement.getProperties().get("actor");
        ArrayField arrayParser = new ArrayField((ArrayList<Object>) toscaElementProperty.getItems().get("default"));
        JsonArray toTest = arrayParser.deploy();
        String reference = "[1,\"String\",5.5,true]";
        assertEquals(reference, String.valueOf(toTest));
    }
}