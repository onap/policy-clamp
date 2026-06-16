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
package org.onap.policy.clamp.acm.runtime.supervision.scanner

import static org.onap.policy.clamp.acm.runtime.helper.PhaseScannerTestHelper.buildAcDefinition
import static org.onap.policy.clamp.acm.runtime.helper.PhaseScannerTestHelper.buildAc
import static org.onap.policy.clamp.acm.runtime.helper.PhaseScannerTestHelper.buildAcWithNullStartPhase
import static org.onap.policy.clamp.acm.runtime.helper.PhaseScannerTestHelper.buildDeployingAc
import static org.onap.policy.clamp.acm.runtime.helper.PhaseScannerTestHelper.buildDeployingAcForStartPhase
import static org.onap.policy.clamp.acm.runtime.helper.PhaseScannerTestHelper.buildUnlockingAc

import org.onap.policy.clamp.acm.runtime.main.utils.EncryptionUtils
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionDeployPublisher
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionStateChangePublisher
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantSyncPublisher
import org.onap.policy.clamp.acm.runtime.util.CommonTestData
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition
import org.onap.policy.clamp.models.acm.concepts.DeployState
import org.onap.policy.clamp.models.acm.concepts.LockState
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider
import org.onap.policy.clamp.models.acm.utils.TimestampHelper
import spock.lang.Specification

class PhaseScannerSpec extends Specification {

    static final COMPOSITION_ID = UUID.randomUUID()
    static final INSTANCE_ID = UUID.randomUUID()

    def "scan '#desc' should complete and update AC"() {
        given:
        def ac = buildAc(COMPOSITION_ID, INSTANCE_ID, deployState, LockState.NONE)
        def acProvider = Mock(AutomationCompositionProvider) {
            updateAutomationComposition(_) >> ac
        }
        def acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner")
        def phaseScanner = new PhaseScanner(acProvider, Mock(ParticipantSyncPublisher),
                Mock(AutomationCompositionStateChangePublisher),
                Mock(AutomationCompositionDeployPublisher),
                acRuntimeParameterGroup, new EncryptionUtils(acRuntimeParameterGroup))
        def acDefinition = buildAcDefinition()

        when:
        phaseScanner.scanWithPhase(ac, acDefinition, new UpdateSync())

        then:
        updateCount * acProvider.updateAutomationComposition(_ as AutomationComposition)
        deleteCount * acProvider.deleteAutomationComposition(ac.instanceId)

        where:
        desc         | deployState            | updateCount | deleteCount
        "undeploy"   | DeployState.UNDEPLOYING | 1          | 0
        "delete"     | DeployState.DELETING    | 0          | 1
    }

    def "scan deploying AC with timeout should set TIMEOUT state"() {
        given:
        def ac = buildDeployingAc(COMPOSITION_ID, INSTANCE_ID)
        ac.stateChangeResult = StateChangeResult.NO_ERROR
        ac.lastMsg = TimestampHelper.now()
        def acProvider = Mock(AutomationCompositionProvider) {
            updateAutomationComposition(_) >> ac
        }
        def acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner")
        acRuntimeParameterGroup.participantParameters.maxOperationWaitMs = -1
        def participantSyncPublisher = Mock(ParticipantSyncPublisher)
        def phaseScanner = new PhaseScanner(acProvider, participantSyncPublisher,
                Mock(AutomationCompositionStateChangePublisher),
                Mock(AutomationCompositionDeployPublisher),
                acRuntimeParameterGroup, new EncryptionUtils(acRuntimeParameterGroup))
        def acDefinition = buildAcDefinition()

        when:
        phaseScanner.scanWithPhase(ac, acDefinition, new UpdateSync())

        then:
        1 * acProvider.updateAutomationComposition(_ as AutomationComposition)
        1 * participantSyncPublisher.sendSync(_ as AutomationComposition)
        ac.stateChangeResult == StateChangeResult.TIMEOUT
    }

    def "scan AC already in TIMEOUT should not update"() {
        given:
        def ac = buildDeployingAc(COMPOSITION_ID, INSTANCE_ID)
        ac.stateChangeResult = StateChangeResult.TIMEOUT
        ac.lastMsg = TimestampHelper.now()
        def acProvider = Mock(AutomationCompositionProvider)
        def acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner")
        acRuntimeParameterGroup.participantParameters.maxOperationWaitMs = -1
        def participantSyncPublisher = Mock(ParticipantSyncPublisher)
        def phaseScanner = new PhaseScanner(acProvider, participantSyncPublisher,
                Mock(AutomationCompositionStateChangePublisher),
                Mock(AutomationCompositionDeployPublisher),
                acRuntimeParameterGroup, new EncryptionUtils(acRuntimeParameterGroup))
        def acDefinition = buildAcDefinition()

        when:
        phaseScanner.scanWithPhase(ac, acDefinition, new UpdateSync())

        then:
        0 * acProvider.updateAutomationComposition(_ as AutomationComposition)
        0 * participantSyncPublisher.sendSync(_ as AutomationComposition)
    }

