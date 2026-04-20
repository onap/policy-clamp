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

import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup
import org.onap.policy.clamp.acm.runtime.main.utils.EncryptionUtils
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionAcHandler
import org.onap.policy.clamp.acm.runtime.util.CommonTestData
import org.onap.policy.clamp.models.acm.concepts.*
import org.onap.policy.clamp.models.acm.document.concepts.DocToscaServiceTemplate
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.AcInstanceStateUpdate
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.LockOrder
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.SubOrder
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaAutomationCompositionRollback
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider
import org.onap.policy.clamp.models.acm.persistence.provider.AcInstanceStateResolver
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider
import org.onap.policy.clamp.models.acm.persistence.provider.ProviderUtils
import org.onap.policy.models.base.PfModelRuntimeException
import org.onap.policy.models.base.PfUtils
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate
import org.springframework.data.domain.Pageable
import spock.lang.Shared
import spock.lang.Specification

class AutomationCompositionInstantiationProviderSpec extends Specification {

    static final AC_CREATE_JSON = "src/test/resources/rest/acm/AutomationComposition.json"
    static final AC_UPDATE_JSON = "src/test/resources/rest/acm/AutomationCompositionUpdate.json"
    static final AC_MIGRATE_JSON = "src/test/resources/rest/acm/AutomationCompositionMigrate.json"
    static final AC_ELEMENT_NOT_FOUND_JSON = "src/test/resources/rest/acm/AutomationCompositionElementsNotFound.json"
    static final AC_DEF_NOT_FOUND_JSON = "src/test/resources/rest/acm/AutomationCompositionNotFound.json"
    static final MIGRATION_YAML = "clamp/acm/pmsh/funtional-pmsh-usecase-migration.yaml"
    static final DO_NOT_MATCH = " do not match with "

    @Shared
    ToscaServiceTemplate serviceTemplate

    @Shared
    ToscaServiceTemplate serviceTemplateMigration

    // shared mocks
    AutomationCompositionProvider acProvider
    AcDefinitionProvider acDefinitionProvider
    SupervisionAcHandler supervisionAcHandler
    ParticipantProvider participantProvider

    def setupSpec() {
        serviceTemplate = toAuthorative(CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML)
        serviceTemplateMigration = toAuthorative(MIGRATION_YAML)
    }

    def setup() {
        acProvider = Mock()
        acDefinitionProvider = Mock()
        supervisionAcHandler = Mock()
        participantProvider = Mock()
    }

    private static ToscaServiceTemplate toAuthorative(String path) {
        def st = InstantiationUtils.getToscaServiceTemplate(path)
        ProviderUtils.getJpaAndValidate(st, DocToscaServiceTemplate::new, "toscaServiceTemplate").toAuthorative()
    }

    private AutomationCompositionInstantiationProvider createProvider(
            AcRuntimeParameterGroup params = CommonTestData.getTestParamaterGroup(),
            EncryptionUtils encryption = new EncryptionUtils(CommonTestData.getTestParamaterGroup())) {
        new AutomationCompositionInstantiationProvider(acProvider, acDefinitionProvider,
                new AcInstanceStateResolver(), supervisionAcHandler, participantProvider, params, encryption)
    }

    private UUID setupPrimedDefinition(ToscaServiceTemplate template = serviceTemplate) {
        def acDef = CommonTestData.createAcDefinition(template, AcTypeState.PRIMED)
        acDefinitionProvider.getAcDefinition(acDef.compositionId) >> acDef
        acDef.compositionId
    }

    // --- Null checks ---

    def "getAutomationCompositions throws on null compositionId or pageable"() {
        given:
        def provider = createProvider()

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
        def ac = InstantiationUtils.getAutomationCompositionFromResource(AC_CREATE_JSON, "Crud")
        ac.compositionId = compositionId
        acProvider.createAutomationComposition(ac) >> ac

        def provider = createProvider()

        when: "create"
        def response = provider.createAutomationComposition(compositionId, ac)

        then:
        response != null

        when: "get"
        acProvider.getAutomationCompositions(compositionId, ac.name, ac.version, Pageable.unpaged()) >> [ac]
        def result = provider.getAutomationCompositions(compositionId, ac.name, ac.version, Pageable.unpaged())

        then:
        result.automationCompositionList[0] == ac

        when: "update"
        def updateAc = InstantiationUtils.getAutomationCompositionFromResource(AC_UPDATE_JSON, "Crud")
        updateAc.compositionId = compositionId
        acProvider.getAutomationComposition(updateAc.instanceId) >> updateAc
        acProvider.updateAutomationComposition(updateAc) >> updateAc
        def updateResp = provider.updateAutomationComposition(compositionId, updateAc)

        then:
        updateResp != null

        when: "delete"
        provider.deleteAutomationComposition(compositionId, ac.instanceId)

        then:
        1 * acProvider.getAutomationComposition(ac.instanceId) >> ac
        1 * supervisionAcHandler.delete(_, _)
    }

