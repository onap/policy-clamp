/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024 Nordix Foundation
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.parameters.annotations.ClassName;
import org.onap.policy.common.parameters.annotations.Max;
import org.onap.policy.common.parameters.annotations.Min;
import org.onap.policy.common.parameters.annotations.NotBlank;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.common.parameters.annotations.Pattern;
import org.onap.policy.common.parameters.annotations.Size;
import org.onap.policy.common.parameters.annotations.Valid;

class TestBeanValidator {
    private static final String TOP = "top";
    private static final String STR_FIELD = "strValue";
    private static final String INT_FIELD = "intValue";
    private static final String NUM_FIELD = "numValue";
    private static final String ITEMS_FIELD = "items";
    private static final String STRING_VALUE = "string value";
    private static final int INT_VALUE = 20;

    private BeanValidator validator;

    @BeforeEach
    void setUp() {
        validator = new BeanValidator();
    }

    @Test
    void testValidateTop_testValidateFields() {
        // validate null
        assertTrue(validator.validateTop(TOP, null).isValid());

        // validate something that has no annotations
        assertTrue(validator.validateTop(TOP, validator).isValid());

        @NotNull
        @Getter
        class Data {
            String strValue;
        }

        // one failure case
        Data data = new Data();
        BeanValidationResult result = validator.validateTop(TOP, data);
        assertInvalid("testValidateFields", result, STR_FIELD, "null");
        assertTrue(result.getResult().contains(TOP));

        // one success case
        data.strValue = STRING_VALUE;
        assertTrue(validator.validateTop(TOP, data).isValid());

        @Getter
        class Derived extends Data {
            @Min(10)
            int intValue;
        }

        Derived derived = new Derived();
        derived.strValue = STRING_VALUE;
        derived.intValue = INT_VALUE;

        // success case
        assertTrue(validator.validateTop(TOP, derived).isValid());

        // failure cases
        derived.strValue = null;
        assertInvalid("testValidateFields", validator.validateTop(TOP, derived), STR_FIELD, "null");
        derived.strValue = STRING_VALUE;

        derived.intValue = 1;
        assertInvalid("testValidateFields", validator.validateTop(TOP, derived), INT_FIELD, "minimum");
        derived.intValue = INT_VALUE;

        // both invalid
        derived.strValue = null;
        derived.intValue = 1;
        result = validator.validateTop(TOP, derived);
        assertInvalid("testValidateFields", result, STR_FIELD, "null");
        assertInvalid("testValidateFields", result, INT_FIELD, "minimum");
        derived.strValue = STRING_VALUE;
        derived.intValue = INT_VALUE;
    }

    @Test
    void testVerNotNull() {
        @Getter
        class NotNullCheck {
            @Min(1)
            @NotNull
            Integer intValue;
        }

        NotNullCheck notNullCheck = new NotNullCheck();
        assertInvalid("testVerNotNull", validator.validateTop(TOP, notNullCheck), INT_FIELD, "null");

        notNullCheck.intValue = INT_VALUE;
        assertTrue(validator.validateTop(TOP, notNullCheck).isValid());

        notNullCheck.intValue = 0;
        assertInvalid("testVerNotNull", validator.validateTop(TOP, notNullCheck), INT_FIELD, "minimum");
    }

    @Test
    void testVerNotBlank() {
        @Getter
        class NotBlankCheck {
            @NotBlank
            String strValue;
        }

        NotBlankCheck notBlankCheck = new NotBlankCheck();

        // null
        assertTrue(validator.validateTop(TOP, notBlankCheck).isValid());

        // empty
        notBlankCheck.strValue = "";
        assertInvalid("testVerNotNull", validator.validateTop(TOP, notBlankCheck), STR_FIELD, "blank");

        // spaces
        notBlankCheck.strValue = "  ";
        assertInvalid("testVerNotNull", validator.validateTop(TOP, notBlankCheck), STR_FIELD, "blank");

        // not blank
        notBlankCheck.strValue = STRING_VALUE;
        assertTrue(validator.validateTop(TOP, notBlankCheck).isValid());

        /*
         * Class with "blank" annotation on an integer.
         */
        @Getter
        class NotBlankInt {
            @NotBlank
            int intValue;
        }

        NotBlankInt notBlankInt = new NotBlankInt();
        notBlankInt.intValue = 0;
        assertTrue(validator.validateTop(TOP, notBlankInt).isValid());
    }

