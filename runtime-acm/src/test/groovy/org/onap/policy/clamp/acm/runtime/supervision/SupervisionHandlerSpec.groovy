/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2026 Nordix Foundation.
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
package org.onap.policy.clamp.acm.runtime.supervision

import static org.onap.policy.clamp.acm.runtime.util.CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML

import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils
import org.onap.policy.clamp.acm.runtime.util.CommonTestData
import org.onap.policy.clamp.models.acm.concepts.AcTypeState
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrimeAck
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider
import org.onap.policy.clamp.models.acm.persistence.provider.MessageProvider
import spock.lang.Specification

class SupervisionHandlerSpec extends Specification {

    def "handle invalid PrimeAck should not lookup definition - #scenario"() {
        given:
        def acDefinitionProvider = Mock(AcDefinitionProvider)
        def handler = new SupervisionHandler(acDefinitionProvider, Mock(MessageProvider))
        def msg = new ParticipantPrimeAck(
                participantId: CommonTestData.participantId,
                compositionId: compositionId,
                stateChangeResult: stateChangeResult,
                compositionState: compositionState)

        when:
        handler.handleParticipantMessage(msg)

        then:
        0 * acDefinitionProvider.findAcDefinition(_)

        where:
        scenario                        | compositionId    | stateChangeResult          | compositionState
        "null compositionId"            | null             | null                       | null
        "null stateChangeResult"        | UUID.randomUUID()| null                       | null
        "null compositionState"         | UUID.randomUUID()| StateChangeResult.NO_ERROR | null
        "state PRIMING"                 | UUID.randomUUID()| StateChangeResult.NO_ERROR | AcTypeState.PRIMING
        "state DEPRIMING"               | UUID.randomUUID()| StateChangeResult.NO_ERROR | AcTypeState.DEPRIMING
        "stateChangeResult TIMEOUT"     | UUID.randomUUID()| StateChangeResult.TIMEOUT  | AcTypeState.COMMISSIONED
    }

    def "handle PrimeAck when definition not found"() {
        given:
        def acDefinitionProvider = Mock(AcDefinitionProvider)
        def messageProvider = Mock(MessageProvider)
        def handler = new SupervisionHandler(acDefinitionProvider, messageProvider)
        def msg = new ParticipantPrimeAck(
                participantId: CommonTestData.participantId,
                compositionId: UUID.randomUUID(),
                stateChangeResult: StateChangeResult.NO_ERROR,
                compositionState: AcTypeState.PRIMED)

        when:
        handler.handleParticipantMessage(msg)

        then:
        1 * acDefinitionProvider.findAcDefinition(msg.compositionId) >> Optional.empty()
        0 * messageProvider.save(_)
    }

    def "handle PrimeAck when definition not in transition - #scenario"() {
        given:
        def acDefinitionProvider = Mock(AcDefinitionProvider)
        def messageProvider = Mock(MessageProvider)
        def handler = new SupervisionHandler(acDefinitionProvider, messageProvider)
        def acDefinition = CommonTestData.createAcDefinition(
                InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML), definitionState)
        def msg = new ParticipantPrimeAck(
                participantId: CommonTestData.participantId,
                compositionId: acDefinition.compositionId,
                stateChangeResult: StateChangeResult.NO_ERROR,
                compositionState: AcTypeState.PRIMED)

        when:
        handler.handleParticipantMessage(msg)

        then:
        1 * acDefinitionProvider.findAcDefinition(_) >> Optional.of(acDefinition)
        0 * messageProvider.save(_)

        where:
        scenario        | definitionState
        "PRIMED"        | AcTypeState.PRIMED
        "COMMISSIONED"  | AcTypeState.COMMISSIONED
    }

    def "handle PrimeAck saves message - #scenario"() {
        given:
        def acDefinitionProvider = Mock(AcDefinitionProvider)
        def messageProvider = Mock(MessageProvider)
        def handler = new SupervisionHandler(acDefinitionProvider, messageProvider)
        def acDefinition = CommonTestData.createAcDefinition(
                InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML), AcTypeState.PRIMING)
        acDefinition.stateChangeResult = StateChangeResult.NO_ERROR
        acDefinition.elementStateMap.values().each { it.participantId = CommonTestData.participantId }
        def msg = new ParticipantPrimeAck(
                participantId: CommonTestData.participantId,
                compositionId: acDefinition.compositionId,
                stateChangeResult: stateChangeResult,
                compositionState: compositionState)
        when:
        handler.handleParticipantMessage(msg)

        then:
        1 * acDefinitionProvider.findAcDefinition(_) >> Optional.of(acDefinition)
        1 * messageProvider.save(msg)

        where:
        scenario       | stateChangeResult          | compositionState
        "success"      | StateChangeResult.NO_ERROR | AcTypeState.PRIMED
        "failed"       | StateChangeResult.FAILED   | AcTypeState.COMMISSIONED
    }
}
