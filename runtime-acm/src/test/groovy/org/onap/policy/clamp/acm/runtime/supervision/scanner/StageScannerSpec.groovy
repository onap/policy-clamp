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

import static org.onap.policy.clamp.acm.runtime.helper.StageScannerTestHelper.buildAcDefinition
import static org.onap.policy.clamp.acm.runtime.helper.StageScannerTestHelper.buildMigratingAc
import static org.onap.policy.clamp.acm.runtime.helper.StageScannerTestHelper.buildMigrationRevertingAc
import static org.onap.policy.clamp.acm.runtime.helper.StageScannerTestHelper.buildPreparingAc

import org.onap.policy.clamp.acm.runtime.main.utils.EncryptionUtils
import org.onap.policy.clamp.acm.runtime.supervision.comm.AcPreparePublisher
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionMigrationPublisher
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantSyncPublisher
import org.onap.policy.clamp.acm.runtime.util.CommonTestData
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition
import org.onap.policy.clamp.models.acm.concepts.DeployState
import org.onap.policy.clamp.models.acm.concepts.MigrationState
import org.onap.policy.clamp.models.acm.concepts.SubState
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider
import spock.lang.Specification

class StageScannerSpec extends Specification {

    static final COMPOSITION_ID = UUID.randomUUID()

    def "scan #scenario - first element not yet transitioned should not update"() {
        given:
        def acProvider = Mock(AutomationCompositionProvider)
        def scanner = buildStageScanner(acProvider)
        def compositionTargetId = UUID.randomUUID()
        def ac = acBuilder(COMPOSITION_ID, compositionTargetId)
        def acDefinition = buildAcDefinition()

        when:
        scanner.scanStage(ac, acDefinition, new UpdateSync(), UUID.randomUUID())

        then:
        0 * acProvider.updateAutomationComposition(_)
        ac.deployState == expectedState

        where:
        scenario              | acBuilder                        | expectedState
        "migrating AC"        | this.&buildMigratingAcWrapper   | DeployState.MIGRATING
        "migration reverting" | this.&buildRevertingAcWrapper    | DeployState.MIGRATION_REVERTING
    }

    def "scan #scenario - send message for next stage"() {
        given:
        def acProvider = Mock(AutomationCompositionProvider)
        def scanner = buildStageScanner(acProvider)
        def compositionTargetId = UUID.randomUUID()
        def ac = acBuilder(COMPOSITION_ID, compositionTargetId)
        def acDefinition = buildAcDefinition()
        def element = ac.elements.values().iterator().next()
        def toscaNodeTemplate = acDefinition.serviceTemplate.toscaTopologyTemplate
                .nodeTemplates[element.definition.name]
        toscaNodeTemplate.properties = [stage: [1]]
        acProvider.updateAutomationComposition(_) >> ac

        when:
        scanner.scanStage(ac, acDefinition, new UpdateSync(), UUID.randomUUID())

        then:
        1 * acProvider.updateAutomationComposition(_ as AutomationComposition)
        ac.deployState == expectedState

        where:
        scenario              | acBuilder                        | expectedState
        "migrating AC"        | this.&buildMigratingAcWrapper   | DeployState.MIGRATING
        "migration reverting" | this.&buildRevertingAcWrapper    | DeployState.MIGRATION_REVERTING
    }

    def "scan migrating AC - first element migrated should complete"() {
        given:
        def acProvider = Mock(AutomationCompositionProvider)
        def scanner = buildStageScanner(acProvider)
        def compositionTargetId = UUID.randomUUID()
        def ac = buildMigratingAc(COMPOSITION_ID, compositionTargetId)
        def acDefinition = buildAcDefinition()
        def element = ac.elements.values().iterator().next()
        element.deployState = DeployState.DEPLOYED
        acProvider.updateAutomationComposition(_) >> ac

        when:
        scanner.scanStage(ac, acDefinition, new UpdateSync(), UUID.randomUUID())

        then:
        1 * acProvider.updateAutomationComposition(_ as AutomationComposition)
        ac.deployState == DeployState.DEPLOYED
        ac.compositionId == compositionTargetId
    }

    def "scan migrating AC - element deleted should remove and send delete sync"() {
        given:
        def participantSyncPublisher = Mock(ParticipantSyncPublisher)
        def acProvider = Mock(AutomationCompositionProvider)
        def scanner = buildStageScanner(acProvider, participantSyncPublisher)
        def compositionTargetId = UUID.randomUUID()
        def ac = buildMigratingAc(COMPOSITION_ID, compositionTargetId)
        def acDefinition = buildAcDefinition()
        def element = ac.elements.values().iterator().next()
        element.deployState = DeployState.DELETED
        acProvider.updateAutomationComposition(_) >> ac

        when:
        scanner.scanStage(ac, acDefinition, new UpdateSync(), UUID.randomUUID())

        then:
        1 * acProvider.updateAutomationComposition(_ as AutomationComposition)
        !ac.elements.containsKey(element.id)
        1 * participantSyncPublisher.sendDeleteSync(ac, element.participantId)
    }

