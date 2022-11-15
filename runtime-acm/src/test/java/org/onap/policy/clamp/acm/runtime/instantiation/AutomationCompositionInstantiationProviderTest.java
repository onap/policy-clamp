/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation.
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

package org.onap.policy.clamp.acm.runtime.instantiation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.onap.policy.clamp.acm.runtime.util.CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionHandler;
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionRuntimeException;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationCommand;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationResponse;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ProviderUtils;
import org.onap.policy.clamp.models.acm.persistence.provider.ServiceTemplateProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.simple.concepts.JpaToscaServiceTemplate;

/**
 * Class to perform unit test of {@link AutomationCompositionInstantiationProvider}}.
 *
 */
class AutomationCompositionInstantiationProviderTest {
    private static final String AC_INSTANTIATION_CREATE_JSON =
            "src/test/resources/rest/acm/AutomationCompositions.json";
    private static final String AC_INSTANTIATION_UPDATE_JSON =
            "src/test/resources/rest/acm/AutomationCompositionsUpdate.json";
    private static final String AC_INSTANTIATION_CHANGE_STATE_JSON = "src/test/resources/rest/acm/PassiveCommand.json";
    private static final String AC_INSTANTIATION_DEFINITION_NAME_NOT_FOUND_JSON =
            "src/test/resources/rest/acm/AutomationCompositionElementsNotFound.json";
    private static final String AC_INSTANTIATION_AC_DEFINITION_NOT_FOUND_JSON =
            "src/test/resources/rest/acm/AutomationCompositionsNotFound.json";
    private static final String AUTOMATION_COMPOSITION_NOT_FOUND = "Automation composition not found";
    private static final String DELETE_BAD_REQUEST = "Automation composition state is still %s";
    private static final String ORDERED_STATE_INVALID = "ordered state invalid or not specified on command";
    private static final String AC_ELEMENT_NAME_NOT_FOUND =
            "\"AutomationCompositions\" INVALID, item has status INVALID\n"
                    + " {2}\"entry org.onap.domain.pmsh.PMSHAutomationCompositionDefinition\" "
                    + "INVALID, item has status INVALID\n"
                    + " {4}\"entry org.onap.domain.pmsh.DCAEMicroservice\" INVALID, Not found\n"
                    + " {2}\"entry org.onap.domain.pmsh.PMSHAutomationCompositionDefinition\" "
                    + "INVALID, item has status INVALID\n"
                    + " {4}\"entry org.onap.domain.pmsh.DCAEMicroservice\" INVALID, Not found\n";

    private static final String AC_DEFINITION_NOT_FOUND =
            "\"AutomationCompositions\" INVALID, item has status INVALID\n"
                    + "  item \"ServiceTemplate\" value \"\" INVALID,"
                    + " Commissioned automation composition definition not found\n";

    private static ToscaServiceTemplate serviceTemplate = new ToscaServiceTemplate();

    @BeforeAll
    public static void setUpBeforeClass() {
        serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        var jpa =
                ProviderUtils.getJpaAndValidate(serviceTemplate, JpaToscaServiceTemplate::new, "toscaServiceTemplate");
        serviceTemplate = jpa.toAuthorative();
    }