    def "scan TIMEOUT AC with all elements completed should reset to NO_ERROR"() {
        given:
        def ac = buildDeployingAc(COMPOSITION_ID, INSTANCE_ID)
        ac.stateChangeResult = StateChangeResult.TIMEOUT
        ac.lastMsg = TimestampHelper.now()
        ac.elements.values().each { it.deployState = DeployState.DEPLOYED }
        def acProvider = Mock(AutomationCompositionProvider) {
            updateAutomationComposition(_) >> ac
        }
        def acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner")
        acRuntimeParameterGroup.participantParameters.maxOperationWaitMs = -1
        def participantSyncPublisher = Mock(ParticipantSyncPublisher)
        def phaseScanner = new PhaseScanner(acProvider, participantSyncPublisher,
                Mock(AutomationCompositionStateChangePublisher),
                Mock(AutomationCompositionDeployPublisher),
                acRuntimeParameterGroup, new EncryptionUtils(acRuntimeParameterGroup))
        def acDefinition = buildAcDefinition()

        when:
        phaseScanner.scanWithPhase(ac, acDefinition, new UpdateSync())

        then:
        1 * acProvider.updateAutomationComposition(_ as AutomationComposition)
        1 * participantSyncPublisher.sendSync(_ as AutomationComposition)
        ac.stateChangeResult == StateChangeResult.NO_ERROR
    }

    def "scan deploying AC with next phase should send deploy message"() {
        given:
        def ac = buildDeployingAcForStartPhase(COMPOSITION_ID, INSTANCE_ID)
        def acProvider = Mock(AutomationCompositionProvider)
        def acDeployPublisher = Mock(AutomationCompositionDeployPublisher)
        def acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner")
        def phaseScanner = new PhaseScanner(acProvider, Mock(ParticipantSyncPublisher),
                Mock(AutomationCompositionStateChangePublisher),
                acDeployPublisher, acRuntimeParameterGroup,
                new EncryptionUtils(acRuntimeParameterGroup))
        def acDefinition = buildAcDefinition()

        when:
        phaseScanner.scanWithPhase(ac, acDefinition, new UpdateSync())

        then:
        1 * acDeployPublisher.send(_ as AutomationComposition, _ as int, _ as boolean, _ as UUID)
    }

    def "scan deploying AC with null start phase should not send message"() {
        given:
        def ac = buildAcWithNullStartPhase(COMPOSITION_ID, INSTANCE_ID)
        def acProvider = Mock(AutomationCompositionProvider)
        def acDeployPublisher = Mock(AutomationCompositionDeployPublisher)
        def acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner")
        def phaseScanner = new PhaseScanner(acProvider, Mock(ParticipantSyncPublisher),
                Mock(AutomationCompositionStateChangePublisher),
                acDeployPublisher, acRuntimeParameterGroup,
                new EncryptionUtils(acRuntimeParameterGroup))
        def acDefinition = buildAcDefinition()

        when:
        phaseScanner.scanWithPhase(ac, acDefinition, new UpdateSync())

        then:
        0 * acDeployPublisher.send(_ as AutomationComposition, _ as int, _ as boolean, _ as UUID)
    }

    def "scan unlocking AC should send state change message"() {
        given:
        def ac = buildUnlockingAc(COMPOSITION_ID, INSTANCE_ID)
        def acProvider = Mock(AutomationCompositionProvider)
        def acStateChangePublisher = Mock(AutomationCompositionStateChangePublisher)
        def acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner")
        def phaseScanner = new PhaseScanner(acProvider, Mock(ParticipantSyncPublisher),
                acStateChangePublisher, Mock(AutomationCompositionDeployPublisher),
                acRuntimeParameterGroup, new EncryptionUtils(acRuntimeParameterGroup))
        def acDefinition = buildAcDefinition()

        when:
        phaseScanner.scanWithPhase(ac, acDefinition, new UpdateSync())

        then:
        1 * acStateChangePublisher.send(_ as AutomationComposition, _ as int, _ as boolean, _ as UUID)
    }
}
