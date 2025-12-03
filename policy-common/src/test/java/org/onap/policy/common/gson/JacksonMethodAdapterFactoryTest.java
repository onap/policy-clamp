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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.util.Map;
import java.util.TreeMap;
import lombok.ToString;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.gson.annotation.GsonJsonAnyGetter;
import org.onap.policy.common.gson.annotation.GsonJsonAnySetter;
import org.onap.policy.common.gson.annotation.GsonJsonIgnore;
import org.onap.policy.common.gson.annotation.GsonJsonProperty;

class JacksonMethodAdapterFactoryTest {

    private static JacksonMethodAdapterFactory factory = new JacksonMethodAdapterFactory();

    private static Gson gson = new GsonBuilder().setExclusionStrategies(new JacksonExclusionStrategy())
                    .registerTypeAdapterFactory(factory).create();

    @Test
    void testCreate() {
        // unhandled types
        assertNull(factory.create(gson, TypeToken.get(JsonElement.class)));
        assertNull(factory.create(gson, TypeToken.get(NothingToSerialize.class)));

        assertNotNull(factory.create(gson, TypeToken.get(Data.class)));
        assertNotNull(factory.create(gson, TypeToken.get(Derived.class)));
        assertNotNull(factory.create(gson, TypeToken.get(OnlyGetters.class)));
        assertNotNull(factory.create(gson, TypeToken.get(OnlySetters.class)));
        assertNotNull(factory.create(gson, TypeToken.get(OnlyAnyGetter.class)));
        assertNotNull(factory.create(gson, TypeToken.get(OnlyAnySetter.class)));

        // unhandled type

        Data data = new Data();
        data.id = 10;
        data.text = "some text";

        String result = gson.toJson(data);
        Data data2 = gson.fromJson(result, Data.class);
        assertEquals(data.toString(), data2.toString());

        Derived der = new Derived();
        der.setId(20);
        der.setText("hello");
        der.text = "world";
        der.map = new TreeMap<>();
        der.map.put("mapA", "valA");
        der.map.put("mapB", "valB");

        result = gson.toJson(der);

        // should not contain the unserialized fields
        assertFalse(result.contains("hello"));
        assertFalse(result.contains("world"));

        // null out unserialized fields
        der.text = null;

        // null out overridden field
        der.setText(null);

        Derived der2 = gson.fromJson(result, Derived.class);

        assertEquals(der.toString(), der2.toString());

        // override of AnyGetter
        AnyGetterOverride dblget = new AnyGetterOverride();
        dblget.setMap(der.map);
        dblget.overMap = new TreeMap<>();
        dblget.overMap.put("getA", 100);
        dblget.overMap.put("getB", 110);

        String result2 = gson.toJson(dblget);
        dblget.overMap.keySet().forEach(key -> assertTrue(result2.contains(key), "over contains " + key));
        der.map.keySet().forEach(key -> assertFalse(result2.contains(key), "sub contains " + key));

        // override of AnySetter
        Map<String, Integer> map = new TreeMap<>();
        map.put("setA", 200);
        map.put("setB", 210);
        AnySetterOverride dblset = gson.fromJson(gson.toJson(map), AnySetterOverride.class);
        assertEquals(map.toString(), dblset.overMap.toString());
        assertNull(dblset.getTheMap());

        // non-static nested class - can serialize, but not de-serialize
        Container cont = new Container(500, "bye bye");
        result = gson.toJson(cont);
        assertEquals("{'id':500,'nested':{'value':'bye bye'}}".replace('\'', '"'), result);
    }

    @ToString
    protected static class Data {
        private int id;
        private String text;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        // not public, but property provided
        @GsonJsonProperty("text")
        protected String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public void unused(String text) {
            // do nothing
        }
    }

    @ToString(callSuper = true)
    protected static class Derived extends Data {

        // overrides private field from Data
        public String text;

        private Map<String, String> map;

        @GsonJsonAnyGetter
        public Map<String, String> getTheMap() {
            return map;
        }

        @GsonJsonIgnore
        public void setMap(Map<String, String> map) {
            this.map = map;
        }

        @GsonJsonAnySetter
        public void setMapValue(String key, String value) {
            if (map == null) {
                map = new TreeMap<>();
            }

            map.put(key, value);
        }
    }

    /**
     * Has {@link GsonJsonAnyGetter} method that overrides the super class' method.
     */
    private static class AnyGetterOverride extends Derived {
        private Map<String, Integer> overMap;

        @GsonJsonAnyGetter
        private Map<String, Integer> getOverride() {
            return overMap;
        }
    }

    /**
     * Has {@link GsonJsonAnySetter} method that overrides the super class' method.
     */
    private static class AnySetterOverride extends Derived {
        private Map<String, Integer> overMap;

        @GsonJsonAnySetter
        private void setOverride(String key, int value) {
            if (overMap == null) {
                overMap = new TreeMap<>();
            }

            overMap.put(key, value);
        }
    }

    /**
     * Has nothing to serialize.
     */
    protected static class NothingToSerialize {
        // not serialized
        protected String unserialized;
    }

    /**
     * Only has getters.
     */
    protected static class OnlyGetters {
        public int getId() {
            return 1010;
        }
    }

    /**
     * Only has setters.
     */
    protected static class OnlySetters {
        public void setId(int id) {
            // do nothing
        }
    }

    /**
     * Only has {@link GsonJsonAnyGetter}.
     */
    private static class OnlyAnyGetter {
        @GsonJsonAnyGetter
        public Map<String, Integer> getOverride() {
            return null;
        }
    }

    /**
     * Only has {@link GsonJsonAnySetter}.
     */
    private static class OnlyAnySetter {
        @GsonJsonAnySetter
        public void setOverride(String key, int value) {
            // do nothing
        }
    }

    /**
     * Used to test serialization of non-static nested classes.
     */
    @ToString
    protected static class Container {
        private int id;
        private Nested nested;

        public Container() {
            super();
        }

        public Container(int id, String value) {
            this.id = id;
            this.nested = new Nested(value);
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public Nested getNested() {
            return nested;
        }


        @ToString
        protected class Nested {
            private String value;

            public Nested(String val) {
                value = val;
            }

            public String getValue() {
                return value;
            }
        }
    }
}
