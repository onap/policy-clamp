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

import org.onap.policy.clamp.acm.runtime.helper.CommissioningProviderTestHelper
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
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider
import org.onap.policy.models.base.PfModelRuntimeException
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate
import org.springframework.data.domain.Pageable
import spock.lang.Shared
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

class CommissioningProviderSpec extends Specification {

    @Shared
    CommissioningProviderTestHelper helper = new CommissioningProviderTestHelper()

    def setup() {
        helper.acDefinitionProvider = Mock(AcDefinitionProvider)
        helper.acProvider = Mock(AutomationCompositionProvider)
        helper.participantProvider = Mock(ParticipantProvider)
        helper.participantPrimePublisher = Mock(ParticipantPrimePublisher)
    }

    // --- getAutomationCompositionDefinitions ---

    def "given no templates in the provider, getAutomationCompositionDefinitions should return an empty list"() {
        given: "a provider that returns no service templates"
        helper.acDefinitionProvider.getServiceTemplateList(null, null, Pageable.unpaged()) >> []
        def provider = helper.createProvider(Mock(AcRuntimeParameterGroup))

        when: "querying for automation composition definitions with no filters"
        def result = provider.getAutomationCompositionDefinitions(null, null, Pageable.unpaged())

        then: "the result should contain an empty service templates list"
        result.serviceTemplates.empty
    }

    def "given one template in the provider, getAutomationCompositionDefinitions should return it"() {
        given: "a provider that returns a single service template"
        helper.acDefinitionProvider.getServiceTemplateList(null, null, Pageable.unpaged()) >> [new ToscaServiceTemplate()]
        def provider = helper.createProvider(Mock(AcRuntimeParameterGroup))

        when: "querying for automation composition definitions"
        def result = provider.getAutomationCompositionDefinitions(null, null, Pageable.unpaged())

        then: "the result should contain exactly one service template"
        result.serviceTemplates.size() == 1
    }

    def "given a null pageable parameter, getAutomationCompositionDefinitions should throw NullPointerException"() {
        given: "a provider with no special configuration"
        def provider = helper.createProvider(Mock(AcRuntimeParameterGroup))

        when: "querying with null pageable"
        provider.getAutomationCompositionDefinitions(null, null, null)

        then: "a NullPointerException should be thrown"
        thrown(NullPointerException)
    }

    // --- createAutomationCompositionDefinition ---

    def "given a valid service template, createAutomationCompositionDefinition should return affected definitions with NO_ERROR state"() {
        given: "a valid service template and a mocked definition provider that returns a new definition"
        def serviceTemplate = helper.loadServiceTemplate()
        serviceTemplate.name = "Name"
        serviceTemplate.version = "1.0.0"

        def acmDefinition = new AutomationCompositionDefinition(
                compositionId: UUID.randomUUID(), serviceTemplate: serviceTemplate)

        helper.acDefinitionProvider.createAutomationCompositionDefinition(serviceTemplate,
                CommonTestData.TOSCA_ELEMENT_NAME, CommonTestData.TOSCA_COMP_NAME) >> acmDefinition

        def provider = helper.createProvider(CommonTestData.getTestParamaterGroup())

        when: "creating an automation composition definition"
        def result = provider.createAutomationCompositionDefinition(serviceTemplate)

        then: "the result should contain the expected number of affected definitions and state should be NO_ERROR"
        result.affectedAutomationCompositionDefinitions.size() == helper.expectedAffectedDefinitionsCount
        acmDefinition.stateChangeResult == StateChangeResult.NO_ERROR
    }

    // --- deleteAutomationCompositionDefinition ---

    def "given existing instances, delete should fail with an error about instances"() {
        given: "a composition that has associated automation composition instances"
        def compositionId = UUID.randomUUID()
        helper.acProvider.getAcInstancesByCompositionId(compositionId) >> [new AutomationComposition()]
        def provider = helper.createProvider()

        when: "attempting to delete the composition definition"
        provider.deleteAutomationCompositionDefinition(compositionId)

        then: "a PfModelRuntimeException should be thrown indicating instances exist"
        def ex = thrown(PfModelRuntimeException)
        ex.message.contains(helper.getErrorMessage("deleteWithInstances"))
    }

