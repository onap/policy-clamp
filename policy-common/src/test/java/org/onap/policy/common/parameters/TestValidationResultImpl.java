/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestValidationResultImpl {
    private static final String NAME = "my-name";
    private static final Object OBJECT = "my-object";

    private MyResult result;

    @BeforeEach
    void setUp() {
        result = new MyResult(NAME, OBJECT);
    }

    @Test
    void testValidationResultImplStringObjectValidationStatusString() {
        result = new MyResult(NAME, OBJECT, ValidationStatus.INVALID, "invalid data");
        assertEquals(NAME, result.getName());
        assertEquals(OBJECT, result.getObject());
        assertEquals(ValidationStatus.INVALID, result.getStatus());
        assertEquals("invalid data", result.getMessage());
    }

    @Test
    void testSetResult_testGetResult_testGetStatus() {
        assertEquals(ValidationStatus.CLEAN, result.getStatus());

        result.setResult(ValidationStatus.INVALID, "is invalid");
        assertEquals(ValidationStatus.INVALID, result.getStatus());

        assertFalse(result.isValid());
        assertEquals("INVALID is invalid", result.getResult());
    }

    @Test
    void testGetName() {
        assertEquals(NAME, result.getName());
    }

    private static class MyResult extends ValidationResultImpl {
        public MyResult(String name, Object object) {
            super(name, object);
        }

        public MyResult(String name, Object object, ValidationStatus status, String message) {
            super(name, object, status, message);
        }

        @Override
        public String getResult(String initialIndentation, String subIndentation) {
            if (getStatus() == ValidationStatus.CLEAN) {
                return null;
            }

            return (getStatus() + " " + getMessage());
        }
    }
}
