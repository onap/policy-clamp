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
package org.onap.policy.clamp.acm.runtime.helper

import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils
import org.onap.policy.clamp.acm.runtime.main.utils.EncryptionUtils
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionAcHandler
import org.onap.policy.clamp.acm.runtime.supervision.comm.*
import org.onap.policy.clamp.acm.runtime.util.CommonTestData
import org.onap.policy.clamp.models.acm.concepts.*
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeployAck
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessageType
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider
import org.onap.policy.clamp.models.acm.persistence.provider.MessageProvider

class SupervisionAcHandlerTestHelper {

    static final AC_JSON_PATH = "src/test/resources/rest/acm/AutomationComposition.json"
    static final LATCH_TIMEOUT_SECONDS = 5

    final UUID identifier = UUID.randomUUID()

    AutomationCompositionProvider acProvider
    MessageProvider messageProvider
    AutomationCompositionDeployPublisher deployPublisher
    AutomationCompositionStateChangePublisher stateChangePublisher
    AcElementPropertiesPublisher elementPublisher
    AutomationCompositionMigrationPublisher migrationPublisher
    AcPreparePublisher preparePublisher
    EncryptionUtils encryptionUtils
    SupervisionAcHandler handler

    void initMocks(AutomationCompositionProvider acProvider, MessageProvider messageProvider,
                   AutomationCompositionDeployPublisher deployPublisher,
                   AutomationCompositionStateChangePublisher stateChangePublisher,
                   AcElementPropertiesPublisher elementPublisher,
                   AutomationCompositionMigrationPublisher migrationPublisher,
                   AcPreparePublisher preparePublisher, EncryptionUtils encryptionUtils) {
        this.acProvider = acProvider
        this.messageProvider = messageProvider
        this.deployPublisher = deployPublisher
        this.stateChangePublisher = stateChangePublisher
        this.elementPublisher = elementPublisher
        this.migrationPublisher = migrationPublisher
        this.preparePublisher = preparePublisher
        this.encryptionUtils = encryptionUtils
        this.handler = new SupervisionAcHandler(acProvider, deployPublisher, stateChangePublisher,
                elementPublisher, migrationPublisher, preparePublisher, messageProvider, encryptionUtils)
    }

    static def getLatchTimeoutSeconds() {
        LATCH_TIMEOUT_SECONDS
    }

    def loadAc(String suffix) {
        def ac = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON_PATH, suffix)
        ac.instanceId = identifier
        return ac
    }

    static def createDefinition() {
        def st = InstantiationUtils.getToscaServiceTemplate(CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML)
        return CommonTestData.createAcDefinition(st, AcTypeState.PRIMED)
    }

    static def buildAck(AutomationComposition ac,
                                                   DeployState deployState = DeployState.DEPLOYED,
                                                   LockState lockState = LockState.UNLOCKED) {
        def msg = new AutomationCompositionDeployAck(ParticipantMessageType.AUTOMATION_COMPOSITION_STATECHANGE_ACK)
        ac.elements.each { key, _ ->
            msg.automationCompositionResultMap[key] = new AcElementDeployAck(deployState, lockState, "", "", [:], true, "")
        }
        msg.automationCompositionId = ac.instanceId
        msg.participantId = CommonTestData.participantId
        msg.stateChangeResult = StateChangeResult.NO_ERROR
        return msg
    }

    static def ackTestData() {
        def stateChangeAck = "handleAutomationCompositionStateChangeAckMessage"
        def updateAck = "handleAutomationCompositionUpdateAckMessage"
        def found = { Optional.of(it as Object) }
        def notFound = { Optional.empty() }
        def noOp = { }

        return [
            [scenario: "stateChangeResult is null", expectedSaves: 0,
             handlerMethod: stateChangeAck, deployState: DeployState.DEPLOYED,
             lockState: LockState.UNLOCKED, acResult: found,
             msgModifier: { it.stateChangeResult = null }],
            [scenario: "stateChangeResult is TIMEOUT", expectedSaves: 0,
             handlerMethod: stateChangeAck, deployState: DeployState.DEPLOYED,
             lockState: LockState.UNLOCKED, acResult: found,
             msgModifier: { it.stateChangeResult = StateChangeResult.TIMEOUT }],
            [scenario: "automationCompositionId is null", expectedSaves: 0,
             handlerMethod: stateChangeAck, deployState: DeployState.DEPLOYED,
             lockState: LockState.UNLOCKED, acResult: found,
             msgModifier: { it.stateChangeResult = StateChangeResult.NO_ERROR;
                            it.automationCompositionId = null }],
            [scenario: "a valid ack and an existing AC", expectedSaves: 1,
             handlerMethod: stateChangeAck, deployState: DeployState.DEPLOYED,
             lockState: LockState.UNLOCKED, acResult: found,
             msgModifier: noOp],
            [scenario: "stateChangeResult is FAILED", expectedSaves: 1,
             handlerMethod: stateChangeAck, deployState: DeployState.DEPLOYED,
             lockState: LockState.UNLOCKED, acResult: found,
             msgModifier: { it.stateChangeResult = StateChangeResult.FAILED }],
            [scenario: "AC not found in provider", expectedSaves: 0,
             handlerMethod: stateChangeAck, deployState: DeployState.DEPLOYED,
             lockState: LockState.UNLOCKED, acResult: notFound,
             msgModifier: noOp],
            [scenario: "null resultMap via deploy ack", expectedSaves: 1,
             handlerMethod: updateAck, deployState: DeployState.DEPLOYED,
             lockState: LockState.UNLOCKED, acResult: found,
             msgModifier: { it.automationCompositionResultMap = null }],
            [scenario: "empty resultMap via deploy ack", expectedSaves: 1,
             handlerMethod: updateAck, deployState: DeployState.DEPLOYED,
             lockState: LockState.UNLOCKED, acResult: found,
             msgModifier: { it.automationCompositionResultMap = [:] }],
            [scenario: "transitional states with no stage", expectedSaves: 0,
             handlerMethod: stateChangeAck, deployState: DeployState.DEPLOYING,
             lockState: LockState.NONE, acResult: found,
             msgModifier: { it.stage = null }],
        ]
    }
}
