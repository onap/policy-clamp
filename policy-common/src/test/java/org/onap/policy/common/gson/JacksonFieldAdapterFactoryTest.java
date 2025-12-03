/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024 Nordix Foundation
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.common.gson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;
import lombok.ToString;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.gson.annotation.GsonJsonIgnore;
import org.onap.policy.common.gson.annotation.GsonJsonProperty;

class JacksonFieldAdapterFactoryTest {

    private static JacksonFieldAdapterFactory factory = new JacksonFieldAdapterFactory();

    private static Gson gson = new GsonBuilder().setExclusionStrategies(new JacksonExclusionStrategy())
                    .registerTypeAdapterFactory(factory).create();

    @Test
    void testCreate() {
        // unhandled types
        assertNull(factory.create(gson, TypeToken.get(JsonElement.class)));
        assertNull(factory.create(gson, TypeToken.get(NothingToSerialize.class)));

        assertNotNull(factory.create(gson, TypeToken.get(Data.class)));
        assertNotNull(factory.create(gson, TypeToken.get(Derived.class)));

        Data data = new Data();

        // deserialize using fields that aren't in the Data object
        Data data2 = gson.fromJson("{\"abc\":100}", Data.class);
        assertEquals(data.toString(), data2.toString());

        // now work with valid fields
        data.id = 10;
        data.text = "hello";

        String result = gson.toJson(data);
        data2 = gson.fromJson(result, Data.class);
        assertEquals(data.toString(), data2.toString());

        // should also work with derived types
        Derived der = new Derived();
        der.setId(20);
        der.text = "world";
        der.unserialized = "abc";

        result = gson.toJson(der);

        // should not contain the unserialized field
        assertFalse(result.contains("abc"));

        Derived der2 = gson.fromJson(result, Derived.class);
        der.unserialized = null;
        assertEquals(der.toString(), der2.toString());
    }

    @Test
    void testCreate_Lists() {
        DataList lst = new DataList();
        lst.theList = new ArrayList<>();
        lst.theList.add(new Data(200, "text 20"));
        lst.theList.add(new Data(210, "text 21"));

        String result = gson.toJson(lst);
        assertEquals("{'theList':[{'my-id':200,'text':'text 20'},{'my-id':210,'text':'text 21'}]}".replace('\'', '"'),
                        result);

        DataList lst2 = gson.fromJson(result, DataList.class);
        assertEquals(stripIdent(lst.toString()), stripIdent(lst2.toString()));
        assertEquals(Data.class, lst2.theList.get(0).getClass());
    }

    @Test
    void testCreate_OnlyOutProps() {
        InFieldIgnored data = new InFieldIgnored();
        data.value = "out only";

        // field should be serialized
        String result = gson.toJson(data);
        assertEquals("{'value':'out only'}".replace('\'', '"'), result);

        // field should NOT be deserialized
        data = gson.fromJson(result, InFieldIgnored.class);
        assertNull(data.value);
    }

    @Test
    void testCreate_OnlyInProps() {
        OutFieldIgnored data = new OutFieldIgnored();
        data.value = "in only";

        // field should NOT be serialized
        String result = gson.toJson(data);
        assertEquals("{}", result);

        // field should NOT be deserialized
        data = gson.fromJson("{'value':'in only'}".replace('\'', '"'), OutFieldIgnored.class);
        assertEquals("in only", data.value);
    }

    /**
     * Object identifiers may change with each execution, so this method is used to strip
     * the identifier from the text string so that the strings will still match across
     * different runs.
     *
     * @param text text from which to strip the identifier
     * @return the text, without the identifier
     */
    private String stripIdent(String text) {
        return text.replaceFirst("@\\w+", "@");
    }

    @ToString
    private static class Data {
        @GsonJsonProperty("my-id")
        private int id;

        public String text;

        public Data() {
            super();
        }

        public Data(int id, String text) {
            this.id = id;
            this.text = text;
        }

        void setId(int id) {
            this.id = id;
        }
    }

    @ToString(callSuper = true)
    private static class Derived extends Data {
        // not serialized
        private String unserialized;
    }

    private static class DataList {
        @GsonJsonProperty
        private List<Data> theList;
    }

    protected static class NothingToSerialize {
        // not serialized
        protected String unserialized;
    }

    /**
     * This has a field that should show up in the "output" list, but not in the "input"
     * list, because the method will override it.
     */
    private static class InFieldIgnored {
        @GsonJsonProperty("value")
        private String value;

        @GsonJsonIgnore
        public void setValue(String value) {
            this.value = value;
        }
    }

    /**
     * This has a field that should show up in the "input" list, but not in the "output"
     * list, because the method will override it.
     */
    private static class OutFieldIgnored {
        @GsonJsonProperty("value")
        private String value;

        @GsonJsonIgnore
        public String getValue() {
            return value;
        }
    }
}
