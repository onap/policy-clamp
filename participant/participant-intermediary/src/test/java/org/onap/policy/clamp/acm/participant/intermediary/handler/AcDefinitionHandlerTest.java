/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2026 OpenInfra Foundation Europe. All rights reserved.
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.acm.participant.intermediary.handler.cache.CacheProvider;
import org.onap.policy.clamp.acm.participant.intermediary.main.parameters.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDefinition;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.dto.CompositionDto;
import org.onap.policy.clamp.models.acm.dto.ParticipantPrimeDto;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrime;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrimeAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantSync;
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.PrimeOrder;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class AcDefinitionHandlerTest {

    @Test
    void handleCompositionPrimeTest() {
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getParticipantId()).thenReturn(CommonTestData.getParticipantId());
        var compositionId = UUID.randomUUID();
        var participantPrimeMsg = new ParticipantPrime();
        participantPrimeMsg.setCompositionId(compositionId);
        participantPrimeMsg.setRevisionIdComposition(UUID.randomUUID());
        participantPrimeMsg.setPrimeOrder(PrimeOrder.PRIME);
        participantPrimeMsg.setPrimeDtoList(List.of(createParticipantPrimeDto(compositionId)));
        var listener = mock(ThreadHandler.class);
        var ach = new AcDefinitionHandler(cacheProvider, mock(ParticipantMessagePublisher.class), listener);
        ach.handlePrime(participantPrimeMsg);
        verify(cacheProvider).addCompositionDto(any(CompositionDto.class));
        verify(listener).prime(any(UUID.class), any(CompositionDto.class));
    }

    private ParticipantDefinition createParticipantDefinition() {
        var def = new ParticipantDefinition();
        def.setParticipantId(CommonTestData.getParticipantId());
        def.setAutomationCompositionElementDefinitionList(
                List.of(CommonTestData.createAutomationCompositionElementDefinition(
                        new ToscaConceptIdentifier("key", "1.0.0"))));
        return def;
    }

    private ParticipantPrimeDto createParticipantPrimeDto(UUID compositionId) {
        var elementId = new ToscaConceptIdentifier("key", "1.0.0");
        var compositionDto = new CompositionDto(compositionId,
                Map.of(elementId, Map.of("startPhase", 0)),
                Map.of(elementId, Map.of()));
        var primeDto = new ParticipantPrimeDto();
        primeDto.setParticipantId(CommonTestData.getParticipantId());
        primeDto.setCompositionDto(compositionDto);
        return primeDto;
    }

    @Test
    void handleCompositionDeprimeTest() {
        var compositionId = UUID.randomUUID();
        var elementId = new ToscaConceptIdentifier("key", "1.0.0");
        var compositionDto = new CompositionDto(compositionId,
                Map.of(elementId, Map.of()), Map.of(elementId, Map.of()));

        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getParticipantId()).thenReturn(CommonTestData.getParticipantId());
        when(cacheProvider.getCompositionDtos()).thenReturn(Map.of(compositionId, compositionDto));
        var participantPrimeMsg = new ParticipantPrime();
        participantPrimeMsg.setCompositionId(compositionId);
        participantPrimeMsg.setPrimeOrder(PrimeOrder.DEPRIME);
        participantPrimeMsg.setPrimeDtoList(List.of(createParticipantPrimeDto(compositionId)));
        var listener = mock(ThreadHandler.class);
        var ach = new AcDefinitionHandler(cacheProvider, mock(ParticipantMessagePublisher.class), listener);
        ach.handlePrime(participantPrimeMsg);
        verify(listener).deprime(any(UUID.class), any(CompositionDto.class));
    }

    @Test
    void handleCompositionAlreadyDeprimedTest() {
        var compositionId = UUID.randomUUID();
        var participantMessagePublisher = mock(ParticipantMessagePublisher.class);
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getParticipantId()).thenReturn(CommonTestData.getParticipantId());
        var ach = new AcDefinitionHandler(cacheProvider, participantMessagePublisher,
                mock(ThreadHandler.class));
        var participantPrimeMsg = new ParticipantPrime();
        participantPrimeMsg.setCompositionId(compositionId);
        participantPrimeMsg.setPrimeOrder(PrimeOrder.DEPRIME);
        ach.handlePrime(participantPrimeMsg);
        verify(participantMessagePublisher).sendParticipantPrimeAck(any(ParticipantPrimeAck.class));
    }

    @Test
    void syncTest() {
        var participantSyncMsg = new ParticipantSync();
        participantSyncMsg.setState(AcTypeState.PRIMED);
        participantSyncMsg.setCompositionId(UUID.randomUUID());
        participantSyncMsg.getParticipantDefinitionUpdates().add(createParticipantDefinition());
        participantSyncMsg.setAutomationcompositionList(List.of(CommonTestData.createParticipantRestartAc()));
        participantSyncMsg.setRevisionIdComposition(UUID.randomUUID());
        participantSyncMsg.setRevisionIdInstance(UUID.randomUUID());

        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getParticipantId()).thenReturn(CommonTestData.getParticipantId());
        var listener = mock(ThreadHandler.class);
        var ach = new AcDefinitionHandler(cacheProvider, mock(ParticipantMessagePublisher.class), listener);
        ach.handleParticipantSync(participantSyncMsg);
        verify(cacheProvider).initializeAutomationComposition(any(UUID.class), any());
        verify(cacheProvider).addCompositionDto(any(UUID.class), any());
    }

    @Test
    void syncCompositionDefinitionTimeout() {
        var participantSyncMsg = new ParticipantSync();
        participantSyncMsg.setState(AcTypeState.PRIMED);
        participantSyncMsg.setStateChangeResult(StateChangeResult.TIMEOUT);
        participantSyncMsg.setCompositionId(UUID.randomUUID());
        participantSyncMsg.getParticipantDefinitionUpdates().add(createParticipantDefinition());
        participantSyncMsg.setRevisionIdComposition(UUID.randomUUID());
        participantSyncMsg.setRevisionIdInstance(UUID.randomUUID());
        var participantRestartAc = CommonTestData.createParticipantRestartAc();
        participantRestartAc.setStateChangeResult(StateChangeResult.TIMEOUT);
        participantSyncMsg.setAutomationcompositionList(List.of(participantRestartAc));

        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getParticipantId()).thenReturn(CommonTestData.getParticipantId());
        var listener = mock(ThreadHandler.class);
        var ach = new AcDefinitionHandler(cacheProvider, mock(ParticipantMessagePublisher.class), listener);
        ach.handleParticipantSync(participantSyncMsg);
        verify(cacheProvider).initializeAutomationComposition(any(UUID.class), any());
        verify(cacheProvider).addCompositionDto(any(UUID.class), any());
        verify(listener).cleanExecution(participantSyncMsg.getCompositionId(), participantSyncMsg.getMessageId());
        var elementId = participantRestartAc.getAcElementList().getFirst().getId();
        verify(listener).cleanExecution(elementId, participantSyncMsg.getMessageId());
    }

    @Test
    void syncDeleteTest() {
        var participantSyncMsg = new ParticipantSync();
        participantSyncMsg.setState(AcTypeState.COMMISSIONED);
        participantSyncMsg.setDelete(true);
        participantSyncMsg.setCompositionId(UUID.randomUUID());
        participantSyncMsg.getParticipantDefinitionUpdates().add(createParticipantDefinition());
        var restartAc = CommonTestData.createParticipantRestartAc();
        participantSyncMsg.setAutomationcompositionList(List.of(restartAc));

        var cacheProvider = mock(CacheProvider.class);
        var listener = mock(ThreadHandler.class);
        var ach = new AcDefinitionHandler(cacheProvider, mock(ParticipantMessagePublisher.class), listener);
        ach.handleParticipantSync(participantSyncMsg);
        verify(cacheProvider).removeCompositionDto(participantSyncMsg.getCompositionId());
        verify(cacheProvider).removeAutomationComposition(restartAc.getAutomationCompositionId());
    }
}
