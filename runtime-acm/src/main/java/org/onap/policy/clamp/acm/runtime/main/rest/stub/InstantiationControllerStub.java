/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation.
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
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.runtime.main.rest.gen.AutomationCompositionInstanceApi;
import org.onap.policy.clamp.acm.runtime.main.web.AbstractRestController;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.AcInstanceStateUpdate;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
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
    private String pathToSingleIntance;

    @Value("${stub.getAllCompositionInstancesResponse}")
    private String pathToAllIntances;

    @Value("${stub.postInstanceResponse}")
    private String pathPostIntance;

    @Value("${stub.putCompositionInstanceUpdateResponse}")
    private String pathToPutUpdate;

    @Override
    public ResponseEntity<InstantiationResponse> createCompositionInstance(UUID compositionId,
            AutomationComposition body, UUID xonaprequestid) {
        if (body.getInstanceId() == null) {
            return stubUtils.getResponse(pathPostIntance, InstantiationResponse.class);
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
        return stubUtils.getResponse(pathToSingleIntance, AutomationComposition.class);
    }

    @Override
    public ResponseEntity<AutomationCompositions> queryCompositionInstances(UUID compositionId, String name,
            String version, UUID xonaprequestid) {
        return stubUtils.getResponse(pathToAllIntances, AutomationCompositions.class);
    }

    @Override
    public ResponseEntity<Void> compositionInstanceState(UUID compositionId, UUID instanceId,
            @Valid AcInstanceStateUpdate body, UUID requestId) {
        // TODO Auto-generated method stub
        return null;
    }
}
