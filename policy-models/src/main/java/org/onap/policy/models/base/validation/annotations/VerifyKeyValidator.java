/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.models.base.validation.annotations;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.onap.policy.models.base.PfKey;
import org.onap.policy.models.base.PfKeyImpl;
import org.onap.policy.models.base.PfUtils;

public class VerifyKeyValidator implements ConstraintValidator<VerifyKey, PfKey> {

    private VerifyKey annotation;

    @Override
    public void initialize(VerifyKey constraintAnnotation) {
        this.annotation = constraintAnnotation;
    }

    @Override
    public boolean isValid(PfKey pfkey, ConstraintValidatorContext context) {
        if (pfkey == null) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        boolean valid = true;

        if (annotation.keyNotNull() && PfUtils.isNullKey(pfkey)) {
            context.buildConstraintViolationWithTemplate("is a null key").addConstraintViolation();
            return false;
        }

        if (pfkey instanceof PfKeyImpl keyImpl) {
            if (annotation.nameNotNull() && PfUtils.isNullName(keyImpl)) {
                context.buildConstraintViolationWithTemplate("is null")
                    .addPropertyNode("name").addConstraintViolation();
                valid = false;
            }

            if (annotation.versionNotNull() && PfUtils.isNullVersion(keyImpl)) {
                context.buildConstraintViolationWithTemplate("is null")
                    .addPropertyNode("version").addConstraintViolation();
                valid = false;
            }
        }

        return valid;
    }
}
