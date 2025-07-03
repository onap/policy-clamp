/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.participant.intermediary.handler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.acm.participant.intermediary.handler.cache.CacheProvider;
import org.onap.policy.clamp.acm.participant.intermediary.main.parameters.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantSupportedElementType;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeploy;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionMigration;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionPrepare;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionStateChange;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantAckMessage;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantDeregister;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantDeregisterAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessage;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrime;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantRegister;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantRegisterAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatus;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatusReq;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantSync;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.PropertiesUpdate;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.LockOrder;

class ParticipantHandlerTest {

    @Test
    void handleParticipantStatusReqTest() {
        var publisher = mock(ParticipantMessagePublisher.class);
        when(publisher.isActive()).thenReturn(true);
        var cacheProvider = mock(CacheProvider.class);
        var msgExecutor = mock(MsgExecutor.class);
        var participantHandler = new ParticipantHandler(mock(AutomationCompositionHandler.class),
            mock(AcLockHandler.class), mock(AcSubStateHandler.class), mock(AcDefinitionHandler.class),
            publisher, cacheProvider, msgExecutor);
        participantHandler.handleParticipantStatusReq(new ParticipantStatusReq());
        verify(publisher).sendParticipantRegister(any(ParticipantRegister.class));

        when(cacheProvider.isRegistered()).thenReturn(true);
        clearInvocations(publisher);
        participantHandler.handleParticipantStatusReq(new ParticipantStatusReq());
        verify(publisher).sendParticipantStatus(any(ParticipantStatus.class));
    }

    @Test
    void handleAutomationCompositionDeployTest() {
        var cacheProvider = mock(CacheProvider.class);
        var automationCompositionDeploy = new AutomationCompositionDeploy();
        automationCompositionDeploy.setAutomationCompositionId(UUID.randomUUID());
        automationCompositionDeploy.setRevisionIdInstance(UUID.randomUUID());
        when(cacheProvider.isInstanceUpdated(automationCompositionDeploy.getAutomationCompositionId(),
                automationCompositionDeploy.getRevisionIdInstance())).thenReturn(true);
        var acHandler = mock(AutomationCompositionHandler.class);
        var msgExecutor = new MsgExecutor(cacheProvider, mock(ParticipantMessagePublisher.class));
        var participantHandler = new ParticipantHandler(acHandler, mock(AcLockHandler.class),
                mock(AcSubStateHandler.class), mock(AcDefinitionHandler.class), mock(ParticipantMessagePublisher.class),
                cacheProvider, msgExecutor);
        participantHandler.handleAutomationCompositionDeploy(automationCompositionDeploy);
        verify(acHandler).handleAutomationCompositionDeploy(automationCompositionDeploy);
    }

    @Test
    void handleAutomationCompositionStateChangeTest() {
        var acStateChange = new AutomationCompositionStateChange();
        acStateChange.setCompositionId(UUID.randomUUID());
        acStateChange.setRevisionIdComposition(UUID.randomUUID());
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.isCompositionDefinitionUpdated(acStateChange.getCompositionId(),
                acStateChange.getRevisionIdComposition())).thenReturn(true);

        acStateChange.setDeployOrderedState(DeployOrder.DEPLOY);
        acStateChange.setLockOrderedState(LockOrder.NONE);
        var acHandler = mock(AutomationCompositionHandler.class);
        var acLockHandler = mock(AcLockHandler.class);
        var msgExecutor = new MsgExecutor(cacheProvider, mock(ParticipantMessagePublisher.class));
        var participantHandler = new ParticipantHandler(acHandler, acLockHandler, mock(AcSubStateHandler.class),
                mock(AcDefinitionHandler.class), mock(ParticipantMessagePublisher.class), cacheProvider, msgExecutor);
        participantHandler.handleAutomationCompositionStateChange(acStateChange);
        verify(acHandler).handleAutomationCompositionStateChange(acStateChange);

