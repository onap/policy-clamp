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
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.onap.policy.clamp.acm.runtime.main.rest.gen.AutomationCompositionDefinitionApi;
import org.onap.policy.clamp.acm.runtime.main.web.AbstractRestController;
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.AcTypeStateUpdate;
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.CommissioningResponse;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("stub")
public class CommissioningControllerStub extends AbstractRestController
    implements AutomationCompositionDefinitionApi {

    private static final Logger log = LoggerFactory.getLogger(CommissioningControllerStub.class);

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private StubUtils stubUtils;

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
    public ResponseEntity<CommissioningResponse> createCompositionDefinitions(
            @Valid @RequestBody ToscaServiceTemplate body,
            @RequestHeader(value = "X-onap-RequestId", required = false) UUID xonaprequestid) {
        return stubUtils.getResponse(pathToPostResponse, CommissioningResponse.class, request, log);
    }

    @Override
    public ResponseEntity<CommissioningResponse> deleteCompositionDefinition(
            @PathVariable("compositionId") UUID compositionId,
            @RequestHeader(value = "X-onap-RequestId", required = false) UUID xonaprequestid) {
        return stubUtils.getResponse(pathToResponseFile, CommissioningResponse.class, request, log);
    }

    @Override
    public ResponseEntity<ToscaServiceTemplate> getCompositionDefinition(
            @PathVariable("compositionId") UUID compositionId,
            @RequestHeader(value = "X-onap-RequestId", required = false) UUID xonaprequestid) {
        return stubUtils.getResponse(pathToSingleDefinition, ToscaServiceTemplate.class, request, log);
    }

    @Override
    public ResponseEntity<ToscaServiceTemplates> queryCompositionDefinitions(
            @Valid @RequestParam(value = "name", required = false) String name,
            @Valid @RequestParam(value = "version", required = false) String version,
            @RequestHeader(value = "X-onap-RequestId", required = false) UUID xonaprequestid) {
        return stubUtils.getResponse(pathToAllDefinitions, ToscaServiceTemplates.class, request, log);
    }

    public ResponseEntity<CommissioningResponse> updateCompositionDefinition(
            @PathVariable("compositionId") UUID compositionId,
            @Valid @RequestBody ToscaServiceTemplate body,
            @RequestHeader(value = "X-onap-RequestId", required = false) UUID xonaprequestid) {
        return stubUtils.getResponse(pathToPutUpdate, CommissioningResponse.class, request, log);
    }

    @Override
    public ResponseEntity<Void> compositionDefinitionPriming(UUID compositionId, UUID requestId,
        AcTypeStateUpdate body) {
        // TODO Auto-generated method stub
        return null;
    }
}
