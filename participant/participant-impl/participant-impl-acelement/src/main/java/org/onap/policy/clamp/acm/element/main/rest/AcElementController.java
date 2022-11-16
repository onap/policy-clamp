/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Nordix Foundation.
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

package org.onap.policy.clamp.acm.element.main.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.element.service.ConfigService;
import org.onap.policy.clamp.models.acm.messages.rest.element.ElementConfig;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AcElementController extends AbstractRestController {

    private final ConfigService configService;

    /**
     * REST endpoint to get the existing element config.
     *
     * @return the element config params
     */
    // @formatter:off
    @GetMapping(path = "/config", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Return the element config",
        tags = { "Clamp Automation Composition AC Element Impl API" })
    @ApiResponses(
        value = {
            @ApiResponse(responseCode = OK_CODE, description = SERVER_OK_MESSAGE,
                    content = @Content(schema = @Schema(implementation = ElementConfig.class)),
                    headers = {
                        @Header(name = API_VERSION_NAME),
                        @Header(name = VERSION_MINOR_NAME, description = VERSION_MINOR_DESCRIPTION),
                        @Header(name = VERSION_PATCH_NAME, description = VERSION_PATCH_DESCRIPTION),
                        @Header(name = VERSION_PATCH_NAME, description = VERSION_PATCH_DESCRIPTION),
                        @Header(name = VERSION_LATEST_NAME, description = VERSION_LATEST_DESCRIPTION),
                        @Header(name = REQUEST_ID_NAME, description = REQUEST_ID_HDR_DESCRIPTION)
                    }),
            @ApiResponse(responseCode = AUTHENTICATION_ERROR_CODE, description = AUTHENTICATION_ERROR_MESSAGE)
        }
    )
    // @formatter:on
    public ResponseEntity<ElementConfig> getElementConfig() {
        return new ResponseEntity<>(configService.getElementConfig(), HttpStatus.OK);
    }

    /**
     * REST endpoint to activate the element.
     *
     * @param params element parameters for this ac element
     */
    // @formatter:off
    @PostMapping(path = "/activate", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Activates the element config",
        tags = { "Clamp Automation Composition AC Element Impl API" }
    )
    @ApiResponses(
        value = {
            @ApiResponse(responseCode = CREATED_CODE, description = SERVER_OK_MESSAGE,
                    headers = {
                        @Header(name = API_VERSION_NAME),
                        @Header(name = VERSION_MINOR_NAME, description = VERSION_MINOR_DESCRIPTION),
                        @Header(name = VERSION_PATCH_NAME, description = VERSION_PATCH_DESCRIPTION),
                        @Header(name = VERSION_PATCH_NAME, description = VERSION_PATCH_DESCRIPTION),
                        @Header(name = VERSION_LATEST_NAME, description = VERSION_LATEST_DESCRIPTION),
                        @Header(name = REQUEST_ID_NAME, description = REQUEST_ID_HDR_DESCRIPTION)
                    }),
            @ApiResponse(responseCode = AUTHENTICATION_ERROR_CODE, description = AUTHENTICATION_ERROR_MESSAGE),
            @ApiResponse(responseCode = BAD_REQUEST_ERROR_CODE, description = BAD_REQUEST_ERROR_MESSAGE)
        }
    )
    // formatter:on
    public ResponseEntity<Object> activateElement(@RequestBody ElementConfig params) {
        configService.activateElement(params);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * REST endpoint to delete the element config.
     *
     * @return Status of operation
     */
    // @formatter:off
    @DeleteMapping(path = "/deactivate")
    @Operation(summary = "Delete the element config",
        tags = { "Clamp Automation Composition AC Element Impl API" }
    )
    @ApiResponses(
        value = {
            @ApiResponse(responseCode = NO_CONTENT_CODE, description = SERVER_OK_MESSAGE,
                    headers = {
                        @Header(name = API_VERSION_NAME),
                        @Header(name = VERSION_MINOR_NAME, description = VERSION_MINOR_DESCRIPTION),
                        @Header(name = VERSION_PATCH_NAME, description = VERSION_PATCH_DESCRIPTION),
                        @Header(name = VERSION_PATCH_NAME, description = VERSION_PATCH_DESCRIPTION),
                        @Header(name = VERSION_LATEST_NAME, description = VERSION_LATEST_DESCRIPTION),
                        @Header(name = REQUEST_ID_NAME, description = REQUEST_ID_HDR_DESCRIPTION)
                    }),
            @ApiResponse(responseCode = AUTHENTICATION_ERROR_CODE, description = AUTHENTICATION_ERROR_MESSAGE)
        }
    )
    // @formatter:on
    public ResponseEntity<Void> deleteConfig() {
        configService.deleteConfig();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
