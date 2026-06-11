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

import jakarta.validation.ValidationException
import org.onap.policy.clamp.models.acm.concepts.ParticipantSupportedElementType
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier

import static org.onap.policy.clamp.acm.runtime.helper.SupervisionParticipantHandlerTestHelper.buildAcDef
import static org.onap.policy.clamp.acm.runtime.helper.SupervisionParticipantHandlerTestHelper.buildAcDefWithElement
import static org.onap.policy.clamp.acm.runtime.helper.SupervisionParticipantHandlerTestHelper.buildAcDefWithServiceTemplate
import static org.onap.policy.clamp.acm.runtime.helper.SupervisionParticipantHandlerTestHelper.buildDeployedAc
import static org.onap.policy.clamp.acm.runtime.helper.SupervisionParticipantHandlerTestHelper.buildMigratingAc
import static org.onap.policy.clamp.acm.runtime.helper.SupervisionParticipantHandlerTestHelper.buildRollback
import static org.onap.policy.clamp.acm.runtime.helper.SupervisionParticipantHandlerTestHelper.createDeregisterMessage
import static org.onap.policy.clamp.acm.runtime.helper.SupervisionParticipantHandlerTestHelper.createHandler
import static org.onap.policy.clamp.acm.runtime.helper.SupervisionParticipantHandlerTestHelper.createRegisterMessage
import static org.onap.policy.clamp.acm.runtime.helper.SupervisionParticipantHandlerTestHelper.createReqSyncMessage
import static org.onap.policy.clamp.acm.runtime.helper.SupervisionParticipantHandlerTestHelper.createStatusMessage
import static org.onap.policy.clamp.acm.runtime.helper.SupervisionParticipantHandlerTestHelper.createStatusMessageWithParticipantDef

import org.onap.policy.clamp.acm.runtime.main.utils.EncryptionUtils
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantDeregisterAckPublisher
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantRegisterAckPublisher
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantSyncPublisher
import org.onap.policy.clamp.acm.runtime.util.CommonTestData
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionInfo
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider
import org.onap.policy.clamp.models.acm.persistence.provider.MessageProvider
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider
import spock.lang.Specification

class SupervisionParticipantHandlerSpec extends Specification {

    static final PARTICIPANT_ID = CommonTestData.getParticipantId()
    static final REPLICA_ID = CommonTestData.getReplicaId()

    // ---- Deregister ----

    def "deregister should send ack"() {
        given:
        def deregisterAck = Mock(ParticipantDeregisterAckPublisher)
        def participantProvider = Mock(ParticipantProvider)
        def handler = buildHandler(
                participantProvider: participantProvider,
                deregisterAckPublisher: deregisterAck)
        def msg = createDeregisterMessage(msgParams)

        when:
        handler.handleParticipantMessage(msg)

        then:
        1 * participantProvider.findParticipantReplica(lookupId) >>
                replicaOpt
        deleteCount * participantProvider
                .deleteParticipantReplica(lookupId)
        1 * deregisterAck.send(msg.messageId)

        where:
        msgParams               | lookupId   | replicaOpt                                                       | deleteCount
        [replicaId: REPLICA_ID] | REPLICA_ID | Optional.of(CommonTestData.createParticipantReplica(REPLICA_ID)) | 1
    }

    // ---- Register ----

    def "register '#desc' should save and send ack"() {
        given:
        def participantProvider = Mock(ParticipantProvider)
        def registerAck = Mock(ParticipantRegisterAckPublisher)
        def handler = buildHandler(
                participantProvider: participantProvider,
                registerAckPublisher: registerAck)
        def msg = createRegisterMessage()

        when:
        handler.handleParticipantMessage(msg)

        then:
        1 * participantProvider.getSupportedElementMap() >> Map.of()
        1 * participantProvider.findParticipant(PARTICIPANT_ID) >>
                participantOpt
        1 * participantProvider.saveParticipant(_)
        1 * participantProvider.getCompositionIds(PARTICIPANT_ID) >>
                Collections.emptySet()
        1 * registerAck.send(msg.messageId, PARTICIPANT_ID, REPLICA_ID)

        where:
        participantOpt                       | desc
        Optional.empty()                     | "new replica"
        Optional.of(buildParticipantDiff())  | "new replica with new supported element type"
    }