    /**
     * Tests verSize with a collection.
     */
    @Test
    void testVerSizeCollection() {
        @Getter
        class CollectionSizeCheck {
            @Size(min = 3)
            Collection<Integer> items;
        }

        CollectionSizeCheck collCheck = new CollectionSizeCheck();

        // valid length - exact
        collCheck.items = List.of(1, 2, 3);
        assertThat(validator.validateTop(TOP, collCheck).isValid()).isTrue();

        // valid length - extra
        collCheck.items = List.of(1, 2, 3, 4);
        assertThat(validator.validateTop(TOP, collCheck).isValid()).isTrue();

        // too few
        collCheck.items = List.of(1, 2);
        assertInvalid("testVerSize", validator.validateTop(TOP, collCheck), ITEMS_FIELD, "minimum", "3");

        // null
        collCheck.items = null;
        assertThat(validator.validateTop(TOP, collCheck).isValid()).isTrue();
    }

    /**
     * Tests verSize with a map.
     */
    @Test
    void testVerSizeMap() {
        @Getter
        class MapSizeCheck {
            @Size(min = 3)
            Map<Integer, Integer> items;
        }

        MapSizeCheck mapCheck = new MapSizeCheck();

        // valid length - exact
        mapCheck.items = Map.of(1, 10, 2, 20, 3, 30);
        assertThat(validator.validateTop(TOP, mapCheck).isValid()).isTrue();

        // valid length - extra
        mapCheck.items = Map.of(1, 10, 2, 20, 3, 30, 4, 40);
        assertThat(validator.validateTop(TOP, mapCheck).isValid()).isTrue();

        // too few
        mapCheck.items = Map.of(1, 10, 2, 20);
        assertInvalid("testVerSize", validator.validateTop(TOP, mapCheck), ITEMS_FIELD, "minimum", "3");

        // null
        mapCheck.items = null;
        assertThat(validator.validateTop(TOP, mapCheck).isValid()).isTrue();
    }

    /**
     * Tests verSize with an object for which it doesn't apply.
     */
    @Test
    void testVerSizeOther() {
        @Getter
        class OtherSizeCheck {
            @Size(min = 3)
            Integer items;
        }

        OtherSizeCheck otherCheck = new OtherSizeCheck();

        otherCheck.items = 10;
        assertThat(validator.validateTop(TOP, otherCheck).isValid()).isTrue();
    }

    @Test
    void testVerRegex() {
        @Getter
        class RegexCheck {
            @Pattern(regexp = "[a-f]*")
            String strValue;
        }

        RegexCheck regexCheck = new RegexCheck();

        // does not match
        regexCheck.strValue = "xyz";
        assertInvalid("testVerRegex", validator.validateTop(TOP, regexCheck), STR_FIELD,
                        "does not match regular expression [a-f]");

        // matches
        regexCheck.strValue = "abcabc";
        assertTrue(validator.validateTop(TOP, regexCheck).isValid());

        // invalid regex
        @Getter
        class InvalidRegexCheck {
            @Pattern(regexp = "[a-f")
            String strValue;
        }

        InvalidRegexCheck invalidRegexCheck = new InvalidRegexCheck();

        // does not match
        invalidRegexCheck.strValue = "abc";
        assertInvalid("testVerRegex", validator.validateTop(TOP, invalidRegexCheck), STR_FIELD,
                        "does not match regular expression [a-f");

        // matches
        regexCheck.strValue = "abcabc";
        assertTrue(validator.validateTop(TOP, regexCheck).isValid());

        /*
         * Class with "regex" annotation on an integer.
         */
        @Getter
        class RegexInt {
            @Pattern(regexp = "[a-f]*")
            int intValue;
        }

        RegexInt regexInt = new RegexInt();
        regexInt.intValue = 0;
        assertInvalid("testVerRegex", validator.validateTop(TOP, regexInt), INT_FIELD,
                        "does not match regular expression [a-f]");
    }

