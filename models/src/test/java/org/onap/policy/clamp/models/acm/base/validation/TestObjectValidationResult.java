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

package org.onap.policy.clamp.models.acm.base.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class TestObjectValidationResult {
    private static final String NAME = "my-name";
    private static final Object OBJECT = "my-object";

    private ObjectValidationResult result;

    @Test
    void testValidationResultImplStringObjectValidationStatusString() {
        result = new ObjectValidationResult(NAME, OBJECT, ValidationStatus.INVALID, "invalid data");
        assertEquals(NAME, result.getName());
        assertEquals(OBJECT, result.getObject());
        assertEquals(ValidationStatus.INVALID, result.getStatus());
        assertEquals("invalid data", result.getMessage());
    }

    @Test
    void testGetResult() {
        result = new ObjectValidationResult(NAME, OBJECT);
        assertEquals(ValidationStatus.CLEAN, result.getStatus());
        assertNull(result.getResult());

        result.setResult(ValidationStatus.INVALID, "is invalid");
        assertEquals(ValidationStatus.INVALID, result.getStatus());

        assertFalse(result.isValid());
        assertEquals("item \"my-name\" value \"my-object\" INVALID, is invalid\n", result.getResult());
    }
}
