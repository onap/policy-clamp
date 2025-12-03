/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.JsonElement;
import java.lang.reflect.GenericArrayType;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Excludes all fields from serialization/deserialization, if the class is managed.
 */
public class JacksonExclusionStrategy implements ExclusionStrategy {

    /**
     * Classes that are explicitly not managed by the GSON jackson adapters.
     */
    // @formatter:off
    private static final Set<Class<?>> unmanaged = new HashSet<>(Arrays.asList(
                    boolean.class,
                    byte.class,
                    short.class,
                    int.class,
                    long.class,
                    float.class,
                    double.class,
                    char.class,
                    Boolean.class,
                    Byte.class,
                    Short.class,
                    Integer.class,
                    Long.class,
                    Float.class,
                    Double.class,
                    Character.class,
                    String.class));
    // @formatter:on

    /**
     * Classes whose subclasses are explicitly not managed by the GSON jackson adapters.
     */
    // @formatter:off
    private static final Set<Class<?>> unmanagedSuper = new HashSet<>(Arrays.asList(
                    GenericArrayType.class,
                    Map.class,
                    Collection.class,
                    JsonElement.class));
    // @formatter:on

    @Override
    public boolean shouldSkipField(FieldAttributes attrs) {
        return isManaged(attrs.getDeclaringClass());
    }

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
        return false;
    }

    /**
     * Determines if a class is managed by this adapter, which typically means that it is
     * <i>not</i> a generic class such as {@link JsonElement} or some type of collection.
     *
     * @param clazz the class to be examined
     * @return {@code true} if the class is managed by this adapter, {@code false}
     *         otherwise
     */
    public static boolean isManaged(Class<?> clazz) {
        if (clazz.isArray() || clazz.isEnum() || clazz.isPrimitive() || unmanaged.contains(clazz)) {
            return false;
        }

        for (Class<?> sup : unmanagedSuper) {
            if (sup.isAssignableFrom(clazz)) {
                return false;
            }
        }

        return true;
    }
}
