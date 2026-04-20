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
package org.onap.policy.clamp.acm.runtime.instantiation

import jakarta.ws.rs.core.Response
import org.onap.policy.clamp.acm.runtime.helper.InstantiationProviderTestHelper
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup
import org.onap.policy.clamp.acm.runtime.main.utils.EncryptionUtils
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionAcHandler
import org.onap.policy.clamp.acm.runtime.util.CommonTestData
import org.onap.policy.clamp.models.acm.concepts.*
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.AcInstanceStateUpdate
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.LockOrder
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.SubOrder
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaAutomationCompositionRollback
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider
import org.onap.policy.models.base.PfModelRuntimeException
import org.onap.policy.models.base.PfUtils
import org.springframework.data.domain.Pageable
import spock.lang.Shared
import spock.lang.Specification

class AutomationCompositionInstantiationProviderSpec extends Specification {

    @Shared
    InstantiationProviderTestHelper helper = new InstantiationProviderTestHelper()

    def setup() {
        helper.acProvider = Mock(AutomationCompositionProvider)
        helper.acDefinitionProvider = Mock(AcDefinitionProvider)
        helper.supervisionAcHandler = Mock(SupervisionAcHandler)
        helper.participantProvider = Mock(ParticipantProvider)
    }

    // --- Helpers ---

    private UUID setupPrimedDefinition(def template = helper.serviceTemplate) {
        def acDef = helper.createPrimedDefinition(template)
        helper.acDefinitionProvider.getAcDefinition(acDef.compositionId) >> acDef
        acDef.compositionId
    }

    // --- Null checks ---

    def "getAutomationCompositions throws on null compositionId or pageable"() {
        given:
        def provider = helper.createProvider()

        when:
        provider.getAutomationCompositions(null, null, null, Pageable.unpaged())

        then:
        thrown(NullPointerException)

        when:
        provider.getAutomationCompositions(UUID.randomUUID(), null, null, null)

        then:
        thrown(NullPointerException)
    }

    // --- CRUD ---

    def "create, get, update, delete lifecycle"() {
        given:
        def compositionId = setupPrimedDefinition()
        def ac = helper.loadAc("acCreate", "Crud")
        ac.compositionId = compositionId
        helper.acProvider.createAutomationComposition(ac) >> ac

        def provider = helper.createProvider()

        when: "create"
        def response = provider.createAutomationComposition(compositionId, ac)

        then:
        response != null

        when: "get"
        helper.acProvider.getAutomationCompositions(compositionId, ac.name, ac.version, Pageable.unpaged()) >> [ac]
        def result = provider.getAutomationCompositions(compositionId, ac.name, ac.version, Pageable.unpaged())

        then:
        result.automationCompositionList[0] == ac

        when: "update"
        def updateAc = helper.loadAc("acUpdate", "Crud")
        updateAc.compositionId = compositionId
        helper.acProvider.getAutomationComposition(updateAc.instanceId) >> updateAc
        helper.acProvider.updateAutomationComposition(updateAc) >> updateAc
        def updateResp = provider.updateAutomationComposition(compositionId, updateAc)

        then:
        updateResp != null

        when: "delete"
        provider.deleteAutomationComposition(compositionId, ac.instanceId)

        then:
        1 * helper.acProvider.getAutomationComposition(ac.instanceId) >> ac
        1 * helper.supervisionAcHandler.delete(_, _)
    }

    // --- Update deployed instance ---

    def "update deployed instance triggers supervision update"() {
        given:
        def compositionId = setupPrimedDefinition()
        def ac = helper.loadCustomAc("acUpdate", "Crud", DeployState.DEPLOYED, compositionId)
        ac.compositionTargetId = null
        def acFromDb = new AutomationComposition(ac)

        helper.acProvider.getAutomationComposition(ac.instanceId) >> acFromDb
        helper.acProvider.updateAutomationComposition(acFromDb) >> acFromDb

        def provider = helper.createProvider()

        when:
        provider.updateAutomationComposition(compositionId, ac)

        then:
        1 * helper.supervisionAcHandler.update(_, _)
    }

    def "update fails when element IDs mismatch"() {
        given:
        def compositionId = setupPrimedDefinition()
        def ac = helper.loadCustomAc("acUpdate", "Crud", DeployState.DEPLOYED, compositionId)
        ac.compositionTargetId = null

        def acFromDb = new AutomationComposition(ac)
        acFromDb.deployState = DeployState.DEPLOYED
        acFromDb.elements.values().each { it.deployState = DeployState.DEPLOYED }

        def elements = new ArrayList(ac.elements.values())
        ac.elements.clear()
        elements.each { it.id = UUID.randomUUID(); ac.elements.put(it.id, it) }

        helper.acProvider.getAutomationComposition(ac.instanceId) >> acFromDb
        def provider = helper.createProvider()

        when:
        provider.updateAutomationComposition(compositionId, ac)

        then:
        def ex = thrown(Exception)
        ex.message.startsWith(helper.getErrorMessage("elementIdNotPresent"))
    }

