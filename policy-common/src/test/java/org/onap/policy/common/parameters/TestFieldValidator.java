/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023-2024 Nordix Foundation.
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.gson.annotations.SerializedName;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.parameters.annotations.Min;
import org.onap.policy.common.parameters.annotations.NotBlank;
import org.onap.policy.common.parameters.annotations.NotNull;

class TestFieldValidator extends ValidatorUtil {
    private static final String INT_LIST_FIELD = "intList";
    private static final String INT_MAP_FIELD = "intMap";
    private static final String UNANNOTATED_FIELD = "unannotated";
    private static final String INT_FIELD = "intValue";
    private static final int VALID_INT = 10;
    private static final int INVALID_INT = -10;

    @Getter
    private int unannotated;

    @Min(0)
    @Getter
    private int intValue;

    @Getter
    private List<@Min(1) Integer> intList;

    @Getter
    private Map<@NotBlank String, @Min(1) Integer> intMap;

    @SerializedName("annotated_key_map")
    @Getter
    private Map<@NotBlank String, Integer> annotatedKeyMap;

    @Getter
    private Map<String, @Min(1) Integer> annotatedValueMap;

    @Getter
    private List<Integer> unannotatedList;

    @Getter
    private Map<String, Integer> unannotatedMap;

    @NotNull
    @Getter
    private boolean boolValue;

    @NotNull
    @Getter
    private String notNullValue;

    @Min(0)
    @Getter
    private static int staticField;

    /**
     * Has no accessor.
     */
    @Min(0)
    private int noMethod;

    /**
     * Accessor is {@link #getStaticMethod()}, which is static.
     */
    @Min(0)
    private int staticMethod;

    /**
     * Accessor is {@link #getVoidMethod()}, which returns a void.
     */
    @Min(0)
    private int voidMethod;

    /**
     * Accessor is {@link #getParameterizedMethod(boolean)}, which requires a parameter.
     */
    @Min(0)
    private int parameterizedMethod;

    /**
     * Accessor is {@link #getExMethod()}, which throws an exception.
     */
    @Min(0)
    private int exMethod;


    @BeforeEach
    void setUp() {
        bean = new BeanValidator();
    }

    @Test
    void testGetAnnotation() {
        // field-level annotation
        assertThat(new FieldValidator(bean, TestFieldValidator.class, getField(INT_FIELD)).isEmpty()).isFalse();

        // class-level annotation
        assertThat(new FieldValidator(bean, ClassAnnot.class, getField(ClassAnnot.class, "text")).isEmpty()).isFalse();
    }

    @Test
    void testFieldValidator() throws NoSuchFieldException, SecurityException {
        /*
         * Note: nested classes contain fields like "$this", thus the check for "$" in the
         * variable name is already covered by the other tests.
         */

        /*
         * Class with no annotations.
         */
        @NotNull
        class NoAnnotations {
            @SuppressWarnings("unused")
            String strValue;
        }

        Field field = NoAnnotations.class.getDeclaredField("this$0");

        assertThat(new FieldValidator(bean, NoAnnotations.class, field).isEmpty()).isTrue();

        // unannotated
        assertThat(new FieldValidator(bean, TestFieldValidator.class, getField("unannotated")).isEmpty()).isTrue();

        // these are invalid for various reasons

        Field staticField2 = getField("staticField");
        assertThatThrownBy(() -> new FieldValidator(bean, TestFieldValidator.class, staticField2))
                        .isInstanceOf(IllegalArgumentException.class);

        Field noMethodField = getField("noMethod");
        assertThatThrownBy(() -> new FieldValidator(bean, TestFieldValidator.class, noMethodField))
                        .isInstanceOf(IllegalArgumentException.class);

        // annotated
        assertThat(new FieldValidator(bean, TestFieldValidator.class, getField(INT_FIELD)).isEmpty()).isFalse();
    }

