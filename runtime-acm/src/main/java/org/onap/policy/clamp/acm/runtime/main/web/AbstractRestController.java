/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2022 Nordix Foundation.
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

import io.swagger.annotations.Api;
import io.swagger.annotations.BasicAuthDefinition;
import io.swagger.annotations.Info;
import io.swagger.annotations.SecurityDefinition;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionRuntimeException;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Common superclass to provide REST endpoints for the participant simulator.
 */
// @formatter:off
@RequestMapping(value = "/v2",
    produces = {MediaType.APPLICATION_JSON, AbstractRestController.APPLICATION_YAML})
@Api(value = "Automation Composition Commissioning API")
@SwaggerDefinition(
        info = @Info(description =
                        "Automation Composition Service", version = "v1.0",
                        title = "Automation Composition"),
        consumes = {MediaType.APPLICATION_JSON, AbstractRestController.APPLICATION_YAML},
        produces = {MediaType.APPLICATION_JSON, AbstractRestController.APPLICATION_YAML},
        schemes = {SwaggerDefinition.Scheme.HTTP, SwaggerDefinition.Scheme.HTTPS},
        tags = {@Tag(name = "automationcomposition", description = "Automation Composition Service")},
        securityDefinition = @SecurityDefinition(basicAuthDefinitions = {@BasicAuthDefinition(key = "basicAuth")}))
// @formatter:on
public abstract class AbstractRestController {
    public static final String APPLICATION_YAML = "application/yaml";

    public static final String EXTENSION_NAME = "interface info";

    public static final String API_VERSION_NAME = "api-version";
    public static final String API_VERSION = "1.0.0";

    public static final String LAST_MOD_NAME = "last-mod-release";
    public static final String LAST_MOD_RELEASE = "Istanbul";

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
    public static final String REQUEST_ID_PARAM_DESCRIPTION = "RequestID for http transaction";

    public static final String AUTHORIZATION_TYPE = "basicAuth";

    public static final int AUTHENTICATION_ERROR_CODE = HttpURLConnection.HTTP_UNAUTHORIZED;
    public static final int AUTHORIZATION_ERROR_CODE = HttpURLConnection.HTTP_FORBIDDEN;
    public static final int SERVER_ERROR_CODE = HttpURLConnection.HTTP_INTERNAL_ERROR;

    public static final String AUTHENTICATION_ERROR_MESSAGE = "Authentication Error";
    public static final String AUTHORIZATION_ERROR_MESSAGE = "Authorization Error";
    public static final String SERVER_ERROR_MESSAGE = "Internal Server Error";

    /**
     * Constructor.
     */
    protected AbstractRestController() {
    }

    protected URI createUri(String str) {
        try {
            return new URI(str);
        } catch (URISyntaxException e) {
            throw new AutomationCompositionRuntimeException(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
