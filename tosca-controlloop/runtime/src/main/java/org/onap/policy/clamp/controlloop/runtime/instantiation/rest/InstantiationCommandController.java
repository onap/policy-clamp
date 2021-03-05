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

package org.onap.policy.clamp.controlloop.runtime.instantiation.rest;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.Extension;
import io.swagger.annotations.ExtensionProperty;
import io.swagger.annotations.ResponseHeader;
import java.util.UUID;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopException;
import org.onap.policy.clamp.controlloop.models.messages.rest.instantiation.InstantiationCommand;
import org.onap.policy.clamp.controlloop.models.messages.rest.instantiation.InstantiationResponse;
import org.onap.policy.clamp.controlloop.runtime.instantiation.ControlLoopInstantiationProvider;
import org.onap.policy.clamp.controlloop.runtime.instantiation.InstantiationHandler;
import org.onap.policy.clamp.controlloop.runtime.main.rest.RestController;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to provide REST end points for commanding a control loop definition.
 */
public class InstantiationCommandController extends RestController {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstantiationCommandController.class);

    // The CL provider for instantiation requests
    private final ControlLoopInstantiationProvider provider;

    /**
     * create Instantiation Command Controller.
     */
    public InstantiationCommandController() {
        provider = InstantiationHandler.getInstance().getControlLoopInstantiationProvider();
    }

    /**
     * Issues control loop commands to control loops.
     *
     * @param requestId request ID used in ONAP logging
     * @param command the command to issue to control loops
     * @return the control loop definitions
     */
    // @formatter:off
    @PUT
    @Path("/instantiation/command")
    @ApiOperation(value = "Issue a command to the requested control loops",
            notes = "Issues a command to a control loop, ordering a state change on the control loop",
            response = InstantiationResponse.class,
            tags = {
                "Clamp Control Loop Instantiation API"
            },
            authorizations = @Authorization(value = AUTHORIZATION_TYPE),
            responseHeaders = {
                    @ResponseHeader(
                            name = VERSION_MINOR_NAME, description = VERSION_MINOR_DESCRIPTION,
                                    response = String.class),
                    @ResponseHeader(name = VERSION_PATCH_NAME, description = VERSION_PATCH_DESCRIPTION,
                                    response = String.class),
                    @ResponseHeader(name = VERSION_LATEST_NAME, description = VERSION_LATEST_DESCRIPTION,
                                    response = String.class),
                    @ResponseHeader(name = REQUEST_ID_NAME, description = REQUEST_ID_HDR_DESCRIPTION,
                                    response = UUID.class)},
            extensions = {
                    @Extension(
                            name = EXTENSION_NAME,
                            properties = {
                                    @ExtensionProperty(name = API_VERSION_NAME, value = API_VERSION),
                                    @ExtensionProperty(name = LAST_MOD_NAME, value = LAST_MOD_RELEASE)
                        }
                    )
                }
        )
    @ApiResponses(
            value = {
                @ApiResponse(code = AUTHENTICATION_ERROR_CODE, message = AUTHENTICATION_ERROR_MESSAGE),
                @ApiResponse(code = AUTHORIZATION_ERROR_CODE, message = AUTHORIZATION_ERROR_MESSAGE),
                @ApiResponse(code = SERVER_ERROR_CODE, message = SERVER_ERROR_MESSAGE)
            }
        )
    // @formatter:on
    public Response issueControlLoopCommand(
            @HeaderParam(REQUEST_ID_NAME) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
            @ApiParam(value = "Entity Body of control loop command", required = true) InstantiationCommand command) {

        try {
            InstantiationResponse response = provider.issueControlLoopCommand(command);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Status.ACCEPTED)), requestId)
                    .entity(response).build();

        } catch (PfModelRuntimeException | PfModelException | ControlLoopException e) {
            LOGGER.warn("creation of control loop failed", e);
            InstantiationResponse resp = new InstantiationResponse();
            resp.setErrorDetails(e.getErrorResponse().getErrorMessage());
            return addLoggingHeaders(addVersionControlHeaders(Response.status(e.getErrorResponse().getResponseCode())),
                    requestId).entity(resp).build();
        }
    }
}
