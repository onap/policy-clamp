/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.common.gson.internal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.gson.JsonParseException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.gson.annotation.GsonJsonAnyGetter;
import org.onap.policy.common.gson.annotation.GsonJsonAnySetter;
import org.onap.policy.common.gson.annotation.GsonJsonIgnore;
import org.onap.policy.common.gson.annotation.GsonJsonProperty;

class ClassWalkerTest {

    private static final String SET_OVERRIDE = ".setOverride";
    private static final String INVALID_FIELD_NAME = "invalidFieldName";

    private MyWalker walker;

    /**
     * Set up.
     */
    @BeforeEach
    void setUp() {
        walker = new MyWalker();
    }

    @Test
    void testExamineClassOfQ_testExamineField_testExamineInField_testExamineOutField() {
        walker.walkClassHierarchy(DerivedFromBottom.class);

        assertEquals("[InterfaceOne, InterfaceTwo, InterfaceOne, InterfaceThree, Bottom, DerivedFromBottom]",
            walker.classes.toString());

        List<String> inFields = walker.getInProps(Field.class).stream().map(Field::getName).sorted().toList();
        assertEquals("[exposedField, overriddenValue, transField]", inFields.toString());

        List<String> outFields = walker.getInProps(Field.class).stream().map(Field::getName).sorted().toList();
        assertEquals("[exposedField, overriddenValue, transField]", outFields.toString());

        // should work with interfaces without throwing an NPE
        walker.walkClassHierarchy(InterfaceOne.class);
    }

    @Test
    void testHasAnyGetter() {
        walker.walkClassHierarchy(Object.class);
        assertNull(walker.getAnyGetter());
        assertNull(walker.getAnySetter());

        walker.walkClassHierarchy(AnyGetterIgnored.class);
        assertNull(walker.getAnyGetter());
        assertNull(walker.getAnySetter());

        walker.walkClassHierarchy(AnyGetterOnly.class);
        assertNotNull(walker.getAnyGetter());
        assertNull(walker.getAnySetter());
    }

    @Test
    void testHasAnySetter() {
        walker.walkClassHierarchy(Object.class);
        assertNull(walker.getAnySetter());
        assertNull(walker.getAnyGetter());

        walker.walkClassHierarchy(AnySetterIgnored.class);
        assertNull(walker.getAnySetter());
        assertNull(walker.getAnyGetter());

        walker.walkClassHierarchy(AnySetterOnly.class);
        assertNotNull(walker.getAnySetter());
        assertNull(walker.getAnyGetter());
    }

    @Test
    void testExamineMethod() {
        walker.walkClassHierarchy(DerivedFromData.class);

        assertEquals("[Data, DerivedFromData]", walker.classes.toString());

        // ensure all methods were examined
        Collections.sort(walker.methods);
        List<String> lst = Arrays.asList("getId", "getValue", "getOnlyOut", "getStatic", "getText", "getTheMap",
                        "getUnserialized", "getValue", "getWithParams", "setExtraParams", "setId", "setMap",
                        "setMapValue", "setMissingParams", "setNonPublic", "setOnlyIn", "setText", "setUnserialized",
                        "setValue", "setValue", "wrongGetPrefix", "wrongSetPrefix");
        Collections.sort(lst);
        assertEquals(lst.toString(), walker.methods.toString());

        assertNotNull(walker.getAnyGetter());
        assertEquals("getTheMap", walker.getAnyGetter().getName());

        List<String> getters = walker.getOutProps(Method.class).stream().map(Method::getName).sorted().toList();
        assertEquals("[getId, getOnlyOut, getValue]", getters.toString());

        assertNotNull(walker.getAnySetter());
        assertEquals("setMapValue", walker.getAnySetter().getName());

        List<String> setters = walker.getInProps(Method.class).stream().map(Method::getName).sorted().toList();
        assertEquals("[setId, setOnlyIn, setValue]", setters.toString());

        // getter with invalid parameter count
        assertThatThrownBy(() -> walker.walkClassHierarchy(AnyGetterMismatchParams.class))
                        .isInstanceOf(JsonParseException.class).hasMessage(ClassWalker.ANY_GETTER_MISMATCH_ERR
                                        + AnyGetterMismatchParams.class.getName() + ".getTheMap");

        // setter with too few parameters
        assertThatThrownBy(() -> walker.walkClassHierarchy(AnySetterTooFewParams.class))
                        .isInstanceOf(JsonParseException.class).hasMessage(ClassWalker.ANY_SETTER_MISMATCH_ERR
                                        + AnySetterTooFewParams.class.getName() + SET_OVERRIDE);

        // setter with too many parameters
        assertThatThrownBy(() -> walker.walkClassHierarchy(AnySetterTooManyParams.class))
                        .isInstanceOf(JsonParseException.class).hasMessage(ClassWalker.ANY_SETTER_MISMATCH_ERR
                                        + AnySetterTooManyParams.class.getName() + SET_OVERRIDE);

        // setter with invalid parameter type
        assertThatThrownBy(() -> walker.walkClassHierarchy(AnySetterInvalidParam.class))
                        .isInstanceOf(JsonParseException.class).hasMessage(ClassWalker.ANY_SETTER_TYPE_ERR
                                        + AnySetterInvalidParam.class.getName() + SET_OVERRIDE);
    }

