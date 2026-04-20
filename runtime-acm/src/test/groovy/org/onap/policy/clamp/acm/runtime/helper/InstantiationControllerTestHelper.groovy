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
import org.onap.policy.clamp.models.acm.concepts.AcTypeState
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationResponse
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate
import org.yaml.snakeyaml.Yaml

class InstantiationControllerTestHelper {

    private static final String CONFIG_PATH = "instantiation/instantiation-controller-test-config.yaml"

    private final Map config
    private final ToscaServiceTemplate serviceTemplate

    private final CommonRestClient client
    private AcDefinitionProvider acDefinitionProvider
    private ParticipantProvider participantProvider

    InstantiationControllerTestHelper() {
        this.client = new CommonRestClient()
        this.config = new Yaml().load(getClass().classLoader.getResourceAsStream(CONFIG_PATH))
        this.serviceTemplate = InstantiationUtils.getToscaServiceTemplate(CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML)
    }

    void initializeRestClient(int port) {
        client.initializeRestClient(port)
    }

    void testSwagger() {
        client.testSwagger(String.format(endpointPattern, "{compositionId}"))
    }

    void assertUnauthorizedPost(UUID id, Object body) {
        client.assertUnauthorizedPost(endpoint(id), body)
    }

    void assertUnauthorizedGet(UUID id) {
        client.assertUnauthorizedGet(endpoint(id))
    }

    void assertUnauthorizedPut(UUID id, Object body) {
        client.assertUnauthorizedPut(endpoint(id), body)
    }

    void assertUnauthorizedDelete(UUID id) {
        client.assertUnauthorizedDelete(endpoint(id))
    }

    def sendGet(String url) {
        client.sendGet(url)
    }

    def sendPost(String url) {
        client.sendPost(url)
    }

    def sendPut(String url) {
        client.sendPut(url)
    }

    def sendDelete(String url) {
        client.sendDelete(url)
    }

    void setProviders(AcDefinitionProvider acDefinitionProvider, ParticipantProvider participantProvider) {
        this.acDefinitionProvider = acDefinitionProvider
        this.participantProvider = participantProvider
    }

    String getEndpointPattern() {
        config.endpointPattern
    }

    String getNodeType() {
        config.nodeType
    }

    int getNumInstances() {
        config.numInstances
    }

    String getResourcePath(String key) {
        config.resourcePaths[key]
    }

    String endpoint(UUID compositionId, UUID instanceId = null) {
        def base = String.format(endpointPattern, compositionId)
        instanceId ? "$base/$instanceId" : base
    }

    UUID createDefinition(String name) {
        createDefinition(new ToscaServiceTemplate(serviceTemplate).tap { it.name = name })
    }

    UUID createDefinition(ToscaServiceTemplate template) {
        def acmDef = CommonTestData.createAcDefinition(template, AcTypeState.PRIMED)
        acDefinitionProvider.updateAcDefinition(acmDef, nodeType)
        def participant = CommonTestData.createParticipant(CommonTestData.participantId)
        def replica = CommonTestData.createParticipantReplica(CommonTestData.replicaId)
        participant.replicas[replica.replicaId] = replica
        participantProvider.saveParticipant(participant)
        acmDef.compositionId
    }

    AutomationComposition loadAc(String resourceKey, String suffix) {
        InstantiationUtils.getAutomationCompositionFromResource(getResourcePath(resourceKey), suffix)
    }

    AutomationComposition loadAcFromYaml(String resourceKey, String suffix) {
        InstantiationUtils.getAutomationCompositionFromYaml(getResourcePath(resourceKey), suffix)
    }

    InstantiationResponse postAc(UUID compositionId, AutomationComposition ac, int expectedStatus) {
        def resp = client.sendPost(endpoint(compositionId)).body(ac).retrieve().toEntity(InstantiationResponse)
        assert resp.statusCode.value() == expectedStatus
        resp.body
    }

    int querySize(String url) {
        def resp = client.sendGet(url).retrieve().toEntity(AutomationCompositions)
        assert resp.statusCode.value() == 200
        resp.body.automationCompositionList.size()
    }
}
