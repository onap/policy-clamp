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

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.element.main.rest.genapi.AcElementControllerApi;
import org.onap.policy.clamp.acm.element.service.ConfigService;
import org.onap.policy.clamp.models.acm.messages.rest.element.ElementConfig;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AcElementController implements AcElementControllerApi {

    private final ConfigService configService;

    /**
     * REST end point to get the existing element configuration.
     *
     * @return the element configuration parameters
     */
    @Override
    public ResponseEntity<ElementConfig> getElementConfig(UUID onapRequestId) {
        return new ResponseEntity<>(configService.getElementConfig(), HttpStatus.OK);
    }

    /**
     * REST end point to activate the element.
     *
     * @param params element parameters for this AC element
     */
    @Override
    public ResponseEntity<String> activateElement(UUID onapRequestId, ElementConfig params) {
        configService.activateElement(params);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * REST end point to delete the element configuration.
     *
     * @return Status of operation
     */
    @Override
    public ResponseEntity<Void> deleteConfig(UUID onapRequestId) {
        configService.deleteConfig();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
