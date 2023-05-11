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

package org.onap.policy.clamp.acm.participant.intermediary.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.acm.participant.intermediary.main.parameters.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDefinition;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantAckMessage;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantDeregister;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessage;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantPrime;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantRegisterAck;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.models.base.PfModelException;

class ParticipantHandlerTest {

    private final CommonTestData commonTestData = new CommonTestData();

    @Test
    void handleUpdateTest() {
        var parameters = CommonTestData.getParticipantParameters();
        var automationCompositionHander = commonTestData.getMockAutomationCompositionHandler();
        var publisher = new ParticipantMessagePublisher();
        var emptyParticipantHandler =
                new ParticipantHandler(parameters, publisher, automationCompositionHander);
        var participantPrimeMsg = new ParticipantPrime();

        assertThatThrownBy(() ->
                emptyParticipantHandler.handleParticipantPrime(participantPrimeMsg))
                .isInstanceOf(RuntimeException.class);

        var participantHandler = commonTestData.getMockParticipantHandler();

        var participantId = CommonTestData.getParticipantId();
        participantPrimeMsg.setCompositionId(CommonTestData.AC_ID_1);
        participantPrimeMsg.setParticipantId(participantId);
        participantPrimeMsg.setMessageId(UUID.randomUUID());
        participantPrimeMsg.setTimestamp(Instant.ofEpochMilli(3000));

        var heartbeatF = participantHandler.makeHeartbeat(false);
        assertEquals(participantId, heartbeatF.getParticipantId());
        assertThat(heartbeatF.getAutomationCompositionInfoList()).isEmpty();

        participantHandler.handleParticipantPrime(participantPrimeMsg);

        var heartbeatT = participantHandler.makeHeartbeat(true);
        assertEquals(participantId, heartbeatT.getParticipantId());
        assertThat(heartbeatT.getParticipantDefinitionUpdates()).isNotEmpty();
        assertEquals(participantId, heartbeatT.getParticipantDefinitionUpdates().get(0).getParticipantId());

        var pum = setListParticipantDefinition(participantPrimeMsg);
        participantHandler.handleParticipantPrime(pum);
        var heartbeatTAfterUpdate = participantHandler.makeHeartbeat(true);
        assertEquals(participantId, heartbeatTAfterUpdate.getParticipantId());
    }

    private ParticipantPrime setListParticipantDefinition(ParticipantPrime participantPrimeMsg) {
        var def = new ParticipantDefinition();
        def.setParticipantId(CommonTestData.getParticipantId());
        participantPrimeMsg.setParticipantDefinitionUpdates(List.of(def));
        return participantPrimeMsg;
    }

    @Test
    void checkAppliesTo() {
        var participantHandler = commonTestData.getMockParticipantHandler();
        var participantAckMsg =
                new ParticipantAckMessage(ParticipantMessageType.AUTOMATION_COMPOSITION_DEPLOY);
        assertTrue(participantHandler.appliesTo(participantAckMsg));

        var participantMsg =
                new ParticipantMessage(ParticipantMessageType.PARTICIPANT_STATUS);
        assertTrue(participantHandler.appliesTo(participantMsg));

        var randomId = UUID.randomUUID();
        participantMsg.setParticipantId(randomId);
        assertFalse(participantHandler.appliesTo(participantMsg));

    }

    @Test
    void getAutomationCompositionInfoListTest() throws CoderException {
        var automationCompositionHandler = mock(AutomationCompositionHandler.class);
        var participantHandler =
                commonTestData.getParticipantHandlerAutomationCompositions(automationCompositionHandler);
        clearInvocations(automationCompositionHandler);
        participantHandler.sendHeartbeat();
        verify(automationCompositionHandler).getAutomationCompositionInfoList();
    }

    @Test
    void testHandleParticipantRegisterAck() {
        var parameters = CommonTestData.getParticipantParameters();
        var automationCompositionHandler = commonTestData.getMockAutomationCompositionHandler();
        var publisher = mock(ParticipantMessagePublisher.class);
        var participantHandler = new ParticipantHandler(parameters, publisher, automationCompositionHandler);

        participantHandler.handleParticipantRegisterAck(new ParticipantRegisterAck());
        verify(publisher).sendParticipantStatus(any());
    }

    @Test
    void testSendParticipantDeregister() throws PfModelException {
        var commonTestData = new CommonTestData();
        var automationCompositionMap = commonTestData.getTestAutomationCompositionMap();
        var automationCompositionHandler = mock(AutomationCompositionHandler.class);

        automationCompositionMap.values().iterator().next().getElements().values().iterator().next()
            .setParticipantId(CommonTestData.getParticipantId());
        when(automationCompositionHandler.getAutomationCompositionMap()).thenReturn(automationCompositionMap);

        var publisher = mock(ParticipantMessagePublisher.class);
        var parameters = CommonTestData.getParticipantParameters();
        var participantHandler = new ParticipantHandler(parameters, publisher, automationCompositionHandler);

        participantHandler.sendParticipantDeregister();
        verify(publisher).sendParticipantDeregister(any(ParticipantDeregister.class));
        verify(automationCompositionHandler).undeployInstances();
    }
}
