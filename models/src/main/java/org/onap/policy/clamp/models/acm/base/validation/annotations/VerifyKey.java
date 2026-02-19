/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.models.acm.base.validation.annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Verifies a key is not a null key. Note @Valid annotation should also be used with this one to fully validate the key.
 */
@Retention(RUNTIME)
@Target({FIELD, METHOD})
@Constraint(validatedBy = VerifyKeyValidator.class)
public @interface VerifyKey {

    /**
     * Validates that key.isNullKey() is {@code false}.
     */
    boolean keyNotNull() default true;

    /**
     * Validates that key.isNullName() is {@code false}.
     */
    boolean nameNotNull() default true;

    /**
     * Validates that key.isNullVersion() is {@code false}.
     */
    boolean versionNotNull() default false;

    /**
     * The error message template.
     */
    String message() default "Key validation failed";

    /**
     * The groups the constraint belongs to.
     */
    Class<?>[] groups() default {};

    /**
     * The payload associated to the constraint.
     */
    Class<? extends Payload>[] payload() default {};
}
