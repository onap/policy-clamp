/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestBeanValidationResult {
    private static final String TEXT1 = "abc";
    private static final String TEXT2 = "def";
    private static final String MY_LIST = "my-list";
    private static final String MY_MAP = "my-map";
    private static final String OBJECT = "an object";
    private static final String INITIAL_INDENT = "xx ";
    private static final String NEXT_INDENT = "yy ";
    private static final String MID_INDENT = "xx yy ";
    private static final String NAME = "my-name";
    private static final String MY_LIST_INVALID = "  'my-list' INVALID, item has status INVALID\n    ";
    private static final String MY_MAP_INVALID = "  'my-map' INVALID, item has status INVALID\n    ";
    private static final String BEAN_INVALID_MSG = requote("'my-name' INVALID, item has status INVALID\n");

    private String cleanMsg;
    private String invalidMsg;

    private BeanValidationResult bean;
    private ObjectValidationResult clean;
    private ObjectValidationResult invalid;

    /**
     * Sets up.
     */
    @BeforeEach
    void setUp() {
        clean = new ObjectValidationResult(TEXT1, 10);
        cleanMsg = clean.getResult("", "", true);

        invalid = new ObjectValidationResult(TEXT2, 20);
        invalid.setResult(ValidationStatus.INVALID, "invalid");
        invalidMsg = invalid.getResult();

        bean = new BeanValidationResult(NAME, OBJECT);
    }

    @Test
    void testBeanValidationResult() {
        assertTrue(bean.isValid());
        assertNull(bean.getResult());
    }

    @Test
    void testAddResult_testGetResult() {
        // null should be ok
        assertTrue(bean.addResult(null));

        assertTrue(bean.addResult(clean));
        assertTrue(bean.isValid());
        assertNull(bean.getResult());

        assertFalse(bean.addResult(invalid));
        assertFalse(bean.isValid());
        assertEquals(BEAN_INVALID_MSG + "  " + invalidMsg, bean.getResult());

        assertEquals(INITIAL_INDENT + BEAN_INVALID_MSG + MID_INDENT + cleanMsg + MID_INDENT + invalidMsg,
                        bean.getResult(INITIAL_INDENT, NEXT_INDENT, true));

        bean = new BeanValidationResult(NAME, OBJECT);
        assertFalse(bean.addResult(MY_LIST, "hello", ValidationStatus.INVALID, TEXT1));
        assertThat(bean.getResult()).contains("\"" + MY_LIST + "\" value \"hello\" INVALID, " + TEXT1);
    }

    @Test
    void testValidateNotNull() {
        assertTrue(bean.validateNotNull("sub-name", "sub-object"));
        assertTrue(bean.isValid());
        assertNull(bean.getResult());

        assertFalse(bean.validateNotNull("sub-name", null));
        assertFalse(bean.isValid());
        assertEquals(requote(BEAN_INVALID_MSG + "  item 'sub-name' value 'null' INVALID, is null\n"), bean.getResult());
    }

    @Test
    void testValidateNotNullList() {
        List<ValidationResult> list = List.of(clean);
        assertTrue(bean.validateNotNullList(MY_LIST, list, item -> item));
        assertTrue(bean.isValid());
        assertNull(bean.getResult());

        list = Arrays.asList(invalid, invalid);
        assertFalse(bean.validateNotNullList(MY_LIST, list, item -> item));
        assertFalse(bean.isValid());
        assertEquals(requote(BEAN_INVALID_MSG + MY_LIST_INVALID + invalidMsg
                        + "    " + invalidMsg), bean.getResult());
    }

    @Test
    void testValidateNotNullList_NullList() {
        List<ValidationResult> list = null;
        assertFalse(bean.validateNotNullList(MY_LIST, list, item -> item));
        assertFalse(bean.isValid());
        assertEquals(requote(BEAN_INVALID_MSG + "  item 'my-list' value 'null' INVALID, is null\n"), bean.getResult());

    }

    @Test
    void testValidateList() {
        List<ValidationResult> list = null;
        bean = new BeanValidationResult(NAME, OBJECT);
        assertTrue(bean.validateList(MY_LIST, list, item -> item));
        assertTrue(bean.isValid());
        assertNull(bean.getResult());

        list = List.of(clean);
        bean = new BeanValidationResult(NAME, OBJECT);
        assertTrue(bean.validateList(MY_LIST, list, item -> item));
        assertTrue(bean.isValid());
        assertNull(bean.getResult());

        // null item in the list
        list = Arrays.asList(clean, null);
        bean = new BeanValidationResult(NAME, OBJECT);
        assertFalse(bean.validateList(MY_LIST, list, item -> item));
        assertFalse(bean.isValid());
        assertEquals(requote(BEAN_INVALID_MSG + MY_LIST_INVALID
                        + "item 'item' value 'null' INVALID, null\n"), bean.getResult());

        list = Arrays.asList(invalid, invalid);
        bean = new BeanValidationResult(NAME, OBJECT);
        assertFalse(bean.validateList(MY_LIST, list, item -> item));
        assertFalse(bean.isValid());
        assertEquals(requote(BEAN_INVALID_MSG + MY_LIST_INVALID + invalidMsg
                        + "    " + invalidMsg), bean.getResult());

    }

    @Test
    void testValidateMap() {
        Map<String, ValidationResult> map = null;
        bean = new BeanValidationResult(NAME, OBJECT);
        assertTrue(bean.validateMap(MY_MAP, map, validMapEntry()));
        assertTrue(bean.isValid());
        assertNull(bean.getResult());

        map = Map.of(TEXT1, clean, TEXT2, clean);
        bean = new BeanValidationResult(NAME, OBJECT);
        assertTrue(bean.validateMap(MY_MAP, map, validMapEntry()));
        assertTrue(bean.isValid());
        assertNull(bean.getResult());

        // null value in the map
        map = new TreeMap<>();
        map.put(TEXT1, clean);
        map.put(TEXT2, null);
        bean = new BeanValidationResult(NAME, OBJECT);
        assertFalse(bean.validateMap(MY_MAP, map, validMapEntry()));
        assertFalse(bean.isValid());
        assertEquals(requote(BEAN_INVALID_MSG + MY_MAP_INVALID
                        + "item 'def' value 'null' INVALID, is null\n"), bean.getResult());

        map = Map.of(TEXT1, invalid, TEXT2, invalid);
        bean = new BeanValidationResult(NAME, OBJECT);
        assertFalse(bean.validateMap(MY_MAP, map, validMapEntry()));
        assertFalse(bean.isValid());
        assertEquals(requote(BEAN_INVALID_MSG + MY_MAP_INVALID + invalidMsg
                        + "    " + invalidMsg), bean.getResult());

    }

    private BiConsumer<BeanValidationResult, Entry<String, ValidationResult>> validMapEntry() {
        return (result, entry) -> {
            var value = entry.getValue();
            if (value == null) {
                result.validateNotNull(entry.getKey(), value);
            } else {
                result.addResult(value);
            }
        };
    }

    private static String requote(String text) {
        return text.replace('\'', '"');
    }
}