    @Test
    void testVerMax() {
        /*
         * Field is not a number.
         */
        @Getter
        class NonNumeric {
            @Max(100)
            String strValue;
        }

        NonNumeric nonNumeric = new NonNumeric();
        nonNumeric.strValue = STRING_VALUE;
        assertTrue(validator.validateTop(TOP, nonNumeric).isValid());

        /*
         * Integer field.
         */
        @Getter
        class IntField {
            @Max(100)
            Integer intValue;
        }

        // ok value
        IntField intField = new IntField();
        assertNumeric(intField, value -> {
            intField.intValue = value;
        }, INT_FIELD, "maximum", 100, 101);

        /*
         * Long field.
         */
        @Getter
        class LongField {
            @Max(100)
            Long numValue;
        }

        // ok value
        LongField longField = new LongField();
        assertNumeric(longField, value -> {
            longField.numValue = (long) value;
        }, NUM_FIELD, "maximum", 100, 101);

        /*
         * Float field.
         */
        @Getter
        class FloatField {
            @Max(100)
            Float numValue;
        }

        // ok value
        FloatField floatField = new FloatField();
        assertNumeric(floatField, value -> {
            floatField.numValue = (float) value;
        }, NUM_FIELD, "maximum", 100, 101);

        /*
         * Double field.
         */
        @Getter
        class DoubleField {
            @Max(100)
            Double numValue;
        }

        // ok value
        DoubleField doubleField = new DoubleField();
        assertNumeric(doubleField, value -> {
            doubleField.numValue = (double) value;
        }, NUM_FIELD, "maximum", 100, 101);

        /*
         * Atomic Integer field (which is a subclass of Number).
         */
        @Getter
        class AtomIntValue {
            @Max(100)
            AtomicInteger numValue;
        }

        // ok value
        AtomIntValue atomIntField = new AtomIntValue();
        atomIntField.numValue = new AtomicInteger(INT_VALUE);
        assertTrue(validator.validateTop(TOP, atomIntField).isValid());

        // invalid value - should be OK, because it isn't an Integer
        atomIntField.numValue.set(101);
        assertTrue(validator.validateTop(TOP, atomIntField).isValid());
    }

    @Test
    void testVerMin() {
        /*
         * Field is not a number.
         */
        @Getter
        class NonNumeric {
            @Min(10)
            String strValue;
        }

        NonNumeric nonNumeric = new NonNumeric();
        nonNumeric.strValue = STRING_VALUE;
        assertTrue(validator.validateTop(TOP, nonNumeric).isValid());

        /*
         * Integer field.
         */
        @Getter
        class IntField {
            @Min(10)
            Integer intValue;
        }

        // ok value
        IntField intField = new IntField();
        assertNumeric(intField, value -> {
            intField.intValue = value;
        }, INT_FIELD, "minimum", 10, 1);

        /*
         * Long field.
         */
        @Getter
        class LongField {
            @Min(10)
            Long numValue;
        }

        // ok value
        LongField longField = new LongField();
        assertNumeric(longField, value -> {
            longField.numValue = (long) value;
        }, NUM_FIELD, "minimum", 10, 1);

        /*
         * Float field.
         */
        @Getter
        class FloatField {
            @Min(10)
            Float numValue;
        }

        // ok value
        FloatField floatField = new FloatField();
        assertNumeric(floatField, value -> {
            floatField.numValue = (float) value;
        }, NUM_FIELD, "minimum", 10, 1);

        /*
         * Double field.
         */
        @Getter
        class DoubleField {
            @Min(10)
            Double numValue;
        }

        // ok value
        DoubleField doubleField = new DoubleField();
        assertNumeric(doubleField, value -> {
            doubleField.numValue = (double) value;
        }, NUM_FIELD, "minimum", 10, 1);

        /*
         * Atomic Integer field (which is a subclass of Number).
         */
        @Getter
        class AtomIntValue {
            @Min(10)
            AtomicInteger numValue;
        }

        // ok value
        AtomIntValue atomIntField = new AtomIntValue();
        atomIntField.numValue = new AtomicInteger(INT_VALUE);
        assertTrue(validator.validateTop(TOP, atomIntField).isValid());

        // invalid value - should be OK, because it isn't an Integer
        atomIntField.numValue.set(101);
        assertTrue(validator.validateTop(TOP, atomIntField).isValid());
    }

    @Test
    void testVerClassName() {
        @Getter
        class ClassNameCheck {
            @ClassName
            String strValue;
        }

        ClassNameCheck classCheck = new ClassNameCheck();

        // null should be OK
        classCheck.strValue = null;
        assertTrue(validator.validateTop(TOP, classCheck).isValid());

        // valid class name
        classCheck.strValue = getClass().getName();
        assertTrue(validator.validateTop(TOP, classCheck).isValid());

        // invalid class name
        classCheck.strValue = "<unknown class>";
        assertInvalid("testVerClassName", validator.validateTop(TOP, classCheck),
                        STR_FIELD, "class is not in the classpath");
    }