    def "given a definition in PRIMED state, delete should fail with a not-commissioned error"() {
        given: "a composition definition in PRIMED state with no instances"
        def compositionId = UUID.randomUUID()
        def acmDefinition = new AutomationCompositionDefinition(
                compositionId: compositionId, state: AcTypeState.PRIMED,
                serviceTemplate: helper.loadServiceTemplate())

        helper.acProvider.getAcInstancesByCompositionId(compositionId) >> []
        helper.acDefinitionProvider.getAcDefinition(compositionId) >> acmDefinition
        def provider = helper.createProvider()

        when: "attempting to delete the composition definition"
        provider.deleteAutomationCompositionDefinition(compositionId)

        then: "a PfModelRuntimeException should be thrown indicating the definition is not in COMMISSIONED state"
        def ex = thrown(PfModelRuntimeException)
        ex.message.contains(helper.getErrorMessage("deleteNotCommissioned"))
    }

    def "given a COMMISSIONED definition with no instances, delete should succeed and invoke deleteAcDefinition"() {
        given: "a composition definition in COMMISSIONED state with no instances"
        def compositionId = UUID.randomUUID()
        def serviceTemplate = helper.loadServiceTemplate()
        def acmDefinition = new AutomationCompositionDefinition(
                compositionId: compositionId, state: AcTypeState.COMMISSIONED, serviceTemplate: serviceTemplate)

        helper.acProvider.getAcInstancesByCompositionId(compositionId) >> []
        helper.acDefinitionProvider.getAcDefinition(compositionId) >> acmDefinition
        helper.acDefinitionProvider.deleteAcDefinition(compositionId) >> serviceTemplate
        def provider = helper.createProvider()

        when: "deleting the composition definition"
        provider.deleteAutomationCompositionDefinition(compositionId)

        then: "deleteAcDefinition should be called exactly once on the provider"
        1 * helper.acDefinitionProvider.deleteAcDefinition(compositionId) >> serviceTemplate
    }

    // --- compositionDefinitionPriming ---

    def "given a COMMISSIONED definition with PRIME order, priming should trigger sendPriming and update the definition"() {
        given: "a COMMISSIONED definition with no instances and a mocked prime publisher"
        def acmDefinition = CommonTestData.createAcDefinition(helper.loadServiceTemplate(), AcTypeState.COMMISSIONED)
        def compositionId = acmDefinition.compositionId

        helper.acDefinitionProvider.getAcDefinition(compositionId) >> acmDefinition
        helper.acProvider.getAcInstancesByCompositionId(compositionId) >> []
        helper.acProvider.getAcInstancesByTargetCompositionId(compositionId) >> []

        def latch = new CountDownLatch(1)
        helper.participantPrimePublisher.sendPriming(_, _, _) >> { latch.countDown() }

        def provider = helper.createProvider(CommonTestData.getTestParamaterGroup())

        when: "compositionDefinitionPriming is called with PrimeOrder.PRIME"
        provider.compositionDefinitionPriming(compositionId, new AcTypeStateUpdate(primeOrder: PrimeOrder.PRIME))

        then: "the definition should be updated in the provider"
        1 * helper.acDefinitionProvider.updateAcDefinition(acmDefinition, CommonTestData.TOSCA_COMP_NAME)

        and: "sendPriming should be invoked asynchronously"
        latch.await(helper.latchTimeoutSeconds, TimeUnit.SECONDS)
    }

