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
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionParticipantHandler
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantDeregisterAckPublisher
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantRegisterAckPublisher
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantSyncPublisher
import org.onap.policy.clamp.acm.runtime.util.CommonTestData
import org.onap.policy.clamp.models.acm.concepts.AcTypeState
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionRollback
import org.onap.policy.clamp.models.acm.concepts.DeployState
import org.onap.policy.clamp.models.acm.concepts.NodeTemplateState
import org.onap.policy.clamp.models.acm.concepts.ParticipantDefinition
import org.onap.policy.clamp.models.acm.concepts.ParticipantState
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantDeregister
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantRegister
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantReqSync
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatus
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider
import org.onap.policy.clamp.models.acm.persistence.provider.MessageProvider
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate
import org.onap.policy.models.tosca.authorative.concepts.ToscaTopologyTemplate

class SupervisionParticipantHandlerTestHelper {

    static final PARTICIPANT_ID = CommonTestData.getParticipantId()
    static final REPLICA_ID = CommonTestData.getReplicaId()
    static final AC_JSON =
            "src/test/resources/rest/acm/AutomationComposition.json"

    static createHandler(Map mocks) {
        return new SupervisionParticipantHandler(
                mocks.participantProvider as ParticipantProvider,
                mocks.registerAckPublisher as ParticipantRegisterAckPublisher,
                mocks.deregisterAckPublisher as ParticipantDeregisterAckPublisher,
                mocks.acProvider as AutomationCompositionProvider,
                mocks.acDefinitionProvider as AcDefinitionProvider,
                mocks.syncPublisher as ParticipantSyncPublisher,
                mocks.messageProvider as MessageProvider,
                mocks.encryptionUtils as EncryptionUtils
        )
    }


    static buildAcDefWithElement(String name) {
        def nodeState = new NodeTemplateState()
        return new AutomationCompositionDefinition(
                state: AcTypeState.COMMISSIONED,
                compositionId: UUID.randomUUID(),
                elementStateMap: [(name): nodeState])
    }

    static createDeregisterMessage(Map params = [:]) {
        def msg = new ParticipantDeregister()
        msg.messageId = UUID.randomUUID()
        msg.participantId = params.getOrDefault(
                'participantId', PARTICIPANT_ID)
        if (params.containsKey('replicaId')) {
            msg.replicaId = params.replicaId
        }
        return msg
    }

    static createRegisterMessage(Map params = [:]) {
        def msg = new ParticipantRegister()
        msg.messageId = UUID.randomUUID()
        msg.participantId = params.getOrDefault(
                'participantId', PARTICIPANT_ID)
        if (params.containsKey('replicaId')) {
            msg.replicaId = params.replicaId
        }
        if (params.containsKey('intermediaryVersion')) {
            msg.intermediaryVersion = params.intermediaryVersion
        }
        def supported = CommonTestData.createParticipantSupportedElementType()
        msg.participantSupportedElementType = [supported]
        return msg
    }

    static createStatusMessage(Map params = [:]) {
        def msg = new ParticipantStatus()
        msg.participantId = params.getOrDefault(
                'participantId', PARTICIPANT_ID)
        msg.state = ParticipantState.ON_LINE
        def supported = CommonTestData.createParticipantSupportedElementType()
        msg.participantSupportedElementType = [supported]
        if (params.containsKey('compositionId')) {
            msg.compositionId = params.compositionId
        }
        if (params.containsKey('replicaId')) {
            msg.replicaId = params.replicaId
        }
        return msg
    }

    static createReqSyncMessage(Map params = [:]) {
        def msg = new ParticipantReqSync()
        msg.participantId = params.getOrDefault(
                'participantId', PARTICIPANT_ID)
        msg.replicaId = params.getOrDefault('replicaId', REPLICA_ID)
        if (params.containsKey('compositionId')) {
            msg.compositionId = params.compositionId
        }
        if (params.containsKey('compositionTargetId')) {
            msg.compositionTargetId = params.compositionTargetId
        }
        if (params.containsKey('automationCompositionId')) {
            msg.automationCompositionId = params.automationCompositionId
        }
        return msg
    }

    static createParticipantDefMessage(UUID compositionId, UUID participantId, Map params = [:]) {
        def msg = createStatusMessage(
                compositionId: compositionId,
                participantId: participantId)
        if (params.containsKey('replicaId')) {
            msg.replicaId = params.replicaId
        }
        return msg
    }

    static createStatusMessageWithParticipantDef(UUID compositionId, UUID participantId) {
        def acElementDef = new AutomationCompositionElementDefinition(
                acElementDefinitionId: new ToscaConceptIdentifier(
                        "code", "1.0.0"))
        def participantDef = new ParticipantDefinition(
                participantId: participantId,
                automationCompositionElementDefinitionList: [acElementDef])
        def msg = createStatusMessage(compositionId: compositionId)
        msg.participantDefinitionUpdates = [participantDef]
        return msg
    }

    static buildAcDef(UUID compositionId) {
        new AutomationCompositionDefinition(compositionId: compositionId)
    }

    static buildAcDefWithServiceTemplate(UUID compositionId) {
        def acDef = buildAcDef(compositionId)
        acDef.serviceTemplate = new ToscaServiceTemplate(
                toscaTopologyTemplate: new ToscaTopologyTemplate())
        return acDef
    }

    static buildDeployedAc(UUID compositionId) {
        def ac = InstantiationUtils.getAutomationCompositionFromResource(
                AC_JSON, "Crud")
        ac.compositionId = compositionId
        ac.instanceId = UUID.randomUUID()
        ac.deployState = DeployState.DEPLOYED
        return ac
    }

    static buildMigratingAc(UUID compositionTargetId, int phase) {
        new AutomationComposition(
                elements: [:],
                phase: phase,
                compositionTargetId: compositionTargetId,
                instanceId: UUID.randomUUID(),
                deployState: DeployState.MIGRATING)
    }

    static buildRollback() {
        new AutomationCompositionRollback(elements: [:])
    }
}
