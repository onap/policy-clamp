/*
 * ============LICENSE_START=======================================================
 * Common Utils-Test
 * ================================================================================
 * Copyright (C) 2018-2019 AT&T Intellectual Property. All rights reserved.
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

class ThrowablesTesterTest {

    @Test
    void test() {
        assertEquals(2, new ThrowablesTester().testAllThrowable(SimpleThrowable.class));
        assertEquals(5, new ThrowablesTester().testAllThrowable(StaticThrowable.class));
    }

    @Test
    void testNoConstructorsThrowable() {
        // this will not throw an error, but it should return 0, as there are
        // no matching constructors
        assertEquals(0, new ThrowablesTester().testAllThrowable(NoConstructorsThrowable.class));
    }

    @Test
    void testIgnoreMessageThrowable() {
        ThrowablesTester tester = new ThrowablesTester();
        assertThatThrownBy(() -> tester.testAllThrowable(IgnoreMessageThrowable.class))
            .isInstanceOf(AssertionError.class);
    }

    @Test
    void testIgnoreCauseThrowable() {
        ThrowablesTester tester = new ThrowablesTester();
        assertThatThrownBy(() -> tester.testAllThrowable(IgnoreCauseThrowable.class))
            .isInstanceOf(AssertionError.class);
    }

    @Test
    void testAlwaysSuppressThrowable() {
        ThrowablesTester tester = new ThrowablesTester();
        assertThatThrownBy(() -> tester.testAllThrowable(AlwaysSuppressThrowable.class))
            .isInstanceOf(AssertionError.class);
    }

    @Test
    void testNeverSuppressThrowable() {
        ThrowablesTester tester = new ThrowablesTester();
        assertThatThrownBy(() -> tester.testAllThrowable(NeverSuppressThrowable.class))
            .isInstanceOf(AssertionError.class);
    }

    @Test
    void testAlwaysWritableThrowable() {
        ThrowablesTester tester = new ThrowablesTester();
        assertThatThrownBy(() -> tester.testAllThrowable(AlwaysWritableThrowable.class))
            .isInstanceOf(AssertionError.class);
    }

    @Test
    void testNeverWritableThrowable() {
        ThrowablesTester tester = new ThrowablesTester();
        assertThatThrownBy(() -> tester.testAllThrowable(NeverWritableThrowable.class))
            .isInstanceOf(AssertionError.class);
    }

    @Test
    void testThrowInstantiationException() {
        ThrowablesTester tester = new ThrowablesTester();
        assertThatThrownBy(() -> tester.testAllThrowable(ThrowInstantiationThrowable.class))
            .isInstanceOf(AssertionError.class);
    }

    /**
     * Used to test a failure case - message text is ignored.
     */
    public static class IgnoreMessageThrowable extends Throwable {
        private static final long serialVersionUID = 1L;

        public IgnoreMessageThrowable(String message) {
            super("bogus");
        }
    }

    /**
     * Used to test a failure case - cause is ignored.
     */
    public static class IgnoreCauseThrowable extends Throwable {
        private static final long serialVersionUID = 1L;

        public IgnoreCauseThrowable(Throwable cause) {
            super(new Throwable("another cause"));
        }
    }

    /**
     * Used to test a failure case - this has no standard constructors. The only constructor it has
     * takes an "int", thus it is not one of the standard constructors.
     */
    public static class NoConstructorsThrowable extends Throwable {
        private static final long serialVersionUID = 1L;

        public NoConstructorsThrowable(int value) {
            super();
        }
    }

    /**
     * Used to test a failure case - always suppresses.
     */
    public static class AlwaysSuppressThrowable extends Throwable {
        private static final long serialVersionUID = 1L;

        public AlwaysSuppressThrowable(String message, Throwable cause, boolean enableSuppression,
                boolean writableStackTrace) {
            super(message, cause, true, writableStackTrace);
        }
    }

    /**
     * Used to test a failure case - never suppresses.
     */
    public static class NeverSuppressThrowable extends Throwable {
        private static final long serialVersionUID = 1L;

        public NeverSuppressThrowable(String message, Throwable cause, boolean enableSuppression,
                boolean writableStackTrace) {
            super(message, cause, false, writableStackTrace);
        }
    }

    /**
     * Used to test a failure case - always allows stack writes.
     */
    public static class AlwaysWritableThrowable extends Throwable {
        private static final long serialVersionUID = 1L;

        public AlwaysWritableThrowable(String message, Throwable cause, boolean enableSuppression,
                boolean writableStackTrace) {
            super(message, cause, enableSuppression, true);
        }
    }

    /**
     * Used to test a failure case - never allows stack writes.
     */
    public static class NeverWritableThrowable extends Throwable {
        private static final long serialVersionUID = 1L;

        public NeverWritableThrowable(String message, Throwable cause, boolean enableSuppression,
                boolean writableStackTrace) {
            super(message, cause, enableSuppression, false);
        }
    }

    /**
     * Used to test a failure case - throws InstantiationException when constructed.
     */
    public static class ThrowInstantiationThrowable extends Throwable {
        private static final long serialVersionUID = 1L;

        public ThrowInstantiationThrowable(String message, Throwable cause, boolean enableSuppression,
                boolean writableStackTrace) throws InstantiationException {

            throw new InstantiationException(ThrowablesTester.EXPECTED_EXCEPTION_MSG);
        }
    }

    /**
     * Used to test a simple success case.
     */
    public static class SimpleThrowable extends Throwable {
        private static final long serialVersionUID = 1L;

        public SimpleThrowable() {
            super();
        }

        public SimpleThrowable(String message) {
            super(message);
        }
    }

    /**
     * Used to test the exhaustive success case.
     */
    public static class StaticThrowable extends Throwable {
        private static final long serialVersionUID = 1L;

        public StaticThrowable() {
            super();
        }

        public StaticThrowable(String message) {
            super(message);
        }

        public StaticThrowable(Throwable cause) {
            super(cause);
        }

        public StaticThrowable(String message, Throwable cause) {
            super(message, cause);
        }

        public StaticThrowable(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }

}
