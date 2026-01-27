/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
 *  Modifications Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.common.parameters;

import java.util.ArrayList;
import java.util.List;

/**
 * This class holds the result of the validation of an arbitrary bean.
 */
public class BeanValidationResult extends ValidationResultImpl {

    /**
     * Validation results for each item in the bean.
     */
    private final List<ValidationResult> itemResults = new ArrayList<>();


    /**
     * Constructs the object.
     *
     * @param name name of the bean being validated
     * @param object object being validated
     */
    public BeanValidationResult(String name, Object object) {
        super(name, object);
    }

    /**
     * Adds a result to this result.
     *
     * @param result the result to be added
     * @return {@code true} if the result is {@code null} or valid, {@code false} if the
     *         result is invalid
     */
    public boolean addResult(ValidationResult result) {
        if (result == null) {
            return true;
        }

        itemResults.add(result);
        setResult(result.getStatus());

        return result.isValid();
    }

    /**
     * Adds a result to this result.
     * @param name name of the object of this result
     * @param object object being validated
     * @param status status of the new result
     * @param message new result message
     * @return {@code true} if the status is {@code null} or valid, {@code false} if the
     *         status is invalid
     */
    public boolean addResult(String name, Object object, ValidationStatus status, String message) {
        return addResult(new ObjectValidationResult(name, object, status, message));
    }

    /**
     * Gets the validation result.
     *
     * @param initialIndentation the indentation to use on the main result output
     * @param subIndentation the indentation to use on sub parts of the result output
     * @return the result
     */
    @Override
    public String getResult(final String initialIndentation, final String subIndentation) {
        if (getStatus() == ValidationStatus.CLEAN) {
            return null;
        }

        var builder = new StringBuilder();

        builder.append(initialIndentation);
        builder.append('"');
        builder.append(getName());

        builder.append("\" ");
        builder.append(getStatus());
        builder.append(", ");
        builder.append(getMessage());
        builder.append('\n');

        for (ValidationResult itemResult : itemResults) {
            String message = itemResult.getResult(initialIndentation + subIndentation, subIndentation);
            if (message != null) {
                builder.append(message);
            }
        }

        return builder.toString();
    }
}
