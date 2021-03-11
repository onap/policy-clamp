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

package org.onap.policy.clamp.controlloop.runtime.commissioning.rest;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.Extension;
import io.swagger.annotations.ExtensionProperty;
import io.swagger.annotations.ResponseHeader;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.onap.policy.clamp.controlloop.models.messages.rest.commissioning.CommissioningResponse;
import org.onap.policy.clamp.controlloop.runtime.commissioning.CommissioningHandler;
import org.onap.policy.clamp.controlloop.runtime.commissioning.CommissioningProvider;
import org.onap.policy.clamp.controlloop.runtime.main.rest.RestController;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.errors.concepts.ErrorResponseInfo;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to provide REST end points for creating, deleting, querying commissioned control loops.
 */
public class CommissioningController extends RestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommissioningController.class);

    private final CommissioningProvider provider;

    /**
     * create Commissioning Controller.
     */
    public CommissioningController() {
        this.provider = CommissioningHandler.getInstance().getProvider();
    }

    /**
     * Creates a control loop definition.
     *
     * @param requestId request ID used in ONAP logging
     * @param body the body of control loop following TOSCA definition
     * @return a response
     */
    // @formatter:off
    @POST
    @Path("/commission")
    @ApiOperation(
            value = "Commissions control loop definitions",
            notes = "Commissions control loop definitions, returning the commissioned control loop definition IDs",
            response = CommissioningResponse.class,
            tags = {
                    "Control Loop Commissioning API"
            },
            authorizations = @Authorization(value = AUTHORIZATION_TYPE),
            responseHeaders = {
                    @ResponseHeader(
                            name = VERSION_MINOR_NAME,
                            description = VERSION_MINOR_DESCRIPTION,
                            response = String.class),
                    @ResponseHeader(
                            name = VERSION_PATCH_NAME,
                            description = VERSION_PATCH_DESCRIPTION,
                            response = String.class),
                    @ResponseHeader(
                            name = VERSION_LATEST_NAME,
                            description = VERSION_LATEST_DESCRIPTION,
                            response = String.class),
                    @ResponseHeader(
                            name = REQUEST_ID_NAME,
                            description = REQUEST_ID_HDR_DESCRIPTION,
                            response = UUID.class)
            },
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
    public Response create(
            @HeaderParam(REQUEST_ID_NAME) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
            @ApiParam(value = "Entity Body of Control Loop", required = true) ToscaServiceTemplate body) {

        try {
            CommissioningResponse response = provider.createControlLoopDefinitions(body);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Status.OK)), requestId)
                    .entity(response).build();

        } catch (PfModelRuntimeException | PfModelException e) {
            LOGGER.warn("Commissioning of the control loops failed", e);
            return createCommissioningErrorResponse(e, requestId);
        }

    }

    /**
     * Deletes a control loop definition.
     *
     * @param requestId request ID used in ONAP logging
     * @param name the name of the control loop definition to delete
     * @param version the version of the control loop definition to delete
     * @return a response
     */
    // @formatter:off
    @DELETE
    @Path("/commission")
    @ApiOperation(value = "Delete a commissioned control loop",
            notes = "Deletes a Commissioned Control Loop, returning optional error details",
            response = CommissioningResponse.class,
            tags = {
                    "Clamp Control Loop Commissioning API"
            },
            authorizations = @Authorization(value = AUTHORIZATION_TYPE),
            responseHeaders = {
                    @ResponseHeader(
                            name = VERSION_MINOR_NAME,
                            description = VERSION_MINOR_DESCRIPTION,
                            response = String.class),
                    @ResponseHeader(
                            name = VERSION_PATCH_NAME,
                            description = VERSION_PATCH_DESCRIPTION,
                            response = String.class),
                    @ResponseHeader(
                            name = VERSION_LATEST_NAME,
                            description = VERSION_LATEST_DESCRIPTION,
                            response = String.class),
                    @ResponseHeader(
                            name = REQUEST_ID_NAME,
                            description = REQUEST_ID_HDR_DESCRIPTION,
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
    @ApiResponses(value = {
            @ApiResponse(code = AUTHENTICATION_ERROR_CODE, message = AUTHENTICATION_ERROR_MESSAGE),
            @ApiResponse(code = AUTHORIZATION_ERROR_CODE, message = AUTHORIZATION_ERROR_MESSAGE),
            @ApiResponse(code = SERVER_ERROR_CODE, message = SERVER_ERROR_MESSAGE)
        }
    )
    // @formatter:on
    public Response delete(
            @HeaderParam(REQUEST_ID_NAME) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
            @ApiParam(value = "Control Loop definition name", required = true) @QueryParam("name") String name,
            @ApiParam(value = "Control Loop definition version", required = true)
            @QueryParam("version") String version) {

        try {
            CommissioningResponse response = provider.deleteControlLoopDefinition(name, version);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Status.OK)), requestId)
                    .entity(response).build();

        } catch (PfModelRuntimeException | PfModelException e) {
            LOGGER.warn("Decommisssioning of control loop failed", e);
            return createCommissioningErrorResponse(e, requestId);
        }

    }

    /**
     * Queries details of all or specific control loop definitions.
     *
     * @param requestId request ID used in ONAP logging
     * @param name the name of the control loop definition to get, null for all definitions
     * @param version the version of the control loop definition to get, null for all definitions
     * @return the control loop definitions
     */
    // @formatter:off
    @GET
    @Path("/commission")
    @ApiOperation(value = "Query details of the requested commissioned control loop definitions",
            notes = "Queries details of the requested commissioned control loop definitions, "
                    + "returning all control loop details",
            response = ToscaNodeTemplate.class,
            tags = {
                    "Clamp Control Loop Commissioning API"
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
    public Response query(@HeaderParam(REQUEST_ID_NAME) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
                          @ApiParam(value = "Control Loop definition name", required = true)
                              @QueryParam("name") String name,
                          @ApiParam(value = "Control Loop definition version", required = true)
                              @QueryParam("version") String version) {

        try {
            List<ToscaNodeTemplate> response = provider.getControlLoopDefinitions(name, version);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Status.OK)), requestId).entity(response)
                    .build();

        } catch (PfModelRuntimeException | PfModelException e) {
            LOGGER.warn("Get of control loop definitions failed", e);
            return createCommissioningErrorResponse(e, requestId);
        }

    }

    /**
     * Queries the elements of a specific control loop.
     *
     * @param requestId request ID used in ONAP logging
     * @param name the name of the control loop definition to get
     * @param version the version of the control loop definition to get
     * @return the control loop element definitions
     */
    // @formatter:off
    @GET
    @Path("/commission/elements")
    @ApiOperation(value = "Query details of the requested commissioned control loop element definitions",
            notes = "Queries details of the requested commissioned control loop element definitions, "
                    + "returning all control loop elements' details",
            response = ToscaNodeTemplate.class,
            tags = {
                    "Clamp Control Loop Commissioning API"
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
    public Response queryElements(@HeaderParam(REQUEST_ID_NAME) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
                          @ApiParam(value = "Control Loop definition name", required = true)
                          @QueryParam("name") String name,
                          @ApiParam(value = "Control Loop definition version", required = true)
                          @QueryParam("version") String version) throws Exception {

        try {
            List<ToscaNodeTemplate> nodeTemplate = provider.getControlLoopDefinitions(name, version);
            //Prevent ambiguous queries with multiple returns
            if (nodeTemplate.size() > 1) {
                throw new Exception();
            }
            List<ToscaNodeTemplate> response = provider.getControlLoopElementDefinitions(nodeTemplate.get(0));
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Status.OK)), requestId).entity(response)
                    .build();

        } catch (PfModelRuntimeException | PfModelException e) {
            LOGGER.warn("Get of control loop element definitions failed", e);
            return createCommissioningErrorResponse(e, requestId);
        }

    }

    private Response createCommissioningErrorResponse(ErrorResponseInfo e, UUID requestId) {
        CommissioningResponse resp = new CommissioningResponse();
        resp.setErrorDetails(e.getErrorResponse().getErrorMessage());
        return addLoggingHeaders(addVersionControlHeaders(Response.status(e.getErrorResponse().getResponseCode())),
                requestId).entity(resp).build();
    }

}
