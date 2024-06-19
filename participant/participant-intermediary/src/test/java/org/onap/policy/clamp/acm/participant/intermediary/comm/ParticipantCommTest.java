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
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeployAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantDeregister;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrimeAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantRegister;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantRegisterAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatus;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatusReq;
import org.onap.policy.common.endpoints.event.comm.TopicSink;

class ParticipantCommTest {

    @Test
    void participantListenerTest() {
        var participantHandler = mock(ParticipantHandler.class);

        var participantRegisterAckListener = new ParticipantRegisterAckListener(participantHandler);
        participantRegisterAckListener.onTopicEvent(null, null, null, new ParticipantRegisterAck());
        assertEquals(ParticipantMessageType.PARTICIPANT_REGISTER_ACK.name(), participantRegisterAckListener.getType());
        assertEquals(participantRegisterAckListener, participantRegisterAckListener.getScoListener());

        var participantStatusReqListener = new ParticipantStatusReqListener(participantHandler);
        participantStatusReqListener.onTopicEvent(null, null, null, new ParticipantStatusReq());
        assertEquals(ParticipantMessageType.PARTICIPANT_STATUS_REQ.name(), participantStatusReqListener.getType());
        assertEquals(participantStatusReqListener, participantStatusReqListener.getScoListener());

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

        var participantSyncListener = new ParticipantSyncListener(participantHandler);
        assertEquals(ParticipantMessageType.PARTICIPANT_SYNC_MSG.name(),
                participantSyncListener.getType());

        var acMigrationListener = new AutomationCompositionMigrationListener(participantHandler);
        assertEquals(ParticipantMessageType.AUTOMATION_COMPOSITION_MIGRATION.name(), acMigrationListener.getType());
    }

    @Test
    void participantMessagePublisherTest() {
        var publisher = new ParticipantMessagePublisher();
        publisher.active(Collections.singletonList(Mockito.mock(TopicSink.class)));
        var participantStatus = new ParticipantStatus();
        assertDoesNotThrow(() -> publisher.sendParticipantStatus(participantStatus));

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
    void messageSenderTest() {
        var participantHandler = mock(ParticipantHandler.class);
        var participantParameters = CommonTestData.getParticipantParameters();
        var messageSender = new MessageSender(participantHandler, participantParameters);
        messageSender.handleContextRefreshEvent(null);
        messageSender.run();
        assertFalse(messageSender.makeTimerPool().isTerminated());
        messageSender.close();
    }
}