    def "register existing supported element type should throw ValidationException"() {
        given:
        def participantProvider = Mock(ParticipantProvider)
        def registerAck = Mock(ParticipantRegisterAckPublisher)
        def syncPublisher = Mock(ParticipantSyncPublisher)
        def acDefinitionProvider = Mock(AcDefinitionProvider)
        def handler = buildHandler(
                participantProvider: participantProvider,
                registerAckPublisher: registerAck,
                acDefinitionProvider: acDefinitionProvider,
                syncPublisher: syncPublisher)
        def msg = createRegisterMessage(replicaId: REPLICA_ID)
        def participant = buildParticipant()
        def supportedElement = CommonTestData.createParticipantSupportedElementType()

        when:
        handler.handleParticipantMessage(msg)

        then: "a ValidationException should be thrown"
        then:
        1 * participantProvider.getSupportedElementMap() >>
                Map.of(new ToscaConceptIdentifier(supportedElement.typeName, supportedElement.typeVersion),
                        UUID.randomUUID())
        0 * registerAck.send(msg.messageId, PARTICIPANT_ID, REPLICA_ID)
        0 * acDefinitionProvider.updateAcDefinition(_, _)
        0 * syncPublisher.sendRestartMsg(_, _, _, _)
        thrown(ValidationException)
    }

    def "register existing replica should ack without restart"() {
        given:
        def participantProvider = Mock(ParticipantProvider)
        def registerAck = Mock(ParticipantRegisterAckPublisher)
        def syncPublisher = Mock(ParticipantSyncPublisher)
        def acDefinitionProvider = Mock(AcDefinitionProvider)
        def handler = buildHandler(
                participantProvider: participantProvider,
                registerAckPublisher: registerAck,
                acDefinitionProvider: acDefinitionProvider,
                syncPublisher: syncPublisher)
        def msg = createRegisterMessage(replicaId: REPLICA_ID)
        def participant = buildParticipant()
        def supportedElement = CommonTestData.createParticipantSupportedElementType()

        when:
        handler.handleParticipantMessage(msg)

        then:
        1 * participantProvider.getSupportedElementMap() >>
                Map.of(new ToscaConceptIdentifier(supportedElement.typeName, supportedElement.typeVersion),
                        PARTICIPANT_ID)
        1 * participantProvider.findParticipant(PARTICIPANT_ID) >>
                Optional.of(participant)
        1 * registerAck.send(msg.messageId, PARTICIPANT_ID, REPLICA_ID)
        0 * acDefinitionProvider.updateAcDefinition(_, _)
        0 * syncPublisher.sendRestartMsg(_, _, _, _)
    }

    def "register with intermediaryVersion should log and process successfully"() {
        given:
        def participantProvider = Mock(ParticipantProvider)
        def registerAck = Mock(ParticipantRegisterAckPublisher)
        def handler = buildHandler(
                participantProvider: participantProvider,
                registerAckPublisher: registerAck)
        def msg = createRegisterMessage(intermediaryVersion: "9.0.2-SNAPSHOT", replicaId: REPLICA_ID)

        when:
        handler.handleParticipantMessage(msg)

        then:
        1 * participantProvider.findParticipantReplica(REPLICA_ID) >>
                Optional.empty()
        1 * participantProvider.findParticipant(PARTICIPANT_ID) >>
                Optional.empty()
        1 * participantProvider.saveParticipant(_)
        1 * participantProvider.getCompositionIds(PARTICIPANT_ID) >>
                Collections.emptySet()
        1 * registerAck.send(msg.messageId, PARTICIPANT_ID, REPLICA_ID)
    }

