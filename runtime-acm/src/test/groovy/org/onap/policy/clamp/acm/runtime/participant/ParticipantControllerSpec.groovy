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

import org.onap.policy.clamp.acm.runtime.instantiation.AutomationCompositionInstantiationProvider
import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils
import org.onap.policy.clamp.acm.runtime.util.CommonTestData
import org.onap.policy.clamp.acm.runtime.util.rest.CommonRestClient
import org.onap.policy.clamp.models.acm.concepts.AcTypeState
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition
import org.onap.policy.clamp.models.acm.concepts.Participant
import org.onap.policy.clamp.models.acm.concepts.ParticipantInformation
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider
import org.onap.policy.common.utils.resources.ResourceUtils
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
    CommonRestClient client = new CommonRestClient()

    @LocalServerPort
    int randomServerPort

    @Autowired
    ParticipantProvider participantProvider

    @Autowired
    AcDefinitionProvider acDefinitionProvider

    @Autowired
    AutomationCompositionInstantiationProvider instantiationProvider

    static final String ENDPOINT = "participants"
    private static final String AC_INSTANTIATION_CREATE_JSON = "rest/acm/AutomationComposition.json"

    private static AutomationComposition loadAutomationComposition(String suffix) {
        def json = ResourceUtils.getResourceAsString(AC_INSTANTIATION_CREATE_JSON)
        def ac = CommonTestData.getObjectFromJson(json, AutomationComposition)
        ac.name = ac.name + suffix
        return ac
    }
    static final String NODE_TYPE = "org.onap.policy.clamp.acm.AutomationComposition"
    static final int NUMBER_RECORDS = 10
    static int seedCounter = 0

    def setup() {
        client.initializeRestClient(randomServerPort)
    }

    // -------------------------
    // Unauthorized
    // -------------------------

    def "should reject unauthorized query"() {
        when:
        client.sendGetNoAuth(ENDPOINT).retrieve().toBodilessEntity()

        then:
        def ex = thrown(HttpClientErrorException)
        ex.statusCode.value() == 401
    }

    // -------------------------
    // Query single participant
    // -------------------------

    def "should query participant with pagination"() {
        given:
        def participantId = seedParticipantWithData()

        expect:
        validatePageable("/$participantId?page=1&size=4", 4)
        validateNotPageable("/$participantId?page=0", NUMBER_RECORDS)
        validateNotPageable("/$participantId?size=5", NUMBER_RECORDS)
        validateNotPageable("/$participantId", NUMBER_RECORDS)
    }

    def "should return 404 for unknown participant"() {
        given:
        participantProvider.saveParticipant(sampleParticipant())

        expect:
        client.sendGet("$ENDPOINT/${UUID.randomUUID()}")
                .retrieve().toBodilessEntity()
                .statusCode.value() == 404
    }

    // -------------------------
    // Query all participants
    // -------------------------

    def "should return all participants"() {
        given:
        inputParticipants().each { participantProvider.saveParticipant(it) }

        when:
        def resp = client.sendGet(ENDPOINT)
                .retrieve()
                .toEntity(new ParameterizedTypeReference<List<ParticipantInformation>>() {})

        then:
        resp.statusCode.value() == 200
        resp.body*.participant*.participantId.containsAll(
                inputParticipants()*.participantId
        )
    }

    def "should paginate all participants"() {
        given:
        seedParticipantWithData()

        expect:
        validateAllPageable("?page=1&size=4", 4)
        validateAllNotPageable("?page=0", NUMBER_RECORDS)
        validateAllNotPageable("?size=5", NUMBER_RECORDS)
        validateAllNotPageable("", NUMBER_RECORDS)
    }

    // -------------------------
    // Commands
    // -------------------------

    def "should trigger participant report"() {
        given:
        def id = saveSingleParticipant()

        expect:
        client.sendPut("$ENDPOINT/$id")
                .body("")
                .retrieve()
                .toBodilessEntity()
                .statusCode.value() == 202
    }

    def "should fail report for unknown participant"() {
        expect:
        client.sendPut("$ENDPOINT/${UUID.randomUUID()}")
                .body("")
                .retrieve()
                .toBodilessEntity()
                .statusCode.value() == 404
    }

    def "should trigger report for all participants"() {
        given:
        inputParticipants().each { participantProvider.saveParticipant(it) }

        expect:
        client.sendPut(ENDPOINT)
                .body("")
                .retrieve()
                .toBodilessEntity()
                .statusCode.value() == 202
    }

    @Unroll
    def "sync endpoint '#path' should return #status"() {
        given:
        inputParticipants().each { participantProvider.saveParticipant(it) }

        expect:
        client.sendPut("$ENDPOINT$path")
                .body("")
                .retrieve()
                .toBodilessEntity()
                .statusCode.value() == status

        where:
        path                          || status
        "/sync"                       || 202
        "/sync/82fd8ef9-1d1e-4343-9b28-7f9564ee3de6" || 202
        "/sync/${UUID.randomUUID()}" || 404
    }

    // -------------------------
    // Helpers
    // -------------------------

    private UUID seedParticipantWithData() {
        def participant = sampleParticipant()
        participantProvider.saveParticipant(participant)
        def prefix = "test_${seedCounter++}_"

        (0..<NUMBER_RECORDS).each {
            createAcDefinition("${prefix}$it")
        }

        participant.participantId
    }

    private Participant sampleParticipant() {
        def p = CommonTestData.createParticipant(CommonTestData.participantId)
        def r = CommonTestData.createParticipantReplica(CommonTestData.participantId)
        p.replicas[r.replicaId] = r
        return p
    }

    private List<Participant> inputParticipants() {
        [
                CommonTestData.getObjectFromJson(
                        ResourceUtils.getResourceAsString("src/test/resources/providers/TestParticipant.json"),
                        Participant
                ),
                CommonTestData.getObjectFromJson(
                        ResourceUtils.getResourceAsString("src/test/resources/providers/TestParticipant2.json"),
                        Participant
                )
        ]
    }

    private void validatePageable(String url, int expected) {
        def info = getParticipantInfo(url)
        assert info.acNodeTemplateStateDefinitionMap.size() == expected
        assert info.acElementInstanceMap.size() == expected
    }

    private void validateNotPageable(String url, int expected) {
        def info = getParticipantInfo(url)
        assert info.acNodeTemplateStateDefinitionMap.size() >= expected
        assert info.acElementInstanceMap.size() >= expected
    }

    private void validateAllPageable(String url, int expected) {
        def info = getFirstParticipantInfo(url)
        assert info.acNodeTemplateStateDefinitionMap.size() == expected
    }

    private void validateAllNotPageable(String url, int expected) {
        def info = getFirstParticipantInfo(url)
        assert info.acNodeTemplateStateDefinitionMap.size() >= expected
    }

    private ParticipantInformation getParticipantInfo(String url) {
        def resp = client.sendGet("$ENDPOINT$url")
                .retrieve().toEntity(ParticipantInformation)

        assert resp.statusCode.value() == 200
        resp.body
    }

    private ParticipantInformation getFirstParticipantInfo(String url) {
        def resp = client.sendGet("$ENDPOINT$url")
                .retrieve()
                .toEntity(new ParameterizedTypeReference<List<ParticipantInformation>>() {})

        assert resp.statusCode.value() == 200

        resp.body.find {
            it.participant.participantId == CommonTestData.participantId
        }
    }

    private void createAcDefinition(String name) {
        def serviceTemplate = InstantiationUtils.getToscaServiceTemplate(CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML)
        serviceTemplate.name = name

        def defn = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED)
        acDefinitionProvider.updateAcDefinition(defn, NODE_TYPE)

        def ac = new AutomationComposition(loadAutomationComposition(name))
        ac.compositionId = defn.compositionId
        ac.name = name
        def elements = new ArrayList<>(ac.elements.values())
        ac.elements.clear()
        elements.each {
            it.id = UUID.randomUUID()
            ac.elements[it.id] = it
        }

        instantiationProvider.createAutomationComposition(defn.compositionId, ac)
    }

    private UUID saveSingleParticipant() {
        def p = sampleParticipant()
        participantProvider.saveParticipant(p)
        p.participantId
    }
}