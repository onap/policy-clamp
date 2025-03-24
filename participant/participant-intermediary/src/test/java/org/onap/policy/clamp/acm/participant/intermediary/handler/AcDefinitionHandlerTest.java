/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation.
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionDto;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.acm.participant.intermediary.main.parameters.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDefinition;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrime;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrimeAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantSync;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class AcDefinitionHandlerTest {

    @Test
    void handleCompositionPrimeTest() {
        var listener = mock(ThreadHandler.class);
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getParticipantId()).thenReturn(CommonTestData.getParticipantId());
        var ach = new AcDefinitionHandler(cacheProvider, mock(ParticipantMessagePublisher.class), listener);
        var participantPrimeMsg = new ParticipantPrime();
        participantPrimeMsg.setCompositionId(UUID.randomUUID());
        participantPrimeMsg.setParticipantDefinitionUpdates(List.of(createParticipantDefinition()));
        ach.handlePrime(participantPrimeMsg);
        verify(cacheProvider).addElementDefinition(any(UUID.class), anyList());
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

    @Test
    void handleCompositionDeprimeTest() {
        var acElementDefinition = CommonTestData.createAutomationCompositionElementDefinition(
                new ToscaConceptIdentifier("key", "1.0.0"));
        var compositionId = UUID.randomUUID();
        var listener = mock(ThreadHandler.class);
        var cacheProvider = mock(CacheProvider.class);
        var ach = new AcDefinitionHandler(cacheProvider, mock(ParticipantMessagePublisher.class), listener);
        when(cacheProvider.getAcElementsDefinitions())
                .thenReturn(Map.of(compositionId, Map.of(new ToscaConceptIdentifier(), acElementDefinition)));
        var participantPrimeMsg = new ParticipantPrime();
        participantPrimeMsg.setCompositionId(compositionId);
        ach.handlePrime(participantPrimeMsg);
        verify(listener).deprime(any(UUID.class), any(CompositionDto.class));
    }

    @Test
    void handleCompositionAlreadyDeprimedTest() {
        var compositionId = UUID.randomUUID();
        var participantMessagePublisher =  mock(ParticipantMessagePublisher.class);
        var ach = new AcDefinitionHandler(mock(CacheProvider.class), participantMessagePublisher,
                mock(ThreadHandler.class));
        var participantPrimeMsg = new ParticipantPrime();
        participantPrimeMsg.setCompositionId(compositionId);
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

        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getParticipantId()).thenReturn(CommonTestData.getParticipantId());
        var listener = mock(ThreadHandler.class);
        var ach = new AcDefinitionHandler(cacheProvider, mock(ParticipantMessagePublisher.class), listener);
        ach.handleParticipantSync(participantSyncMsg);
        verify(cacheProvider).initializeAutomationComposition(any(UUID.class), any());
        verify(cacheProvider).addElementDefinition(any(), any());
    }

    @Test
    void syncCompositionDefinitionTimeout() {
        var participantSyncMsg = new ParticipantSync();
        participantSyncMsg.setState(AcTypeState.PRIMED);
        participantSyncMsg.setStateChangeResult(StateChangeResult.TIMEOUT);
        participantSyncMsg.setCompositionId(UUID.randomUUID());
        participantSyncMsg.getParticipantDefinitionUpdates().add(createParticipantDefinition());
        var participantRestartAc = CommonTestData.createParticipantRestartAc();
        participantRestartAc.setStateChangeResult(StateChangeResult.TIMEOUT);
        participantSyncMsg.setAutomationcompositionList(List.of(participantRestartAc));

        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getParticipantId()).thenReturn(CommonTestData.getParticipantId());
        var listener = mock(ThreadHandler.class);
        var ach = new AcDefinitionHandler(cacheProvider, mock(ParticipantMessagePublisher.class), listener);
        ach.handleParticipantSync(participantSyncMsg);
        verify(cacheProvider).initializeAutomationComposition(any(UUID.class), any());
        verify(cacheProvider).addElementDefinition(any(), any());
        verify(listener).cleanExecution(participantSyncMsg.getCompositionId(), participantSyncMsg.getMessageId());
        var elementId = participantRestartAc.getAcElementList().get(0).getId();
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
        verify(cacheProvider).removeElementDefinition(participantSyncMsg.getCompositionId());
        verify(cacheProvider).removeAutomationComposition(restartAc.getAutomationCompositionId());
    }
}
