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

import com.google.gson.JsonParseException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.Getter;
import org.onap.policy.common.gson.annotation.GsonJsonAnyGetter;
import org.onap.policy.common.gson.annotation.GsonJsonAnySetter;
import org.onap.policy.common.gson.annotation.GsonJsonIgnore;
import org.onap.policy.common.gson.annotation.GsonJsonProperty;

/**
 * Data populated while walking the hierarchy of a class.
 */
public class ClassWalker {

    public static final String ANY_GETTER_MISMATCH_ERR =
                    GsonJsonAnyGetter.class.getSimpleName() + " parameter mismatch for: ";

    public static final String ANY_SETTER_MISMATCH_ERR =
                    GsonJsonAnySetter.class.getSimpleName() + " parameter mismatch for: ";

    public static final String ANY_SETTER_TYPE_ERR =
                    GsonJsonAnySetter.class.getSimpleName() + " first parameter must be a string: ";

    /**
     * Maps an input property name to an item within the class, where item is one of:
     * {@link Field}, {@link Method}, or {@code null}. Entries are overwritten as new
     * items are added.
     */
    private final Map<String, Object> inProps = new HashMap<>();

    /**
     * Maps an output property name to an item within the class, where item is one of:
     * {@link Field}, {@link Method}, or {@code null}. Entries are overwritten as new
     * items are added.
     */
    private final Map<String, Object> outProps = new HashMap<>();

    /**
     * Maps a method name to a "get" method. Used when overriding properties associated
     * with a method.
     */
    private final Map<String, Method> getters = new HashMap<>();

    /**
     * Maps a method name to a "set" method. Used when overriding properties associated
     * with a method.
     */
    private final Map<String, Method> setters = new HashMap<>();

    /**
     * Method having {@link GsonJsonAnyGetter} annotation. Overwritten as new "any-getters"
     * are identified.
     */
    @Getter
    private Method anyGetter = null;

    /**
     * Method having {@link GsonJsonAnySetter} annotation. Overwritten as new "any-setters"
     * are identified.
     */
    @Getter
    private Method anySetter = null;

    /**
     * Gets the names of input properties that are not being ignored.
     *
     * @return the non-ignored input property names
     */
    public List<String> getInNotIgnored() {
        return getNonNull(inProps);
    }

    /**
     * Gets the names of output properties that are not being ignored.
     *
     * @return the non-ignored output property names
     */
    public List<String> getOutNotIgnored() {
        return getNonNull(outProps);
    }

    /**
     * Gets the property names, associated with a non-null value, from a set of
     * properties.
     *
     * @param props set of properties from which to extract the names
     * @return the property names having a non-null value
     */
    private List<String> getNonNull(Map<String, Object> props) {
        List<String> lst = new ArrayList<>(props.size());

        for (Entry<String, Object> ent : props.entrySet()) {
            if (ent.getValue() != null) {
                lst.add(ent.getKey());
            }
        }

        return lst;
    }

    /**
     * Gets the input properties whose values are of the given class.
     *
     * @param clazz class of properties to get
     * @return the input properties of the given class
     */
    public <T> List<T> getInProps(Class<T> clazz) {
        return getProps(clazz, inProps.values());
    }

    /**
     * Gets the output properties whose values are of the given class.
     *
     * @param clazz class of properties to get
     * @return the output properties of the given class
     */
    public <T> List<T> getOutProps(Class<T> clazz) {
        return getProps(clazz, outProps.values());
    }

    /**
     * Gets the properties whose values are of the given class.
     *
     * @param clazz class of properties to get
     * @param values values from which to select
     * @return the output properties of the given class
     */
    @SuppressWarnings("unchecked")
    private <T> List<T> getProps(Class<T> clazz, Collection<Object> values) {
        List<T> lst = new ArrayList<>(values.size());

        for (Object val : values) {
            if (val != null && val.getClass() == clazz) {
                lst.add((T) val);
            }
        }

        return lst;
    }

    /**
     * Recursively walks a class hierarchy, including super classes and interfaces,
     * examining each class for various annotations.
     *
     * @param clazz class whose hierarchy is to be walked
     */
    public void walkClassHierarchy(Class<?> clazz) {
        if (clazz == Object.class) {
            return;
        }

        // walk interfaces first
        for (Class<?> intfc : clazz.getInterfaces()) {
            walkClassHierarchy(intfc);
        }

        // walk superclass next, overwriting previous items
        Class<?> sup = clazz.getSuperclass();
        if (sup != null) {
            walkClassHierarchy(sup);
        }

        // finally, examine this class, overwriting previous items
        examine(clazz);
    }

    /**
     * Examines a class for annotations, examining fields and then methods.
     *
     * @param clazz class to be examined
     */
    protected void examine(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            examine(field);
        }