    def "scan migrating AC - element deleted but participant retains other elements should not send delete sync"() {
        given:
        def participantSyncPublisher = Mock(ParticipantSyncPublisher)
        def acProvider = Mock(AutomationCompositionProvider)
        def scanner = buildStageScanner(acProvider, participantSyncPublisher)
        def compositionTargetId = UUID.randomUUID()
        def ac = buildMigratingAc(COMPOSITION_ID, compositionTargetId)
        def acDefinition = buildAcDefinition()
        def element = ac.elements.values().iterator().next()
        element.deployState = DeployState.DELETED
        ac.elements.values().each { it.participantId = element.participantId }
        acProvider.updateAutomationComposition(_) >> ac

        when:
        scanner.scanStage(ac, acDefinition, new UpdateSync(), UUID.randomUUID())

        then:
        1 * acProvider.updateAutomationComposition(_ as AutomationComposition)
        !ac.elements.containsKey(element.id)
        0 * participantSyncPublisher.sendDeleteSync(ac, element.participantId)
    }

    def "scan migration reverting AC - element migrated should complete and reset migration state"() {
        given:
        def acProvider = Mock(AutomationCompositionProvider)
        def scanner = buildStageScanner(acProvider)
        def compositionTargetId = UUID.randomUUID()
        def ac = buildMigrationRevertingAc(COMPOSITION_ID, compositionTargetId)
        def acDefinition = buildAcDefinition()
        def element = ac.elements.values().iterator().next()
        element.deployState = DeployState.DEPLOYED
        element.migrationState = MigrationState.REMOVED
        acProvider.updateAutomationComposition(_) >> ac

        when:
        scanner.scanStage(ac, acDefinition, new UpdateSync(), UUID.randomUUID())

        then:
        1 * acProvider.updateAutomationComposition(_ as AutomationComposition)
        element.migrationState == MigrationState.DEFAULT
        ac.deployState == DeployState.DEPLOYED
    }

    def "scan preparing AC - first element not prepared yet should not update"() {
        given:
        def acProvider = Mock(AutomationCompositionProvider)
        def scanner = buildStageScanner(acProvider)
        def ac = buildPreparingAc(COMPOSITION_ID)
        def acDefinition = buildAcDefinition()

        when:
        scanner.scanStage(ac, acDefinition, new UpdateSync(), UUID.randomUUID())

        then:
        0 * acProvider.updateAutomationComposition(_)
        ac.subState == SubState.PREPARING
    }

    def "scan preparing AC - send message for next stage"() {
        given:
        def acProvider = Mock(AutomationCompositionProvider)
        def scanner = buildStageScanner(acProvider)
        def ac = buildPreparingAc(COMPOSITION_ID)
        def acDefinition = buildAcDefinition()
        def element = ac.elements.values().iterator().next()
        def toscaNodeTemplate = acDefinition.serviceTemplate.toscaTopologyTemplate
                .nodeTemplates[element.definition.name]
        toscaNodeTemplate.properties = [stage: [prepare: [1]]]
        acProvider.updateAutomationComposition(_) >> ac

        when:
        scanner.scanStage(ac, acDefinition, new UpdateSync(), UUID.randomUUID())

        then:
        1 * acProvider.updateAutomationComposition(_ as AutomationComposition)
        ac.subState == SubState.PREPARING
    }

    def "scan preparing AC - first element prepared should complete"() {
        given:
        def acProvider = Mock(AutomationCompositionProvider)
        def scanner = buildStageScanner(acProvider)
        def ac = buildPreparingAc(COMPOSITION_ID)
        def acDefinition = buildAcDefinition()
        def element = ac.elements.values().iterator().next()
        element.subState = SubState.NONE
        acProvider.updateAutomationComposition(_) >> ac

        when:
        scanner.scanStage(ac, acDefinition, new UpdateSync(), UUID.randomUUID())

        then:
        1 * acProvider.updateAutomationComposition(_ as AutomationComposition)
        ac.subState == SubState.NONE
    }

    static buildMigratingAcWrapper(UUID compositionId, UUID compositionTargetId) {
        buildMigratingAc(compositionId, compositionTargetId)
    }

    static buildRevertingAcWrapper(UUID compositionId, UUID compositionTargetId) {
        buildMigrationRevertingAc(compositionId, compositionTargetId)
    }

    def buildStageScanner(AutomationCompositionProvider acProvider,
                          ParticipantSyncPublisher participantSyncPublisher = null) {
        def acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner")
        def encryptionUtils = new EncryptionUtils(acRuntimeParameterGroup)
        return new StageScanner(acProvider,
                participantSyncPublisher ?: Mock(ParticipantSyncPublisher),
                Mock(AutomationCompositionMigrationPublisher), Mock(AcPreparePublisher),
                acRuntimeParameterGroup, encryptionUtils)
    }
}
