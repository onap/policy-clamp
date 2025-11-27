/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021,2023,2025 OpenInfra Foundation Europe. All rights reserved
 * Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

import static org.springframework.boot.web.error.ErrorAttributeOptions.Include;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.onap.policy.clamp.models.acm.messages.rest.SimpleResponse;
import org.onap.policy.clamp.models.acm.messages.rest.TypedSimpleResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;

@Controller
@Hidden
public class RuntimeErrorController implements ErrorController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeErrorController.class);

    private final ErrorAttributes errorAttributes;

    /**
     * Constructor.
     *
     * @param errorAttributes ErrorAttributes
     */
    public RuntimeErrorController(ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    protected HttpStatus getStatus(HttpServletRequest request) {
        var statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (statusCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        try {
            return HttpStatus.valueOf(statusCode);
        } catch (Exception ex) {
            LOGGER.error("statusCode {} Not Valid", statusCode, ex);
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    /**
     * Handle errors that aren't handled to GlobalControllerExceptionHandler.
     *
     * @param request HttpServletRequest
     * @return ResponseEntity
     */
    @SuppressWarnings("squid:S3752")
    @RequestMapping(value = "${server.error.path}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TypedSimpleResponse<SimpleResponse>> handleError(HttpServletRequest request) {
        var map = this.errorAttributes.getErrorAttributes(new ServletWebRequest(request),
                ErrorAttributeOptions.of(Include.MESSAGE, Include.EXCEPTION, Include.BINDING_ERRORS));

        var sb = new StringBuilder();
        final var exception = map.get("exception");
        if (exception != null) {
            sb.append(exception).append(" ");
        }
        final var error = map.get("error");
        if (error != null) {
            sb.append(error).append(" ");
        }
        final var message = map.get("message");
        if (message != null) {
            sb.append(message);
        }

        TypedSimpleResponse<SimpleResponse> resp = new TypedSimpleResponse<>();
        resp.setErrorDetails(sb.toString());

        return ResponseEntity.status(getStatus(request)).body(resp);
    }
}
