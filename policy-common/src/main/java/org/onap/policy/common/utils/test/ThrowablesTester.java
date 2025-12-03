/*
 * ============LICENSE_START====================================================
 * Common Utils-Test
 * =============================================================================
 * Copyright (C) 2018, 2020-2021 AT&T Intellectual Property. All rights reserved.
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to test various Throwable subclasses. Uses reflection to identify the
 * constructors that the subclass supports.
 */
public class ThrowablesTester {

    private static Logger logger =
                    LoggerFactory.getLogger(ThrowablesTester.class);

    public static final String EXPECTED_EXCEPTION_MSG =
                    "expected exception";
    private static final String EXPECTED_SUPPRESSED_EXCEPTION_MSG =
                    "expected suppressed exception";

    /**
     * Passed as a "cause" to constructors.
     */
    public static final Exception CAUSE =
                    new Exception(EXPECTED_EXCEPTION_MSG);

    /**
     * Passed to new objects via the <i>addSuppressed()</i> method..
     */
    public static final Throwable SUPPRESSED =
                    new Throwable(EXPECTED_SUPPRESSED_EXCEPTION_MSG);

    /**
     * Runs tests, on an Throwable subclass, for all of the
     * standard constructors. If the Throwable subclass does
     * not support a given type of constructor, then it skips
     * that test. Does <i>not</i> throw an exception if no
     * standard constructors are found.
     *
     * @param claz subclass to be tested
     * @param <T> To be defined
     * @return the number of constructors that were found/tested
     * @throws ConstructionError
     *             if the Throwable subclass cannot be constructed
     * @throws AssertionError
     *             if the constructed objects fail to pass various tests
     */
    public final <T extends Throwable> int testAllThrowable(
                    final Class<T> claz) {
        var ncons = 0;

        ncons += testDefault(claz);
        ncons += testString(claz);
        ncons += testThrowable(claz);
        ncons += testStringThrowable(claz);
        ncons += testStringThrowableBooleanBoolean(claz);

        return ncons;
    }

    /**
     * Tests Throwable objects created via the default constructor. Verifies
     * that:
     * <ul>
     * <li><i>toString()</i> returns a non-null value</li>
     * <li><i>getMessage()</i> returns null</li>
     * <li><i>getCause()</i> returns null</li>
     * </ul>
     *
     * <p>If the Throwable subclass does not support this type of
     * constructor, then this method simply returns.
     *
     * @param claz subclass to be tested
     * @param <T> to be defined
     * @return {@code 1}, if the subclass supports this type of constructor,
     *         {@code 0} otherwise
     * @throws ConstructionError
     *             if the Throwable subclass cannot be constructed
     * @throws AssertionError
     *             if the constructed objects fail to pass various tests
     */
    public final <T extends Throwable> int testDefault(
                    final Class<T> claz) {
        Constructor<T> cons = getConstructor(claz, "default");
        if (cons == null) {
            return 0;
        }

        var ex = newInstance(cons);

        assertNotNull(ex.toString());
        assertNull(ex.getMessage());
        assertNull(ex.getCause());

        return 1;
    }

    /**
     * Tests Throwable objects created via the constructor that takes just a
     * String. Verifies that:
     * <ul>
     * <li><i>toString()</i> returns a non-null value</li>
     * <li><i>getMessage()</i> returns the original message passed to the
     * constructor</li>
     * <li><i>getCause()</i> returns null</li>
     * </ul>
     *
     * <p>If the Throwable subclass does not support this type of constructor,
     * then this method simply returns.
     *
     * @param claz
     *            subclass to be tested
     * @param <T> to be defined
     * @return {@code 1}, if the subclass supports this type of constructor,
     *         {@code 0} otherwise
     * @throws ConstructionError
     *             if the Throwable subclass cannot be constructed
     * @throws AssertionError
     *             if the constructed objects fail to pass various tests
     */
    public final <T extends Throwable> int testString(final Class<T> claz) {
        Constructor<T> cons = getConstructor(claz, "string",
                        String.class);
        if (cons == null) {
            return 0;
        }

        var ex = newInstance(cons, "hello");

        assertNotNull(ex.toString());
        assertEquals("hello", ex.getMessage());
        assertNull(ex.getCause());

        return 1;
    }

