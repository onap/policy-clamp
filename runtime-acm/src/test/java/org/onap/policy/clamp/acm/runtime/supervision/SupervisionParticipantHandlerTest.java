/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantDeregisterAckPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantRegisterAckPublisher;
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantDeregister;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantRegister;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantStatus;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class SupervisionParticipantHandlerTest {
    private static final ToscaConceptIdentifier PARTICIPANT_TYPE =
        new ToscaConceptIdentifier("ParticipantType", "1.0.0");

    @Test
    void testHandleParticipantDeregister() {
        var participant = CommonTestData.createParticipant(PARTICIPANT_TYPE, CommonTestData.getParticipantId());

        var participantProvider = mock(ParticipantProvider.class);
        when(participantProvider.findParticipant(CommonTestData.getParticipantId()))
                .thenReturn(Optional.of(participant));

        var participantDeregisterMessage = new ParticipantDeregister();
        participantDeregisterMessage.setMessageId(UUID.randomUUID());
        participantDeregisterMessage.setParticipantId(CommonTestData.getParticipantId());
        participantDeregisterMessage.setParticipantType(PARTICIPANT_TYPE);
        var participantDeregisterAckPublisher = mock(ParticipantDeregisterAckPublisher.class);
        var handler = new SupervisionParticipantHandler(participantProvider,
            mock(ParticipantRegisterAckPublisher.class), participantDeregisterAckPublisher);

        handler.handleParticipantMessage(participantDeregisterMessage);

        verify(participantProvider).updateParticipant(any());
        verify(participantDeregisterAckPublisher).send(participantDeregisterMessage.getMessageId());
    }

    @Test
    void testHandleParticipantRegister() {
        var participantRegisterMessage = new ParticipantRegister();
        participantRegisterMessage.setMessageId(UUID.randomUUID());
        participantRegisterMessage.setParticipantId(CommonTestData.getParticipantId());
        participantRegisterMessage.setParticipantType(PARTICIPANT_TYPE);
        var participantProvider = mock(ParticipantProvider.class);
        var participantRegisterAckPublisher = mock(ParticipantRegisterAckPublisher.class);
        var handler = new SupervisionParticipantHandler(participantProvider, participantRegisterAckPublisher,
            mock(ParticipantDeregisterAckPublisher.class));

        handler.handleParticipantMessage(participantRegisterMessage);

        verify(participantProvider).saveParticipant(any());
        verify(participantRegisterAckPublisher).send(participantRegisterMessage.getMessageId(),
                CommonTestData.getParticipantId(), PARTICIPANT_TYPE);
    }

    @Test
    void testHandleParticipantStatus() {
        var participantStatusMessage = new ParticipantStatus();
        participantStatusMessage.setParticipantId(CommonTestData.getParticipantId());
        participantStatusMessage.setParticipantType(PARTICIPANT_TYPE);
        participantStatusMessage.setState(ParticipantState.ON_LINE);

        var participantProvider = mock(ParticipantProvider.class);
        var handler = new SupervisionParticipantHandler(participantProvider,
            mock(ParticipantRegisterAckPublisher.class), mock(ParticipantDeregisterAckPublisher.class));
        handler.handleParticipantMessage(participantStatusMessage);

        verify(participantProvider).saveParticipant(any());
    }
}
