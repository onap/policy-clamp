/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.policy.clamp.acm.runtime.main.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import jakarta.servlet.RequestDispatcher;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

class RuntimeErrorControllerTest {

    private ErrorAttributes errorAttributes;
    private RuntimeErrorController controller;

    @BeforeEach
    void setup() {
        errorAttributes = Mockito.mock(ErrorAttributes.class);
        controller = new RuntimeErrorController(errorAttributes);
    }

    @Test
    void testGetStatus_validStatus() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);

        HttpStatus result = controller.getStatus(request);
        assertThat(result).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testGetStatus_nullStatus() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        HttpStatus result = controller.getStatus(request);
        assertThat(result).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void testGetStatus_invalidStatus() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 9999); // invalid

        HttpStatus result = controller.getStatus(request);
        assertThat(result).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void testHandleError_fullAttributes() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 400);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("exception", "IllegalArgumentException");
        attributes.put("error", "Bad Request");
        attributes.put("message", "Invalid input");

        when(errorAttributes.getErrorAttributes(any(ServletWebRequest.class), any(ErrorAttributeOptions.class)))
            .thenReturn(attributes);

        var response = controller.handleError(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        assertNotNull(response.getBody());
        String details = response.getBody().getErrorDetails();
        assertThat(details).contains("IllegalArgumentException")
            .contains("Bad Request")
            .contains("Invalid input");
    }

    @Test
    void testHandleError_missingException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("error", "Internal Server Error");
        attributes.put("message", "Something broke");

        when(errorAttributes.getErrorAttributes(any(ServletWebRequest.class), any(ErrorAttributeOptions.class)))
            .thenReturn(attributes);

        var response = controller.handleError(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        assertNotNull(response.getBody());
        String details = response.getBody().getErrorDetails();
        assertThat(details).isEqualTo("Internal Server Error Something broke");
    }

    @Test
    void testHandleError_noFieldsProvided() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500);

        Map<String, Object> attributes = new HashMap<>();

        when(errorAttributes.getErrorAttributes(any(ServletWebRequest.class), any(ErrorAttributeOptions.class)))
            .thenReturn(attributes);

        var response = controller.handleError(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertNotNull(response.getBody());
        assertThat(response.getBody().getErrorDetails()).isEmpty();
    }
}

