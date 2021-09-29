/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.runtime.supervision;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopException;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.Participant;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantHealthStatus;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ControlLoopProvider;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ControlLoopAck;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantDeregister;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantRegister;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantStatus;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantUpdateAck;
import org.onap.policy.clamp.controlloop.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.controlloop.runtime.monitoring.MonitoringProvider;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ControlLoopStateChangePublisher;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ControlLoopUpdatePublisher;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ParticipantDeregisterAckPublisher;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ParticipantRegisterAckPublisher;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ParticipantUpdatePublisher;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class SupervisionHandlerTest {
    private static final String CL_INSTANTIATION_CREATE_JSON = "src/test/resources/rest/controlloops/ControlLoops.json";
    private static final ToscaConceptIdentifier identifier = new ToscaConceptIdentifier("PMSHInstance0Crud", "1.0.1");
    private static final ToscaConceptIdentifier participantId = new ToscaConceptIdentifier("ParticipantId", "1.0.0");
    private static final ToscaConceptIdentifier participantType =
            new ToscaConceptIdentifier("ParticipantType", "1.0.0");

    @Test
    void testTriggerControlLoopSupervisionEmpty() throws ControlLoopException, PfModelException, CoderException {
        var handler = createSupervisionHandler(mock(ControlLoopProvider.class), mock(ParticipantProvider.class),
                mock(MonitoringProvider.class), mock(ParticipantRegisterAckPublisher.class),
                mock(ParticipantDeregisterAckPublisher.class), mock(ControlLoopUpdatePublisher.class),
                mock(ParticipantUpdatePublisher.class));

        assertThatThrownBy(() -> handler.triggerControlLoopSupervision(List.of()))
                .hasMessageMatching("The list of control loops for supervision is empty");
    }

    @Test
    void testTriggerControlLoopSupervision() throws ControlLoopException, PfModelException, CoderException {
        var controlLoopProvider = mock(ControlLoopProvider.class);
        var controlLoopUpdatePublisher = mock(ControlLoopUpdatePublisher.class);
        var handler = createSupervisionHandler(controlLoopProvider, mock(ParticipantProvider.class),
                mock(MonitoringProvider.class), mock(ParticipantRegisterAckPublisher.class),
                mock(ParticipantDeregisterAckPublisher.class), controlLoopUpdatePublisher,
                mock(ParticipantUpdatePublisher.class));

        handler.triggerControlLoopSupervision(List.of(identifier));

        verify(controlLoopUpdatePublisher).send(any(ControlLoop.class));
        verify(controlLoopProvider).updateControlLoop(any(ControlLoop.class));
    }

    @Test
    void testHandleControlLoopStateChangeAckMessage() throws PfModelException, CoderException {
        var controlLoopProvider = mock(ControlLoopProvider.class);
        var handler = createSupervisionHandler(controlLoopProvider, mock(ParticipantProvider.class),
                mock(MonitoringProvider.class), mock(ParticipantRegisterAckPublisher.class),
                mock(ParticipantDeregisterAckPublisher.class), mock(ControlLoopUpdatePublisher.class),
                mock(ParticipantUpdatePublisher.class));
        var controlLoopAckMessage = new ControlLoopAck(ParticipantMessageType.CONTROLLOOP_STATECHANGE_ACK);
        controlLoopAckMessage.setControlLoopResultMap(Map.of());
        controlLoopAckMessage.setControlLoopId(identifier);

        handler.handleControlLoopStateChangeAckMessage(controlLoopAckMessage);

        verify(controlLoopProvider).updateControlLoop(any(ControlLoop.class));
    }

    @Test
    void testHandleControlLoopUpdateAckMessage() throws PfModelException, CoderException {
        var controlLoopAckMessage = new ControlLoopAck(ParticipantMessageType.CONTROLLOOP_UPDATE_ACK);
        controlLoopAckMessage.setParticipantId(participantId);
        controlLoopAckMessage.setParticipantType(participantType);
        controlLoopAckMessage.setControlLoopResultMap(Map.of());
        controlLoopAckMessage.setControlLoopId(identifier);
        var controlLoopProvider = mock(ControlLoopProvider.class);
        var handler = createSupervisionHandler(controlLoopProvider, mock(ParticipantProvider.class),
                mock(MonitoringProvider.class), mock(ParticipantRegisterAckPublisher.class),
                mock(ParticipantDeregisterAckPublisher.class), mock(ControlLoopUpdatePublisher.class),
                mock(ParticipantUpdatePublisher.class));

        handler.handleControlLoopUpdateAckMessage(controlLoopAckMessage);

        verify(controlLoopProvider).updateControlLoop(any(ControlLoop.class));
    }

    @Test
    void testHandleParticipantDeregister() throws PfModelException, CoderException {
        var participant = new Participant();
        participant.setName(participantId.getName());
        participant.setVersion(participantId.getVersion());
        participant.setParticipantType(participantType);

        var participantProvider = mock(ParticipantProvider.class);
        when(participantProvider.getParticipants(eq(participantId.getName()), eq(participantId.getVersion())))
                .thenReturn(List.of(participant));

        var participantDeregisterMessage = new ParticipantDeregister();
        participantDeregisterMessage.setMessageId(UUID.randomUUID());
        participantDeregisterMessage.setParticipantId(participantId);
        participantDeregisterMessage.setParticipantType(participantType);
        var participantDeregisterAckPublisher = mock(ParticipantDeregisterAckPublisher.class);
        var handler = createSupervisionHandler(mock(ControlLoopProvider.class), participantProvider,
                mock(MonitoringProvider.class), mock(ParticipantRegisterAckPublisher.class),
                participantDeregisterAckPublisher, mock(ControlLoopUpdatePublisher.class),
                mock(ParticipantUpdatePublisher.class));

        handler.handleParticipantMessage(participantDeregisterMessage);

        verify(participantProvider).updateParticipants(anyList());
        verify(participantDeregisterAckPublisher).send(eq(participantDeregisterMessage.getMessageId()));
    }

    @Test
    void testHandleParticipantRegister() throws PfModelException, CoderException {
        var participant = new Participant();
        participant.setName(participantId.getName());
        participant.setVersion(participantId.getVersion());
        participant.setParticipantType(participantType);

        var participantRegisterMessage = new ParticipantRegister();
        participantRegisterMessage.setMessageId(UUID.randomUUID());
        participantRegisterMessage.setParticipantId(participantId);
        participantRegisterMessage.setParticipantType(participantType);
        var participantProvider = mock(ParticipantProvider.class);
        var participantRegisterAckPublisher = mock(ParticipantRegisterAckPublisher.class);
        var handler = createSupervisionHandler(mock(ControlLoopProvider.class), participantProvider,
                mock(MonitoringProvider.class), participantRegisterAckPublisher,
                mock(ParticipantDeregisterAckPublisher.class), mock(ControlLoopUpdatePublisher.class),
                mock(ParticipantUpdatePublisher.class));

        handler.handleParticipantMessage(participantRegisterMessage);

        verify(participantProvider).createParticipants(anyList());
        verify(participantRegisterAckPublisher).send(eq(participantRegisterMessage.getMessageId()), eq(participantId),
                eq(participantType));
    }

    @Test
    void testParticipantUpdateAck() throws PfModelException, CoderException {
        var participant = new Participant();
        participant.setName(participantId.getName());
        participant.setVersion(participantId.getVersion());
        participant.setParticipantType(participantType);

        var participantProvider = mock(ParticipantProvider.class);
        when(participantProvider.getParticipants(eq(participantId.getName()), eq(participantId.getVersion())))
                .thenReturn(List.of(participant));

        var participantUpdateAckMessage = new ParticipantUpdateAck();
        participantUpdateAckMessage.setParticipantId(participantId);
        participantUpdateAckMessage.setParticipantType(participantType);
        participantUpdateAckMessage.setState(ParticipantState.PASSIVE);
        var handler = createSupervisionHandler(mock(ControlLoopProvider.class), participantProvider,
                mock(MonitoringProvider.class), mock(ParticipantRegisterAckPublisher.class),
                mock(ParticipantDeregisterAckPublisher.class), mock(ControlLoopUpdatePublisher.class),
                mock(ParticipantUpdatePublisher.class));

        handler.handleParticipantMessage(participantUpdateAckMessage);

        verify(participantProvider).updateParticipants(anyList());
    }

    @Test
    void testHandleParticipantStatus() throws PfModelException, CoderException {
        var participantStatusMessage = new ParticipantStatus();
        participantStatusMessage.setParticipantId(participantId);
        participantStatusMessage.setParticipantType(participantType);
        participantStatusMessage.setState(ParticipantState.PASSIVE);
        participantStatusMessage.setHealthStatus(ParticipantHealthStatus.HEALTHY);
        participantStatusMessage.setParticipantStatistics(new ParticipantStatistics());

        var participantProvider = mock(ParticipantProvider.class);
        var monitoringProvider = mock(MonitoringProvider.class);
        var handler = createSupervisionHandler(mock(ControlLoopProvider.class), participantProvider, monitoringProvider,
                mock(ParticipantRegisterAckPublisher.class), mock(ParticipantDeregisterAckPublisher.class),
                mock(ControlLoopUpdatePublisher.class), mock(ParticipantUpdatePublisher.class));
        handler.handleParticipantMessage(participantStatusMessage);

        verify(participantProvider).createParticipants(anyList());
        verify(monitoringProvider).createParticipantStatistics(anyList());
    }

    @Test
    void testHandleSendCommissionMessage() throws PfModelException, CoderException {
        var participantUpdatePublisher = mock(ParticipantUpdatePublisher.class);
        var handler = createSupervisionHandler(mock(ControlLoopProvider.class), mock(ParticipantProvider.class),
                mock(MonitoringProvider.class), mock(ParticipantRegisterAckPublisher.class),
                mock(ParticipantDeregisterAckPublisher.class), mock(ControlLoopUpdatePublisher.class),
                participantUpdatePublisher);
        handler.handleSendCommissionMessage(Map.of());

        verify(participantUpdatePublisher).send(anyMap(), eq(true));
    }

    @Test
    void testHandleSendDeCommissionMessage() throws PfModelException, CoderException {
        var participantUpdatePublisher = mock(ParticipantUpdatePublisher.class);
        var handler = createSupervisionHandler(mock(ControlLoopProvider.class), mock(ParticipantProvider.class),
                mock(MonitoringProvider.class), mock(ParticipantRegisterAckPublisher.class),
                mock(ParticipantDeregisterAckPublisher.class), mock(ControlLoopUpdatePublisher.class),
                participantUpdatePublisher);
        handler.handleSendDeCommissionMessage();

        verify(participantUpdatePublisher).send(anyMap(), eq(false));
    }

    private SupervisionHandler createSupervisionHandler(ControlLoopProvider controlLoopProvider,
            ParticipantProvider participantProvider, MonitoringProvider monitoringProvider,
            ParticipantRegisterAckPublisher participantRegisterAckPublisher,
            ParticipantDeregisterAckPublisher participantDeregisterAckPublisher,
            ControlLoopUpdatePublisher controlLoopUpdatePublisher,
            ParticipantUpdatePublisher participantUpdatePublisher) throws PfModelException, CoderException {
        var controlLoopsCreate = InstantiationUtils.getControlLoopsFromResource(CL_INSTANTIATION_CREATE_JSON, "Crud");

        var controlLoop = controlLoopsCreate.getControlLoopList().get(0);
        controlLoop.setOrderedState(ControlLoopOrderedState.PASSIVE);

        var controlLoopStateChangePublisher = mock(ControlLoopStateChangePublisher.class);

        when(controlLoopProvider.getControlLoop(eq(identifier))).thenReturn(controlLoop);

        return new SupervisionHandler(controlLoopProvider, participantProvider, monitoringProvider,
                controlLoopUpdatePublisher, controlLoopStateChangePublisher, participantRegisterAckPublisher,
                participantDeregisterAckPublisher, participantUpdatePublisher);

    }
}
