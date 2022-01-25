/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
 * ================================================================================
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

package org.onap.policy.clamp.acm.participant.simulator.main.rest;

import java.util.Map;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import org.onap.policy.clamp.models.acm.messages.rest.SimpleResponse;
import org.onap.policy.clamp.models.acm.messages.rest.TypedSimpleResponse;
import org.springframework.beans.factory.annotation.Value;
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
public class ParticipantErrorController implements ErrorController {

    private final ErrorAttributes errorAttributes;

    @Value("${server.error.path}")
    private String path;

    /**
     * Constructor.
     *
     * @param errorAttributes ErrorAttributes
     */
    public ParticipantErrorController(ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    protected HttpStatus getStatus(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (statusCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        try {
            return HttpStatus.valueOf(statusCode);
        } catch (Exception ex) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    /**
     * Handle Errors not handled to GlobalControllerExceptionHandler.
     *
     * @param request HttpServletRequest
     * @return ResponseEntity
     */
    @RequestMapping(value = "${server.error.path}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TypedSimpleResponse<SimpleResponse>> handleError(HttpServletRequest request) {
        Map<String, Object> map = this.errorAttributes.getErrorAttributes(new ServletWebRequest(request),
                ErrorAttributeOptions.defaults());

        var sb = new StringBuilder();
        final Object error = map.get("error");
        if (error != null) {
            sb.append(error.toString() + " ");
        }
        final Object message = map.get("message");
        if (message != null) {
            sb.append(message.toString());
        }

        TypedSimpleResponse<SimpleResponse> resp = new TypedSimpleResponse<>();
        resp.setErrorDetails(sb.toString());

        return ResponseEntity.status(getStatus(request)).body(resp);

    }
}
