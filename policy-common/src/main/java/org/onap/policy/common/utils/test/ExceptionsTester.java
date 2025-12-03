/*
 * ============LICENSE_START====================================================
 * Common Utils-Test
 * =============================================================================
 * Copyright (C) 2018, 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024 Nordix Foundation
 * =============================================================================
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
 * ============LICENSE_END======================================================
 */

package org.onap.policy.common.utils.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;

/**
 * Used to test various Exception subclasses. Uses reflection to identify the
 * constructors that the subclass supports.
 */
public class ExceptionsTester extends ThrowablesTester {

    /**
     * Runs tests, on an Exception subclass, for all of the standard
     * constructors. If the Exception subclass does not support a given
     * type of constructor, then it skips that test.
     *
     * @param claz subclass to be tested
     * @param <T> Type of the class
     *
     * @return the number of constructors that were found/tested
     * @throws ConstructionError
     *             if the Exception subclass cannot be constructed
     * @throws AssertionError
     *             if the constructed objects fail to pass various tests
     */
    public <T extends Exception> int test(final Class<T> claz) {
        int ncons = testAllException(claz);

        assertTrue(ncons > 0);

        return ncons;
    }

    /**
     * Runs tests, on an Exception subclass, for all of the standard
     * constructors. If the Exception subclass does not support a given
     * type of constructor, then it skips that test. Does <i>not</i> throw
     * an exception if no standard constructors are found.
     *
     * @param claz subclass to be tested
     * @param <T> type of the class
     *
     * @return the number of constructors that were found/tested
     * @throws ConstructionError
     *             if the Exception subclass cannot be constructed
     * @throws AssertionError
     *             if the constructed objects fail to pass various tests
     */
    public <T extends Exception> int testAllException(final Class<T> claz) {
        var ncons = 0;

        ncons += testAllThrowable(claz);
        ncons += testException(claz);
        ncons += testStringException(claz);
        ncons += testStringExceptionBooleanBoolean(claz);

        return ncons;
    }

    /**
     * Tests exceptions created via the constructor that takes just an
     * Exception. Verifies that:
     * <ul>
     * <li><i>toString()</i> returns a non-null value</li>
     * <li><i>getMessage()</i> returns the cause's message</li>
     * <li><i>getCause()</i> returns the original cause passed to the
     * constructor</li>
     * </ul>
     *
     * <p>If the Exception subclass does not support this type of constructor,
     * then this method simply returns.
     *
     * @param claz subclass to be tested
     * @param <T> Type of the class
     *
     * @return {@code 1}, if the subclass supports this type of constructor,
     *         {@code 0} otherwise
     * @throws ConstructionError
     *             if the Exception subclass cannot be constructed
     * @throws AssertionError
     *             if the constructed objects fail to pass various tests
     */
    public <T extends Exception> int testException(final Class<T> claz) {
        Constructor<T> cons = getConstructor(claz, "exception",
                        Exception.class);

        if (cons == null) {
            return 0;
        }

        var cause = new Exception(EXPECTED_EXCEPTION_MSG);
        var ex = newInstance(cons, cause);

        assertNotNull(ex.toString());
        assertEquals(ex.getMessage(), ex.getMessage());
        assertEquals(cause, ex.getCause());

        return 1;
    }

    /**
     * Tests exceptions created via the constructor that takes a String
     * and an Exception. Verifies that:
     * <ul>
     * <li><i>toString()</i> returns a non-null value</li>
     * <li><i>getMessage()</i> returns the original message passed to
     * the constructor</li>
     * <li><i>getCause()</i> returns the original cause passed to the
     * constructor</li>
     * </ul>
     *
     * <p>If the Exception subclass does not support this type of
     * constructor, then this method simply returns.
     *
     * @param claz subclass to be tested
     * @param <T> Type of the class
     *
     * @return {@code 1}, if the subclass supports this type of constructor,
     *         {@code 0} otherwise
     * @throws ConstructionError
     *             if the Exception subclass cannot be constructed
     */
    public <T extends Exception> int testStringException(
                    final Class<T> claz) {
        Constructor<T> cons = getConstructor(claz, "string-exception",
                        String.class, Exception.class);
        if (cons == null) {
            return 0;
        }

        var cause = new Exception(EXPECTED_EXCEPTION_MSG);
        var ex = newInstance(cons, "world", cause);

        assertNotNull(ex.toString());
        assertEquals("world", ex.getMessage());
        assertEquals(cause, ex.getCause());

        return 1;
    }

    /**
     * Tests exceptions created via the constructor that takes a String, an
     * Exception, and two booleans. Verifies that:
     * <ul>
     * <li><i>toString()</i> returns a non-null value</li>
     * <li><i>getMessage()</i> returns the original message passed to the
     * constructor</li>
     * <li><i>getCause()</i> returns the original cause passed to the
     * constructor</li>
     * <li>suppressed exceptions can be added, if enabled</li>
     * <li>the stack trace can be added, if enabled</li>
     * </ul>
     *
     * <p>If the Exception subclass does not support this type of constructor,
     * then this method simply returns.
     *
     * @param claz subclass to be tested
     * @param <T> Type of the class
     *
     * @return {@code 1}, if the subclass supports this type of constructor,
     *         {@code 0} otherwise
     * @throws ConstructionError
     *             if the Exception subclass cannot be constructed
     */
    public <T extends Exception> int testStringExceptionBooleanBoolean(
                    final Class<T> claz) {

        Constructor<T> cons = getConstructor(claz,
                        "string-exception-flags", String.class, Exception.class,
                        Boolean.TYPE, Boolean.TYPE);

        if (cons == null) {
            return 0;
        }

        // test each combination of "message" and "cause"
        testMessageCauseCombos(cons);

        // test each combination of the boolean flags
        testFlagCombos(cons);

        return 1;
    }
}