    // ---- Status ----

    def "status with instance out properties should save them"() {
        given:
        def participantProvider = Mock(ParticipantProvider)
        def messageProvider = Mock(MessageProvider)
        def handler = buildHandler(
                participantProvider: participantProvider,
                messageProvider: messageProvider)
        def msg = createStatusMessage(
                compositionId: UUID.randomUUID(),
                replicaId: REPLICA_ID)
        msg.automationCompositionInfoList = [
                new AutomationCompositionInfo()]
        def participant = buildParticipant()
        def replica = participant.getReplicas().get(REPLICA_ID)

        when:
        handler.handleParticipantMessage(msg)

        then:
        1 * participantProvider.findParticipant(PARTICIPANT_ID) >>
                Optional.of(participant)
        1 * participantProvider.saveParticipantReplica(replica)
        1 * messageProvider.saveInstanceOutProperties(_)
    }

    def "status composition out '#desc'"() {
        given:
        def participantProvider = Mock(ParticipantProvider)
        def messageProvider = Mock(MessageProvider)
        def acDefinitionProvider = Mock(AcDefinitionProvider)
        def compositionId = UUID.randomUUID()
        def handler = buildHandler(
                participantProvider: participantProvider,
                acDefinitionProvider: acDefinitionProvider,
                messageProvider: messageProvider)
        def msg = createStatusMessageWithParticipantDef(
                compositionId, PARTICIPANT_ID)
        def participant = buildParticipant()

        when:
        handler.handleParticipantMessage(msg)

        then:
        1 * participantProvider.findParticipant(PARTICIPANT_ID) >>
                Optional.of(participant)
        1 * acDefinitionProvider.findAcDefinition(compositionId) >>
                definitionOpt
        saveCount * messageProvider.saveCompositionOutProperties(_, _)

        where:
        desc        | definitionOpt                              | saveCount
        "found"     | Optional.of(buildAcDefWithElement("code")) | 1
        "not found" | Optional.empty()                           | 0
    }

    def "status '#desc' should handle participant"() {
        given:
        def participantProvider = Mock(ParticipantProvider)
        def handler = buildHandler(
                participantProvider: participantProvider)
        def msg = createStatusMessage()
        msg.automationCompositionInfoList = []

        when:
        handler.handleParticipantMessage(msg)

        then:
        1 * participantProvider.findParticipant(PARTICIPANT_ID) >>
                participantOpt
        restart * participantProvider.getCompositionIds(PARTICIPANT_ID) >>
                Set.of()
        saveReplicaCount * participantProvider.saveParticipantReplica(_)
        savePartCount * participantProvider.saveParticipant(_)

        where:
        desc             | registered | saveReplicaCount | savePartCount | restart
        "not registered" | false      | 0                | 1             | 1
        "check online"   | true       | 1                | 0             | 0

        participantOpt = registered
                ? Optional.of(buildParticipant())
                : Optional.empty()
    }

    // ---- ReqSync ----

    def "reqSync '#desc' should send restart with empty AC list"() {
        given:
        def acDefinitionProvider = Mock(AcDefinitionProvider)
        def syncPublisher = Mock(ParticipantSyncPublisher)
        def compositionId = UUID.randomUUID()
        def acDef = buildAcDef(compositionId)
        def handler = buildHandler(
                acDefinitionProvider: acDefinitionProvider,
                syncPublisher: syncPublisher)
        def msg = createReqSyncMessage(msgParams(compositionId))

        when:
        handler.handleParticipantReqSync(msg)

        then:
        1 * acDefinitionProvider.getAcDefinition(compositionId) >> acDef
        1 * syncPublisher.sendRestartMsg(
                PARTICIPANT_ID, null, acDef, [])

        where:
        desc                  | msgParams
        "compositionId"       | { id -> [compositionId: id] }
        "compositionTargetId" | { id -> [compositionTargetId: id] }
    }

