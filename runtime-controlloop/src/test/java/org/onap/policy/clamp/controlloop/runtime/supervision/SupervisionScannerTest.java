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

package org.onap.policy.clamp.controlloop.runtime.supervision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.Participant;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantHealthStatus;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantState;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ControlLoopProvider;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.controlloop.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ControlLoopStateChangePublisher;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ControlLoopUpdatePublisher;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ParticipantStatusReqPublisher;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ParticipantUpdatePublisher;
import org.onap.policy.clamp.controlloop.runtime.util.CommonTestData;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

class SupervisionScannerTest {

    private static final String TOSCA_SERVICE_TEMPLATE_YAML =
            "src/test/resources/rest/servicetemplates/tosca-for-smoke-testing.yaml";

    @Test
    void testScannerOrderedStateEqualsToState() throws PfModelException {
        var controlLoopProvider = mock(ControlLoopProvider.class);
        var modelsProvider = mock(PolicyModelsProvider.class);
        var controlLoopStateChangePublisher = mock(ControlLoopStateChangePublisher.class);
        var controlLoopUpdatePublisher = mock(ControlLoopUpdatePublisher.class);
        var participantProvider = mock(ParticipantProvider.class);
        var participantStatusReqPublisher = mock(ParticipantStatusReqPublisher.class);
        var participantUpdatePublisher = mock(ParticipantUpdatePublisher.class);
        var clRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var controlLoop = new ControlLoop();
        when(controlLoopProvider.getControlLoops(null, null)).thenReturn(List.of(controlLoop));

        var supervisionScanner = new SupervisionScanner(controlLoopProvider, modelsProvider,
                controlLoopStateChangePublisher, controlLoopUpdatePublisher, participantProvider,
                participantStatusReqPublisher, participantUpdatePublisher, clRuntimeParameterGroup);
        supervisionScanner.run(false);

        verify(controlLoopProvider, times(0)).updateControlLoop(any(ControlLoop.class));
    }

    @Test
    void testScannerOrderedStateDifferentToState() throws PfModelException {
        var controlLoop = new ControlLoop();
        controlLoop.setState(ControlLoopState.UNINITIALISED2PASSIVE);
        controlLoop.setOrderedState(ControlLoopOrderedState.UNINITIALISED);
        controlLoop.setElements(Map.of(UUID.randomUUID(), new ControlLoopElement()));
        var controlLoopProvider = mock(ControlLoopProvider.class);
        when(controlLoopProvider.getControlLoops(null, null)).thenReturn(List.of(controlLoop));

        var controlLoopUpdatePublisher = mock(ControlLoopUpdatePublisher.class);
        var controlLoopStateChangePublisher = mock(ControlLoopStateChangePublisher.class);
        var participantProvider = mock(ParticipantProvider.class);
        var participantStatusReqPublisher = mock(ParticipantStatusReqPublisher.class);
        var participantUpdatePublisher = mock(ParticipantUpdatePublisher.class);
        var clRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var modelsProvider = mock(PolicyModelsProvider.class);
        when(modelsProvider.getServiceTemplateList(null, null)).thenReturn(List.of(new ToscaServiceTemplate()));

        var supervisionScanner = new SupervisionScanner(controlLoopProvider, modelsProvider,
                controlLoopStateChangePublisher, controlLoopUpdatePublisher, participantProvider,
                participantStatusReqPublisher, participantUpdatePublisher, clRuntimeParameterGroup);
        supervisionScanner.run(false);

        verify(controlLoopProvider, times(1)).updateControlLoop(any(ControlLoop.class));
    }

    @Test
    void testScanner() throws PfModelException {
        var controlLoopProvider = mock(ControlLoopProvider.class);
        var controlLoop = new ControlLoop();
        when(controlLoopProvider.getControlLoops(null, null)).thenReturn(List.of(controlLoop));

        var participantProvider = mock(ParticipantProvider.class);
        var participant = new Participant();
        participant.setName("Participant0");
        participant.setVersion("1.0.0");
        when(participantProvider.getParticipants(null, null)).thenReturn(List.of(participant));

        var modelsProvider = mock(PolicyModelsProvider.class);
        var controlLoopUpdatePublisher = mock(ControlLoopUpdatePublisher.class);
        var participantStatusReqPublisher = mock(ParticipantStatusReqPublisher.class);
        var controlLoopStateChangePublisher = mock(ControlLoopStateChangePublisher.class);
        var participantUpdatePublisher = mock(ParticipantUpdatePublisher.class);
        var clRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var supervisionScanner = new SupervisionScanner(controlLoopProvider, modelsProvider,
                controlLoopStateChangePublisher, controlLoopUpdatePublisher, participantProvider,
                participantStatusReqPublisher, participantUpdatePublisher, clRuntimeParameterGroup);

        supervisionScanner.handleParticipantStatus(participant.getKey().asIdentifier());
        supervisionScanner.run(true);
        verify(controlLoopProvider, times(0)).updateControlLoop(any(ControlLoop.class));
        verify(participantStatusReqPublisher, times(0)).send(any(ToscaConceptIdentifier.class));
    }

