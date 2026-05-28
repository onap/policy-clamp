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
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import spock.lang.Shared
import spock.lang.Specification

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(["test", "default"])
@EmbeddedKafka
@DirtiesContext
class ParticipantControllerSpec extends Specification {

    @Shared
    def helper = new ParticipantControllerTestHelper()

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

    def "given an unauthenticated #method request to '#endpoint', the server should reject with 401"() {
        when: "a request is sent without credentials"
        requestCall.call()

        then: "a 401 unauthorized response should be returned"
        def ex = thrown(HttpClientErrorException)
        ex.statusCode == HttpStatus.UNAUTHORIZED

        where:
        method | endpoint       | requestCall
        "GET"  | "participants" | { helper.sendGetNoAuth(helper.endpoint).retrieve().toBodilessEntity() }
        "PUT"  | "participants" | { helper.sendPutNoAuth(helper.endpoint).body("").retrieve().toBodilessEntity() }
        "PUT"  | "sync"         | { helper.sendPutNoAuth("${helper.endpoint}/sync").body("").retrieve().toBodilessEntity() }
    }

    // -------------------------
    // Query single participant
    // -------------------------

    def "given a seeded participant, querying by ID should return correct participant details"() {
        given: "a participant with seeded data records"
        def participantId = helper.seedParticipantWithData()

        when: "querying the participant by ID"
        def resp = helper.sendGet("${helper.endpoint}/$participantId")
                .retrieve()
                .toEntity(ParticipantInformation)

        then: "the response contains the correct participant ID"
        resp.statusCode == HttpStatus.OK
        resp.body.participant.participantId == participantId
    }

    def "given a seeded participant, query with '#url' should return #expectedSize"() {
        given: "a participant with seeded data records"
        def participantId = helper.seedParticipantWithData()
        def n = helper.numberRecords

        expect: "the query returns the correct result size"
        pageable ?
                helper.validatePageable("/$participantId$url", expectedSize) :
                helper.validateNotPageable("/$participantId$url", n)

        where:
        url              | pageable || expectedSize
        "?page=1&size=4" | true     || 4
        "?page=0"        | false    || _
        "?size=5"        | false    || _
        ""               | false    || _
    }

    def "given a random UUID, querying a single participant should return 404"() {
        given: "at least one participant exists in the provider"
        participantProvider.saveParticipant(helper.sampleParticipant())

        expect: "a 404 response when querying with a non-existent participant ID"
        helper.sendGet("${helper.endpoint}/${UUID.randomUUID()}")
                .retrieve().toBodilessEntity()
                .statusCode == HttpStatus.NOT_FOUND
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
        resp.statusCode == HttpStatus.OK
        resp.body*.participant*.participantId.containsAll(
                helper.inputParticipants()*.participantId
        )
    }

    def "given saved participants, querying all with filter '#queryParams' should return OK"() {
        given: "multiple participants saved in the provider"
        helper.saveInputParticipants()

        when: "querying with filters"
        def resp = helper.sendGet("${helper.endpoint}?$queryParams")
                .retrieve()
                .toEntity(new ParameterizedTypeReference<List<ParticipantInformation>>() {})

        then: "the response should be successful"
        resp.statusCode == HttpStatus.OK

        where:
        queryParams << ["name=participant1", "name=participant1&version=1.0.0"]
    }

    def "given seeded participants, query all with '#url' should return #expectedSize"() {
        given: "a participant with seeded data records"
        helper.seedParticipantWithData()
        def n = helper.numberRecords

        expect: "the query returns the correct result size"
        pageable ?
                helper.validateAllPageable(url, expectedSize) :
                helper.validateAllNotPageable(url, n)

        where:
        url              | pageable || expectedSize
        "?page=1&size=4" | true     || 4
        "?page=0"        | false    || _
        "?size=5"        | false    || _
        ""               | false    || _
    }

    // -------------------------
    // Commands
    // -------------------------

    def "given saved participants, PUT '#path' should return #status"() {
        given: "participants saved in the provider"
        helper.saveInputParticipants()
        def id = helper.saveSingleParticipant()

        expect: "the endpoint to return the expected HTTP status"
        helper.sendPut("${helper.endpoint}${pathResolver.call(id)}")
                .body("")
                .retrieve()
                .toBodilessEntity()
                .statusCode == status

        where:
        path                   | pathResolver                                          || status
        "/{id} (report)"       | { pid -> "/$pid" }                                    || HttpStatus.ACCEPTED
        "/{random} (report)"   | { pid -> "/${UUID.randomUUID()}" }                     || HttpStatus.NOT_FOUND
        " (report all)"        | { pid -> "" }                                         || HttpStatus.ACCEPTED
        "/sync"                | { pid -> "/sync" }                                    || HttpStatus.ACCEPTED
        "/sync/{id}"           | { pid -> "/sync/82fd8ef9-1d1e-4343-9b28-7f9564ee3de6" } || HttpStatus.ACCEPTED
        "/sync/{random}"       | { pid -> "/sync/${UUID.randomUUID()}" }                || HttpStatus.NOT_FOUND
    }
}
