/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation.
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

package org.onap.policy.clamp.common.acm.rest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RequestResponseLoggingFilterTest {

    @Test
    void initTest() {
        var e = new RequestResponseLoggingFilter();
        var res = Mockito.mock(HttpServletResponse.class);
        var req = Mockito.mock(HttpServletRequest.class);
        var chain = Mockito.mock(FilterChain.class);
        Mockito.when(req
                .getHeader(RequestResponseLoggingFilter.REQUEST_ID_NAME))
                .thenReturn("id");

        assertDoesNotThrow(() -> e.doFilter(req, res, chain));
    }
}