    @Test
    void testSendControlLoopMsgUpdate() throws PfModelException {
        var controlLoop = new ControlLoop();
        controlLoop.setState(ControlLoopState.UNINITIALISED2PASSIVE);
        controlLoop.setOrderedState(ControlLoopOrderedState.PASSIVE);
        controlLoop.setElements(Map.of(UUID.randomUUID(),
                createHttpElement(ControlLoopState.UNINITIALISED, ControlLoopOrderedState.PASSIVE)));

        var controlLoopProvider = mock(ControlLoopProvider.class);
        when(controlLoopProvider.getControlLoops(null, null)).thenReturn(List.of(controlLoop));

        ToscaServiceTemplate serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        var modelsProvider = mock(PolicyModelsProvider.class);
        when(modelsProvider.getServiceTemplateList(null, null)).thenReturn(List.of(serviceTemplate));

        var participantProvider = mock(ParticipantProvider.class);
        var controlLoopUpdatePublisher = mock(ControlLoopUpdatePublisher.class);
        var participantStatusReqPublisher = mock(ParticipantStatusReqPublisher.class);
        var controlLoopStateChangePublisher = mock(ControlLoopStateChangePublisher.class);
        var participantUpdatePublisher = mock(ParticipantUpdatePublisher.class);
        var clRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var supervisionScanner = new SupervisionScanner(controlLoopProvider, modelsProvider,
                controlLoopStateChangePublisher, controlLoopUpdatePublisher, participantProvider,
                participantStatusReqPublisher, participantUpdatePublisher, clRuntimeParameterGroup);

        supervisionScanner.run(false);

        verify(controlLoopUpdatePublisher).send(any(ControlLoop.class), anyInt());
    }

    private ControlLoopElement createHttpElement(ControlLoopState state, ControlLoopOrderedState orderedState) {
        var element = new ControlLoopElement();
        element.setDefinition(new ToscaConceptIdentifier(
                "org.onap.domain.database.Http_PMSHMicroserviceControlLoopElement", "1.2.3"));
        element.setState(state);
        element.setOrderedState(orderedState);
        element.setParticipantId(new ToscaConceptIdentifier("HttpParticipant0", "1.0.0"));
        element.setParticipantType(
                new ToscaConceptIdentifier("org.onap.k8s.controlloop.HttpControlLoopParticipant", "2.3.4"));

        return element;
    }

    @Test
    void testScanParticipant() throws PfModelException {
        var controlLoopProvider = mock(ControlLoopProvider.class);
        var controlLoop = new ControlLoop();
        when(controlLoopProvider.getControlLoops(null, null)).thenReturn(List.of(controlLoop));

        var clRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanParticipant");
        clRuntimeParameterGroup.getParticipantParameters().getUpdateParameters().setMaxWaitMs(-1);
        clRuntimeParameterGroup.getParticipantParameters().setMaxStatusWaitMs(-1);

        var participant = new Participant();
        participant.setName("Participant0");
        participant.setVersion("1.0.0");
        participant.setHealthStatus(ParticipantHealthStatus.HEALTHY);
        participant.setParticipantState(ParticipantState.ACTIVE);
        participant.setDefinition(new ToscaConceptIdentifier("unknown", "0.0.0"));
        participant.setParticipantType(new ToscaConceptIdentifier("ParticipantType1", "1.0.0"));
        var participantProvider = new ParticipantProvider(clRuntimeParameterGroup.getDatabaseProviderParameters());
        participantProvider.updateParticipants(List.of(participant));

        var modelsProvider = mock(PolicyModelsProvider.class);
        var controlLoopUpdatePublisher = mock(ControlLoopUpdatePublisher.class);
        var participantStatusReqPublisher = mock(ParticipantStatusReqPublisher.class);
        var controlLoopStateChangePublisher = mock(ControlLoopStateChangePublisher.class);
        var participantUpdatePublisher = mock(ParticipantUpdatePublisher.class);

        var supervisionScanner = new SupervisionScanner(controlLoopProvider, modelsProvider,
                controlLoopStateChangePublisher, controlLoopUpdatePublisher, participantProvider,
                participantStatusReqPublisher, participantUpdatePublisher, clRuntimeParameterGroup);

        supervisionScanner.handleParticipantStatus(participant.getKey().asIdentifier());
        supervisionScanner.run(true);
        verify(participantStatusReqPublisher, times(1)).send(any(ToscaConceptIdentifier.class));

        List<Participant> participants = participantProvider.getParticipants(null, null);
        assertThat(participants.get(0).getHealthStatus()).isEqualTo(ParticipantHealthStatus.NOT_HEALTHY);

        supervisionScanner.run(true);
        participants = participantProvider.getParticipants(null, null);
        assertThat(participants.get(0).getHealthStatus()).isEqualTo(ParticipantHealthStatus.OFF_LINE);
    }
}
