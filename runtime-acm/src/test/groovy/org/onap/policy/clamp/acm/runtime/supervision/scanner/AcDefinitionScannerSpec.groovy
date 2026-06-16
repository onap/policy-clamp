/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025-2026 OpenInfra Foundation Europe. All rights reserved.
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
package org.onap.policy.clamp.acm.runtime.supervision.scanner

import static org.onap.policy.clamp.acm.runtime.util.CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML

import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantSyncPublisher
import org.onap.policy.clamp.acm.runtime.util.CommonTestData
import org.onap.policy.clamp.models.acm.concepts.AcTypeState
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition
import org.onap.policy.clamp.models.acm.concepts.NodeTemplateState
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult
import org.onap.policy.clamp.models.acm.document.concepts.DocMessage
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessageType
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider
import org.onap.policy.clamp.models.acm.utils.TimestampHelper
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier
import spock.lang.Specification

class AcDefinitionScannerSpec extends Specification {

    static final COMPOSITION_ID = UUID.randomUUID()
    static final OUT_PROPERTIES = [key: "value"]

    def "scan message with FAILED state should update definition and flag sync"() {
        given:
        def scanner = buildScanner(Mock(AcDefinitionProvider), Mock(ParticipantSyncPublisher))
        def acDefinition = createAcDefinition(AcTypeState.PRIMING, StateChangeResult.NO_ERROR)
        def element = acDefinition.elementStateMap.values().iterator().next()
        def docMessage = new DocMessage(
                compositionId: COMPOSITION_ID,
                messageType: ParticipantMessageType.PARTICIPANT_PRIME_ACK,
                stateChangeResult: StateChangeResult.FAILED,
                compositionState: AcTypeState.COMMISSIONED,
                participantId: element.participantId)

        when:
        def result = scanner.scanMessage(acDefinition, docMessage)

        then:
        result.updated
        result.toBeSync
        acDefinition.elementStateMap[element.nodeTemplateStateId.toString()].state == AcTypeState.COMMISSIONED
        acDefinition.stateChangeResult == StateChangeResult.FAILED
    }

    def "scan message with wrong data should not update - #scenario"() {
        given:
        def scanner = buildScanner(Mock(AcDefinitionProvider), Mock(ParticipantSyncPublisher))
        def acDefinition = createAcDefinition(AcTypeState.DEPRIMING, StateChangeResult.NO_ERROR)
        def element = acDefinition.elementStateMap.values().iterator().next()
        def docMessage = new DocMessage(
                compositionId: COMPOSITION_ID,
                stateChangeResult: StateChangeResult.NO_ERROR,
                compositionState: AcTypeState.COMMISSIONED,
                participantId: participantId.call(element),
                messageType: messageType,
                outProperties: outProps,
                acElementDefinitionId: elementDefId)

        when:
        def result = scanner.scanMessage(acDefinition, docMessage)

        then:
        !result.updated
        !result.toBeSync

        where:
        scenario                          | messageType                                                    | outProps       | elementDefId                                  | participantId
        "wrong MessageType"               | ParticipantMessageType.AUTOMATION_COMPOSITION_STATECHANGE_ACK  | null           | null                                          | { it.participantId }
        "wrong elementId in outProperties"| ParticipantMessageType.PARTICIPANT_STATUS                      | OUT_PROPERTIES | new ToscaConceptIdentifier("wrong", "1.0.1")  | { it.participantId }
        "wrong participantId"             | ParticipantMessageType.PARTICIPANT_PRIME_ACK                   | null           | null                                          | { UUID.randomUUID() }
    }

    def "scan message state change should update element state"() {
        given:
        def scanner = buildScanner(Mock(AcDefinitionProvider), Mock(ParticipantSyncPublisher))
        def acDefinition = createAcDefinition(AcTypeState.DEPRIMING, StateChangeResult.NO_ERROR)
        def element = acDefinition.elementStateMap.values().iterator().next()
        def docMessage = new DocMessage(
                compositionId: COMPOSITION_ID,
                messageType: ParticipantMessageType.PARTICIPANT_PRIME_ACK,
                stateChangeResult: StateChangeResult.NO_ERROR,
                compositionState: AcTypeState.COMMISSIONED,
                participantId: element.participantId)

        when:
        def result = scanner.scanMessage(acDefinition, docMessage)

        then:
        result.updated
        !result.toBeSync
        acDefinition.elementStateMap[element.nodeTemplateStateId.toString()].state == AcTypeState.COMMISSIONED
    }

    def "scan message outProperties should update element outProperties"() {
        given:
        def scanner = buildScanner(Mock(AcDefinitionProvider), Mock(ParticipantSyncPublisher))
        def acDefinition = createAcDefinition(AcTypeState.DEPRIMING, StateChangeResult.NO_ERROR)
        def element = acDefinition.elementStateMap.values().iterator().next()
        def docMessage = new DocMessage(
                compositionId: COMPOSITION_ID,
                messageType: ParticipantMessageType.PARTICIPANT_STATUS,
                outProperties: OUT_PROPERTIES,
                acElementDefinitionId: element.nodeTemplateId)

        when:
        def result = scanner.scanMessage(acDefinition, docMessage)

        then:
        result.updated
        result.toBeSync
        acDefinition.elementStateMap[element.nodeTemplateStateId.toString()].outProperties == OUT_PROPERTIES
    }

