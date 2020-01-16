/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 * Modifications Copyright (c) 2019 Samsung
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
 * Modifications copyright (c) 2019 Nokia
 * ===================================================================
 *
 */

package org.onap.clamp.clds.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;

public class JsonUtilsTest {

    public static class TestClass extends TestObject {

        String test2;
        TestObject2 object2;

        TestClass(String value1, String value2) {
            super(value1);
            test2 = value2;
        }

        void setObject2(TestObject2 object2) {
            this.object2 = object2;
        }
    }

    @Test
    public void testGetObjectMapperInstance() {
        assertNotNull(JsonUtils.GSON);
    }

    /**
     * This method test that the security hole in GSON is not enabled in the default
     * ObjectMapper.
     */
    @Test
    public void testCreateBeanDeserializer() {
        TestClass test = new TestClass("value1", "value2");
        test.setObject2(new TestObject2("test3"));
        Object testObject = JsonUtils.GSON.fromJson("[\"org.onap.clamp.clds.util.JsonUtilsTest$TestClass\""
                + ",{\"test\":\"value1\",\"test2\":\"value2\",\"object2\":[\"org.onap.clamp.clds.util.TestObject2\","
                + "{\"test3\":\"test3\"}]}]", Object.class);
        assertNotNull(testObject);
        assertFalse(testObject instanceof TestObject);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionFileNotExists() throws IOException {
        ResourceFileUtil.getResourceAsString("example/notExist.json");
    }
}
