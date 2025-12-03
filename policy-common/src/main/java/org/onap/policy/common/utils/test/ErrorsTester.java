/*
 * ============LICENSE_START====================================================
 * Common Utils-Test
 * =============================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights reserved.
 * =============================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END======================================================
 */

package org.onap.policy.common.utils.test;

/**
 * Used to test various Error subclasses. Uses reflection to identify the
 * constructors that the subclass supports.
 */
public class ErrorsTester extends ThrowablesTester {

    /**
     * Runs tests, on an Error subclass, for all of the standard
     * constructors.If the Error subclass does not support a given
     * type of constructor, then it skips that test.
     * Does <i>not</i> throw an exception if no standard constructors
     * are found.
     *
     * @param claz subclass to be tested
     * @param <T> this needs to be declared
     *
     * @return the number of constructors that were found/tested
     * @throws ConstructionError
     *             if the Error subclass cannot be constructed
     * @throws AssertionError
     *             if the constructed objects fail to pass various tests
     */
    public <T extends Error> int testAllError(final Class<T> claz) {
        return testAllThrowable(claz);
    }
}
