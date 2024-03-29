/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021,2023 Nordix Foundation.
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

package org.onap.policy.clamp.acm.runtime.main.web;

import org.onap.policy.clamp.common.acm.exception.AutomationCompositionRuntimeException;
import org.onap.policy.clamp.models.acm.messages.rest.SimpleResponse;
import org.onap.policy.clamp.models.acm.rest.RestUtils;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.errors.concepts.ErrorResponseInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalControllerExceptionHandler {

    /**
     * Handle AutomationCompositionRuntimeException, PfModelRuntimeException and PfModelException.
     *
     * @param ex Exception
     * @return ResponseEntity
     */
    @ExceptionHandler({AutomationCompositionRuntimeException.class, PfModelRuntimeException.class,
        PfModelException.class})
    public ResponseEntity<SimpleResponse> handleBadRequest(ErrorResponseInfo ex) {
        return RestUtils.toSimpleResponse(ex);
    }
}
