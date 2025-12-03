/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.function.Function;

/**
 * GSON Type Adapter for fields that are encoded as Strings.
 */
public class StringTypeAdapter<T> extends TypeAdapter<T> {
    private final String exMessage;
    private final Function<String, T> deserializer;
    private final Function<T, String> serializer;

    /**
     * Constructs an adapter.
     *
     * @param type type of value, used in exception messages
     * @param deserializer function used to deserialize a String into a value
     * @param serializer function used to serialize a value into a String
     */
    public StringTypeAdapter(String type, Function<String, T> deserializer, Function<T, String> serializer) {
        this.exMessage = "invalid " + type;
        this.deserializer = deserializer;
        this.serializer = serializer;
    }

    @Override
    public T read(JsonReader in) throws IOException {
        try {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            } else {
                return deserializer.apply(in.nextString());
            }

        } catch (RuntimeException e) {
            throw new JsonParseException(exMessage, e);
        }
    }

    @Override
    public void write(JsonWriter out, T value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            String text = serializer.apply(value);
            out.value(text);
        }
    }
}
