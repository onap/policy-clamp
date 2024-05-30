/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation.
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.acm.participant.intermediary.main.parameters.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantSupportedElementType;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeploy;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionMigration;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionStateChange;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantAckMessage;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantDeregister;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantDeregisterAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessage;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrime;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantRegister;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantRegisterAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantRestart;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatus;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatusReq;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.PropertiesUpdate;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.LockOrder;

class ParticipantHandlerTest {

    @Test
    void handleParticipantStatusReqTest() {
        var publisher = mock(ParticipantMessagePublisher.class);
        when(publisher.isActive()).thenReturn(true);
        var cacheProvider = mock(CacheProvider.class);
        var participantHandler = new ParticipantHandler(mock(AutomationCompositionHandler.class),
                mock(AcLockHandler.class), mock(AcDefinitionHandler.class), publisher, cacheProvider);
        participantHandler.handleParticipantStatusReq(new ParticipantStatusReq());
        verify(publisher).sendParticipantRegister(any(ParticipantRegister.class));

        when(cacheProvider.isRegistered()).thenReturn(true);
        clearInvocations(publisher);
        participantHandler.handleParticipantStatusReq(new ParticipantStatusReq());
        verify(publisher).sendParticipantStatus(any(ParticipantStatus.class));
    }

    @Test
    void handleAutomationCompositionDeployTest() {
        var acHandler = mock(AutomationCompositionHandler.class);
        var participantHandler = new ParticipantHandler(acHandler, mock(AcLockHandler.class),
                mock(AcDefinitionHandler.class), mock(ParticipantMessagePublisher.class), mock(CacheProvider.class));
        var automationCompositionDeploy = new AutomationCompositionDeploy();
        participantHandler.handleAutomationCompositionDeploy(automationCompositionDeploy);
        verify(acHandler).handleAutomationCompositionDeploy(automationCompositionDeploy);
    }

    @Test
    void handleAutomationCompositionStateChangeTest() {
        var acHandler = mock(AutomationCompositionHandler.class);
        var acLockHandler = mock(AcLockHandler.class);
        var participantHandler = new ParticipantHandler(acHandler, acLockHandler, mock(AcDefinitionHandler.class),
                mock(ParticipantMessagePublisher.class), mock(CacheProvider.class));
        var acStateChange = new AutomationCompositionStateChange();

        acStateChange.setDeployOrderedState(DeployOrder.DEPLOY);
        acStateChange.setLockOrderedState(LockOrder.NONE);
        participantHandler.handleAutomationCompositionStateChange(acStateChange);
        verify(acHandler).handleAutomationCompositionStateChange(acStateChange);

        acStateChange.setDeployOrderedState(DeployOrder.NONE);
        acStateChange.setLockOrderedState(LockOrder.LOCK);
        participantHandler.handleAutomationCompositionStateChange(acStateChange);
        verify(acLockHandler).handleAutomationCompositionStateChange(acStateChange);
    }

    @Test
    void handleAutomationCompositionMigrationTest() {
        var acHandler = mock(AutomationCompositionHandler.class);
        var participantHandler = new ParticipantHandler(acHandler, mock(AcLockHandler.class),
                mock(AcDefinitionHandler.class), mock(ParticipantMessagePublisher.class), mock(CacheProvider.class));
        var migrationMsg = new AutomationCompositionMigration();
        participantHandler.handleAutomationCompositionMigration(migrationMsg);
        verify(acHandler).handleAutomationCompositionMigration(migrationMsg);
    }

    @Test
    void handleAcPropertyUpdateTest() {
        var acHandler = mock(AutomationCompositionHandler.class);
        var participantHandler = new ParticipantHandler(acHandler, mock(AcLockHandler.class),
                mock(AcDefinitionHandler.class), mock(ParticipantMessagePublisher.class), mock(CacheProvider.class));
        var propertyUpdateMsg = new PropertiesUpdate();
        participantHandler.handleAcPropertyUpdate(propertyUpdateMsg);
        verify(acHandler).handleAcPropertyUpdate(propertyUpdateMsg);
    }

