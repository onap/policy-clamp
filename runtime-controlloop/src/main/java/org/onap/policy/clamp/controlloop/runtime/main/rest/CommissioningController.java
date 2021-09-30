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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.core.Response.Status;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopException;
import org.onap.policy.clamp.controlloop.models.messages.rest.commissioning.CommissioningResponse;
import org.onap.policy.clamp.controlloop.runtime.commissioning.CommissioningProvider;
import org.onap.policy.clamp.controlloop.runtime.main.web.AbstractRestController;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Class to provide REST end points for creating, deleting, querying commissioned control loops.
 */
@RestController
@RequiredArgsConstructor
public class CommissioningController extends AbstractRestController {

    private static final String TAGS = "Clamp Control Loop Commissioning API";

    private final CommissioningProvider provider;

    /**
     * Creates a control loop definition.
     *
     * @param requestId request ID used in ONAP logging
     * @param body the body of control loop following TOSCA definition
     * @return a response
     * @throws PfModelException on errors creating a control loop definition
     */
    // @formatter:off
    @PostMapping(value = "/commission",
            consumes = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML},
            produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML})
    @ApiOperation(
        value = "Commissions control loop definitions",
        notes = "Commissions control loop definitions, returning the commissioned control loop definition IDs",
        response = CommissioningResponse.class,
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
    public ResponseEntity<CommissioningResponse> create(
            @RequestHeader(
                    name = REQUEST_ID_NAME,
                    required = false) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
            @ApiParam(value = "Entity Body of Control Loop", required = true) @RequestBody ToscaServiceTemplate body)
        throws PfModelException {

        return ResponseEntity.ok().body(provider.createControlLoopDefinitions(body));
    }

    /**
     * Deletes a control loop definition.
     *
     * @param requestId request ID used in ONAP logging
     * @param name the name of the control loop definition to delete
     * @param version the version of the control loop definition to delete
     * @return a response
     * @throws PfModelException on errors deleting a control loop definition
     */
    // @formatter:off
    @DeleteMapping(value = "/commission",
            produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML})
    @ApiOperation(value = "Delete a commissioned control loop",
        notes = "Deletes a Commissioned Control Loop, returning optional error details",
        response = CommissioningResponse.class,
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
    public ResponseEntity<CommissioningResponse> delete(
            @RequestHeader(
                    name = REQUEST_ID_NAME,
                    required = false) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
            @ApiParam(value = "Control Loop definition name", required = true) @RequestParam(
                    value = "name") String name,
            @ApiParam(
                    value = "Control Loop definition version",
                    required = true) @RequestParam("version") String version)
        throws PfModelException {

        return ResponseEntity.ok().body(provider.deleteControlLoopDefinition(name, version));
    }

    /**
     * Queries details of all or specific control loop definitions.
     *
     * @param requestId request ID used in ONAP logging
     * @param name the name of the control loop definition to get, null for all definitions
     * @param version the version of the control loop definition to get, null for all definitions
     * @return the control loop definitions
     * @throws PfModelException on errors getting details of all or specific control loop definitions
     */
    // @formatter:off
    @GetMapping(value = "/commission",
            produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML})
    @ApiOperation(value = "Query details of the requested commissioned control loop definitions",
        notes = "Queries details of the requested commissioned control loop definitions, "
            + "returning all control loop details",
        response = ToscaNodeTemplate.class,
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
    public ResponseEntity<List<ToscaNodeTemplate>> query(
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

        return ResponseEntity.ok().body(provider.getControlLoopDefinitions(name, version));
    }

    /**
     * Retrieves the Tosca Service Template.
     *
     * @param requestId request ID used in ONAP logging
     * @param name the name of the tosca service template to retrieve
     * @param version the version of the tosca service template to get
     * @return the specified tosca service template
     * @throws PfModelException on errors getting the Tosca Service Template
     */
    // @formatter:off
    @GetMapping(value = "/commission/toscaservicetemplate",
            produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML})
    @ApiOperation(value = "Query details of the requested tosca service templates",
        notes = "Queries details of the requested commissioned tosca service template, "
            + "returning all tosca service template details",
        response = ToscaServiceTemplate.class,
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
    public ResponseEntity<String> queryToscaServiceTemplate(
            @RequestHeader(
                    name = REQUEST_ID_NAME,
                    required = false) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
            @ApiParam(value = "Tosca service template name", required = false) @RequestParam(
                    value = "name",
                    required = false) String name,
            @ApiParam(value = "Tosca service template version", required = false) @RequestParam(
                    value = "version",
                    required = false) String version)
        throws PfModelException, ControlLoopException {

        return ResponseEntity.ok().body(provider.getToscaServiceTemplateReduced(name, version));
    }

    /**
     * Retrieves the Json Schema for the specified Tosca Service Template.
     *
     * @param requestId request ID used in ONAP logging
     * @param section section of the tosca service template to get schema for
     * @return the specified tosca service template or section Json Schema
     * @throws PfModelException on errros getting the Json Schema for the specified Tosca Service Template
     */
    // @formatter:off
    @GetMapping(value = "/commission/toscaServiceTemplateSchema",
        produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML})
    @ApiOperation(value = "Query details of the requested tosca service template json schema",
        notes = "Queries details of the requested commissioned tosca service template json schema, "
            + "returning all tosca service template json schema details",
        response = ToscaServiceTemplate.class,
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
    public ResponseEntity<String> queryToscaServiceTemplateJsonSchema(
            @RequestHeader(
                    name = REQUEST_ID_NAME,
                    required = false) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
            @ApiParam(value = "Section of Template schema is desired for", required = false) @RequestParam(
                    value = "section",
                    required = false,
                    defaultValue = "all") String section)
            throws PfModelException {

        return ResponseEntity.ok().body(provider.getToscaServiceTemplateSchema(section));
    }

    /**
     * Retrieves the Common or Instance Properties for the specified Tosca Service Template.
     *
     * @param requestId request ID used in ONAP logging
     * @param common a flag, true to get common properties, false to get instance properties
     * @param name the name of the tosca service template to retrieve
     * @param version the version of the tosca service template to get
     * @return the specified tosca service template or section Json Schema
     * @throws PfModelException on errors getting the Common or Instance Properties
     * @throws ControlLoopException on error getting the Common or Instance Properties
     */
    // @formatter:off
    @GetMapping(value = "/commission/getCommonOrInstanceProperties",
        produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML})
    @ApiOperation(value = "Query details of the requested tosca service template common or instance properties",
        notes = "Queries details of the requested commissioned tosca service template json common"
            + "or instance properties, returning all tosca service template common or instance property details",
        response = ToscaServiceTemplate.class,
        tags = {"Clamp Control Loop Commissioning API"},
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
    public ResponseEntity<Map<String, ToscaNodeTemplate>> queryToscaServiceCommonOrInstanceProperties(
            @RequestHeader(
                    name = REQUEST_ID_NAME,
                    required = false) @ApiParam(REQUEST_ID_PARAM_DESCRIPTION) UUID requestId,
            @ApiParam(value = "Flag, true for common properties, false for instance", required = false) @RequestParam(
                    value = "common",
                    defaultValue = "false",
                    required = false) boolean common,
            @ApiParam(value = "Tosca service template name", required = false) @RequestParam(
                    value = "name",
                    required = false) String name,
            @ApiParam(value = "Tosca service template version", required = false) @RequestParam(
                    value = "version",
                    required = false) String version)
        throws PfModelException {

        return ResponseEntity.ok().body(provider.getNodeTemplatesWithCommonOrInstanceProperties(common, name, version));
    }

    /**
     * Queries the elements of a specific control loop.
     *
     * @param requestId request ID used in ONAP logging
     * @param name the name of the control loop definition to get
     * @param version the version of the control loop definition to get
     * @return the control loop element definitions
     * @throws PfModelException on errors getting the elements of a specific control loop
     */
    // @formatter:off
    @GetMapping(value = "/commission/elements",
            produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML})
    @ApiOperation(value = "Query details of the requested commissioned control loop element definitions",
        notes = "Queries details of the requested commissioned control loop element definitions, "
            + "returning all control loop elements' details",
        response = ToscaNodeTemplate.class,
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
    public ResponseEntity<List<ToscaNodeTemplate>> queryElements(
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

        List<ToscaNodeTemplate> nodeTemplate = provider.getControlLoopDefinitions(name, version);
        // Prevent ambiguous queries with multiple returns
        if (nodeTemplate.size() > 1) {
            throw new PfModelException(Status.NOT_ACCEPTABLE, "Multiple ControlLoops are not supported");
        }

        List<ToscaNodeTemplate> response = provider.getControlLoopElementDefinitions(nodeTemplate.get(0));
        return ResponseEntity.ok().body(response);
    }
}
