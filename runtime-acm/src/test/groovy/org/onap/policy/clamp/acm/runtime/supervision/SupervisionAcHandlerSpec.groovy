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
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeployAck
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider
import org.onap.policy.clamp.models.acm.persistence.provider.MessageProvider
import org.onap.policy.models.base.PfModelRuntimeException
import spock.lang.Shared
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SupervisionAcHandlerSpec extends Specification {

    @Shared
    SupervisionAcHandlerTestHelper helper = new SupervisionAcHandlerTestHelper()

    def setup() {
        helper.initMocks(
                Mock(AutomationCompositionProvider), Mock(MessageProvider),
                Mock(AutomationCompositionDeployPublisher), Mock(AutomationCompositionStateChangePublisher),
                Mock(AcElementPropertiesPublisher), Mock(AutomationCompositionMigrationPublisher),
                Mock(AcPreparePublisher), Mock(EncryptionUtils))
    }

    // --- Ack validation ---

    def "given invalid ack messages, handleAutomationCompositionStateChangeAckMessage should not save"() {
        given: "an automation composition and an ack message built from it"
        def ac = helper.loadAc("Crud")
        def msg = helper.buildAck(ac)

        when: "stateChangeResult is null"
        msg.stateChangeResult = null
        helper.handler.handleAutomationCompositionStateChangeAckMessage(msg)

        then: "the message should not be saved"
        0 * helper.messageProvider.save(_)

        when: "stateChangeResult is TIMEOUT"
        msg.stateChangeResult = StateChangeResult.TIMEOUT
        helper.handler.handleAutomationCompositionStateChangeAckMessage(msg)

        then: "the message should not be saved"
        0 * helper.messageProvider.save(_)

        when: "automationCompositionId is null"
        msg.stateChangeResult = StateChangeResult.NO_ERROR
        msg.automationCompositionId = null
        helper.handler.handleAutomationCompositionStateChangeAckMessage(msg)

        then: "the message should not be saved"
        0 * helper.messageProvider.save(_)
    }

    def "given a valid ack and an existing AC, handleAutomationCompositionStateChangeAckMessage should save the ack"() {
        given: "an automation composition that exists in the provider"
        def ac = helper.loadAc("Crud")
        helper.acProvider.findAutomationComposition(helper.identifier) >> Optional.of(ac)

        when: "a valid ack message is handled"
        helper.handler.handleAutomationCompositionStateChangeAckMessage(helper.buildAck(ac))

        then: "the ack should be saved exactly once"
        1 * helper.messageProvider.save(_ as AutomationCompositionDeployAck)
    }

    // --- Deploy ---

    def "given an UNDEPLOYED AC, deploy should update the AC and send a deploy message"() {
        given: "an automation composition in UNDEPLOYED state"
        def ac = helper.loadAc("Deploy")
        ac.deployState = DeployState.UNDEPLOYED
        def latch = new CountDownLatch(1)
        helper.deployPublisher.send(_, _, _, _) >> { latch.countDown() }

        when: "deploy is called"
        helper.handler.deploy(ac, helper.createDefinition())

        then: "the AC should be updated in the provider"
        1 * helper.acProvider.updateAutomationComposition(ac)

        and: "the deploy message should be sent asynchronously"
        latch.await(helper.latchTimeoutSeconds, TimeUnit.SECONDS)
    }

    def "given an AC with a NEW migration state element, deploy should throw PfModelRuntimeException"() {
        given: "an AC in DEPLOYING state with an element that has MigrationState.NEW"
        def ac = helper.loadAc("Deploy")
        ac.deployState = DeployState.DEPLOYING
        ac.elements[UUID.randomUUID()] = new AutomationCompositionElement(
                deployState: DeployState.UNDEPLOYED, migrationState: MigrationState.NEW)

        when: "deploy is called"
        helper.handler.deploy(ac, new AutomationCompositionDefinition())

        then: "a PfModelRuntimeException should be thrown"
        thrown(PfModelRuntimeException)
    }

    def "given an AC with a REMOVED migration state element, deploy should throw PfModelRuntimeException"() {
        given: "an AC in DEPLOYING state with an element that has MigrationState.REMOVED"
        def ac = helper.loadAc("Deploy")
        ac.deployState = DeployState.DEPLOYING
        ac.elements[UUID.randomUUID()] = new AutomationCompositionElement(
                deployState: DeployState.UNDEPLOYED, migrationState: MigrationState.REMOVED)

        when: "deploy is called"
        helper.handler.deploy(ac, new AutomationCompositionDefinition())

        then: "a PfModelRuntimeException should be thrown"
        thrown(PfModelRuntimeException)
    }

    // --- Undeploy ---

    def "given a DEPLOYED AC, undeploy should update the AC and send a state change message"() {
        given: "an automation composition in DEPLOYED state with NO_ERROR result"
        def ac = helper.loadAc("Undeploy")
        ac.deployState = DeployState.DEPLOYED
        ac.stateChangeResult = StateChangeResult.NO_ERROR
        ac.elements.values().each { it.deployState = DeployState.DEPLOYED }
        def latch = new CountDownLatch(1)
        helper.stateChangePublisher.send(_, _, _, _) >> { latch.countDown() }

        when: "undeploy is called"
        helper.handler.undeploy(ac, helper.createDefinition())

        then: "the AC should be updated in the provider"
        1 * helper.acProvider.updateAutomationComposition(_)

        and: "the state change message should be sent asynchronously"
        latch.await(helper.latchTimeoutSeconds, TimeUnit.SECONDS)
    }

    // --- Lock ---

    def "given an UNLOCKED AC, lock should update the AC and send a state change message"() {
        given: "an automation composition in UNLOCKED state"
        def ac = helper.loadAc("Lock")
        ac.lockState = LockState.UNLOCKED
        ac.elements.values().each { it.lockState = LockState.UNLOCKED }
        def latch = new CountDownLatch(1)
        helper.stateChangePublisher.send(_, _, _, _) >> { latch.countDown() }

        when: "lock is called"
        helper.handler.lock(ac, helper.createDefinition())

        then: "the AC should be updated in the provider"
        1 * helper.acProvider.updateAutomationComposition(_)

        and: "the state change message should be sent asynchronously"
        latch.await(helper.latchTimeoutSeconds, TimeUnit.SECONDS)
    }

    // --- Unlock ---

    def "given a LOCKED AC, unlock should update the AC and send a state change message"() {
        given: "an automation composition in LOCKED state"
        def ac = helper.loadAc("UnLock")
        ac.lockState = LockState.LOCKED
        ac.elements.values().each { it.lockState = LockState.LOCKED }
        def latch = new CountDownLatch(1)
        helper.stateChangePublisher.send(_, _, _, _) >> { latch.countDown() }

        when: "unlock is called"
        helper.handler.unlock(ac, helper.createDefinition())

        then: "the AC should be updated in the provider"
        1 * helper.acProvider.updateAutomationComposition(_)

        and: "the state change message should be sent asynchronously"
        latch.await(helper.latchTimeoutSeconds, TimeUnit.SECONDS)
    }

    // --- Update ---

    def "given an AC, update should send element properties asynchronously"() {
        given: "an automation composition"
        def ac = helper.loadAc("Lock")
        def latch = new CountDownLatch(1)
        helper.elementPublisher.send(_, _) >> { latch.countDown() }

        when: "update is called"
        helper.handler.update(ac, UUID.randomUUID())

        then: "the element properties message should be sent asynchronously"
        latch.await(helper.latchTimeoutSeconds, TimeUnit.SECONDS)
    }

    // --- Migrate ---

    def "given an AC with phase 0, migrate should send a migration message asynchronously"() {
        given: "an automation composition with phase set to 0"
        def ac = helper.loadAc("Migrate")
        ac.phase = 0
        def latch = new CountDownLatch(1)
        helper.migrationPublisher.send(_, _, _, _, _) >> { latch.countDown() }

        when: "migrate is called"
        helper.handler.migrate(ac, UUID.randomUUID(), UUID.randomUUID())

        then: "the migration message should be sent asynchronously"
        latch.await(helper.latchTimeoutSeconds, TimeUnit.SECONDS)
    }

    def "given an AC, migratePrecheck should send a migration message asynchronously"() {
        given: "an automation composition"
        def ac = helper.loadAc("Migrate")
        def latch = new CountDownLatch(1)
        helper.migrationPublisher.send(_, _, _, _, _) >> { latch.countDown() }

        when: "migratePrecheck is called"
        helper.handler.migratePrecheck(ac, UUID.randomUUID(), UUID.randomUUID())

        then: "the migration message should be sent asynchronously"
        latch.await(helper.latchTimeoutSeconds, TimeUnit.SECONDS)
    }

    // --- Prepare / Review ---

    def "given an AC, prepare should send a prepare message asynchronously"() {
        given: "an automation composition"
        def ac = helper.loadAc("Migrate")
        def latch = new CountDownLatch(1)
        helper.preparePublisher.sendPrepare(_, _, _) >> { latch.countDown() }

        when: "prepare is called"
        helper.handler.prepare(ac, helper.createDefinition())

        then: "the prepare message should be sent asynchronously"
        latch.await(helper.latchTimeoutSeconds, TimeUnit.SECONDS)
    }

    def "given an AC, review should send a review message asynchronously"() {
        given: "an automation composition"
        def ac = helper.loadAc("Migrate")
        def latch = new CountDownLatch(1)
        helper.preparePublisher.sendReview(_, _) >> { latch.countDown() }

        when: "review is called"
        helper.handler.review(ac, new AutomationCompositionDefinition())

        then: "the review message should be sent asynchronously"
        latch.await(helper.latchTimeoutSeconds, TimeUnit.SECONDS)
    }
}
