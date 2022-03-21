/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.intermediary.api.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.policy.clamp.acm.participant.intermediary.api.AutomationCompositionElementListener;
import org.onap.policy.clamp.acm.participant.intermediary.main.parameters.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AcElementStatistics;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantHealthStatus;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantStatistics;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class ParticipantIntermediaryApiImplTest {

    private final CommonTestData commonTestData = new CommonTestData();
    private static final String ID_NAME = "org.onap.PM_CDS_Blueprint";
    private static final String ID_VERSION = "1.0.1";

    private static final String ID_NAME_E = "org.onap.domain.pmsh.PMSHAutomationCompositionDefinition";
    private static final String ID_VERSION_E = "1.0.0";

    private static final String ID_NAME_TYPE = "org.onap.dcae.acm.DCAEMicroserviceAutomationCompositionParticipant";
    private static final String ID_VERSION_TYPE = "2.3.4";

    @Test
    void mockParticipantIntermediaryApiImplTest() throws CoderException {
        var uuid = UUID.randomUUID();
        var id = new ToscaConceptIdentifier(ID_NAME, ID_VERSION);
        var participantHandler = commonTestData.getParticipantHandlerAutomationCompositions();
        var automationComposiitonHandler = commonTestData.setTestAutomationCompositionHandler(id, uuid);
        var apiImpl = new ParticipantIntermediaryApiImpl(participantHandler, automationComposiitonHandler);
        var acElementListener = Mockito.mock(AutomationCompositionElementListener.class);
        apiImpl.registerAutomationCompositionElementListener(acElementListener);

        assertNotNull(apiImpl.getAutomationCompositions(id.getName(), id.getVersion()));
        assertThat(apiImpl.getAcElementDefinitionCommonProperties(id)).isEmpty();

        var participantStatistics = new ParticipantStatistics();
        participantStatistics.setParticipantId(id);
        participantStatistics.setTimeStamp(Instant.ofEpochMilli(123456L));
        participantStatistics.setState(ParticipantState.PASSIVE);
        participantStatistics.setHealthStatus(ParticipantHealthStatus.HEALTHY);
        apiImpl.updateParticipantStatistics(participantStatistics);

        var participants = apiImpl.getParticipants(id.getName(), id.getVersion());
        assertEquals(ParticipantState.UNKNOWN, participants.get(0).getParticipantState());

        var participant = apiImpl.updateParticipantState(id, ParticipantState.TERMINATED);
        assertEquals(ParticipantState.TERMINATED, participant.getParticipantState());

        var elements = apiImpl.getAutomationCompositionElements(ID_NAME_E, ID_VERSION_E);
        assertFalse(elements.containsKey(uuid));

        var element = apiImpl.getAutomationCompositionElement(elements.keySet().iterator().next());
        var idType = new ToscaConceptIdentifier(ID_NAME_TYPE, ID_VERSION_TYPE);
        assertEquals(idType, element.getParticipantType());

        var acElementStatistics = new AcElementStatistics();
        var automationCompositionId = new ToscaConceptIdentifier("defName", "0.0.1");
        acElementStatistics.setParticipantId(automationCompositionId);
        acElementStatistics.setState(AutomationCompositionState.RUNNING);
        acElementStatistics.setTimeStamp(Instant.now());

        apiImpl.updateAutomationCompositionElementStatistics(uuid, acElementStatistics);
        var acElement =
            apiImpl.updateAutomationCompositionElementState(id, uuid, AutomationCompositionOrderedState.UNINITIALISED,
                AutomationCompositionState.PASSIVE, ParticipantMessageType.AUTOMATION_COMPOSITION_STATECHANGE_ACK);
        assertEquals(AutomationCompositionOrderedState.UNINITIALISED, acElement.getOrderedState());
        assertEquals(uuid, acElement.getId());

    }
}
