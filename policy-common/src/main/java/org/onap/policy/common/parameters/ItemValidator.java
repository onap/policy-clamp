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

/**
 * Validator of an "item", which is typically found in a collection, or the key or value
 * components of an entry in a Map.
 */
public class ItemValidator extends ValueValidator {
    private final AnnotatedType annotatedType;

    /**
     * Constructs the object.
     *
     * @param validator provider of validation methods
     * @param annotatedType a type having validation annotations to be
     *        applied to the item
     */
    public ItemValidator(BeanValidator validator, AnnotatedType annotatedType) {
        this(validator, annotatedType, true);
    }

    /**
     * Constructs the object.
     *
     * @param validator provider of validation methods
     * @param annotatedType a type having validation annotations to be
     *        applied to the item
     * @param addValidators {@code true} if to add validators
     */
    public ItemValidator(BeanValidator validator, AnnotatedType annotatedType, boolean addValidators) {
        this.annotatedType = annotatedType;

        if (addValidators) {
            validator.addValidators(this);
        }
    }

    /**
     * Gets an annotation from the field or the class.
     *
     * @param annotClass annotation class of interest
     * @return the annotation, or {@code null} if the {@link #annotatedType} does
     *         not contain the desired annotation
     */
    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotClass) {
        return annotatedType.getAnnotation(annotClass);
    }
}
