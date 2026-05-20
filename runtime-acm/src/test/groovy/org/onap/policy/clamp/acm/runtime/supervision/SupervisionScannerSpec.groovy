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

import static org.onap.policy.clamp.acm.runtime.helper.SupervisionScannerTestHelper.buildAcDefinition
import static org.onap.policy.clamp.acm.runtime.helper.SupervisionScannerTestHelper.buildMigratingAcFromResource
import static org.onap.policy.clamp.acm.runtime.helper.SupervisionScannerTestHelper.buildMigratingAcWithMigrationStates

import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils
import org.onap.policy.clamp.acm.runtime.main.utils.EncryptionUtils
import org.onap.policy.clamp.acm.runtime.supervision.comm.AcPreparePublisher
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionMigrationPublisher
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantSyncPublisher
import org.onap.policy.clamp.acm.runtime.supervision.scanner.AcDefinitionScanner
import org.onap.policy.clamp.acm.runtime.supervision.scanner.MonitoringScanner
import org.onap.policy.clamp.acm.runtime.supervision.scanner.PhaseScanner
import org.onap.policy.clamp.acm.runtime.supervision.scanner.SimpleScanner
import org.onap.policy.clamp.acm.runtime.supervision.scanner.StageScanner
import org.onap.policy.clamp.acm.runtime.supervision.scanner.UpdateSync
import org.onap.policy.clamp.acm.runtime.util.CommonTestData
import org.onap.policy.clamp.models.acm.concepts.AcTypeState
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition
import org.onap.policy.clamp.models.acm.concepts.DeployState
import org.onap.policy.clamp.models.acm.concepts.LockState
import org.onap.policy.clamp.models.acm.concepts.MigrationState
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult
import org.onap.policy.clamp.models.acm.concepts.SubState
import org.onap.policy.clamp.models.acm.document.concepts.DocMessage
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider
import org.onap.policy.clamp.models.acm.persistence.provider.MessageProvider
import org.onap.policy.clamp.models.acm.utils.TimestampHelper
import org.springframework.dao.DataIntegrityViolationException
import spock.lang.Specification

class SupervisionScannerSpec extends Specification {

    static final COMPOSITION_ID = UUID.randomUUID()
    static final INSTANCE_ID = UUID.randomUUID()
    static final JOB_ID = "JOB_ID"
    static final AC_SMOKE_JSON = "src/test/resources/rest/acm/AutomationCompositionSmoke.json"

    def "scan AcDefinition should process messages and remove job"() {
        given:
        def acDefinitionProvider = buildAcDefinitionProvider(AcTypeState.PRIMING)
        def acDefinitionScanner = Mock(AcDefinitionScanner)
        acDefinitionScanner.scanMessage(_, _) >> new UpdateSync()
        def message = new DocMessage()
        def messageProvider = Mock(MessageProvider) {
            findCompositionMessages() >> new HashSet([COMPOSITION_ID])
            findInstanceMessages() >> new HashSet()
            createJob(COMPOSITION_ID) >> Optional.of(JOB_ID)
            getAllMessages(COMPOSITION_ID) >> [message]
        }
        def acProvider = mockAcProvider()
        def monitoringScanner = new MonitoringScanner(acProvider, acDefinitionProvider,
                acDefinitionScanner, Mock(StageScanner), Mock(SimpleScanner),
                Mock(PhaseScanner), messageProvider)
        def scanner = new SupervisionScanner(acProvider, acDefinitionProvider,
                messageProvider, monitoringScanner)

        when:
        scanner.run()

        then:
        1 * acDefinitionScanner.scanAutomationCompositionDefinition(_, _)
        1 * messageProvider.removeMessage(_)
        1 * messageProvider.removeJob(JOB_ID)
    }

    def "scan AcDefinition when job already exists should skip scan"() {
        given:
        def acDefinitionProvider = buildAcDefinitionProvider(AcTypeState.PRIMING)
        def acDefinitionScanner = Mock(AcDefinitionScanner)
        def messageProvider = Mock(MessageProvider) {
            findCompositionMessages() >> new HashSet()
            findInstanceMessages() >> new HashSet()
            createJob(COMPOSITION_ID) >> Optional.empty()
        }
        def acProvider = mockAcProvider()
        def monitoringScanner = new MonitoringScanner(acProvider, acDefinitionProvider,
                acDefinitionScanner, Mock(StageScanner), Mock(SimpleScanner),
                Mock(PhaseScanner), messageProvider)
        def scanner = new SupervisionScanner(acProvider, acDefinitionProvider,
                messageProvider, monitoringScanner)

        when:
        scanner.run()

        then:
        0 * acDefinitionScanner.scanAutomationCompositionDefinition(_, _)
    }

