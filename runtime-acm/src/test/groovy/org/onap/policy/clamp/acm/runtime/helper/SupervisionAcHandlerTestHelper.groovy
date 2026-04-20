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
import org.yaml.snakeyaml.Yaml

class SupervisionAcHandlerTestHelper {

    private static final String CONFIG_PATH = "supervision/supervision-ac-handler-test-config.yaml"

    private final Map config
    private final UUID identifier = UUID.randomUUID()

    AutomationCompositionProvider acProvider
    MessageProvider messageProvider
    AutomationCompositionDeployPublisher deployPublisher
    AutomationCompositionStateChangePublisher stateChangePublisher
    AcElementPropertiesPublisher elementPublisher
    AutomationCompositionMigrationPublisher migrationPublisher
    AcPreparePublisher preparePublisher
    EncryptionUtils encryptionUtils
    SupervisionAcHandler handler

    SupervisionAcHandlerTestHelper() {
        this.config = new Yaml().load(getClass().classLoader.getResourceAsStream(CONFIG_PATH))
    }

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

    String getResourcePath(String key) {
        config.resourcePaths[key]
    }

    int getLatchTimeoutSeconds() {
        config.latchTimeoutSeconds as int
    }

    AutomationComposition loadAc(String suffix) {
        def ac = InstantiationUtils.getAutomationCompositionFromResource(getResourcePath("acJson"), suffix)
        ac.instanceId = identifier
        ac
    }

    static AutomationCompositionDefinition createDefinition() {
        def st = InstantiationUtils.getToscaServiceTemplate(CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML)
        CommonTestData.createAcDefinition(st, AcTypeState.PRIMED)
    }

    static AutomationCompositionDeployAck buildAck(AutomationComposition ac,
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
}
