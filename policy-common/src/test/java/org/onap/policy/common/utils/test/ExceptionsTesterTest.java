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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ExceptionsTesterTest {

    @Test
    void test() {
        assertEquals(2, new ExceptionsTester().test(SimpleException.class));
        assertEquals(8, new ExceptionsTester().test(StaticException.class));
    }

    @Test
    void testNoConstructorsException() {
        ExceptionsTester tester = new ExceptionsTester();
        assertThatThrownBy(() -> tester.test(NoConstructorsException.class))
            .isInstanceOf(AssertionError.class);
    }

    /**
     * Used to test a failure case - this has no standard constructors. The only constructor it has
     * takes an "int", thus it is not one of the standard constructors.
     */
    public static class NoConstructorsException extends Exception {
        private static final long serialVersionUID = 1L;

        public NoConstructorsException(int value) {
            super();
        }
    }

    /**
     * Used to test a simple success case.
     */
    public static class SimpleException extends Exception {
        private static final long serialVersionUID = 1L;

        public SimpleException() {
            super();
        }

        public SimpleException(String message) {
            super(message);
        }
    }

    /**
     * Used to test the exhaustive success case.
     */
    public static class StaticException extends Exception {
        private static final long serialVersionUID = 1L;

        public StaticException() {
            super();
        }

        public StaticException(String message) {
            super(message);
        }

        public StaticException(Throwable cause) {
            super(cause);
        }

        public StaticException(String message, Throwable cause) {
            super(message, cause);
        }

        public StaticException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }

        // same as above, but with Exceptions substituted for Throwables

        public StaticException(Exception cause) {
            super(cause);
        }

        public StaticException(String message, Exception cause) {
            super(message, cause);
        }

        public StaticException(String message, Exception cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }

}
