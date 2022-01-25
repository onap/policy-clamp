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

package org.onap.policy.clamp.acm.runtime.supervision;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionStateChangePublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionUpdatePublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantStatusReqPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantUpdatePublisher;
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.concepts.ParticipantHealthStatus;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ServiceTemplateProvider;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

class SupervisionScannerTest {

    private static final String TOSCA_SERVICE_TEMPLATE_YAML =
        "src/test/resources/rest/servicetemplates/tosca-for-smoke-testing.yaml";
    private static final String AC_JSON = "src/test/resources/rest/acm/AutomationCompositionsSmoke.json";

    private static ServiceTemplateProvider serviceTemplateProvider = mock(ServiceTemplateProvider.class);

    @BeforeAll
    public static void setUpBeforeAll() throws Exception {
        ToscaServiceTemplate serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        when(serviceTemplateProvider.getAllServiceTemplates()).thenReturn(List.of(serviceTemplate));
    }

    @Test
    void testScannerOrderedStateEqualsToState() throws PfModelException, CoderException {
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var automationCompositionStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var automationCompositionUpdatePublisher = mock(AutomationCompositionUpdatePublisher.class);
        var participantProvider = mock(ParticipantProvider.class);
        var participantStatusReqPublisher = mock(ParticipantStatusReqPublisher.class);
        var participantUpdatePublisher = mock(ParticipantUpdatePublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var automationCompositions =
            InstantiationUtils.getAutomationCompositionsFromResource(AC_JSON, "Crud").getAutomationCompositionList();
        when(automationCompositionProvider.getAutomationCompositions()).thenReturn(automationCompositions);

        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, serviceTemplateProvider,
            automationCompositionStateChangePublisher, automationCompositionUpdatePublisher, participantProvider,
            participantStatusReqPublisher, participantUpdatePublisher, acRuntimeParameterGroup);
        supervisionScanner.run(false);

        verify(automationCompositionProvider, times(0)).saveAutomationComposition(any(AutomationComposition.class));
    }

    @Test
    void testScannerOrderedStateDifferentToState() throws PfModelException, CoderException {
        var automationCompositions =
            InstantiationUtils.getAutomationCompositionsFromResource(AC_JSON, "Crud").getAutomationCompositionList();
        automationCompositions.get(0).setState(AutomationCompositionState.UNINITIALISED2PASSIVE);
        automationCompositions.get(0).setOrderedState(AutomationCompositionOrderedState.UNINITIALISED);
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        when(automationCompositionProvider.getAutomationCompositions()).thenReturn(automationCompositions);

        var automationCompositionUpdatePublisher = mock(AutomationCompositionUpdatePublisher.class);
        var automationCompositionStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var participantProvider = mock(ParticipantProvider.class);
        var participantStatusReqPublisher = mock(ParticipantStatusReqPublisher.class);
        var participantUpdatePublisher = mock(ParticipantUpdatePublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, serviceTemplateProvider,
            automationCompositionStateChangePublisher, automationCompositionUpdatePublisher, participantProvider,
            participantStatusReqPublisher, participantUpdatePublisher, acRuntimeParameterGroup);
        supervisionScanner.run(false);

        verify(automationCompositionProvider, times(1)).saveAutomationComposition(any(AutomationComposition.class));
    }

