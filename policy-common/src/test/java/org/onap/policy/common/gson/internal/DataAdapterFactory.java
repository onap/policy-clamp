/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.common.gson.internal;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.ToString;

/**
 * Factory used with test Data.
 */
public class DataAdapterFactory implements TypeAdapterFactory {

    /**
     * Output of {@link #makeList()}, encoded as json.
     */
    public static final String ENCODED_LIST = "[{'id':100},{'id':101}]".replace('\'', '"');

    /**
     * Output of {@link #makeMap()}, encoded as json.
     */
    public static final String ENCODED_MAP = "'data-100':{'id':100},'data-101':{'id':101}".replace('\'', '"');

    /**
     * Object handled by this factory.
     */
    @ToString
    public static class Data {
        private int id;

        public Data() {
            super();
        }

        public Data(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }
    }

    /**
     * Object derived from Data.
     */
    @ToString(callSuper = true)
    public static class DerivedData extends Data {
        private String text;

        public DerivedData() {
            super();
        }

        public DerivedData(int id, String text) {
            super(id);
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    /**
     * Set to {@code true} when {@link #write(JsonWriter, Data)} has been invoked.
     */
    private boolean dataWritten = false;

    /**
     * Set to {@code true} when {@link #read(JsonReader)} has been invoked.
     */
    private boolean dataRead = false;

    /**
     * Clears the flags that indicate that "read" or "write" has been invoked.
     */
    public void reset() {
        dataWritten = true;
        dataRead = true;
    }

    public boolean isDataWritten() {
        return dataWritten;
    }

    public boolean isDataRead() {
        return dataRead;
    }

    /**
     * Makes a list of Data.
     *
     * @return a new list of Data
     */
    public static List<Data> makeList() {
        List<Data> listField = new ArrayList<>();

        listField.add(new Data(100));
        listField.add(new Data(101));

        return listField;
    }

    /**
     * Makes an array of Data.
     *
     * @return a new array of Data
     */
    public static JsonArray makeArray() {
        JsonArray arr = new JsonArray();

        for (Data data : makeList()) {
            JsonObject json = new JsonObject();
            json.addProperty("id", data.getId());
            arr.add(json);
        }

        return arr;
    }

    /**
     * Makes a map of Data.
     *
     * @return a new map of Data
     */
    public static Map<String, List<Data>> makeMap() {
        Map<String, List<Data>> map = new TreeMap<>();

        for (Data data : makeList()) {
            map.put("data-" + data.getId(), Arrays.asList(data));
        }

        return map;
    }

    /**
     * Adds Data objects to a tree, mirroring {@link #makeMap()}.
     *
     * @param tree tree into which objects are to be added
     */
    public static void addToObject(JsonObject tree) {
        for (JsonElement ent : makeArray()) {
            JsonObject obj = ent.getAsJsonObject();
            JsonArray arr = new JsonArray();
            arr.add(obj);
            tree.add("data-" + obj.get("id").getAsString(), arr);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (type.getRawType() == Data.class) {
            return (TypeAdapter<T>) new DataTypeAdapter(gson.getDelegateAdapter(this, TypeToken.get(Data.class)),
                            gson.getAdapter(JsonElement.class));
        }

        if (type.getRawType() == DerivedData.class) {
            return (TypeAdapter<T>) new DerivedDataTypeAdapter(
                            gson.getDelegateAdapter(this, TypeToken.get(DerivedData.class)),
                            gson.getAdapter(JsonElement.class));
        }

        return null;
    }

    /**
     * Adapter for "Data".
     */
    private class DataTypeAdapter extends TypeAdapter<Data> {
        private TypeAdapter<Data> delegate;
        private TypeAdapter<JsonElement> elementAdapter;

        /**
         * Constructs the object.
         *
         * @param delegate delegate adapter
         * @param elementAdapter element adapter
         */
        public DataTypeAdapter(TypeAdapter<Data> delegate, TypeAdapter<JsonElement> elementAdapter) {
            this.delegate = delegate;
            this.elementAdapter = elementAdapter;
        }

        @Override
        public void write(JsonWriter out, Data data) throws IOException {
            dataWritten = true;

            JsonElement tree = delegate.toJsonTree(data);

            if (tree.isJsonObject()) {
                JsonObject jsonObj = tree.getAsJsonObject();
                jsonObj.addProperty("id", data.getId());
            }

            elementAdapter.write(out, tree);
        }

        @Override
        public Data read(JsonReader in) throws IOException {
            dataRead = true;

            JsonElement tree = elementAdapter.read(in);
            Data data = delegate.fromJsonTree(tree);

            if (tree.isJsonObject()) {
                JsonObject jsonObj = tree.getAsJsonObject();
                data.setId(jsonObj.get("id").getAsInt());
            }

            return data;
        }
    }

    /**
     * Adapter for "DerivedData".
     */
    private class DerivedDataTypeAdapter extends TypeAdapter<DerivedData> {
        private TypeAdapter<DerivedData> delegate;
        private TypeAdapter<JsonElement> elementAdapter;

        /**
         * Constructs the object.
         *
         * @param delegate delegate adapter
         * @param elementAdapter element adapter
         */
        public DerivedDataTypeAdapter(TypeAdapter<DerivedData> delegate, TypeAdapter<JsonElement> elementAdapter) {
            this.delegate = delegate;
            this.elementAdapter = elementAdapter;
        }

        @Override
        public void write(JsonWriter out, DerivedData data) throws IOException {
            dataWritten = true;

            JsonElement tree = delegate.toJsonTree(data);

            if (tree.isJsonObject()) {
                JsonObject jsonObj = tree.getAsJsonObject();
                jsonObj.addProperty("id", data.getId());
                jsonObj.addProperty("text", data.getText());
            }

            elementAdapter.write(out, tree);
        }

        @Override
        public DerivedData read(JsonReader in) throws IOException {
            dataRead = true;

            JsonElement tree = elementAdapter.read(in);
            DerivedData data = delegate.fromJsonTree(tree);

            if (tree.isJsonObject()) {
                JsonObject jsonObj = tree.getAsJsonObject();
                data.setId(jsonObj.get("id").getAsInt());
                data.setText(jsonObj.get("text").getAsString());
            }

            return data;
        }
    }
}
