/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
 * Modificaitons Copyright (C) 2023-2025 Nordix Foundation.
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import java.io.Serial;
import java.lang.reflect.GenericArrayType;
import java.util.LinkedList;
import java.util.TreeMap;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class JacksonExclusionStrategyTest {

    private static JacksonExclusionStrategy strategy;
    private static Gson gson;

    @BeforeAll
    public static void setUpBeforeClass() {
        strategy = new JacksonExclusionStrategy();
        gson = new GsonBuilder().setExclusionStrategies(strategy).create();
    }

    @Test
    void testWithGson() {
        Derived data = new Derived();
        data.setId(10);
        data.setText("some text");
        data.setValue("some value");

        // no fields should be serialized
        String result = gson.toJson(data);
        assertEquals("{}", result);

        // no fields should be deserialized
        result = "{'id':20, 'text':'my text', 'value':'my value'}".replace('\'', '"');
        Derived data2 = gson.fromJson(result, Derived.class);
        assertEquals(new Derived().toString(), data2.toString());
    }

    @Test
    void testShouldSkipField() throws Exception {
        // should skip every field of Data
        assertTrue(strategy.shouldSkipField(new FieldAttributes(Data.class.getDeclaredField("id"))));
        assertTrue(strategy.shouldSkipField(new FieldAttributes(Data.class.getDeclaredField("text"))));

        // should not skip fields in Map
        assertFalse(strategy.shouldSkipField(new FieldAttributes(MyMap.class.getDeclaredField("mapId"))));
    }

    @Test
    void testShouldSkipClass() {
        assertFalse(strategy.shouldSkipClass(null));
        assertFalse(strategy.shouldSkipClass(Object.class));
    }

    @Test
    void testIsManaged() {
        // these classes SHOULD be managed
        Class<?>[] managed = {Data.class, Intfc.class, com.google.gson.TypeAdapter.class};

        for (Class<?> clazz : managed) {
            assertTrue(JacksonExclusionStrategy.isManaged(clazz), clazz.getName());
        }

        // generic classes should NOT be managed
        Class<?>[] unmanaged = {
            Data[].class, Enum.class, boolean.class, byte.class, short.class, int.class,
            long.class, float.class, double.class, char.class, Boolean.class, Byte.class, Short.class,
            Integer.class, Long.class, Float.class, Double.class, Character.class, String.class,
            MyMap.class, MyList.class, MyJson.class, GenericArrayType.class};

        for (Class<?> clazz : unmanaged) {
            assertFalse(JacksonExclusionStrategy.isManaged(clazz), clazz.getName());
        }
    }

    /**
     * Used to verify that no fields are exposed.
     */
    @Getter
    @Setter
    @ToString
    public static class Data {
        private int id;
        public String text;

        void setId(int id) {
            this.id = id;
        }

        void setText(String text) {
            this.text = text;
        }
    }

    @Getter
    @ToString(callSuper = true)
    public static class Derived extends Data {
        protected String value;

        void setValue(String value) {
            this.value = value;
        }
    }

    /**
     * Used to verify that enums are not managed.
     */
    public enum Enum {
        UP, DOWN,
    }

    /**
     * Used to verify that interfaces <i>are</i> managed.
     */
    public interface Intfc {
        int getId();
    }

    /**
     * Used to verify that Maps are not managed.
     */
    @Getter
    public static class MyMap extends TreeMap<String, Data> {
        @Serial
        private static final long serialVersionUID = 1L;

        private int mapId;

    }

    /**
     * Used to verify that Collections are not managed.
     */
    public static class MyList extends LinkedList<Data> {
        @Serial
        private static final long serialVersionUID = 1L;
    }

    /**
     * Used to verify that JsonElements are not managed.
     */
    @SuppressWarnings("deprecation")
    public static class MyJson extends JsonElement {
        @Override
        public JsonElement deepCopy() {
            return null;
        }
    }
}
