/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
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
 * This class holds the result of the validation of an object within a bean.
 */
public class ObjectValidationResult extends ValidationResultImpl {

    /**
     * Constructs the object.
     *
     * @param name name of the object of this result
     * @param object object being validated
     */
    public ObjectValidationResult(String name, Object object) {
        super(name, object);
    }

    /**
     * Constructs the object.
     *
     * @param name name of the object of this result
     * @param object object being validated
     * @param status result status
     * @param message result message
     */
    public ObjectValidationResult(String name, Object object, ValidationStatus status, String message) {
        super(name, object, status, message);
    }

    /**
     * Gets the validation result.
     *
     * @param initialIndentation the result indentation
     * @param subIndentation the indentation to use on sub parts of the result output
     * @param showClean output information on clean fields
     * @return the result
     */
    @Override
    public String getResult(final String initialIndentation, final String subIndentation, final boolean showClean) {
        if (!showClean && getStatus() == ValidationStatus.CLEAN) {
            return null;
        }

        return initialIndentation + "item \"" + getName() + "\" value \"" + getObject() + "\" " + getStatus() + ", "
                        + getMessage() + '\n';
    }
}
