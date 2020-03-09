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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;

public class TemplateTest extends TestCase {

    /**
     * Test check failed.
     */
    public void testCheckFields() {
        Template toTest = new Template("toTest");
        List<Field> fields = new ArrayList<>(Arrays.asList(new Field("type"), new Field("description"),new Field(
                "enum")));
        toTest.setFields(fields);
        Template reference = new Template("toTest");
        reference.setFields(fields);
        assertTrue(toTest.checkFields(reference));
    }

}