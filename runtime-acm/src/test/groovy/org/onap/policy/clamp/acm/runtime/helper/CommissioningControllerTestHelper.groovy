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

import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils
import org.onap.policy.clamp.acm.runtime.util.CommonTestData
import org.onap.policy.clamp.acm.runtime.util.rest.CommonRestClient
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition
import org.onap.policy.clamp.models.acm.concepts.ParticipantSupportedElementType
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.AcTypeStateUpdate
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.CommissioningResponse
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.PrimeOrder
import org.onap.policy.clamp.models.acm.utils.AcmUtils
import org.onap.policy.models.tosca.authorative.concepts.ToscaDataType
import org.onap.policy.models.tosca.authorative.concepts.ToscaProperty
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplates
import org.springframework.http.HttpStatus

class CommissioningControllerTestHelper {

    static final ENDPOINT = "compositions"
    static final AC_ELEMENT_TYPE = CommonTestData.TOSCA_ELEMENT_NAME
    static final INVALID_VERSION = "1.0.0.0.0"
    static final UPDATE_DATA_TYPE_NAME = "org.onap.datatypes.policy.clamp.test.UpdateDataType"
    static final CREATE_EXPECTED_COUNT = 7
    static final VERSIONING_EXPECTED_COUNT = 11
    static final DATA_TYPE_DERIVED_FROM = "tosca.datatypes.Root"
    static final DATA_TYPE_PROPERTY_NAME = "testProperty"
    static final DATA_TYPE_PROPERTY_TYPE = "string"

    final endpoint = ENDPOINT
    final serviceTemplate = InstantiationUtils.getToscaServiceTemplate(CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML)
    final client = new CommonRestClient()
    def acDefinitionProvider
    def participantProvider

    void initializeRestClient(port) {
        client.initializeRestClient(port)
    }

    void testSwagger() {
        client.testSwagger(endpoint)
    }

    void assertUnauthorizedPost() {
        client.assertUnauthorizedPost(endpoint, serviceTemplate)
    }

    void assertUnauthorizedGet() {
        client.assertUnauthorizedGet(endpoint)
    }

    void assertUnauthorizedDelete() {
        client.assertUnauthorizedDelete(endpoint)
    }

    void setProviders(acDefinitionProvider, participantProvider) {
        this.acDefinitionProvider = acDefinitionProvider
        this.participantProvider = participantProvider
    }

    def getAcDefinitionProvider() {
        return acDefinitionProvider
    }

    static getAcElementType() {
        return AC_ELEMENT_TYPE
    }

    static getInvalidVersion() {
        return INVALID_VERSION
    }

    static getUpdateDataTypeName() {
        return UPDATE_DATA_TYPE_NAME
    }

    static getCreateExpectedCount() {
        return CREATE_EXPECTED_COUNT
    }

    static getVersioningExpectedCount() {
        return VERSIONING_EXPECTED_COUNT
    }

    def post(body) {
        return client.sendPost(endpoint).body(body).retrieve().toEntity(CommissioningResponse)
    }

    def createTemplate(template, expectedStatus) {
        def resp = client.sendPost(endpoint).body(template).retrieve().toEntity(CommissioningResponse)
        assert resp.statusCode == expectedStatus
        return resp.body
    }

    def getTemplate(url) {
        def resp = client.sendGet(url).retrieve().toEntity(AutomationCompositionDefinition)
        assert resp.statusCode == HttpStatus.OK
        return resp.body
    }

    def getStatusForGet(url) {
        return client.sendGet(url).retrieve().toBodilessEntity().statusCode
    }

    def get(url) {
        def resp = client.sendGet(url).retrieve().toEntity(ToscaServiceTemplates)
        assert resp.statusCode == HttpStatus.OK
        return resp.body
    }

    def deleteAndGetStatus(url) {
        return client.sendDelete(url).retrieve().toBodilessEntity().statusCode
    }

    def prime(compositionId) {
        return sendPrimeOrder(compositionId, PrimeOrder.PRIME)
    }

    def deprime(compositionId) {
        return sendPrimeOrder(compositionId, PrimeOrder.DEPRIME)
    }

    def sendPrimeOrder(compositionId, order) {
        def body = new AcTypeStateUpdate(primeOrder: order)
        return client.sendPut("$endpoint/$compositionId").body(body).retrieve().toBodilessEntity().statusCode
    }

    void assertUnauthorizedPut() {
        def body = new AcTypeStateUpdate(primeOrder: PrimeOrder.PRIME)
        client.assertUnauthorizedPut("$endpoint/${UUID.randomUUID()}", body)
    }

    def deleteWithResponse(url) {
        def resp = client.sendDelete(url).retrieve().toEntity(CommissioningResponse)
        assert resp.statusCode == HttpStatus.OK
        return resp.body
    }

    def createEntry(name) {
        def template = new ToscaServiceTemplate(serviceTemplate)
        template.name = name
        return acDefinitionProvider.createAutomationCompositionDefinition(
                template, CommonTestData.TOSCA_ELEMENT_NAME, CommonTestData.TOSCA_COMP_NAME
        ).compositionId
    }

    void registerParticipants(withReplica) {
        AcmUtils.extractAcElementsFromServiceTemplate(serviceTemplate, acElementType).each { entry ->
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

    static allNodeTemplatesPresent(template, response) {
        return template.toscaTopologyTemplate.nodeTemplates.keySet().every { name ->
            response.affectedAutomationCompositionDefinitions.any { it.name == name }
        }
    }

    static buildDataType(name) {
        def prop = new ToscaProperty(name: DATA_TYPE_PROPERTY_NAME, type: DATA_TYPE_PROPERTY_TYPE)
        return new ToscaDataType(name: name, derivedFrom: DATA_TYPE_DERIVED_FROM, properties: [(prop.name): prop])
    }
}
