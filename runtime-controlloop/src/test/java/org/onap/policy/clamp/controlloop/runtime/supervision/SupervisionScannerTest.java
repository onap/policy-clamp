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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.Participant;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantHealthStatus;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantState;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ControlLoopProvider;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ServiceTemplateProvider;
import org.onap.policy.clamp.controlloop.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ControlLoopStateChangePublisher;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ControlLoopUpdatePublisher;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ParticipantStatusReqPublisher;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ParticipantUpdatePublisher;
import org.onap.policy.clamp.controlloop.runtime.util.CommonTestData;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

class SupervisionScannerTest {

    private static final String TOSCA_SERVICE_TEMPLATE_YAML =
            "src/test/resources/rest/servicetemplates/tosca-for-smoke-testing.yaml";
    private static final String CONTROLLOOP_JSON = "src/test/resources/rest/controlloops/ControlLoopsSmoke.json";

    private static ServiceTemplateProvider serviceTemplateProvider = mock(ServiceTemplateProvider.class);

    @BeforeAll
    public static void setUpBeforeAll() throws Exception {
        ToscaServiceTemplate serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        when(serviceTemplateProvider.getAllServiceTemplates()).thenReturn(List.of(serviceTemplate));
    }

    @Test
    void testScannerOrderedStateEqualsToState() throws PfModelException, CoderException {
        var controlLoopProvider = mock(ControlLoopProvider.class);
        var controlLoopStateChangePublisher = mock(ControlLoopStateChangePublisher.class);
        var controlLoopUpdatePublisher = mock(ControlLoopUpdatePublisher.class);
        var participantProvider = mock(ParticipantProvider.class);
        var participantStatusReqPublisher = mock(ParticipantStatusReqPublisher.class);
        var participantUpdatePublisher = mock(ParticipantUpdatePublisher.class);
        var clRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var controlLoops =
                InstantiationUtils.getControlLoopsFromResource(CONTROLLOOP_JSON, "Crud").getControlLoopList();
        when(controlLoopProvider.getControlLoops()).thenReturn(controlLoops);

        var supervisionScanner = new SupervisionScanner(controlLoopProvider, serviceTemplateProvider,
                controlLoopStateChangePublisher, controlLoopUpdatePublisher, participantProvider,
                participantStatusReqPublisher, participantUpdatePublisher, clRuntimeParameterGroup);
        supervisionScanner.run(false);

        verify(controlLoopProvider, times(0)).saveControlLoop(any(ControlLoop.class));
    }

    @Test
    void testScannerOrderedStateDifferentToState() throws PfModelException, CoderException {
        var controlLoops =
                InstantiationUtils.getControlLoopsFromResource(CONTROLLOOP_JSON, "Crud").getControlLoopList();
        controlLoops.get(0).setState(ControlLoopState.UNINITIALISED2PASSIVE);
        controlLoops.get(0).setOrderedState(ControlLoopOrderedState.UNINITIALISED);
        var controlLoopProvider = mock(ControlLoopProvider.class);
        when(controlLoopProvider.getControlLoops()).thenReturn(controlLoops);

        var controlLoopUpdatePublisher = mock(ControlLoopUpdatePublisher.class);
        var controlLoopStateChangePublisher = mock(ControlLoopStateChangePublisher.class);
        var participantProvider = mock(ParticipantProvider.class);
        var participantStatusReqPublisher = mock(ParticipantStatusReqPublisher.class);
        var participantUpdatePublisher = mock(ParticipantUpdatePublisher.class);
        var clRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var supervisionScanner = new SupervisionScanner(controlLoopProvider, serviceTemplateProvider,
                controlLoopStateChangePublisher, controlLoopUpdatePublisher, participantProvider,
                participantStatusReqPublisher, participantUpdatePublisher, clRuntimeParameterGroup);
        supervisionScanner.run(false);

        verify(controlLoopProvider, times(1)).saveControlLoop(any(ControlLoop.class));
    }

