/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2026 OpenInfra Foundation Europe. All rights reserved.
 *  Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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
package org.onap.policy.clamp.acm.runtime.supervision.comm

import org.onap.policy.clamp.models.acm.concepts.AutomationComposition

import static org.onap.policy.clamp.acm.runtime.helper.SupervisionMessagesTestHelper.buildAcmDefinition
import static org.onap.policy.clamp.acm.runtime.helper.SupervisionMessagesTestHelper.loadAcFromResource
import static org.onap.policy.clamp.acm.runtime.util.CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML

import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionAcHandler
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionHandler
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionParticipantHandler
import org.onap.policy.clamp.acm.runtime.util.CommonTestData
import org.onap.policy.clamp.models.acm.concepts.AcTypeState
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition
import org.onap.policy.clamp.models.acm.concepts.DeployState
import org.onap.policy.clamp.models.acm.concepts.LockState
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeployAck
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionMigration
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionPrepare
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionStateChange
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantDeregister
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantDeregisterAck
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessageType
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrime
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrimeAck
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantRegister
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantRegisterAck
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantReqSync
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatus
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatusReq
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantSync
import org.onap.policy.clamp.models.acm.messages.kafka.participant.PropertiesUpdate
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider
import org.onap.policy.clamp.models.acm.utils.AcmUtils
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier
import spock.lang.Specification

class SupervisionMessagesSpec extends Specification {

    def participantPublisher = Mock(ParticipantPublisher)
    def participantAckPublisher = Mock(ParticipantAckPublisher)

    def "send ParticipantRegisterAck"() {
        given:
        def publisher = new ParticipantRegisterAckPublisher(participantAckPublisher)

        when:
        publisher.send(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())

        then:
        1 * participantAckPublisher.sendToSyncTopic(_ as ParticipantRegisterAck)
    }

    def "send ParticipantDeregisterAck"() {
        given:
        def publisher = new ParticipantDeregisterAckPublisher(participantAckPublisher)

        when:
        publisher.send(UUID.randomUUID())

        then:
        1 * participantAckPublisher.send(_ as ParticipantDeregisterAck)
    }

    def "send AutomationCompositionStateChange"() {
        given:
        def publisher = new AutomationCompositionStateChangePublisher(participantPublisher)
        def ac = new AutomationComposition(
                name: "NAME", version: "0.0.1",
                deployState: DeployState.DEPLOYED,
                lockState: LockState.UNLOCKING,
                elements: [:])

        when:
        publisher.send(ac, 0, true, UUID.randomUUID())

        then:
        1 * participantPublisher.send(_ as AutomationCompositionStateChange)
    }

    def "send ParticipantStatusReq"() {
        given:
        def publisher = new ParticipantStatusReqPublisher(participantPublisher)

        when:
        publisher.send(CommonTestData.participantId)

        then:
        1 * participantPublisher.sendToSyncTopic(_ as ParticipantStatusReq)
    }

    def "send AcElementProperties"() {
        given:
        def publisher = new AcElementPropertiesPublisher(participantPublisher)
        def ac = loadAcFromResource()

        when:
        publisher.send(ac, UUID.randomUUID())

        then:
        1 * participantPublisher.send(_ as PropertiesUpdate)
    }

    def "send AutomationCompositionMigration"() {
        given:
        def publisher = new AutomationCompositionMigrationPublisher(participantPublisher)
        def ac = loadAcFromResource()

        when:
        publisher.send(ac, 0, UUID.randomUUID(), UUID.randomUUID(), true)

        then:
        1 * participantPublisher.send(_ as AutomationCompositionMigration)
    }

    def "send AcPrepare - #scenario"() {
        given:
        def publisher = new AcPreparePublisher(participantPublisher)
        def ac = loadAcFromResource()

        when:
        sendAction.call(publisher, ac)

        then:
        1 * participantPublisher.send(_ as AutomationCompositionPrepare)

        where:
        scenario  | sendAction
        "prepare" | { p, a -> p.sendPrepare(a, 0, UUID.randomUUID()) }
        "review"  | { p, a -> p.sendReview(a, UUID.randomUUID()) }
    }

    def "send ParticipantPrime for depriming"() {
        given:
        def publisher = new ParticipantPrimePublisher(
                Mock(ParticipantProvider), Mock(AcRuntimeParameterGroup), participantPublisher)

        when:
        publisher.sendDepriming(UUID.randomUUID(), Set.of(UUID.randomUUID()), UUID.randomUUID())

        then:
        1 * participantPublisher.send(_)
    }

    def "send ParticipantPrime for priming"() {
        given:
        def participantId = UUID.randomUUID()
        def supportedElementMap = [
                (new ToscaConceptIdentifier("org.onap.policy.clamp.acm.PolicyAutomationCompositionElement", "1.0.0")): participantId,
                (new ToscaConceptIdentifier("org.onap.policy.clamp.acm.K8SMicroserviceAutomationCompositionElement", "1.0.0")): participantId,
                (new ToscaConceptIdentifier("org.onap.policy.clamp.acm.HttpAutomationCompositionElement", "1.0.0")): participantId
        ]
        def participantProvider = Mock(ParticipantProvider) {
            getSupportedElementMap() >> supportedElementMap
        }
        def serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML)
        serviceTemplate.name = "Name"
        serviceTemplate.version = "1.0.0"
        def acmDefinition = new AutomationCompositionDefinition(
                compositionId: UUID.randomUUID(),
                serviceTemplate: serviceTemplate)
        def acElements = AcmUtils.extractAcElementsFromServiceTemplate(serviceTemplate,
                CommonTestData.TOSCA_ELEMENT_NAME)
        acmDefinition.elementStateMap = AcmUtils.createElementStateMap(acElements, AcTypeState.COMMISSIONED)
        def publisher = new ParticipantPrimePublisher(participantProvider,
                CommonTestData.getTestParamaterGroup(), participantPublisher)
        def preparation = publisher.prepareParticipantPriming(acmDefinition)

