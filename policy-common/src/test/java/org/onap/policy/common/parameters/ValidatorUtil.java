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

package org.onap.policy.common.parameters;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;

/**
 * Utilities for validator tests.
 */
public class ValidatorUtil {
    protected static final String MY_NAME = "My-Name";
    protected static final String MY_FIELD = "My-Field";
    protected static final String HELLO = "hello";

    protected BeanValidator bean;

    /**
     * Gets the single annotation for a given field.
     *
     * @param fieldName name of the field having the desired annotation
     * @return the given field's annotation
     */
    protected Annotation getAnnot(String fieldName) {
        return getField(fieldName).getAnnotations()[0];
    }

    /**
     * Gets the annotated type for a given field.
     *
     * @param fieldName name of the field of interest
     * @return the given field's annotated type
     */
    protected AnnotatedType getAnnotType(String fieldName) {
        return getField(fieldName).getAnnotatedType();
    }

    /**
     * Gets a field from this object.
     *
     * @param fieldName name of the field of interest
     * @return the given field
     */
    protected Field getField(String fieldName) {
        return getField(getClass(), fieldName);
    }

    /**
     * Gets a field from a given class.
     *
     * @param clazz class containing the field
     * @param fieldName name of the field of interest
     * @return the given field
     */
    protected Field getField(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);

        } catch (NoSuchFieldException | SecurityException e) {
            throw new RuntimeException(e);
        }
    }

}
