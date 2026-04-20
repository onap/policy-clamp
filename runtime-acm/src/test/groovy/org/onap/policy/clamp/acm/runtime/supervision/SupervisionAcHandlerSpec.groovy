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

import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils
import org.onap.policy.clamp.acm.runtime.main.utils.EncryptionUtils
import org.onap.policy.clamp.acm.runtime.supervision.comm.*
import org.onap.policy.clamp.acm.runtime.util.CommonTestData
import org.onap.policy.clamp.models.acm.concepts.*
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeployAck
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessageType
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider
import org.onap.policy.clamp.models.acm.persistence.provider.MessageProvider
import org.onap.policy.models.base.PfModelRuntimeException
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class SupervisionAcHandlerSpec extends Specification {

    static final AC_JSON = "src/test/resources/rest/acm/AutomationComposition.json"
    static final IDENTIFIER = UUID.randomUUID()

    AutomationCompositionProvider acProvider
    MessageProvider messageProvider
    AutomationCompositionDeployPublisher deployPublisher
    AutomationCompositionStateChangePublisher stateChangePublisher
    AcElementPropertiesPublisher elementPublisher
    AutomationCompositionMigrationPublisher migrationPublisher
    AcPreparePublisher preparePublisher
    EncryptionUtils encryptionUtils
    SupervisionAcHandler handler

    def setup() {
        acProvider = Mock()
        messageProvider = Mock()
        deployPublisher = Mock()
        stateChangePublisher = Mock()
        elementPublisher = Mock()
        migrationPublisher = Mock()
        preparePublisher = Mock()
        encryptionUtils = Mock()
        handler = new SupervisionAcHandler(acProvider, deployPublisher, stateChangePublisher,
                elementPublisher, migrationPublisher, preparePublisher, messageProvider, encryptionUtils)
    }

    private AutomationComposition loadAc(String suffix) {
        def ac = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, suffix)
        ac.instanceId = IDENTIFIER
        ac
    }

    private AutomationCompositionDefinition createDefinition() {
        def st = InstantiationUtils.getToscaServiceTemplate(CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML)
        CommonTestData.createAcDefinition(st, AcTypeState.PRIMED)
    }

    private AutomationCompositionDeployAck buildAck(AutomationComposition ac,
                                                     DeployState deployState = DeployState.DEPLOYED,
                                                     LockState lockState = LockState.UNLOCKED) {
        def msg = new AutomationCompositionDeployAck(ParticipantMessageType.AUTOMATION_COMPOSITION_STATECHANGE_ACK)
        ac.elements.each { key, _ ->
            msg.automationCompositionResultMap[key] = new AcElementDeployAck(deployState, lockState, "", "", [:], true, "")
        }
        msg.automationCompositionId = ac.instanceId
        msg.participantId = CommonTestData.participantId
        msg.stateChangeResult = StateChangeResult.NO_ERROR
        msg
    }

    // --- Ack validation ---

    def "does not save ack when validation fails"() {
        given:
        def ac = loadAc("Crud")
        def msg = buildAck(ac)

        when: "stateChangeResult is null"
        msg.stateChangeResult = null
        handler.handleAutomationCompositionStateChangeAckMessage(msg)

        then:
        0 * messageProvider.save(_)

        when: "stateChangeResult is TIMEOUT"
        msg.stateChangeResult = StateChangeResult.TIMEOUT
        handler.handleAutomationCompositionStateChangeAckMessage(msg)

        then:
        0 * messageProvider.save(_)

        when: "automationCompositionId is null"
        msg.stateChangeResult = StateChangeResult.NO_ERROR
        msg.automationCompositionId = null
        handler.handleAutomationCompositionStateChangeAckMessage(msg)

        then:
        0 * messageProvider.save(_)
    }

    def "saves ack when AC exists"() {
        given:
        def ac = loadAc("Crud")
        acProvider.findAutomationComposition(IDENTIFIER) >> Optional.of(ac)

        when:
        handler.handleAutomationCompositionStateChangeAckMessage(buildAck(ac))

        then:
        1 * messageProvider.save(_ as AutomationCompositionDeployAck)
    }

    // --- Deploy ---

    def "deploy updates AC and sends message"() {
        given:
        def ac = loadAc("Deploy")
        ac.deployState = DeployState.UNDEPLOYED
        def latch = new CountDownLatch(1)
        deployPublisher.send(_, _, _, _) >> { latch.countDown() }

        when:
        handler.deploy(ac, createDefinition())

        then:
        1 * acProvider.updateAutomationComposition(ac)
        latch.await(2, TimeUnit.SECONDS)
    }

    def "deploy throws for invalid NEW element"() {
        given:
        def ac = loadAc("Deploy")
        ac.deployState = DeployState.DEPLOYING
        ac.elements[UUID.randomUUID()] = new AutomationCompositionElement(
                deployState: DeployState.UNDEPLOYED, migrationState: MigrationState.NEW)

        when:
        handler.deploy(ac, new AutomationCompositionDefinition())

        then:
        thrown(PfModelRuntimeException)
    }

    def "deploy throws for invalid REMOVED element"() {
        given:
        def ac = loadAc("Deploy")
        ac.deployState = DeployState.DEPLOYING
        ac.elements[UUID.randomUUID()] = new AutomationCompositionElement(
                deployState: DeployState.UNDEPLOYED, migrationState: MigrationState.REMOVED)

        when:
        handler.deploy(ac, new AutomationCompositionDefinition())

        then:
        thrown(PfModelRuntimeException)
    }

    // --- Undeploy ---

    def "undeploy updates AC and sends state change"() {
        given:
        def ac = loadAc("Undeploy")
        ac.deployState = DeployState.DEPLOYED
        ac.stateChangeResult = StateChangeResult.NO_ERROR
        ac.elements.values().each { it.deployState = DeployState.DEPLOYED }
        def latch = new CountDownLatch(1)
        stateChangePublisher.send(_, _, _, _) >> { latch.countDown() }

        when:
        handler.undeploy(ac, createDefinition())

        then:
        1 * acProvider.updateAutomationComposition(_)
        latch.await(2, TimeUnit.SECONDS)
    }

    // --- Lock ---

    def "lock updates AC and sends state change"() {
        given:
        def ac = loadAc("Lock")
        ac.lockState = LockState.UNLOCKED
        ac.elements.values().each { it.lockState = LockState.UNLOCKED }
        def latch = new CountDownLatch(1)
        stateChangePublisher.send(_, _, _, _) >> { latch.countDown() }

        when:
        handler.lock(ac, createDefinition())

        then:
        1 * acProvider.updateAutomationComposition(_)
        latch.await(2, TimeUnit.SECONDS)
    }

    // --- Unlock ---

    def "unlock updates AC and sends state change"() {
        given:
        def ac = loadAc("UnLock")
        ac.lockState = LockState.LOCKED
        ac.elements.values().each { it.lockState = LockState.LOCKED }
        def latch = new CountDownLatch(1)
        stateChangePublisher.send(_, _, _, _) >> { latch.countDown() }

        when:
        handler.unlock(ac, createDefinition())

        then:
        1 * acProvider.updateAutomationComposition(_)
        latch.await(2, TimeUnit.SECONDS)
    }

    // --- Update ---

    def "update sends element properties"() {
        given:
        def ac = loadAc("Lock")
        def latch = new CountDownLatch(1)
        elementPublisher.send(_, _) >> { latch.countDown() }

        when:
        handler.update(ac, UUID.randomUUID())

        then:
        latch.await(2, TimeUnit.SECONDS)
    }

    // --- Migrate ---

    def "migrate sends migration message"() {
        given:
        def ac = loadAc("Migrate")
        ac.phase = 0
        def latch = new CountDownLatch(1)
        migrationPublisher.send(_, _, _, _, _) >> { latch.countDown() }

        when:
        handler.migrate(ac, UUID.randomUUID(), UUID.randomUUID())

        then:
        latch.await(2, TimeUnit.SECONDS)
    }

    def "migratePrecheck sends migration message"() {
        given:
        def ac = loadAc("Migrate")
        def latch = new CountDownLatch(1)
        migrationPublisher.send(_, _, _, _, _) >> { latch.countDown() }

        when:
        handler.migratePrecheck(ac, UUID.randomUUID(), UUID.randomUUID())

        then:
        latch.await(2, TimeUnit.SECONDS)
    }

    // --- Prepare / Review ---

    def "prepare sends prepare message"() {
        given:
        def ac = loadAc("Migrate")
        def latch = new CountDownLatch(1)
        preparePublisher.sendPrepare(_, _, _) >> { latch.countDown() }

        when:
        handler.prepare(ac, createDefinition())

        then:
        latch.await(2, TimeUnit.SECONDS)
    }

    def "review sends review message"() {
        given:
        def ac = loadAc("Migrate")
        def latch = new CountDownLatch(1)
        preparePublisher.sendReview(_, _) >> { latch.countDown() }

        when:
        handler.review(ac, new AutomationCompositionDefinition())

        then:
        latch.await(2, TimeUnit.SECONDS)
    }
}