    /**
     * Tests Throwable objects created via the constructor that takes just a
     * Throwable. Verifies that:
     * <ul>
     * <li><i>toString()</i> returns a non-null value</li>
     * <li><i>getMessage()</i> returns the cause's message</li>
     * <li><i>getCause()</i> returns the original cause passed to the
     * constructor</li>
     * </ul>
     *
     * <p>If the Throwable subclass does not support this type of constructor,
     * then this method simply returns.
     *
     * @param claz
     *            subclass to be tested
     * @param <T> to be defined
     * @return {@code 1}, if the subclass supports this type of constructor,
     *         {@code 0} otherwise
     * @throws ConstructionError
     *             if the Throwable subclass cannot be constructed
     * @throws AssertionError
     *             if the constructed objects fail to pass various tests
     */
    public final <T extends Throwable> int testThrowable(
                    final Class<T> claz) {
        Constructor<T> cons = getConstructor(claz, "throwable",
                        Throwable.class);
        if (cons == null) {
            return 0;
        }

        var ex = newInstance(cons, CAUSE);

        assertEquals(ex.getMessage(), ex.getMessage());
        assertNotNull(ex.toString());
        assertEquals(CAUSE, ex.getCause());

        return 1;
    }

    /**
     * Tests Throwable objects created via the constructor that takes
     * a String and a Throwable. Verifies that:
     * <ul>
     * <li><i>toString()</i> returns a non-null value</li>
     * <li><i>getMessage()</i> returns the original message passed to the
     * constructor</li>
     * <li><i>getCause()</i> returns the original cause passed to the
     * constructor</li>
     * </ul>
     *
     * <p>If the Throwable subclass does not support this type of constructor,
     * then this method simply returns.
     *
     * @param claz subclass to be tested
     * @param <T> to be defined
     * @return {@code 1}, if the subclass supports this type of constructor,
     *         {@code 0} otherwise
     * @throws ConstructionError
     *             if the Throwable subclass cannot be constructed
     * @throws AssertionError
     *             if the constructed objects fail to pass various tests
     */
    public final <T extends Throwable> int testStringThrowable(
                    final Class<T> claz) {
        Constructor<T> cons = getConstructor(claz, "string-throwable",
                        String.class, Throwable.class);
        if (cons == null) {
            return 0;
        }

        var ex = newInstance(cons, "world", CAUSE);

        assertNotNull(ex.toString());
        assertEquals("world", ex.getMessage());
        assertEquals(CAUSE, ex.getCause());

        return 1;
    }

    /**
     * Tests Throwable objects created via the constructor that takes
     * a String, a Throwable, and two booleans. Verifies that:
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
     * <p>If the Throwable subclass does not support this type of constructor,
     * then this method simply returns.
     *
     * @param claz
     *            subclass to be tested
     * @param <T> to be defined
     * @return {@code 1}, if the subclass supports this type of constructor,
     *         {@code 0} otherwise
     * @throws ConstructionError
     *             if the Throwable subclass cannot be constructed
     * @throws AssertionError
     *             if the constructed objects fail to pass various tests
     */
    public final <T extends Throwable> int
        testStringThrowableBooleanBoolean(
                    final Class<T> claz) {
        Constructor<T> cons = getConstructor(claz,
                        "string-throwable-flags",
                        String.class, Throwable.class,
                        Boolean.TYPE, Boolean.TYPE);
        if (cons == null) {
            return 0;
        }

        // test each combination of "message" and "cause"
        testMessageCauseCombos(cons);

        // test each combination of the boolean flags
        testSuppressStack(cons);
        testSuppressNoStack(cons);
        testNoSuppressStack(cons);
        testNoSuppressNoStack(cons);

        return 1;
    }

