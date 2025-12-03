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
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.List;


/**
 * Adapter for a single class that implements a jackson-style behavior.
 *
 * @param <T> type of class on which the adapter works
 */
public class JacksonTypeAdapter<T> extends TypeAdapter<T> {

    /**
     * Used to create an object of the given class.
     */
    private final TypeAdapter<T> delegate;

    /**
     * Used to serialize/deserialize a JsonElement.
     */
    private final TypeAdapter<JsonElement> elementAdapter;

    /**
     * Serializers for each item within the object.
     */
    private final Serializer[] serializers;

    /**
     * Deserializers for each item within the object.
     */
    private final Deserializer[] deserializers;

    /**
     * Constructs the object.
     *
     * @param gson the associated gson object
     * @param delegate default constructor for the type
     * @param serializers the serializers to use to serialize items within the object
     * @param deserializers the deserializers to use to deserialize items into the object
     */
    public JacksonTypeAdapter(Gson gson, TypeAdapter<T> delegate, List<Serializer> serializers,
                    List<Deserializer> deserializers) {
        this.delegate = delegate;
        this.elementAdapter = gson.getAdapter(JsonElement.class);
        this.serializers = serializers.toArray(new Serializer[0]);
        this.deserializers = deserializers.toArray(new Deserializer[0]);
    }

    @Override
    public void write(JsonWriter out, T value) throws IOException {
        JsonElement tree = delegate.toJsonTree(value);

        if (tree.isJsonObject()) {
            var jsonObj = tree.getAsJsonObject();

            // serialize each item from the value into the target tree
            for (Serializer serializer : serializers) {
                serializer.addToTree(value, jsonObj);
            }
        }

        elementAdapter.write(out, tree);
    }

    @Override
    public T read(JsonReader in) throws IOException {
        JsonElement tree = elementAdapter.read(in);
        var object = delegate.fromJsonTree(tree);

        if (tree.isJsonObject()) {
            var jsonObj = tree.getAsJsonObject();

            // deserialize each item from the tree into the target object
            for (Deserializer dser : deserializers) {
                dser.getFromTree(jsonObj, object);
            }
        }

        return object;
    }
}
