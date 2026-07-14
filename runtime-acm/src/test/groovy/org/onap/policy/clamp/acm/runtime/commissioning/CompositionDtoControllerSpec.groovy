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

package org.onap.policy.clamp.acm.runtime.commissioning

import org.onap.policy.clamp.acm.runtime.helper.CompositionDtoControllerTestHelper
import org.onap.policy.clamp.acm.runtime.helper.InstantiationControllerTestHelper
import org.onap.policy.clamp.acm.runtime.util.CommonTestData
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions
import org.onap.policy.clamp.models.acm.dto.CompositionDto
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider
import org.onap.policy.models.base.PfUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka
@ActiveProfiles(["test", "default"])
@DirtiesContext
class CompositionDtoControllerSpec extends Specification {

    def helper = new CompositionDtoControllerTestHelper()
    def instantiationHelper = new InstantiationControllerTestHelper()

    @Autowired
    AcDefinitionProvider acDefinitionProvider

    @Autowired
    ParticipantProvider participantProvider

    @LocalServerPort
    int port

    def setup() {
        instantiationHelper.setProviders(acDefinitionProvider, participantProvider)
        instantiationHelper.initializeRestClient(port)
        helper.initializeRestClient(port)
    }

    def "swagger should be accessible and return API docs"() {
        expect:
        helper.testSwagger()
    }

    def "unauthenticated GET request should be rejected with 401"() {
        given:
        def id = UUID.randomUUID()

        expect:
        helper.assertUnauthorizedGet(id)
    }

    def "query returns empty when participant not found"() {
        expect:
        helper.querySize(helper.endpoint(UUID.randomUUID())) == 0
    }

    def "query returns matching CompositionDto"() {
        given:
        def compositionId = instantiationHelper.createDefinition("Query")

        when:
        def resp = helper.sendGet(helper.endpoint(CommonTestData.participantId, compositionId))
                .retrieve().toEntity(CompositionDto)
        def acResult = resp.body

        then:
        acResult.compositionId() == compositionId
    }

}
