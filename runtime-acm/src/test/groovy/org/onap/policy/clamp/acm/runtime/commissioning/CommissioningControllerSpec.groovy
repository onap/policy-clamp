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

import org.onap.policy.clamp.acm.runtime.helper.CommissioningControllerTestHelper
import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils
import org.onap.policy.clamp.acm.runtime.util.CommonTestData
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider
import org.onap.policy.models.base.PfKey
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka
@ActiveProfiles(["test", "default"])
@DirtiesContext
class CommissioningControllerSpec extends Specification {

    def helper = new CommissioningControllerTestHelper()

    @Autowired
    AcDefinitionProvider acDefinitionProvider

    @Autowired
    ParticipantProvider participantProvider

    @LocalServerPort
    int randomServerPort

    def setup() {
        helper.setProviders(acDefinitionProvider, participantProvider)
        helper.initializeRestClient(randomServerPort)
    }

    // ------------------- auth & swagger -------------------

    def "swagger should be accessible and return API docs"() {
        expect:
        helper.testSwagger()
    }

    def "unauthenticated POST request should be rejected with 401"() {
        expect:
        helper.assertUnauthorizedPost()
    }

    def "unauthenticated GET request should be rejected with 401"() {
        expect:
        helper.assertUnauthorizedGet()
    }

    def "unauthenticated DELETE request should be rejected with 401"() {
        expect:
        helper.assertUnauthorizedDelete()
    }

    def "unauthenticated PUT request should be rejected with 401"() {
        expect:
        helper.assertUnauthorizedPut()
    }

    // ------------------- create -------------------

    def "non-TOSCA payload should return BAD_REQUEST"() {
        when:
        def commissioningResponse = helper.post("NotToscaServiceTemplate")

        then:
        commissioningResponse.statusCode == HttpStatus.BAD_REQUEST
        commissioningResponse.body != null
        commissioningResponse.body.errorDetails.contains("HttpMessageNotReadableException")
        commissioningResponse.body.affectedAutomationCompositionDefinitions == null
    }

    def "template with invalid version format should return INTERNAL_SERVER_ERROR"() {
        given:
        def template = new ToscaServiceTemplate(helper.serviceTemplate)
        def node = new ToscaNodeTemplate(template.toscaTopologyTemplate.nodeTemplates.values().first())
        node.version = helper.invalidVersion
        template.toscaTopologyTemplate.nodeTemplates[node.name] = node

        when:
        def commissioningResponse = helper.createTemplate(template, HttpStatus.INTERNAL_SERVER_ERROR)

        then:
        commissioningResponse.errorDetails == "java.lang.IllegalArgumentException parameter " +
                "\"version\": value \"${helper.invalidVersion}\", does not match regular expression \"" +
                PfKey.VERSION_REGEXP + "\""
        commissioningResponse.affectedAutomationCompositionDefinitions == null
    }

    def "valid service template creation should return CREATED with all node templates"() {
        given:
        def template = new ToscaServiceTemplate(helper.serviceTemplate)
        template.name = "Create"

        when:
        def commissioningResponse = helper.createTemplate(template, HttpStatus.CREATED)

        then:
        commissioningResponse != null
        commissioningResponse.errorDetails == null
        commissioningResponse.affectedAutomationCompositionDefinitions.size() == helper.createExpectedCount
        CommissioningControllerTestHelper.allNodeTemplatesPresent(template, commissioningResponse)
    }

    def "versioning service template creation should return CREATED with correct count"() {
        given:
        def template = InstantiationUtils.getToscaServiceTemplate(CommonTestData.TOSCA_VERSIONING)

        when:
        def commissioningResponse = helper.createTemplate(template, HttpStatus.CREATED)

        then:
        commissioningResponse != null
        commissioningResponse.errorDetails == null
        commissioningResponse.affectedAutomationCompositionDefinitions.size() == helper.versioningExpectedCount
        CommissioningControllerTestHelper.allNodeTemplatesPresent(template, commissioningResponse)
    }

    // ------------------- update -------------------

    def "update with new data type should return OK and persist the data type"() {
        given:
        def dataTypeName = helper.updateDataTypeName
        def compositionId = helper.createEntry("forUpdate")
        def template = new ToscaServiceTemplate(helper.serviceTemplate)
        template.dataTypes[dataTypeName] = helper.buildDataType(dataTypeName)
        template.metadata = [compositionId: compositionId]

        when:
        def commissioningResponse = helper.createTemplate(template, HttpStatus.OK)

        then:
        commissioningResponse != null
        commissioningResponse.errorDetails == null
        commissioningResponse.affectedAutomationCompositionDefinitions.size() == helper.createExpectedCount
        CommissioningControllerTestHelper.allNodeTemplatesPresent(template, commissioningResponse)

        and:
        helper.getTemplate("${helper.endpoint}/$compositionId").serviceTemplate.dataTypes.containsKey(dataTypeName)
    }