    /**
     * Tests each combination of values for the "message" and the "cause"
     * when using the constructor that takes a String,
     * a Throwable/Exception, and two booleans. Verifies that expected
     * values are returned
     * <ul>
     * <i>toString()</i>,
     * <i>getMessage()</i>, and <i>getCause()</i>.
     * </ul>
     *
     * @param cons
     *            constructor to be invoked
     * @param <T> to be defined
     * @throws ConstructionError
     *             if the Throwable subclass cannot be constructed
     * @throws AssertionError
     *             if the constructed objects fail to pass various tests
     */
    public final <T extends Throwable> void testMessageCauseCombos(
                    final Constructor<T> cons) {
        T ex;

        ex = newInstance(cons, null, null, true, true);
        assertNotNull(ex.toString());
        assertNull(ex.getMessage());
        assertNull(ex.getCause());

        ex = newInstance(cons, "abc", null, true, true);
        assertNotNull(ex.toString());
        assertEquals("abc", ex.getMessage());
        assertNull(ex.getCause());

        ex = newInstance(cons, null, CAUSE, true, true);
        assertNotNull(ex.toString());
        assertNull(ex.getMessage());
        assertEquals(CAUSE, ex.getCause());

        ex = newInstance(cons, "xyz", CAUSE, true, true);
        assertNotNull(ex.toString());
        assertEquals("xyz", ex.getMessage());
        assertEquals(CAUSE, ex.getCause());
    }

    /**
     * Tests each combination of values for the "message" and the
     * "cause" when using the constructor that takes a String,
     * a Throwable/Exception, and two booleans. Verifies that
     * expected values are returned by
     * <ul>
     * <i>toString()</i>,
     * <i>getMessage()</i>, and <i>getCause()</i>.
     * </ul>
     *
     * @param cons
     *            constructor to be invoked
     * @param <T> to be defined
     * @throws ConstructionError
     *             if the Throwable subclass cannot be constructed
     * @throws AssertionError
     *             if the constructed objects fail to pass various tests
     */
    public final <T extends Throwable> void testFlagCombos(
                    final Constructor<T> cons) {
        testSuppressStack(cons);
        testSuppressNoStack(cons);
        testNoSuppressStack(cons);
        testNoSuppressNoStack(cons);
    }

    /**
     * Tests Throwable objects constructed with
     * {@code enableSuppression=true} and
     * {@code writableStackTrace=true}. Verifies that:
     * <ul>
     * <li><i>toString()</i> returns a non-null value</li>
     * <li><i>getMessage()</i> returns the original message passed to the
     * constructor</li>
     * <li><i>getCause()</i> returns the original cause passed to the
     * constructor</li>
     * <li>suppressed exceptions are added</li>
     * <li>the stack trace is added</li>
     * </ul>
     *
     * @param cons
     *            the throwable's class constructor
     * @param <T> to be defined
     * @throws ConstructionError
     *             if the Throwable subclass cannot be constructed
     * @throws AssertionError
     *             if the constructed objects fail to pass various tests
     */
    public final <T extends Throwable> void testSuppressStack(
                    final Constructor<T> cons) {
        var ex = newInstance(cons, "yes,yes", CAUSE, true, true);

        ex.addSuppressed(SUPPRESSED);

        assertNotNull(ex.toString());
        assertEquals("yes,yes", ex.getMessage());
        assertEquals(CAUSE, ex.getCause());

        assertEquals(1, ex.getSuppressed().length);
        assertEquals(SUPPRESSED, ex.getSuppressed()[0]);

        assertTrue(ex.getStackTrace().length > 0);
    }

    /**
     * Tests Throwable objects constructed with
     * {@code enableSuppression=true} and
     * {@code writableStackTrace=false}. Verifies that:
     * <ul>
     * <li><i>toString()</i> returns a non-null value</li>
     * <li><i>getMessage()</i> returns the original message passed to the
     * constructor</li>
     * <li><i>getCause()</i> returns the original cause passed to the
     * constructor</li>
     * <li>suppressed exceptions are added</li>
     * <li>the stack trace is <i>not</i> added</li>
     * </ul>
     *
     * @param cons
     *            the throwable's class constructor
     * @param <T> to be defined
     * @throws ConstructionError
     *             if the Throwable subclass cannot be constructed
     * @throws AssertionError
     *             if the constructed objects fail to pass various tests
     */
    public final <T extends Throwable> void testSuppressNoStack(
                    final Constructor<T> cons) {
        var ex = newInstance(cons, "yes,no", CAUSE, true, false);

        ex.addSuppressed(SUPPRESSED);

        assertNotNull(ex.toString());
        assertEquals("yes,no", ex.getMessage());
        assertEquals(CAUSE, ex.getCause());

        assertEquals(1, ex.getSuppressed().length);
        assertEquals(SUPPRESSED, ex.getSuppressed()[0]);

        assertEquals(0, ex.getStackTrace().length);
    }

