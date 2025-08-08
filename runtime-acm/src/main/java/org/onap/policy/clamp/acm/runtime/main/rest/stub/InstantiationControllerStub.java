/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023,2025 OpenInfra Foundation Europe. All rights reserved.
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

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.runtime.main.rest.gen.AutomationCompositionInstanceApi;
import org.onap.policy.clamp.acm.runtime.main.web.AbstractRestController;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.AcInstanceStateUpdate;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("stub")
@RequiredArgsConstructor
public class InstantiationControllerStub extends AbstractRestController implements AutomationCompositionInstanceApi {

    private final StubUtils stubUtils;

    @Value("${stub.deleteCompositionInstanceResponse}")
    private String pathToResponseFile;

    @Value("${stub.getCompositionInstancesResponse}")
    private String pathToSingleInstance;

    @Value("${stub.getAllCompositionInstancesResponse}")
    private String pathToAllInstances;

    @Value("${stub.postInstanceResponse}")
    private String pathPostInstance;

    @Value("${stub.postCompositionInstanceUpdateResponse}")
    private String pathToPutUpdate;

    @Override
    public ResponseEntity<InstantiationResponse> createCompositionInstance(UUID compositionId,
            AutomationComposition body, UUID xonaprequestid) {
        if (body.getInstanceId() == null) {
            return stubUtils.getResponse(pathPostInstance, InstantiationResponse.class);
        } else {
            return stubUtils.getResponse(pathToResponseFile, InstantiationResponse.class);
        }
    }

    @Override
    public ResponseEntity<InstantiationResponse> deleteCompositionInstance(UUID compositionId, UUID instanceId,
            UUID xonaprequestid) {
        return stubUtils.getResponse(pathToResponseFile, InstantiationResponse.class);
    }

    @Override
    public ResponseEntity<AutomationComposition> getCompositionInstance(UUID compositionId, UUID instanceId,
            UUID xonaprequestid) {
        return stubUtils.getResponse(pathToSingleInstance, AutomationComposition.class);
    }

    @Override
    public ResponseEntity<AutomationCompositions> queryCompositionInstances(UUID compositionId, String name,
            String version, Integer page, Integer size, UUID xonaprequestid) {
        return stubUtils.getResponse(pathToAllInstances, AutomationCompositions.class);
    }

    @Override
    public ResponseEntity<Void> compositionInstanceState(UUID compositionId, UUID instanceId,
            @Valid AcInstanceStateUpdate body, UUID requestId) {
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @Override
    public ResponseEntity<Void> rollbackCompositionInstance(UUID compositionId, UUID instanceId) {
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @Override
    public ResponseEntity<AutomationCompositions> queryCompositionInstancesByFilter(
        String compositionIds, String deployState, String stateChangeResult,
        Integer page, Integer size, String sort, String sortOrder, UUID onapRequestId) {
        return stubUtils.getResponse(pathToAllInstances, AutomationCompositions.class);
    }
}
