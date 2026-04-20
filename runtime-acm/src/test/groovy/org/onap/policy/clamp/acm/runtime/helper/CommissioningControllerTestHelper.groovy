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
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider
import org.onap.policy.clamp.models.acm.utils.AcmUtils
import org.onap.policy.models.tosca.authorative.concepts.ToscaDataType
import org.onap.policy.models.tosca.authorative.concepts.ToscaProperty
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplates
import org.yaml.snakeyaml.Yaml

class CommissioningControllerTestHelper {

    private static final String CONFIG_PATH = "commissioning/commissioning-test-config.yaml"

    private final Map config
    private final String endpoint
    private final ToscaServiceTemplate serviceTemplate

    private final CommonRestClient client
    private AcDefinitionProvider acDefinitionProvider
    private ParticipantProvider participantProvider

    CommissioningControllerTestHelper() {
        this.client = new CommonRestClient()
        this.config = new Yaml().load(getClass().classLoader.getResourceAsStream(CONFIG_PATH))
        this.endpoint = config.endpoint
        this.serviceTemplate = InstantiationUtils.getToscaServiceTemplate(CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML)
    }

    void initializeRestClient(int port) {
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

    void setProviders(AcDefinitionProvider acDefinitionProvider, ParticipantProvider participantProvider) {
        this.acDefinitionProvider = acDefinitionProvider
        this.participantProvider = participantProvider
    }

    AcDefinitionProvider getAcDefinitionProvider() {
        acDefinitionProvider
    }

    String getAcElementType() {
        config.acElementType
    }

    String getInvalidVersion() {
        config.testCases.invalidVersion
    }

    String getUpdateDataTypeName() {
        config.testCases.updateDataTypeName
    }

    int getCreateExpectedCount() {
        config.testCases.create.expectedNodeTemplateCount
    }

    int getVersioningExpectedCount() {
        config.testCases.versioning.expectedNodeTemplateCount
    }

    def post(body) {
        client.sendPost(endpoint).body(body).retrieve().toEntity(CommissioningResponse)
    }

    CommissioningResponse createTemplate(ToscaServiceTemplate template, int expectedStatus) {
        def resp = client.sendPost(endpoint).body(template).retrieve().toEntity(CommissioningResponse)
        assert resp.statusCode.value() == expectedStatus
        resp.body
    }

    AutomationCompositionDefinition getTemplate(String url) {
        def resp = client.sendGet(url).retrieve().toEntity(AutomationCompositionDefinition)
        assert resp.statusCode.value() == 200
        resp.body
    }

    ToscaServiceTemplates get(String url) {
        def resp = client.sendGet(url).retrieve().toEntity(ToscaServiceTemplates)
        assert resp.statusCode.value() == 200
        resp.body
    }

    int deleteAndGetStatus(String url) {
        client.sendDelete(url).retrieve().toBodilessEntity().statusCode.value()
    }

    int prime(UUID compositionId) {
        def body = new AcTypeStateUpdate(primeOrder: PrimeOrder.PRIME)
        client.sendPut("$endpoint/$compositionId").body(body).retrieve().toBodilessEntity().statusCode.value()
    }

    UUID createEntry(String name) {
        def template = new ToscaServiceTemplate(serviceTemplate)
        template.name = name
        acDefinitionProvider.createAutomationCompositionDefinition(
                template, CommonTestData.TOSCA_ELEMENT_NAME, CommonTestData.TOSCA_COMP_NAME
        ).compositionId
    }

    void registerParticipants(boolean withReplica) {
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

    static boolean allNodeTemplatesPresent(ToscaServiceTemplate template, CommissioningResponse response) {
        template.toscaTopologyTemplate.nodeTemplates.keySet().every { name ->
            response.affectedAutomationCompositionDefinitions.any { it.name == name }
        }
    }

    ToscaDataType buildDataType(String name) {
        def dtConfig = config.dataType
        def prop = new ToscaProperty(name: dtConfig.propertyName, type: dtConfig.propertyType)
        new ToscaDataType(name: name, derivedFrom: dtConfig.derivedFrom, properties: [(prop.name): prop])
    }
}
