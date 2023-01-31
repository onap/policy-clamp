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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionStateChangePublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionUpdatePublisher;
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.models.base.PfModelException;

class SupervisionScannerTest {

    private static final String AC_JSON = "src/test/resources/rest/acm/AutomationCompositionSmoke.json";

    private static final AcDefinitionProvider acDefinitionProvider = mock(AcDefinitionProvider.class);

    private static UUID compositionId;

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
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        when(automationCompositionProvider.getAcInstancesByCompositionId(compositionId))
            .thenReturn(List.of(automationComposition));

        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, acDefinitionProvider,
            automationCompositionStateChangePublisher, automationCompositionUpdatePublisher, participantProvider,
            acRuntimeParameterGroup);
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
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, acDefinitionProvider,
            automationCompositionStateChangePublisher, automationCompositionUpdatePublisher, participantProvider,
            acRuntimeParameterGroup);
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
        var participant = CommonTestData.createParticipant(CommonTestData.getParticipantId());
        when(participantProvider.getParticipants()).thenReturn(List.of(participant));

        var automationCompositionUpdatePublisher = mock(AutomationCompositionUpdatePublisher.class);
        var automationCompositionStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, acDefinitionProvider,
            automationCompositionStateChangePublisher, automationCompositionUpdatePublisher, participantProvider,
            acRuntimeParameterGroup);

        supervisionScanner.handleParticipantStatus(participant.getParticipantId());
        supervisionScanner.run(true);
        verify(automationCompositionProvider, times(0)).updateAutomationComposition(any(AutomationComposition.class));
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
        var automationCompositionStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, acDefinitionProvider,
            automationCompositionStateChangePublisher, automationCompositionUpdatePublisher, participantProvider,
            acRuntimeParameterGroup);

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

        var participant = CommonTestData.createParticipant(CommonTestData.getParticipantId());
        participant.setParticipantState(ParticipantState.OFF_LINE);
        var participantProvider = mock(ParticipantProvider.class);
        when(participantProvider.getParticipants()).thenReturn(List.of(participant));

        var automationCompositionUpdatePublisher = mock(AutomationCompositionUpdatePublisher.class);
        var automationCompositionStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);

        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, acDefinitionProvider,
            automationCompositionStateChangePublisher, automationCompositionUpdatePublisher, participantProvider,
            acRuntimeParameterGroup);

        supervisionScanner.handleParticipantStatus(participant.getParticipantId());
        supervisionScanner.run(true);
        verify(participantProvider, times(0)).saveParticipant(any());

        supervisionScanner.run(true);
        verify(participantProvider, times(1)).updateParticipant(any());
    }
}
