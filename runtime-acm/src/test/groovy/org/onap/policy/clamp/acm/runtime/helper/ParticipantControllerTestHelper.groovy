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
import org.springframework.core.ParameterizedTypeReference
import org.yaml.snakeyaml.Yaml

class ParticipantControllerTestHelper {

    private static final String CONFIG_PATH = "participant/participant-controller-test-config.yaml"

    private final Map config

    private final CommonRestClient client
    private ParticipantProvider participantProvider
    private AcDefinitionProvider acDefinitionProvider
    private AutomationCompositionInstantiationProvider instantiationProvider

    private int seedCounter = 0

    ParticipantControllerTestHelper() {
        this.client = new CommonRestClient()
        this.config = new Yaml().load(getClass().classLoader.getResourceAsStream(CONFIG_PATH))
    }

    void initializeRestClient(int port) {
        client.initializeRestClient(port)
    }

    def sendGet(String url) {
        client.sendGet(url)
    }

    def sendGetNoAuth(String url) {
        client.sendGetNoAuth(url)
    }

    def sendPut(String url) {
        client.sendPut(url)
    }

    void setProviders(ParticipantProvider participantProvider, AcDefinitionProvider acDefinitionProvider,
                      AutomationCompositionInstantiationProvider instantiationProvider) {
        this.participantProvider = participantProvider
        this.acDefinitionProvider = acDefinitionProvider
        this.instantiationProvider = instantiationProvider
    }

    String getEndpoint() {
        config.endpoint
    }

    String getNodeType() {
        config.nodeType
    }

    int getNumberRecords() {
        config.numberRecords
    }

    String getResourcePath(String key) {
        config.resourcePaths[key]
    }

    String getSyncPath(String key) {
        config.syncPaths[key]
    }

    Participant sampleParticipant() {
        def p = CommonTestData.createParticipant(CommonTestData.participantId)
        def r = CommonTestData.createParticipantReplica(CommonTestData.participantId)
        p.replicas[r.replicaId] = r
        p
    }

    List<Participant> inputParticipants() {
        [
                CommonTestData.getObjectFromJson(
                        ResourceUtils.getResourceAsString(getResourcePath("testParticipant1")), Participant),
                CommonTestData.getObjectFromJson(
                        ResourceUtils.getResourceAsString(getResourcePath("testParticipant2")), Participant)
        ]
    }

    AutomationComposition loadAutomationComposition(String suffix) {
        def json = ResourceUtils.getResourceAsString(getResourcePath("acInstantiationCreate"))
        def ac = CommonTestData.getObjectFromJson(json, AutomationComposition)
        ac.name = ac.name + suffix
        ac
    }

    UUID seedParticipantWithData() {
        def participant = sampleParticipant()
        participantProvider.saveParticipant(participant)
        def prefix = "test_${seedCounter++}_"

        (0..<numberRecords).each {
            createAcDefinition("${prefix}$it")
        }

        participant.participantId
    }

    UUID saveSingleParticipant() {
        def p = sampleParticipant()
        participantProvider.saveParticipant(p)
        p.participantId
    }

    void saveInputParticipants() {
        inputParticipants().each { participantProvider.saveParticipant(it) }
    }

    void validatePageable(String url, int expected) {
        def info = getParticipantInfo(url)
        assert info.acNodeTemplateStateDefinitionMap.size() == expected
        assert info.acElementInstanceMap.size() == expected
    }

    void validateNotPageable(String url, int expected) {
        def info = getParticipantInfo(url)
        assert info.acNodeTemplateStateDefinitionMap.size() >= expected
        assert info.acElementInstanceMap.size() >= expected
    }

    void validateAllPageable(String url, int expected) {
        def info = getFirstParticipantInfo(url)
        assert info.acNodeTemplateStateDefinitionMap.size() == expected
    }

    void validateAllNotPageable(String url, int expected) {
        def info = getFirstParticipantInfo(url)
        assert info.acNodeTemplateStateDefinitionMap.size() >= expected
    }

    private ParticipantInformation getParticipantInfo(String url) {
        def resp = client.sendGet("$endpoint$url")
                .retrieve().toEntity(ParticipantInformation)
        assert resp.statusCode.value() == 200
        resp.body
    }

    private ParticipantInformation getFirstParticipantInfo(String url) {
        def resp = client.sendGet("$endpoint$url")
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
        acDefinitionProvider.updateAcDefinition(defn, nodeType)

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
}
