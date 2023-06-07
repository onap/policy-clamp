/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation.
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.acm.participant.intermediary.main.parameters.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantSupportedElementType;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionDeploy;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionStateChange;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantAckMessage;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantDeregister;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantDeregisterAck;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessage;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantPrime;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantPrimeAck;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantRegister;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantRegisterAck;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantStatus;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantStatusReq;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.PropertiesUpdate;

class ParticipantHandlerTest {

    @Test
    void handleParticipantStatusReqTest() {
        var publisher = mock(ParticipantMessagePublisher.class);
        var cacheProvider = mock(CacheProvider.class);
        var participantHandler = new ParticipantHandler(mock(AutomationCompositionHandler.class),
                mock(AutomationCompositionOutHandler.class), publisher, cacheProvider);
        participantHandler.handleParticipantStatusReq(new ParticipantStatusReq());
        verify(publisher).sendParticipantStatus(any(ParticipantStatus.class));
    }

    @Test
    void handleAutomationCompositionDeployTest() {
        var acHandler = mock(AutomationCompositionHandler.class);
        var participantHandler = new ParticipantHandler(acHandler, mock(AutomationCompositionOutHandler.class),
                mock(ParticipantMessagePublisher.class), mock(CacheProvider.class));
        var automationCompositionDeploy = new AutomationCompositionDeploy();
        participantHandler.handleAutomationCompositionDeploy(automationCompositionDeploy);
        verify(acHandler).handleAutomationCompositionDeploy(automationCompositionDeploy);
    }

    @Test
    void handleAutomationCompositionStateChangeTest() {
        var acHandler = mock(AutomationCompositionHandler.class);
        var participantHandler = new ParticipantHandler(acHandler, mock(AutomationCompositionOutHandler.class),
                mock(ParticipantMessagePublisher.class), mock(CacheProvider.class));
        var acStateChange = new AutomationCompositionStateChange();
        participantHandler.handleAutomationCompositionStateChange(acStateChange);
        verify(acHandler).handleAutomationCompositionStateChange(acStateChange);
    }

    @Test
    void handleAcPropertyUpdateTest() {
        var acHandler = mock(AutomationCompositionHandler.class);
        var participantHandler = new ParticipantHandler(acHandler, mock(AutomationCompositionOutHandler.class),
                mock(ParticipantMessagePublisher.class), mock(CacheProvider.class));
        var propertyUpdateMsg = new PropertiesUpdate();
        participantHandler.handleAcPropertyUpdate(propertyUpdateMsg);
        verify(acHandler).handleAcPropertyUpdate(propertyUpdateMsg);
    }

    @Test
    void appliesToTest() {
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getParticipantId()).thenReturn(CommonTestData.getParticipantId());
        var participantHandler = new ParticipantHandler(mock(AutomationCompositionHandler.class),
                mock(AutomationCompositionOutHandler.class), mock(ParticipantMessagePublisher.class), cacheProvider);

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
                mock(AutomationCompositionOutHandler.class), publisher, cacheProvider);

        participantHandler.sendParticipantRegister();
        verify(publisher).sendParticipantRegister(any(ParticipantRegister.class));
    }

    @Test
    void handleParticipantRegisterAckTest() {
        var publisher = mock(ParticipantMessagePublisher.class);
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getParticipantId()).thenReturn(CommonTestData.getParticipantId());
        var participantHandler = new ParticipantHandler(mock(AutomationCompositionHandler.class),
                mock(AutomationCompositionOutHandler.class), publisher, cacheProvider);

        participantHandler.handleParticipantRegisterAck(new ParticipantRegisterAck());
        verify(publisher).sendParticipantStatus(any(ParticipantStatus.class));
    }

    @Test
    void sendParticipantDeregisterTest() {
        var publisher = mock(ParticipantMessagePublisher.class);
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getParticipantId()).thenReturn(CommonTestData.getParticipantId());
        var participantHandler = new ParticipantHandler(mock(AutomationCompositionHandler.class),
                mock(AutomationCompositionOutHandler.class), publisher, cacheProvider);

        participantHandler.sendParticipantDeregister();
        verify(publisher).sendParticipantDeregister(any(ParticipantDeregister.class));
    }

    @Test
    void handleParticipantDeregisterAckTest() {
        var participantHandler = new ParticipantHandler(mock(AutomationCompositionHandler.class),
                mock(AutomationCompositionOutHandler.class), mock(ParticipantMessagePublisher.class),
                mock(CacheProvider.class));
        var participantDeregisterAck = new ParticipantDeregisterAck();
        assertDoesNotThrow(() -> participantHandler.handleParticipantDeregisterAck(participantDeregisterAck));
    }

    @Test
    void handleParticipantPrimeTest() {
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getParticipantId()).thenReturn(CommonTestData.getParticipantId());
        var publisher = mock(ParticipantMessagePublisher.class);
        var participantHandler = new ParticipantHandler(mock(AutomationCompositionHandler.class),
                mock(AutomationCompositionOutHandler.class), publisher, cacheProvider);

        var participantPrime = new ParticipantPrime();
        participantPrime.setCompositionId(UUID.randomUUID());
        participantPrime.setParticipantDefinitionUpdates(List.of(createParticipantDefinition()));
        participantHandler.handleParticipantPrime(participantPrime);
        verify(cacheProvider).addElementDefinition(any(), any());
        verify(publisher).sendParticipantPrimeAck(any(ParticipantPrimeAck.class));
    }

    @Test
    void handleParticipantDeprimeTest() {
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getParticipantId()).thenReturn(CommonTestData.getParticipantId());
        var publisher = mock(ParticipantMessagePublisher.class);
        var participantHandler = new ParticipantHandler(mock(AutomationCompositionHandler.class),
                mock(AutomationCompositionOutHandler.class), publisher, cacheProvider);
        var participantPrime = new ParticipantPrime();
        var compositionId = UUID.randomUUID();
        participantPrime.setCompositionId(compositionId);
        participantHandler.handleParticipantPrime(participantPrime);
        verify(cacheProvider).removeElementDefinition(compositionId);
        verify(publisher).sendParticipantPrimeAck(any(ParticipantPrimeAck.class));
    }

    @Test
    void sendHeartbeatTest() {
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getParticipantId()).thenReturn(CommonTestData.getParticipantId());
        when(cacheProvider.getAutomationCompositions()).thenReturn(CommonTestData.getTestAutomationCompositionMap());
        var publisher = mock(ParticipantMessagePublisher.class);
        var participantHandler = new ParticipantHandler(mock(AutomationCompositionHandler.class),
                mock(AutomationCompositionOutHandler.class), publisher, cacheProvider);
        participantHandler.sendHeartbeat();
        verify(publisher).sendHeartbeat(any(ParticipantStatus.class));
    }

    private ParticipantDefinition createParticipantDefinition() {
        var def = new ParticipantDefinition();
        def.setParticipantId(CommonTestData.getParticipantId());
        return def;
    }
}