    def "scan AC not in transition or failed should not invoke scanners"() {
        given:
        def ac = InstantiationUtils.getAutomationCompositionFromResource(
                AC_SMOKE_JSON, "Crud")
        ac.instanceId = INSTANCE_ID
        ac.compositionId = COMPOSITION_ID
        def acProvider = mockAcProvider(new HashSet([ac.instanceId]))
        acProvider.findAutomationComposition(ac.instanceId) >> Optional.of(ac)
        def stageScanner = Mock(StageScanner)
        def simpleScanner = Mock(SimpleScanner)
        def phaseScanner = Mock(PhaseScanner)
        def messageProvider = Mock(MessageProvider) {
            findCompositionMessages() >> new HashSet()
            findInstanceMessages() >> new HashSet()
            createJob(ac.instanceId) >> Optional.of(JOB_ID)
            getAllMessages(_) >> []
        }
        def acDefinitionProvider = buildAcDefinitionProvider(AcTypeState.PRIMED)
        def monitoringScanner = new MonitoringScanner(acProvider, acDefinitionProvider,
                Mock(AcDefinitionScanner), stageScanner, simpleScanner,
                phaseScanner, messageProvider)
        def scanner = new SupervisionScanner(acProvider, acDefinitionProvider,
                messageProvider, monitoringScanner)

        when:
        scanner.run()

        then:
        0 * stageScanner.scanStage(_, _, _, _)
        0 * simpleScanner.simpleScan(_, _)
        0 * phaseScanner.scanWithPhase(_, _, _)

        where:
        desc               | acDeployState          | acStateChangeResult
        "not in transition"| DeployState.DEPLOYED   | StateChangeResult.NO_ERROR
        "failed"           | DeployState.DEPLOYING  | StateChangeResult.FAILED
    }

    def "scan AC removed from DB should remove job"() {
        given:
        def acProvider = mockAcProvider(new HashSet([INSTANCE_ID]))
        acProvider.findAutomationComposition(INSTANCE_ID) >> Optional.empty()
        def stageScanner = Mock(StageScanner)
        def simpleScanner = Mock(SimpleScanner)
        def phaseScanner = Mock(PhaseScanner)
        def messageProvider = Mock(MessageProvider) {
            findCompositionMessages() >> new HashSet()
            findInstanceMessages() >> new HashSet()
            createJob(INSTANCE_ID) >> Optional.of(JOB_ID)
            getAllMessages(_) >> []
        }
        def acDefinitionProvider = buildAcDefinitionProvider(AcTypeState.PRIMED)
        def monitoringScanner = new MonitoringScanner(acProvider, acDefinitionProvider,
                Mock(AcDefinitionScanner), stageScanner, simpleScanner,
                phaseScanner, messageProvider)
        def scanner = new SupervisionScanner(acProvider, acDefinitionProvider,
                messageProvider, monitoringScanner)

        when:
        scanner.run()

        then:
        0 * stageScanner.scanStage(_, _, _, _)
        0 * simpleScanner.simpleScan(_, _)
        0 * phaseScanner.scanWithPhase(_, _, _)
        1 * messageProvider.removeJob(JOB_ID)
    }

    def "scan job failure due to DataIntegrityViolation should skip processing"() {
        given:
        def ac = new AutomationComposition(
                instanceId: INSTANCE_ID,
                compositionId: COMPOSITION_ID,
                deployState: DeployState.DEPLOYING)
        def acProvider = mockAcProvider(new HashSet([ac.instanceId]))
        acProvider.findAutomationComposition(ac.instanceId) >> Optional.of(ac)
        def stageScanner = Mock(StageScanner)
        def simpleScanner = Mock(SimpleScanner)
        simpleScanner.scanMessage(_, _) >> new UpdateSync()
        def phaseScanner = Mock(PhaseScanner)
        def message = new DocMessage()
        def messageProvider = Mock(MessageProvider) {
            findCompositionMessages() >> new HashSet()
            findInstanceMessages() >> new HashSet([INSTANCE_ID])
            createJob(ac.instanceId) >> { throw new DataIntegrityViolationException("", null) }
            getAllMessages(INSTANCE_ID) >> [message]
        }
        def acDefinitionProvider = buildAcDefinitionProvider(AcTypeState.PRIMED)
        def monitoringScanner = new MonitoringScanner(acProvider, acDefinitionProvider,
                Mock(AcDefinitionScanner), stageScanner, simpleScanner,
                phaseScanner, messageProvider)
        def scanner = new SupervisionScanner(acProvider, acDefinitionProvider,
                messageProvider, monitoringScanner)

        when:
        scanner.run()

        then:
        0 * stageScanner.scanStage(_, _, _, _)
        0 * simpleScanner.simpleScan(_, _)
        0 * phaseScanner.scanWithPhase(_, _, _)
        0 * messageProvider.removeMessage(_)
        0 * messageProvider.removeJob(_)
    }

