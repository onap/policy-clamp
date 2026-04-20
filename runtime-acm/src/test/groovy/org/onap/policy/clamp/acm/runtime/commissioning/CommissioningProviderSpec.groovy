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
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantPrimePublisher
import org.onap.policy.clamp.acm.runtime.util.CommonTestData
import org.onap.policy.clamp.models.acm.concepts.AcTypeState
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.AcTypeStateUpdate
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.PrimeOrder
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider
import org.onap.policy.clamp.models.acm.persistence.provider.AcTypeStateResolver
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider
import org.onap.policy.models.base.PfModelRuntimeException
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate
import org.springframework.data.domain.Pageable
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

class CommissioningProviderSpec extends Specification {

    static final TOSCA_YAML = CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML

    // shared mocks
    AcDefinitionProvider acDefinitionProvider
    AutomationCompositionProvider acProvider
    ParticipantProvider participantProvider
    ParticipantPrimePublisher participantPrimePublisher

    def setup() {
        acDefinitionProvider = Mock()
        acProvider = Mock()
        participantProvider = Mock()
        participantPrimePublisher = Mock()
    }

    private CommissioningProvider createProvider(AcRuntimeParameterGroup params = null) {
        new CommissioningProvider(acDefinitionProvider, acProvider, participantProvider,
                new AcTypeStateResolver(), participantPrimePublisher, params)
    }

    // --- getAutomationCompositionDefinitions ---

    def "getAutomationCompositionDefinitions returns empty when no templates"() {
        given:
        acDefinitionProvider.getServiceTemplateList(null, null, Pageable.unpaged()) >> []
        def provider = createProvider(Mock(AcRuntimeParameterGroup))

        when:
        def result = provider.getAutomationCompositionDefinitions(null, null, Pageable.unpaged())

        then:
        result.serviceTemplates.empty
    }

    def "getAutomationCompositionDefinitions returns templates when they exist"() {
        given:
        acDefinitionProvider.getServiceTemplateList(null, null, Pageable.unpaged()) >> [new ToscaServiceTemplate()]
        def provider = createProvider(Mock(AcRuntimeParameterGroup))

        when:
        def result = provider.getAutomationCompositionDefinitions(null, null, Pageable.unpaged())

        then:
        result.serviceTemplates.size() == 1
    }

    def "getAutomationCompositionDefinitions throws on null pageable"() {
        given:
        def provider = createProvider(Mock(AcRuntimeParameterGroup))

        when:
        provider.getAutomationCompositionDefinitions(null, null, null)

        then:
        thrown(NullPointerException)
    }

    // --- createAutomationCompositionDefinition ---

    def "createAutomationCompositionDefinition returns affected definitions"() {
        given:
        def serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_YAML)
        serviceTemplate.name = "Name"
        serviceTemplate.version = "1.0.0"

        def acmDefinition = new AutomationCompositionDefinition(
                compositionId: UUID.randomUUID(), serviceTemplate: serviceTemplate)

        acDefinitionProvider.createAutomationCompositionDefinition(serviceTemplate,
                CommonTestData.TOSCA_ELEMENT_NAME, CommonTestData.TOSCA_COMP_NAME) >> acmDefinition

        def provider = createProvider(CommonTestData.getTestParamaterGroup())

        when:
        def result = provider.createAutomationCompositionDefinition(serviceTemplate)

