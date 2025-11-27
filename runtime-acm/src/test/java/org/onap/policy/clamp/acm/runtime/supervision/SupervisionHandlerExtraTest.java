
/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.policy.clamp.acm.runtime.supervision;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrimeAck;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.MessageProvider;

/**
 * Extra tests to exercise additional branches in SupervisionHandler.
 */
class SupervisionHandlerExtraTest {

    private AcDefinitionProvider acDefinitionProvider;
    private MessageProvider messageProvider;
    private SupervisionHandler handler;

    @BeforeEach
    void setUp() {
        acDefinitionProvider = mock(AcDefinitionProvider.class);
        messageProvider = mock(MessageProvider.class);
        handler = new SupervisionHandler(acDefinitionProvider, messageProvider);
    }

    @Test
    void whenMessageHasNullFields_thenReturnsEarly() {
        ParticipantPrimeAck msg = new ParticipantPrimeAck();
        // all null -> should return without interactions
        handler.handleParticipantMessage(msg);

        verifyNoInteractions(acDefinitionProvider, messageProvider);
    }

    @Test
    void whenCompositionStateInvalid_thenReturnsEarly() {
        ParticipantPrimeAck msg = new ParticipantPrimeAck();
        msg.setCompositionId(UUID.randomUUID());
        msg.setParticipantId(UUID.randomUUID());
        // set compositionState to PRIMING which should cause early return
        msg.setCompositionState(AcTypeState.PRIMING);
        msg.setStateChangeResult(StateChangeResult.NO_ERROR);

        handler.handleParticipantMessage(msg);

        verifyNoInteractions(acDefinitionProvider, messageProvider);
    }

    @Test
    void whenStateChangeResultInvalid_thenReturnsEarly() {
        ParticipantPrimeAck msg = new ParticipantPrimeAck();
        msg.setCompositionId(UUID.randomUUID());
        msg.setParticipantId(UUID.randomUUID());
        msg.setCompositionState(AcTypeState.PRIMED);
        // set an unexpected state change result (use null-equivalent by creating a different enum via valueOf)
        msg.setStateChangeResult(StateChangeResult.TIMEOUT);

        handler.handleParticipantMessage(msg);

        verifyNoInteractions(acDefinitionProvider, messageProvider);
    }

    @Test
    void whenAcDefinitionNotFound_thenReturnsEarly() {
        ParticipantPrimeAck msg = new ParticipantPrimeAck();
        UUID compId = UUID.randomUUID();
        msg.setCompositionId(compId);
        msg.setParticipantId(UUID.randomUUID());
        msg.setCompositionState(AcTypeState.PRIMED);
        msg.setStateChangeResult(StateChangeResult.NO_ERROR);

        when(acDefinitionProvider.findAcDefinition(compId)).thenReturn(Optional.empty());

        handler.handleParticipantMessage(msg);

        verify(acDefinitionProvider).findAcDefinition(compId);
        verifyNoMoreInteractions(messageProvider);
    }

    @Test
    void whenAcDefinitionStateMismatches_thenReturnsEarly() {
        ParticipantPrimeAck msg = new ParticipantPrimeAck();
        UUID compId = UUID.randomUUID();
        msg.setCompositionId(compId);
        msg.setParticipantId(UUID.randomUUID());
        msg.setCompositionState(AcTypeState.PRIMED);
        msg.setStateChangeResult(StateChangeResult.NO_ERROR);

        AutomationCompositionDefinition def = new AutomationCompositionDefinition();
        def.setState(AcTypeState.COMMISSIONED);

        when(acDefinitionProvider.findAcDefinition(compId)).thenReturn(Optional.of(def));

        handler.handleParticipantMessage(msg);

        verify(acDefinitionProvider).findAcDefinition(compId);
        verifyNoInteractions(messageProvider);
    }

    @Test
    void whenAllOk_thenMessageSaved() {
        ParticipantPrimeAck msg = new ParticipantPrimeAck();
        UUID compId = UUID.randomUUID();
        msg.setCompositionId(compId);
        msg.setParticipantId(UUID.randomUUID());
        msg.setCompositionState(AcTypeState.PRIMED);
        msg.setStateChangeResult(StateChangeResult.NO_ERROR);

        AutomationCompositionDefinition def = new AutomationCompositionDefinition();
        def.setState(AcTypeState.PRIMING);

        when(acDefinitionProvider.findAcDefinition(compId)).thenReturn(Optional.of(def));

        handler.handleParticipantMessage(msg);

        verify(acDefinitionProvider).findAcDefinition(compId);
        verify(messageProvider).save(msg);
    }
}
