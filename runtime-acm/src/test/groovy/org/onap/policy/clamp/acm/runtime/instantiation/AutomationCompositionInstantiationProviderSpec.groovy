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

    def setupPrimedDefinition(def template = helper.serviceTemplate) {
        def acDef = helper.createPrimedDefinition(template)
        helper.acDefinitionProvider.getAcDefinition(acDef.compositionId) >> acDef
        acDef.compositionId
    }

    // --- Null checks ---

    def "getAutomationCompositions throws on null #param"() {
        given:
        def provider = helper.createProvider()

        when:
        provider.getAutomationCompositions(compositionId, null, null, pageable)

        then:
        thrown(NullPointerException)

        where:
        param           | compositionId    | pageable
        "compositionId" | null             | Pageable.unpaged()
        "pageable"      | UUID.randomUUID()| null
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

    def "#scenario fails with bad request"() {
        given:
        def compositionId = setupPrimedDefinition()
        def ac = helper.loadCustomAc("acUpdate", "Crud", initialState, compositionId)
        acSetup(ac, compositionId)
        helper.acProvider.getAutomationComposition(ac.instanceId) >> ac
        def provider = helper.createProvider(new AcRuntimeParameterGroup(), Mock(EncryptionUtils))

        when:
        provider.updateAutomationComposition(compositionId, acToUpdate(ac, compositionId))

        then:
        def ex = thrown(PfModelRuntimeException)
        ex.message.contains(helper.getErrorMessage(errorKey))

        where:
        scenario                          | initialState          | errorKey
        "update in DEPLOYING"             | DeployState.DEPLOYING | "notAllowedUpdateDeploying"
        "migrate in UPDATING"             | DeployState.UPDATING  | "notAllowedMigrateUpdating"
        "migrate precheck in DEPLOYING"   | DeployState.DEPLOYED  | "notAllowedNoneDeploying"

        acSetup << [
                { a, cId -> a.compositionTargetId = null },
                { a, cId -> a.lockState = LockState.LOCKED; a.compositionTargetId = UUID.randomUUID() },
                { a, cId ->
                    a.precheck = true
                    def acDefTarget = CommonTestData.createAcDefinition(helper.serviceTemplate, AcTypeState.PRIMED)
                    helper.acDefinitionProvider.getAcDefinition(acDefTarget.compositionId) >> acDefTarget
                    a.deployState = DeployState.DEPLOYING
                }
        ]
        acToUpdate << [
                { a, cId -> a },
                { a, cId -> a },
                { a, cId ->
                    def acDefTarget = CommonTestData.createAcDefinition(helper.serviceTemplate, AcTypeState.PRIMED)
                    def m = new AutomationComposition(a)
                    m.compositionTargetId = acDefTarget.compositionId
                    m
                }
        ]
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

    def "rollback fails when #scenario"() {
        given:
        def compositionId = setupPrimedDefinition()
        def ac = helper.loadCustomAc("acCreate", "Rollback", DeployState.MIGRATION_REVERTING, compositionId)
        stateSetup(ac)
        helper.acProvider.getAutomationComposition(ac.instanceId) >> ac
        helper.acProvider.getAutomationCompositionRollback(_) >> new JpaAutomationCompositionRollback().toAuthorative()

        def provider = helper.createProvider(new AcRuntimeParameterGroup())

        when:
        provider.rollback(compositionId, ac.instanceId)

        then:
        thrown(PfModelRuntimeException)

        where:
        scenario                  | stateSetup
        "empty rollback record"   | { a -> /* default MIGRATION_REVERTING state */ }
        "DELETING state"          | { a -> a.deployState = DeployState.DELETING }
        "SubState PREPARING"      | { a -> a.deployState = DeployState.DEPLOYED; a.subState = SubState.PREPARING }
        "StateChangeResult FAILED"| { a -> a.deployState = DeployState.DEPLOYED; a.subState = SubState.NONE; a.stateChangeResult = StateChangeResult.FAILED }
        "compositionId mismatch"  | { a -> a.deployState = DeployState.DEPLOYED; a.subState = SubState.NONE; a.stateChangeResult = StateChangeResult.NO_ERROR; a.compositionId = UUID.randomUUID() }
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

    // --- Create/Update validation errors ---

    def "#operation fails when AC element not found in definition"() {
        given:
        def compositionId = setupPrimedDefinition()
        def ac = helper.loadAc("acElementNotFound", "AcElementNotFound")
        ac.compositionId = compositionId
        helper.acProvider.getAutomationComposition(ac.instanceId) >> ac
        def provider = helper.createProvider(CommonTestData.getTestParamaterGroup(), Mock(EncryptionUtils))

        when:
        action(provider, compositionId, ac)

        then:
        def ex = thrown(PfModelRuntimeException)
        ex.message.contains(helper.getErrorMessage("notFound"))

        where:
        operation | action
        "create"  | { p, cId, a -> p.createAutomationComposition(cId, a) }
        "update"  | { p, cId, a -> p.updateAutomationComposition(cId, a) }
    }

    def "#operation fails when AC definition not found"() {
        given:
        def ac = helper.loadAc("acDefNotFound", "AcNotFound")
        def compositionId = ac.compositionId
        helper.acProvider.getAutomationComposition(ac.instanceId) >> ac
        helper.acDefinitionProvider.getAcDefinition(compositionId) >> {
            throw new PfModelRuntimeException(Response.Status.NOT_FOUND, "definition not found")
        }
        def provider = helper.createProvider(Mock(AcRuntimeParameterGroup), null)

        when:
        action(provider, compositionId, ac)

        then:
        thrown(PfModelRuntimeException)

        where:
        operation | action
        "create"  | { p, cId, a -> p.createAutomationComposition(cId, a) }
        "update"  | { p, cId, a -> p.updateAutomationComposition(cId, a) }
    }

    def "#operation fails when compositionId does not match"() {
        given:
        def ac = helper.loadAc("acDefNotFound", "AcNotFound")
        helper.acProvider.getAutomationComposition(ac.instanceId) >> ac
        def wrongId = UUID.randomUUID()
        def provider = helper.createProvider(Mock(AcRuntimeParameterGroup), null)

        when:
        action(provider, wrongId, ac)

        then:
        def ex = thrown(PfModelRuntimeException)
        ex.message.contains(helper.getErrorMessage("doNotMatch"))

        where:
        operation          | action
        "create"           | { p, id, a -> p.createAutomationComposition(id, a) }
        "update"           | { p, id, a -> p.updateAutomationComposition(id, a) }
        "get"              | { p, id, a -> p.getAutomationComposition(id, a.instanceId) }
        "compositionState" | { p, id, a -> p.compositionInstanceState(id, a.instanceId, new AcInstanceStateUpdate()) }
    }

    def "get succeeds when compositionTargetId matches"() {
        given:
        def ac = helper.loadAc("acDefNotFound", "AcNotFound")
        def targetId = UUID.randomUUID()
        ac.compositionTargetId = targetId
        helper.acProvider.getAutomationComposition(ac.instanceId) >> ac
        def provider = helper.createProvider(Mock(AcRuntimeParameterGroup), null)

        when:
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

        def provider = new AutomationCompositionInstantiationProvider(helper.acProvider, helper.acDefinitionProvider,
                null, null, null, null, null)

        when:
        provider.createAutomationComposition(compositionId, ac)

        then:
        def ex = thrown(PfModelRuntimeException)
        ex.message.contains(helper.getErrorMessage("notPrimed"))
    }

    // --- compositionInstanceState orders ---

    def "compositionInstanceState handles #order order"() {
        given:
        def acDef = CommonTestData.createAcDefinition(helper.serviceTemplate, AcTypeState.COMMISSIONED)
        def compositionId = acDef.compositionId
        helper.acDefinitionProvider.getAcDefinition(compositionId) >> acDef

        def ac = helper.loadAc("acDefNotFound", "AcNotFound")
        def instanceId = UUID.randomUUID()
        ac.compositionId = compositionId
        ac.instanceId = instanceId
        ac.deployState = acDeployState
        ac.lockState = acLockState
        helper.acProvider.getAutomationComposition(instanceId) >> ac

        def provider = helper.createProvider(Mock(AcRuntimeParameterGroup), null)
        def update = new AcInstanceStateUpdate(
                deployOrder: deployOrder, lockOrder: lockOrder, subOrder: subOrder)

        when:
        provider.compositionInstanceState(compositionId, instanceId, update)

        then:
        1 * helper.supervisionAcHandler."$expectedMethod"(_, _)

        where:
        order     | acDeployState          | acLockState        | deployOrder         | lockOrder       | subOrder        || expectedMethod
        "DEPLOY"  | DeployState.UNDEPLOYED | LockState.NONE     | DeployOrder.DEPLOY  | LockOrder.NONE  | null            || "deploy"
        "UNDEPLOY"| DeployState.DEPLOYED   | LockState.LOCKED   | DeployOrder.UNDEPLOY| LockOrder.NONE  | null            || "undeploy"
        "UNLOCK"  | DeployState.DEPLOYED   | LockState.LOCKED   | DeployOrder.NONE    | LockOrder.UNLOCK| null            || "unlock"
        "LOCK"    | DeployState.DEPLOYED   | LockState.UNLOCKED | DeployOrder.NONE    | LockOrder.LOCK  | null            || "lock"
        "PREPARE" | DeployState.UNDEPLOYED | LockState.NONE     | DeployOrder.NONE    | LockOrder.NONE  | SubOrder.PREPARE|| "prepare"
        "REVIEW"  | DeployState.DEPLOYED   | LockState.LOCKED   | DeployOrder.NONE    | LockOrder.NONE  | SubOrder.REVIEW || "review"
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