    @Test
    void testScanner() throws PfModelException {
        var controlLoopProvider = mock(ControlLoopProvider.class);
        var controlLoop = new ControlLoop();
        when(controlLoopProvider.getControlLoops()).thenReturn(List.of(controlLoop));

        var participantProvider = mock(ParticipantProvider.class);
        var participant = new Participant();
        participant.setName("Participant0");
        participant.setVersion("1.0.0");
        when(participantProvider.getParticipants(null, null)).thenReturn(List.of(participant));

        var controlLoopUpdatePublisher = mock(ControlLoopUpdatePublisher.class);
        var participantStatusReqPublisher = mock(ParticipantStatusReqPublisher.class);
        var controlLoopStateChangePublisher = mock(ControlLoopStateChangePublisher.class);
        var participantUpdatePublisher = mock(ParticipantUpdatePublisher.class);
        var clRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var supervisionScanner = new SupervisionScanner(controlLoopProvider, serviceTemplateProvider,
                controlLoopStateChangePublisher, controlLoopUpdatePublisher, participantProvider,
                participantStatusReqPublisher, participantUpdatePublisher, clRuntimeParameterGroup);

        supervisionScanner.handleParticipantStatus(participant.getKey().asIdentifier());
        supervisionScanner.run(true);
        verify(controlLoopProvider, times(0)).saveControlLoop(any(ControlLoop.class));
        verify(participantStatusReqPublisher, times(0)).send(any(ToscaConceptIdentifier.class));
    }

    @Test
    void testSendControlLoopMsgUpdate() throws PfModelException, CoderException {
        var controlLoops =
                InstantiationUtils.getControlLoopsFromResource(CONTROLLOOP_JSON, "Crud").getControlLoopList();
        controlLoops.get(0).setState(ControlLoopState.UNINITIALISED2PASSIVE);
        controlLoops.get(0).setOrderedState(ControlLoopOrderedState.PASSIVE);
        for (var element : controlLoops.get(0).getElements().values()) {
            if ("org.onap.domain.database.Http_PMSHMicroserviceControlLoopElement"
                    .equals(element.getDefinition().getName())) {
                element.setOrderedState(ControlLoopOrderedState.PASSIVE);
                element.setState(ControlLoopState.UNINITIALISED);
            } else {
                element.setOrderedState(ControlLoopOrderedState.PASSIVE);
                element.setState(ControlLoopState.PASSIVE);
            }
        }

        var controlLoopProvider = mock(ControlLoopProvider.class);
        when(controlLoopProvider.getControlLoops()).thenReturn(controlLoops);

        var participantProvider = mock(ParticipantProvider.class);
        var controlLoopUpdatePublisher = mock(ControlLoopUpdatePublisher.class);
        var participantStatusReqPublisher = mock(ParticipantStatusReqPublisher.class);
        var controlLoopStateChangePublisher = mock(ControlLoopStateChangePublisher.class);
        var participantUpdatePublisher = mock(ParticipantUpdatePublisher.class);
        var clRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var supervisionScanner = new SupervisionScanner(controlLoopProvider, serviceTemplateProvider,
                controlLoopStateChangePublisher, controlLoopUpdatePublisher, participantProvider,
                participantStatusReqPublisher, participantUpdatePublisher, clRuntimeParameterGroup);

        supervisionScanner.run(false);

        verify(controlLoopUpdatePublisher).send(any(ControlLoop.class), anyInt());
    }

    @Test
    void testScanParticipant() throws PfModelException {
        var controlLoopProvider = mock(ControlLoopProvider.class);
        var controlLoop = new ControlLoop();
        when(controlLoopProvider.getControlLoops()).thenReturn(List.of(controlLoop));

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
        var participantProvider = mock(ParticipantProvider.class);
        when(participantProvider.getParticipants()).thenReturn(List.of(participant));

        var controlLoopUpdatePublisher = mock(ControlLoopUpdatePublisher.class);
        var participantStatusReqPublisher = mock(ParticipantStatusReqPublisher.class);
        var controlLoopStateChangePublisher = mock(ControlLoopStateChangePublisher.class);
        var participantUpdatePublisher = mock(ParticipantUpdatePublisher.class);

        var supervisionScanner = new SupervisionScanner(controlLoopProvider, serviceTemplateProvider,
                controlLoopStateChangePublisher, controlLoopUpdatePublisher, participantProvider,
                participantStatusReqPublisher, participantUpdatePublisher, clRuntimeParameterGroup);

        supervisionScanner.handleParticipantStatus(participant.getKey().asIdentifier());
        supervisionScanner.run(true);
        verify(participantStatusReqPublisher).send(any(ToscaConceptIdentifier.class));
        verify(participantProvider).saveParticipant(any());

        supervisionScanner.run(true);
        verify(participantProvider, times(2)).saveParticipant(any());
    }
}
