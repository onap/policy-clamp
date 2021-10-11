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

package org.onap.policy.clamp.controlloop.participant.intermediary.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.Participant;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantHealthStatus;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantState;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantRegisterAck;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantStatus;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantUpdate;
import org.onap.policy.clamp.controlloop.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.controlloop.participant.intermediary.main.parameters.CommonTestData;
import org.onap.policy.clamp.controlloop.participant.intermediary.parameters.ParticipantParameters;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;


class ParticipantHandlerTest {

    private CommonTestData commonTestData = new CommonTestData();

    @Test
    void mockParticipantHandlerTest() {
        ParticipantHandler participantHandler = commonTestData.getMockParticipantHandler();
        assertNull(participantHandler.getParticipant(null, null));
        assertEquals("org.onap.PM_CDS_Blueprint 1.0.1", participantHandler.getParticipantId().toString());

        ToscaConceptIdentifier id = new ToscaConceptIdentifier("org.onap.PM_CDS_Blueprint", "1.0.1");
        assertEquals(id, participantHandler.getParticipantId());
        assertEquals(id, participantHandler.getParticipantType());
        assertThat(participantHandler.getClElementDefinitionCommonProperties(id)).isEmpty();

    }

    @Test
    void handleUpdateTest() {
        ParticipantParameters parameters = CommonTestData.getParticipantParameters();
        ControlLoopHandler controlLoopHander = commonTestData.getMockControlLoopHandler();
        ParticipantMessagePublisher publisher = new ParticipantMessagePublisher();
        ParticipantHandler emptyParticipantHandler =
                new ParticipantHandler(parameters, publisher, controlLoopHander);
        ParticipantUpdate participantUpdateMsg = new ParticipantUpdate();

        assertThatThrownBy(() ->
                emptyParticipantHandler.handleParticipantUpdate(participantUpdateMsg))
                .isInstanceOf(RuntimeException.class);

        ParticipantHandler participantHandler = commonTestData.getMockParticipantHandler();
        ToscaConceptIdentifier id = new ToscaConceptIdentifier("org.onap.PM_CDS_Blueprint", "1.0.1");
        participantUpdateMsg.setControlLoopId(id);
        participantUpdateMsg.setParticipantId(id);
        participantUpdateMsg.setParticipantType(id);
        participantUpdateMsg.setMessageId(UUID.randomUUID());
        participantUpdateMsg.setTimestamp(Instant.ofEpochMilli(3000));


        ParticipantStatus heartbeatF = participantHandler.makeHeartbeat(false);
        assertEquals(id, heartbeatF.getParticipantId());
        assertEquals(ParticipantState.UNKNOWN, heartbeatF.getParticipantStatistics().getState());
        assertThat(heartbeatF.getControlLoopInfoList()).isEmpty();

        participantHandler.handleParticipantUpdate(participantUpdateMsg);
        assertThat(participantHandler.getClElementDefinitionCommonProperties(id)).isEmpty();

        ParticipantStatus heartbeatT = participantHandler.makeHeartbeat(true);
        assertEquals(id, heartbeatT.getParticipantId());
        assertEquals(ParticipantState.TERMINATED, heartbeatT.getParticipantStatistics().getState());
        assertThat(heartbeatT.getParticipantDefinitionUpdates()).isNotEmpty();
        assertEquals(id, heartbeatT.getParticipantDefinitionUpdates().get(0).getParticipantId());

    }

    @Test
    void handleParticipantTest() {
        ParticipantHandler participantHandler = commonTestData.getMockParticipantHandler();
        ToscaConceptIdentifier id = new ToscaConceptIdentifier("org.onap.PM_CDS_Blueprint", "1.0.1");
        Participant p = participantHandler.getParticipant(id.getName(), id.getVersion());
        assertEquals(ParticipantState.UNKNOWN, p.getParticipantState());

        participantHandler.updateParticipantState(id, ParticipantState.PASSIVE);
        Participant p2 = participantHandler.getParticipant(id.getName(), id.getVersion());
        assertEquals(ParticipantState.PASSIVE, p2.getParticipantState());

        ParticipantRegisterAck participantRegisterAckMsg = new ParticipantRegisterAck();
        participantRegisterAckMsg.setState(ParticipantState.TERMINATED);
        participantHandler.handleParticipantRegisterAck(participantRegisterAckMsg);
        assertEquals(ParticipantHealthStatus.HEALTHY, participantHandler.makeHeartbeat(false).getHealthStatus());

    }

}
