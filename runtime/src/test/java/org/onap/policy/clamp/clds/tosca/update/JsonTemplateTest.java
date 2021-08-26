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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;
import org.onap.policy.clamp.clds.tosca.update.templates.JsonTemplate;
import org.onap.policy.clamp.clds.tosca.update.templates.JsonTemplateField;

public class JsonTemplateTest extends TestCase {

    JsonTemplate toTest = new JsonTemplate("toTest");
    List<JsonTemplateField>
            jsonTemplateFields = new ArrayList<>(
            Arrays.asList(new JsonTemplateField("type"), new JsonTemplateField("description"),
                    new JsonTemplateField(
                            "enum")));

    /**
     * Test check failed.
     */
    public void testCheckFields() {
        toTest.setJsonTemplateFields(jsonTemplateFields);
        JsonTemplate reference = new JsonTemplate("toTest");
        reference.setJsonTemplateFields(jsonTemplateFields);
        assertTrue(toTest.checkFields(reference));
    }

    /**
     * Test other methods.
     */
    public void testOtherFields() {
        toTest.setJsonTemplateFields(jsonTemplateFields);
        toTest.addField(new JsonTemplateField("moreField"));
        toTest.setVisibility("moreField", true);
        toTest.setStatic("moreField", true);
        toTest.updateValueField("moreField", "testValue");

        assertTrue(toTest.isVisible("moreField"));
        assertEquals("testValue", toTest.getSpecificField("moreField").getValue());
        assertTrue(toTest.fieldStaticStatus("moreField"));
        assertEquals(" templateFields : [type null null null, description null null null, "
                + "enum null null null, moreField testValue true true]", toTest.toString());
    }
}