    @Test
    void appliesToTest() {
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getParticipantId()).thenReturn(CommonTestData.getParticipantId());
        var participantHandler = new ParticipantHandler(mock(AutomationCompositionHandler.class),
                mock(AcLockHandler.class), mock(AcDefinitionHandler.class), mock(ParticipantMessagePublisher.class),
                cacheProvider);

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
        var participantHandler = new ParticipantHandler(mock(AutomationCompositionHandler.class),
                mock(AcLockHandler.class), mock(AcDefinitionHandler.class), publisher, cacheProvider);

        participantHandler.sendParticipantRegister();
        verify(publisher).sendParticipantRegister(any(ParticipantRegister.class));
    }

    @Test
    void handleParticipantRegisterAckTest() {
        var publisher = mock(ParticipantMessagePublisher.class);
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getParticipantId()).thenReturn(CommonTestData.getParticipantId());
        var participantHandler = new ParticipantHandler(mock(AutomationCompositionHandler.class),
                mock(AcLockHandler.class), mock(AcDefinitionHandler.class), publisher, cacheProvider);

        participantHandler.handleParticipantRegisterAck(new ParticipantRegisterAck());
        verify(publisher).sendParticipantStatus(any(ParticipantStatus.class));
    }

    @Test
    void sendParticipantDeregisterTest() {
        var publisher = mock(ParticipantMessagePublisher.class);
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getParticipantId()).thenReturn(CommonTestData.getParticipantId());
        var participantHandler = new ParticipantHandler(mock(AutomationCompositionHandler.class),
                mock(AcLockHandler.class), mock(AcDefinitionHandler.class), publisher, cacheProvider);

        participantHandler.sendParticipantDeregister();
        verify(publisher).sendParticipantDeregister(any(ParticipantDeregister.class));
    }

    @Test
    void handleParticipantDeregisterAckTest() {
        var participantHandler = new ParticipantHandler(mock(AutomationCompositionHandler.class),
                mock(AcLockHandler.class), mock(AcDefinitionHandler.class), mock(ParticipantMessagePublisher.class),
                mock(CacheProvider.class));
        var participantDeregisterAck = new ParticipantDeregisterAck();
        assertDoesNotThrow(() -> participantHandler.handleParticipantDeregisterAck(participantDeregisterAck));
    }

    @Test
    void handleParticipantPrimeTest() {
        var participantPrime = new ParticipantPrime();
        participantPrime.setCompositionId(UUID.randomUUID());
        participantPrime.setMessageId(UUID.randomUUID());

        var acHandler = mock(AcDefinitionHandler.class);
        var participantHandler = new ParticipantHandler(mock(AutomationCompositionHandler.class),
                mock(AcLockHandler.class), acHandler, mock(ParticipantMessagePublisher.class),
                mock(CacheProvider.class));

        participantHandler.handleParticipantPrime(participantPrime);
        verify(acHandler).handlePrime(participantPrime);
    }

    @Test
    void handleParticipantRestartTest() {
        var participantRestartMsg = new ParticipantRestart();
        participantRestartMsg.setState(AcTypeState.PRIMED);
        participantRestartMsg.setCompositionId(UUID.randomUUID());

        var cacheProvider = mock(CacheProvider.class);
        var publisher = mock(ParticipantMessagePublisher.class);
        var acHandler = mock(AcDefinitionHandler.class);
        var participantHandler = new ParticipantHandler(mock(AutomationCompositionHandler.class),
                mock(AcLockHandler.class), acHandler, publisher, cacheProvider);

        participantHandler.handleParticipantRestart(participantRestartMsg);
        verify(acHandler).handleParticipantRestart(participantRestartMsg);
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
        var participantHandler = new ParticipantHandler(mock(AutomationCompositionHandler.class),
                mock(AcLockHandler.class), acHandler, publisher, cacheProvider);
        participantHandler.sendHeartbeat();
        verify(publisher).sendParticipantRegister(any(ParticipantRegister.class));

        when(cacheProvider.isRegistered()).thenReturn(true);
        clearInvocations(publisher);
        participantHandler.sendHeartbeat();
        verify(publisher).sendParticipantStatus(any(ParticipantStatus.class));
    }
}
