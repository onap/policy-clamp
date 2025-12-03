/*
 * ============LICENSE_START=======================================================
 * Common Utils-Test
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.common.utils.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ErrorsTesterTest {

    @Test
    void test() {
        assertEquals(2, new ErrorsTester().testAllError(SimpleError.class));
        assertEquals(5, new ErrorsTester().testAllError(StaticError.class));
    }

    /**
     * Used to test a simple success case.
     */
    public static class SimpleError extends Error {
        private static final long serialVersionUID = 1L;

        public SimpleError() {
            super();
        }

        public SimpleError(String message) {
            super(message);
        }
    }

    /**
     * Used to test the exhaustive success case.
     */
    public static class StaticError extends Error {
        private static final long serialVersionUID = 1L;

        public StaticError() {
            super();
        }

        public StaticError(String message) {
            super(message);
        }

        public StaticError(Throwable cause) {
            super(cause);
        }

        public StaticError(String message, Throwable cause) {
            super(message, cause);
        }

        public StaticError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }

}
