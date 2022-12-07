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

package org.onap.policy.clamp.acm.runtime.supervision;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.onap.policy.clamp.acm.runtime.util.CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionStateChangePublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionUpdatePublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantStatusReqPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantUpdatePublisher;
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.concepts.ParticipantHealthStatus;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class SupervisionScannerTest {

    private static final String AC_JSON = "src/test/resources/rest/acm/AutomationCompositionSmoke.json";

    private static final AcDefinitionProvider acDefinitionProvider = mock(AcDefinitionProvider.class);

    private static final String PARTICIPANT_NAME = "Participant0";
    private static final String PARTICIPANT_VERSION = "1.0.0";

    private static UUID compositionId;

    private static final ToscaConceptIdentifier PARTICIPANT_TYPE =
            new ToscaConceptIdentifier("org.onap.policy.clamp.acm.PolicyParticipant", PARTICIPANT_VERSION);

    @BeforeAll
    public static void setUpBeforeAll() {
        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        var acDefinition = new AutomationCompositionDefinition();
        compositionId = UUID.randomUUID();
        acDefinition.setCompositionId(compositionId);
        acDefinition.setServiceTemplate(serviceTemplate);
        when(acDefinitionProvider.getAllAcDefinitions()).thenReturn(List.of(Objects.requireNonNull(acDefinition)));
    }

    @Test
    void testScannerOrderedStateEqualsToState() {
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var automationCompositionStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var automationCompositionUpdatePublisher = mock(AutomationCompositionUpdatePublisher.class);
        var participantProvider = mock(ParticipantProvider.class);
        var participantStatusReqPublisher = mock(ParticipantStatusReqPublisher.class);
        var participantUpdatePublisher = mock(ParticipantUpdatePublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        when(automationCompositionProvider.getAcInstancesByCompositionId(compositionId))
                .thenReturn(List.of(automationComposition));

        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, acDefinitionProvider,
                automationCompositionStateChangePublisher, automationCompositionUpdatePublisher, participantProvider,
                participantStatusReqPublisher, participantUpdatePublisher, acRuntimeParameterGroup);
        supervisionScanner.run(false);

        verify(automationCompositionProvider, times(0)).updateAutomationComposition(any(AutomationComposition.class));
    }

    @Test
    void testScannerOrderedStateDifferentToState() {
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        automationComposition.setState(AutomationCompositionState.UNINITIALISED2PASSIVE);
        automationComposition.setOrderedState(AutomationCompositionOrderedState.UNINITIALISED);
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        when(automationCompositionProvider.getAcInstancesByCompositionId(compositionId))
                .thenReturn(List.of(automationComposition));

        var automationCompositionUpdatePublisher = mock(AutomationCompositionUpdatePublisher.class);
        var automationCompositionStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var participantProvider = mock(ParticipantProvider.class);
        var participantStatusReqPublisher = mock(ParticipantStatusReqPublisher.class);
        var participantUpdatePublisher = mock(ParticipantUpdatePublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, acDefinitionProvider,
                automationCompositionStateChangePublisher, automationCompositionUpdatePublisher, participantProvider,
                participantStatusReqPublisher, participantUpdatePublisher, acRuntimeParameterGroup);
        supervisionScanner.run(false);

        verify(automationCompositionProvider, times(1)).updateAutomationComposition(any(AutomationComposition.class));
    }

    @Test
    void testScanner() {
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var automationComposition = new AutomationComposition();
        when(automationCompositionProvider.getAcInstancesByCompositionId(compositionId))
                .thenReturn(List.of(automationComposition));

        var participantProvider = mock(ParticipantProvider.class);
        var participant = new Participant();
        participant.setName(PARTICIPANT_NAME);
        participant.setVersion(PARTICIPANT_VERSION);
        when(participantProvider.getParticipants(null, null)).thenReturn(List.of(participant));

        var automationCompositionUpdatePublisher = mock(AutomationCompositionUpdatePublisher.class);
        var participantStatusReqPublisher = mock(ParticipantStatusReqPublisher.class);
        var automationCompositionStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var participantUpdatePublisher = mock(ParticipantUpdatePublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, acDefinitionProvider,
                automationCompositionStateChangePublisher, automationCompositionUpdatePublisher, participantProvider,
                participantStatusReqPublisher, participantUpdatePublisher, acRuntimeParameterGroup);

        supervisionScanner.handleParticipantStatus(participant.getKey().asIdentifier());
        supervisionScanner.run(true);
        verify(automationCompositionProvider, times(0)).updateAutomationComposition(any(AutomationComposition.class));
        verify(participantStatusReqPublisher, times(0)).send(any(ToscaConceptIdentifier.class));
    }

    @Test
    void testSendAutomationCompositionMsgUpdate() {
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        automationComposition.setState(AutomationCompositionState.UNINITIALISED2PASSIVE);
        automationComposition.setOrderedState(AutomationCompositionOrderedState.PASSIVE);
        for (var element : automationComposition.getElements().values()) {
            if ("org.onap.domain.database.Http_PMSHMicroserviceAutomationCompositionElement"
                    .equals(element.getDefinition().getName())) {
                element.setOrderedState(AutomationCompositionOrderedState.PASSIVE);
                element.setState(AutomationCompositionState.UNINITIALISED);
            } else {
                element.setOrderedState(AutomationCompositionOrderedState.PASSIVE);
                element.setState(AutomationCompositionState.PASSIVE);
            }
        }

        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        when(automationCompositionProvider.getAcInstancesByCompositionId(compositionId))
                .thenReturn(List.of(automationComposition));

        var participantProvider = mock(ParticipantProvider.class);
        var automationCompositionUpdatePublisher = mock(AutomationCompositionUpdatePublisher.class);
        var participantStatusReqPublisher = mock(ParticipantStatusReqPublisher.class);
        var automationCompositionStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var participantUpdatePublisher = mock(ParticipantUpdatePublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, acDefinitionProvider,
                automationCompositionStateChangePublisher, automationCompositionUpdatePublisher, participantProvider,
                participantStatusReqPublisher, participantUpdatePublisher, acRuntimeParameterGroup);

        supervisionScanner.run(false);

        verify(automationCompositionUpdatePublisher).send(any(AutomationComposition.class), anyInt());
    }

    @Test
    void testScanParticipant() throws PfModelException {
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var automationComposition = new AutomationComposition();
        when(automationCompositionProvider.getAcInstancesByCompositionId(compositionId))
                .thenReturn(List.of(automationComposition));

        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanParticipant");
        acRuntimeParameterGroup.getParticipantParameters().getUpdateParameters().setMaxWaitMs(-1);
        acRuntimeParameterGroup.getParticipantParameters().setMaxStatusWaitMs(-1);

        var participant = new Participant();
        participant.setName(PARTICIPANT_NAME);
        participant.setVersion(PARTICIPANT_VERSION);
        participant.setHealthStatus(ParticipantHealthStatus.HEALTHY);
        participant.setParticipantState(ParticipantState.ACTIVE);
        participant.setDefinition(new ToscaConceptIdentifier("unknown", "0.0.0"));
        participant.setParticipantType(PARTICIPANT_TYPE);
        var participantProvider = mock(ParticipantProvider.class);
        when(participantProvider.getParticipants()).thenReturn(List.of(participant));

        var automationCompositionUpdatePublisher = mock(AutomationCompositionUpdatePublisher.class);
        var participantStatusReqPublisher = mock(ParticipantStatusReqPublisher.class);
        var automationCompositionStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var participantUpdatePublisher = mock(ParticipantUpdatePublisher.class);

        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, acDefinitionProvider,
                automationCompositionStateChangePublisher, automationCompositionUpdatePublisher, participantProvider,
                participantStatusReqPublisher, participantUpdatePublisher, acRuntimeParameterGroup);

        supervisionScanner
                .handleParticipantRegister(new ImmutablePair<>(participant.getKey().asIdentifier(), PARTICIPANT_TYPE));
        supervisionScanner.handleParticipantStatus(participant.getKey().asIdentifier());
        supervisionScanner.run(true);
        verify(participantStatusReqPublisher).send(any(ToscaConceptIdentifier.class));
        verify(participantProvider).saveParticipant(any());

        supervisionScanner
                .handleParticipantUpdateAck(new ImmutablePair<>(participant.getKey().asIdentifier(), PARTICIPANT_TYPE));
        supervisionScanner.run(true);
        verify(participantProvider, times(2)).saveParticipant(any());
    }
}
