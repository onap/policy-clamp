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

package org.onap.policy.common.gson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Adapter factory for Map&lt;String,Object&gt; and List&lt;String&gt;. By default, GSON treats all Objects, that
 * are numbers, as Double. This recursively walks a map/list and converts Doubles to Integer or Long, if
 * possible.
 */
public class MapDoubleAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!isMapType(type) && !isListType(type)) {
            return null;
        }

        TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);

        return new MapAdapter<>(delegate);
    }

    private <T> boolean isMapType(TypeToken<T> type) {
        if (!Map.class.isAssignableFrom(type.getRawType())) {
            return false;
        }

        // only supports Map<String,Object>

        if (!(type.getType() instanceof ParameterizedType)) {
            // untyped - assume the parameters are the correct type
            return true;
        }

        Type[] actualParams = ((ParameterizedType) type.getType()).getActualTypeArguments();

        return (actualParams[0] == String.class && actualParams[1] == Object.class);
    }

    private <T> boolean isListType(TypeToken<T> type) {
        if (!List.class.isAssignableFrom(type.getRawType())) {
            return false;
        }

        // only supports List<Object>

        if (!(type.getType() instanceof ParameterizedType)) {
            // untyped - assume the parameters are the correct type
            return true;
        }

        Type[] actualParams = ((ParameterizedType) type.getType()).getActualTypeArguments();

        return (actualParams[0] == Object.class);
    }

    /**
     * Type adapter that performs conversion from Double to Integer/Long.
     *
     * @param <T> type of object on which this works (always Map.class)
     */
    private static class MapAdapter<T> extends TypeAdapter<T> {

        /**
         * Used to perform conversion between JSON and Map&lt;String,Object&gt;.
         */
        private final TypeAdapter<T> delegate;

        /**
         * Constructs the object.
         *
         * @param delegate JSON/Map converter
         */
        public MapAdapter(TypeAdapter<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void write(JsonWriter out, T value) throws IOException {
            delegate.write(out, value);
        }

        @Override
        public T read(JsonReader in) throws IOException {
            var value = delegate.read(in);

            DoubleConverter.convertFromDouble(value);

            return value;
        }
    }
}
