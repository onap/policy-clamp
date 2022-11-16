/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Nordix Foundation.
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

package org.onap.policy.clamp.acm.element.main.rest;

import javax.ws.rs.core.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Common superclass to provide REST endpoints for the AC element.
 */
// @formatter:off
@RequestMapping(
        value = "/onap/policy/clamp/acelement/v2",
        produces = {
            MediaType.APPLICATION_JSON,
            AbstractRestController.APPLICATION_YAML
        }
)
// @formatter:on
public abstract class AbstractRestController {
    public static final String APPLICATION_YAML = "application/yaml";

    public static final String API_VERSION_NAME = "api-version";

    public static final String VERSION_MINOR_NAME = "X-MinorVersion";
    public static final String VERSION_MINOR_DESCRIPTION =
            "Used to request or communicate a MINOR version back from the client"
                    + " to the server, and from the server back to the client";

    public static final String VERSION_PATCH_NAME = "X-PatchVersion";
    public static final String VERSION_PATCH_DESCRIPTION = "Used only to communicate a PATCH version in a response for"
            + " troubleshooting purposes only, and will not be provided by" + " the client on request";

    public static final String VERSION_LATEST_NAME = "X-LatestVersion";
    public static final String VERSION_LATEST_DESCRIPTION = "Used only to communicate an API's latest version";

    public static final String REQUEST_ID_NAME = "X-ONAP-RequestID";
    public static final String REQUEST_ID_HDR_DESCRIPTION = "Used to track REST transactions for logging purpose";

    public static final String OK_CODE = "200";
    public static final String CREATED_CODE = "201";
    public static final String NO_CONTENT_CODE = "204";
    public static final String AUTHENTICATION_ERROR_CODE = "401";
    public static final String BAD_REQUEST_ERROR_CODE = "400";

    public static final String AUTHENTICATION_ERROR_MESSAGE = "Authentication Error";
    public static final String BAD_REQUEST_ERROR_MESSAGE = "Bad request";
    public static final String SERVER_OK_MESSAGE = "Success";
}