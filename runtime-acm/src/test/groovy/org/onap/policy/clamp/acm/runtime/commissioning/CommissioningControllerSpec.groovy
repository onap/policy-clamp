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

import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils
import org.onap.policy.clamp.acm.runtime.util.CommonTestData
import org.onap.policy.clamp.acm.runtime.util.rest.CommonRestClient
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition
import org.onap.policy.clamp.models.acm.concepts.ParticipantSupportedElementType
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.AcTypeStateUpdate
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.CommissioningResponse
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.PrimeOrder
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider
import org.onap.policy.clamp.models.acm.utils.AcmUtils
import org.onap.policy.models.base.PfKey
import org.onap.policy.models.tosca.authorative.concepts.ToscaDataType
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate
import org.onap.policy.models.tosca.authorative.concepts.ToscaProperty
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplates
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

    private static final String ENDPOINT = "compositions"

    @Shared
    CommonRestClient client = new CommonRestClient()

    @Autowired
    AcDefinitionProvider acDefinitionProvider

    @Autowired
    ParticipantProvider participantProvider

    @LocalServerPort
    int port

    @Shared
    ToscaServiceTemplate serviceTemplate

    @Shared
    boolean clientInitialized = false

    def setupSpec() {
        serviceTemplate = InstantiationUtils.getToscaServiceTemplate(CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML)
    }

    def setup() {
        if (!clientInitialized) {
            client.initializeRestClient(port)
            clientInitialized = true
        }
    }

    // ------------------- auth & swagger -------------------

    def "swagger endpoint should be accessible"() {
        expect:
        client.testSwagger(ENDPOINT)
    }

    def "unauthorized create should be rejected"() {
        expect:
        client.assertUnauthorizedPost(ENDPOINT, serviceTemplate)
    }

    def "unauthorized query should be rejected"() {
        expect:
        client.assertUnauthorizedGet(ENDPOINT)
    }

    def "unauthorized delete should be rejected"() {
        expect:
        client.assertUnauthorizedDelete(ENDPOINT)
    }

    // ------------------- create -------------------

    def "invalid payload returns bad request"() {
        when:
        def resp = post("NotToscaServiceTemplate")

        then:
        resp.statusCode.value() == 400
        resp.body.errorDetails.contains("HttpMessageNotReadableException")
        resp.body.affectedAutomationCompositionDefinitions == null
    }

    def "invalid version format fails"() {
        given:
        def template = new ToscaServiceTemplate(serviceTemplate)
        def node = new ToscaNodeTemplate(template.toscaTopologyTemplate.nodeTemplates.values().first())
        node.version = "1.0.wrong"
        template.toscaTopologyTemplate.nodeTemplates[node.name] = node

        when:
        def response = createTemplate(template, 500)

        then:
        response.errorDetails == "java.lang.IllegalArgumentException parameter " +
                "\"version\": value \"1.0.wrong\", does not match regular expression \"" +
                PfKey.VERSION_REGEXP + "\""
        response.affectedAutomationCompositionDefinitions == null
    }

    def "successful create returns all node templates"() {
        given:
        def template = new ToscaServiceTemplate(serviceTemplate)
        template.name = "Create"

        when:
        def response = createTemplate(template, 201)

        then:
        response.errorDetails == null
        response.affectedAutomationCompositionDefinitions.size() == 7
        allNodeTemplatesPresent(template, response)
    }

    def "versioning template creates successfully"() {
        given:
        def template = InstantiationUtils.getToscaServiceTemplate(CommonTestData.TOSCA_VERSIONING)

        when:
        def response = createTemplate(template, 201)

        then:
        response.errorDetails == null
        response.affectedAutomationCompositionDefinitions.size() == 11
        allNodeTemplatesPresent(template, response)
    }

    // ------------------- update -------------------

    def "update existing composition adds data type"() {
        given:
        def dataTypeName = "org.onap.datatypes.policy.clamp.Configuration"
        def compositionId = createEntry("forUpdate")
        def template = new ToscaServiceTemplate(serviceTemplate)
        template.dataTypes[dataTypeName] = buildDataType(dataTypeName)
        template.metadata = [compositionId: compositionId]

        when:
        def response = createTemplate(template, 200)

        then:
        response.errorDetails == null
        response.affectedAutomationCompositionDefinitions.size() == 7
        allNodeTemplatesPresent(template, response)

        and:
        getTemplate("$ENDPOINT/$compositionId").serviceTemplate.dataTypes.containsKey(dataTypeName)
    }

    // ------------------- query -------------------

    def "query with non-existent name returns empty"() {
        when:
        def body = get("$ENDPOINT?name=noResultWithThisName")

        then:
        body.serviceTemplates.isEmpty()
    }

    def "query returns data when entries exist"() {
        given:
        createEntry("forQuery")

        when:
        def body = get(ENDPOINT)

        then:
        !body.serviceTemplates.isEmpty()
    }

    def "pageable query works"() {
        given:
        10.times { createEntry("pageable$it") }

        expect:
        get("$ENDPOINT?name=wrongName").serviceTemplates.size() == 0
        get("$ENDPOINT?name=pageable1").serviceTemplates.size() == 1
        get("$ENDPOINT?page=1&size=5").serviceTemplates.size() == 5
        get("$ENDPOINT?size=4").serviceTemplates.size() >= 10
        get("$ENDPOINT?page=2").serviceTemplates.size() >= 10
        get(ENDPOINT).serviceTemplates.size() >= 10
    }

    // ------------------- delete -------------------

    def "delete unknown composition returns not found"() {
        given:
        createEntry("DeleteBadRequest")

        expect:
        deleteAndGetStatus("$ENDPOINT/${UUID.randomUUID()}") == 404
    }

    def "delete existing composition removes it"() {
        given:
        def id = createEntry("forDelete")

        when:
        deleteAndGetStatus("$ENDPOINT/$id")

        then:
        acDefinitionProvider.findAcDefinition(id).empty
    }

    // ------------------- prime -------------------

    def "prime succeeds with complete participants"() {
        given:
        def id = createEntry("Prime")
        registerParticipants(true)

        expect:
        prime(id) == 202
    }

    def "prime fails with incomplete participants"() {
        given:
        def id = createEntry("Prime")
        registerParticipants(false)

        expect:
        prime(id) == 409
    }

    // ------------------- helpers -------------------

    private post(body) {
        client.sendPost(ENDPOINT).body(body).retrieve().toEntity(CommissioningResponse)
    }

    private CommissioningResponse createTemplate(ToscaServiceTemplate template, int expectedStatus) {
        def resp = client.sendPost(ENDPOINT).body(template).retrieve().toEntity(CommissioningResponse)
        assert resp.statusCode.value() == expectedStatus
        resp.body
    }

    private AutomationCompositionDefinition getTemplate(String url) {
        def resp = client.sendGet(url).retrieve().toEntity(AutomationCompositionDefinition)
        assert resp.statusCode.value() == 200
        resp.body
    }

    private ToscaServiceTemplates get(String url) {
        def resp = client.sendGet(url).retrieve().toEntity(ToscaServiceTemplates)
        assert resp.statusCode.value() == 200
        resp.body
    }

    private int deleteAndGetStatus(String url) {
        client.sendDelete(url).retrieve().toBodilessEntity().statusCode.value()
    }

    private int prime(UUID compositionId) {
        def body = new AcTypeStateUpdate(primeOrder: PrimeOrder.PRIME)
        client.sendPut("$ENDPOINT/$compositionId").body(body).retrieve().toBodilessEntity().statusCode.value()
    }

    private UUID createEntry(String name) {
        def template = new ToscaServiceTemplate(serviceTemplate)
        template.name = name
        acDefinitionProvider.createAutomationCompositionDefinition(
                template, CommonTestData.TOSCA_ELEMENT_NAME, CommonTestData.TOSCA_COMP_NAME
        ).compositionId
    }

    private void registerParticipants(boolean withReplica) {
        AcmUtils.extractAcElementsFromServiceTemplate(
                serviceTemplate, "org.onap.policy.clamp.acm.AutomationCompositionElement"
        ).each { entry ->
            def participant = CommonTestData.createParticipant(UUID.randomUUID())
            def supported = new ParticipantSupportedElementType(
                    typeName: entry.value.type, typeVersion: entry.value.typeVersion)
            participant.participantSupportedElementTypes[supported.id] = supported

            if (withReplica) {
                def replica = CommonTestData.createParticipantReplica(UUID.randomUUID())
                participant.replicas[replica.replicaId] = replica
            }
            participantProvider.saveParticipant(participant)
        }
    }

    private static boolean allNodeTemplatesPresent(ToscaServiceTemplate template, CommissioningResponse response) {
        template.toscaTopologyTemplate.nodeTemplates.keySet().every { name ->
            response.affectedAutomationCompositionDefinitions.any { it.name == name }
        }
    }

    private static ToscaDataType buildDataType(String name) {
        def prop = new ToscaProperty(name: "configurationEntityId", type: "onap.datatypes.ToscaConceptIdentifier")
        new ToscaDataType(name: name, derivedFrom: "tosca.datatypes.Root", properties: [(prop.name): prop])
    }
}