    def "scan DEPLOYING AC should invoke phaseScanner"() {
        given:
        def ac = new AutomationComposition(
                instanceId: INSTANCE_ID,
                compositionId: COMPOSITION_ID,
                deployState: DeployState.DEPLOYING)
        def acProvider = mockAcProvider(new HashSet([ac.instanceId]))
        acProvider.findAutomationComposition(ac.instanceId) >> Optional.of(ac)
        def stageScanner = Mock(StageScanner)
        def simpleScanner = Mock(SimpleScanner)
        simpleScanner.scanMessage(_, _) >> new UpdateSync()
        def phaseScanner = Mock(PhaseScanner)
        def message = new DocMessage()
        def messageProvider = Mock(MessageProvider) {
            findCompositionMessages() >> new HashSet()
            findInstanceMessages() >> new HashSet([INSTANCE_ID])
            createJob(ac.instanceId) >> Optional.of(JOB_ID)
            getAllMessages(INSTANCE_ID) >> [message]
        }
        def acDefinitionProvider = buildAcDefinitionProvider(AcTypeState.PRIMED)
        def monitoringScanner = new MonitoringScanner(acProvider, acDefinitionProvider,
                Mock(AcDefinitionScanner), stageScanner, simpleScanner,
                phaseScanner, messageProvider)
        def scanner = new SupervisionScanner(acProvider, acDefinitionProvider,
                messageProvider, monitoringScanner)

        when:
        scanner.run()

        then:
        0 * stageScanner.scanStage(_, _, _, _)
        0 * simpleScanner.simpleScan(_, _)
        1 * phaseScanner.scanWithPhase(_, _, _)
        1 * messageProvider.removeMessage(_)
        1 * messageProvider.removeJob(JOB_ID)
    }

    def "scan MIGRATING AC should invoke stageScanner"() {
        given:
        def compositionTargetId = UUID.randomUUID()
        def ac = buildMigratingAcFromResource(
                COMPOSITION_ID, INSTANCE_ID, compositionTargetId)
        def acProvider = mockAcProvider(new HashSet([ac.instanceId]))
        acProvider.findAutomationComposition(ac.instanceId) >> Optional.of(ac)
        def acDefinitionTarget = buildAcDefinition(compositionTargetId, AcTypeState.PRIMED)
        def acDefinitionProvider = buildAcDefinitionProvider(AcTypeState.PRIMED)
        acDefinitionProvider.getAcDefinition(compositionTargetId) >> acDefinitionTarget
        acDefinitionProvider.getAcDefinition(COMPOSITION_ID) >>
                new AutomationCompositionDefinition(compositionId: COMPOSITION_ID)
        def stageScanner = Mock(StageScanner)
        def messageProvider = Mock(MessageProvider) {
            findCompositionMessages() >> new HashSet()
            findInstanceMessages() >> new HashSet()
            createJob(ac.instanceId) >> Optional.of(JOB_ID)
            getAllMessages(_) >> []
        }
        def monitoringScanner = new MonitoringScanner(acProvider, acDefinitionProvider,
                Mock(AcDefinitionScanner), stageScanner, Mock(SimpleScanner),
                Mock(PhaseScanner), messageProvider)
        def scanner = new SupervisionScanner(acProvider, acDefinitionProvider,
                messageProvider, monitoringScanner)

        when:
        scanner.run()

        then:
        1 * stageScanner.scanStage(ac, _, _, _)
        1 * messageProvider.removeJob(JOB_ID)
    }