        then:
        result.affectedAutomationCompositionDefinitions.size() == 7
        acmDefinition.stateChangeResult == StateChangeResult.NO_ERROR
    }

    // --- deleteAutomationCompositionDefinition ---

    def "delete fails when instances exist"() {
        given:
        def compositionId = UUID.randomUUID()
        acProvider.getAcInstancesByCompositionId(compositionId) >> [new AutomationComposition()]
        def provider = createProvider()

        when:
        provider.deleteAutomationCompositionDefinition(compositionId)

        then:
        def ex = thrown(PfModelRuntimeException)
        ex.message.contains("Delete instances, to commission automation composition definitions")
    }

    def "delete fails when not in COMMISSIONED state"() {
        given:
        def compositionId = UUID.randomUUID()
        def acmDefinition = new AutomationCompositionDefinition(
                compositionId: compositionId, state: AcTypeState.PRIMED,
                serviceTemplate: InstantiationUtils.getToscaServiceTemplate(TOSCA_YAML))

        acProvider.getAcInstancesByCompositionId(compositionId) >> []
        acDefinitionProvider.getAcDefinition(compositionId) >> acmDefinition
        def provider = createProvider()

        when:
        provider.deleteAutomationCompositionDefinition(compositionId)

        then:
        def ex = thrown(PfModelRuntimeException)
        ex.message.contains("ACM not in COMMISSIONED state, Delete of ACM Definition not allowed")
    }

    def "delete succeeds when COMMISSIONED and no instances"() {
        given:
        def compositionId = UUID.randomUUID()
        def serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_YAML)
        def acmDefinition = new AutomationCompositionDefinition(
                compositionId: compositionId, state: AcTypeState.COMMISSIONED, serviceTemplate: serviceTemplate)

        acProvider.getAcInstancesByCompositionId(compositionId) >> []
        acDefinitionProvider.getAcDefinition(compositionId) >> acmDefinition
        acDefinitionProvider.deleteAcDefinition(compositionId) >> serviceTemplate
        def provider = createProvider()

        when:
        provider.deleteAutomationCompositionDefinition(compositionId)

        then:
        1 * acDefinitionProvider.deleteAcDefinition(compositionId) >> serviceTemplate
    }

    // --- compositionDefinitionPriming ---

    def "priming triggers sendPriming"() {
        given:
        def acmDefinition = CommonTestData.createAcDefinition(
                InstantiationUtils.getToscaServiceTemplate(TOSCA_YAML), AcTypeState.COMMISSIONED)
        def compositionId = acmDefinition.compositionId

        acDefinitionProvider.getAcDefinition(compositionId) >> acmDefinition
        acProvider.getAcInstancesByCompositionId(compositionId) >> []
        acProvider.getAcInstancesByTargetCompositionId(compositionId) >> []

        def latch = new CountDownLatch(1)
        participantPrimePublisher.sendPriming(_, _, _) >> { latch.countDown() }

        def provider = createProvider(CommonTestData.getTestParamaterGroup())

        when:
        provider.compositionDefinitionPriming(compositionId, new AcTypeStateUpdate(primeOrder: PrimeOrder.PRIME))

        then:
        1 * acDefinitionProvider.updateAcDefinition(acmDefinition, CommonTestData.TOSCA_COMP_NAME)

        and:
        latch.await(2, TimeUnit.SECONDS)
    }

    def "depriming triggers sendDepriming"() {
        given:
        def acmDefinition = CommonTestData.createAcDefinition(
                InstantiationUtils.getToscaServiceTemplate(TOSCA_YAML), AcTypeState.PRIMED)
        def compositionId = acmDefinition.compositionId

        acDefinitionProvider.getAcDefinition(compositionId) >> acmDefinition
        acProvider.getAcInstancesByCompositionId(compositionId) >> []
        acProvider.getAcInstancesByTargetCompositionId(compositionId) >> []

        def participantIds = acmDefinition.elementStateMap.values().stream()
                .map({ it.participantId }).collect(Collectors.toSet())

        def latch = new CountDownLatch(1)

        def provider = createProvider(CommonTestData.getTestParamaterGroup())

        when:
        provider.compositionDefinitionPriming(compositionId, new AcTypeStateUpdate(primeOrder: PrimeOrder.DEPRIME))

        then:
        1 * participantProvider.verifyParticipantState(participantIds)
        1 * participantPrimePublisher.sendDepriming(compositionId, participantIds, _) >> { latch.countDown() }

        and:
        latch.await(2, TimeUnit.SECONDS)
    }

    // --- bad request scenarios ---

    def "priming fails when instances exist"() {
        given:
        def compositionId = UUID.randomUUID()
        acProvider.getAcInstancesByCompositionId(compositionId) >> [new AutomationComposition()]
        def provider = createProvider(CommonTestData.getTestParamaterGroup())

        when:
        provider.compositionDefinitionPriming(compositionId, new AcTypeStateUpdate())

        then:
        def ex = thrown(PfModelRuntimeException)
        ex.message.contains("There are instances, Priming/Depriming not allowed")
    }

    def "priming fails when compositionTargetId exists"() {
        given:
        def compositionId = UUID.randomUUID()
        acProvider.getAcInstancesByCompositionId(compositionId) >> []
        acProvider.getAcInstancesByTargetCompositionId(compositionId) >> [new AutomationComposition()]
        def provider = createProvider(CommonTestData.getTestParamaterGroup())

        when:
        provider.compositionDefinitionPriming(compositionId, new AcTypeStateUpdate())

        then:
        def ex = thrown(PfModelRuntimeException)
        ex.message.contains("This compositionId is referenced as a targetCompositionId in the instance table.")
    }

    def "updateCompositionDefinition fails when instances exist"() {
        given:
        def compositionId = UUID.randomUUID()
        acProvider.getAcInstancesByCompositionId(compositionId) >> [new AutomationComposition()]
        def provider = createProvider(Mock(AcRuntimeParameterGroup))

        when:
        provider.updateCompositionDefinition(compositionId, new ToscaServiceTemplate())

        then:
        def ex = thrown(PfModelRuntimeException)
        ex.message.contains("There are ACM instances, Update of ACM Definition not allowed")
    }

    def "updateCompositionDefinition fails when not COMMISSIONED"() {
        given:
        def compositionId = UUID.randomUUID()
        def acmDefinition = CommonTestData.createAcDefinition(
                InstantiationUtils.getToscaServiceTemplate(TOSCA_YAML), AcTypeState.PRIMED)
        acmDefinition.compositionId = compositionId

        acProvider.getAcInstancesByCompositionId(compositionId) >> []
        acDefinitionProvider.getAcDefinition(compositionId) >> acmDefinition
        def provider = createProvider(Mock(AcRuntimeParameterGroup))

        when:
        provider.updateCompositionDefinition(compositionId, InstantiationUtils.getToscaServiceTemplate(TOSCA_YAML))

        then:
        def ex = thrown(PfModelRuntimeException)
        ex.message.contains("ACM not in COMMISSIONED state, Update of ACM Definition not allowed")
    }


}
