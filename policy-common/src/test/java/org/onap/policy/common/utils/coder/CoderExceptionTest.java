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

package org.onap.policy.common.utils.coder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CoderExceptionTest {
    private static final String STRING_VALUE = "My String";
    private static final Throwable CAUSE = new Throwable();

    private CoderException exc;

    @Test
    void testCoderException() {
        exc = new CoderException();

        assertNull(exc.getMessage());
        assertSame(null, exc.getCause());
        assertNotNull(exc.toString());
    }

    @Test
    void testCoderExceptionString() {
        exc = new CoderException(STRING_VALUE);

        assertEquals(STRING_VALUE, exc.getMessage());
        assertSame(null, exc.getCause());
        assertTrue(exc.toString().contains(STRING_VALUE));
    }

    @Test
    void testCoderExceptionThrowable() {
        exc = new CoderException(CAUSE);

        assertEquals(CAUSE.toString(), exc.getMessage());
        assertSame(CAUSE, exc.getCause());
        assertNotNull(exc.toString());
    }

    @Test
    void testCoderExceptionStringThrowable() {
        exc = new CoderException(STRING_VALUE, CAUSE);

        assertEquals(STRING_VALUE, exc.getMessage());
        assertSame(CAUSE, exc.getCause());
        assertTrue(exc.toString().contains(STRING_VALUE));
    }

}