    // --- Update bad requests ---

    def "update fails in DEPLOYING state"() {
        given:
        def compositionId = setupPrimedDefinition()
        def ac = helper.loadCustomAc("acUpdate", "Crud", DeployState.DEPLOYING, compositionId)
        ac.compositionTargetId = null
        helper.acProvider.getAutomationComposition(ac.instanceId) >> ac
        def provider = helper.createProvider(Mock(AcRuntimeParameterGroup), Mock(EncryptionUtils))

        when:
        provider.updateAutomationComposition(compositionId, ac)

        then:
        def ex = thrown(PfModelRuntimeException)
        ex.message.contains(helper.getErrorMessage("notAllowedUpdateDeploying"))
    }

    def "migrate fails in UPDATING state"() {
        given:
        def compositionId = setupPrimedDefinition()
        def ac = helper.loadCustomAc("acUpdate", "Crud", DeployState.UPDATING, compositionId)
        ac.lockState = LockState.LOCKED
        ac.compositionTargetId = UUID.randomUUID()
        helper.acProvider.getAutomationComposition(ac.instanceId) >> ac
        def provider = helper.createProvider(Mock(AcRuntimeParameterGroup), Mock(EncryptionUtils))

        when:
        provider.updateAutomationComposition(compositionId, ac)

        then:
        def ex = thrown(PfModelRuntimeException)
        ex.message.contains(helper.getErrorMessage("notAllowedMigrateUpdating"))
    }

    def "migrate precheck fails in DEPLOYING state"() {
        given:
        def compositionId = setupPrimedDefinition()
        def ac = helper.loadCustomAc("acUpdate", "Crud", DeployState.DEPLOYED, compositionId)
        ac.precheck = true

        def acDefTarget = CommonTestData.createAcDefinition(helper.serviceTemplate, AcTypeState.PRIMED)
        helper.acDefinitionProvider.getAcDefinition(acDefTarget.compositionId) >> acDefTarget

        def acMigrate = new AutomationComposition(ac)
        acMigrate.compositionTargetId = acDefTarget.compositionId
        ac.deployState = DeployState.DEPLOYING
        ac.precheck = true

        helper.acProvider.getAutomationComposition(acMigrate.instanceId) >> ac
        def provider = helper.createProvider(new AcRuntimeParameterGroup(), Mock(EncryptionUtils))

        when:
        provider.updateAutomationComposition(compositionId, acMigrate)

        then:
        def ex = thrown(PfModelRuntimeException)
        ex.message.startsWith(helper.getErrorMessage("notAllowedNoneDeploying"))
    }

    // --- Delete ---

    def "delete fails with wrong compositionId, succeeds with correct one"() {
        given:
        def compositionId = setupPrimedDefinition()
        def ac = helper.loadAc("acCreate", "Delete")
        ac.stateChangeResult = StateChangeResult.NO_ERROR
        ac.compositionId = compositionId
        helper.acProvider.getAutomationComposition(ac.instanceId) >> ac

        def wrongId = UUID.randomUUID()
        def provider = helper.createProvider(Mock(AcRuntimeParameterGroup), Mock(EncryptionUtils))

        when: "wrong compositionId"
        provider.deleteAutomationComposition(wrongId, ac.instanceId)

        then:
        def ex = thrown(PfModelRuntimeException)
        ex.message.contains(compositionId.toString() + helper.getErrorMessage("doNotMatch") + wrongId.toString())

        when: "correct compositionId"
        ac.deployState = DeployState.UNDEPLOYED
        ac.lockState = LockState.NONE
        provider.deleteAutomationComposition(compositionId, ac.instanceId)

        then:
        1 * helper.supervisionAcHandler.delete(_, _)
    }

    def "delete fails for invalid deploy states"() {
        given:
        def ac = helper.loadAc("acCreate", "Delete")
        ac.stateChangeResult = StateChangeResult.NO_ERROR

        expect:
        assertDeleteThrows(ac, deployState, lockState)

        where:
        deployState              | lockState
        DeployState.DEPLOYED     | LockState.LOCKED
        DeployState.DEPLOYING    | LockState.NONE
        DeployState.UNDEPLOYING  | LockState.LOCKED
        DeployState.DELETING     | LockState.NONE
    }

