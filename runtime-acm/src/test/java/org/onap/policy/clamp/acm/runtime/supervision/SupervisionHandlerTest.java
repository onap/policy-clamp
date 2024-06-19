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

package org.onap.policy.clamp.acm.runtime.supervision;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.onap.policy.clamp.acm.runtime.util.CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantSyncPublisher;
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrimeAck;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;

class SupervisionHandlerTest {

    @Test
    void testParticipantPrimeAckNotFound() {
        var participantPrimeAckMessage = new ParticipantPrimeAck();
        participantPrimeAckMessage.setParticipantId(CommonTestData.getParticipantId());
        participantPrimeAckMessage.setState(ParticipantState.ON_LINE);
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var handler = new SupervisionHandler(acDefinitionProvider, mock(ParticipantSyncPublisher.class));

        handler.handleParticipantMessage(participantPrimeAckMessage);
        verify(acDefinitionProvider).findAcDefinition(any());
    }

    @Test
    void testParticipantPrimeAckPrimed() {
        var participantPrimeAckMessage = new ParticipantPrimeAck();
        participantPrimeAckMessage.setParticipantId(CommonTestData.getParticipantId());
        participantPrimeAckMessage.setState(ParticipantState.ON_LINE);

        var acDefinition = CommonTestData.createAcDefinition(
                InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML), AcTypeState.PRIMED);
        participantPrimeAckMessage.setCompositionId(acDefinition.getCompositionId());

        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        when(acDefinitionProvider.findAcDefinition(acDefinition.getCompositionId()))
                .thenReturn(Optional.of(acDefinition));
        var handler = new SupervisionHandler(acDefinitionProvider, mock(ParticipantSyncPublisher.class));

        handler.handleParticipantMessage(participantPrimeAckMessage);
        verify(acDefinitionProvider).findAcDefinition(any());
    }

    @Test
    void testParticipantPrimeAck() {
        var participantPrimeAckMessage = new ParticipantPrimeAck();
        participantPrimeAckMessage.setParticipantId(CommonTestData.getParticipantId());
        participantPrimeAckMessage.setCompositionState(AcTypeState.PRIMED);
        participantPrimeAckMessage.setState(ParticipantState.ON_LINE);

        var acDefinition = CommonTestData.createAcDefinition(
                InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML), AcTypeState.PRIMING);
        acDefinition.setStateChangeResult(StateChangeResult.NO_ERROR);
        participantPrimeAckMessage.setCompositionId(acDefinition.getCompositionId());
        for (var element : acDefinition.getElementStateMap().values()) {
            element.setParticipantId(CommonTestData.getParticipantId());
        }

        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        when(acDefinitionProvider.findAcDefinition(acDefinition.getCompositionId()))
                .thenReturn(Optional.of(acDefinition));

        var handler = new SupervisionHandler(acDefinitionProvider, mock(ParticipantSyncPublisher.class));

        handler.handleParticipantMessage(participantPrimeAckMessage);
        verify(acDefinitionProvider).findAcDefinition(any());
        verify(acDefinitionProvider, times(acDefinition.getElementStateMap().size()))
            .updateAcDefinitionElement(any(), any());
        verify(acDefinitionProvider).updateAcDefinitionState(acDefinition.getCompositionId(), AcTypeState.PRIMED,
            StateChangeResult.NO_ERROR, null);
    }

    @Test
    void testParticipantPrimeAckFailed() {
        var participantPrimeAckMessage = new ParticipantPrimeAck();
        participantPrimeAckMessage.setParticipantId(CommonTestData.getParticipantId());
        participantPrimeAckMessage.setState(ParticipantState.ON_LINE);
        participantPrimeAckMessage.setStateChangeResult(StateChangeResult.FAILED);

        var acDefinition = CommonTestData.createAcDefinition(
                InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML), AcTypeState.PRIMING);
        participantPrimeAckMessage.setCompositionId(acDefinition.getCompositionId());
        acDefinition.getElementStateMap().values().iterator().next()
                .setParticipantId(CommonTestData.getParticipantId());

        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        when(acDefinitionProvider.findAcDefinition(acDefinition.getCompositionId()))
                .thenReturn(Optional.of(acDefinition));

        var handler = new SupervisionHandler(acDefinitionProvider, mock(ParticipantSyncPublisher.class));

        handler.handleParticipantMessage(participantPrimeAckMessage);
        verify(acDefinitionProvider).findAcDefinition(any());
        verify(acDefinitionProvider).updateAcDefinitionElement(any(), any());
        verify(acDefinitionProvider).updateAcDefinitionState(acDefinition.getCompositionId(), AcTypeState.PRIMING,
            StateChangeResult.FAILED, null);
    }

    @Test
    void testParticipantPrimeAckRestarted() {
        var participantPrimeAckMessage = new ParticipantPrimeAck();
        participantPrimeAckMessage.setParticipantId(CommonTestData.getParticipantId());
        participantPrimeAckMessage.setCompositionState(AcTypeState.PRIMED);
        participantPrimeAckMessage.setState(ParticipantState.ON_LINE);

        var acDefinition = CommonTestData.createAcDefinition(
            InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML), AcTypeState.PRIMED);
        acDefinition.setStateChangeResult(StateChangeResult.TIMEOUT);
        acDefinition.setRestarting(true);
        participantPrimeAckMessage.setCompositionId(acDefinition.getCompositionId());
        for (var element : acDefinition.getElementStateMap().values()) {
            element.setParticipantId(CommonTestData.getParticipantId());
            element.setRestarting(true);
        }

        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        when(acDefinitionProvider.findAcDefinition(acDefinition.getCompositionId()))
            .thenReturn(Optional.of(acDefinition));

        var handler = new SupervisionHandler(acDefinitionProvider, mock(ParticipantSyncPublisher.class));

        handler.handleParticipantMessage(participantPrimeAckMessage);
        verify(acDefinitionProvider).findAcDefinition(any());
        verify(acDefinitionProvider, times(acDefinition.getElementStateMap().size()))
            .updateAcDefinitionElement(any(), any());
        verify(acDefinitionProvider).updateAcDefinitionState(acDefinition.getCompositionId(), AcTypeState.PRIMED,
            StateChangeResult.NO_ERROR, null);
    }
}
