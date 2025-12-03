/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024-2025 Nordix Foundation
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

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.parameters.annotations.Min;
import org.onap.policy.common.parameters.annotations.NotBlank;
import org.onap.policy.common.parameters.annotations.NotNull;

class TestValueValidator extends ValidatorUtil {

    private ValueValidator validator;

    // these fields just provide place-holders for annotations

    @NotNull
    @NotBlank
    private final int annotField = 1;


    @BeforeEach
    void setUp() {
        validator = new MyValueValidator();
    }

    @Test
    void testIsEmpty() {
        assertThat(validator.isEmpty()).isTrue();

        validator.addAnnotation(NotNull.class, (result2, fieldName, value) -> true);
        assertThat(validator.isEmpty()).isFalse();
    }

    @Test
    void testValidateValue_NullValue() {
        BeanValidationResult result = new BeanValidationResult(MY_NAME, this);

        validator.validateValue(result, MY_FIELD, null);
        assertThat(result.getResult()).isNull();

        validator.addAnnotation(NotNull.class, BeanValidationResult::validateNotNull);
        validator.validateValue(result, MY_FIELD, null);
        assertThat(result.getResult()).contains(MY_FIELD, "null");
    }

    @Test
    void testValidateValue_NotNullValue() {
        BeanValidationResult result = new BeanValidationResult(MY_NAME, this);

        validator.validateValue(result, MY_FIELD, HELLO);
        assertThat(result.getResult()).isNull();

        validator.addAnnotation(NotNull.class, BeanValidationResult::validateNotNull);
        validator.validateValue(result, MY_FIELD, HELLO);
        assertThat(result.getResult()).isNull();
    }

    @Test
    void testAddAnnotationClassOfTChecker() {
        // the field does not have this annotation
        validator.addAnnotation(Min.class, (result2, fieldName, value) -> true);
        assertThat(validator.isEmpty()).isTrue();

        // "null" flag should stay true with this annotation
        assertThat(validator.isNullAllowed()).isTrue();
        validator.addAnnotation(NotBlank.class, (result2, fieldName, value) -> true);
        assertThat(validator.isNullAllowed()).isTrue();

        // "null" flag should become false with this annotation
        validator.addAnnotation(NotNull.class, (result2, fieldName, value) -> true);
        assertThat(validator.isNullAllowed()).isFalse();
    }

    @Test
    void testAddAnnotationClassOfTCheckerWithAnnotOfT() {
        // the field does not have this annotation
        validator.addAnnotation(Min.class, (result2, fieldName, annot, value) -> true);
        assertThat(validator.isEmpty()).isTrue();

        // indicates the annotation value
        AtomicBoolean wasNull = new AtomicBoolean(false);

        // the field DOES have this annotation
        validator.addAnnotation(NotNull.class, (result2, fieldName, annot, value) -> {
            wasNull.set(annot != null);
            return result2.validateNotNull(fieldName, value);
        });
        assertThat(validator.isEmpty()).isFalse();

        // ensure that the checker is invoked
        BeanValidationResult result = new BeanValidationResult(MY_NAME, this);
        validator.validateValue(result, MY_FIELD, HELLO);
        assertThat(result.getResult()).isNull();

        assertThat(wasNull.get()).isTrue();
    }

    @Test
    void testGetAnnotation() {
        assertThat(new ValueValidator().getAnnotation(NotNull.class)).isNull();
    }

    /**
     * Checks for annotations on the "annotField" field.
     */
    private static class MyValueValidator extends ValueValidator {
        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotClass) {
            try {
                return TestValueValidator.class.getDeclaredField("annotField").getAnnotation(annotClass);
            } catch (NoSuchFieldException | SecurityException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