    @Test
    void testInstantiationCrud() throws Exception {
        var participantProvider = Mockito.mock(ParticipantProvider.class);
        var participants = CommonTestData.createParticipants();
        when(participantProvider.getParticipants()).thenReturn(participants);

        var serviceTemplateProvider = mock(ServiceTemplateProvider.class);
        when(serviceTemplateProvider.getAllServiceTemplates()).thenReturn(List.of(serviceTemplate));
        var supervisionHandler = mock(SupervisionHandler.class);
        var acProvider = mock(AutomationCompositionProvider.class);
        var instantiationProvider = new AutomationCompositionInstantiationProvider(acProvider, supervisionHandler,
                participantProvider, serviceTemplateProvider);
        var automationCompositionsCreate =
                InstantiationUtils.getAutomationCompositionsFromResource(AC_INSTANTIATION_CREATE_JSON, "Crud");
        var instantiationResponse = instantiationProvider.createAutomationCompositions(automationCompositionsCreate);
        InstantiationUtils.assertInstantiationResponse(instantiationResponse, automationCompositionsCreate);

        verify(acProvider).saveAutomationCompositions(automationCompositionsCreate.getAutomationCompositionList());

        for (var automationComposition : automationCompositionsCreate.getAutomationCompositionList()) {
            when(acProvider.getAutomationCompositions(automationComposition.getName(),
                    automationComposition.getVersion())).thenReturn(List.of(automationComposition));

            var automationCompositionsGet = instantiationProvider
                    .getAutomationCompositions(automationComposition.getName(), automationComposition.getVersion());
            assertThat(automationCompositionsGet.getAutomationCompositionList()).hasSize(1);
            assertThat(automationComposition)
                    .isEqualTo(automationCompositionsGet.getAutomationCompositionList().get(0));
        }

        var automationCompositionsUpdate =
                InstantiationUtils.getAutomationCompositionsFromResource(AC_INSTANTIATION_UPDATE_JSON, "Crud");

        instantiationResponse = instantiationProvider.updateAutomationCompositions(automationCompositionsUpdate);
        InstantiationUtils.assertInstantiationResponse(instantiationResponse, automationCompositionsUpdate);

        verify(acProvider).saveAutomationCompositions(automationCompositionsUpdate.getAutomationCompositionList());

        for (var automationComposition : automationCompositionsUpdate.getAutomationCompositionList()) {
            when(acProvider.findAutomationComposition(automationComposition.getKey().asIdentifier()))
                    .thenReturn(Optional.of(automationComposition));
            when(acProvider.findAutomationComposition(automationComposition.getName(),
                    automationComposition.getVersion())).thenReturn(Optional.of(automationComposition));
            when(acProvider.deleteAutomationComposition(automationComposition.getName(),
                    automationComposition.getVersion())).thenReturn(automationComposition);
        }

        var instantiationCommand =
                InstantiationUtils.getInstantiationCommandFromResource(AC_INSTANTIATION_CHANGE_STATE_JSON, "Crud");
        instantiationResponse = instantiationProvider.issueAutomationCompositionCommand(instantiationCommand);
        InstantiationUtils.assertInstantiationResponse(instantiationResponse, instantiationCommand);

        verify(supervisionHandler)
                .triggerAutomationCompositionSupervision(instantiationCommand.getAutomationCompositionIdentifierList());

        // in order to delete a automationComposition the state must be UNINITIALISED
        automationCompositionsCreate.getAutomationCompositionList()
                .forEach(ac -> ac.setState(AutomationCompositionState.UNINITIALISED));
        instantiationProvider.updateAutomationCompositions(automationCompositionsCreate);

        for (AutomationComposition automationComposition : automationCompositionsCreate
                .getAutomationCompositionList()) {
            instantiationProvider.deleteAutomationComposition(automationComposition.getName(),
                    automationComposition.getVersion());

            verify(acProvider).deleteAutomationComposition(automationComposition.getName(),
                    automationComposition.getVersion());
        }
    }

    @Test
    void testInstantiationDelete() throws Exception {

        AutomationCompositions automationCompositions =
                InstantiationUtils.getAutomationCompositionsFromResource(AC_INSTANTIATION_CREATE_JSON, "Delete");

        AutomationComposition automationComposition0 = automationCompositions.getAutomationCompositionList().get(0);
        var participantProvider = Mockito.mock(ParticipantProvider.class);
        var acProvider = mock(AutomationCompositionProvider.class);
        var supervisionHandler = mock(SupervisionHandler.class);
        var serviceTemplateProvider = mock(ServiceTemplateProvider.class);

        var instantiationProvider = new AutomationCompositionInstantiationProvider(acProvider, supervisionHandler,
                participantProvider, serviceTemplateProvider);

        assertThatThrownBy(() -> instantiationProvider.deleteAutomationComposition(automationComposition0.getName(),
                automationComposition0.getVersion())).hasMessageMatching(AUTOMATION_COMPOSITION_NOT_FOUND);

        for (AutomationCompositionState state : AutomationCompositionState.values()) {
            if (!AutomationCompositionState.UNINITIALISED.equals(state)) {
                assertThatDeleteThrownBy(automationCompositions, state);
            }
        }
        automationComposition0.setState(AutomationCompositionState.UNINITIALISED);

        for (AutomationComposition automationComposition : automationCompositions.getAutomationCompositionList()) {
            when(acProvider.findAutomationComposition(automationComposition.getName(),
                    automationComposition.getVersion())).thenReturn(Optional.of(automationComposition));
            when(acProvider.deleteAutomationComposition(automationComposition.getName(),
                    automationComposition.getVersion())).thenReturn(automationComposition);

            instantiationProvider.deleteAutomationComposition(automationComposition.getName(),
                    automationComposition.getVersion());
        }
    }

    private void assertThatDeleteThrownBy(AutomationCompositions automationCompositions,
            AutomationCompositionState state) throws Exception {
        AutomationComposition automationComposition = automationCompositions.getAutomationCompositionList().get(0);
        automationComposition.setState(state);
        var participantProvider = Mockito.mock(ParticipantProvider.class);
        var acProvider = mock(AutomationCompositionProvider.class);
        var supervisionHandler = mock(SupervisionHandler.class);
        var serviceTemplateProvider = mock(ServiceTemplateProvider.class);

        var instantiationProvider = new AutomationCompositionInstantiationProvider(acProvider, supervisionHandler,
                participantProvider, serviceTemplateProvider);

        when(acProvider.findAutomationComposition(automationComposition.getName(), automationComposition.getVersion()))
                .thenReturn(Optional.of(automationComposition));

        assertThatThrownBy(() -> instantiationProvider.deleteAutomationComposition(automationComposition.getName(),
                automationComposition.getVersion())).hasMessageMatching(String.format(DELETE_BAD_REQUEST, state));
    }

