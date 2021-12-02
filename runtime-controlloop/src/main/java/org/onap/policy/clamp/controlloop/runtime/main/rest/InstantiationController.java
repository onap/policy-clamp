/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
 *  Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopException;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoops;
import org.onap.policy.clamp.controlloop.models.messages.rest.instantiation.ControlLoopOrderStateResponse;
import org.onap.policy.clamp.controlloop.models.messages.rest.instantiation.ControlLoopPrimedResponse;
import org.onap.policy.clamp.controlloop.models.messages.rest.instantiation.InstancePropertiesResponse;
import org.onap.policy.clamp.controlloop.models.messages.rest.instantiation.InstantiationCommand;
import org.onap.policy.clamp.controlloop.models.messages.rest.instantiation.InstantiationResponse;
import org.onap.policy.clamp.controlloop.runtime.instantiation.ControlLoopInstantiationProvider;
import org.onap.policy.clamp.controlloop.runtime.main.web.AbstractRestController;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
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
@RequiredArgsConstructor
public class InstantiationController extends AbstractRestController {

    private static final String TAGS = "Clamp Control Loop Instantiation API";

    // The CL provider for instantiation requests
    private final ControlLoopInstantiationProvider provider;

    /**
     * Creates a control loop.
     *
     * @param requestId request ID used in ONAP logging
     * @param controlLoops the control loops
     * @return a response
     * @throws PfModelException on errors creating a control loop
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
            @RequestHeader(
                    name = REQUEST_ID_NAME,
                    required = false) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
            @ApiParam(value = "Entity Body of Control Loop", required = true) @RequestBody ControlLoops controlLoops)
            throws PfModelException {

        return ResponseEntity.ok().body(provider.createControlLoops(controlLoops));
    }

    /**
     * Saves instance properties.
     *
     * @param requestId request ID used in ONAP logging
     * @param body the body of control loop following TOSCA definition
     * @return a response
     */
    // @formatter:off
    @PostMapping(value = "/instanceProperties",
        consumes = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML},
        produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML})
    @ApiOperation(
        value = "Saves instance properties",
        notes = "Saves instance properties, returning the saved instances properties and it's version",
        response = InstancePropertiesResponse.class,
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
    public ResponseEntity<InstancePropertiesResponse> createInstanceProperties(
            @RequestHeader(
                    name = REQUEST_ID_NAME,
                    required = false) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
            @ApiParam(value = "Body of instance properties", required = true) @RequestBody ToscaServiceTemplate body)
            throws PfModelException {

        return ResponseEntity.ok().body(provider.createInstanceProperties(body));
    }

    /**
     * Deletes a control loop definition and instance properties.
     *
     * @param requestId request ID used in ONAP logging
     * @param name the name of the control loop to delete
     * @param version the version of the control loop to delete
     * @return a response
     * @throws PfModelException on errors deleting of control loop and instance properties
     */
    // @formatter:off
    @DeleteMapping(value = "/instanceProperties",
        produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML})
    @ApiOperation(value = "Delete a control loop and instance properties",
        notes = "Deletes a control loop and instance properties, returning optional error details",
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

    public ResponseEntity<InstantiationResponse> deleteInstanceProperties(
            @RequestHeader(
                    name = REQUEST_ID_NAME,
                    required = false) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
            @ApiParam(value = "Control Loop definition name", required = true) @RequestParam("name") String name,
            @ApiParam(value = "Control Loop definition version") @RequestParam(
                    value = "version",
                    required = true) String version)
            throws PfModelException {

        return ResponseEntity.ok().body(provider.deleteInstanceProperties(name, version));
    }

    /**
     * Queries details of all control loops.
     *
     * @param requestId request ID used in ONAP logging
     * @param name the name of the control loop to get, null for all control loops
     * @param version the version of the control loop to get, null for all control loops
     * @return the control loops
     * @throws PfModelException on errors getting commissioning of control loop
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
    public ResponseEntity<ControlLoops> query(
            @RequestHeader(
                    name = REQUEST_ID_NAME,
                    required = false) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
            @ApiParam(value = "Control Loop definition name", required = false) @RequestParam(
                    value = "name",
                    required = false) String name,
            @ApiParam(value = "Control Loop definition version", required = false) @RequestParam(
                    value = "version",
                    required = false) String version)
            throws PfModelException {

        return ResponseEntity.ok().body(provider.getControlLoops(name, version));
    }

    /**
     * Updates a control loop.
     *
     * @param requestId request ID used in ONAP logging
     * @param controlLoops the control loops
     * @return a response
     * @throws PfModelException on errors updating of control loops
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
            @RequestHeader(
                    name = REQUEST_ID_NAME,
                    required = false) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
            @ApiParam(value = "Entity Body of Control Loop", required = true) @RequestBody ControlLoops controlLoops)
            throws PfModelException {

        return ResponseEntity.ok().body(provider.updateControlLoops(controlLoops));
    }

    /**
     * Deletes a control loop definition.
     *
     * @param requestId request ID used in ONAP logging
     * @param name the name of the control loop to delete
     * @param version the version of the control loop to delete
     * @return a response
     * @throws PfModelException on errors deleting of control loop
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
            @RequestHeader(
                    name = REQUEST_ID_NAME,
                    required = false) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
            @ApiParam(value = "Control Loop definition name", required = true) @RequestParam("name") String name,
            @ApiParam(value = "Control Loop definition version") @RequestParam(
                    value = "version",
                    required = true) String version)
            throws PfModelException {

        return ResponseEntity.ok().body(provider.deleteControlLoop(name, version));
    }

    /**
     * Issues control loop commands to control loops.
     *
     * @param requestId request ID used in ONAP logging
     * @param command the command to issue to control loops
     * @return the control loop definitions
     * @throws PfModelException on errors issuing a command
     * @throws ControlLoopException on errors issuing a command
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
            @RequestHeader(
                    name = REQUEST_ID_NAME,
                    required = false) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
            @ApiParam(
                    value = "Entity Body of control loop command",
                    required = true) @RequestBody InstantiationCommand command)
            throws ControlLoopException, PfModelException {

        return ResponseEntity.accepted().body(provider.issueControlLoopCommand(command));
    }

    /**
     * Queries details of all control loops.
     *
     * @param requestId request ID used in ONAP logging
     * @param name the name of the control loop to get, null for all control loops
     * @param version the version of the control loop to get, null for all control loops
     * @return the control loops
     * @throws PfModelException on errors getting commissioning of control loop
     */
    // @formatter:off
    @GetMapping(value = "/instantiationState",
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
    public ResponseEntity<ControlLoopOrderStateResponse> getInstantiationOrderState(
            @RequestHeader(
                    name = REQUEST_ID_NAME,
                    required = false) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
            @ApiParam(value = "Control Loop name", required = false) @RequestParam(
                    value = "name",
                    required = false) String name,
            @ApiParam(value = "Control Loop version", required = false) @RequestParam(
                    value = "version",
                    required = false) String version)
            throws PfModelException {

        return ResponseEntity.ok().body(provider.getInstantiationOrderState(name, version));
    }

    /**
     * Queries Primed/De-Primed status of a control loop.
     *
     * @param requestId request ID used in ONAP logging
     * @param name the name of the control loop to get, null for all control loops
     * @param version the version of the control loop to get, null for all control loops
     * @return the control loops
     * @throws PfModelException on errors getting priming of control loop
     */
    // @formatter:off
    @GetMapping(value = "/controlLoopPriming",
        produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML})
    @ApiOperation(value = "Query priming details of the requested control loops",
        notes = "Queries priming details of the requested control loops, returning primed/deprimed control loops",
        response = ControlLoopPrimedResponse.class,
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
    public ResponseEntity<ControlLoopPrimedResponse> getControlLoopPriming(
            @RequestHeader(
                    name = REQUEST_ID_NAME,
                    required = false) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
            @ApiParam(value = "Control Loop definition name", required = false) @RequestParam(
                    value = "name",
                    required = false) String name,
            @ApiParam(value = "Control Loop definition version", required = false) @RequestParam(
                    value = "version",
                    required = false) String version)
            throws PfModelException {

        return ResponseEntity.ok().body(provider.getControlLoopPriming(name, version));
    }
}
