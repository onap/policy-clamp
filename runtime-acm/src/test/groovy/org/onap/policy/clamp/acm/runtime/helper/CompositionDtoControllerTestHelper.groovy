/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.runtime.helper

import org.onap.policy.clamp.acm.runtime.util.rest.CommonRestClient
import org.onap.policy.clamp.models.acm.dto.CompositionDtos
import org.springframework.http.HttpStatus

class CompositionDtoControllerTestHelper {

    static final String ENDPOINT_PATTERN = "participants/%s/compositions"

    final CommonRestClient client = new CommonRestClient()

    void initializeRestClient(int port) {
        client.initializeRestClient(port)
    }

    void testSwagger() {
        client.testSwagger(String.format(endpointPattern, "{participantId}"))
    }

    static def getEndpointPattern() {
        return ENDPOINT_PATTERN
    }

    void assertUnauthorizedGet(UUID id) {
        client.assertUnauthorizedGet(endpoint(id))
    }

    static def endpoint(UUID participantId, UUID compositionId = null) {
        def base = String.format(endpointPattern, participantId)
        return compositionId ? "$base/$compositionId" : base
    }

    def querySize(String url) {
        def resp = client.sendGet(url).retrieve().toEntity(CompositionDtos)
        assert resp.statusCode.value() == HttpStatus.OK.value()
        return resp.body.compositions().size()
    }

    def sendGet(String url) {
        return client.sendGet(url)
    }
}
