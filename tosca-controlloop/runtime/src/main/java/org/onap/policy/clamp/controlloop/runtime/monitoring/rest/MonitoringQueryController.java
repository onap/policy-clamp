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

package org.onap.policy.clamp.controlloop.runtime.monitoring.rest;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.Extension;
import io.swagger.annotations.ExtensionProperty;
import io.swagger.annotations.ResponseHeader;
import java.time.Instant;
import java.util.UUID;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ClElementStatisticsList;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantStatisticsList;
import org.onap.policy.clamp.controlloop.runtime.main.rest.RestController;
import org.onap.policy.clamp.controlloop.runtime.monitoring.MonitoringHandler;
import org.onap.policy.clamp.controlloop.runtime.monitoring.MonitoringProvider;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles REST endpoints for CL Statistics monitoring.
 */
public class MonitoringQueryController extends RestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitoringQueryController.class);
    private final MonitoringProvider provider;

    /**
     * Create Monitoring Controller.
     */
    public MonitoringQueryController() {
        this.provider = MonitoringHandler.getInstance().getMonitoringProvider();
    }


    /**
     * Queries details of control loop participants statistics.
     *
     * @param requestId request ID used in ONAP logging
     * @param name the name of the participant to get, null for all participants statistics
     * @param recordCount the record count to be fetched
     * @return the participant statistics
     */
    // @formatter:off
    @GET
    @Path("/monitoring/participant")
    @ApiOperation(value = "Query details of the requested participant stats",
        notes = "Queries details of the requested participant stats, returning all participant stats",
        response = ParticipantStatisticsList.class,
        tags = {
            "Clamp control loop Monitoring API"
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
    public Response queryParticipantStatistics(@HeaderParam(REQUEST_ID_NAME)
                                               @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
                                               @ApiParam(value = "Control Loop participant name", required = true)
                                               @QueryParam("name") final String name,
                                               @ApiParam(value = "Control Loop participant version", required = true)
                                               @QueryParam("version") final String version,
                                               @ApiParam(value = "Record count", required = false) @DefaultValue("0")
                                               @QueryParam("recordCount") final int recordCount,
                                               @ApiParam(value = "start time", required = false)
                                               @QueryParam("startTime") final String startTime,
                                               @ApiParam(value = "end time", required = false)
                                               @QueryParam("endTime") final String endTime) {

        try {
            Instant startTimestamp = null;
            Instant endTimestamp = null;

            if (startTime != null) {
                startTimestamp = Instant.parse(startTime);
            }
            if (endTime != null) {
                endTimestamp = Instant.parse(endTime);
            }
            ParticipantStatisticsList response = provider.fetchFilteredParticipantStatistics(name, version, recordCount,
                startTimestamp, endTimestamp);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                .entity(response)
                .build();

        } catch (PfModelRuntimeException e) {
            LOGGER.warn("Monitoring of participants statistics failed", e);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(e.getErrorResponse().getResponseCode())),
                requestId).build();
        }

    }

    /**
     * Queries details of all participant statistics per control loop.
     *
     * @param requestId request ID used in ONAP logging
     * @param name the name of the control loop
     * @param version version of the control loop
     * @return the control loop element statistics
     */
    // @formatter:off
    @GET
    @Path("/monitoring/participants/controlloop")
    @ApiOperation(value = "Query details of all the participant stats in a control loop",
        notes = "Queries details of the participant stats, returning all participant stats",
        response = ClElementStatisticsList.class,
        tags = {
            "Clamp control loop Monitoring API"
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
        })
    @ApiResponses(
        value = {
            @ApiResponse(code = AUTHENTICATION_ERROR_CODE, message = AUTHENTICATION_ERROR_MESSAGE),
            @ApiResponse(code = AUTHORIZATION_ERROR_CODE, message = AUTHORIZATION_ERROR_MESSAGE),
            @ApiResponse(code = SERVER_ERROR_CODE, message = SERVER_ERROR_MESSAGE)
        }
    )
    // @formatter:on
    public Response queryParticipantStatisticsPerControlLoop(@HeaderParam(REQUEST_ID_NAME)
                                                             @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
                                                             @ApiParam(value = "Control Loop name", required = true)
                                                             @QueryParam("name") final String name,
                                                             @ApiParam(value = "Control Loop version", required = true)
                                                             @QueryParam("version") final String version) {

        try {
            ParticipantStatisticsList response = provider.fetchParticipantStatsPerControlLoop(name, version);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                .entity(response)
                .build();

        } catch (PfModelRuntimeException | PfModelException e) {
            LOGGER.warn("Monitoring of Cl participant statistics failed", e);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(e.getErrorResponse().getResponseCode())),
                requestId).build();
        }

    }



    /**
     * Queries details of all control loop element statistics per control loop.
     *
     * @param requestId request ID used in ONAP logging
     * @param name the name of the control loop
     * @param version version of the control loop
     * @return the control loop element statistics
     */
    // @formatter:off
    @GET
    @Path("/monitoring/clelements/controlloop")
    @ApiOperation(value = "Query details of the requested cl element stats in a control loop",
        notes = "Queries details of the requested cl element stats, returning all clElement stats",
        response = ClElementStatisticsList.class,
        tags = {
            "Clamp control loop Monitoring API"
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
        })
    @ApiResponses(
        value = {
            @ApiResponse(code = AUTHENTICATION_ERROR_CODE, message = AUTHENTICATION_ERROR_MESSAGE),
            @ApiResponse(code = AUTHORIZATION_ERROR_CODE, message = AUTHORIZATION_ERROR_MESSAGE),
            @ApiResponse(code = SERVER_ERROR_CODE, message = SERVER_ERROR_MESSAGE)
        }
    )
    // @formatter:on
    public Response queryElementStatisticsPerControlLoop(@HeaderParam(REQUEST_ID_NAME)
                                                         @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
                                                         @ApiParam(value = "Control Loop name", required = true)
                                                         @QueryParam("name") final String name,
                                                         @ApiParam(value = "Control Loop version", required = true)
                                                         @QueryParam("version") final String version) {

        try {
            ClElementStatisticsList response = provider.fetchClElementStatsPerControlLoop(name, version);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                .entity(response)
                .build();

        } catch (PfModelRuntimeException | PfModelException e) {
            LOGGER.warn("Monitoring of Cl Element statistics failed", e);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(e.getErrorResponse().getResponseCode())),
                requestId).build();
        }

    }




    /**
     * Queries details of all control loop element statistics per control loop.
     *
     * @param requestId request ID used in ONAP logging
     * @param name the name of the control loop
     * @param version version of the control loop
     * @param id Id of the control loop element
     * @param recordCount the record count to be fetched
     * @return the control loop element statistics
     */
    // @formatter:off
    @GET
    @Path("/monitoring/clelement")
    @ApiOperation(value = "Query details of the requested cl element stats",
        notes = "Queries details of the requested cl element stats, returning all clElement stats",
        response = ClElementStatisticsList.class,
        tags = {
            "Clamp control loop Monitoring API"
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
        })
    @ApiResponses(
        value = {
            @ApiResponse(code = AUTHENTICATION_ERROR_CODE, message = AUTHENTICATION_ERROR_MESSAGE),
            @ApiResponse(code = AUTHORIZATION_ERROR_CODE, message = AUTHORIZATION_ERROR_MESSAGE),
            @ApiResponse(code = SERVER_ERROR_CODE, message = SERVER_ERROR_MESSAGE)
        }
    )
    // @formatter:on
    public Response queryElementStatistics(@HeaderParam(REQUEST_ID_NAME)
                                           @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
                                           @ApiParam(value = "Participant name", required = true)
                                           @QueryParam("name") final String name,
                                           @ApiParam(value = "Participant version", required = true)
                                           @QueryParam("version") final String version,
                                           @ApiParam(value = "Record count", required = false)
                                           @DefaultValue("0") @QueryParam("recordCount") final int recordCount,
                                           @ApiParam(value = "Control Loop element id", required = false)
                                           @QueryParam("id") final String id,
                                           @ApiParam(value = "start time", required = false)
                                           @QueryParam("startTime") final String startTime,
                                           @ApiParam(value = "end time", required = false)
                                           @QueryParam("endTime") final String endTime) {

        try {
            Instant startTimestamp = null;
            Instant endTimestamp = null;

            if (startTime != null) {
                startTimestamp = Instant.parse(startTime);
            }
            if (endTime != null) {
                endTimestamp = Instant.parse(endTime);
            }
            ClElementStatisticsList response = provider.fetchFilteredClElementStatistics(name, version, id,
                startTimestamp, endTimestamp, recordCount);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                .entity(response)
                .build();

        } catch (PfModelRuntimeException | PfModelException e) {
            LOGGER.warn("Monitoring of Cl Element statistics failed", e);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(e.getErrorResponse().getResponseCode())),
                requestId).build();
        }

    }

}