    @Test
    void testFieldValidator_SetNullAllowed() {
        // default - null is allowed
        assertThat(new FieldValidator(bean, TestFieldValidator.class, getField(INT_FIELD)).isNullAllowed()).isTrue();

        // field-level NotNull
        assertThat(new FieldValidator(bean, TestFieldValidator.class, getField("notNullValue")).isNullAllowed())
                        .isFalse();

        // class-level NotNull
        assertThat(new FieldValidator(bean, ClassAnnot.class, getField(ClassAnnot.class, "noMethod")).isNullAllowed())
                        .isFalse();
    }

    @Test
    void testAddListValidator() {

        // unannotated
        assertThat(new FieldValidator(bean, TestFieldValidator.class, getField("unannotatedList")).isEmpty()).isTrue();

        // annotated
        assertThat(new FieldValidator(bean, TestFieldValidator.class, getField(INT_LIST_FIELD)).isEmpty()).isFalse();
    }

    @Test
    void testAddMapValidator() {

        // unannotated
        assertThat(new FieldValidator(bean, TestFieldValidator.class, getField("unannotatedMap")).isEmpty()).isTrue();

        // annotated
        assertThat(new FieldValidator(bean, TestFieldValidator.class, getField(INT_MAP_FIELD)).isEmpty()).isFalse();

        // only the key is annotated
        FieldValidator validator = new FieldValidator(bean, TestFieldValidator.class, getField("annotatedKeyMap"));
        assertThat(validator.isEmpty()).isFalse();

        BeanValidationResult result = new BeanValidationResult(MY_NAME, this);
        annotatedKeyMap = Map.of("abc", -10);
        validator.validateField(result, this);
        assertThat(result.getResult()).isNull();

        annotatedKeyMap = Map.of(" ", -10);
        validator.validateField(result, this);
        assertThat(result.getResult()).contains("annotated_key_map", "blank").doesNotContain("-10");

        // only the value is annotated
        validator = new FieldValidator(bean, TestFieldValidator.class, getField("annotatedValueMap"));
        assertThat(validator.isEmpty()).isFalse();

        result = new BeanValidationResult(MY_NAME, this);
        annotatedValueMap = Map.of(" ", 10);
        validator.validateField(result, this);
        assertThat(result.getResult()).isNull();

        annotatedValueMap = Map.of(" ", -10);
        validator.validateField(result, this);
        assertThat(result.getResult()).doesNotContain("blank").contains("annotatedValueMap", "\" \"", "-10");
    }

    @SuppressWarnings("deprecation")
    @Test
    void testValidateField_testGetValue() {
        // unannotated
        BeanValidationResult result = new BeanValidationResult(MY_NAME, this);
        new FieldValidator(bean, getClass(), getField(UNANNOTATED_FIELD)).validateField(result, this);
        assertThat(result.getResult()).isNull();

        // valid
        intValue = VALID_INT;
        result = new BeanValidationResult(MY_NAME, this);
        new FieldValidator(bean, getClass(), getField(INT_FIELD)).validateField(result, this);
        assertThat(result.getResult()).isNull();

        // invalid
        intValue = INVALID_INT;
        result = new BeanValidationResult(MY_NAME, this);
        new FieldValidator(bean, getClass(), getField(INT_FIELD)).validateField(result, this);
        assertThat(result.getResult()).contains(INT_FIELD);

        // throws an exception
        FieldValidator validator = new FieldValidator(bean, TestFieldValidator.class, getField("exMethod"));
        BeanValidationResult result2 = new BeanValidationResult(MY_NAME, this);
        assertThatThrownBy(() -> validator.validateField(result2, this)).isInstanceOf(IllegalArgumentException.class)
                        .getCause().isInstanceOf(InvocationTargetException.class).getCause()
                        .hasMessage("expected exception");
    }

    @Test
    void testValidateField_testGetValue_ListField() {
        // valid
        BeanValidationResult result = new BeanValidationResult(MY_NAME, this);
        intList = List.of(10, 20, 30, 40);
        new FieldValidator(bean, getClass(), getField(INT_LIST_FIELD)).validateField(result, this);
        assertThat(result.getResult()).isNull();

        // invalid
        result = new BeanValidationResult(MY_NAME, this);
        intList = List.of(9, -8, 7, -6);
        new FieldValidator(bean, getClass(), getField(INT_LIST_FIELD)).validateField(result, this);
        assertThat(result.getResult()).doesNotContain("0", "9").contains("1", "-8").doesNotContain("2", "7")
                        .contains("3", "-6");
    }

