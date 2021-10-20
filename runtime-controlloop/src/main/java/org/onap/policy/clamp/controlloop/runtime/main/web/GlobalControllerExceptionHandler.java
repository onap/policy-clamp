/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.runtime.main.web;

import org.onap.policy.clamp.controlloop.common.exception.ControlLoopException;
import org.onap.policy.clamp.controlloop.models.messages.rest.SimpleResponse;
import org.onap.policy.clamp.controlloop.models.rest.RestUtils;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalControllerExceptionHandler {

    /**
     * Handle ControlLoopException.
     *
     * @param ex ControlLoopException
     * @return ResponseEntity
     */
    @ExceptionHandler(ControlLoopException.class)
    public ResponseEntity<SimpleResponse> handleBadRequest(ControlLoopException ex) {
        return RestUtils.toSimpleResponse(ex);
    }

    /**
     * Handle PfModelRuntimeException.
     *
     * @param ex PfModelRuntimeException
     * @return ResponseEntity
     */
    @ExceptionHandler(PfModelRuntimeException.class)
    public ResponseEntity<SimpleResponse> handleBadRequest(PfModelRuntimeException ex) {
        return RestUtils.toSimpleResponse(ex);
    }

    /**
     * Handle PfModelException.
     *
     * @param ex PfModelException
     * @return ResponseEntity
     */
    @ExceptionHandler(PfModelException.class)
    public ResponseEntity<SimpleResponse> handleBadRequest(PfModelException ex) {
        return RestUtils.toSimpleResponse(ex);
    }
}
