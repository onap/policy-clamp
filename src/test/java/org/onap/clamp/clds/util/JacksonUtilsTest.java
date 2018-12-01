/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights
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

package org.onap.clamp.clds.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import java.io.IOException;

import org.junit.Test;

public class JacksonUtilsTest {

    public static class TestClass extends TestObject {

        String test2;
        TestObject2 object2;

        public TestClass(String value1, String value2) {
            super(value1);
            test2 = value2;
        }

        public TestClass() {
        }

        public String getTest2() {
            return test2;
        }

        public void setTest2(String test2) {
            this.test2 = test2;
        }

        public TestObject2 getObject2() {
            return object2;
        }

        public void setObject2(TestObject2 object2) {
            this.object2 = object2;
        }
    }

    @Test
    public void testGetObjectMapperInstance() {
        assertNotNull(JacksonUtils.getObjectMapperInstance());
    }

    /**
     * This method test that the security hole in Jackson is not enabled in the
     * default ObjectMapper.
     *
     * @throws JsonParseException
     *         In case of issues
     * @throws JsonMappingException
     *         In case of issues
     * @throws IOException
     *         In case of issues
     */
    @Test
    public void testCreateBeanDeserializer() throws JsonParseException, JsonMappingException, IOException {
        TestClass test = new TestClass("value1", "value2");
        test.setObject2(new TestObject2("test3"));
        Object testObject = JacksonUtils.getObjectMapperInstance()
            .readValue("[\"org.onap.clamp.clds.util.JacksonUtilsTest$TestClass\""
                + ",{\"test\":\"value1\",\"test2\":\"value2\",\"object2\":[\"org.onap.clamp.clds.util.TestObject2\","
                + "{\"test3\":\"test3\"}]}]", Object.class);
        assertNotNull(testObject);
        assertFalse(testObject instanceof TestObject);
        assertFalse(testObject instanceof TestClass);
    }
}