        acStateChange.setDeployOrderedState(DeployOrder.NONE);
        acStateChange.setLockOrderedState(LockOrder.LOCK);
        participantHandler.handleAutomationCompositionStateChange(acStateChange);
        verify(acLockHandler).handleAutomationCompositionStateChange(acStateChange);
    }

    @Test
    void handleAutomationCompositionMigrationTest() {
        var cacheProvider = mock(CacheProvider.class);
        var migrationMsg = new AutomationCompositionMigration();
        migrationMsg.setCompositionId(UUID.randomUUID());
        migrationMsg.setRevisionIdComposition(UUID.randomUUID());
        when(cacheProvider.isCompositionDefinitionUpdated(migrationMsg.getCompositionId(),
                migrationMsg.getRevisionIdComposition())).thenReturn(true);

        migrationMsg.setAutomationCompositionId(UUID.randomUUID());
        migrationMsg.setRevisionIdInstance(UUID.randomUUID());
        when(cacheProvider.isInstanceUpdated(migrationMsg.getAutomationCompositionId(),
                migrationMsg.getRevisionIdInstance())).thenReturn(true);

        migrationMsg.setCompositionTargetId(UUID.randomUUID());
        migrationMsg.setRevisionIdCompositionTarget(UUID.randomUUID());
        when(cacheProvider.isCompositionDefinitionUpdated(migrationMsg.getCompositionTargetId(),
                migrationMsg.getRevisionIdCompositionTarget())).thenReturn(true);

        var acHandler = mock(AutomationCompositionHandler.class);
        var acSubStateHandler = mock(AcSubStateHandler.class);
        var msgExecutor = new MsgExecutor(cacheProvider, mock(ParticipantMessagePublisher.class));
        var participantHandler = new ParticipantHandler(acHandler, mock(AcLockHandler.class),
                acSubStateHandler, mock(AcDefinitionHandler.class), mock(ParticipantMessagePublisher.class),
                cacheProvider, msgExecutor);
        participantHandler.handleAutomationCompositionMigration(migrationMsg);
        verify(acHandler).handleAutomationCompositionMigration(migrationMsg);

        migrationMsg.setPrecheck(true);
        participantHandler.handleAutomationCompositionMigration(migrationMsg);
        verify(acSubStateHandler).handleAcMigrationPrecheck(migrationMsg);
    }

    @Test
    void handleAcPropertyUpdateTest() {
        var propertyUpdateMsg = new PropertiesUpdate();
        propertyUpdateMsg.setCompositionId(UUID.randomUUID());
        propertyUpdateMsg.setRevisionIdComposition(UUID.randomUUID());
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.isCompositionDefinitionUpdated(propertyUpdateMsg.getCompositionId(),
                propertyUpdateMsg.getRevisionIdComposition())).thenReturn(true);

        propertyUpdateMsg.setAutomationCompositionId(UUID.randomUUID());
        propertyUpdateMsg.setRevisionIdInstance(UUID.randomUUID());
        when(cacheProvider.isInstanceUpdated(propertyUpdateMsg.getAutomationCompositionId(),
                propertyUpdateMsg.getRevisionIdInstance())).thenReturn(true);

        var acHandler = mock(AutomationCompositionHandler.class);
        var msgExecutor = new MsgExecutor(cacheProvider, mock(ParticipantMessagePublisher.class));
        var participantHandler = new ParticipantHandler(acHandler, mock(AcLockHandler.class),
                mock(AcSubStateHandler.class), mock(AcDefinitionHandler.class), mock(ParticipantMessagePublisher.class),
                cacheProvider, msgExecutor);
        participantHandler.handleAcPropertyUpdate(propertyUpdateMsg);
        verify(acHandler).handleAcPropertyUpdate(propertyUpdateMsg);
    }

    @Test
    void sendHandleAutomationCompositionPrepare() {
        var acPrepareMsg = new AutomationCompositionPrepare();
        acPrepareMsg.setParticipantId(UUID.randomUUID());
        acPrepareMsg.setRevisionIdComposition(UUID.randomUUID());
        acPrepareMsg.setPreDeploy(false);

        var cacheProvider = mock(CacheProvider.class);
        var acSubStateHandler = mock(AcSubStateHandler.class);
        var msgExecutor = new MsgExecutor(cacheProvider, mock(ParticipantMessagePublisher.class));
        var participantHandler = new ParticipantHandler(mock(AutomationCompositionHandler.class),
                mock(AcLockHandler.class), acSubStateHandler, mock(AcDefinitionHandler.class),
                mock(ParticipantMessagePublisher.class), cacheProvider, msgExecutor);

        participantHandler.handleAutomationCompositionPrepare(acPrepareMsg);
        verify(acSubStateHandler).handleAcPrepare(acPrepareMsg);
    }

    @Test
    void appliesToTest() {
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getParticipantId()).thenReturn(CommonTestData.getParticipantId());
        when(cacheProvider.getReplicaId()).thenReturn(CommonTestData.getReplicaId());
        var msgExecutor = mock(MsgExecutor.class);
        var participantHandler = new ParticipantHandler(mock(AutomationCompositionHandler.class),
            mock(AcLockHandler.class), mock(AcSubStateHandler.class), mock(AcDefinitionHandler.class),
            mock(ParticipantMessagePublisher.class), cacheProvider, msgExecutor);

        var participantAckMsg = new ParticipantAckMessage(ParticipantMessageType.AUTOMATION_COMPOSITION_DEPLOY);
        assertTrue(participantHandler.appliesTo(participantAckMsg));

        var participantMsg = new ParticipantMessage(ParticipantMessageType.PARTICIPANT_STATUS);
        assertTrue(participantHandler.appliesTo(participantMsg));

        participantMsg.setParticipantId(UUID.randomUUID());
        assertFalse(participantHandler.appliesTo(participantMsg));
    }

    @Test
    void sendParticipantRegister() {
        var publisher = mock(ParticipantMessagePublisher.class);
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getParticipantId()).thenReturn(CommonTestData.getParticipantId());
        when(cacheProvider.getSupportedAcElementTypes()).thenReturn(List.of(new ParticipantSupportedElementType()));
        var msgExecutor = mock(MsgExecutor.class);
        var participantHandler = new ParticipantHandler(mock(AutomationCompositionHandler.class),
            mock(AcLockHandler.class), mock(AcSubStateHandler.class), mock(AcDefinitionHandler.class), publisher,
            cacheProvider, msgExecutor);

        participantHandler.sendParticipantRegister();
        verify(publisher).sendParticipantRegister(any(ParticipantRegister.class));
    }

    @Test
    void handleParticipantRegisterAckTest() {
        var publisher = mock(ParticipantMessagePublisher.class);
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getParticipantId()).thenReturn(CommonTestData.getParticipantId());
        var msgExecutor = mock(MsgExecutor.class);
        var participantHandler = new ParticipantHandler(mock(AutomationCompositionHandler.class),
            mock(AcLockHandler.class), mock(AcSubStateHandler.class), mock(AcDefinitionHandler.class), publisher,
            cacheProvider, msgExecutor);

        participantHandler.handleParticipantRegisterAck(new ParticipantRegisterAck());
        verify(publisher).sendParticipantStatus(any(ParticipantStatus.class));
    }

    @Test
    void sendParticipantDeregisterTest() {
        var publisher = mock(ParticipantMessagePublisher.class);
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getParticipantId()).thenReturn(CommonTestData.getParticipantId());
        var msgExecutor = mock(MsgExecutor.class);
        var participantHandler = new ParticipantHandler(mock(AutomationCompositionHandler.class),
            mock(AcLockHandler.class), mock(AcSubStateHandler.class), mock(AcDefinitionHandler.class), publisher,
            cacheProvider, msgExecutor);

        participantHandler.sendParticipantDeregister();
        verify(publisher).sendParticipantDeregister(any(ParticipantDeregister.class));
    }

    @Test
    void handleParticipantDeregisterAckTest() {
        var msgExecutor = mock(MsgExecutor.class);
        var participantHandler = new ParticipantHandler(mock(AutomationCompositionHandler.class),
            mock(AcLockHandler.class), mock(AcSubStateHandler.class), mock(AcDefinitionHandler.class),
            mock(ParticipantMessagePublisher.class), mock(CacheProvider.class), msgExecutor);
        var participantDeregisterAck = new ParticipantDeregisterAck();
        assertDoesNotThrow(() -> participantHandler.handleParticipantDeregisterAck(participantDeregisterAck));
    }

    @Test
    void handleParticipantPrimeTest() {
        var participantPrime = new ParticipantPrime();
        participantPrime.setCompositionId(UUID.randomUUID());
        participantPrime.setMessageId(UUID.randomUUID());

        var acHandler = mock(AcDefinitionHandler.class);
        var msgExecutor = mock(MsgExecutor.class);
        var participantHandler = new ParticipantHandler(mock(AutomationCompositionHandler.class),
            mock(AcLockHandler.class), mock(AcSubStateHandler.class), acHandler,
            mock(ParticipantMessagePublisher.class), mock(CacheProvider.class), msgExecutor);

        participantHandler.handleParticipantPrime(participantPrime);
        verify(acHandler).handlePrime(participantPrime);
    }

    @Test
    void handleParticipantRestartTest() {
        var participantSyncMsg = new ParticipantSync();
        participantSyncMsg.setState(AcTypeState.PRIMED);
        participantSyncMsg.setCompositionId(UUID.randomUUID());
        participantSyncMsg.setReplicaId(CommonTestData.getReplicaId());

        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getReplicaId()).thenReturn(CommonTestData.getReplicaId());
        var publisher = mock(ParticipantMessagePublisher.class);
        var acHandler = mock(AcDefinitionHandler.class);
        var msgExecutor = mock(MsgExecutor.class);
        var participantHandler = new ParticipantHandler(mock(AutomationCompositionHandler.class),
            mock(AcLockHandler.class), mock(AcSubStateHandler.class), acHandler, publisher, cacheProvider,
            msgExecutor);

        participantSyncMsg.getExcludeReplicas().add(cacheProvider.getReplicaId());
        participantHandler.handleParticipantSync(participantSyncMsg);
        verify(acHandler, times(0)).handleParticipantSync(participantSyncMsg);

        participantSyncMsg.getExcludeReplicas().clear();
        participantHandler.handleParticipantSync(participantSyncMsg);
        verify(acHandler).handleParticipantSync(participantSyncMsg);
    }

    @Test
    void sendHeartbeatTest() {
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getParticipantId()).thenReturn(CommonTestData.getParticipantId());
        when(cacheProvider.isRegistered()).thenReturn(false);
        when(cacheProvider.getAutomationCompositions()).thenReturn(CommonTestData.getTestAutomationCompositionMap());
        var publisher = mock(ParticipantMessagePublisher.class);
        when(publisher.isActive()).thenReturn(true);
        var acHandler = mock(AcDefinitionHandler.class);
        var msgExecutor = mock(MsgExecutor.class);
        var participantHandler = new ParticipantHandler(mock(AutomationCompositionHandler.class),
            mock(AcLockHandler.class), mock(AcSubStateHandler.class), acHandler, publisher, cacheProvider,
            msgExecutor);
        participantHandler.sendHeartbeat();
        verify(publisher).sendParticipantRegister(any(ParticipantRegister.class));

        when(cacheProvider.isRegistered()).thenReturn(true);
        clearInvocations(publisher);
        participantHandler.sendHeartbeat();
        verify(publisher).sendParticipantStatus(any(ParticipantStatus.class));
    }
}
