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

package org.onap.policy.clamp.acm.participant.simulator.simulation.rest;

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
import org.onap.policy.clamp.acm.participant.simulator.main.rest.AbstractRestController;
import org.onap.policy.clamp.acm.participant.simulator.simulation.SimulationProvider;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.messages.rest.TypedSimpleResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Class to provide REST end points for participant simulator to query/update details of all participants.
 */
@RestController
public class SimulationParticipantController extends AbstractRestController {

    /**
     * Constructor.
     *
     * @param simulationProvider the Simulation Provider
     */
    public SimulationParticipantController(SimulationProvider simulationProvider) {
        super(simulationProvider);
    }

    /**
     * Queries details of all participants within the simulator.
     *
     * @param requestId request ID used in ONAP logging
     * @param name the name of the participant to get, null to get all
     * @param version the version of the participant to get, null to get all
     * @return the participants
     */
    // @formatter:off
    @GetMapping("/participants/{name}/{version}")
    @ApiOperation(value = "Query details of the requested simulated participants",
            notes = "Queries details of the requested simulated participants, "
                    + "returning all participant details",
            response = List.class,
            tags = {
                "Clamp Automation Composition Participant Simulator API"
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
                @Extension
                    (
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
    public ResponseEntity<List<Participant>> participants(
        @RequestHeader(name = REQUEST_ID_NAME, required = false) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
        @ApiParam(value = "Participant name", required = true) @PathVariable("name") String name,
        @ApiParam(value = "Participant version", required = true) @PathVariable("version") String version) {

        return ResponseEntity
            .ok()
            .headers(super.getCommonHeaders(requestId))
            .body(getSimulationProvider().getParticipants(name, version));
    }

    /**
     * Updates a participant in the simulator.
     *
     * @param requestId request ID used in ONAP logging
     * @param body the body of a participant
     * @return a response
     */
    // @formatter:off
    @PutMapping("/participants")
    @ApiOperation(
            value = "Updates simulated participants",
            notes = "Updates simulated participants, returning the updated automation composition definition IDs",
            response = TypedSimpleResponse.class,
            tags = {
                "Clamp Automation Composition Participant Simulator API"
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
                @Extension
                    (
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
    public ResponseEntity<TypedSimpleResponse<Participant>> update(
        @RequestHeader(name = REQUEST_ID_NAME, required = false) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
        @ApiParam(value = "Body of a participant", required = true) @RequestBody Participant body) {

        return ResponseEntity
            .ok()
            .headers(super.getCommonHeaders(requestId))
            .body(getSimulationProvider().updateParticipant(body));
    }
}
