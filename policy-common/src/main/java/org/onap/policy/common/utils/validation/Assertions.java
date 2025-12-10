/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2016-2018 Ericsson. All rights reserved.
 *  Modifications Copyright (C) 2019 Nordix Foundation.
 *  Modifications Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.common.utils.validation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * The Class Assertions is a static class that is used as a shorthand for assertions in the source code.
 * It throws runtime exceptions on assertion fails.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Assertions {
    /**
     * Checks if a string parameter matches a regular expression.
     *
     * @param parameterName the string parameter name
     * @param parameterValue the string parameter value
     * @param pattern The regular expression
     * @return the trimmed string
     */
    public static String validateStringParameter(final String parameterName, final String parameterValue,
                    final String pattern) {
        argumentNotNull(parameterName, "parameter name is null");
        argumentNotNull(parameterValue, "parameter \"" + parameterName + "\" is null");
        argumentNotNull(pattern, "parameter pattern is null");

        final String trimmedValue = parameterValue.trim();
        if (trimmedValue.matches(pattern)) {
            return trimmedValue;
        } else {
            throw new IllegalArgumentException("parameter \"" + parameterName + "\": value \"" + parameterValue
                            + "\", does not match regular expression \"" + pattern + "\"");
        }
    }

    /**
     * Used as a shorthand to check that method arguments are not null, throws IllegalArgumentException on error.
     *
     * @param <T> the generic type of the argument to check
     * @param value the value of the type
     * @param message the error message to issue
     */
    public static <T> void argumentNotNull(final T value, final String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Used as a shorthand to check that an object is an instance of a given class, throws IllegalArgumentException on
     * error.
     *
     * @param <T> the generic type of the argument to check
     * @param objectInstance the object instance for which to check the class
     * @param requiredClass the class that the object should be an instance of
     * @throws IllegalArgumentException if the incoming object is not an instance of requiredClass
     */
    public static <T> void instanceOf(final Object objectInstance, final Class<T> requiredClass) {
        if (!requiredClass.isAssignableFrom(objectInstance.getClass())) {
            throw new IllegalArgumentException(objectInstance.getClass().getName() + " is not an instance of "
                            + requiredClass.getName());
        }
    }
}
