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

import junit.framework.TestCase;
import org.onap.clamp.clds.tosca.update.templates.JsonTemplateField;

public class JsonTemplateFieldTest extends TestCase {

    JsonTemplateField field1 = new JsonTemplateField("type", "testType", true, true);
    JsonTemplateField field2 = new JsonTemplateField("type");
    JsonTemplateField field3 = new JsonTemplateField("type", "testType1", true, true);
    JsonTemplateField field4 = new JsonTemplateField("type", "testType", false, true);
    JsonTemplateField field5 = new JsonTemplateField("type", "testType", true, false);
    JsonTemplateField field6 = new JsonTemplateField("type", "testType", true, true);

    /**
     * Test fieldsEqual method.
     */
    public void testFieldsEqualsMethod() {
        assertFalse(JsonTemplateField.fieldsEquals(field1,field3));
        assertFalse(JsonTemplateField.fieldsEquals(field1,field4));
        assertFalse(JsonTemplateField.fieldsEquals(field1,field5));
        assertTrue(JsonTemplateField.fieldsEquals(field1,field6));
    }

    /**
     * Test equals method.
     */
    public void testEqualsMethod() {
        assertTrue(field1.equals(field2));
        assertTrue(field1.equals(field3));
        assertTrue(field1.equals(field4));
        assertTrue(field1.equals(field5));
        assertTrue(field1.equals(field6));
    }

    /**
     * Test compareWithField method.
     */
    public void testCompareWithFieldMethod() {
        assertFalse(field1.compareWithField(field2));
        assertFalse(field1.compareWithField(field3));
        assertFalse(field1.compareWithField(field4));
        assertFalse(field1.compareWithField(field5));
        assertTrue(field1.equals(field6));
    }
}