    // --- Rollback ---

    def "rollback fails for various invalid states"() {
        given:
        def compositionId = setupPrimedDefinition()
        def ac = helper.loadCustomAc("acCreate", "Rollback", DeployState.MIGRATION_REVERTING, compositionId)
        helper.acProvider.getAutomationComposition(ac.instanceId) >> ac
        helper.acProvider.getAutomationCompositionRollback(_) >> new JpaAutomationCompositionRollback().toAuthorative()

        def provider = helper.createProvider(new AcRuntimeParameterGroup())

        when: "empty rollback record"
        provider.rollback(compositionId, ac.instanceId)
        then:
        thrown(PfModelRuntimeException)

        when: "DELETING state"
        ac.deployState = DeployState.DELETING
        provider.rollback(compositionId, ac.instanceId)
        then:
        thrown(PfModelRuntimeException)

        when: "SubState PREPARING"
        ac.deployState = DeployState.DEPLOYED
        ac.subState = SubState.PREPARING
        provider.rollback(compositionId, ac.instanceId)
        then:
        thrown(PfModelRuntimeException)

        when: "StateChangeResult FAILED"
        ac.subState = SubState.NONE
        ac.stateChangeResult = StateChangeResult.FAILED
        provider.rollback(compositionId, ac.instanceId)
        then:
        thrown(PfModelRuntimeException)

        when: "compositionId mismatch"
        ac.stateChangeResult = StateChangeResult.NO_ERROR
        ac.compositionId = UUID.randomUUID()
        provider.rollback(compositionId, ac.instanceId)
        then:
        thrown(PfModelRuntimeException)
        0 * helper.acProvider.updateAutomationComposition(_)
    }

    def "rollback succeeds for migration revert"() {
        given:
        def compositionId = setupPrimedDefinition()
        def ac = helper.loadCustomAc("acMigrate", "Crud", DeployState.MIGRATING, compositionId)
        ac.stateChangeResult = StateChangeResult.FAILED
        def compositionTargetId = ac.compositionTargetId

        helper.acProvider.getAutomationComposition(ac.instanceId) >> ac
        helper.acProvider.updateAutomationComposition(_) >> ac

        def rollback = new AutomationCompositionRollback(
                compositionId: compositionTargetId, instanceId: ac.instanceId,
                elements: helper.loadAc("acUpdate", "Crud").elements)
        helper.acProvider.getAutomationCompositionRollback(ac.instanceId) >> rollback

        def acDefTarget = CommonTestData.createAcDefinition(helper.serviceTemplateMigration, AcTypeState.PRIMED)
        helper.acDefinitionProvider.getAcDefinition(compositionTargetId) >> acDefTarget

        def provider = helper.createProvider(new AcRuntimeParameterGroup())

        when:
        provider.rollback(compositionId, ac.instanceId)

        then:
        1 * helper.acProvider.updateAutomationComposition(_)
    }

    def "rollback fails when target definition not found"() {
        given:
        def compositionId = setupPrimedDefinition()
        def ac = helper.loadCustomAc("acMigrate", "Crud", DeployState.MIGRATING, compositionId)
        ac.stateChangeResult = StateChangeResult.FAILED
        def compositionTargetId = ac.compositionTargetId

        helper.acProvider.getAutomationComposition(ac.instanceId) >> ac

        def rollback = new AutomationCompositionRollback(
                compositionId: compositionTargetId, instanceId: ac.instanceId,
                elements: helper.loadAc("acUpdate", "Crud").elements)
        helper.acProvider.getAutomationCompositionRollback(ac.instanceId) >> rollback

        helper.acDefinitionProvider.getAcDefinition(compositionTargetId) >> {
            throw new PfModelRuntimeException(Response.Status.NOT_FOUND, "not found")
        }

        def provider = helper.createProvider(new AcRuntimeParameterGroup())

        when:
        provider.rollback(compositionId, ac.instanceId)

        then:
        thrown(PfModelRuntimeException)
    }

    def "update rollback succeeds"() {
        given:
        def compositionId = setupPrimedDefinition()
        def ac = helper.loadCustomAc("acUpdate", "Crud", DeployState.UPDATING, compositionId)
        ac.stateChangeResult = StateChangeResult.FAILED
        ac.compositionTargetId = null

        helper.acProvider.getAutomationComposition(ac.instanceId) >> ac
        helper.acProvider.updateAutomationComposition(_ as AutomationComposition) >> ac

        def rollback = new AutomationCompositionRollback(
                instanceId: ac.instanceId,
                elements: helper.loadAc("acUpdate", "Crud").elements)
        helper.acProvider.getAutomationCompositionRollback(ac.instanceId) >> rollback

        def provider = helper.createProvider(new AcRuntimeParameterGroup())

        when:
        provider.rollback(compositionId, ac.instanceId)

        then:
        1 * helper.acProvider.updateAutomationComposition(_)
        1 * helper.supervisionAcHandler.update(_, _)
    }