    /**
     * Tests Throwable objects constructed with
     * {@code enableSuppression=false} and
     * {@code writableStackTrace=true}. Verifies that:
     * <ul>
     * <li><i>toString()</i> returns a non-null value</li>
     * <li><i>getMessage()</i> returns the original message passed to the
     * constructor</li>
     * <li><i>getCause()</i> returns the original cause passed to the
     * constructor</li>
     * <li>suppressed exceptions are <i>not</i> added</li>
     * <li>the stack trace is added</li>
     * </ul>
     *
     * @param cons
     *            the throwable's class constructor
     * @param <T> to be defined
     * @throws ConstructionError
     *             if the Throwable subclass cannot be constructed
     * @throws AssertionError
     *             if the constructed objects fail to pass various tests
     */
    public final <T extends Throwable> void testNoSuppressStack(
                    final Constructor<T> cons) {
        var ex = newInstance(cons, "no,yes", CAUSE, false, true);

        ex.addSuppressed(SUPPRESSED);

        assertNotNull(ex.toString());
        assertEquals("no,yes", ex.getMessage());
        assertEquals(CAUSE, ex.getCause());

        assertEquals(0, ex.getSuppressed().length);

        assertTrue(ex.getStackTrace().length > 0);
    }

    /**
     * Tests Throwable objects constructed with
     * {@code enableSuppression=false} and
     * {@code writableStackTrace=false}. Verifies that:
     * <ul>
     * <li><i>toString()</i> returns a non-null value</li>
     * <li><i>getMessage()</i> returns the original message passed to the
     * constructor</li>
     * <li><i>getCause()</i> returns the original cause passed to the
     * constructor</li>
     * <li>suppressed exceptions are <i>not</i> added</li>
     * <li>the stack trace is <i>not</i> added</li>
     * </ul>
     * @param cons
     *            the throwable's class constructor
     * @param <T> to be defined
     * @throws ConstructionError
     *             if the Throwable subclass cannot be constructed
     * @throws AssertionError
     *             if the constructed objects fail to pass various tests
     */
    public final <T extends Throwable> void testNoSuppressNoStack(
                    final Constructor<T> cons) {
        var ex = newInstance(cons, "no,no", CAUSE, false, false);

        ex.addSuppressed(SUPPRESSED);

        assertNotNull(ex.toString());
        assertEquals("no,no", ex.getMessage());
        assertEquals(CAUSE, ex.getCause());

        assertEquals(0, ex.getSuppressed().length);
        assertEquals(0, ex.getStackTrace().length);
    }

    /**
     * Attempts to get a constructor for objects of a given type.
     *
     * @param claz
     *            class of objects whose constructor is to be gotten
     * @param <T> to be defined
     * @param testType
     *            type of test being run
     * @param argTypes
     *            argument types to be passed to the constructor
     * @return the desired constructor, or {@code null} if the desired
     *         constructor is not available
     */
    protected <T extends Throwable> Constructor<T> getConstructor(
                    final Class<T> claz,
                    final String testType,
                    final Class<?>... argTypes) {

        try {
            return claz.getConstructor(argTypes);

        } catch (NoSuchMethodException | SecurityException e) {
            // this constructor is not defined so nothing to test
            logger.debug("skipped test, no constructor for: {}", claz, e);
            return null;
        }
    }

    /**
     * Creates a new instance of an Throwable subclass.
     *
     * @param cons
     *            subclass constructor
     * @param <T> to be defined
     * @param args
     *            arguments to be passed to the constructor
     * @return a new instance of the Throwable subclass
     * @throws ConstructionError
     *             if the Throwable subclass cannot be constructed
     */
    protected <T extends Throwable> T newInstance(
                    final Constructor<T> cons,
                    final Object... args) {
        try {
            return cons.newInstance(args);

        } catch (InstantiationException | IllegalAccessException
                        | IllegalArgumentException
                        | InvocationTargetException e) {

            throw new ConstructionError(e);
        }

    }
}