    @Test
    void testExamineMethod_AnyGetter() {
        walker.walkClassHierarchy(AnyGetterOverride.class);

        assertNotNull(walker.getAnyGetter());
        assertEquals("getOverride", walker.getAnyGetter().getName());
    }

    @Test
    void testExamineMethod_AnySetter() {
        walker.walkClassHierarchy(AnySetterOverride.class);

        assertNotNull(walker.getAnySetter());
        assertEquals("setOverride", walker.getAnySetter().getName());
    }

    @Test
    void testGetInNotIgnored_testGetOutNotIgnored() {
        walker.walkClassHierarchy(DerivedFromData.class);

        assertEquals("[id, onlyIn, text, value]", new TreeSet<>(walker.getInNotIgnored()).toString());
        assertEquals("[id, onlyOut, text, value]", new TreeSet<>(walker.getOutNotIgnored()).toString());
    }

    /**
     * Walker subclass that records items that are examined.
     */
    private static class MyWalker extends ClassWalker {
        private final List<String> classes = new ArrayList<>();
        private final List<String> methods = new ArrayList<>();

        @Override
        protected void examine(Class<?> clazz) {
            classes.add(clazz.getSimpleName());

            super.examine(clazz);
        }

        @Override
        protected void examine(Method method) {
            if (Adapter.isManaged(method)) {
                methods.add(method.getName());
            }

            super.examine(method);
        }

        @Override
        protected String detmPropName(Field field) {
            if (INVALID_FIELD_NAME.equals(field.getName())) {
                return null;
            }

            return super.detmPropName(field);
        }
    }

    protected interface InterfaceOne {
        int id = 1000; // NOSONAR I think this is meant to be accessible as fields, not constants
    }

    protected interface InterfaceTwo {
        String text = "intfc2-text"; // NOSONAR I think this is meant to be accessible as fields, not constants
    }

    private interface InterfaceThree {

    }

    protected static class Bottom implements InterfaceOne, InterfaceThree {
        private int id;
        public String value;

        // this is not actually invalid, but will be treated as if it were
        public String invalidFieldName;

        @GsonJsonProperty("exposed")
        private String exposedField;

        @GsonJsonIgnore
        public int ignored;

        public transient int ignoredTransField;

        @GsonJsonProperty("trans")
        public transient int transField;

        @GsonJsonIgnore
        public int getId() {
            return id;
        }

        @GsonJsonIgnore
        public void setId(int id) {
            this.id = id;
        }
    }

    protected static class DerivedFromBottom extends Bottom implements InterfaceOne, InterfaceTwo {
        private String text;
        protected String anotherValue;

        @GsonJsonProperty("value")
        public String overriddenValue;

        @GsonJsonIgnore
        public String getText() {
            return text;
        }

        @GsonJsonIgnore
        public void setText(String text) {
            this.text = text;
        }
    }

    @Setter
    protected static class Data {
        @Getter
        private int id;
        // this will be ignored, because there's already a field by this name
        private String text;

        // not public, but property provided
        @GsonJsonProperty("text")
        protected String getText() {
            return text;
        }

        // should only show up in the output list
        public int getOnlyOut() {
            return 1100;
        }

        // will be overridden by subclass
        @GsonJsonProperty("super-value-getter")
        public String getValue() {
            return null;
        }

        // will be overridden by subclass
        @GsonJsonProperty("super-value-setter")
        public void setValue(String value) {
            // do nothing
        }
    }

