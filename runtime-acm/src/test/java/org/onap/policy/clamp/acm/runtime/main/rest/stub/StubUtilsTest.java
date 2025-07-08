/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.runtime.main.rest.stub;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

class StubUtilsTest {

    HttpServletRequest mockRequest;

    StubUtils stubUtil;

    @BeforeEach
    void setUp() {
        mockRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(mockRequest.getHeader("Accept")).thenReturn("application/yaml");
        stubUtil = new StubUtils(mockRequest);
    }

    @Test
    void getResponse() {
        assertDoesNotThrow(() -> stubUtil.getResponse("does/not/exist/path", Object.class));
        var response = stubUtil.getResponse("does/not/exist/path", Object.class);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void getResponseList() {
        assertDoesNotThrow(() -> stubUtil.getResponseList("does/not/exist/path"));
        var response = stubUtil.getResponseList("does/not/exist/path");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}