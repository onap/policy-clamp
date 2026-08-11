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
package org.onap.policy.clamp.acm.runtime.supervision

import org.onap.policy.clamp.acm.runtime.helper.SupervisionAcHandlerTestHelper
import org.onap.policy.clamp.acm.runtime.main.utils.EncryptionUtils
import org.onap.policy.clamp.acm.runtime.supervision.comm.*
import org.onap.policy.clamp.models.acm.concepts.*
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider
import org.onap.policy.clamp.models.acm.persistence.provider.MessageProvider
import org.onap.policy.models.base.PfModelRuntimeException
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SupervisionAcHandlerSpec extends Specification {

    def helper = new SupervisionAcHandlerTestHelper().tap {
        initMocks(
                Mock(AutomationCompositionProvider), Mock(MessageProvider),
                Mock(AutomationCompositionDeployPublisher), Mock(AutomationCompositionStateChangePublisher),
                Mock(AcElementPropertiesPublisher), Mock(AutomationCompositionMigrationPublisher),
                Mock(AcPreparePublisher), Mock(EncryptionUtils))
    }

    def awaitLatch(CountDownLatch latch) {
        latch.await(SupervisionAcHandlerTestHelper.LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    // --- Ack validation ---

    def "given #scenario, ack handler should save #expectedSaves time(s)"() {
        given: "an ack message with specific conditions"
        def ac = helper.loadAc("Crud")
        def msg = helper.buildAck(ac, deployState, lockState)
        msgModifier(msg)
        helper.acProvider.findAutomationComposition(_) >> acResult(ac)

        when: "the ack message is handled"
        helper.handler."$handlerMethod"(msg)

        then: "the message should be saved the expected number of times"
        expectedSaves * helper.messageProvider.save(_)

        where:
        data         << SupervisionAcHandlerTestHelper.ackTestData()
        scenario      = data.scenario
        expectedSaves = data.expectedSaves
        handlerMethod = data.handlerMethod
        deployState   = data.deployState
        lockState     = data.lockState
        acResult      = data.acResult
        msgModifier   = data.msgModifier
    }

    // --- Deploy / Undeploy / Delete ---

    def "given an AC in #initialState state, #operation should update the AC and send a message"() {
        given: "an automation composition in the specified state"
        def ac = helper.loadAc(suffix)
        ac.deployState = initialState
        if (initialState == DeployState.DEPLOYED) {
            ac.stateChangeResult = StateChangeResult.NO_ERROR
            ac.elements.values().each { it.deployState = DeployState.DEPLOYED }
        }
        def latch = new CountDownLatch(1)
        helper.deployPublisher.send(*_) >> { latch.countDown() }
        helper.stateChangePublisher.send(*_) >> { latch.countDown() }

        when: "#operation is called"
        action(helper.handler, ac)

        then: "the AC should be updated in the provider"
        1 * helper.acProvider.updateAutomationComposition(_)

        and: "the message should be sent asynchronously"
        awaitLatch(latch)

        where:
        operation  | suffix     | initialState            | action
        "deploy"   | "Deploy"   | DeployState.UNDEPLOYED  | { h, a -> def d = SupervisionAcHandlerTestHelper.createDefinition(); h.deploy(a, d) }
        "undeploy" | "Undeploy" | DeployState.DEPLOYED    | { h, a -> def d = SupervisionAcHandlerTestHelper.createDefinition(); h.undeploy(a, d, d) }
        "delete"   | "Deploy"   | DeployState.UNDEPLOYED  | { h, a -> def d = SupervisionAcHandlerTestHelper.createDefinition(); h.delete(a, d, d) }
    }

    def "given a FAILED #operation with multiple elements, retry should only update non-terminal elements"() {
        given: "an AC in FAILED state with multiple elements, some already in terminal state"
        def ac = helper.loadAc("Deploy")
        ac.stateChangeResult = StateChangeResult.FAILED
        ac.deployState = deployState
        ac.lockState = lockState
        ac.elements[UUID.randomUUID()] = new AutomationCompositionElement(
                deployState: elementTerminalState as DeployState ?: DeployState.UNDEPLOYED,
                lockState: elementTerminalLock as LockState ?: LockState.NONE)
        def latch = new CountDownLatch(1)
        helper.deployPublisher.send(*_) >> { latch.countDown() }
        helper.stateChangePublisher.send(*_) >> { latch.countDown() }

        when: "#operation is called"
        action(helper.handler, ac)

        then: "the AC should be updated in the provider"
        1 * helper.acProvider.updateAutomationComposition(_)

        and: "the message should be sent asynchronously"
        awaitLatch(latch)

        where:
        operation  | deployState            | lockState          | elementTerminalState    | elementTerminalLock  | action
        "deploy"   | DeployState.DEPLOYING  | LockState.NONE     | DeployState.DEPLOYED    | null                 | { h, a -> def d = SupervisionAcHandlerTestHelper.createDefinition(); h.deploy(a, d) }
        "undeploy" | DeployState.UNDEPLOYING | LockState.NONE    | DeployState.UNDEPLOYED  | null                 | { h, a -> def d = SupervisionAcHandlerTestHelper.createDefinition(); h.undeploy(a, d, d) }
        "lock"     | DeployState.DEPLOYED   | LockState.LOCKING  | null                    | LockState.LOCKED     | { h, a -> def d = SupervisionAcHandlerTestHelper.createDefinition(); h.lock(a, d) }
        "unlock"   | DeployState.DEPLOYED   | LockState.UNLOCKING | null                   | LockState.UNLOCKED   | { h, a -> def d = SupervisionAcHandlerTestHelper.createDefinition(); h.unlock(a, d) }
        "deleting" | DeployState.DELETING   | LockState.NONE     | DeployState.DELETED     | LockState.NONE       | { h, a -> def d = SupervisionAcHandlerTestHelper.createDefinition(); h.delete(a, d, d) }
    }

    def "given an AC with #migrationState migration state element, deploy should throw PfModelRuntimeException"() {
        given: "an AC in DEPLOYING state with an element that has the specified MigrationState"
        def ac = helper.loadAc("Deploy")
        ac.deployState = DeployState.DEPLOYING
        ac.elements[UUID.randomUUID()] = new AutomationCompositionElement(
                deployState: DeployState.UNDEPLOYED, migrationState: migrationState)

        when: "deploy is called"
        helper.handler.deploy(ac, new AutomationCompositionDefinition())

        then: "a PfModelRuntimeException should be thrown"
        thrown(PfModelRuntimeException)

        where:
        migrationState         | _
        MigrationState.NEW     | _
        MigrationState.REMOVED | _
    }

    // --- Lock / Unlock ---

    def "given an AC in #initialLockState state, #operation should update the AC and send a state change message"() {
        given: "an automation composition in the specified lock state"
        def ac = helper.loadAc(suffix)
        ac.lockState = initialLockState
        ac.elements.values().each { it.lockState = initialLockState }
        def latch = new CountDownLatch(1)
        helper.stateChangePublisher.send(_, _, _,_) >> { latch.countDown() }

        when: "#operation is called"
        action(helper.handler, ac)

        then: "the AC should be updated in the provider"
        1 * helper.acProvider.updateAutomationComposition(_)

        and: "the state change message should be sent asynchronously"
        awaitLatch(latch)

        where:
        operation | suffix   | initialLockState   | action
        "lock"    | "Lock"   | LockState.UNLOCKED | { h, a -> def d = SupervisionAcHandlerTestHelper.createDefinition(); h.lock(a, d) }
        "unlock"  | "UnLock" | LockState.LOCKED   | { h, a -> def d = SupervisionAcHandlerTestHelper.createDefinition(); h.unlock(a, d) }
    }

    // --- Update ---

    def "given an AC, update should send element properties asynchronously"() {
        given: "an automation composition"
        def ac = helper.loadAc("Lock")
        def latch = new CountDownLatch(1)
        helper.elementPublisher.send(_, _,_) >> { latch.countDown() }

        when: "update is called"
        helper.handler.update(ac, SupervisionAcHandlerTestHelper.createDefinition())

        then: "the element properties message should be sent asynchronously"
        awaitLatch(latch)
    }

    // --- Migrate ---

    def "given an AC, #operation should send a migration message asynchronously"() {
        given: "an automation composition"
        def ac = helper.loadAc("Migrate")
        acSetup(ac)
        def latch = new CountDownLatch(1)
        helper.migrationPublisher.send(_, _, _, _, _,_) >> { latch.countDown() }

        when: "#operation is called"
        action(helper.handler, ac)

        then: "the migration message should be sent asynchronously"
        awaitLatch(latch)

        where:
        operation         | acSetup                | action
        "migrate"         | { it.phase = 0 }       | { h, a -> def d = SupervisionAcHandlerTestHelper.createDefinition(); h.migrate(new AutomationCompositionRollback(), a, d, d) }
        "migratePrecheck" | { }                    | { h, a -> def d = SupervisionAcHandlerTestHelper.createDefinition(); h.migratePrecheck(new AutomationCompositionRollback(), a, d, d) }
    }

    // --- Prepare / Review ---

    def "given an AC, #operation should send a message asynchronously"() {
        given: "an automation composition"
        def ac = helper.loadAc("Migrate")
        def latch = new CountDownLatch(1)
        helper.preparePublisher.sendPrepare(*_) >> { latch.countDown() }
        helper.preparePublisher.sendReview(*_) >> { latch.countDown() }

        when: "#operation is called"
        helper.handler."$operation"(ac, definition)

        then: "the message should be sent asynchronously"
        awaitLatch(latch)

        where:
        operation | definition
        "prepare" | SupervisionAcHandlerTestHelper.createDefinition()
        "review"  | new AutomationCompositionDefinition()
    }
}
