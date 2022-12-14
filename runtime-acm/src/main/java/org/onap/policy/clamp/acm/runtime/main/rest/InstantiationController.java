/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation.
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

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.runtime.instantiation.AutomationCompositionInstantiationProvider;
import org.onap.policy.clamp.acm.runtime.main.rest.gen.AutomationCompositionInstanceApi;
import org.onap.policy.clamp.acm.runtime.main.web.AbstractRestController;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationResponse;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationUpdate;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Class to provide REST end points for creating, deleting, query and commanding a automation composition definition.
 */
@RestController
@RequiredArgsConstructor
@Profile("default")
public class InstantiationController extends AbstractRestController implements AutomationCompositionInstanceApi {

    // The Automation Composition provider for instantiation requests
    private final AutomationCompositionInstantiationProvider provider;

    /**
     * Creates an automation composition.
     *
     * @param compositionId The UUID of the automation composition definition
     * @param automationComposition the automation composition
     * @param requestId request ID used in ONAP logging
     * @return a response
     */
    @Override
    public ResponseEntity<InstantiationResponse> createCompositionInstance(UUID compositionId,
            AutomationComposition automationComposition, UUID requestId) {

        var response = provider.createAutomationComposition(compositionId, automationComposition);
        return ResponseEntity
                .created(createUri("/compositions/" + compositionId + "/instances/" + response.getInstanceId()))
                .body(response);
    }

    /**
     * Gets an automation composition.
     *
     * @param compositionId The UUID of the automation composition definition
     * @param instanceId The UUID of the automation composition instance
     * @param requestId request ID used in ONAP logging
     * @return the automation composition instance
     */
    @Override
    public ResponseEntity<AutomationComposition> getCompositionInstance(UUID compositionId, UUID instanceId,
            UUID requestId) {
        return ResponseEntity.ok().body(provider.getAutomationComposition(compositionId, instanceId));
    }

    /**
     * Queries details of all automation compositions.
     *
     * @param compositionId The UUID of the automation composition definition
     * @param name the name of the automation composition to get, null for all automation compositions
     * @param version the version of the automation composition to get, null for all automation compositions
     * @param requestId request ID used in ONAP logging
     * @return the automation compositions
     */
    @Override
    public ResponseEntity<AutomationCompositions> queryCompositionInstances(UUID compositionId, String name,
            String version, UUID requestId) {

        return ResponseEntity.ok().body(provider.getAutomationCompositions(compositionId, name, version));
    }

    /**
     * Updates a automation composition.
     *
     * @param compositionId The UUID of the automation composition definition
     * @param instanceId The UUID of the automation composition instance
     * @param instanceUpdate the automation composition to update
     * @param requestId request ID used in ONAP logging
     * @return a response
     */
    @Override
    public ResponseEntity<InstantiationResponse> updateCompositionInstance(UUID compositionId, UUID instanceId,
            InstantiationUpdate instanceUpdate, UUID requestId) {

        return ResponseEntity.ok()
                .body(provider.updateAutomationComposition(compositionId, instanceId, instanceUpdate));
    }

    /**
     * Deletes a automation composition definition.
     *
     * @param compositionId The UUID of the automation composition definition
     * @param instanceId The UUID of the automation composition instance
     * @param requestId request ID used in ONAP logging
     * @return a response
     */
    @Override
    public ResponseEntity<InstantiationResponse> deleteCompositionInstance(UUID compositionId, UUID instanceId,
            UUID requestId) {

        return ResponseEntity.ok().body(provider.deleteAutomationComposition(compositionId, instanceId));
    }
}
