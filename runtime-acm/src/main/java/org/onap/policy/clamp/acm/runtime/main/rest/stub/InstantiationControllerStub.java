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
import org.onap.policy.clamp.acm.runtime.main.rest.gen.AutomationCompositionInstanceApi;
import org.onap.policy.clamp.acm.runtime.main.web.AbstractRestController;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationResponse;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class InstantiationControllerStub extends AbstractRestController implements AutomationCompositionInstanceApi {

    private static final Logger log = LoggerFactory.getLogger(InstantiationControllerStub.class);
    private final StubUtils stubUtils = new StubUtils();
    private final HttpServletRequest request;

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

    @org.springframework.beans.factory.annotation.Autowired
    public InstantiationControllerStub(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public ResponseEntity<InstantiationResponse> createCompositionInstance(
          @PathVariable("compositionId") UUID compositionId,
          @Valid @RequestBody AutomationComposition body,
          @RequestHeader(value = "X-onap-RequestId", required = false) UUID xonaprequestid) {
        return stubUtils.getResponse(pathPostIntance, InstantiationResponse.class, request, log);
    }

    @Override
    public ResponseEntity<InstantiationResponse> deleteCompositionInstance(
          @PathVariable("compositionId") UUID compositionId,
          @PathVariable("instanceId") UUID instanceId,
          @RequestHeader(value = "X-onap-RequestId", required = false) UUID xonaprequestid) {
        return stubUtils.getResponse(pathToResponseFile, InstantiationResponse.class, request, log);
    }

    @Override
    public ResponseEntity<AutomationComposition> getCompositionInstance(
            @PathVariable("compositionId") UUID compositionId,
            @PathVariable("instanceId") UUID instanceId,
            @RequestHeader(value = "X-onap-RequestId", required = false) UUID xonaprequestid) {
        return stubUtils.getResponse(pathToSingleIntance, AutomationComposition.class, request, log);
    }

    @Override
    public ResponseEntity<AutomationCompositions> queryCompositionInstances(
            @PathVariable("compositionId") UUID compositionId,
            @Valid @RequestParam(value = "name", required = false) String name,
            @Valid @RequestParam(value = "version", required = false) String version,
            @RequestHeader(value = "X-onap-RequestId", required = false) UUID xonaprequestid) {
        return stubUtils.getResponse(pathToAllIntances, AutomationCompositions.class, request, log);
    }

    @Override
    public ResponseEntity<InstantiationResponse> updateCompositionInstance(
            UUID compositionId,
            UUID instanceId,
            InstantiationUpdate body,
            UUID xonaprequestid) {
        return stubUtils.getResponse(pathToResponseFile, InstantiationResponse.class, request, log);
    }
}