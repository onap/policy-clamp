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

import org.onap.policy.clamp.acm.runtime.helper.InstantiationControllerTestHelper
import org.onap.policy.clamp.acm.runtime.util.CommonTestData
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions
import org.onap.policy.clamp.models.acm.concepts.DeployState
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.AcInstanceStateUpdate
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationResponse
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider
import org.onap.policy.models.base.PfUtils
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

    @Shared
    InstantiationControllerTestHelper helper = new InstantiationControllerTestHelper()

    @Autowired
    AcDefinitionProvider acDefinitionProvider

    @Autowired
    ParticipantProvider participantProvider

    @Autowired
    AutomationCompositionInstantiationProvider instantiationProvider

    @LocalServerPort
    int port

    def setup() {
        helper.setProviders(acDefinitionProvider, participantProvider)
        helper.initializeRestClient(port)
    }

    // --- Swagger ---

    def "swagger contains instantiation path"() {
        expect:
        helper.testSwagger()
    }

    // --- Unauthorized ---

    def "unauthorized requests are rejected"() {
        given:
        def ac = helper.loadAc("acCreate", "Unauthorized")
        def id = UUID.randomUUID()

        expect:
        helper.assertUnauthorizedPost(id, ac)
        helper.assertUnauthorizedGet(id)
        helper.assertUnauthorizedPut(id, helper.loadAc("acUpdate", "Unauthorized"))
        helper.assertUnauthorizedDelete(id)
    }

    // --- Create ---

    def "create succeeds and persists correctly"() {
        given:
        def compositionId = helper.createDefinition("Create")
        def ac = helper.loadAc("acCreate", "Create")
        ac.compositionId = compositionId

        when:
        def resp = helper.postAc(compositionId, ac, 201)

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
        def compositionId = helper.createDefinition("CreateBadRequest")
        def ac = helper.loadAc("acCreate", "CreateBadRequest")
        ac.compositionId = compositionId
        helper.postAc(compositionId, ac, 201)

        when:
        def resp = helper.postAc(compositionId, ac, 400)

        then:
        resp.errorDetails != null
        resp.affectedAutomationComposition == null
    }

    def "create returns 400 when elements are null"() {
        given:
        def compositionId = helper.createDefinition("CreateBadRequestNoEl")
        def ac = helper.loadAc("acCreate", "CreateBadRequest")
        ac.compositionId = compositionId
        ac.elements = null

        when:
        def resp = helper.postAc(compositionId, ac, 400)

        then:
        resp.errorDetails != null
        resp.affectedAutomationComposition == null
    }

    // --- Versioning ---

    def "versioned AC creation succeeds"() {
        given:
        def templateVer = InstantiationUtils.getToscaServiceTemplate(CommonTestData.TOSCA_VERSIONING)
        def compositionId = helper.createDefinition(templateVer)
        def ac = helper.loadAcFromYaml("acVersioning", "Versioning")
        ac.compositionId = compositionId

        when:
        def resp = helper.postAc(compositionId, ac, 201)

        then:
        resp.errorDetails == null
        resp.affectedAutomationComposition == PfUtils.getKey(ac).asIdentifier()
    }

    // --- Query ---

    def "query returns empty when name not found"() {
        expect:
        helper.querySize("${helper.endpoint(UUID.randomUUID())}?name=noResultWithThisName") == 0
    }

    def "query returns matching AC"() {
        given:
        def compositionId = helper.createDefinition("Query")
        def ac = helper.loadAc("acCreate", "Query")
        ac.compositionId = compositionId
        instantiationProvider.createAutomationComposition(compositionId, ac)

        when:
        def resp = helper.sendGet("${helper.endpoint(compositionId)}?name=${PfUtils.getKey(ac).name}")
                .retrieve().toEntity(AutomationCompositions)
        def acResult = resp.body.automationCompositionList[0]
        ac.lastMsg = acResult.lastMsg

        then:
        resp.body.automationCompositionList.size() == 1
        ac == acResult
    }

    def "query with pagination"() {
        given:
        def compositionId = helper.createDefinition("Query")
        def ac = helper.loadAc("acCreate", "Query")
        ac.compositionId = compositionId
        def n = helper.numInstances
        n.times { ac.name = "acm_$it"; instantiationProvider.createAutomationComposition(compositionId, ac) }
        def ep = helper.endpoint(compositionId)

        expect: "pageable queries"
        helper.querySize("$ep?name=wrong_name") == 0
        helper.querySize("$ep?name=acm_1") == 1
        helper.querySize("$ep?page=1&size=4") == 4

        and: "non-pageable queries return all"
        helper.querySize("$ep?page=0") >= n
        helper.querySize("$ep?size=5") >= n
        helper.querySize("$ep") >= n
    }

    // --- Get ---

    def "get AC by id"() {
        given:
        def compositionId = helper.createDefinition("Get")
        def ac = helper.loadAc("acCreate", "Get")
        ac.compositionId = compositionId
        instantiationProvider.createAutomationComposition(compositionId, ac)

        when:
        def resp = helper.sendGet(helper.endpoint(compositionId, ac.instanceId)).retrieve().toEntity(AutomationComposition)
        ac.lastMsg = resp.body.lastMsg

        then:
        resp.statusCode.value() == 200
        ac == resp.body
    }

    // --- Update ---

    def "update succeeds and persists"() {
        given:
        def compositionId = helper.createDefinition("Update")
        def acCreate = helper.loadAc("acCreate", "Update")
        acCreate.compositionId = compositionId
        def createResp = instantiationProvider.createAutomationComposition(compositionId, acCreate)

        def acUpdate = helper.loadAc("acUpdate", "Update")
        acUpdate.compositionId = compositionId
        acUpdate.instanceId = createResp.instanceId
        acUpdate.elements.values().each { it.participantId = CommonTestData.participantId }

        when:
        def resp = helper.sendPost(helper.endpoint(compositionId)).body(acUpdate).retrieve().toEntity(InstantiationResponse)

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
        def compositionId = helper.createDefinition("Delete")
        def ac = helper.loadAc("acCreate", "Delete")
        ac.compositionId = compositionId
        def instResp = instantiationProvider.createAutomationComposition(compositionId, ac)

        when:
        def resp = helper.sendDelete(helper.endpoint(compositionId, instResp.instanceId))
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
        def compositionId = helper.createDefinition("DeleteNotFound")
        def ac = helper.loadAc("acCreate", "DelNotFound")
        ac.compositionId = compositionId
        instantiationProvider.createAutomationComposition(compositionId, ac)

        when:
        def resp = helper.sendDelete(helper.endpoint(compositionId, UUID.randomUUID())).retrieve().toBodilessEntity()

        then:
        resp.statusCode.value() == 404
    }

    // --- Rollback ---

    def "rollback returns 404 for unknown instance"() {
        given:
        def compositionId = helper.createDefinition("RollbackNotFound")

        when:
        def resp = helper.sendPost("${helper.endpoint(compositionId, UUID.randomUUID())}/rollback")
                .body("").retrieve().toBodilessEntity()

        then:
        resp.statusCode.value() == 404
    }

    def "rollback returns 400 for invalid state"() {
        given:
        def compositionId = helper.createDefinition("RollbackNotFound")
        def ac = helper.loadAc("acCreate", "NotValid")
        ac.compositionId = compositionId
        def instResp = instantiationProvider.createAutomationComposition(compositionId, ac)

        when:
        def resp = helper.sendPost("${helper.endpoint(compositionId, instResp.instanceId)}/rollback")
                .body("").retrieve().toBodilessEntity()

        then:
        resp.statusCode.value() == 400
    }

    // --- Deploy ---

    def "deploy returns 404 for unknown instance"() {
        given:
        def compositionId = helper.createDefinition("Deploy_NotFound")

        when:
        def resp = helper.sendPut(helper.endpoint(compositionId, UUID.randomUUID()))
                .body(new AcInstanceStateUpdate()).retrieve().toBodilessEntity()

        then:
        resp.statusCode.value() == 404
    }

    def "deploy returns 400 when orders are null"() {
        given:
        def compositionId = helper.createDefinition("Deploy_BadRequest")
        def ac = helper.loadAc("acCreate", "BadRequest")
        ac.compositionId = compositionId
        def instResp = instantiationProvider.createAutomationComposition(compositionId, ac)

        when:
        def resp = helper.sendPut(helper.endpoint(compositionId, instResp.instanceId))
                .body(new AcInstanceStateUpdate(deployOrder: null, lockOrder: null))
                .retrieve().toBodilessEntity()

        then:
        resp.statusCode.value() == 400
    }

    def "deploy succeeds with valid order"() {
        given:
        def compositionId = helper.createDefinition("Deploy")
        def ac = helper.loadAc("acCreate", "Command")
        ac.compositionId = compositionId
        def instResp = instantiationProvider.createAutomationComposition(compositionId, ac)

        when:
        def resp = helper.sendPut(helper.endpoint(compositionId, instResp.instanceId))
                .body(new AcInstanceStateUpdate(deployOrder: DeployOrder.DEPLOY, lockOrder: null))
                .retrieve().toBodilessEntity()

        then:
        resp.statusCode.value() == 202
    }

    // --- Query by filter ---

    def "query by filter without instance IDs"() {
        given:
        def compositionId = helper.createDefinition("Query")
        def ac = helper.loadAc("acCreate", "Query")
        ac.compositionId = compositionId
        def n = helper.numInstances
        n.times { ac.name = "acmr_$it"; instantiationProvider.createAutomationComposition(compositionId, ac) }

        expect:
        helper.querySize("instances") == n
        helper.querySize("instances?page=1&size=4") == 4
        helper.querySize("instances?size=4") == n
        helper.querySize("instances?stateChangeResult=FAILED,TIMEOUT") == 0
        helper.querySize("instances?deployState=UNDEPLOYED") == n
        helper.querySize("instances?stateChangeResult=NO_ERROR&deployState=UNDEPLOYED") == n
        helper.querySize("instances?sort=name&sortOrder=DESC") == n
    }

    def "query by filter with instance IDs"() {
        given:
        def compositionId = helper.createDefinition("Query")
        def ac = helper.loadAc("acCreate", "Query")
        ac.compositionId = compositionId
        def n = helper.numInstances
        def ids = (0..<n).collect {
            ac.name = "acmr_$it"
            instantiationProvider.createAutomationComposition(compositionId, ac).instanceId
        }.join(",")

        expect:
        helper.querySize("instances?instanceIds=$ids") == n
        helper.querySize("instances?page=1&size=4&instanceIds=$ids") == 4
        helper.querySize("instances?size=4&instanceIds=$ids") == n
        helper.querySize("instances?stateChangeResult=FAILED,TIMEOUT&instanceIds=$ids") == 0
        helper.querySize("instances?deployState=UNDEPLOYED&instanceIds=$ids") == n
        helper.querySize("instances?sort=name&sortOrder=DESC&instanceIds=$ids") == n
    }
}