    @Test
    void testScanner() throws PfModelException {
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var automationComposition = new AutomationComposition();
        when(automationCompositionProvider.getAutomationCompositions()).thenReturn(List.of(automationComposition));

        var participantProvider = mock(ParticipantProvider.class);
        var participant = new Participant();
        participant.setName("Participant0");
        participant.setVersion("1.0.0");
        when(participantProvider.getParticipants(null, null)).thenReturn(List.of(participant));

        var automationCompositionUpdatePublisher = mock(AutomationCompositionUpdatePublisher.class);
        var participantStatusReqPublisher = mock(ParticipantStatusReqPublisher.class);
        var automationCompositionStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var participantUpdatePublisher = mock(ParticipantUpdatePublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, serviceTemplateProvider,
            automationCompositionStateChangePublisher, automationCompositionUpdatePublisher, participantProvider,
            participantStatusReqPublisher, participantUpdatePublisher, acRuntimeParameterGroup);

        supervisionScanner.handleParticipantStatus(participant.getKey().asIdentifier());
        supervisionScanner.run(true);
        verify(automationCompositionProvider, times(0)).saveAutomationComposition(any(AutomationComposition.class));
        verify(participantStatusReqPublisher, times(0)).send(any(ToscaConceptIdentifier.class));
    }

    @Test
    void testSendAutomationCompositionMsgUpdate() throws PfModelException, CoderException {
        var automationCompositions =
            InstantiationUtils.getAutomationCompositionsFromResource(AC_JSON, "Crud").getAutomationCompositionList();
        automationCompositions.get(0).setState(AutomationCompositionState.UNINITIALISED2PASSIVE);
        automationCompositions.get(0).setOrderedState(AutomationCompositionOrderedState.PASSIVE);
        for (var element : automationCompositions.get(0).getElements().values()) {
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
        when(automationCompositionProvider.getAutomationCompositions()).thenReturn(automationCompositions);

        var participantProvider = mock(ParticipantProvider.class);
        var automationCompositionUpdatePublisher = mock(AutomationCompositionUpdatePublisher.class);
        var participantStatusReqPublisher = mock(ParticipantStatusReqPublisher.class);
        var automationCompositionStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var participantUpdatePublisher = mock(ParticipantUpdatePublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, serviceTemplateProvider,
            automationCompositionStateChangePublisher, automationCompositionUpdatePublisher, participantProvider,
            participantStatusReqPublisher, participantUpdatePublisher, acRuntimeParameterGroup);

        supervisionScanner.run(false);

        verify(automationCompositionUpdatePublisher).send(any(AutomationComposition.class), anyInt());
    }

    @Test
    void testScanParticipant() throws PfModelException {
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var automationComposition = new AutomationComposition();
        when(automationCompositionProvider.getAutomationCompositions()).thenReturn(List.of(automationComposition));

        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanParticipant");
        acRuntimeParameterGroup.getParticipantParameters().getUpdateParameters().setMaxWaitMs(-1);
        acRuntimeParameterGroup.getParticipantParameters().setMaxStatusWaitMs(-1);

        var participant = new Participant();
        participant.setName("Participant0");
        participant.setVersion("1.0.0");
        participant.setHealthStatus(ParticipantHealthStatus.HEALTHY);
        participant.setParticipantState(ParticipantState.ACTIVE);
        participant.setDefinition(new ToscaConceptIdentifier("unknown", "0.0.0"));
        participant.setParticipantType(new ToscaConceptIdentifier("ParticipantType1", "1.0.0"));
        var participantProvider = mock(ParticipantProvider.class);
        when(participantProvider.getParticipants()).thenReturn(List.of(participant));

        var automationCompositionUpdatePublisher = mock(AutomationCompositionUpdatePublisher.class);
        var participantStatusReqPublisher = mock(ParticipantStatusReqPublisher.class);
        var automationCompositionStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var participantUpdatePublisher = mock(ParticipantUpdatePublisher.class);

        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, serviceTemplateProvider,
            automationCompositionStateChangePublisher, automationCompositionUpdatePublisher, participantProvider,
            participantStatusReqPublisher, participantUpdatePublisher, acRuntimeParameterGroup);

        supervisionScanner.handleParticipantStatus(participant.getKey().asIdentifier());
        supervisionScanner.run(true);
        verify(participantStatusReqPublisher).send(any(ToscaConceptIdentifier.class));
        verify(participantProvider).saveParticipant(any());

        supervisionScanner.run(true);
        verify(participantProvider, times(2)).saveParticipant(any());
    }
}
