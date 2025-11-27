/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.clamp.acm.element.main.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.ServletWebRequest;

class AcElementErrorControllerTest {

    @Mock
    private ErrorAttributes errorAttributes;

    @Mock
    private HttpServletRequest request;

    private AcElementErrorController controller;
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        this.closeable = MockitoAnnotations.openMocks(this);
        controller = new AcElementErrorController(errorAttributes);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void testGetStatus_validStatus() {
        when(request.getAttribute("jakarta.servlet.error.status_code")).thenReturn(404);

        HttpStatus status = controller.getStatus(request);

        assertThat(status).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testGetStatus_nullStatus() {
        when(request.getAttribute("jakarta.servlet.error.status_code")).thenReturn(null);

        HttpStatus status = controller.getStatus(request);

        assertThat(status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void testGetStatus_invalidStatus() {
        when(request.getAttribute("jakarta.servlet.error.status_code")).thenReturn(999);

        HttpStatus status = controller.getStatus(request);

        assertThat(status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void testHandleError_buildsErrorDetails() {
        when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(400);

        Map<String, Object> map = new HashMap<>();
        map.put("exception", "IllegalArgumentException");
        map.put("error", "Bad Request");
        map.put("message", "Invalid payload");

        when(errorAttributes.getErrorAttributes(any(ServletWebRequest.class), any(ErrorAttributeOptions.class)))
            .thenReturn(map);

        ResponseEntity<?> response = controller.handleError(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        var body = (org.onap.policy.clamp.models.acm.messages.rest.TypedSimpleResponse<?>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getErrorDetails()).isEqualTo("IllegalArgumentException Bad Request Invalid payload");
    }

    @Test
    void testHandleError_missingFields() {
        when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(500);

        Map<String, Object> map = new HashMap<>();
        // no exception, no error, only the message
        map.put("message", "Something went wrong");

        when(errorAttributes.getErrorAttributes(any(ServletWebRequest.class), any(ErrorAttributeOptions.class)))
            .thenReturn(map);

        ResponseEntity<?> response = controller.handleError(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        var body = (org.onap.policy.clamp.models.acm.messages.rest.TypedSimpleResponse<?>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getErrorDetails()).isEqualTo("Something went wrong");
    }
}

