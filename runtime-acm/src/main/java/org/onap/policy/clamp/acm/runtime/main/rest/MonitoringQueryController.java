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

package org.onap.policy.clamp.acm.runtime.main.rest;

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
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.runtime.main.web.AbstractRestController;
import org.onap.policy.clamp.acm.runtime.monitoring.MonitoringProvider;
import org.onap.policy.clamp.models.acm.concepts.AcElementStatisticsList;
import org.onap.policy.clamp.models.acm.concepts.ParticipantStatisticsList;
import org.onap.policy.models.base.PfModelException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * This class handles REST endpoints for ACM Statistics monitoring.
 */
@RestController
@RequiredArgsConstructor
public class MonitoringQueryController extends AbstractRestController {

    private static final String TAGS = "Clamp Automation Composition Monitoring API";
    private final MonitoringProvider provider;

    /**
     * Queries details of automation composition participants statistics.
     *
     * @param requestId request ID used in ONAP logging
     * @param name the name of the participant to get, null for all participants statistics
     * @param version the version of the participant to get, null for all participants with the given name
     * @param recordCount the record count to be fetched
     * @param startTime the time from which to get statistics
     * @param endTime the time to which to get statistics
     * @return the participant statistics
     */
    // @formatter:off
    @GetMapping(value = "/monitoring/participant",
            produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML})
    @ApiOperation(value = "Query details of the requested participant stats",
        notes = "Queries details of the requested participant stats, returning all participant stats",
        response = ParticipantStatisticsList.class,
        tags = {TAGS},
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
    public ResponseEntity<ParticipantStatisticsList> queryParticipantStatistics(
        @RequestHeader(name = REQUEST_ID_NAME, required = false) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
        @ApiParam(value = "Automation composition  participant name") @RequestParam(
            value = "name",
            required = false) final String name,
        @ApiParam(value = "Automation composition  participant version", required = false) @RequestParam(
            value = "version",
            required = false) final String version,
        @ApiParam(value = "Record count", required = false) @RequestParam(
            value = "recordCount",
            required = false,
            defaultValue = "0") final int recordCount,
        @ApiParam(value = "start time", required = false) @RequestParam(
            value = "startTime",
            required = false) final String startTime,
        @ApiParam(value = "end time", required = false) @RequestParam(
            value = "endTime",
            required = false) final String endTime) {

        Instant startTimestamp = null;
        Instant endTimestamp = null;

        if (startTime != null) {
            startTimestamp = Instant.parse(startTime);
        }
        if (endTime != null) {
            endTimestamp = Instant.parse(endTime);
        }
        return ResponseEntity.ok().body(
            provider.fetchFilteredParticipantStatistics(name, version, recordCount, startTimestamp, endTimestamp));
    }

    /**
     * Queries details of all participant statistics per automation composition.
     *
     * @param requestId request ID used in ONAP logging
     * @param name the name of the automation composition
     * @param version version of the automation composition
     * @return the automation composition element statistics
     */
    // @formatter:off
    @GetMapping(value = "/monitoring/participants/automationcomposition",
            produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML})
    @ApiOperation(value = "Query details of all the participant stats in a automation composition",
        notes = "Queries details of the participant stats, returning all participant stats",
        response = AcElementStatisticsList.class,
        tags = {TAGS},
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
        })
    @ApiResponses(
        value = {
            @ApiResponse(code = AUTHENTICATION_ERROR_CODE, message = AUTHENTICATION_ERROR_MESSAGE),
            @ApiResponse(code = AUTHORIZATION_ERROR_CODE, message = AUTHORIZATION_ERROR_MESSAGE),
            @ApiResponse(code = SERVER_ERROR_CODE, message = SERVER_ERROR_MESSAGE)
        }
    )
    // @formatter:on
    public ResponseEntity<ParticipantStatisticsList> queryParticipantStatisticsPerAutomationComposition(
        @RequestHeader(name = REQUEST_ID_NAME, required = false) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
        @ApiParam(value = "Automation composition  name", required = true) @RequestParam(
            value = "name",
            required = false) final String name,
        @ApiParam(value = "Automation composition  version", required = true) @RequestParam(
            value = "version",
            required = false) final String version) {

        return ResponseEntity.ok().body(provider.fetchParticipantStatsPerAutomationComposition(name, version));
    }

    /**
     * Queries details of all automation composition element statistics per automation composition.
     *
     * @param requestId request ID used in ONAP logging
     * @param name the name of the automation composition
     * @param version version of the automation composition
     * @return the automation composition element statistics
     */
    // @formatter:off
    @GetMapping(value = "/monitoring/acelements/automationcomposition",
            produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML})
    @ApiOperation(value = "Query details of the requested acElement stats in a automation composition",
        notes = "Queries details of the requested acElement stats, returning all acElement stats",
        response = AcElementStatisticsList.class,
        tags = {TAGS},
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
        })
    @ApiResponses(
        value = {
            @ApiResponse(code = AUTHENTICATION_ERROR_CODE, message = AUTHENTICATION_ERROR_MESSAGE),
            @ApiResponse(code = AUTHORIZATION_ERROR_CODE, message = AUTHORIZATION_ERROR_MESSAGE),
            @ApiResponse(code = SERVER_ERROR_CODE, message = SERVER_ERROR_MESSAGE)
        }
    )
    // @formatter:on
    public ResponseEntity<AcElementStatisticsList> queryElementStatisticsPerAutomationComposition(
        @RequestHeader(name = REQUEST_ID_NAME, required = false) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
        @ApiParam(value = "Automation composition  name", required = true) @RequestParam(
            value = "name",
            required = false) final String name,
        @ApiParam(value = "Automation composition  version", required = true) @RequestParam(
            value = "version",
            required = false) final String version) {

        return ResponseEntity.ok().body(provider.fetchAcElementStatsPerAutomationComposition(name, version));
    }

    /**
     * Queries details of all automation composition element statistics per automation composition.
     *
     * @param requestId request ID used in ONAP logging
     * @param name the name of the automation composition
     * @param version version of the automation composition
     * @param id Id of the automation composition element
     * @param recordCount the record count to be fetched
     * @param startTime the time from which to get statistics
     * @param endTime the time to which to get statistics
     * @return the automation composition element statistics
     * @throws PfModelException on errors getting details of all automation composition element statistics per
     *         automation composition
     */
    // @formatter:off
    @GetMapping(value = "/monitoring/acelement",
            produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML})
    @ApiOperation(value = "Query details of the requested acElement stats",
        notes = "Queries details of the requested acElement stats, returning all acElement stats",
        response = AcElementStatisticsList.class,
        tags = {TAGS},
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
        })
    @ApiResponses(
        value = {
            @ApiResponse(code = AUTHENTICATION_ERROR_CODE, message = AUTHENTICATION_ERROR_MESSAGE),
            @ApiResponse(code = AUTHORIZATION_ERROR_CODE, message = AUTHORIZATION_ERROR_MESSAGE),
            @ApiResponse(code = SERVER_ERROR_CODE, message = SERVER_ERROR_MESSAGE)
        }
    )
    // @formatter:on
    public ResponseEntity<AcElementStatisticsList> queryElementStatistics(
        @RequestHeader(name = REQUEST_ID_NAME, required = false) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
        @ApiParam(value = "Participant name", required = true) @RequestParam(
            value = "name",
            required = false) final String name,
        @ApiParam(value = "Participant version", required = true) @RequestParam(
            value = "version",
            required = false) final String version,
        @ApiParam(value = "Record count", required = false) @RequestParam(
            value = "recordCount",
            required = false,
            defaultValue = "0") final int recordCount,
        @ApiParam(value = "Automation composition  element id", required = false) @RequestParam(
            value = "id",
            required = false) final String id,
        @ApiParam(value = "start time", required = false) @RequestParam(
            value = "startTime",
            required = false) final String startTime,
        @ApiParam(value = "end time", required = false) @RequestParam(
            value = "endTime",
            required = false) final String endTime)
        throws PfModelException {

        Instant startTimestamp = null;
        Instant endTimestamp = null;

        if (startTime != null) {
            startTimestamp = Instant.parse(startTime);
        }
        if (endTime != null) {
            endTimestamp = Instant.parse(endTime);
        }
        return ResponseEntity.ok().body(
            provider.fetchFilteredAcElementStatistics(name, version, id, startTimestamp, endTimestamp, recordCount));
    }

}
