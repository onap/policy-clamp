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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.onap.policy.common.gson.internal.ClassWalker;
import org.onap.policy.common.gson.internal.Deserializer;
import org.onap.policy.common.gson.internal.FieldDeserializer;
import org.onap.policy.common.gson.internal.FieldSerializer;
import org.onap.policy.common.gson.internal.JacksonTypeAdapter;
import org.onap.policy.common.gson.internal.Serializer;

/**
 * Factory that serializes/deserializes class fields following the normal behavior of
 * jackson. Supports the following annotations:
 * <ul>
 * <li>GsonJsonIgnore</li>
 * <li>GsonJsonProperty</li>
 * </ul>
 *
 * <p>Note: {@link JacksonExclusionStrategy} must also be registered with the gson object.
 */
public class JacksonFieldAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<? super T> clazz = type.getRawType();

        if (!JacksonExclusionStrategy.isManaged(clazz)) {
            return null;
        }

        var data = new ClassWalker();
        data.walkClassHierarchy(clazz);

        if (data.getInProps(Field.class).isEmpty() && data.getOutProps(Field.class).isEmpty()) {
            // no fields to serialize
            return null;
        }

        TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
        List<Serializer> sers = makeSerializers(gson, data);
        List<Deserializer> desers = makeDeserializers(gson, data);

        return new JacksonTypeAdapter<>(gson, delegate, sers, desers);
    }

    /**
     * Creates a complete list of serializers.
     *
     * @param gson the associated gson object
     * @param data data used to configure the serializers
     * @return a list of all serializers
     */
    private List<Serializer> makeSerializers(Gson gson, ClassWalker data) {
        List<Serializer> ser = new ArrayList<>();

        data.getOutProps(Field.class).forEach(field -> ser.add(new FieldSerializer(gson, field)));

        return ser;
    }

    /**
     * Creates a complete list of deserializers.
     *
     * @param gson the associated gson object
     * @param data data used to configure the deserializers
     * @return a list of all deserializers
     */
    private List<Deserializer> makeDeserializers(Gson gson, ClassWalker data) {
        List<Deserializer> deser = new ArrayList<>();

        data.getInProps(Field.class).forEach(field -> deser.add(new FieldDeserializer(gson, field)));

        return deser;
    }
}
