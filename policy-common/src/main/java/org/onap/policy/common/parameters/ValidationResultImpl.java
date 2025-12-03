/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
 *  Modifications Copyright (C) 2020 Bell Canada. All rights reserved.
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

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Basic implementation of a ValidationResult.
 */
@Getter
@AllArgsConstructor
public abstract class ValidationResultImpl implements ValidationResult {
    public static final String ITEM_HAS_STATUS_MESSAGE = "item has status ";

    /**
     * Name of the object of this result.
     */
    private final String name;

    /**
     * Object being validated.
     */
    private final Object object;

    /**
     * Validation status of this object.
     */
    private ValidationStatus status = ValidationStatus.CLEAN;

    /**
     * Validation message.
     */
    private String message = ITEM_HAS_STATUS_MESSAGE + status.toString();


    /**
     * Constructs the object.
     *
     * @param name name of the object of this result
     * @param object object being validated
     */
    protected ValidationResultImpl(String name, Object object) {
        this.name = name;
        this.object = object;
    }

    /**
     * Validates that the value is not {@code null}.
     *
     * @return {@code true} if the value is not null, {@code false} otherwise
     */
    public boolean validateNotNull() {
        if (object == null) {
            setResult(ValidationStatus.INVALID, "is null");
            return false;

        } else {
            return true;
        }
    }

    /**
     * Set the validation result status. On a sequence of calls, the most serious
     * validation status is recorded, assuming the status enum ordinals increase in order
     * of severity.
     *
     * @param status validation status the bean is receiving
     */
    public void setResult(final ValidationStatus status) {
        setResult(status, ITEM_HAS_STATUS_MESSAGE + status.toString());
    }

    /**
     * Set the validation result status. On a sequence of calls, the most serious
     * validation status is recorded, assuming the status enum ordinals increase in order
     * of severity.
     *
     * @param status the validation status
     * @param message the validation message explaining the validation status
     */
    @Override
    public void setResult(final ValidationStatus status, final String message) {
        if (this.status.ordinal() < status.ordinal()) {
            this.status = status;
            this.message = message;
        }
    }
}