    @Test
    void testValidateField_testGetValue_MapField() {
        // valid
        BeanValidationResult result = new BeanValidationResult(MY_NAME, this);
        intMap = Map.of("ten", 10, "twenty", 20, "thirty", 30, "forty", 40);
        new FieldValidator(bean, getClass(), getField(INT_MAP_FIELD)).validateField(result, this);
        assertThat(result.getResult()).isNull();

        // invalid
        result = new BeanValidationResult(MY_NAME, this);
        intMap = Map.of("ten", 9, "twenty", -8, "thirty", 7, "forty", -6);
        new FieldValidator(bean, getClass(), getField(INT_MAP_FIELD)).validateField(result, this);
        assertThat(result.getResult()).doesNotContain("ten", "9").contains("twenty", "-8").doesNotContain("thirty", "7")
                        .contains("forty", "-6");
    }

    @Test
    void testClassOnly() {
        // class-level annotation has no bearing on a static field
        assertThat(new FieldValidator(bean, ClassAnnot.class, getField(ClassAnnot.class, "staticValue")).isEmpty())
                        .isTrue();

        // field-level annotation on a static field
        Field staticField2 = getField("staticField");
        assertThatThrownBy(() -> new FieldValidator(bean, TestFieldValidator.class, staticField2))
                        .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testGetAccessor() {
        // uses "getXxx"
        assertThat(new FieldValidator(bean, TestFieldValidator.class, getField(INT_FIELD)).isEmpty()).isFalse();

        // uses "isXxx"
        assertThat(new FieldValidator(bean, TestFieldValidator.class, getField("boolValue")).isEmpty()).isFalse();
    }

    @Test
    void testGetMethod() {
        assertThat(new FieldValidator(bean, TestFieldValidator.class, getField(INT_FIELD)).isEmpty()).isFalse();

        // these are invalid for various reasons

        Field noMethodField = getField("noMethod");
        assertThatThrownBy(() -> new FieldValidator(bean, TestFieldValidator.class, noMethodField))
                        .isInstanceOf(IllegalArgumentException.class);

        Field staticMethodField = getField("staticMethod");
        assertThatThrownBy(() -> new FieldValidator(bean, TestFieldValidator.class, staticMethodField))
                        .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testValidMethod() {
        assertThat(new FieldValidator(bean, TestFieldValidator.class, getField(INT_FIELD)).isEmpty()).isFalse();

        // these are invalid for various reasons

        Field staticMethodField = getField("staticMethod");
        assertThatThrownBy(() -> new FieldValidator(bean, TestFieldValidator.class, staticMethodField))
                        .isInstanceOf(IllegalArgumentException.class);

        Field voidMethodField = getField("voidMethod");
        assertThatThrownBy(() -> new FieldValidator(bean, TestFieldValidator.class, voidMethodField))
                        .isInstanceOf(IllegalArgumentException.class);

        Field paramMethodField = getField("parameterizedMethod");
        assertThatThrownBy(() -> new FieldValidator(bean, TestFieldValidator.class, paramMethodField))
                        .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testIsFieldAnnotated_testSetFieldAnnotated() {
        // annotated at the field level
        assertThat(new FieldValidator(bean, getClass(), getField(INT_FIELD)).isFieldAnnotated()).isTrue();

        // unannotated
        assertThat(new FieldValidator(bean, getClass(), getField(UNANNOTATED_FIELD)).isFieldAnnotated()).isFalse();
    }

    public static int getStaticMethod() {
        return -1000;
    }

    void getVoidMethod() {
        // do nothing
    }

    public int getParameterizedMethod(boolean flag) {
        return flag ? 0 : 1;
    }

    public int getExMethod() {
        throw new RuntimeException("expected exception");
    }

    @NotNull
    public static class ClassAnnot {
        @Getter
        private String text;

        // no "get" method
        @SuppressWarnings("unused")
        private String noMethod;

        @Getter
        private static int staticValue;
    }
}