    def "given a PRIMED definition with DEPRIME order, depriming should trigger sendDepriming and verify participants"() {
        given: "a PRIMED definition with no instances and a mocked deprime publisher"
        def acmDefinition = CommonTestData.createAcDefinition(helper.loadServiceTemplate(), AcTypeState.PRIMED)
        def compositionId = acmDefinition.compositionId

        helper.acDefinitionProvider.getAcDefinition(compositionId) >> acmDefinition
        helper.acProvider.getAcInstancesByCompositionId(compositionId) >> []
        helper.acProvider.getAcInstancesByTargetCompositionId(compositionId) >> []

        def participantIds = acmDefinition.elementStateMap.values().stream()
                .map({ it.participantId }).collect(Collectors.toSet())

        def latch = new CountDownLatch(1)
        helper.participantPrimePublisher.sendDepriming(compositionId, participantIds, _) >> { latch.countDown() }

        def provider = helper.createProvider(CommonTestData.getTestParamaterGroup())

        when: "compositionDefinitionPriming is called with PrimeOrder.DEPRIME"
        provider.compositionDefinitionPriming(compositionId, new AcTypeStateUpdate(primeOrder: PrimeOrder.DEPRIME))

        then: "participant state should be verified for all participant IDs"
        1 * helper.participantProvider.verifyParticipantState(participantIds)

        and: "sendDepriming should be invoked asynchronously"
        latch.await(helper.latchTimeoutSeconds, TimeUnit.SECONDS)
    }

    // --- bad request scenarios ---

    def "given existing instances, priming should fail with an error about instances"() {
        given: "a composition that has associated automation composition instances"
        def compositionId = UUID.randomUUID()
        helper.acProvider.getAcInstancesByCompositionId(compositionId) >> [new AutomationComposition()]
        def provider = helper.createProvider(CommonTestData.getTestParamaterGroup())

        when: "attempting to prime the composition definition"
        provider.compositionDefinitionPriming(compositionId, new AcTypeStateUpdate())

        then: "a PfModelRuntimeException should be thrown indicating instances exist"
        def ex = thrown(PfModelRuntimeException)
        ex.message.contains(helper.getErrorMessage("primingWithInstances"))
    }

    def "given instances with a target composition ID, priming should fail with a target composition error"() {
        given: "a composition with no direct instances but with instances referencing it as a target"
        def compositionId = UUID.randomUUID()
        helper.acProvider.getAcInstancesByCompositionId(compositionId) >> []
        helper.acProvider.getAcInstancesByTargetCompositionId(compositionId) >> [new AutomationComposition()]
        def provider = helper.createProvider(CommonTestData.getTestParamaterGroup())

        when: "attempting to prime the composition definition"
        provider.compositionDefinitionPriming(compositionId, new AcTypeStateUpdate())

        then: "a PfModelRuntimeException should be thrown indicating target composition instances exist"
        def ex = thrown(PfModelRuntimeException)
        ex.message.contains(helper.getErrorMessage("primingWithTargetComposition"))
    }

    def "given existing instances, updateCompositionDefinition should fail with an error about instances"() {
        given: "a composition that has associated automation composition instances"
        def compositionId = UUID.randomUUID()
        helper.acProvider.getAcInstancesByCompositionId(compositionId) >> [new AutomationComposition()]
        def provider = helper.createProvider(Mock(AcRuntimeParameterGroup))

        when: "attempting to update the composition definition"
        provider.updateCompositionDefinition(compositionId, new ToscaServiceTemplate())

        then: "a PfModelRuntimeException should be thrown indicating instances exist"
        def ex = thrown(PfModelRuntimeException)
        ex.message.contains(helper.getErrorMessage("updateWithInstances"))
    }

    def "given a PRIMED definition, updateCompositionDefinition should fail with a not-commissioned error"() {
        given: "a composition definition in PRIMED state with no instances"
        def compositionId = UUID.randomUUID()
        def acmDefinition = CommonTestData.createAcDefinition(helper.loadServiceTemplate(), AcTypeState.PRIMED)
        acmDefinition.compositionId = compositionId

        helper.acProvider.getAcInstancesByCompositionId(compositionId) >> []
        helper.acDefinitionProvider.getAcDefinition(compositionId) >> acmDefinition
        def provider = helper.createProvider(Mock(AcRuntimeParameterGroup))

        when: "attempting to update the composition definition"
        provider.updateCompositionDefinition(compositionId, helper.loadServiceTemplate())

        then: "a PfModelRuntimeException should be thrown indicating the definition is not in COMMISSIONED state"
        def ex = thrown(PfModelRuntimeException)
        ex.message.contains(helper.getErrorMessage("updateNotCommissioned"))
    }
}
