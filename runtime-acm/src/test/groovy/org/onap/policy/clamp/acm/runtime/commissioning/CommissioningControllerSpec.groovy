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
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared
import spock.lang.Specification

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka
@ActiveProfiles(["test", "default"])
@DirtiesContext
class CommissioningControllerSpec extends Specification {

    @Shared
    CommissioningControllerTestHelper helper = new CommissioningControllerTestHelper()

    @Autowired
    AcDefinitionProvider acDefinitionProvider

    @Autowired
    ParticipantProvider participantProvider

    @LocalServerPort
    int port

    def setup() {
        helper.setProviders(acDefinitionProvider, participantProvider)
        helper.initializeRestClient(port)
    }

    // ------------------- auth & swagger -------------------

    def "given commissioning endpoint, swagger should be accessible and return API docs"() {
        expect: "a successful response from the swagger endpoint"
        helper.testSwagger()
    }

    def "given an unauthenticated POST request, the server should reject with 401"() {
        expect: "an unauthorized response when no credentials are provided"
        helper.assertUnauthorizedPost()
    }

    def "given an unauthenticated GET request, the server should reject with 401"() {
        expect: "an unauthorized response when no credentials are provided"
        helper.assertUnauthorizedGet()
    }

    def "given an unauthenticated DELETE request, the server should reject with 401"() {
        expect: "an unauthorized response when no credentials are provided"
        helper.assertUnauthorizedDelete()
    }

    // ------------------- create -------------------

    def "given a non-TOSCA payload, the server should return 400 bad request"() {
        when: "a plain string is posted instead of a valid ToscaServiceTemplate"
        def resp = helper.post("NotToscaServiceTemplate")

        then: "the response should indicate a deserialization error with no affected definitions"
        resp.statusCode.value() == 400
        resp.body.errorDetails.contains("HttpMessageNotReadableException")
        resp.body.affectedAutomationCompositionDefinitions == null
    }

    def "given a template with an invalid version format, the server should return 500"() {
        given: "a service template with a node whose version does not match the expected pattern"
        def template = new ToscaServiceTemplate(helper.serviceTemplate)
        def node = new ToscaNodeTemplate(template.toscaTopologyTemplate.nodeTemplates.values().first())
        node.version = helper.invalidVersion
        template.toscaTopologyTemplate.nodeTemplates[node.name] = node

        when: "the template is submitted for commissioning"
        def response = helper.createTemplate(template, 500)

        then: "the error details should report the version regexp mismatch"
        response.errorDetails == "java.lang.IllegalArgumentException parameter " +
                "\"version\": value \"${helper.invalidVersion}\", does not match regular expression \"" +
                PfKey.VERSION_REGEXP + "\""
        response.affectedAutomationCompositionDefinitions == null
    }

    def "given a valid service template, creation should return 201 with all node templates"() {
        given: "a valid ToscaServiceTemplate"
        def template = new ToscaServiceTemplate(helper.serviceTemplate)
        template.name = "Create"

        when: "the template is commissioned"
        def response = helper.createTemplate(template, 201)

        then: "all node templates from the input should be present in the response"
        response.errorDetails == null
        response.affectedAutomationCompositionDefinitions.size() == helper.createExpectedCount
        CommissioningControllerTestHelper.allNodeTemplatesPresent(template, response)
    }

    def "given a versioning service template, creation should return 201 with correct count"() {
        given: "a ToscaServiceTemplate loaded from the versioning test resource"
        def template = InstantiationUtils.getToscaServiceTemplate(CommonTestData.TOSCA_VERSIONING)

        when: "the template is commissioned"
        def response = helper.createTemplate(template, 201)

        then: "all node templates should be present with the expected versioning count"
        response.errorDetails == null
        response.affectedAutomationCompositionDefinitions.size() == helper.versioningExpectedCount
        CommissioningControllerTestHelper.allNodeTemplatesPresent(template, response)
    }

    // ------------------- update -------------------

    def "given an existing composition and a new data type, update should return 200 with the data type persisted"() {
        given: "an existing commissioned definition and a template with an additional data type"
        def dataTypeName = helper.updateDataTypeName
        def compositionId = helper.createEntry("forUpdate")
        def template = new ToscaServiceTemplate(helper.serviceTemplate)
        template.dataTypes[dataTypeName] = helper.buildDataType(dataTypeName)
        template.metadata = [compositionId: compositionId]

        when: "the updated template is submitted with the compositionId in metadata"
        def response = helper.createTemplate(template, 200)

        then: "the response should contain all node templates with no errors"
        response.errorDetails == null
        response.affectedAutomationCompositionDefinitions.size() == helper.createExpectedCount
        CommissioningControllerTestHelper.allNodeTemplatesPresent(template, response)

        and: "the persisted definition should include the newly added data type"
        helper.getTemplate("${helper.endpoint}/$compositionId").serviceTemplate.dataTypes.containsKey(dataTypeName)
    }

    // ------------------- query -------------------

    def "given a name filter that matches nothing, query should return an empty list"() {
        when: "querying with a non-existent name parameter"
        def body = helper.get("${helper.endpoint}?name=noResultWithThisName")

        then: "the service templates list should be empty"
        body.serviceTemplates.isEmpty()
    }

    def "given existing commissioned entries, query should return a non-empty list"() {
        given: "at least one commissioned definition exists"
        helper.createEntry("forQuery")

        when: "querying without filters"
        def body = helper.get(helper.endpoint)

        then: "the response should contain service templates"
        !body.serviceTemplates.isEmpty()
    }

    def "given multiple entries, pageable query should return correct page sizes and filter results"() {
        given: "10 commissioned definitions with 'pageable' prefix"
        def ep = helper.endpoint
        10.times { helper.createEntry("pageable$it") }

        expect: "correct result counts for various page and filter combinations"
        helper.get("$ep?name=wrongName").serviceTemplates.size() == 0
        helper.get("$ep?name=pageable1").serviceTemplates.size() == 1
        helper.get("$ep?page=1&size=5").serviceTemplates.size() == 5
        helper.get("$ep?size=4").serviceTemplates.size() >= 10
        helper.get("$ep?page=2").serviceTemplates.size() >= 10
        helper.get(ep).serviceTemplates.size() >= 10
    }

    // ------------------- delete -------------------

    def "given a random UUID, delete should return 404 not found"() {
        given: "at least one entry exists but a random UUID is used for deletion"
        helper.createEntry("DeleteBadRequest")

        expect: "a 404 status since the composition ID does not exist"
        helper.deleteAndGetStatus("${helper.endpoint}/${UUID.randomUUID()}") == 404
    }

    def "given an existing composition, delete should remove it from the provider"() {
        given: "a commissioned definition"
        def id = helper.createEntry("forDelete")

        when: "the composition is deleted by its ID"
        helper.deleteAndGetStatus("${helper.endpoint}/$id")

        then: "the definition should no longer be found in the provider"
        acDefinitionProvider.findAcDefinition(id).empty
    }

    // ------------------- prime -------------------

    def "given a composition with all required participants registered, prime should return 202"() {
        given: "a commissioned definition and all required participants registered"
        def id = helper.createEntry("Prime")
        helper.registerParticipants(true)

        expect: "a 202 accepted response indicating priming has started"
        helper.prime(id) == 202
    }

    def "given a composition with incomplete participants, prime should return 409 conflict"() {
        given: "a commissioned definition with only partial participants registered"
        def id = helper.createEntry("Prime")
        helper.registerParticipants(false)

        expect: "a 409 conflict response since not all required participants are available"
        helper.prime(id) == 409
    }
}
