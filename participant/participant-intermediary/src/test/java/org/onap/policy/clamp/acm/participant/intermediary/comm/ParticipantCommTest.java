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

package org.onap.policy.clamp.acm.participant.intermediary.comm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.policy.clamp.acm.participant.intermediary.main.parameters.CommonTestData;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionRuntimeException;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionAck;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantDeregister;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantRegister;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantStatus;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.common.utils.coder.CoderException;

class ParticipantCommTest {

    private CommonTestData commonTestData = new CommonTestData();

    @Test
    void participantReqTest() throws CoderException {
        var participantHandler = commonTestData.getParticipantHandlerAutomationCompositions();

        var participantRegisterAckListener = new ParticipantRegisterAckListener(participantHandler);
        assertEquals(ParticipantMessageType.PARTICIPANT_REGISTER_ACK.name(), participantRegisterAckListener.getType());

        var participantStatusReqListener = new ParticipantStatusReqListener(participantHandler);
        assertEquals(ParticipantMessageType.PARTICIPANT_STATUS_REQ.name(), participantStatusReqListener.getType());

        var participantDeregisterAckListener = new ParticipantDeregisterAckListener(participantHandler);
        assertEquals(ParticipantMessageType.PARTICIPANT_DEREGISTER_ACK.name(),
            participantDeregisterAckListener.getType());

        var participantUpdateListener = new ParticipantUpdateListener(participantHandler);
        assertEquals(ParticipantMessageType.PARTICIPANT_UPDATE.name(), participantUpdateListener.getType());

        var automationCompositionUpdateListener = new AutomationCompositionUpdateListener(participantHandler);
        assertEquals(ParticipantMessageType.AUTOMATION_COMPOSITION_UPDATE.name(),
            automationCompositionUpdateListener.getType());

        var automationCompositionStateChangeListener = new AutomationCompositionStateChangeListener(participantHandler);
        assertEquals(ParticipantMessageType.AUTOMATION_COMPOSITION_STATE_CHANGE.name(),
            automationCompositionStateChangeListener.getType());
    }

    @Test
    void participantMessagePublisherExceptionsTest() {
        var participantMessagePublisher = new ParticipantMessagePublisher();

        var participantStatus = Mockito.mock(ParticipantStatus.class);
        assertThrows(AutomationCompositionRuntimeException.class, () -> {
            participantMessagePublisher.sendParticipantStatus(participantStatus);
        });
        assertThrows(AutomationCompositionRuntimeException.class, () -> {
            participantMessagePublisher.sendHeartbeat(participantStatus);
        });

        var participantRegister = Mockito.mock(ParticipantRegister.class);
        assertThrows(AutomationCompositionRuntimeException.class, () -> {
            participantMessagePublisher.sendParticipantRegister(participantRegister);
        });

        var participantDeregister = Mockito.mock(ParticipantDeregister.class);
        assertThrows(AutomationCompositionRuntimeException.class, () -> {
            participantMessagePublisher.sendParticipantDeregister(participantDeregister);
        });

        var automationCompositionAck = Mockito.mock(AutomationCompositionAck.class);
        assertThrows(AutomationCompositionRuntimeException.class, () -> {
            participantMessagePublisher.sendAutomationCompositionAck(automationCompositionAck);
        });

        List<TopicSink> emptyList = Collections.emptyList();
        assertThrows(IllegalArgumentException.class, () -> {
            participantMessagePublisher.active(emptyList);
        });

        participantMessagePublisher.stop();
    }

    @Test
    void messageSenderTest() throws CoderException {
        var participantHandler = commonTestData.getParticipantHandlerAutomationCompositions();
        var participantParameters = CommonTestData.getParticipantParameters();
        var messageSender = new MessageSender(participantHandler, participantParameters);
        messageSender.run();
        assertFalse(messageSender.makeTimerPool().isTerminated());
        messageSender.close();
    }

}
