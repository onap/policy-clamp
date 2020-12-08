/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 Samsung. All rights reserved.
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

package org.onap.clamp.clds.model.jsontype;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;

import org.hibernate.HibernateException;
import org.junit.Test;
import org.onap.clamp.dao.model.jsontype.JsonTypeDescriptor;

public class JsonTypeDescriptorTest {

    private JsonTypeDescriptor descriptor = new JsonTypeDescriptor();

    @Test
    public void testFromString() {
        JsonObject object = new JsonObject();
        object.addProperty("one","oneValue");
        JsonObject child = new JsonObject();
        child.addProperty("two","twoValue");
        object.add("child",child);

        JsonObject jsonResult = descriptor.fromString("{\"one\":\"oneValue\",\"child\":{\"two\":\"twoValue\"}}");

        assertThat(jsonResult).isEqualTo(object);
    }

    @Test
    public void testUnwrap() {
        JsonObject res1 = descriptor.unwrap(null, null, null);
        assertThat(res1).isNull();

        JsonObject object = new JsonObject();
        object.addProperty("one","oneValue");
        JsonObject child = new JsonObject();
        child.addProperty("two","twoValue");
        object.add("child",child);
        String res2 = descriptor.unwrap(object, String.class, null);
        assertThat(res2.replace("\n", "").replace(" ", ""))
                .isEqualTo("{\"one\":\"oneValue\",\"child\":{\"two\":\"twoValue\"}}");

        Object res3 = descriptor.unwrap(object, JsonObject.class, null);
        String res3Str = ((String) res3).replace(" ", "").replace("\\n", "").replace("\\", "")
                .replace("\"{", "{").replace("}\"", "}");
        assertThat(res3Str).isEqualTo("{\"one\":\"oneValue\",\"child\":{\"two\":\"twoValue\"}}");
    }

    @Test(expected = HibernateException.class)
    public void testUnwrapExpectationThrown() {
        JsonObject object = new JsonObject();
        object.addProperty("one","oneValue");

        descriptor.unwrap(object, Integer.class, null);
    }

    @Test
    public void testWrap() {
        JsonObject res1 = descriptor.wrap(null, null);
        assertThat(res1).isNull();

        JsonObject object = new JsonObject();
        object.addProperty("one","oneValue");
        JsonObject child = new JsonObject();
        child.addProperty("two","twoValue");
        object.add("child",child);
        JsonObject res2 = descriptor.wrap("{\"one\":\"oneValue\",\"child\":{\"two\":\"twoValue\"}}", null);
        assertThat(res2).isEqualTo(object);
    }

    @Test(expected = HibernateException.class)
    public void testWrapExpectationThrown() {
        descriptor.wrap(1, null);
    }
}