    // --- Update deployed instance ---

    def "update deployed instance triggers supervision update"() {
        given:
        def compositionId = setupPrimedDefinition()
        def ac = InstantiationUtils.getCustomAutomationComposition(AC_UPDATE_JSON, "Crud", DeployState.DEPLOYED, compositionId)
        ac.compositionTargetId = null
        def acFromDb = new AutomationComposition(ac)

        acProvider.getAutomationComposition(ac.instanceId) >> acFromDb
        acProvider.updateAutomationComposition(acFromDb) >> acFromDb

        def provider = createProvider()

        when:
        provider.updateAutomationComposition(compositionId, ac)

        then:
        1 * supervisionAcHandler.update(_, _)
    }

    def "update fails when element IDs mismatch"() {
        given:
        def compositionId = setupPrimedDefinition()
        def ac = InstantiationUtils.getCustomAutomationComposition(AC_UPDATE_JSON, "Crud", DeployState.DEPLOYED, compositionId)
        ac.compositionTargetId = null

        def acFromDb = new AutomationComposition(ac)
        acFromDb.deployState = DeployState.DEPLOYED
        acFromDb.elements.values().each { it.deployState = DeployState.DEPLOYED }

        // Randomize element IDs
        def elements = new ArrayList(ac.elements.values())
        ac.elements.clear()
        elements.each { it.id = UUID.randomUUID(); ac.elements.put(it.id, it) }

        acProvider.getAutomationComposition(ac.instanceId) >> acFromDb
        def provider = createProvider()

        when:
        provider.updateAutomationComposition(compositionId, ac)

        then:
        def ex = thrown(Exception)
        ex.message.startsWith("Element id not present")
    }

    // --- Update bad requests ---

    def "update fails in DEPLOYING state"() {
        given:
        def compositionId = setupPrimedDefinition()
        def ac = InstantiationUtils.getCustomAutomationComposition(AC_UPDATE_JSON, "Crud", DeployState.DEPLOYING, compositionId)
        ac.compositionTargetId = null
        acProvider.getAutomationComposition(ac.instanceId) >> ac
        def provider = createProvider(Mock(AcRuntimeParameterGroup), Mock(EncryptionUtils))

        when:
        provider.updateAutomationComposition(compositionId, ac)

        then:
        def ex = thrown(PfModelRuntimeException)
        ex.message.contains("Not allowed to UPDATE in the state DEPLOYING")
    }

    def "migrate fails in UPDATING state"() {
        given:
        def compositionId = setupPrimedDefinition()
        def ac = InstantiationUtils.getCustomAutomationComposition(AC_UPDATE_JSON, "Crud", DeployState.UPDATING, compositionId)
        ac.lockState = LockState.LOCKED
        ac.compositionTargetId = UUID.randomUUID()
        acProvider.getAutomationComposition(ac.instanceId) >> ac
        def provider = createProvider(Mock(AcRuntimeParameterGroup), Mock(EncryptionUtils))

        when:
        provider.updateAutomationComposition(compositionId, ac)

        then:
        def ex = thrown(PfModelRuntimeException)
        ex.message.contains("Not allowed to MIGRATE in the state UPDATING")
    }

    def "migrate precheck fails in DEPLOYING state"() {
        given:
        def compositionId = setupPrimedDefinition()
        def ac = InstantiationUtils.getCustomAutomationComposition(AC_UPDATE_JSON, "Crud", DeployState.DEPLOYED, compositionId)
        ac.precheck = true

        def acDefTarget = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED)
        acDefinitionProvider.getAcDefinition(acDefTarget.compositionId) >> acDefTarget

        def acMigrate = new AutomationComposition(ac)
        acMigrate.compositionTargetId = acDefTarget.compositionId
        ac.deployState = DeployState.DEPLOYING
        ac.precheck = true

        acProvider.getAutomationComposition(acMigrate.instanceId) >> ac
        def provider = createProvider(new AcRuntimeParameterGroup(), Mock(EncryptionUtils))

        when:
        provider.updateAutomationComposition(compositionId, acMigrate)

