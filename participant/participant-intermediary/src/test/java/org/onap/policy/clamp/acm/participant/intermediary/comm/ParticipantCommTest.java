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

package org.onap.policy.clamp.acm.participant.intermediary.comm;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.policy.clamp.acm.participant.intermediary.handler.ParticipantHandler;
import org.onap.policy.clamp.acm.participant.intermediary.main.parameters.CommonTestData;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionRuntimeException;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionDeployAck;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantDeregister;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantPrimeAck;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantRegister;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantStatus;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.common.utils.coder.CoderException;

class ParticipantCommTest {

    @Test
    void participantReqTest() throws CoderException {
        var participantHandler = mock(ParticipantHandler.class);

        var participantRegisterAckListener = new ParticipantRegisterAckListener(participantHandler);
        assertEquals(ParticipantMessageType.PARTICIPANT_REGISTER_ACK.name(), participantRegisterAckListener.getType());

        var participantStatusReqListener = new ParticipantStatusReqListener(participantHandler);
        assertEquals(ParticipantMessageType.PARTICIPANT_STATUS_REQ.name(), participantStatusReqListener.getType());

        var participantDeregisterAckListener = new ParticipantDeregisterAckListener(participantHandler);
        assertEquals(ParticipantMessageType.PARTICIPANT_DEREGISTER_ACK.name(),
                participantDeregisterAckListener.getType());

        var participantPrimeListener = new ParticipantPrimeListener(participantHandler);
        assertEquals(ParticipantMessageType.PARTICIPANT_PRIME.name(), participantPrimeListener.getType());

        var acPropertyUpdateListener = new AcPropertyUpdateListener(participantHandler);
        assertEquals(ParticipantMessageType.PROPERTIES_UPDATE.name(), acPropertyUpdateListener.getType());

        var automationCompositionUpdateListener = new AutomationCompositionDeployListener(participantHandler);
        assertEquals(ParticipantMessageType.AUTOMATION_COMPOSITION_DEPLOY.name(),
                automationCompositionUpdateListener.getType());

        var automationCompositionStateChangeListener = new AutomationCompositionStateChangeListener(participantHandler);
        assertEquals(ParticipantMessageType.AUTOMATION_COMPOSITION_STATE_CHANGE.name(),
                automationCompositionStateChangeListener.getType());
    }

    @Test
    void participantMessagePublisherTest() {
        var publisher = new ParticipantMessagePublisher();
        publisher.active(Collections.singletonList(Mockito.mock(TopicSink.class)));
        var participantStatus = new ParticipantStatus();
        assertDoesNotThrow(() -> publisher.sendParticipantStatus(participantStatus));

        assertDoesNotThrow(() -> publisher.sendHeartbeat(participantStatus));

        var participantRegister = new ParticipantRegister();
        assertDoesNotThrow(() -> publisher.sendParticipantRegister(participantRegister));

        var participantDeregister = new ParticipantDeregister();
        assertDoesNotThrow(() -> publisher.sendParticipantDeregister(participantDeregister));

        var participantPrimeAck = new ParticipantPrimeAck();
        assertDoesNotThrow(() -> publisher.sendParticipantPrimeAck(participantPrimeAck));

        var automationCompositionAck = mock(AutomationCompositionDeployAck.class);
        assertDoesNotThrow(() -> publisher.sendAutomationCompositionAck(automationCompositionAck));
    }

    @Test
    void participantMessagePublisherExceptionsTest() {
        var publisher = new ParticipantMessagePublisher();

        var participantStatus = new ParticipantStatus();
        assertThrows(AutomationCompositionRuntimeException.class,
                () -> publisher.sendParticipantStatus(participantStatus));
        assertThrows(AutomationCompositionRuntimeException.class, () -> publisher.sendHeartbeat(participantStatus));

        var participantRegister = new ParticipantRegister();
        assertThrows(AutomationCompositionRuntimeException.class,
                () -> publisher.sendParticipantRegister(participantRegister));

        var participantDeregister = new ParticipantDeregister();
        assertThrows(AutomationCompositionRuntimeException.class,
                () -> publisher.sendParticipantDeregister(participantDeregister));

        var automationCompositionAck = mock(AutomationCompositionDeployAck.class);
        assertThrows(AutomationCompositionRuntimeException.class,
                () -> publisher.sendAutomationCompositionAck(automationCompositionAck));

        List<TopicSink> emptyList = Collections.emptyList();
        assertThrows(IllegalArgumentException.class, () -> publisher.active(emptyList));

        publisher.stop();
    }

    @Test
    void messageSenderTest() throws CoderException {
        var participantHandler = mock(ParticipantHandler.class);
        var participantParameters = CommonTestData.getParticipantParameters();
        var messageSender = new MessageSender(participantHandler, participantParameters);
        messageSender.run();
        assertFalse(messageSender.makeTimerPool().isTerminated());
        messageSender.close();
    }

}
