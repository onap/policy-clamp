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

package org.onap.policy.clamp.acm.runtime.main.rest;

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
import org.onap.policy.clamp.acm.runtime.instantiation.AutomationCompositionInstantiationProvider;
import org.onap.policy.clamp.acm.runtime.main.web.AbstractRestController;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionException;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.AutomationCompositionOrderStateResponse;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.AutomationCompositionPrimedResponse;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstancePropertiesResponse;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationCommand;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationResponse;
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
 * Class to provide REST end points for creating, deleting, query and commanding a automation composition definition.
 */
@RestController
@RequiredArgsConstructor
public class InstantiationController extends AbstractRestController {

    private static final String TAGS = "Clamp Automation Composition Instantiation API";

    // The Automation Composition provider for instantiation requests
    private final AutomationCompositionInstantiationProvider provider;

    /**
     * Creates a automation composition.
     *
     * @param requestId request ID used in ONAP logging
     * @param automationCompositions the automation compositions
     * @return a response
     * @throws PfModelException on errors creating a automation composition
     */
    // @formatter:off
    @PostMapping(value = "/instantiation",
            produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML},
            consumes = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML})
    @ApiOperation(
            value = "Commissions automation composition definitions",
            notes = "Commissions automation composition definitions, returning the automation composition IDs",
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
        @RequestHeader(name = REQUEST_ID_NAME, required = false) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
        @ApiParam(
            value = "Entity Body of automation composition",
            required = true) @RequestBody AutomationCompositions automationCompositions)
        throws PfModelException {

        return ResponseEntity.ok().body(provider.createAutomationCompositions(automationCompositions));
    }

    /**
     * Saves instance properties.
     *
     * @param requestId request ID used in ONAP logging
     * @param body the body of automation composition following TOSCA definition
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
        @RequestHeader(name = REQUEST_ID_NAME, required = false) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
        @ApiParam(value = "Body of instance properties", required = true) @RequestBody ToscaServiceTemplate body)
        throws PfModelException {

        return ResponseEntity.ok().body(provider.createInstanceProperties(body));
    }

    /**
     * Deletes a automation composition definition and instance properties.
     *
     * @param requestId request ID used in ONAP logging
     * @param name the name of the automation composition to delete
     * @param version the version of the automation composition to delete
     * @return a response
     * @throws PfModelException on errors deleting of automation composition and instance properties
     */
    // @formatter:off
    @DeleteMapping(value = "/instanceProperties",
        produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML})
    @ApiOperation(value = "Delete a automation composition and instance properties",
        notes = "Deletes a automation composition and instance properties, returning optional error details",
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
        @RequestHeader(name = REQUEST_ID_NAME, required = false) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
        @ApiParam(value = "Automation composition  definition name", required = true) @RequestParam("name") String name,
        @ApiParam(value = "Automation composition  definition version") @RequestParam(
            value = "version",
            required = true) String version)
        throws PfModelException {

        return ResponseEntity.ok().body(provider.deleteInstanceProperties(name, version));
    }

    /**
     * Queries details of all automation compositions.
     *
     * @param requestId request ID used in ONAP logging
     * @param name the name of the automation composition to get, null for all automation compositions
     * @param version the version of the automation composition to get, null for all automation compositions
     * @return the automation compositions
     * @throws PfModelException on errors getting commissioning of automation composition
     */
    // @formatter:off
    @GetMapping(value = "/instantiation",
            produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML})
    @ApiOperation(value = "Query details of the requested automation compositions",
            notes = "Queries details of the requested automation compositions, returning all composition details",
            response = AutomationCompositions.class,
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
    public ResponseEntity<AutomationCompositions> query(
        @RequestHeader(name = REQUEST_ID_NAME, required = false) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
        @ApiParam(value = "Automation composition  definition name", required = false) @RequestParam(
            value = "name",
            required = false) String name,
        @ApiParam(value = "Automation composition  definition version", required = false) @RequestParam(
            value = "version",
            required = false) String version)
        throws PfModelException {

        return ResponseEntity.ok().body(provider.getAutomationCompositions(name, version));
    }

    /**
     * Updates a automation composition.
     *
     * @param requestId request ID used in ONAP logging
     * @param automationCompositions the automation compositions
     * @return a response
     * @throws PfModelException on errors updating of automation compositions
     */
    // @formatter:off
    @PutMapping(value = "/instantiation",
            produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML},
            consumes = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML})
    @ApiOperation(
            value = "Updates automation composition definitions",
            notes = "Updates automation composition definitions, returning the updated composition definition IDs",
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
        @RequestHeader(name = REQUEST_ID_NAME, required = false) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
        @ApiParam(
            value = "Entity Body of Automation Composition",
            required = true) @RequestBody AutomationCompositions automationCompositions)
        throws PfModelException {

        return ResponseEntity.ok().body(provider.updateAutomationCompositions(automationCompositions));
    }

    /**
     * Deletes a automation composition definition.
     *
     * @param requestId request ID used in ONAP logging
     * @param name the name of the automation composition to delete
     * @param version the version of the automation composition to delete
     * @return a response
     * @throws PfModelException on errors deleting of automation composition
     */
    // @formatter:off
    @DeleteMapping(value = "/instantiation",
            produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML})
    @ApiOperation(value = "Delete a automation composition",
            notes = "Deletes a automation composition, returning optional error details",
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
        @RequestHeader(name = REQUEST_ID_NAME, required = false) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
        @ApiParam(value = "Automation composition  definition name", required = true) @RequestParam("name") String name,
        @ApiParam(value = "Automation composition  definition version") @RequestParam(
            value = "version",
            required = true) String version)
        throws PfModelException {

        return ResponseEntity.ok().body(provider.deleteAutomationComposition(name, version));
    }

    /**
     * Issues automation composition commands to automation compositions.
     *
     * @param requestId request ID used in ONAP logging
     * @param command the command to issue to automation compositions
     * @return the automation composition definitions
     * @throws PfModelException on errors issuing a command
     * @throws AutomationCompositionException on errors issuing a command
     */
    // @formatter:off
    @PutMapping(value = "/instantiation/command",
            produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML},
            consumes = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML})
    @ApiOperation(value = "Issue a command to the requested automation compositions",
            notes = "Issues a command to an automation composition, ordering a state change on the composition",
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
    public ResponseEntity<InstantiationResponse> issueAutomationCompositionCommand(
        @RequestHeader(name = REQUEST_ID_NAME, required = false) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
        @ApiParam(
            value = "Entity Body of automation composition command",
            required = true) @RequestBody InstantiationCommand command)
        throws AutomationCompositionException, PfModelException {

        return ResponseEntity.accepted().body(provider.issueAutomationCompositionCommand(command));
    }

    /**
     * Queries details of all automation compositions.
     *
     * @param requestId request ID used in ONAP logging
     * @param name the name of the automation composition to get, null for all automation compositions
     * @param version the version of the automation composition to get, null for all automation compositions
     * @return the automation compositions
     * @throws PfModelException on errors getting commissioning of automation composition
     */
    // @formatter:off
    @GetMapping(value = "/instantiationState",
        produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML})
    @ApiOperation(value = "Query details of the requested automation compositions",
        notes = "Queries details of requested automation compositions, returning all automation composition details",
        response = AutomationCompositions.class,
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
    public ResponseEntity<AutomationCompositionOrderStateResponse> getInstantiationOrderState(
        @RequestHeader(name = REQUEST_ID_NAME, required = false) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
        @ApiParam(value = "Automation composition  name", required = false) @RequestParam(
            value = "name",
            required = false) String name,
        @ApiParam(value = "Automation composition  version", required = false) @RequestParam(
            value = "version",
            required = false) String version)
        throws PfModelException {

        return ResponseEntity.ok().body(provider.getInstantiationOrderState(name, version));
    }

    /**
     * Queries Primed/De-Primed status of a automation composition.
     *
     * @param requestId request ID used in ONAP logging
     * @param name the name of the automation composition to get, null for all automation compositions
     * @param version the version of the automation composition to get, null for all automation compositions
     * @return the automation compositions
     * @throws PfModelException on errors getting priming of automation composition
     */
    // @formatter:off
    @GetMapping(value = "/automationCompositionPriming",
        produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML})
    @ApiOperation(value = "Query priming details of the requested automation compositions",
        notes = "Queries priming details of requested automation compositions, returning primed/deprimed compositions",
        response = AutomationCompositionPrimedResponse.class,
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
    public ResponseEntity<AutomationCompositionPrimedResponse> getAutomationCompositionPriming(
        @RequestHeader(name = REQUEST_ID_NAME, required = false) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
        @ApiParam(value = "Automation composition  definition name", required = false) @RequestParam(
            value = "name",
            required = false) String name,
        @ApiParam(value = "Automation composition  definition version", required = false) @RequestParam(
            value = "version",
            required = false) String version)
        throws PfModelException {

        return ResponseEntity.ok().body(provider.getAutomationCompositionPriming(name, version));
    }
}