    // --- Duplicates ---

    def "create rejects duplicates"() {
        given:
        def compositionId = setupPrimedDefinition()
        def ac = helper.loadAc("acCreate", "NoDuplicates")
        ac.compositionId = compositionId
        ac.instanceId = UUID.randomUUID()
        def acId = PfUtils.getKey(ac).asIdentifier()

        helper.acProvider.createAutomationComposition(ac) >> ac
        def provider = helper.createProvider()

        when: "first create succeeds"
        def resp = provider.createAutomationComposition(compositionId, ac)
        then:
        resp != null

        when: "duplicate fails"
        helper.acProvider.validateNameVersion(acId) >> {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, "$acId already defined")
        }
        provider.createAutomationComposition(compositionId, ac)

        then:
        def ex = thrown(PfModelRuntimeException)
        ex.message.contains(helper.getErrorMessage("alreadyDefined"))
    }

    // --- Element not found ---

    def "create and update fail when AC element not found in definition"() {
        given:
        def compositionId = setupPrimedDefinition()
        def ac = helper.loadAc("acElementNotFound", "AcElementNotFound")
        ac.compositionId = compositionId
        helper.acProvider.getAutomationComposition(ac.instanceId) >> ac

        def provider = helper.createProvider(CommonTestData.getTestParamaterGroup(), Mock(EncryptionUtils))

        when:
        provider.createAutomationComposition(compositionId, ac)
        then:
        def ex1 = thrown(PfModelRuntimeException)
        ex1.message.contains(helper.getErrorMessage("notFound"))

        when:
        provider.updateAutomationComposition(compositionId, ac)
        then:
        def ex2 = thrown(PfModelRuntimeException)
        ex2.message.contains(helper.getErrorMessage("notFound"))
    }

    // --- Definition not found ---

    def "create and update fail when AC definition not found"() {
        given:
        def ac = helper.loadAc("acDefNotFound", "AcNotFound")
        def compositionId = ac.compositionId
        helper.acProvider.getAutomationComposition(ac.instanceId) >> ac
        helper.acDefinitionProvider.getAcDefinition(compositionId) >> {
            throw new PfModelRuntimeException(Response.Status.NOT_FOUND, "definition not found")
        }

        def provider = helper.createProvider(Mock(AcRuntimeParameterGroup), null)

        when:
        provider.createAutomationComposition(compositionId, ac)
        then:
        thrown(PfModelRuntimeException)

        when:
        provider.updateAutomationComposition(compositionId, ac)
        then:
        thrown(PfModelRuntimeException)
    }

    // --- CompositionId mismatch ---

    def "operations fail when compositionId does not match"() {
        given:
        def ac = helper.loadAc("acDefNotFound", "AcNotFound")
        helper.acProvider.getAutomationComposition(ac.instanceId) >> ac
        def wrongId = UUID.randomUUID()
        def provider = helper.createProvider(Mock(AcRuntimeParameterGroup), null)

        when:
        provider.createAutomationComposition(wrongId, ac)
        then:
        def ex1 = thrown(PfModelRuntimeException)
        ex1.message.contains(helper.getErrorMessage("doNotMatch"))

        when:
        provider.updateAutomationComposition(wrongId, ac)
        then:
        def ex2 = thrown(PfModelRuntimeException)
        ex2.message.contains(helper.getErrorMessage("doNotMatch"))

        when:
        provider.getAutomationComposition(wrongId, ac.instanceId)
        then:
        def ex3 = thrown(PfModelRuntimeException)
        ex3.message.contains(helper.getErrorMessage("doNotMatch"))

        when:
        provider.compositionInstanceState(wrongId, ac.instanceId, new AcInstanceStateUpdate())
        then:
        def ex4 = thrown(PfModelRuntimeException)
        ex4.message.contains(helper.getErrorMessage("doNotMatch"))

        when: "compositionTargetId matches"
        def targetId = UUID.randomUUID()
        ac.compositionTargetId = targetId
        def result = provider.getAutomationComposition(targetId, ac.instanceId)
        then:
        result != null
    }

    // --- Composition not primed ---

    def "create fails when composition not primed"() {
        given:
        def acDef = CommonTestData.createAcDefinition(helper.serviceTemplate, AcTypeState.COMMISSIONED)
        def compositionId = acDef.compositionId
        helper.acDefinitionProvider.getAcDefinition(compositionId) >> acDef

        def ac = helper.loadAc("acCreate", "Crud")
        ac.compositionId = compositionId

        def provider = new AutomationCompositionInstantiationProvider(
                helper.acProvider, helper.acDefinitionProvider, null, null, null, null, null)

        when:
        provider.createAutomationComposition(compositionId, ac)

        then:
        def ex = thrown(PfModelRuntimeException)
        ex.message.contains(helper.getErrorMessage("notPrimed"))
    }

    // --- compositionInstanceState orders ---

    def "compositionInstanceState handles deploy, undeploy, lock, unlock, prepare, review"() {
        given:
        def acDef = CommonTestData.createAcDefinition(helper.serviceTemplate, AcTypeState.COMMISSIONED)
        def compositionId = acDef.compositionId
        helper.acDefinitionProvider.getAcDefinition(compositionId) >> acDef

        def ac = helper.loadAc("acDefNotFound", "AcNotFound")
        def instanceId = UUID.randomUUID()
        ac.compositionId = compositionId
        ac.instanceId = instanceId
        helper.acProvider.getAutomationComposition(instanceId) >> ac

        def provider = helper.createProvider(Mock(AcRuntimeParameterGroup), null)
        def update = new AcInstanceStateUpdate()

        when: "DEPLOY"
        update.deployOrder = DeployOrder.DEPLOY
        update.lockOrder = LockOrder.NONE
        provider.compositionInstanceState(compositionId, instanceId, update)
        then:
        1 * helper.supervisionAcHandler.deploy(_, _)

        when: "UNDEPLOY"
        ac.deployState = DeployState.DEPLOYED
        ac.lockState = LockState.LOCKED
        update.deployOrder = DeployOrder.UNDEPLOY
        provider.compositionInstanceState(compositionId, instanceId, update)
        then:
        1 * helper.supervisionAcHandler.undeploy(_, _)

        when: "UNLOCK"
        ac.deployState = DeployState.DEPLOYED
        ac.lockState = LockState.LOCKED
        update.deployOrder = DeployOrder.NONE
        update.lockOrder = LockOrder.UNLOCK
        provider.compositionInstanceState(compositionId, instanceId, update)
        then:
        1 * helper.supervisionAcHandler.unlock(_, _)

        when: "LOCK"
        ac.deployState = DeployState.DEPLOYED
        ac.lockState = LockState.UNLOCKED
        update.deployOrder = DeployOrder.NONE
        update.lockOrder = LockOrder.LOCK
        provider.compositionInstanceState(compositionId, instanceId, update)
        then:
        1 * helper.supervisionAcHandler.lock(_, _)

        when: "PREPARE"
        ac.deployState = DeployState.UNDEPLOYED
        ac.lockState = LockState.NONE
        update.deployOrder = DeployOrder.NONE
        update.lockOrder = LockOrder.NONE
        update.subOrder = SubOrder.PREPARE
        provider.compositionInstanceState(compositionId, instanceId, update)
        then:
        1 * helper.supervisionAcHandler.prepare(_, _)

        when: "REVIEW"
        ac.deployState = DeployState.DEPLOYED
        ac.lockState = LockState.LOCKED
        update.subOrder = SubOrder.REVIEW
        provider.compositionInstanceState(compositionId, instanceId, update)
        then:
        1 * helper.supervisionAcHandler.review(_, _)
    }

    // --- Delete helper ---

    private void assertDeleteThrows(AutomationComposition ac, DeployState deployState, LockState lockState) {
        def localAcProvider = Mock(AutomationCompositionProvider)
        def localAcDefProvider = Mock(AcDefinitionProvider)
        def ctx = helper.createDeleteProvider(ac, deployState, lockState,
                localAcProvider, localAcDefProvider, Mock(ParticipantProvider), Mock(AcRuntimeParameterGroup))

        localAcDefProvider.getAcDefinition(ctx.acDef.compositionId) >> ctx.acDef
        localAcProvider.getAutomationComposition(ac.instanceId) >> ac

        try {
            ctx.provider.deleteAutomationComposition(ctx.acDef.compositionId, ac.instanceId)
            assert false, "Expected PfModelRuntimeException for state $deployState"
        } catch (PfModelRuntimeException ex) {
            assert ex.message.startsWith(helper.getErrorMessage("notValidOrderDelete"))
        }
    }
}