        then:
        def ex = thrown(PfModelRuntimeException)
        ex.message.startsWith("Not allowed to NONE in the state DEPLOYING")
    }

    // --- Delete ---

    def "delete fails with wrong compositionId, succeeds with correct one"() {
        given:
        def compositionId = setupPrimedDefinition()
        def ac = InstantiationUtils.getAutomationCompositionFromResource(AC_CREATE_JSON, "Delete")
        ac.stateChangeResult = StateChangeResult.NO_ERROR
        ac.compositionId = compositionId
        acProvider.getAutomationComposition(ac.instanceId) >> ac

        def wrongId = UUID.randomUUID()
        def provider = createProvider(Mock(AcRuntimeParameterGroup), Mock(EncryptionUtils))

        when: "wrong compositionId"
        provider.deleteAutomationComposition(wrongId, ac.instanceId)

        then:
        def ex = thrown(PfModelRuntimeException)
        ex.message.contains(compositionId.toString() + DO_NOT_MATCH + wrongId.toString())

        when: "correct compositionId"
        ac.deployState = DeployState.UNDEPLOYED
        ac.lockState = LockState.NONE
        provider.deleteAutomationComposition(compositionId, ac.instanceId)

        then:
        1 * supervisionAcHandler.delete(_, _)
    }

    def "delete fails for invalid deploy states"() {
        given:
        def ac = InstantiationUtils.getAutomationCompositionFromResource(AC_CREATE_JSON, "Delete")
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
        def ac = InstantiationUtils.getCustomAutomationComposition(AC_CREATE_JSON, "Rollback", DeployState.MIGRATION_REVERTING, compositionId)
        acProvider.getAutomationComposition(ac.instanceId) >> ac
        acProvider.getAutomationCompositionRollback(_) >> new JpaAutomationCompositionRollback().toAuthorative()

        def provider = createProvider(new AcRuntimeParameterGroup())

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
        0 * acProvider.updateAutomationComposition(_)
    }

    def "rollback succeeds for migration revert"() {
        given:
        def compositionId = setupPrimedDefinition()
        def ac = InstantiationUtils.getCustomAutomationComposition(AC_MIGRATE_JSON, "Crud", DeployState.MIGRATING, compositionId)
        ac.stateChangeResult = StateChangeResult.FAILED
        def compositionTargetId = ac.compositionTargetId

        acProvider.getAutomationComposition(ac.instanceId) >> ac
        acProvider.updateAutomationComposition(_) >> ac

        def rollback = new AutomationCompositionRollback(
                compositionId: compositionTargetId, instanceId: ac.instanceId,
                elements: InstantiationUtils.getAutomationCompositionFromResource(AC_UPDATE_JSON, "Crud").elements)
        acProvider.getAutomationCompositionRollback(ac.instanceId) >> rollback

        def acDefTarget = CommonTestData.createAcDefinition(serviceTemplateMigration, AcTypeState.PRIMED)
        acDefinitionProvider.getAcDefinition(compositionTargetId) >> acDefTarget

        def provider = createProvider(new AcRuntimeParameterGroup())

        when:
        provider.rollback(compositionId, ac.instanceId)

        then:
        1 * acProvider.updateAutomationComposition(_)
    }

    def "rollback fails when target definition not found"() {
        given:
        def compositionId = setupPrimedDefinition()
        def ac = InstantiationUtils.getCustomAutomationComposition(AC_MIGRATE_JSON, "Crud", DeployState.MIGRATING, compositionId)
        ac.stateChangeResult = StateChangeResult.FAILED
        def compositionTargetId = ac.compositionTargetId

        acProvider.getAutomationComposition(ac.instanceId) >> ac

        def rollback = new AutomationCompositionRollback(
                compositionId: compositionTargetId, instanceId: ac.instanceId,
                elements: InstantiationUtils.getAutomationCompositionFromResource(AC_UPDATE_JSON, "Crud").elements)
        acProvider.getAutomationCompositionRollback(ac.instanceId) >> rollback

        acDefinitionProvider.getAcDefinition(compositionTargetId) >> {
            throw new PfModelRuntimeException(jakarta.ws.rs.core.Response.Status.NOT_FOUND, "not found")
        }

        def provider = createProvider(new AcRuntimeParameterGroup())

        when:
        provider.rollback(compositionId, ac.instanceId)

        then:
        thrown(PfModelRuntimeException)
    }

    def "update rollback succeeds"() {
        given:
        def compositionId = setupPrimedDefinition()
        def ac = InstantiationUtils.getCustomAutomationComposition(AC_UPDATE_JSON, "Crud", DeployState.UPDATING, compositionId)
        ac.stateChangeResult = StateChangeResult.FAILED
        ac.compositionTargetId = null

        acProvider.getAutomationComposition(ac.instanceId) >> ac
        acProvider.updateAutomationComposition(_) >> ac

        def rollback = new AutomationCompositionRollback(
                instanceId: ac.instanceId,
                elements: InstantiationUtils.getAutomationCompositionFromResource(AC_UPDATE_JSON, "Crud").elements)
        acProvider.getAutomationCompositionRollback(ac.instanceId) >> rollback

        def provider = createProvider(new AcRuntimeParameterGroup())

        when:
        provider.rollback(compositionId, ac.instanceId)

        then:
        1 * acProvider.updateAutomationComposition(_)
        1 * supervisionAcHandler.update(_, _)
    }

    // --- Duplicates ---

    def "create rejects duplicates"() {
        given:
        def compositionId = setupPrimedDefinition()
        def ac = InstantiationUtils.getAutomationCompositionFromResource(AC_CREATE_JSON, "NoDuplicates")
        ac.compositionId = compositionId
        ac.instanceId = UUID.randomUUID()
        def acId = PfUtils.getKey(ac).asIdentifier()

        acProvider.createAutomationComposition(ac) >> ac
        def provider = createProvider()

        when: "first create succeeds"
        def resp = provider.createAutomationComposition(compositionId, ac)
        then:
        resp != null

        when: "duplicate fails"
        acProvider.validateNameVersion(acId) >> {
            throw new PfModelRuntimeException(jakarta.ws.rs.core.Response.Status.BAD_REQUEST, "$acId already defined")
        }
        provider.createAutomationComposition(compositionId, ac)

        then:
        def ex = thrown(PfModelRuntimeException)
        ex.message.contains("already defined")
    }

    // --- Element not found ---

    def "create and update fail when AC element not found in definition"() {
        given:
        def compositionId = setupPrimedDefinition()
        def ac = InstantiationUtils.getAutomationCompositionFromResource(AC_ELEMENT_NOT_FOUND_JSON, "AcElementNotFound")
        ac.compositionId = compositionId
        acProvider.getAutomationComposition(ac.instanceId) >> ac

        def provider = createProvider(CommonTestData.getTestParamaterGroup(), Mock(EncryptionUtils))

        when:
        provider.createAutomationComposition(compositionId, ac)
        then:
        def ex1 = thrown(PfModelRuntimeException)
        ex1.message.contains("Not found")

        when:
        provider.updateAutomationComposition(compositionId, ac)
        then:
        def ex2 = thrown(PfModelRuntimeException)
        ex2.message.contains("Not found")
    }

    // --- Definition not found ---

    def "create and update fail when AC definition not found"() {
        given:
        def ac = InstantiationUtils.getAutomationCompositionFromResource(AC_DEF_NOT_FOUND_JSON, "AcNotFound")
        def compositionId = ac.compositionId
        acProvider.getAutomationComposition(ac.instanceId) >> ac
        acDefinitionProvider.getAcDefinition(compositionId) >> {
            throw new PfModelRuntimeException(jakarta.ws.rs.core.Response.Status.NOT_FOUND, "definition not found")
        }

        def provider = createProvider(Mock(AcRuntimeParameterGroup), null)

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
        def ac = InstantiationUtils.getAutomationCompositionFromResource(AC_DEF_NOT_FOUND_JSON, "AcNotFound")
        acProvider.getAutomationComposition(ac.instanceId) >> ac
        def wrongId = UUID.randomUUID()
        def provider = createProvider(Mock(AcRuntimeParameterGroup), null)

        when:
        provider.createAutomationComposition(wrongId, ac)
        then:
        def ex1 = thrown(PfModelRuntimeException)
        ex1.message.contains(DO_NOT_MATCH)

        when:
        provider.updateAutomationComposition(wrongId, ac)
        then:
        def ex2 = thrown(PfModelRuntimeException)
        ex2.message.contains(DO_NOT_MATCH)

        when:
        provider.getAutomationComposition(wrongId, ac.instanceId)
        then:
        def ex3 = thrown(PfModelRuntimeException)
        ex3.message.contains(DO_NOT_MATCH)

        when:
        provider.compositionInstanceState(wrongId, ac.instanceId, new AcInstanceStateUpdate())
        then:
        def ex4 = thrown(PfModelRuntimeException)
        ex4.message.contains(DO_NOT_MATCH)

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
        def acDef = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.COMMISSIONED)
        def compositionId = acDef.compositionId
        acDefinitionProvider.getAcDefinition(compositionId) >> acDef

        def ac = InstantiationUtils.getAutomationCompositionFromResource(AC_CREATE_JSON, "Crud")
        ac.compositionId = compositionId

        def provider = new AutomationCompositionInstantiationProvider(
                acProvider, acDefinitionProvider, null, null, null, null, null)

        when:
        provider.createAutomationComposition(compositionId, ac)

        then:
        def ex = thrown(PfModelRuntimeException)
        ex.message.contains("not primed")
    }

    // --- compositionInstanceState orders ---

    def "compositionInstanceState handles deploy, undeploy, lock, unlock, prepare, review"() {
        given:
        def acDef = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.COMMISSIONED)
        def compositionId = acDef.compositionId
        acDefinitionProvider.getAcDefinition(compositionId) >> acDef

        def ac = InstantiationUtils.getAutomationCompositionFromResource(AC_DEF_NOT_FOUND_JSON, "AcNotFound")
        def instanceId = UUID.randomUUID()
        ac.compositionId = compositionId
        ac.instanceId = instanceId
        acProvider.getAutomationComposition(instanceId) >> ac

        def provider = createProvider(Mock(AcRuntimeParameterGroup), null)
        def update = new AcInstanceStateUpdate()

        when: "DEPLOY"
        update.deployOrder = DeployOrder.DEPLOY
        update.lockOrder = LockOrder.NONE
        provider.compositionInstanceState(compositionId, instanceId, update)
        then:
        1 * supervisionAcHandler.deploy(_, _)

        when: "UNDEPLOY"
        ac.deployState = DeployState.DEPLOYED
        ac.lockState = LockState.LOCKED
        update.deployOrder = DeployOrder.UNDEPLOY
        provider.compositionInstanceState(compositionId, instanceId, update)
        then:
        1 * supervisionAcHandler.undeploy(_, _)

        when: "UNLOCK"
        ac.deployState = DeployState.DEPLOYED
        ac.lockState = LockState.LOCKED
        update.deployOrder = DeployOrder.NONE
        update.lockOrder = LockOrder.UNLOCK
        provider.compositionInstanceState(compositionId, instanceId, update)
        then:
        1 * supervisionAcHandler.unlock(_, _)

        when: "LOCK"
        ac.deployState = DeployState.DEPLOYED
        ac.lockState = LockState.UNLOCKED
        update.deployOrder = DeployOrder.NONE
        update.lockOrder = LockOrder.LOCK
        provider.compositionInstanceState(compositionId, instanceId, update)
        then:
        1 * supervisionAcHandler.lock(_, _)

        when: "PREPARE"
        ac.deployState = DeployState.UNDEPLOYED
        ac.lockState = LockState.NONE
        update.deployOrder = DeployOrder.NONE
        update.lockOrder = LockOrder.NONE
        update.subOrder = SubOrder.PREPARE
        provider.compositionInstanceState(compositionId, instanceId, update)
        then:
        1 * supervisionAcHandler.prepare(_, _)

        when: "REVIEW"
        ac.deployState = DeployState.DEPLOYED
        ac.lockState = LockState.LOCKED
        update.subOrder = SubOrder.REVIEW
        provider.compositionInstanceState(compositionId, instanceId, update)
        then:
        1 * supervisionAcHandler.review(_, _)
    }

    // --- Helper ---

    private void assertDeleteThrows(AutomationComposition ac, DeployState deployState, LockState lockState) {
        ac.deployState = deployState
        ac.lockState = lockState

        def localAcProvider = Mock(AutomationCompositionProvider)
        def localAcDefProvider = Mock(AcDefinitionProvider)
        localAcProvider.getAutomationComposition(ac.instanceId) >> ac

        def acDef = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED)
        localAcDefProvider.getAcDefinition(acDef.compositionId) >> acDef
        ac.compositionId = acDef.compositionId

        def provider = new AutomationCompositionInstantiationProvider(localAcProvider, localAcDefProvider,
                new AcInstanceStateResolver(), null, Mock(ParticipantProvider),
                Mock(AcRuntimeParameterGroup), null)

        try {
            provider.deleteAutomationComposition(acDef.compositionId, ac.instanceId)
            assert false, "Expected PfModelRuntimeException for state $deployState"
        } catch (PfModelRuntimeException ex) {
            assert ex.message.startsWith("Not valid order DELETE;")
        }
    }
}
