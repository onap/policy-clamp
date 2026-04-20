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
package org.onap.policy.clamp.acm.runtime.instantiation

import org.onap.policy.clamp.acm.runtime.util.CommonTestData
import org.onap.policy.clamp.acm.runtime.util.rest.CommonRestClient
import org.onap.policy.clamp.models.acm.concepts.AcTypeState
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions
import org.onap.policy.clamp.models.acm.concepts.DeployState
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.AcInstanceStateUpdate
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationResponse
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider
import org.onap.policy.models.base.PfUtils
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.data.domain.Pageable
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared
import spock.lang.Specification

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka
@ActiveProfiles(["test", "default"])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class InstantiationControllerSpec extends Specification {

    static final int NUM_INSTANCES = 10
    static final AC_CREATE_JSON = "src/test/resources/rest/acm/AutomationComposition.json"
    static final AC_UPDATE_JSON = "src/test/resources/rest/acm/AutomationCompositionUpdate.json"
    static final AC_VERSIONING_YAML = "src/test/resources/rest/acm/AutomationCompositionVersioning.yaml"
    static final ENDPOINT = "compositions/%s/instances"
    static final NODE_TYPE = "org.onap.policy.clamp.acm.AutomationComposition"

    @Shared
    CommonRestClient client = new CommonRestClient()

    @Autowired
    AcDefinitionProvider acDefinitionProvider

    @Autowired
    ParticipantProvider participantProvider

    @Autowired
    AutomationCompositionInstantiationProvider instantiationProvider

    @LocalServerPort
    int port

    @Shared
    ToscaServiceTemplate serviceTemplate

    def setupSpec() {
        serviceTemplate = InstantiationUtils.getToscaServiceTemplate(CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML)
    }

    def setup() {
        client.initializeRestClient(port)
    }

    // --- Helpers ---

    private String endpoint(UUID compositionId, UUID instanceId = null) {
        def base = String.format(ENDPOINT, compositionId)
        instanceId ? "$base/$instanceId" : base
    }

    private UUID createDefinition(String name) {
        createDefinition(new ToscaServiceTemplate(serviceTemplate).tap { it.name = name })
    }

    private UUID createDefinition(ToscaServiceTemplate template) {
        def acmDef = CommonTestData.createAcDefinition(template, AcTypeState.PRIMED)
        acDefinitionProvider.updateAcDefinition(acmDef, NODE_TYPE)
        def participant = CommonTestData.createParticipant(CommonTestData.participantId)
        def replica = CommonTestData.createParticipantReplica(CommonTestData.replicaId)
        participant.replicas[replica.replicaId] = replica
        participantProvider.saveParticipant(participant)
        acmDef.compositionId
    }

    private InstantiationResponse postAc(UUID compositionId, AutomationComposition ac, int expectedStatus) {
        def resp = client.sendPost(endpoint(compositionId)).body(ac).retrieve().toEntity(InstantiationResponse)
        assert resp.statusCode.value() == expectedStatus
        resp.body
    }

    private int querySize(String url) {
        def resp = client.sendGet(url).retrieve().toEntity(AutomationCompositions)
        assert resp.statusCode.value() == 200
        resp.body.automationCompositionList.size()
    }

    // --- Swagger ---

    def "swagger contains instantiation path"() {
        expect:
        client.testSwagger(String.format(ENDPOINT, "{compositionId}"))
    }

    // --- Unauthorized ---

    def "unauthorized requests are rejected"() {
        given:
        def ac = InstantiationUtils.getAutomationCompositionFromResource(AC_CREATE_JSON, "Unauthorized")
        def id = UUID.randomUUID()

        expect:
        client.assertUnauthorizedPost(endpoint(id), ac)
        client.assertUnauthorizedGet(endpoint(id))
        client.assertUnauthorizedPut(endpoint(id),
                InstantiationUtils.getAutomationCompositionFromResource(AC_UPDATE_JSON, "Unauthorized"))
        client.assertUnauthorizedDelete(endpoint(id))
    }

    // --- Create ---

    def "create succeeds and persists correctly"() {
        given:
        def compositionId = createDefinition("Create")
        def ac = InstantiationUtils.getAutomationCompositionFromResource(AC_CREATE_JSON, "Create")
        ac.compositionId = compositionId

        when:
        def resp = postAc(compositionId, ac, 201)

        then:
        resp.errorDetails == null
        resp.affectedAutomationComposition == PfUtils.getKey(ac).asIdentifier()

        when:
        ac.instanceId = resp.instanceId
        ac.elements.values().each { it.participantId = CommonTestData.participantId }
        def fromDb = instantiationProvider.getAutomationComposition(compositionId, resp.instanceId)
        ac.lastMsg = fromDb.lastMsg

        then:
        ac == fromDb
    }

    def "create returns 400 when AC already defined"() {
        given:
        def compositionId = createDefinition("CreateBadRequest")
        def ac = InstantiationUtils.getAutomationCompositionFromResource(AC_CREATE_JSON, "CreateBadRequest")
        ac.compositionId = compositionId
        postAc(compositionId, ac, 201)

        when:
        def resp = postAc(compositionId, ac, 400)

        then:
        resp.errorDetails != null
        resp.affectedAutomationComposition == null
    }

    def "create returns 400 when elements are null"() {
        given:
        def compositionId = createDefinition("CreateBadRequestNoEl")
        def ac = InstantiationUtils.getAutomationCompositionFromResource(AC_CREATE_JSON, "CreateBadRequest")
        ac.compositionId = compositionId
        ac.elements = null

        when:
        def resp = postAc(compositionId, ac, 400)

        then:
        resp.errorDetails != null
        resp.affectedAutomationComposition == null
    }

    // --- Versioning ---

    def "versioned AC creation succeeds"() {
        given:
        def templateVer = InstantiationUtils.getToscaServiceTemplate(CommonTestData.TOSCA_VERSIONING)
        def compositionId = createDefinition(templateVer)
        def ac = InstantiationUtils.getAutomationCompositionFromYaml(AC_VERSIONING_YAML, "Versioning")
        ac.compositionId = compositionId

        when:
        def resp = postAc(compositionId, ac, 201)

        then:
        resp.errorDetails == null
        resp.affectedAutomationComposition == PfUtils.getKey(ac).asIdentifier()
    }

    // --- Query ---

    def "query returns empty when name not found"() {
        expect:
        querySize("${endpoint(UUID.randomUUID())}?name=noResultWithThisName") == 0
    }

    def "query returns matching AC"() {
        given:
        def compositionId = createDefinition("Query")
        def ac = InstantiationUtils.getAutomationCompositionFromResource(AC_CREATE_JSON, "Query")
        ac.compositionId = compositionId
        instantiationProvider.createAutomationComposition(compositionId, ac)

        when:
        def resp = client.sendGet("${endpoint(compositionId)}?name=${PfUtils.getKey(ac).name}")
                .retrieve().toEntity(AutomationCompositions)
        def acResult = resp.body.automationCompositionList[0]
        ac.lastMsg = acResult.lastMsg

        then:
        resp.body.automationCompositionList.size() == 1
        ac == acResult
    }

    def "query with pagination"() {
        given:
        def compositionId = createDefinition("Query")
        def ac = InstantiationUtils.getAutomationCompositionFromResource(AC_CREATE_JSON, "Query")
        ac.compositionId = compositionId
        NUM_INSTANCES.times { ac.name = "acm_$it"; instantiationProvider.createAutomationComposition(compositionId, ac) }
        def ep = endpoint(compositionId)

        expect: "pageable queries"
        querySize("$ep?name=wrong_name") == 0
        querySize("$ep?name=acm_1") == 1
        querySize("$ep?page=1&size=4") == 4

        and: "non-pageable queries return all"
        querySize("$ep?page=0") >= NUM_INSTANCES
        querySize("$ep?size=5") >= NUM_INSTANCES
        querySize("$ep") >= NUM_INSTANCES
    }

    // --- Get ---

    def "get AC by id"() {
        given:
        def compositionId = createDefinition("Get")
        def ac = InstantiationUtils.getAutomationCompositionFromResource(AC_CREATE_JSON, "Get")
        ac.compositionId = compositionId
        instantiationProvider.createAutomationComposition(compositionId, ac)

        when:
        def resp = client.sendGet(endpoint(compositionId, ac.instanceId)).retrieve().toEntity(AutomationComposition)
        ac.lastMsg = resp.body.lastMsg

        then:
        resp.statusCode.value() == 200
        ac == resp.body
    }

    // --- Update ---

    def "update succeeds and persists"() {
        given:
        def compositionId = createDefinition("Update")
        def acCreate = InstantiationUtils.getAutomationCompositionFromResource(AC_CREATE_JSON, "Update")
        acCreate.compositionId = compositionId
        def createResp = instantiationProvider.createAutomationComposition(compositionId, acCreate)

        def acUpdate = InstantiationUtils.getAutomationCompositionFromResource(AC_UPDATE_JSON, "Update")
        acUpdate.compositionId = compositionId
        acUpdate.instanceId = createResp.instanceId
        acUpdate.elements.values().each { it.participantId = CommonTestData.participantId }

        when:
        def resp = client.sendPost(endpoint(compositionId)).body(acUpdate).retrieve().toEntity(InstantiationResponse)

        then:
        resp.statusCode.value() == 200
        resp.body.errorDetails == null
        resp.body.affectedAutomationComposition == PfUtils.getKey(acUpdate).asIdentifier()

        when:
        def key = PfUtils.getKey(acUpdate)
        def fromDb = instantiationProvider.getAutomationCompositions(compositionId, key.name, key.version, Pageable.unpaged())
        def acFromDb = fromDb.automationCompositionList[0]
        acUpdate.lastMsg = acFromDb.lastMsg
        acUpdate.revisionId = acFromDb.revisionId

        then:
        acUpdate == acFromDb
    }

    // --- Delete ---

    def "delete succeeds"() {
        given:
        def compositionId = createDefinition("Delete")
        def ac = InstantiationUtils.getAutomationCompositionFromResource(AC_CREATE_JSON, "Delete")
        ac.compositionId = compositionId
        def instResp = instantiationProvider.createAutomationComposition(compositionId, ac)

        when:
        def resp = client.sendDelete(endpoint(compositionId, instResp.instanceId))
                .retrieve().toEntity(InstantiationResponse)

        then:
        resp.statusCode.value() == 202
        resp.body.affectedAutomationComposition == PfUtils.getKey(ac).asIdentifier()

        and:
        def key = PfUtils.getKey(ac)
        def fromDb = instantiationProvider.getAutomationCompositions(compositionId, key.name, key.version, Pageable.unpaged())
        fromDb.automationCompositionList[0].deployState == DeployState.DELETING
    }

    def "delete returns 404 for unknown instance"() {
        given:
        def compositionId = createDefinition("DeleteNotFound")
        def ac = InstantiationUtils.getAutomationCompositionFromResource(AC_CREATE_JSON, "DelNotFound")
        ac.compositionId = compositionId
        instantiationProvider.createAutomationComposition(compositionId, ac)

        when:
        def resp = client.sendDelete(endpoint(compositionId, UUID.randomUUID())).retrieve().toBodilessEntity()

        then:
        resp.statusCode.value() == 404
    }

    // --- Rollback ---

    def "rollback returns 404 for unknown instance"() {
        given:
        def compositionId = createDefinition("RollbackNotFound")

        when:
        def resp = client.sendPost("${endpoint(compositionId, UUID.randomUUID())}/rollback")
                .body("").retrieve().toBodilessEntity()

        then:
        resp.statusCode.value() == 404
    }

    def "rollback returns 400 for invalid state"() {
        given:
        def compositionId = createDefinition("RollbackNotFound")
        def ac = InstantiationUtils.getAutomationCompositionFromResource(AC_CREATE_JSON, "NotValid")
        ac.compositionId = compositionId
        def instResp = instantiationProvider.createAutomationComposition(compositionId, ac)

        when:
        def resp = client.sendPost("${endpoint(compositionId, instResp.instanceId)}/rollback")
                .body("").retrieve().toBodilessEntity()

        then:
        resp.statusCode.value() == 400
    }

    // --- Deploy ---

    def "deploy returns 404 for unknown instance"() {
        given:
        def compositionId = createDefinition("Deploy_NotFound")

        when:
        def resp = client.sendPut(endpoint(compositionId, UUID.randomUUID()))
                .body(new AcInstanceStateUpdate()).retrieve().toBodilessEntity()

        then:
        resp.statusCode.value() == 404
    }

    def "deploy returns 400 when orders are null"() {
        given:
        def compositionId = createDefinition("Deploy_BadRequest")
        def ac = InstantiationUtils.getAutomationCompositionFromResource(AC_CREATE_JSON, "BadRequest")
        ac.compositionId = compositionId
        def instResp = instantiationProvider.createAutomationComposition(compositionId, ac)

        when:
        def resp = client.sendPut(endpoint(compositionId, instResp.instanceId))
                .body(new AcInstanceStateUpdate(deployOrder: null, lockOrder: null))
                .retrieve().toBodilessEntity()

        then:
        resp.statusCode.value() == 400
    }

    def "deploy succeeds with valid order"() {
        given:
        def compositionId = createDefinition("Deploy")
        def ac = InstantiationUtils.getAutomationCompositionFromResource(AC_CREATE_JSON, "Command")
        ac.compositionId = compositionId
        def instResp = instantiationProvider.createAutomationComposition(compositionId, ac)

        when:
        def resp = client.sendPut(endpoint(compositionId, instResp.instanceId))
                .body(new AcInstanceStateUpdate(deployOrder: DeployOrder.DEPLOY, lockOrder: null))
                .retrieve().toBodilessEntity()

        then:
        resp.statusCode.value() == 202
    }

    // --- Query by filter ---

    def "query by filter without instance IDs"() {
        given:
        def compositionId = createDefinition("Query")
        def ac = InstantiationUtils.getAutomationCompositionFromResource(AC_CREATE_JSON, "Query")
        ac.compositionId = compositionId
        NUM_INSTANCES.times { ac.name = "acmr_$it"; instantiationProvider.createAutomationComposition(compositionId, ac) }

        expect:
        querySize("instances") == 10
        querySize("instances?page=1&size=4") == 4
        querySize("instances?size=4") == 10
        querySize("instances?stateChangeResult=FAILED,TIMEOUT") == 0
        querySize("instances?deployState=UNDEPLOYED") == 10
        querySize("instances?stateChangeResult=NO_ERROR&deployState=UNDEPLOYED") == 10
        querySize("instances?sort=name&sortOrder=DESC") == 10
    }

    def "query by filter with instance IDs"() {
        given:
        def compositionId = createDefinition("Query")
        def ac = InstantiationUtils.getAutomationCompositionFromResource(AC_CREATE_JSON, "Query")
        ac.compositionId = compositionId
        def ids = (0..<NUM_INSTANCES).collect {
            ac.name = "acmr_$it"
            instantiationProvider.createAutomationComposition(compositionId, ac).instanceId
        }.join(",")

        expect:
        querySize("instances?instanceIds=$ids") == 10
        querySize("instances?page=1&size=4&instanceIds=$ids") == 4
        querySize("instances?size=4&instanceIds=$ids") == 10
        querySize("instances?stateChangeResult=FAILED,TIMEOUT&instanceIds=$ids") == 0
        querySize("instances?deployState=UNDEPLOYED&instanceIds=$ids") == 10
        querySize("instances?sort=name&sortOrder=DESC&instanceIds=$ids") == 10
    }
}
