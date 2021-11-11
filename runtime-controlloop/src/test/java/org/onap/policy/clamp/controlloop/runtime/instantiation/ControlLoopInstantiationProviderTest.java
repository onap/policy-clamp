/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
 *  Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.clamp.controlloop.runtime.instantiation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopRuntimeException;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoops;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ControlLoopProvider;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.controlloop.models.messages.rest.instantiation.InstantiationCommand;
import org.onap.policy.clamp.controlloop.models.messages.rest.instantiation.InstantiationResponse;
import org.onap.policy.clamp.controlloop.runtime.commissioning.CommissioningProvider;
import org.onap.policy.clamp.controlloop.runtime.supervision.SupervisionHandler;
import org.onap.policy.clamp.controlloop.runtime.util.CommonTestData;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

/**
 * Class to perform unit test of {@link ControlLoopInstantiationProvider}}.
 *
 */
class ControlLoopInstantiationProviderTest {
    private static final String ID_NAME = "PMSH_Instance1";
    private static final String ID_VERSION = "1.2.3";
    private static final String CL_INSTANTIATION_CREATE_JSON = "src/test/resources/rest/controlloops/ControlLoops.json";
    private static final String CL_INSTANTIATION_UPDATE_JSON =
            "src/test/resources/rest/controlloops/ControlLoopsUpdate.json";
    private static final String CL_INSTANTIATION_CHANGE_STATE_JSON =
            "src/test/resources/rest/controlloops/PassiveCommand.json";
    private static final String CL_INSTANTIATION_DEFINITION_NAME_NOT_FOUND_JSON =
            "src/test/resources/rest/controlloops/ControlLoopElementsNotFound.json";
    private static final String CL_INSTANTIATION_CONTROLLOOP_DEFINITION_NOT_FOUND_JSON =
            "src/test/resources/rest/controlloops/ControlLoopsNotFound.json";
    private static final String TOSCA_TEMPLATE_YAML =
            "src/test/resources/rest/servicetemplates/pmsh_multiple_cl_tosca.yaml";
    private static final String CONTROL_LOOP_NOT_FOUND = "Control Loop not found";
    private static final String DELETE_BAD_REQUEST = "Control Loop State is still %s";
    private static final String ORDERED_STATE_INVALID = "ordered state invalid or not specified on command";
    private static final String CONTROLLOOP_ELEMENT_NAME_NOT_FOUND =
            "\"ControlLoops\" INVALID, item has status INVALID\n"
                    + "  \"entry org.onap.domain.pmsh.PMSHControlLoopDefinition\" INVALID, item has status INVALID\n"
                    + "    \"entry org.onap.domain.pmsh.DCAEMicroservice\" INVALID, Not FOUND\n"
                    + "  \"entry org.onap.domain.pmsh.PMSHControlLoopDefinition\" INVALID, item has status INVALID\n"
                    + "    \"entry org.onap.domain.pmsh.DCAEMicroservice\" INVALID, Not FOUND\n";

    private static final String CONTROLLOOP_DEFINITION_NOT_FOUND = "\"ControlLoops\" INVALID, item has status INVALID\n"
            + "  \"entry org.onap.domain.PMSHControlLoopDefinition\" INVALID, item has status INVALID\n"
            + "    item \"ControlLoop\" value \"org.onap.domain.PMSHControlLoopDefinition\" INVALID,"
            + " Commissioned control loop definition not FOUND\n"
            + "  \"entry org.onap.domain.PMSHControlLoopDefinition\" INVALID, item has status INVALID\n"
            + "    item \"ControlLoop\" value \"org.onap.domain.PMSHControlLoopDefinition\" INVALID,"
            + " Commissioned control loop definition not FOUND\n";

