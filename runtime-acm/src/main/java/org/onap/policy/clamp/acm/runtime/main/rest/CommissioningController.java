/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation.
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

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.runtime.commissioning.CommissioningProvider;
import org.onap.policy.clamp.acm.runtime.main.web.AbstractRestController;
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.CommissioningResponse;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplates;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Class to provide REST end points for creating, deleting, querying commissioned automation compositions.
 */
@RestController
@RequiredArgsConstructor
public class CommissioningController extends AbstractRestController implements AutomationCompositionDefinitionApi {

    private final CommissioningProvider provider;

    /**
     * Creates a automation composition definition.
     *
     * @param body the body of automation composition following TOSCA definition
     * @param requestId request ID used in ONAP logging
     * @return a response
     */
    @Override
    // @formatter:off
    @PostMapping(value = "/commission",
            consumes = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML},
            produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML})
    // @formatter:on
    public ResponseEntity<CommissioningResponse> createCompositionDefinitions(
            @Parameter(
                    description = "Entity Body of Automation Composition",
                    required = true) @RequestBody ToscaServiceTemplate body,
            @RequestHeader(name = REQUEST_ID_NAME, required = false) @Parameter(
                    description = REQUEST_ID_PARAM_DESCRIPTION) UUID requestId) {

        var response = provider.createAutomationCompositionDefinitions(body);
        return ResponseEntity.created(createUri("/commission/" + response.getCompositionId())).body(response);
    }

    /**
     * Deletes a automation composition definition.
     *
     * @param requestId request ID used in ONAP logging
     * @param compositionId The UUID of the automation composition definition to delete
     * @return a response
     */
    @Override
    // @formatter:off
    @DeleteMapping(value = "/commission/{compositionId}",
            produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML})
    // @formatter:on
    public ResponseEntity<CommissioningResponse> deleteCompositionDefinition(
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The UUID of the automation composition definition to delete",
                    required = true) @PathVariable("compositionId") UUID compositionId,
            @RequestHeader(name = REQUEST_ID_NAME, required = false) @Parameter(
                    description = REQUEST_ID_PARAM_DESCRIPTION) UUID requestId) {

        return ResponseEntity.ok().body(provider.deleteAutomationCompositionDefinition(compositionId));
    }

    /**
     * Queries details of all or specific automation composition definitions.
     *
     * @param name the name of the automation composition definition to get, null for all definitions
     * @param version the version of the automation composition definition to get, null for all definitions
     * @param requestId request ID used in ONAP logging
     * @return the automation composition definitions
     * @throws PfModelException on errors getting details of all or specific automation composition definitions
     */
    @Override
    // @formatter:off
    @GetMapping(value = "/commission",
            produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML})
    // @formatter:on
    public ResponseEntity<ToscaServiceTemplates> queryCompositionDefinitions(

            @Parameter(description = "Automation composition  definition name", required = false) @RequestParam(
                    value = "name",
                    required = false) String name,
            @Parameter(description = "Automation composition  definition version", required = false) @RequestParam(
                    value = "version",
                    required = false) String version,
            @RequestHeader(name = REQUEST_ID_NAME, required = false) @Parameter(
                    description = REQUEST_ID_PARAM_DESCRIPTION) UUID requestId) {

        return ResponseEntity.ok().body(provider.getAutomationCompositionDefinitions(name, version));
    }

    // @formatter:off
    @Override
    @GetMapping(value = "/commission/{compositionId}",
            produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML})
    // @formatter:on
    public ResponseEntity<ToscaServiceTemplate> getCompositionDefinition(
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The UUID of the automation composition definition to get",
                    required = true) @PathVariable("compositionId") UUID compositionId,
            @RequestHeader(name = REQUEST_ID_NAME, required = false) @Parameter(
                    description = REQUEST_ID_PARAM_DESCRIPTION) UUID requestId) {

        return ResponseEntity.ok().body(provider.getAutomationCompositionDefinitions(compositionId));
    }

    // @formatter:off
    @Override
    @PutMapping(value = "/commission/{compositionId}",
            consumes = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML},
            produces = {MediaType.APPLICATION_JSON_VALUE, APPLICATION_YAML})
    // @formatter:on
    public ResponseEntity<CommissioningResponse> updateCompositionDefinition(
            @Parameter(
                    in = ParameterIn.PATH,
                    description = "The UUID of the automation composition definition to update",
                    required = true) @PathVariable("compositionId") UUID compositionId,
            @Parameter(
                    in = ParameterIn.DEFAULT,
                    description = "Serialised instance of.",
                    required = true) @RequestBody ToscaServiceTemplate body,
            @RequestHeader(name = REQUEST_ID_NAME, required = false) @Parameter(
                    description = REQUEST_ID_PARAM_DESCRIPTION) UUID requestId) {
        return ResponseEntity.ok().body(provider.updateCompositionDefinition(compositionId, body));
    }
}
