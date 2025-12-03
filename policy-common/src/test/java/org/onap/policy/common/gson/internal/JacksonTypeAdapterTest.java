/*
 * ============LICENSE_START==============================================================
 * ONAP
 * =======================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
 * =======================================================================================
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END================================================================
 */

package org.onap.policy.common.gson.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import lombok.ToString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JacksonTypeAdapterTest {
    private static final String HELLO = "hello";
    private static final String WORLD = "world";

    /**
     * Gson object that excludes fields, as we're going to process the fields ourselves.
     */
    private static final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    private JacksonTypeAdapter<Data> adapter;
    private List<Serializer> sers;
    private List<Deserializer> desers;

    /**
     * Initializes the previously defined fields.
     */
    @BeforeEach
    void setUp() {
        // create list of serializers, one for "id" and one for "value"
        sers = new ArrayList<>(2);
        sers.add(new NamedSer(HELLO) {
            @Override
            protected String getValue(Data data) {
                return data.id;
            }
        });
        sers.add(new NamedSer(WORLD) {
            @Override
            protected String getValue(Data data) {
                return data.value;
            }
        });

        // create list of deserializers, one for "id" and one for "value"
        desers = new ArrayList<>(2);
        desers.add(new NamedDeser(HELLO) {
            @Override
            protected void setValue(Data data, String value) {
                data.id = value;
            }
        });
        desers.add(new NamedDeser(WORLD) {
            @Override
            protected void setValue(Data data, String value) {
                data.value = value;
            }
        });

        TypeAdapterFactory factory = new TypeAdapterFactory() {
            @Override
            public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
                return null;
            }
        };

        TypeAdapter<Data> delegate = gson.getDelegateAdapter(factory, TypeToken.get(Data.class));

        adapter = new JacksonTypeAdapter<>(gson, delegate, sers, desers);
    }

    @Test
    void testWriteJsonWriterT() throws Exception {
        Data data = new Data("abc", "def");

        StringWriter wtr = new StringWriter();
        adapter.write(new JsonWriter(wtr), data);

        assertEquals("{'hello':'abc','world':'def'}".replace('\'', '"'), wtr.toString());
    }

    /**
     * Tests the case where the delegate does not return a JsonObject.
     *
     * @throws Exception if an error occurs
     */
    @Test
    void testWriteJsonWriterT_NotAnObject() throws Exception {
        TypeAdapter<String> delegate = gson.getAdapter(String.class);
        JacksonTypeAdapter<String> stringAdapter = new JacksonTypeAdapter<>(gson, delegate, sers, desers);

        StringWriter wtr = new StringWriter();
        stringAdapter.write(new JsonWriter(wtr), "write text");

        assertEquals("'write text'".replace('\'', '"'), wtr.toString());
    }

    @Test
    void testReadJsonReader() throws Exception {
        Data data = adapter
                        .read(new JsonReader(new StringReader("{'hello':'four','world':'score'}".replace('\'', '"'))));

        assertEquals(new Data("four", "score").toString(), data.toString());
    }

    /**
     * Tests the case where the delegate does not use a JsonObject.
     *
     * @throws Exception if an error occurs
     */
    @Test
    void testReadJsonReader_NotAnObject() throws Exception {
        TypeAdapter<String> delegate = gson.getAdapter(String.class);
        JacksonTypeAdapter<String> stringAdapter = new JacksonTypeAdapter<>(gson, delegate, sers, desers);

        String data = stringAdapter.read(new JsonReader(new StringReader("'read text'".replace('\'', '"'))));

        assertEquals("read text", data);
    }

    @ToString
    private static class Data {
        private String id;
        private String value;

        /*
         * This is invoked by gson via reflection, thus no direct invocation. Hence it has
         * to be labeled "unused".
         */
        @SuppressWarnings("unused")
        public Data() {
            super();
        }

        public Data(String id, String value) {
            this.id = id;
            this.value = value;
        }
    }

    private abstract static class NamedSer implements Serializer {
        private final String name;

        /**
         * Constructs the object.
         *
         * @param name the name of the field, when stored in a JsonObject
         */
        public NamedSer(String name) {
            this.name = name;
        }

        @Override
        public void addToTree(Object source, JsonObject target) {
            Data data = (Data) source;
            target.addProperty(name, getValue(data));
        }

        protected abstract String getValue(Data data);
    }

    private abstract static class NamedDeser implements Deserializer {
        private final String name;

        /**
         * Constructs the object.
         *
         * @param name the name of the field, when stored in a JsonObject
         */
        public NamedDeser(String name) {
            this.name = name;
        }

        @Override
        public void getFromTree(JsonObject source, Object target) {
            Data data = (Data) target;
            setValue(data, source.get(name).getAsString());
        }

        protected abstract void setValue(Data data, String value);
    }
}