        when:
        publisher.sendPriming(preparation, acmDefinition.compositionId, acmDefinition.revisionId)

        then:
        1 * participantPublisher.send(_ as ParticipantPrime)
    }

    def "send ParticipantSync for AC - #scenario"() {
        given:
        def publisher = new ParticipantSyncPublisher(CommonTestData.getTestParamaterGroup(), participantPublisher)
        def ac = loadAcFromResource()
        ac.deployState = deployState

        when:
        syncAction.call(publisher, ac)

        then:
        1 * participantPublisher.sendToSyncTopic(_ as ParticipantSync)

        where:
        scenario       | deployState           | syncAction
        "deployed"     | DeployState.DEPLOYED  | { p, a -> p.sendSync(a) }
        "deleted"      | DeployState.DELETED   | { p, a -> p.sendSync(a) }
        "delete sync"  | DeployState.DEPLOYED  | { p, a -> p.sendDeleteSync(a, UUID.randomUUID()) }
    }

    def "send ParticipantSync for AcDefinition - #scenario"() {
        given:
        def publisher = new ParticipantSyncPublisher(CommonTestData.getTestParamaterGroup(), participantPublisher)
        def acmDefinition = buildAcmDefinition(state)

        when:
        publisher.sendSync(acmDefinition, pId)

        then:
        1 * participantPublisher.sendToSyncTopic(_ as ParticipantSync)

        where:
        scenario       | state                    | pId
        "primed"       | AcTypeState.PRIMED       | null
        "commissioned" | AcTypeState.COMMISSIONED | UUID.randomUUID()
    }

    def "send ParticipantSync for restart"() {
        given:
        def publisher = new ParticipantSyncPublisher(CommonTestData.getTestParamaterGroup(), participantPublisher)
        def ac = loadAcFromResource()
        def participantId = ac.elements.values().iterator().next().participantId
        def acmDefinition = buildAcmDefinition()
        acmDefinition.elementStateMap.values().iterator().next().participantId = participantId

        when:
        publisher.sendRestartMsg(participantId, UUID.randomUUID(), acmDefinition, [ac])

        then:
        1 * participantPublisher.sendToSyncTopic(_ as ParticipantSync)
    }

    def "receive ParticipantRegister should delegate to participantHandler"() {
        given:
        def handler = Mock(SupervisionParticipantHandler)
        def listener = new ParticipantMessageListener(
                Mock(SupervisionAcHandler), handler, Mock(SupervisionHandler))
        def msg = new ParticipantRegister()

        when:
        listener.onTopicEvent(msg)

        then:
        1 * handler.handleParticipantMessage(msg)
    }

    def "receive ParticipantDeregister should delegate to participantHandler"() {
        given:
        def handler = Mock(SupervisionParticipantHandler)
        def listener = new ParticipantMessageListener(
                Mock(SupervisionAcHandler), handler, Mock(SupervisionHandler))
        def msg = new ParticipantDeregister()

        when:
        listener.onTopicEvent(msg)

        then:
        1 * handler.handleParticipantMessage(msg)
    }

    def "receive ParticipantStatus should delegate to participantHandler"() {
        given:
        def handler = Mock(SupervisionParticipantHandler)
        def listener = new ParticipantMessageListener(
                Mock(SupervisionAcHandler), handler, Mock(SupervisionHandler))
        def msg = new ParticipantStatus()

        when:
        listener.onTopicEvent(msg)

        then:
        1 * handler.handleParticipantMessage(msg)
    }

    def "receive ParticipantPrimeAck should delegate to supervisionHandler"() {
        given:
        def handler = Mock(SupervisionHandler)
        def listener = new ParticipantMessageListener(
                Mock(SupervisionAcHandler), Mock(SupervisionParticipantHandler), handler)
        def msg = new ParticipantPrimeAck()

        when:
        listener.onTopicEvent(msg)

        then:
        1 * handler.handleParticipantMessage(msg)
    }

    def "receive ParticipantReqSync should delegate to participantHandler"() {
        given:
        def handler = Mock(SupervisionParticipantHandler)
        def listener = new ParticipantMessageListener(
                Mock(SupervisionAcHandler), handler, Mock(SupervisionHandler))
        def msg = new ParticipantReqSync()

        when:
        listener.onTopicEvent(msg)

        then:
        1 * handler.handleParticipantReqSync(msg)
    }

    def "receive AutomationCompositionDeployAck should delegate to acHandler"() {
        given:
        def handler = Mock(SupervisionAcHandler)
        def listener = new ParticipantMessageListener(
                handler, Mock(SupervisionParticipantHandler), Mock(SupervisionHandler))
        def msg = new AutomationCompositionDeployAck(ParticipantMessageType.AUTOMATION_COMPOSITION_DEPLOY_ACK)

        when:
        listener.onTopicEvent(msg)

        then:
        1 * handler.handleAutomationCompositionUpdateAckMessage(msg)
    }

    def "receive AutomationCompositionStateChangeAck should delegate to acHandler"() {
        given:
        def handler = Mock(SupervisionAcHandler)
        def listener = new ParticipantMessageListener(
                handler, Mock(SupervisionParticipantHandler), Mock(SupervisionHandler))
        def msg = new AutomationCompositionDeployAck(ParticipantMessageType.AUTOMATION_COMPOSITION_STATECHANGE_ACK)

        when:
        listener.onTopicEvent(msg)

        then:
        1 * handler.handleAutomationCompositionStateChangeAckMessage(msg)
    }
}
