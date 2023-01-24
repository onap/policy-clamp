/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation.
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

package org.onap.policy.clamp.acm.runtime.main.rest.stub;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.runtime.main.rest.gen.AutomationCompositionDefinitionApi;
import org.onap.policy.clamp.acm.runtime.main.web.AbstractRestController;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.AcTypeStateUpdate;
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.CommissioningResponse;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplates;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("stub")
@RequiredArgsConstructor
public class CommissioningControllerStub extends AbstractRestController implements AutomationCompositionDefinitionApi {

    private final StubUtils stubUtils;

    @Value("${stub.deleteCompositionDefinitionResponse}")
    private String pathToResponseFile;

    @Value("${stub.getAllCompositionDefinitions}")
    private String pathToAllDefinitions;

    @Value("${stub.getSingleCompositionDefinition}")
    private String pathToSingleDefinition;

    @Value("${stub.postCommissionResponse}")
    private String pathToPostResponse;

    @Value("${stub.putCompositionDefinitionUpdateResponse}")
    private String pathToPutUpdate;

    @Override
    public ResponseEntity<CommissioningResponse> createCompositionDefinitions(ToscaServiceTemplate body,
            UUID xonaprequestid) {
        var compositionId = body.getMetadata() != null ? body.getMetadata().get("compositionId") : null;
        if (compositionId == null) {
            return stubUtils.getResponse(pathToPostResponse, CommissioningResponse.class);
        } else {
            return stubUtils.getResponse(pathToPutUpdate, CommissioningResponse.class);
        }
    }

    @Override
    public ResponseEntity<CommissioningResponse> deleteCompositionDefinition(UUID compositionId, UUID xonaprequestid) {
        return stubUtils.getResponse(pathToResponseFile, CommissioningResponse.class);
    }

    @Override
    public ResponseEntity<AutomationCompositionDefinition> getCompositionDefinition(UUID compositionId,
            UUID xonaprequestid) {
        return stubUtils.getResponse(pathToSingleDefinition, AutomationCompositionDefinition.class);
    }

    @Override
    public ResponseEntity<ToscaServiceTemplates> queryCompositionDefinitions(String name, String version,
            UUID xonaprequestid) {
        return stubUtils.getResponse(pathToAllDefinitions, ToscaServiceTemplates.class);
    }

    @Override
    public ResponseEntity<Void> compositionDefinitionPriming(UUID compositionId, UUID requestId,
            AcTypeStateUpdate body) {
        // TODO Auto-generated method stub
        return null;
    }
}
