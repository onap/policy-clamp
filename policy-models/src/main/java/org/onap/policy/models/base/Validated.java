/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.models.base;

import lombok.NonNull;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.common.parameters.BeanValidator;

/**
 * Classes that can be validated. This can be used as a super class or as a stand-alone
 * utility class.
 */
public class Validated {
    public static final String NOT_FOUND = "not found";

    /**
     * Validates the fields of the object. The default method uses a {@link BeanValidator}
     * to validate the object.
     *
     * @param fieldName name of the field containing this
     * @return the result, or {@code null}
     */
    public final BeanValidationResult validate(@NonNull String fieldName) {
        return BeanValidator.validate(fieldName, this);
    }
}