    private static ToscaServiceTemplate serviceTemplate = new ToscaServiceTemplate();

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_TEMPLATE_YAML);
    }

    @Test
    void testIntanceResponses() throws Exception {
        var participantProvider = Mockito.mock(ParticipantProvider.class);
        var clProvider = mock(ControlLoopProvider.class);
        var supervisionHandler = mock(SupervisionHandler.class);
        var commissioningProvider = mock(CommissioningProvider.class);

        when(commissioningProvider.getAllToscaServiceTemplate()).thenReturn(List.of(serviceTemplate));
        when(commissioningProvider.getToscaServiceTemplate(ID_NAME, ID_VERSION)).thenReturn(serviceTemplate);

        var instantiationProvider = new ControlLoopInstantiationProvider(clProvider, commissioningProvider,
                supervisionHandler, participantProvider);
        var instancePropertyList = instantiationProvider.createInstanceProperties(serviceTemplate);
        assertNull(instancePropertyList.getErrorDetails());
        var id = new ToscaConceptIdentifier(ID_NAME, ID_VERSION);
        assertEquals(id, instancePropertyList.getAffectedInstanceProperties().get(0));

        ControlLoops controlLoops =
                InstantiationUtils.getControlLoopsFromResource(CL_INSTANTIATION_CREATE_JSON, "Crud");
        var controlLoop = controlLoops.getControlLoopList().get(0);
        controlLoop.setName(ID_NAME);
        controlLoop.setVersion(ID_VERSION);
        when(clProvider.getControlLoops(ID_NAME, ID_VERSION)).thenReturn(List.of(controlLoop));

        var instanceOrderState = instantiationProvider.getInstantiationOrderState(ID_NAME, ID_VERSION);
        assertEquals(ControlLoopOrderedState.UNINITIALISED, instanceOrderState.getOrderedState());
        assertEquals(ID_NAME, instanceOrderState.getControlLoopIdentifierList().get(0).getName());

        when(clProvider.findControlLoop(ID_NAME, ID_VERSION)).thenReturn(Optional.of(controlLoop));
        when(clProvider.deleteControlLoop(ID_NAME, ID_VERSION)).thenReturn(controlLoop);

        var instanceResponse = instantiationProvider.deleteInstanceProperties(ID_NAME, ID_VERSION);
        assertEquals(ID_NAME, instanceResponse.getAffectedControlLoops().get(0).getName());

    }

    @Test
    void testInstantiationCrud() throws Exception {
        var participantProvider = Mockito.mock(ParticipantProvider.class);
        var participants = CommonTestData.createParticipants();
        when(participantProvider.getParticipants()).thenReturn(participants);

        var commissioningProvider = mock(CommissioningProvider.class);
        var toscaNodeTemplate1 = new ToscaNodeTemplate();
        toscaNodeTemplate1.setName("org.onap.domain.pmsh.PMSH_MonitoringPolicyControlLoopElement");
        toscaNodeTemplate1.setVersion("1.2.3");
        when(commissioningProvider.getControlLoopDefinitions(anyString(), anyString()))
                .thenReturn(List.of(toscaNodeTemplate1));

        var toscaNodeTemplate2 = new ToscaNodeTemplate();
        toscaNodeTemplate2.setName("org.onap.domain.pmsh.PMSH_OperationalPolicyControlLoopElement");
        toscaNodeTemplate2.setVersion("1.2.3");
        var toscaNodeTemplate3 = new ToscaNodeTemplate();
        toscaNodeTemplate3.setName("org.onap.domain.pmsh.PMSH_CDS_ControlLoopElement");
        toscaNodeTemplate3.setVersion("1.2.3");
        var toscaNodeTemplate4 = new ToscaNodeTemplate();
        toscaNodeTemplate4.setName("org.onap.domain.pmsh.PMSH_DCAEMicroservice");
        toscaNodeTemplate4.setVersion("1.2.3");

        when(commissioningProvider.getControlLoopElementDefinitions(toscaNodeTemplate1))
                .thenReturn(List.of(toscaNodeTemplate1, toscaNodeTemplate2, toscaNodeTemplate3, toscaNodeTemplate4));

        var supervisionHandler = mock(SupervisionHandler.class);
        var clProvider = mock(ControlLoopProvider.class);
        var instantiationProvider = new ControlLoopInstantiationProvider(clProvider, commissioningProvider,
                supervisionHandler, participantProvider);
        ControlLoops controlLoopsCreate =
                InstantiationUtils.getControlLoopsFromResource(CL_INSTANTIATION_CREATE_JSON, "Crud");
        InstantiationResponse instantiationResponse = instantiationProvider.createControlLoops(controlLoopsCreate);
        InstantiationUtils.assertInstantiationResponse(instantiationResponse, controlLoopsCreate);

        verify(clProvider).saveControlLoops(controlLoopsCreate.getControlLoopList());

        for (var controlLoop : controlLoopsCreate.getControlLoopList()) {
            when(clProvider.getControlLoops(controlLoop.getName(), controlLoop.getVersion()))
                    .thenReturn(List.of(controlLoop));

            ControlLoops controlLoopsGet =
                    instantiationProvider.getControlLoops(controlLoop.getName(), controlLoop.getVersion());
            assertThat(controlLoopsGet.getControlLoopList()).hasSize(1);
            assertThat(controlLoop).isEqualTo(controlLoopsGet.getControlLoopList().get(0));
        }

        ControlLoops controlLoopsUpdate =
                InstantiationUtils.getControlLoopsFromResource(CL_INSTANTIATION_UPDATE_JSON, "Crud");

        instantiationResponse = instantiationProvider.updateControlLoops(controlLoopsUpdate);
        InstantiationUtils.assertInstantiationResponse(instantiationResponse, controlLoopsUpdate);

        verify(clProvider).saveControlLoops(controlLoopsUpdate.getControlLoopList());

        for (var controlLoop : controlLoopsUpdate.getControlLoopList()) {
            when(clProvider.findControlLoop(controlLoop.getKey().asIdentifier())).thenReturn(Optional.of(controlLoop));
            when(clProvider.findControlLoop(controlLoop.getName(), controlLoop.getVersion()))
                    .thenReturn(Optional.of(controlLoop));
            when(clProvider.deleteControlLoop(controlLoop.getName(), controlLoop.getVersion())).thenReturn(controlLoop);
        }

        InstantiationCommand instantiationCommand =
                InstantiationUtils.getInstantiationCommandFromResource(CL_INSTANTIATION_CHANGE_STATE_JSON, "Crud");
        instantiationResponse = instantiationProvider.issueControlLoopCommand(instantiationCommand);
        InstantiationUtils.assertInstantiationResponse(instantiationResponse, instantiationCommand);

        verify(supervisionHandler).triggerControlLoopSupervision(instantiationCommand.getControlLoopIdentifierList());

        // in order to delete a controlLoop the state must be UNINITIALISED
        controlLoopsCreate.getControlLoopList().forEach(cl -> cl.setState(ControlLoopState.UNINITIALISED));
        instantiationProvider.updateControlLoops(controlLoopsCreate);

        for (ControlLoop controlLoop : controlLoopsCreate.getControlLoopList()) {
            instantiationProvider.deleteControlLoop(controlLoop.getName(), controlLoop.getVersion());

            verify(clProvider).deleteControlLoop(controlLoop.getName(), controlLoop.getVersion());
        }
    }

    @Test
    void testInstantiationDelete() throws Exception {

        ControlLoops controlLoops =
                InstantiationUtils.getControlLoopsFromResource(CL_INSTANTIATION_CREATE_JSON, "Delete");

        ControlLoop controlLoop0 = controlLoops.getControlLoopList().get(0);
        var participantProvider = Mockito.mock(ParticipantProvider.class);
        var clProvider = mock(ControlLoopProvider.class);
        var supervisionHandler = mock(SupervisionHandler.class);
        var commissioningProvider = mock(CommissioningProvider.class);

        var instantiationProvider = new ControlLoopInstantiationProvider(clProvider, commissioningProvider,
                supervisionHandler, participantProvider);

        assertThatThrownBy(
                () -> instantiationProvider.deleteControlLoop(controlLoop0.getName(), controlLoop0.getVersion()))
                        .hasMessageMatching(CONTROL_LOOP_NOT_FOUND);

        for (ControlLoopState state : ControlLoopState.values()) {
            if (!ControlLoopState.UNINITIALISED.equals(state)) {
                assertThatDeleteThrownBy(controlLoops, state);
            }
        }
        controlLoop0.setState(ControlLoopState.UNINITIALISED);

        for (ControlLoop controlLoop : controlLoops.getControlLoopList()) {
            when(clProvider.findControlLoop(controlLoop.getName(), controlLoop.getVersion()))
                    .thenReturn(Optional.of(controlLoop));
            when(clProvider.deleteControlLoop(controlLoop.getName(), controlLoop.getVersion())).thenReturn(controlLoop);

            instantiationProvider.deleteControlLoop(controlLoop.getName(), controlLoop.getVersion());
        }
    }

    private void assertThatDeleteThrownBy(ControlLoops controlLoops, ControlLoopState state) throws Exception {
        ControlLoop controlLoop = controlLoops.getControlLoopList().get(0);
        controlLoop.setState(state);
        var participantProvider = Mockito.mock(ParticipantProvider.class);
        var clProvider = mock(ControlLoopProvider.class);
        var supervisionHandler = mock(SupervisionHandler.class);
        var commissioningProvider = mock(CommissioningProvider.class);

        var instantiationProvider = new ControlLoopInstantiationProvider(clProvider, commissioningProvider,
                supervisionHandler, participantProvider);

        when(clProvider.findControlLoop(controlLoop.getName(), controlLoop.getVersion()))
                .thenReturn(Optional.of(controlLoop));

        assertThatThrownBy(
                () -> instantiationProvider.deleteControlLoop(controlLoop.getName(), controlLoop.getVersion()))
                        .hasMessageMatching(String.format(DELETE_BAD_REQUEST, state));
    }

    @Test
    void testCreateControlLoops_NoDuplicates() throws Exception {
        var commissioningProvider = mock(CommissioningProvider.class);

        var toscaNodeTemplate1 = new ToscaNodeTemplate();
        toscaNodeTemplate1.setName("org.onap.domain.pmsh.PMSH_MonitoringPolicyControlLoopElement");
        toscaNodeTemplate1.setVersion("1.2.3");
        when(commissioningProvider.getControlLoopDefinitions(anyString(), anyString()))
                .thenReturn(List.of(toscaNodeTemplate1));

        var toscaNodeTemplate2 = new ToscaNodeTemplate();
        toscaNodeTemplate2.setName("org.onap.domain.pmsh.PMSH_OperationalPolicyControlLoopElement");
        toscaNodeTemplate2.setVersion("1.2.3");
        var toscaNodeTemplate3 = new ToscaNodeTemplate();
        toscaNodeTemplate3.setName("org.onap.domain.pmsh.PMSH_CDS_ControlLoopElement");
        toscaNodeTemplate3.setVersion("1.2.3");
        var toscaNodeTemplate4 = new ToscaNodeTemplate();
        toscaNodeTemplate4.setName("org.onap.domain.pmsh.PMSH_DCAEMicroservice");
        toscaNodeTemplate4.setVersion("1.2.3");

        when(commissioningProvider.getControlLoopElementDefinitions(toscaNodeTemplate1))
                .thenReturn(List.of(toscaNodeTemplate1, toscaNodeTemplate2, toscaNodeTemplate3, toscaNodeTemplate4));

        ControlLoops controlLoopsCreate =
                InstantiationUtils.getControlLoopsFromResource(CL_INSTANTIATION_CREATE_JSON, "NoDuplicates");

        var clProvider = mock(ControlLoopProvider.class);
        var participantProvider = Mockito.mock(ParticipantProvider.class);
        var supervisionHandler = mock(SupervisionHandler.class);

        var instantiationProvider = new ControlLoopInstantiationProvider(clProvider, commissioningProvider,
                supervisionHandler, participantProvider);

        InstantiationResponse instantiationResponse = instantiationProvider.createControlLoops(controlLoopsCreate);
        InstantiationUtils.assertInstantiationResponse(instantiationResponse, controlLoopsCreate);

        when(clProvider.findControlLoop(controlLoopsCreate.getControlLoopList().get(0).getKey().asIdentifier()))
                .thenReturn(Optional.of(controlLoopsCreate.getControlLoopList().get(0)));

        assertThatThrownBy(() -> instantiationProvider.createControlLoops(controlLoopsCreate)).hasMessageMatching(
                controlLoopsCreate.getControlLoopList().get(0).getKey().asIdentifier() + " already defined");
    }

    @Test
    void testCreateControlLoops_CommissionedClElementNotFound() throws Exception {
        var toscaNodeTemplate1 = new ToscaNodeTemplate();
        toscaNodeTemplate1.setName("org.onap.domain.pmsh.PMSH_MonitoringPolicyControlLoopElement");
        toscaNodeTemplate1.setVersion("1.2.3");

        var toscaNodeTemplate2 = new ToscaNodeTemplate();
        toscaNodeTemplate2.setName("org.onap.domain.pmsh.PMSH_OperationalPolicyControlLoopElement");
        toscaNodeTemplate2.setVersion("1.2.3");
        var toscaNodeTemplate3 = new ToscaNodeTemplate();
        toscaNodeTemplate3.setName("org.onap.domain.pmsh.PMSH_CDS_ControlLoopElement");
        toscaNodeTemplate3.setVersion("1.2.3");
        var commissioningProvider = mock(CommissioningProvider.class);
        ControlLoops controlLoops = InstantiationUtils
                .getControlLoopsFromResource(CL_INSTANTIATION_DEFINITION_NAME_NOT_FOUND_JSON, "ClElementNotFound");

        when(commissioningProvider.getControlLoopDefinitions(
                controlLoops.getControlLoopList().get(0).getDefinition().getName(),
                controlLoops.getControlLoopList().get(0).getDefinition().getVersion()))
                        .thenReturn(List.of(toscaNodeTemplate1));

        when(commissioningProvider.getControlLoopElementDefinitions(toscaNodeTemplate1))
                .thenReturn(List.of(toscaNodeTemplate1, toscaNodeTemplate2, toscaNodeTemplate3));

        var clProvider = mock(ControlLoopProvider.class);
        var participantProvider = mock(ParticipantProvider.class);
        var supervisionHandler = mock(SupervisionHandler.class);
        var provider = new ControlLoopInstantiationProvider(clProvider, commissioningProvider, supervisionHandler,
                participantProvider);

        assertThatThrownBy(() -> provider.createControlLoops(controlLoops))
                .hasMessageMatching(CONTROLLOOP_ELEMENT_NAME_NOT_FOUND);

        assertThatThrownBy(() -> provider.updateControlLoops(controlLoops))
                .hasMessageMatching(CONTROLLOOP_ELEMENT_NAME_NOT_FOUND);
    }

    @Test
    void testCreateControlLoops_CommissionedClNotFound() throws Exception {
        ControlLoops controlLoops = InstantiationUtils
                .getControlLoopsFromResource(CL_INSTANTIATION_CONTROLLOOP_DEFINITION_NOT_FOUND_JSON, "ClNotFound");

        var participantProvider = Mockito.mock(ParticipantProvider.class);
        var clProvider = mock(ControlLoopProvider.class);
        var supervisionHandler = mock(SupervisionHandler.class);
        var commissioningProvider = mock(CommissioningProvider.class);
        var provider = new ControlLoopInstantiationProvider(clProvider, commissioningProvider, supervisionHandler,
                participantProvider);

        assertThatThrownBy(() -> provider.createControlLoops(controlLoops))
                .hasMessageMatching(CONTROLLOOP_DEFINITION_NOT_FOUND);

        assertThatThrownBy(() -> provider.updateControlLoops(controlLoops))
                .hasMessageMatching(CONTROLLOOP_DEFINITION_NOT_FOUND);
    }

    @Test
    void testIssueControlLoopCommand_OrderedStateInvalid() throws ControlLoopRuntimeException, IOException {
        var participantProvider = Mockito.mock(ParticipantProvider.class);
        var clProvider = mock(ControlLoopProvider.class);
        var supervisionHandler = mock(SupervisionHandler.class);
        var commissioningProvider = mock(CommissioningProvider.class);
        var instantiationProvider = new ControlLoopInstantiationProvider(clProvider, commissioningProvider,
                supervisionHandler, participantProvider);
        assertThatThrownBy(() -> instantiationProvider.issueControlLoopCommand(new InstantiationCommand()))
                .hasMessageMatching(ORDERED_STATE_INVALID);
    }
}
