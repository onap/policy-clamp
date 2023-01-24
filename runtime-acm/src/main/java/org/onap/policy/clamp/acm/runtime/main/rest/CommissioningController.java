/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation.
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
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.runtime.commissioning.CommissioningProvider;
import org.onap.policy.clamp.acm.runtime.main.rest.gen.AutomationCompositionDefinitionApi;
import org.onap.policy.clamp.acm.runtime.main.web.AbstractRestController;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.AcTypeStateUpdate;
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.CommissioningResponse;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplates;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Class to provide REST end points for creating, deleting, querying commissioned automation compositions.
 */
@RestController
@RequiredArgsConstructor
@Profile("default")
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
    public ResponseEntity<CommissioningResponse> createCompositionDefinitions(ToscaServiceTemplate body,
            UUID requestId) {
        var compositionId = body.getMetadata() != null ? body.getMetadata().get("compositionId") : null;
        if (compositionId == null) {
            var response = provider.createAutomationCompositionDefinition(body);
            return ResponseEntity.created(createUri("/compositions/" + response.getCompositionId())).body(response);
        } else {
            return ResponseEntity.ok()
                    .body(provider.updateCompositionDefinition(UUID.fromString(compositionId.toString()), body));
        }
    }

    /**
     * Deletes a automation composition definition.
     *
     * @param requestId request ID used in ONAP logging
     * @param compositionId The UUID of the automation composition definition to delete
     * @return a response
     */
    @Override
    public ResponseEntity<CommissioningResponse> deleteCompositionDefinition(UUID compositionId, UUID requestId) {
        return ResponseEntity.ok().body(provider.deleteAutomationCompositionDefinition(compositionId));
    }

    /**
     * Queries details of all or specific automation composition definitions.
     *
     * @param name the name of the automation composition definition to get, null for all definitions
     * @param version the version of the automation composition definition to get, null for all definitions
     * @param requestId request ID used in ONAP logging
     * @return the automation composition definitions
     */
    @Override
    public ResponseEntity<ToscaServiceTemplates> queryCompositionDefinitions(String name, String version,
        UUID requestId) {
        return ResponseEntity.ok().body(provider.getAutomationCompositionDefinitions(name, version));
    }

    @Override
    public ResponseEntity<AutomationCompositionDefinition> getCompositionDefinition(UUID compositionId,
        UUID requestId) {
        return ResponseEntity.ok().body(provider.getAutomationCompositionDefinition(compositionId));
    }

    @Override
    public ResponseEntity<Void> compositionDefinitionPriming(UUID compositionId, UUID requestId,
        @Valid AcTypeStateUpdate body) {
        // TODO Auto-generated method stub
        return null;
    }
}
