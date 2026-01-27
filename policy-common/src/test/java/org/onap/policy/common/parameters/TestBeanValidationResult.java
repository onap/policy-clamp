/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024-2026 OpenInfra Foundation Europe. All rights reserved.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestBeanValidationResult {
    private static final String TEXT1 = "abc";
    private static final String TEXT2 = "def";
    private static final String MY_LIST = "my-list";
    private static final String OBJECT = "an object";
    private static final String INITIAL_INDENT = "xx ";
    private static final String NEXT_INDENT = "yy ";
    private static final String MID_INDENT = "xx yy ";
    private static final String NAME = "my-name";
    private static final String BEAN_INVALID_MSG = "\"my-name\" INVALID, item has status INVALID\n";

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

        assertEquals(INITIAL_INDENT + BEAN_INVALID_MSG + MID_INDENT + invalidMsg,
                        bean.getResult(INITIAL_INDENT, NEXT_INDENT));

        bean = new BeanValidationResult(NAME, OBJECT);
        assertFalse(bean.addResult(MY_LIST, "hello", ValidationStatus.INVALID, TEXT1));
        assertThat(bean.getResult()).contains("\"" + MY_LIST + "\" value \"hello\" INVALID, " + TEXT1);
    }
}
