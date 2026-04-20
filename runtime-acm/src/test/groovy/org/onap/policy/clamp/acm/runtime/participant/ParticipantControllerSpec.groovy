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
package org.onap.policy.clamp.acm.runtime.participant

import org.onap.policy.clamp.acm.runtime.helper.ParticipantControllerTestHelper
import org.onap.policy.clamp.acm.runtime.instantiation.AutomationCompositionInstantiationProvider
import org.onap.policy.clamp.models.acm.concepts.ParticipantInformation
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.ParameterizedTypeReference
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.HttpClientErrorException
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(["test", "default"])
@EmbeddedKafka
@DirtiesContext
class ParticipantControllerSpec extends Specification {

    @Shared
    ParticipantControllerTestHelper helper = new ParticipantControllerTestHelper()

    @LocalServerPort
    int randomServerPort

    @Autowired
    ParticipantProvider participantProvider

    @Autowired
    AcDefinitionProvider acDefinitionProvider

    @Autowired
    AutomationCompositionInstantiationProvider instantiationProvider

    def setup() {
        helper.setProviders(participantProvider, acDefinitionProvider, instantiationProvider)
        helper.initializeRestClient(randomServerPort)
    }

    // -------------------------
    // Unauthorized
    // -------------------------

    def "given an unauthenticated GET request, the server should reject with 401"() {
        when: "a GET request is sent without credentials"
        helper.sendGetNoAuth(helper.endpoint).retrieve().toBodilessEntity()

        then: "a 401 unauthorized response should be returned"
        def ex = thrown(HttpClientErrorException)
        ex.statusCode.value() == 401
    }

    // -------------------------
    // Query single participant
    // -------------------------

    def "given a seeded participant, paginated and non-paginated queries should return correct result sizes"() {
        given: "a participant with seeded data records"
        def participantId = helper.seedParticipantWithData()
        def n = helper.numberRecords

        expect: "paginated queries return the requested page size, non-paginated return all records"
        helper.validatePageable("/$participantId?page=1&size=4", 4)
        helper.validateNotPageable("/$participantId?page=0", n)
        helper.validateNotPageable("/$participantId?size=5", n)
        helper.validateNotPageable("/$participantId", n)
    }

    def "given a random UUID, querying a single participant should return 404"() {
        given: "at least one participant exists in the provider"
        participantProvider.saveParticipant(helper.sampleParticipant())

        expect: "a 404 response when querying with a non-existent participant ID"
        helper.sendGet("${helper.endpoint}/${UUID.randomUUID()}")
                .retrieve().toBodilessEntity()
                .statusCode.value() == 404
    }

    // -------------------------
    // Query all participants
    // -------------------------

    def "given saved participants, querying all should return a list containing all participant IDs"() {
        given: "multiple participants saved in the provider"
        helper.saveInputParticipants()

        when: "querying the participants endpoint"
        def resp = helper.sendGet(helper.endpoint)
                .retrieve()
                .toEntity(new ParameterizedTypeReference<List<ParticipantInformation>>() {})

        then: "the response should contain all saved participant IDs"
        resp.statusCode.value() == 200
        resp.body*.participant*.participantId.containsAll(
                helper.inputParticipants()*.participantId
        )
    }

    def "given seeded participants, paginated and non-paginated queries on all participants should return correct sizes"() {
        given: "a participant with seeded data records"
        helper.seedParticipantWithData()
        def n = helper.numberRecords

        expect: "paginated queries return the requested page size, non-paginated return all records"
        helper.validateAllPageable("?page=1&size=4", 4)
        helper.validateAllNotPageable("?page=0", n)
        helper.validateAllNotPageable("?size=5", n)
        helper.validateAllNotPageable("", n)
    }

    // -------------------------
    // Commands
    // -------------------------

    def "given a saved participant, triggering a report should return 202 accepted"() {
        given: "a single participant saved in the provider"
        def id = helper.saveSingleParticipant()

        expect: "a 202 accepted response when triggering a report"
        helper.sendPut("${helper.endpoint}/$id")
                .body("")
                .retrieve()
                .toBodilessEntity()
                .statusCode.value() == 202
    }

    def "given a random UUID, triggering a report should return 404 not found"() {
        expect: "a 404 response when the participant does not exist"
        helper.sendPut("${helper.endpoint}/${UUID.randomUUID()}")
                .body("")
                .retrieve()
                .toBodilessEntity()
                .statusCode.value() == 404
    }

    def "given saved participants, triggering a report for all should return 202 accepted"() {
        given: "multiple participants saved in the provider"
        helper.saveInputParticipants()

        expect: "a 202 accepted response when triggering a report for all participants"
        helper.sendPut(helper.endpoint)
                .body("")
                .retrieve()
                .toBodilessEntity()
                .statusCode.value() == 202
    }

    @Unroll
    def "given saved participants, sync endpoint '#path' should return #status"() {
        given: "multiple participants saved in the provider"
        helper.saveInputParticipants()

        expect: "the sync endpoint to return the expected HTTP status"
        helper.sendPut("${helper.endpoint}$path")
                .body("")
                .retrieve()
                .toBodilessEntity()
                .statusCode.value() == status

        where:
        path                                          || status
        helper.getSyncPath("syncAll")                 || 202
        helper.getSyncPath("syncKnown")               || 202
        "/sync/${UUID.randomUUID()}"                  || 404
    }
}
