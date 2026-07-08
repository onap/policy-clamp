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

    static final EXPECTED_AFFECTED_DEFINITIONS_COUNT = 7
    static final LATCH_TIMEOUT_SECONDS = 5
    static final ERROR_MESSAGES = [
            deleteWithInstances         : "Delete instances, to commission automation composition definitions",
            deleteNotCommissioned       : "ACM not in COMMISSIONED state, Delete of ACM Definition not allowed",
            primingWithInstances        : "There are instances, Priming/Depriming not allowed",
            primingWithTargetComposition: "This compositionId is referenced as a targetCompositionId in the instance table.",
            updateWithInstances         : "There are ACM instances, Update of ACM Definition not allowed",
            updateNotCommissioned       : "ACM not in COMMISSIONED state, Update of ACM Definition not allowed"
    ]

    def acDefinitionProvider = Mock(AcDefinitionProvider)
    def acProvider = Mock(AutomationCompositionProvider)
    def participantProvider = Mock(ParticipantProvider)
    def participantPrimePublisher = Mock(ParticipantPrimePublisher)

    // --- getAutomationCompositionDefinitions ---

    def "getAutomationCompositionDefinitions should return expected results based on templates"() {
        given: "a provider configured with the specified templates"
        def commissioningProvider = createProvider(Mock(AcRuntimeParameterGroup))
        commissioningProvider.acDefinitionProvider.getServiceTemplateList(*_) >> templates

        when: "querying for automation composition definitions"
        def result = commissioningProvider
                .getAutomationCompositionDefinitions(null, null, null, Pageable.unpaged())

        then: "the result should contain the expected number of service templates"
        result.serviceTemplates.size() == expectedSize

        where:
        templates                    || expectedSize
        []                           || 0
        [new ToscaServiceTemplate()] || 1
    }

    def "given a null pageable parameter, getAutomationCompositionDefinitions should throw NullPointerException"() {
        given: "a provider with no special configuration"
        def commissioningProvider = createProvider(Mock(AcRuntimeParameterGroup))

        when: "querying with null pageable"
        commissioningProvider.getAutomationCompositionDefinitions(null, null, null, null)

        then: "a NullPointerException should be thrown"
        thrown(NullPointerException)
    }

    // --- createAutomationCompositionDefinition ---

    def "given a valid service template, compositionDefinition should return affected definitions with NO_ERROR state"() {
        given: "a valid service template and a mocked definition provider that returns a new definition"
        def serviceTemplate = loadServiceTemplate()
        serviceTemplate.name = "Name"
        serviceTemplate.version = "1.0.0"

        def acmDefinition = new AutomationCompositionDefinition(
                compositionId: UUID.randomUUID(), serviceTemplate: serviceTemplate)

        acDefinitionProvider.createAutomationCompositionDefinition(serviceTemplate,
                CommonTestData.TOSCA_ELEMENT_NAME, CommonTestData.TOSCA_COMP_NAME) >> acmDefinition

        def commissioningProvider = createProvider(CommonTestData.getTestParamaterGroup())

        when: "creating an automation composition definition"
        def result = commissioningProvider.createAutomationCompositionDefinition(serviceTemplate)

        then: "the result should contain the expected number of affected definitions and state should be NO_ERROR"
        result.affectedAutomationCompositionDefinitions.size() == EXPECTED_AFFECTED_DEFINITIONS_COUNT
        acmDefinition.stateChangeResult == StateChangeResult.NO_ERROR
    }

    // --- deleteAutomationCompositionDefinition ---

    def "given existing instances, delete should fail with an error about instances"() {
        given: "a composition that has associated automation composition instances"
        def compositionId = UUID.randomUUID()
        acProvider.getAcInstancesByCompositionId(compositionId) >> [new AutomationComposition()]
        def commissioningProvider = createProvider()

        when: "attempting to delete the composition definition"
        commissioningProvider.deleteAutomationCompositionDefinition(compositionId)

        then: "a PfModelRuntimeException should be thrown indicating instances exist"
        def ex = thrown(PfModelRuntimeException)
        ex.message.contains(ERROR_MESSAGES["deleteWithInstances"])
    }

    def "given a definition in PRIMED state, delete should fail with a not-commissioned error"() {
        given: "a composition definition in PRIMED state with no instances"
        def compositionId = UUID.randomUUID()
        def acmDefinition = new AutomationCompositionDefinition(
                compositionId: compositionId, state: AcTypeState.PRIMED,
                serviceTemplate: loadServiceTemplate())

        acProvider.getAcInstancesByCompositionId(compositionId) >> []
        acDefinitionProvider.getAcDefinition(compositionId) >> acmDefinition
        def commissioningProvider = createProvider()

        when: "attempting to delete the composition definition"
        commissioningProvider.deleteAutomationCompositionDefinition(compositionId)

        then: "a PfModelRuntimeException should be thrown indicating the definition is not in COMMISSIONED state"
        def ex = thrown(PfModelRuntimeException)
        ex.message.contains(ERROR_MESSAGES["deleteNotCommissioned"])
    }

    def "given a COMMISSIONED definition with no instances, delete should succeed and invoke deleteAcDefinition"() {
        given: "a composition definition in COMMISSIONED state with no instances"
        def compositionId = UUID.randomUUID()
        def serviceTemplate = loadServiceTemplate()
        def acmDefinition = new AutomationCompositionDefinition(
                compositionId: compositionId, state: AcTypeState.COMMISSIONED, serviceTemplate: serviceTemplate)

        acProvider.getAcInstancesByCompositionId(compositionId) >> []
        acDefinitionProvider.getAcDefinition(compositionId) >> acmDefinition
        acDefinitionProvider.deleteAcDefinition(compositionId) >> serviceTemplate
        def commissioningProvider = createProvider()

        when: "deleting the composition definition"
        commissioningProvider.deleteAutomationCompositionDefinition(compositionId)

        then: "deleteAcDefinition should be called exactly once on the provider"
        1 * acDefinitionProvider.deleteAcDefinition(compositionId) >> serviceTemplate
    }

    // --- compositionDefinitionPriming ---

    def "given a COMMISSIONED definition with PRIME order, priming should trigger sendPriming and update the definition"() {
        given: "a COMMISSIONED definition with no instances"
        def acmDefinition = CommonTestData.createAcDefinition(loadServiceTemplate(),
                AcTypeState.COMMISSIONED)
        def compositionId = acmDefinition.compositionId

        acDefinitionProvider.getAcDefinition(compositionId) >> acmDefinition
        acProvider.getAcInstancesByCompositionId(compositionId) >> []
        acProvider.getAcInstancesByTargetCompositionId(compositionId) >> []

        def latch = new CountDownLatch(1)
        participantPrimePublisher.sendPriming(_, _, _) >> { latch.countDown() }

        def commissioningProvider = createProvider(CommonTestData.getTestParamaterGroup())

        when: "compositionDefinitionPriming is called with PrimeOrder.PRIME"
        commissioningProvider.compositionDefinitionPriming(compositionId,
                new AcTypeStateUpdate(primeOrder: PrimeOrder.PRIME))

        then: "the definition should be updated"
        1 * acDefinitionProvider.updateAcDefinition(acmDefinition, CommonTestData.TOSCA_COMP_NAME)

        and: "sendPriming should be invoked asynchronously"
        latch.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    def "given a PRIMED definition with DEPRIME order, depriming should trigger sendDepriming and verify participants"() {
        given: "a PRIMED definition with no instances"
        def acmDefinition = CommonTestData.createAcDefinition(loadServiceTemplate(),
                AcTypeState.PRIMED)
        def compositionId = acmDefinition.compositionId

        acDefinitionProvider.getAcDefinition(compositionId) >> acmDefinition
        acProvider.getAcInstancesByCompositionId(compositionId) >> []
        acProvider.getAcInstancesByTargetCompositionId(compositionId) >> []

        def participantIds = acmDefinition.elementStateMap.values().stream()
                .map({ it.participantId }).collect(Collectors.toSet())

        def latch = new CountDownLatch(1)
        participantPrimePublisher.sendDepriming(compositionId, participantIds, _ as UUID) >> { latch.countDown() }

        def commissioningProvider = createProvider(CommonTestData.getTestParamaterGroup())

        when: "compositionDefinitionPriming is called with PrimeOrder.DEPRIME"
        commissioningProvider.compositionDefinitionPriming(compositionId, new AcTypeStateUpdate(primeOrder: PrimeOrder.DEPRIME))

        then: "participant state should be verified"
        1 * participantProvider.verifyParticipantState(participantIds)

        and: "sendDepriming should be invoked asynchronously"
        latch.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    // --- bad request scenarios ---

    def "priming with existing instances should fail with instances error"() {
        given: "a composition that has associated instances"
        def compositionId = UUID.randomUUID()
        acProvider.getAcInstancesByCompositionId(compositionId) >> [new AutomationComposition()]
        acProvider.getAcInstancesByTargetCompositionId(compositionId) >> []
        def provider = createProvider(CommonTestData.getTestParamaterGroup())

        when: "attempting to prime"
        provider.compositionDefinitionPriming(compositionId, new AcTypeStateUpdate())

        then:
        def ex = thrown(PfModelRuntimeException)
        ex.message.contains(ERROR_MESSAGES["primingWithInstances"])
    }

    def "priming with target composition instances should fail with target composition error"() {
        given: "a composition referenced as targetCompositionId"
        def compositionId = UUID.randomUUID()
        acProvider.getAcInstancesByCompositionId(compositionId) >> []
        acProvider.getAcInstancesByTargetCompositionId(compositionId) >> [new AutomationComposition()]
        def provider = createProvider(CommonTestData.getTestParamaterGroup())

        when: "attempting to prime"
        provider.compositionDefinitionPriming(compositionId, new AcTypeStateUpdate())

        then:
        def ex = thrown(PfModelRuntimeException)
        ex.message.contains(ERROR_MESSAGES["primingWithTargetComposition"])
    }

    def "updating with existing instances should fail with instances error"() {
        given: "a composition that has associated instances"
        def compositionId = UUID.randomUUID()
        acProvider.getAcInstancesByCompositionId(compositionId) >> [new AutomationComposition()]
        def provider = createProvider()

        when: "attempting to update"
        provider.updateCompositionDefinition(compositionId, new ToscaServiceTemplate())

        then:
        def ex = thrown(PfModelRuntimeException)
        ex.message.contains(ERROR_MESSAGES["updateWithInstances"])
    }

    def "updating a PRIMED definition should fail with not-commissioned error"() {
        given: "a composition in PRIMED state with no instances"
        def compositionId = UUID.randomUUID()
        def acmDefinition = CommonTestData.createAcDefinition(loadServiceTemplate(), AcTypeState.PRIMED)
        acmDefinition.compositionId = compositionId

        acProvider.getAcInstancesByCompositionId(compositionId) >> []
        acDefinitionProvider.getAcDefinition(compositionId) >> acmDefinition
        def provider = createProvider()

        when: "attempting to update"
        provider.updateCompositionDefinition(compositionId, loadServiceTemplate())

        then:
        def ex = thrown(PfModelRuntimeException)
        ex.message.contains(ERROR_MESSAGES["updateNotCommissioned"])
    }

    // --- getAutomationCompositionDefinition (single) ---

    def "given a compositionId, getAutomationCompositionDefinition should return the definition"() {
        given: "a definition provider that returns a definition for the given compositionId"
        def compositionId = UUID.randomUUID()
        def acmDefinition = new AutomationCompositionDefinition(
                compositionId: compositionId, serviceTemplate: loadServiceTemplate())
        acDefinitionProvider.getAcDefinition(compositionId) >> acmDefinition
        def commissioningProvider = createProvider()

        when: "fetching the automation composition definition by ID"
        def result = commissioningProvider.getAutomationCompositionDefinition(compositionId)

        then: "the returned definition should match the expected compositionId"
        result.compositionId == compositionId
    }

    // --- updateCompositionDefinition success ---

    def "given a COMMISSIONED definition with no instances, updateCompositionDefinition should succeed"() {
        given: "a COMMISSIONED definition with no instances"
        def compositionId = UUID.randomUUID()
        def serviceTemplate = loadServiceTemplate()
        def acmDefinition = new AutomationCompositionDefinition(
                compositionId: compositionId, state: AcTypeState.COMMISSIONED, serviceTemplate: serviceTemplate)

        acProvider.getAcInstancesByCompositionId(compositionId) >> []
        acDefinitionProvider.getAcDefinition(compositionId) >> acmDefinition
        def commissioningProvider = createProvider(CommonTestData.getTestParamaterGroup())

        when: "updating the composition definition"
        def result = commissioningProvider.updateCompositionDefinition(compositionId, serviceTemplate)

        then: "updateServiceTemplate should be called and response should contain affected definitions"
        1 * acDefinitionProvider.updateServiceTemplate(compositionId, serviceTemplate,
                CommonTestData.TOSCA_ELEMENT_NAME, CommonTestData.TOSCA_COMP_NAME)
        result.affectedAutomationCompositionDefinitions.size() == EXPECTED_AFFECTED_DEFINITIONS_COUNT
    }

    // --- compositionDefinitionPriming invalid order ---

    def "given a COMMISSIONED definition with NONE order, priming should fail with not valid error"() {
        given: "a COMMISSIONED definition with no instances"
        def acmDefinition = CommonTestData.createAcDefinition(loadServiceTemplate(), AcTypeState.COMMISSIONED)
        def compositionId = acmDefinition.compositionId

        acDefinitionProvider.getAcDefinition(compositionId) >> acmDefinition
        acProvider.getAcInstancesByCompositionId(compositionId) >> []
        acProvider.getAcInstancesByTargetCompositionId(compositionId) >> []
        def commissioningProvider = createProvider(CommonTestData.getTestParamaterGroup())

        when: "compositionDefinitionPriming is called with PrimeOrder.NONE"
        commissioningProvider.compositionDefinitionPriming(compositionId, new AcTypeStateUpdate(primeOrder: PrimeOrder.NONE))

        then: "a PfModelRuntimeException should be thrown indicating the order is not valid"
        def ex = thrown(PfModelRuntimeException)
        ex.message.contains("Not valid")
    }

    // --- Helper methods ---

    static loadServiceTemplate() {
        return InstantiationUtils.getToscaServiceTemplate(CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML)
    }

    def createProvider(params = null) {
        return new CommissioningProvider(acDefinitionProvider, acProvider, participantProvider,
                new AcTypeStateResolver(), participantPrimePublisher, params)
    }
}
