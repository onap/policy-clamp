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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.onap.policy.common.gson.internal.AnyGetterSerializer;
import org.onap.policy.common.gson.internal.AnySetterDeserializer;
import org.onap.policy.common.gson.internal.ClassWalker;
import org.onap.policy.common.gson.internal.Deserializer;
import org.onap.policy.common.gson.internal.JacksonTypeAdapter;
import org.onap.policy.common.gson.internal.MethodDeserializer;
import org.onap.policy.common.gson.internal.MethodSerializer;
import org.onap.policy.common.gson.internal.Serializer;

/**
 * Factory that serializes/deserializes class methods following the normal behavior of
 * jackson. Supports the following annotations:
 * <ul>
 * <li>GsonJsonIgnore</li>
 * <li>GsonJsonProperty</li>
 * <li>GsonJsonAnyGetter</li>
 * <li>GsonJsonAnySetter</li>
 * </ul>
 */
public class JacksonMethodAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        Class<? super T> clazz = type.getRawType();

        if (!JacksonExclusionStrategy.isManaged(clazz)) {
            return null;
        }

        var data = new ClassWalker();
        data.walkClassHierarchy(clazz);

        if (data.getInProps(Method.class).isEmpty() && data.getOutProps(Method.class).isEmpty()
                        && data.getAnyGetter() == null && data.getAnySetter() == null) {
            // no methods to serialize
            return null;
        }

        Set<String> unliftedProps = new HashSet<>();
        unliftedProps.addAll(data.getInNotIgnored());
        unliftedProps.addAll(data.getOutNotIgnored());

        TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
        List<Serializer> sers = makeSerializers(gson, data, unliftedProps);
        List<Deserializer> desers = makeDeserializers(gson, data, unliftedProps);

        return new JacksonTypeAdapter<>(gson, delegate, sers, desers);
    }

    /**
     * Creates a complete list of serializers.
     *
     * @param gson the associated gson object
     * @param data data used to configure the serializers
     * @param unliftedProps properties that should not be lowered by "any-getters"
     * @return a list of all serializers
     */
    private List<Serializer> makeSerializers(Gson gson, ClassWalker data, Set<String> unliftedProps) {
        List<Serializer> ser = new ArrayList<>();

        if (data.getAnyGetter() != null) {
            ser.add(new AnyGetterSerializer(gson, unliftedProps, data.getAnyGetter()));
        }

        data.getOutProps(Method.class).forEach(method -> ser.add(new MethodSerializer(gson, method)));

        return ser;
    }

    /**
     * Creates a complete list of deserializers.
     *
     * @param gson the associated gson object
     * @param data data used to configure the deserializers
     * @param unliftedProps properties that should not be lifted by "any-setters"
     * @return a list of all deserializers
     */
    private List<Deserializer> makeDeserializers(Gson gson, ClassWalker data, Set<String> unliftedProps) {
        List<Deserializer> deser = new ArrayList<>();

        if (data.getAnySetter() != null) {
            deser.add(new AnySetterDeserializer(gson, unliftedProps, data.getAnySetter()));
        }

        data.getInProps(Method.class).forEach(method -> deser.add(new MethodDeserializer(gson, method)));

        return deser;
    }
}