    @Test
    void testCreateAutomationCompositions_NoDuplicates() throws Exception {
        var serviceTemplateProvider = mock(ServiceTemplateProvider.class);
        when(serviceTemplateProvider.getAllServiceTemplates()).thenReturn(List.of(serviceTemplate));

        AutomationCompositions automationCompositionsCreate =
                InstantiationUtils.getAutomationCompositionsFromResource(AC_INSTANTIATION_CREATE_JSON, "NoDuplicates");

        var acProvider = mock(AutomationCompositionProvider.class);
        var participantProvider = Mockito.mock(ParticipantProvider.class);
        var supervisionHandler = mock(SupervisionHandler.class);

        var instantiationProvider = new AutomationCompositionInstantiationProvider(acProvider, supervisionHandler,
                participantProvider, serviceTemplateProvider);

        InstantiationResponse instantiationResponse =
                instantiationProvider.createAutomationCompositions(automationCompositionsCreate);
        InstantiationUtils.assertInstantiationResponse(instantiationResponse, automationCompositionsCreate);

        when(acProvider.findAutomationComposition(
                automationCompositionsCreate.getAutomationCompositionList().get(0).getKey().asIdentifier()))
                        .thenReturn(Optional.of(automationCompositionsCreate.getAutomationCompositionList().get(0)));

        assertThatThrownBy(() -> instantiationProvider.createAutomationCompositions(automationCompositionsCreate))
                .hasMessageMatching(
                        automationCompositionsCreate.getAutomationCompositionList().get(0).getKey().asIdentifier()
                                + " already defined");
    }

    @Test
    void testCreateAutomationCompositions_CommissionedAcElementNotFound() throws Exception {
        var serviceTemplateProvider = mock(ServiceTemplateProvider.class);
        when(serviceTemplateProvider.getAllServiceTemplates()).thenReturn(List.of(serviceTemplate));
        AutomationCompositions automationCompositions = InstantiationUtils.getAutomationCompositionsFromResource(
                AC_INSTANTIATION_DEFINITION_NAME_NOT_FOUND_JSON, "AcElementNotFound");

        var acProvider = mock(AutomationCompositionProvider.class);
        var participantProvider = mock(ParticipantProvider.class);
        var supervisionHandler = mock(SupervisionHandler.class);
        var provider = new AutomationCompositionInstantiationProvider(acProvider, supervisionHandler,
                participantProvider, serviceTemplateProvider);

        assertThatThrownBy(() -> provider.createAutomationCompositions(automationCompositions))
                .hasMessageMatching(AC_ELEMENT_NAME_NOT_FOUND);

        assertThatThrownBy(() -> provider.updateAutomationCompositions(automationCompositions))
                .hasMessageMatching(AC_ELEMENT_NAME_NOT_FOUND);
    }

    @Test
    void testCreateAutomationCompositions_CommissionedAcNotFound() throws Exception {
        AutomationCompositions automationCompositions = InstantiationUtils
                .getAutomationCompositionsFromResource(AC_INSTANTIATION_AC_DEFINITION_NOT_FOUND_JSON, "AcNotFound");

        var participantProvider = Mockito.mock(ParticipantProvider.class);
        var acProvider = mock(AutomationCompositionProvider.class);
        var supervisionHandler = mock(SupervisionHandler.class);
        var serviceTemplateProvider = mock(ServiceTemplateProvider.class);
        var provider = new AutomationCompositionInstantiationProvider(acProvider, supervisionHandler,
                participantProvider, serviceTemplateProvider);

        assertThatThrownBy(() -> provider.createAutomationCompositions(automationCompositions))
                .hasMessageMatching(AC_DEFINITION_NOT_FOUND);

        assertThatThrownBy(() -> provider.updateAutomationCompositions(automationCompositions))
                .hasMessageMatching(AC_DEFINITION_NOT_FOUND);
    }

    @Test
    void testIssueAutomationCompositionCommand_OrderedStateInvalid() throws AutomationCompositionRuntimeException {
        var participantProvider = Mockito.mock(ParticipantProvider.class);
        var acProvider = mock(AutomationCompositionProvider.class);
        var supervisionHandler = mock(SupervisionHandler.class);
        var serviceTemplateProvider = mock(ServiceTemplateProvider.class);
        var instantiationProvider = new AutomationCompositionInstantiationProvider(acProvider, supervisionHandler,
                participantProvider, serviceTemplateProvider);
        assertThatThrownBy(() -> instantiationProvider.issueAutomationCompositionCommand(new InstantiationCommand()))
                .hasMessageMatching(ORDERED_STATE_INVALID);
    }
}
