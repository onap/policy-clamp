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

package org.onap.policy.clamp.controlloop.participant.simulator.simulation.rest;

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
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopException;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.Participant;
import org.onap.policy.clamp.controlloop.models.messages.rest.SimpleResponse;
import org.onap.policy.clamp.controlloop.models.messages.rest.TypedSimpleResponse;
import org.onap.policy.clamp.controlloop.participant.simulator.main.rest.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to provide REST end points for participant simulator to query/update details of all participants.
 */
public class SimulationParticipantController extends RestController {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimulationParticipantController.class);

    /**
     * Queries details of all participants within the simulator.
     *
     * @param requestId request ID used in ONAP logging
     * @param name the name of the participant to get, null to get all
     * @param version the version of the participant to get, null to get all
     * @return the participants
     */
    // @formatter:off
    @GET
    @Path("/participants/{name}/{version}")
    @ApiOperation(value = "Query details of the requested simulated participants",
            notes = "Queries details of the requested simulated participants, "
                    + "returning all participant details",
            response = List.class,
            tags = {
                "Clamp Control Loop Participant Simulator API"
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
    public Response participants(@HeaderParam(REQUEST_ID_NAME) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
            @ApiParam(value = "Participant name", required = true) @PathParam("name") String name,
            @ApiParam(value = "Participant version", required = true) @PathParam("version") String version) {

        try {
            List<Participant> response = getSimulationProvider().getParticipants(name, version);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Status.OK)), requestId).entity(response)
                    .build();

        } catch (ControlLoopException cle) {
            LOGGER.warn("get of participants failed", cle);
            SimpleResponse resp = new SimpleResponse();
            resp.setErrorDetails(cle.getErrorResponse().getErrorMessage());
            return addLoggingHeaders(
                    addVersionControlHeaders(Response.status(cle.getErrorResponse().getResponseCode())), requestId)
                            .entity(resp).build();
        }

    }

    /**
     * Updates a participant in the simulator.
     *
     * @param requestId request ID used in ONAP logging
     * @param body the body of a participant
     * @return a response
     */
    // @formatter:off
    @PUT
    @Path("/participants")
    @ApiOperation(
            value = "Updates simulated participants",
            notes = "Updates simulated participants, returning the updated control loop definition IDs",
            response = TypedSimpleResponse.class,
            tags = {
                "Clamp Control Loop Participant Simulator API"
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
    public Response update(@HeaderParam(REQUEST_ID_NAME) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
            @ApiParam(value = "Body of a participant", required = true) Participant body) {

        try {
            TypedSimpleResponse<Participant> response = getSimulationProvider().updateParticipant(body);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Status.OK)), requestId).entity(response)
                    .build();

        } catch (ControlLoopException cle) {
            LOGGER.warn("update of participant failed", cle);
            TypedSimpleResponse<Participant> resp = new TypedSimpleResponse<>();
            resp.setErrorDetails(cle.getErrorResponse().getErrorMessage());
            return addLoggingHeaders(
                    addVersionControlHeaders(Response.status(cle.getErrorResponse().getResponseCode())), requestId)
                            .entity(resp).build();
        }
    }
}