    def "scan MIGRATION_REVERTING AC should invoke stageScanner"() {
        given:
        def compositionTargetId = UUID.randomUUID()
        def ac = buildMigratingAcFromResource(
                COMPOSITION_ID, INSTANCE_ID, compositionTargetId)
        ac.deployState = DeployState.MIGRATION_REVERTING
        def acProvider = mockAcProvider(new HashSet([ac.instanceId]))
        acProvider.findAutomationComposition(ac.instanceId) >> Optional.of(ac)
        def acDefinitionTarget = buildAcDefinition(compositionTargetId, AcTypeState.PRIMED)
        def acDefinitionProvider = buildAcDefinitionProvider(AcTypeState.PRIMED)
        acDefinitionProvider.getAcDefinition(compositionTargetId) >> acDefinitionTarget
        acDefinitionProvider.getAcDefinition(COMPOSITION_ID) >>
                new AutomationCompositionDefinition(compositionId: COMPOSITION_ID)
        def stageScanner = Mock(StageScanner)
        def messageProvider = Mock(MessageProvider) {
            findCompositionMessages() >> new HashSet()
            findInstanceMessages() >> new HashSet()
            createJob(ac.instanceId) >> Optional.of(JOB_ID)
            getAllMessages(_) >> []
        }
        def monitoringScanner = new MonitoringScanner(acProvider, acDefinitionProvider,
                Mock(AcDefinitionScanner), stageScanner, Mock(SimpleScanner),
                Mock(PhaseScanner), messageProvider)
        def scanner = new SupervisionScanner(acProvider, acDefinitionProvider,
                messageProvider, monitoringScanner)

        when:
        scanner.run()

        then:
        1 * stageScanner.scanStage(ac, _, _, _)
        1 * messageProvider.removeJob(JOB_ID)
    }

    def "scan migration success should remove REMOVED elements and reset state"() {
        given:
        def compositionTargetId = UUID.randomUUID()
        def ac = buildMigratingAcWithMigrationStates(
                COMPOSITION_ID, INSTANCE_ID, compositionTargetId)
        def acProvider = mockAcProvider(new HashSet([ac.instanceId]))
        acProvider.findAutomationComposition(ac.instanceId) >> Optional.of(ac)
        acProvider.getAutomationComposition(_) >> ac
        def acDefinitionTarget = buildAcDefinition(compositionTargetId, AcTypeState.PRIMED)
        def acDefinitionProvider = buildAcDefinitionProvider(AcTypeState.PRIMED)
        acDefinitionProvider.getAcDefinition(compositionTargetId) >> acDefinitionTarget
        acDefinitionProvider.getAcDefinition(COMPOSITION_ID) >>
                new AutomationCompositionDefinition(compositionId: COMPOSITION_ID)
        def acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner")
        def stageScanner = new StageScanner(acProvider,
                Mock(ParticipantSyncPublisher),
                Mock(AutomationCompositionMigrationPublisher),
                Mock(AcPreparePublisher), acRuntimeParameterGroup,
                Mock(EncryptionUtils))
        def messageProvider = Mock(MessageProvider) {
            findCompositionMessages() >> new HashSet()
            findInstanceMessages() >> new HashSet()
            createJob(ac.instanceId) >> Optional.of(JOB_ID)
            getAllMessages(_) >> []
        }
        def monitoringScanner = new MonitoringScanner(acProvider, acDefinitionProvider,
                Mock(AcDefinitionScanner), stageScanner, Mock(SimpleScanner),
                Mock(PhaseScanner), messageProvider)
        def scanner = new SupervisionScanner(acProvider, acDefinitionProvider,
                messageProvider, monitoringScanner)

        when:
        scanner.run()

        then:
        ac.elements.size() == 2
        ac.deployState == DeployState.DEPLOYED
        ac.elements.values().every { it.migrationState == MigrationState.DEFAULT }
        1 * messageProvider.removeJob(JOB_ID)
    }

