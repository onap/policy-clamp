/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2018 Ericsson. All rights reserved.
 *  Modifications Copyright (C) 2024-2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.models.acm.base.validation;

/**
 * This interface defines the result of a parameter validation.
 */
public interface ValidationResult {
    // Indentation is 0 on the left and 2 for each level of hierarchy
    String DEFAULT_INITIAL_RESULT_INDENTATION = "";
    String DEFAULT_RESULT_INDENTATION = "  ";

    /**
     * Gets the name of the entity being validated.
     *
     * @return the name
     */
    String getName();

    /**
     * Gets the status of validation.
     *
     * @return the status
     */
    ValidationStatus getStatus();

    /**
     * Checks if the result is valid.
     *
     * @return true, if is valid
     */
    default boolean isValid() {
        return getStatus().isValid();
    }

    /**
     * Checks if the result is clean.
     *
     * @return true, if is clean
     */
    default boolean isClean() {
        return getStatus().isClean();
    }

    /**
     * Gets the validation result.
     *
     * @return the full validation result
     */
    default String getResult() {
        return getResult(DEFAULT_INITIAL_RESULT_INDENTATION, DEFAULT_RESULT_INDENTATION);
    }

    /**
     * Gets the validation result.
     *
     * @param initialIndentation the indentation to use on the main result output
     * @param subIndentation     the indentation to use on sub parts of the result output
     * @return the result
     */
    String getResult(final String initialIndentation, final String subIndentation);

    /**
     * Set a validation result.
     *
     * @param status  The validation status the field is receiving
     * @param message The validation message explaining the validation status
     */
    void setResult(final ValidationStatus status, final String message);
}