    def "reqSync with compositionId and instanceId should include AC"() {
        given:
        def acDefinitionProvider = Mock(AcDefinitionProvider)
        def syncPublisher = Mock(ParticipantSyncPublisher)
        def acProvider = Mock(AutomationCompositionProvider)
        def compositionId = UUID.randomUUID()
        def acDef = buildAcDef(compositionId)
        def ac = buildDeployedAc(compositionId)
        def handler = buildHandler(
                acProvider: acProvider,
                acDefinitionProvider: acDefinitionProvider,
                syncPublisher: syncPublisher)
        def msg = createReqSyncMessage(
                compositionId: compositionId,
                automationCompositionId: ac.instanceId)

        when:
        handler.handleParticipantReqSync(msg)

        then:
        1 * acDefinitionProvider.getAcDefinition(compositionId) >> acDef
        1 * acProvider.getAutomationComposition(ac.instanceId) >> ac
        1 * syncPublisher.sendRestartMsg(
                PARTICIPANT_ID, null, acDef, [ac])
    }

    def "reqSync MIGRATING '#desc' should send sync"() {
        given:
        def acDefinitionProvider = Mock(AcDefinitionProvider)
        def syncPublisher = Mock(ParticipantSyncPublisher)
        def acProvider = Mock(AutomationCompositionProvider)
        def compositionId = UUID.randomUUID()
        def acDef = buildAcDefWithServiceTemplate(compositionId)
        def ac = buildMigratingAc(compositionId, phase)
        def handler = buildHandler(
                acProvider: acProvider,
                acDefinitionProvider: acDefinitionProvider,
                syncPublisher: syncPublisher)
        def msg = createReqSyncMessage(
                automationCompositionId: ac.instanceId)

        when:
        handler.handleParticipantReqSync(msg)

        then:
        1 * acProvider.getAutomationComposition(ac.instanceId) >> ac
        1 * acDefinitionProvider.getAcDefinition(compositionId) >> acDef
        rollbackCount * acProvider.getAutomationCompositionRollback(
                ac.instanceId) >> buildRollback()
        1 * syncPublisher.sendSync(ac)

        where:
        desc                 | phase | rollbackCount
        "not at first stage" | 1     | 0
        "at first stage"     | 0     | 1
    }

    // ---- Helpers ----

    def buildHandler(Map overrides) {
        def defaults = [
                participantProvider   : Mock(ParticipantProvider),
                registerAckPublisher  : Mock(ParticipantRegisterAckPublisher),
                deregisterAckPublisher: Mock(ParticipantDeregisterAckPublisher),
                acProvider            : Mock(AutomationCompositionProvider),
                acDefinitionProvider  : Mock(AcDefinitionProvider),
                syncPublisher         : Mock(ParticipantSyncPublisher),
                messageProvider       : Mock(MessageProvider),
                encryptionUtils       : Mock(EncryptionUtils)
        ]
        createHandler(defaults + overrides)
    }

    def buildParticipant() {
        def replica = CommonTestData.createParticipantReplica(REPLICA_ID)
        def participant = CommonTestData
                .createParticipant(PARTICIPANT_ID)
        participant.participantSupportedElementTypes = Map.of(
                UUID.randomUUID(), CommonTestData.createParticipantSupportedElementType())
        participant.getReplicas().put(REPLICA_ID, replica)
        return participant
    }
    def buildParticipantDiff() {
        def participant = buildParticipant()
        def participantSupportedElementType = new ParticipantSupportedElementType()
        participantSupportedElementType.setTypeName("name")
        participantSupportedElementType.setTypeVersion("1.0.0")
        participant.participantSupportedElementTypes = Map.of(
                UUID.randomUUID(), CommonTestData.createParticipantSupportedElementType(),
                participantSupportedElementType.getId(), participantSupportedElementType)
        return participant
    }
}