    // ------------------- get single composition -------------------

    def "get composition by ID should return the definition with service template"() {
        given:
        def id = helper.createEntry("forGetById")

        when:
        def autoCompositionDef = helper.getTemplate("${helper.endpoint}/$id")

        then:
        autoCompositionDef != null
        autoCompositionDef.compositionId == id
        autoCompositionDef.serviceTemplate != null
    }

    def "get composition with non-existent ID should return NOT_FOUND"() {
        when:
        def status = helper.getStatusForGet("${helper.endpoint}/${UUID.randomUUID()}")

        then:
        status == HttpStatus.NOT_FOUND
    }

    // ------------------- query -------------------

    def "query with non-existent name should return empty list"() {
        when:
        def entityList = helper.get("${helper.endpoint}?name=noResultWithThisName")

        then:
        entityList.serviceTemplates.empty
    }

    def "query with name and version should return matching entry"() {
        given:
        def template = new ToscaServiceTemplate(helper.serviceTemplate)
        template.name = "VersionQuery"
        template.version = "1.0.0"
        helper.createTemplate(template, HttpStatus.CREATED)

        when:
        def entityList = helper.get("${helper.endpoint}?name=VersionQuery&version=1.0.0")

        then:
        !entityList.serviceTemplates.empty
        entityList.serviceTemplates.every { it.name == "VersionQuery" }
    }

    def "query with non-existent version should return empty list"() {
        when:
        def entityList = helper.get("${helper.endpoint}?name=VersionQuery&version=99.99.99")

        then:
        entityList.serviceTemplates.empty
    }

    def "query without filters should return non-empty list"() {
        given:
        helper.createEntry("forQuery")

        when:
        def entityList = helper.get(helper.endpoint)

        then:
        !entityList.serviceTemplates.empty
    }

    def "pageable query '#query' should return #condition"() {
        given:
        def ep = helper.endpoint
        10.times { helper.createEntry("pageable$it") }

        expect:
        helper.get("$ep$query").serviceTemplates.size() >= minSize

        where:
        query                                  | minSize | condition
        "?name=wrongName"                      | 0       | "0 results for wrong name"
        "?name=pageable1"                      | 1       | "1 result for exact name"
        "?page=1&size=5"                       | 5       | "5 results for page size 5"
        "?participantId=" + UUID.randomUUID()  | 0       | "0 results for wrong participantId"
        ""                                     | 10      | "at least 10 results without filters"
    }

    // ------------------- delete -------------------

    def "delete with random UUID should return NOT_FOUND"() {
        given:
        helper.createEntry("DeleteBadRequest")

        expect:
        helper.deleteAndGetStatus("${helper.endpoint}/${UUID.randomUUID()}") == HttpStatus.NOT_FOUND
    }

    def "delete existing composition should return OK with response body"() {
        given:
        def id = helper.createEntry("forDeleteResponse")

        when:
        def commissioningResponse = helper.deleteWithResponse("${helper.endpoint}/$id")

        then:
        commissioningResponse != null
        commissioningResponse.affectedAutomationCompositionDefinitions != null
    }

    def "delete existing composition should remove it from provider"() {
        given:
        def id = helper.createEntry("forDelete")

        when:
        helper.deleteAndGetStatus("${helper.endpoint}/$id")

        then:
        acDefinitionProvider.findAcDefinition(id).empty
    }

    // ------------------- prime -------------------

    def "prime with all participants registered should return ACCEPTED"() {
        given:
        def id = helper.createEntry("Prime")
        helper.registerParticipants(true)

        expect:
        helper.prime(id) == HttpStatus.ACCEPTED
    }

    def "prime with incomplete participants should return CONFLICT"() {
        given:
        def id = helper.createEntry("Prime")
        helper.registerParticipants(false)

        expect:
        helper.prime(id) == HttpStatus.CONFLICT
    }

    def "prime with non-existent composition should return NOT_FOUND"() {
        expect:
        helper.prime(UUID.randomUUID()) == HttpStatus.NOT_FOUND
    }

    // ------------------- deprime -------------------

    def "deprime with non-existent composition should return NOT_FOUND"() {
        expect:
        helper.deprime(UUID.randomUUID()) == HttpStatus.NOT_FOUND
    }

    def "deprime on a commissioned composition should return ACCEPTED"() {
        given:
        def id = helper.createEntry("DeprimeCommissioned")
        helper.registerParticipants(true)

        expect:
        helper.deprime(id) == HttpStatus.ACCEPTED
    }
}