    protected static class DerivedFromData extends Data {
        // not serialized
        private String unserialized;

        // overrides private field and public method from Data
        public String text;

        private Map<String, String> map;

        private String value;

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public void setValue(String value) {
            this.value = value;
        }

        @GsonJsonAnyGetter
        public Map<String, String> getTheMap() {
            return map;
        }

        @GsonJsonIgnore
        public void setMap(Map<String, String> map) {
            this.map = map;
        }

        @GsonJsonAnySetter
        public void setMapValue(String key, String value) {
            if (map == null) {
                map = new TreeMap<>();
            }

            map.put(key, value);
        }

        @GsonJsonIgnore
        public String getUnserialized() {
            return unserialized;
        }

        @GsonJsonIgnore
        public void setUnserialized(String unserialized) {
            this.unserialized = unserialized;
        }

        // should only show up in the input list
        public void setOnlyIn(int value) {
            // do nothing
        }

        // has a param - shouldn't be serialized
        public int getWithParams(String text) {
            return 1000;
        }

        // too few params - shouldn't be serialized
        public void setMissingParams() {
            // do nothing
        }

        // too many params - shouldn't be serialized
        public void setExtraParams(String text, String moreText) {
            // do nothing
        }

        // not public - shouldn't be serialized
        protected void setNonPublic(String text) {
            // do nothing
        }

        // doesn't start with "get"
        public String wrongGetPrefix() {
            return null;
        }

        // doesn't start with "set"
        public void wrongSetPrefix(String text) {
            // do nothing
        }

        // static
        public static String getStatic() {
            return null;
        }
    }

    /**
     * The "get" method has an incorrect argument count.
     */
    private static class AnyGetterMismatchParams {
        @GsonJsonAnyGetter
        public Map<String, String> getTheMap(String arg) {
            return new TreeMap<>();
        }
    }

    /**
     * Has {@link GsonJsonAnyGetter} method.
     */
    private static class AnyGetterOnly {
        @GsonJsonAnyGetter
        private Map<String, Integer> getOverride() {
            return null;
        }
    }

    /**
     * Has {@link GsonJsonAnyGetter} method, but it's ignored.
     */
    private static class AnyGetterIgnored {
        @GsonJsonAnyGetter
        @GsonJsonIgnore
        private Map<String, Integer> getOverride() {
            return null;
        }
    }

    /**
     * Has {@link GsonJsonAnySetter} method.
     */
    private static class AnySetterOnly {
        @GsonJsonAnySetter
        private void setOverride(String key, int value) {
            // do nothing
        }
    }

    /**
     * Has {@link GsonJsonAnySetter} method, but it's ignored.
     */
    private static class AnySetterIgnored {
        @GsonJsonAnySetter
        @GsonJsonIgnore
        private void setOverride(String key, int value) {
            // do nothing
        }
    }

    /**
     * Has {@link GsonJsonAnyGetter} method that overrides the super class' method.
     */
    private static class AnyGetterOverride extends DerivedFromData {
        private Map<String, Integer> overMap;

        @GsonJsonAnyGetter
        private Map<String, Integer> getOverride() {
            return overMap;
        }
    }

    /**
     * Has {@link GsonJsonAnySetter} method that overrides the super class' method.
     */
    private static class AnySetterOverride extends DerivedFromData {
        private Map<String, Integer> overMap;

        @GsonJsonAnySetter
        private void setOverride(String key, int value) {
            if (overMap == null) {
                overMap = new TreeMap<>();
            }

            overMap.put(key, value);
        }
    }

    /**
     * Has {@link GsonJsonAnySetter} method with too few parameters.
     */
    private static class AnySetterTooFewParams extends DerivedFromData {
        @GsonJsonAnySetter
        public void setOverride(String key) {
            // do nothing
        }
    }

    /**
     * Has {@link GsonJsonAnySetter} method with too few parameters.
     */
    private static class AnySetterTooManyParams extends DerivedFromData {
        @GsonJsonAnySetter
        public void setOverride(String key, int value, String anotherValue) {
            // do nothing
        }
    }

    /**
     * Has {@link GsonJsonAnySetter} method whose first argument type is incorrect.
     */
    private static class AnySetterInvalidParam extends DerivedFromData {
        @GsonJsonAnySetter
        public void setOverride(Integer key, String value) {
            // do nothing
        }
    }
}
