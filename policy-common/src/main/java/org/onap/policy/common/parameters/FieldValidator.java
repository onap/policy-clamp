/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.common.parameters;

import com.google.gson.annotations.SerializedName;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.onap.policy.common.parameters.annotations.NotNull;

/**
 * Validator of the contents of a field, supporting the parameter annotations.
 */
public class FieldValidator extends ValueValidator {

    /**
     * {@code True} if there is a field-level annotation, {@code false} otherwise.
     */
    @Getter
    @Setter(AccessLevel.PROTECTED)
    private boolean fieldAnnotated = false;

    /**
     * Class containing the field of interest.
     */
    private final Class<?> clazz;

    /**
     * Field of interest.
     */
    private final Field field;

    /**
     * Name of the field when serialized (i.e., as the client would know it).
     */
    private final String serializedName;

    /**
     * Method to retrieve the field's value.
     */
    private Method accessor;


    /**
     * Constructs the object.
     *
     * @param validator provider of validation methods
     * @param clazz class containing the field
     * @param field field whose value is to be validated
     */
    public FieldValidator(BeanValidator validator, Class<?> clazz, Field field) {
        this.clazz = clazz;
        this.field = field;

        String fieldName = field.getName();
        if (fieldName.contains("$")) {
            serializedName = fieldName;
            return;
        }

        SerializedName serAnnot = field.getAnnotation(SerializedName.class);
        serializedName = (serAnnot != null ? serAnnot.value() : fieldName);

        validator.addValidators(this);
        addListValidator(validator);
        addMapValidator(validator);

        if (checkers.isEmpty()) {
            // has no annotations - nothing to check
            return;
        }

        // verify the field type is of interest
        int mod = field.getModifiers();
        if (Modifier.isStatic(mod)) {
            classOnly(clazz.getName() + "." + fieldName + " is annotated but the field is static");
            checkers.clear();
            return;
        }

        // get the field's "getter" method
        accessor = getAccessor(clazz, fieldName);
        if (accessor == null) {
            classOnly(clazz.getName() + "." + fieldName + " is annotated but has no \"get\" method");
            checkers.clear();
            return;
        }

        // determine if null is allowed
        if (field.getAnnotation(NotNull.class) != null || clazz.getAnnotation(NotNull.class) != null) {
            setNullAllowed(false);
        }
    }

    /**
     * Adds validators for the individual items within a collection, if the field is a
     * collection.
     *
     * @param validator provider of validation methods
     */
    private void addListValidator(BeanValidator validator) {
        if (!Collection.class.isAssignableFrom(field.getType())) {
            return;
        }

        var tannot = field.getAnnotatedType();
        if (!(tannot instanceof AnnotatedParameterizedType)) {
            return;
        }

        AnnotatedType[] targs = ((AnnotatedParameterizedType) tannot).getAnnotatedActualTypeArguments();
        if (targs.length != 1) {
            return;
        }

        var itemValidator = new ItemValidator(validator, targs[0]);
        if (itemValidator.isEmpty()) {
            return;
        }

        checkers.add((result, fieldName, value) -> validator.verCollection(result, fieldName, itemValidator, value));
    }

    /**
     * Adds validators for the individual entries within a map, if the field is a map.
     *
     * @param validator provider of validation methods
     */
    private void addMapValidator(BeanValidator validator) {
        if (!Map.class.isAssignableFrom(field.getType())) {
            return;
        }

        var tannot = field.getAnnotatedType();
        if (!(tannot instanceof AnnotatedParameterizedType)) {
            return;
        }

        AnnotatedType[] targs = ((AnnotatedParameterizedType) tannot).getAnnotatedActualTypeArguments();
        if (targs.length != 2) {
            return;
        }

        var keyValidator = new ItemValidator(validator, targs[0]);
        var valueValidator = new ItemValidator(validator, targs[1]);
        if (keyValidator.isEmpty() && valueValidator.isEmpty()) {
            return;
        }

        checkers.add((result, fieldName, value) -> validator.verMap(result, fieldName, keyValidator, valueValidator,
                        value));
    }

    /**
     * Performs validation of a single field.
     *
     * @param result validation results are added here
     * @param object object whose field is to be validated
     */
    public void validateField(BeanValidationResult result, Object object) {
        if (isEmpty()) {
            // has no annotations - nothing to check
            return;
        }

        // get the value
        Object value = getValue(object, accessor);

        validateValue(result, serializedName, value);
    }

    /**
     * Gets the value from the object using the accessor function.
     *
     * @param object object whose value is to be retrieved
     * @param accessor "getter" method
     * @return the object's value
     */
    private Object getValue(Object object, Method accessor) {
        try {
            return accessor.invoke(object);

        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new IllegalArgumentException(clazz.getName() + "." + field.getName() + " accessor threw an exception",
                            e);
        }
    }

    /**
     * Throws an exception if there are field-level annotations.
     *
     * @param exceptionMessage exception message
     */
    private void classOnly(String exceptionMessage) {
        if (isFieldAnnotated()) {
            throw new IllegalArgumentException(exceptionMessage);
        }
    }

    /**
     * Gets an annotation from the field or the class.
     *
     * @param annotClass annotation class of interest
     * @return the annotation, or {@code null} if neither the field nor the class has the
     *         desired annotation
     */
    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotClass) {

        // field annotation takes precedence over class annotation
        var annot = field.getAnnotation(annotClass);
        if (annot != null) {
            setFieldAnnotated(true);
            return annot;
        }

        return clazz.getAnnotation(annotClass);
    }

    /**
     * Gets an accessor method for the given field.
     *
     * @param clazz class whose methods are to be searched
     * @param fieldName field whose "getter" is to be identified
     * @return the field's "getter" method, or {@code null} if it is not found
     */
    private Method getAccessor(Class<?> clazz, String fieldName) {
        var capname = StringUtils.capitalize(fieldName);
        var accessor2 = getMethod(clazz, "get" + capname);
        if (accessor2 != null) {
            return accessor2;
        }

        return getMethod(clazz, "is" + capname);
    }

    /**
     * Gets the "getter" method having the specified name.
     *
     * @param clazz class whose methods are to be searched
     * @param methodName name of the method of interest
     * @return the method, or {@code null} if it is not found
     */
    private Method getMethod(Class<?> clazz, String methodName) {
        for (Method method : clazz.getMethods()) {
            if (methodName.equals(method.getName()) && validMethod(method)) {
                return method;
            }
        }

        return null;
    }

    /**
     * Determines if a method is a valid "getter".
     *
     * @param method method to be checked
     * @return {@code true} if the method is a valid "getter", {@code false} otherwise
     */
    private boolean validMethod(Method method) {
        int mod = method.getModifiers();
        return !(Modifier.isStatic(mod) || method.getReturnType() == void.class || method.getParameterCount() != 0);
    }
}