    def "scan '#desc' should invoke simpleScanner"() {
        given:
        def ac = InstantiationUtils.getAutomationCompositionFromResource(
                AC_SMOKE_JSON, "Crud")
        ac.lockState = LockState.LOCKED
        ac.deployState = deployState
        ac.subState = subState
        ac.lockState = LockState.NONE
        ac.instanceId = INSTANCE_ID
        ac.compositionId = COMPOSITION_ID
        ac.lastMsg = TimestampHelper.now()
        def acProvider = mockAcProvider(new HashSet([ac.instanceId]))
        acProvider.findAutomationComposition(ac.instanceId) >> Optional.of(ac)
        def simpleScanner = Mock(SimpleScanner)
        def messageProvider = Mock(MessageProvider) {
            findCompositionMessages() >> new HashSet()
            findInstanceMessages() >> new HashSet()
            createJob(ac.instanceId) >> Optional.of(JOB_ID)
            getAllMessages(_) >> []
        }
        def acDefinitionProvider = buildAcDefinitionProvider(AcTypeState.PRIMED)
        def monitoringScanner = new MonitoringScanner(acProvider, acDefinitionProvider,
                Mock(AcDefinitionScanner), Mock(StageScanner), simpleScanner,
                Mock(PhaseScanner), messageProvider)
        def scanner = new SupervisionScanner(acProvider, acDefinitionProvider,
                messageProvider, monitoringScanner)

        when:
        scanner.run()

        then:
        1 * simpleScanner.simpleScan(ac, _)
        1 * messageProvider.removeJob(JOB_ID)

        where:
        desc                    | deployState            | subState
        "MIGRATION_PRECHECKING" | DeployState.DEPLOYED   | SubState.MIGRATION_PRECHECKING
        "REVIEWING"             | DeployState.DEPLOYED   | SubState.REVIEWING
        "UPDATING"              | DeployState.UPDATING   | SubState.NONE
    }

    def "scan message update should save and sync"() {
        given:
        def ac = new AutomationComposition(
                instanceId: INSTANCE_ID,
                compositionId: COMPOSITION_ID,
                deployState: DeployState.DEPLOYED,
                lockState: LockState.LOCKED,
                stateChangeResult: StateChangeResult.NO_ERROR)
        def acProvider = mockAcProvider()
        acProvider.findAutomationComposition(ac.instanceId) >> Optional.of(ac)
        def simpleScanner = Mock(SimpleScanner)
        def updateSync = new UpdateSync()
        updateSync.updated = true
        simpleScanner.scanMessage(_, _) >> updateSync
        def message = new DocMessage()
        def messageProvider = Mock(MessageProvider) {
            findCompositionMessages() >> new HashSet()
            findInstanceMessages() >> new HashSet([INSTANCE_ID])
            createJob(ac.instanceId) >> Optional.of(JOB_ID)
            getAllMessages(INSTANCE_ID) >> [message]
        }
        def phaseScanner = Mock(PhaseScanner)
        def stageScanner = Mock(StageScanner)
        def acDefinitionProvider = buildAcDefinitionProvider(AcTypeState.PRIMED)
        def monitoringScanner = new MonitoringScanner(acProvider, acDefinitionProvider,
                Mock(AcDefinitionScanner), stageScanner, simpleScanner,
                phaseScanner, messageProvider)
        def scanner = new SupervisionScanner(acProvider, acDefinitionProvider,
                messageProvider, monitoringScanner)

        when:
        scanner.run()

        then:
        0 * stageScanner.scanStage(_, _, _, _)
        0 * simpleScanner.simpleScan(_, _)
        0 * phaseScanner.scanWithPhase(_, _, _)
        1 * simpleScanner.saveAndSync(_, _)
        1 * messageProvider.removeMessage(_)
        1 * messageProvider.removeJob(JOB_ID)
    }

    // ---- Helpers ----

    def buildAcDefinitionProvider(AcTypeState acTypeState) {
        def acDefinition = buildAcDefinition(COMPOSITION_ID, acTypeState)
        def provider = Mock(AcDefinitionProvider)
        if (acTypeState in [AcTypeState.PRIMING, AcTypeState.DEPRIMING]) {
            provider.getAllAcDefinitionsInTransition() >>
                    new HashSet([acDefinition.compositionId])
            provider.getAcDefinition(acDefinition.compositionId) >> acDefinition
            provider.findAcDefinition(acDefinition.compositionId) >>
                    Optional.of(acDefinition)
        } else {
            provider.getAllAcDefinitionsInTransition() >> new HashSet()
        }
        provider.getAcDefinition(COMPOSITION_ID) >> acDefinition
        return provider
    }

    def mockAcProvider(Set<UUID> instancesInTransition = new HashSet()) {
        def provider = Mock(AutomationCompositionProvider)
        provider.getAcInstancesInTransition() >> instancesInTransition
        return provider
    }
}
