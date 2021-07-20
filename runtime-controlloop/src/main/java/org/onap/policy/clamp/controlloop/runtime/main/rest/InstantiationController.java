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

package org.onap.policy.clamp.controlloop.runtime.main.rest;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.Extension;
import io.swagger.annotations.ExtensionProperty;
import io.swagger.annotations.ResponseHeader;
import java.util.UUID;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopException;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoops;
import org.onap.policy.clamp.controlloop.models.messages.rest.instantiation.InstantiationCommand;
import org.onap.policy.clamp.controlloop.models.messages.rest.instantiation.InstantiationResponse;
import org.onap.policy.clamp.controlloop.runtime.instantiation.ControlLoopInstantiationProvider;
import org.onap.policy.clamp.controlloop.runtime.main.web.AbstractRestController;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.errors.concepts.ErrorResponseInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Class to provide REST end points for creating, deleting, query and commanding a control loop definition.
 */
@RestController
public class InstantiationController extends AbstractRestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstantiationController.class);

    private static final String TAGS = "Clamp Control Loop Instantiation API";

    // The CL provider for instantiation requests
    private final ControlLoopInstantiationProvider provider;

    /**
     * Create Instantiation Controller.
     *
     * @param provider the ControlLoopInstantiationProvider
     */
    public InstantiationController(ControlLoopInstantiationProvider provider) {
        this.provider = provider;
    }

    /**
     * Creates a control loop.
     *
     * @param requestId request ID used in ONAP logging
     * @param controlLoops the control loops
     * @return a response
     */
    // @formatter:off
    @PostMapping(value = "/instantiation",
            produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML},
            consumes = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML})
    @ApiOperation(
            value = "Commissions control loop definitions",
            notes = "Commissions control loop definitions, returning the control loop IDs",
            response = InstantiationResponse.class,
            tags = {TAGS},
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
    public ResponseEntity<InstantiationResponse> create(
            @RequestHeader(name = REQUEST_ID_NAME, required = false)
            @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
            @ApiParam(value = "Entity Body of Control Loop", required = true)
            @RequestBody ControlLoops controlLoops) {

        try {
            return ResponseEntity.ok().body(provider.createControlLoops(controlLoops));

        } catch (PfModelRuntimeException | PfModelException e) {
            LOGGER.warn("creation of control loop failed", e);
            return createInstantiationErrorResponse(e);
        }
    }

    /**
     * Queries details of all control loops.
     *
     * @param requestId request ID used in ONAP logging
     * @param name the name of the control loop to get, null for all control loops
     * @param version the version of the control loop to get, null for all control loops
     * @return the control loops
     */
    // @formatter:off
    @GetMapping(value = "/instantiation",
            produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML})
    @ApiOperation(value = "Query details of the requested control loops",
            notes = "Queries details of the requested control loops, returning all control loop details",
            response = ControlLoops.class,
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
    public ResponseEntity<?> query(
            @RequestHeader(name = REQUEST_ID_NAME, required = false)
            @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
            @ApiParam(value = "Control Loop definition name", required = false)
            @RequestParam(value = "name", required = false) String name,
            @ApiParam(value = "Control Loop definition version", required = false)
            @RequestParam(value = "version", required = false) String version) {

        try {
            return ResponseEntity.ok().body(provider.getControlLoops(name, version));

        } catch (PfModelRuntimeException | PfModelException e) {
            LOGGER.warn("commisssioning of control loop failed", e);
            return createInstantiationErrorResponse(e);
        }

    }

    /**
     * Updates a control loop.
     *
     * @param requestId request ID used in ONAP logging
     * @param controlLoops the control loops
     * @return a response
     */
    // @formatter:off
    @PutMapping(value = "/instantiation",
            produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML},
            consumes = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML})
    @ApiOperation(
            value = "Updates control loop definitions",
            notes = "Updates control loop definitions, returning the updated control loop definition IDs",
            response = InstantiationResponse.class,
            tags = {TAGS},
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
    public ResponseEntity<InstantiationResponse> update(
            @RequestHeader(name = REQUEST_ID_NAME, required = false)
            @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
            @ApiParam(value = "Entity Body of Control Loop", required = true)
            @RequestBody ControlLoops controlLoops) {

        try {
            return ResponseEntity.ok().body(provider.updateControlLoops(controlLoops));

        } catch (PfModelRuntimeException | PfModelException e) {
            LOGGER.warn("update of control loops failed", e);
            return createInstantiationErrorResponse(e);
        }
    }

    /**
     * Deletes a control loop definition.
     *
     * @param requestId request ID used in ONAP logging
     * @param name the name of the control loop to delete
     * @param version the version of the control loop to delete
     * @return a response
     */
    // @formatter:off
    @DeleteMapping(value = "/instantiation",
            produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML})
    @ApiOperation(value = "Delete a control loop",
            notes = "Deletes a control loop, returning optional error details",
            response = InstantiationResponse.class,
            tags = {TAGS},
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

    public ResponseEntity<InstantiationResponse> delete(
            @RequestHeader(name = REQUEST_ID_NAME, required = false)
            @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
            @ApiParam(value = "Control Loop definition name", required = true)
            @RequestParam("name") String name,
            @ApiParam(value = "Control Loop definition version")
            @RequestParam(value = "version", required = false) String version) {

        try {
            return ResponseEntity.ok().body(provider.deleteControlLoop(name, version));

        } catch (PfModelRuntimeException | PfModelException e) {
            LOGGER.warn("delete of control loop failed", e);
            return createInstantiationErrorResponse(e);
        }
    }

    /**
     * Issues control loop commands to control loops.
     *
     * @param requestId request ID used in ONAP logging
     * @param command the command to issue to control loops
     * @return the control loop definitions
     */
    // @formatter:off
    @PutMapping(value = "/instantiation/command",
            produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML},
            consumes = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML})
    @ApiOperation(value = "Issue a command to the requested control loops",
            notes = "Issues a command to a control loop, ordering a state change on the control loop",
            response = InstantiationResponse.class,
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
    public ResponseEntity<InstantiationResponse> issueControlLoopCommand(
            @RequestHeader(name = REQUEST_ID_NAME, required = false)
            @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
            @ApiParam(value = "Entity Body of control loop command", required = true)
            @RequestBody InstantiationCommand command) {

        try {
            return ResponseEntity.accepted().body(provider.issueControlLoopCommand(command));

        } catch (PfModelRuntimeException | PfModelException | ControlLoopException e) {
            LOGGER.warn("creation of control loop failed", e);
            return createInstantiationErrorResponse(e);
        }
    }

    /**
     * create a Instantiation Response from an exception.
     *
     * @param e the error
     * @return the Instantiation Response
     */
    private ResponseEntity<InstantiationResponse> createInstantiationErrorResponse(ErrorResponseInfo e) {
        var resp = new InstantiationResponse();
        resp.setErrorDetails(e.getErrorResponse().getErrorMessage());
        return ResponseEntity.status(e.getErrorResponse().getResponseCode().getStatusCode()).body(resp);
    }
}