    @Test
    void testVerCascade() {
        @Getter
        class Item {
            @NotNull
            Integer intValue;
        }

        @Getter
        class Container {
            @Valid
            Item checked;

            Item unchecked;

            @Valid
            List<Item> items;

            @Valid
            Map<String, Item> itemMap;
        }

        Container cont = new Container();
        cont.unchecked = new Item();
        cont.items = List.of(new Item());
        cont.itemMap = Map.of(STRING_VALUE, new Item());

        cont.checked = null;
        assertTrue(validator.validateTop(TOP, cont).isValid());

        cont.checked = new Item();

        assertInvalid("testVerCascade", validator.validateTop(TOP, cont), INT_FIELD, "null");

        cont.checked.intValue = INT_VALUE;
        assertTrue(validator.validateTop(TOP, cont).isValid());
    }

    @Test
    void testVerCollection() {
        @Getter
        class Container {
            List<@Min(5) Integer> items;

            // not a collection - should not be checked
            @Valid
            String strValue;

            String noAnnotations;
        }

        Container cont = new Container();
        cont.strValue = STRING_VALUE;
        cont.noAnnotations = STRING_VALUE;

        // null collection - always valid
        assertTrue(validator.validateTop(TOP, cont).isValid());

        // empty collection - always valid
        cont.items = List.of();
        assertTrue(validator.validateTop(TOP, cont).isValid());

        cont.items = List.of(-10, -20);
        assertThat(validator.validateTop(TOP, cont).getResult()).contains("\"0\"", "-10", "\"1\"", "-20", "minimum");

        cont.items = List.of(10, -30);
        assertThat(validator.validateTop(TOP, cont).getResult()).contains("\"1\"", "-30", "minimum")
                        .doesNotContain("\"0\"");

        cont.items = List.of(10, 20);
        assertTrue(validator.validateTop(TOP, cont).isValid());
    }

    @Test
    void testVerMap() {
        @Getter
        class Container {
            Map<String, @Min(5) Integer> items;

            // not a map
            @NotBlank
            String strValue;

            String noAnnotations;
        }

        Container cont = new Container();
        cont.strValue = STRING_VALUE;
        cont.noAnnotations = STRING_VALUE;

        // null map - always valid
        assertTrue(validator.validateTop(TOP, cont).isValid());

        // empty map - always valid
        cont.items = Map.of();
        assertTrue(validator.validateTop(TOP, cont).isValid());

        cont.items = Map.of("abc", -10, "def", -20);
        assertThat(validator.validateTop(TOP, cont).getResult()).contains("abc", "-10", "def", "-20", "minimum");

        cont.items = Map.of("abc", 10, "def", -30);
        assertThat(validator.validateTop(TOP, cont).getResult()).contains("def", "-30", "minimum")
                        .doesNotContain("abc");

        cont.items = Map.of("abc", 10, "def", 20);
        assertTrue(validator.validateTop(TOP, cont).isValid());
    }

    @Test
    void testGetEntryName() {
        assertThat(validator.getEntryName(makeEntry(null))).isEmpty();
        assertThat(validator.getEntryName(makeEntry(""))).isEmpty();
        assertThat(validator.getEntryName(makeEntry(STRING_VALUE))).isEqualTo(STRING_VALUE);
    }

    /**
     * Makes a Map entry with the given key and value.
     *
     * @param key desired key
     * @return a new Map entry
     */
    private Map.Entry<String, Integer> makeEntry(String key) {
        HashMap<String, Integer> map = new HashMap<>();
        map.put(key, 0);
        return map.entrySet().iterator().next();
    }

    private <T> void assertNumeric(T object, Consumer<Integer> setter, String fieldName,
                                   String expectedText, int edge, int outside) {
        setter.accept(TestBeanValidator.INT_VALUE);
        assertTrue(validator.validateTop(TOP, object).isValid());

        // on the edge
        setter.accept(edge);
        assertTrue(validator.validateTop(TOP, object).isValid());

        // invalid
        setter.accept(outside);
        assertInvalid("testVerNotNull", validator.validateTop(TOP, object), fieldName, expectedText);
    }


    private void assertInvalid(String testName, BeanValidationResult result, String... text) {
        assertThat(result.getResult()).describedAs(testName).contains(text);
    }
}