    def "scan definition - #scenario"() {
        given:
        def acDefinitionProvider = Mock(AcDefinitionProvider)
        def participantSyncPublisher = Mock(ParticipantSyncPublisher)
        def scanner = buildScanner(acDefinitionProvider, participantSyncPublisher)
        def acDefinition = createAcDefinition(acTypeState, stateChangeResult)
        if (primeElements) {
            acDefinition.elementStateMap.values().each { it.state = AcTypeState.PRIMED }
        }

        when:
        scanner.scanAutomationCompositionDefinition(acDefinition, new UpdateSync())

        then:
        expectedCalls * acDefinitionProvider.updateAcDefinitionState(_)
        expectedCalls * participantSyncPublisher.sendSync(_, _)

        where:
        scenario                                              | acTypeState         | stateChangeResult          | primeElements | expectedCalls
        "FAILED state should not update provider"             | AcTypeState.PRIMING | StateChangeResult.FAILED   | false         | 0
        "PRIMING with NO_ERROR and elements primed completes" | AcTypeState.PRIMING | StateChangeResult.NO_ERROR | true          | 1
    }

    def "scan definition timeout - #scenario"() {
        given:
        def acDefinitionProvider = Mock(AcDefinitionProvider)
        def participantSyncPublisher = Mock(ParticipantSyncPublisher)
        def acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner")
        acRuntimeParameterGroup.participantParameters.maxOperationWaitMs = maxWaitMs
        def scanner = new AcDefinitionScanner(acDefinitionProvider, participantSyncPublisher,
                acRuntimeParameterGroup)
        def acDefinition = createAcDefinition(acTypeState, stateChangeResult)

        when:
        scanner.scanAutomationCompositionDefinition(acDefinition, new UpdateSync())

        then:
        expectedUpdateCalls * acDefinitionProvider.updateAcDefinitionState(_)
        expectedSyncCalls * participantSyncPublisher.sendSync(_, _)

        where:
        scenario                    | acTypeState           | stateChangeResult          | maxWaitMs | expectedUpdateCalls | expectedSyncCalls
        "DEPRIMING no timeout"      | AcTypeState.DEPRIMING | StateChangeResult.NO_ERROR | 100000    | 0                   | 0
        "PRIMING no timeout"        | AcTypeState.PRIMING   | StateChangeResult.NO_ERROR | 100000    | 0                   | 0
        "PRIMING with timeout"      | AcTypeState.PRIMING   | StateChangeResult.NO_ERROR | -1        | 1                   | 1
        "PRIMING already timeout"   | AcTypeState.PRIMING   | StateChangeResult.TIMEOUT  | -1        | 0                   | 0
    }

    def "scan definition completed should update state to final"() {
        given:
        def acDefinitionProvider = Mock(AcDefinitionProvider)
        def participantSyncPublisher = Mock(ParticipantSyncPublisher)
        def acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner")
        acRuntimeParameterGroup.participantParameters.maxOperationWaitMs = -1
        def scanner = new AcDefinitionScanner(acDefinitionProvider, participantSyncPublisher,
                acRuntimeParameterGroup)
        def acDefinition = createAcDefinition(AcTypeState.PRIMING, StateChangeResult.NO_ERROR)
        acDefinition.elementStateMap.values().each { it.state = AcTypeState.PRIMED }

        when:
        scanner.scanAutomationCompositionDefinition(acDefinition, new UpdateSync())

        then:
        1 * acDefinitionProvider.updateAcDefinitionState(_)
        1 * participantSyncPublisher.sendSync(_, _)
        acDefinition.state == AcTypeState.PRIMED
        acDefinition.stateChangeResult == StateChangeResult.NO_ERROR
    }

    // ---- Helpers ----

    static buildScanner(AcDefinitionProvider acDefinitionProvider,
                                ParticipantSyncPublisher participantSyncPublisher) {
        def acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner")
        new AcDefinitionScanner(acDefinitionProvider, participantSyncPublisher, acRuntimeParameterGroup)
    }

    static createAcDefinition(AcTypeState acTypeState, StateChangeResult stateChangeResult) {
        def serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML)
        serviceTemplate.metadata = [compositionId: COMPOSITION_ID]
        def node = new NodeTemplateState(
                state: acTypeState,
                nodeTemplateStateId: UUID.randomUUID(),
                participantId: UUID.randomUUID(),
                nodeTemplateId: new ToscaConceptIdentifier("name", "1.0.0"))
        new AutomationCompositionDefinition(
                state: acTypeState,
                stateChangeResult: stateChangeResult,
                compositionId: COMPOSITION_ID,
                lastMsg: TimestampHelper.now(),
                serviceTemplate: serviceTemplate,
                elementStateMap: [(node.nodeTemplateStateId.toString()): node])
    }
}
