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

package org.onap.policy.clamp.common.acm.rest;

import java.io.IOException;
import java.util.UUID;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class RequestResponseLoggingFilter implements Filter {

    private static final String VERSION_MINOR_NAME = "X-MinorVersion";
    private static final String VERSION_PATCH_NAME = "X-PatchVersion";
    private static final String VERSION_LATEST_NAME = "X-LatestVersion";
    public static final String API_VERSION = "1.0.0";
    public static final String REQUEST_ID_NAME = "X-ONAP-RequestID";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {


        HttpServletResponse res = (HttpServletResponse) response;
        HttpServletRequest req = (HttpServletRequest) request;

        /*
         * Disabling sonar because of ONAP requires the request ID to be copied from the request
         * to the response.
         */
        String requestId = req.getHeader(REQUEST_ID_NAME);
        res.addHeader(REQUEST_ID_NAME, requestId != null ? requestId : UUID.randomUUID().toString()); // NOSONAR

        res.addHeader(VERSION_MINOR_NAME, "0");
        res.addHeader(VERSION_PATCH_NAME, "0");
        res.addHeader(VERSION_LATEST_NAME, API_VERSION);

        chain.doFilter(request, response);
    }

}
