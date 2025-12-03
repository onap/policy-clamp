/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2018 Ericsson. All rights reserved.
 * Modifications Copyright (C) 2024 Nordix Foundation.
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

/**
 * This interface defines the result of a parameter validation.
 */
public interface ValidationResult {
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
        return getResult(
            ParameterConstants.DEFAULT_INITIAL_RESULT_INDENTATION,
            ParameterConstants.DEFAULT_RESULT_INDENTATION,
            ParameterConstants.DO_NOT_SHOW_CLEAN_RESULTS);
    }

    /**
     * Gets the validation result.
     *
     * @param initialIndentation the indentation to use on the main result output
     * @param subIndentation     the indentation to use on sub parts of the result output
     * @param showClean          output information on clean fields
     * @return the result
     */
    String getResult(final String initialIndentation, final String subIndentation, final boolean showClean);

    /**
     * Set a validation result.
     *
     * @param status  The validation status the field is receiving
     * @param message The validation message explaining the validation status
     */
    void setResult(final ValidationStatus status, final String message);
}