        for (Method method : clazz.getDeclaredMethods()) {
            examine(method);
        }
    }

    /**
     * Examines a field for annotations.
     *
     * @param field field to be examined
     */
    protected void examine(Field field) {
        if (field.isSynthetic()) {
            return;
        }

        int mod = field.getModifiers();

        if (Modifier.isStatic(mod)) {
            // skip static fields
            return;
        }

        if (!Modifier.isPublic(mod) && field.getAnnotation(GsonJsonProperty.class) == null) {
            // private/protected - skip it unless explicitly exposed
            return;
        }

        if (Modifier.isTransient(mod) && field.getAnnotation(GsonJsonProperty.class) == null) {
            // transient - skip it unless explicitly exposed
            return;
        }

        String name = detmPropName(field);
        if (name == null) {
            // invalid name
            return;
        }

        // if ignoring, then insert null into the map, otherwise insert the field
        Field annotField = (field.getAnnotation(GsonJsonIgnore.class) != null ? null : field);

        // a field can be both an input and an output

        inProps.put(name, annotField);
        outProps.put(name, annotField);
    }

    /**
     * Examines a method for annotations.
     *
     * @param method method to be examined
     */
    protected void examine(Method method) {
        if (method.isSynthetic()) {
            return;
        }

        int mod = method.getModifiers();

        if (Modifier.isStatic(mod)) {
            // static methods are not exposed
            return;
        }

        GsonJsonProperty prop = method.getAnnotation(GsonJsonProperty.class);
        GsonJsonAnyGetter get = method.getAnnotation(GsonJsonAnyGetter.class);
        GsonJsonAnySetter set = method.getAnnotation(GsonJsonAnySetter.class);

        if (!Modifier.isPublic(mod) && prop == null && get == null && set == null) {
            // private/protected methods are not exposed, unless annotated
            return;
        }


        if (method.getReturnType() == void.class) {
            // "void" return type - must be a "setter" method
            if (set == null) {
                examineSetter(method);

            } else {
                examineAnySetter(method);
            }

        } else {
            // must be a "getter" method
            if (get == null) {
                examineGetter(method);

            } else {
                examineAnyGetter(method);
            }
        }
    }

    /**
     * Examines a "setter" method.
     *
     * @param method method to be examined
     */
    private void examineSetter(Method method) {
        String name = Adapter.detmSetterPropName(method);
        if (name != null && method.getParameterCount() == 1) {
            // remove old name mapping, if any
            Method old = setters.get(method.getName());
            if (old != null) {
                inProps.remove(Adapter.detmSetterPropName(old));
            }

            setters.put(method.getName(), method);

            // if ignoring, then insert null into the map, otherwise insert the method
            inProps.put(name, (method.getAnnotation(GsonJsonIgnore.class) != null ? null : method));
        }
    }

    /**
     * Examines a "getter" method.
     *
     * @param method method to be examined
     */
    private void examineGetter(Method method) {
        String name = Adapter.detmGetterPropName(method);
        if (name != null && method.getParameterCount() == 0) {
            // remove old name mapping, if any
            Method old = getters.get(method.getName());
            if (old != null) {
                outProps.remove(Adapter.detmGetterPropName(old));
            }

            getters.put(method.getName(), method);

            // if ignoring, then insert null into the map, otherwise insert the method
            outProps.put(name, (method.getAnnotation(GsonJsonIgnore.class) != null ? null : method));
        }
    }

    /**
     * Examines a method having a {@link GsonJsonAnySetter} annotation.
     *
     * @param method method to be examined
     */
    private void examineAnySetter(Method method) {
        if (method.getParameterCount() != 2) {
            throw new JsonParseException(ANY_SETTER_MISMATCH_ERR + getFqdn(method));
        }

        if (method.getParameterTypes()[0] != String.class) {
            throw new JsonParseException(ANY_SETTER_TYPE_ERR + getFqdn(method));
        }

        // if ignoring, then use null, otherwise use the method
        anySetter = (method.getAnnotation(GsonJsonIgnore.class) != null ? null : method);
    }

    /**
     * Examines a method having a {@link GsonJsonAnyGetter} annotation.
     *
     * @param method method to be examined
     */
    private void examineAnyGetter(Method method) {
        if (method.getParameterCount() != 0) {
            throw new JsonParseException(ANY_GETTER_MISMATCH_ERR + getFqdn(method));
        }

        // if ignoring, then use null, otherwise use the method
        anyGetter = (method.getAnnotation(GsonJsonIgnore.class) != null ? null : method);
    }

    /**
     * Gets the fully qualified name of a method.
     *
     * @param method method whose name is desired
     * @return the fully qualified method name
     */
    private String getFqdn(Method method) {
        return (method.getDeclaringClass().getName() + "." + method.getName());
    }

    // these may be overridden by junit tests

    protected String detmPropName(Field field) {
        return Adapter.detmPropName(field);
    }
}
