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
import org.springframework.http.HttpStatus

class InstantiationControllerTestHelper {

    static final String ENDPOINT_PATTERN = "compositions/%s/instances"
    static final String NODE_TYPE = "org.onap.policy.clamp.acm.AutomationComposition"
    static final int NUM_INSTANCES = 10
    static final String AC_CREATE_JSON =
            "src/test/resources/rest/acm/AutomationComposition.json"
    static final String AC_UPDATE_JSON =
            "src/test/resources/rest/acm/AutomationCompositionUpdate.json"
    static final String AC_VERSIONING_YAML =
            "src/test/resources/rest/acm/AutomationCompositionVersioning.yaml"

    final ToscaServiceTemplate serviceTemplate = InstantiationUtils.getToscaServiceTemplate(
            CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML)
    final CommonRestClient client = new CommonRestClient()
    AcDefinitionProvider acDefinitionProvider
    ParticipantProvider participantProvider

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
        return client.sendGet(url)
    }

    def sendPost(String url) {
        return client.sendPost(url)
    }

    def sendPut(String url) {
        return client.sendPut(url)
    }

    def sendDelete(String url) {
        return client.sendDelete(url)
    }

    void setProviders(AcDefinitionProvider acDefinitionProvider, ParticipantProvider participantProvider) {
        this.acDefinitionProvider = acDefinitionProvider
        this.participantProvider = participantProvider
    }

    static def getEndpointPattern() {
        return ENDPOINT_PATTERN
    }

    static def getNodeType() {
        return NODE_TYPE
    }

    static def getNumInstances() {
        return NUM_INSTANCES
    }

    static def endpoint(UUID compositionId, UUID instanceId = null) {
        def base = String.format(endpointPattern, compositionId)
        return instanceId ? "$base/$instanceId" : base
    }

    def createDefinition(String name) {
        return createDefinition(new ToscaServiceTemplate(serviceTemplate).tap { it.name = name })
    }

    def createDefinition(ToscaServiceTemplate template) {
        def acmDef = CommonTestData.createAcDefinition(template, AcTypeState.PRIMED)
        acDefinitionProvider.updateAcDefinition(acmDef, nodeType)
        def participant = CommonTestData.createParticipant(CommonTestData.participantId)
        def replica = CommonTestData.createParticipantReplica(CommonTestData.replicaId)
        participant.replicas[replica.replicaId] = replica
        participantProvider.saveParticipant(participant)
        return acmDef.compositionId
    }

    static def loadAc(String resourceKey, String suffix) {
        def path = resourceKey == "acUpdate" ? AC_UPDATE_JSON : AC_CREATE_JSON
        return InstantiationUtils.getAutomationCompositionFromResource(path, suffix)
    }

    static def loadAcFromYaml(String resourceKey, String suffix) {
        return InstantiationUtils.getAutomationCompositionFromYaml(AC_VERSIONING_YAML, suffix)
    }

    def postAc(UUID compositionId, AutomationComposition ac, int expectedStatus) {
        def resp = client.sendPost(endpoint(compositionId)).body(ac).retrieve().toEntity(InstantiationResponse)
        assert resp.statusCode.value() == expectedStatus
        return resp.body
    }

    def querySize(String url) {
        def resp = client.sendGet(url).retrieve().toEntity(AutomationCompositions)
        assert resp.statusCode.value() == HttpStatus.OK.value()
        return resp.body.automationCompositionList.size()
    }
}
