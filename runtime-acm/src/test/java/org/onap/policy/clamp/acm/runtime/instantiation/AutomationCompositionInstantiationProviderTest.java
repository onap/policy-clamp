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
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionHandler;
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionException;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionRuntimeException;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationCommand;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ProviderUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.simple.concepts.JpaToscaServiceTemplate;

/**
 * Class to perform unit test of {@link AutomationCompositionInstantiationProvider}}.
 *
 */
class AutomationCompositionInstantiationProviderTest {
    private static final String AC_INSTANTIATION_CREATE_JSON = "src/test/resources/rest/acm/AutomationComposition.json";
    private static final String AC_INSTANTIATION_UPDATE_JSON =
            "src/test/resources/rest/acm/AutomationCompositionUpdate.json";
    private static final String AC_INSTANTIATION_CHANGE_STATE_JSON = "src/test/resources/rest/acm/PassiveCommand.json";
    private static final String AC_INSTANTIATION_DEFINITION_NAME_NOT_FOUND_JSON =
            "src/test/resources/rest/acm/AutomationCompositionElementsNotFound.json";
    private static final String AC_INSTANTIATION_AC_DEFINITION_NOT_FOUND_JSON =
            "src/test/resources/rest/acm/AutomationCompositionNotFound.json";
    private static final String AUTOMATION_COMPOSITION_NOT_FOUND = "Automation composition not found";
    private static final String DELETE_BAD_REQUEST = "Automation composition state is still %s";
    private static final String ORDERED_STATE_INVALID = "ordered state invalid or not specified on command";
    private static final String AC_ELEMENT_NAME_NOT_FOUND =
            "\"AutomationComposition\" INVALID, item has status INVALID\n"
            + "  \"entry PMSHInstance0AcElementNotFound\" INVALID, item has status INVALID\n"
            + "    \"entry org.onap.domain.pmsh.DCAEMicroservice\" INVALID, Not found\n"
            + "    \"entry org.onap.domain.pmsh.PMSH_MonitoringPolicyAutomationCompositionElement\""
            + " INVALID, Not found\n";
    private static final String AC_DEFINITION_NOT_FOUND =
            "\"AutomationComposition\" INVALID, item has status INVALID\n"
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
    void testInstantiationCrud() throws AutomationCompositionException {
        var participantProvider = Mockito.mock(ParticipantProvider.class);
        var participants = CommonTestData.createParticipants();
        when(participantProvider.getParticipants()).thenReturn(participants);

        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var compositionId = UUID.randomUUID();
        when(acDefinitionProvider.findAcDefinition(compositionId)).thenReturn(Optional.of(serviceTemplate));
        var supervisionHandler = mock(SupervisionHandler.class);
        var acProvider = mock(AutomationCompositionProvider.class);
        var instantiationProvider = new AutomationCompositionInstantiationProvider(acProvider, supervisionHandler,
                participantProvider, acDefinitionProvider);
        var automationCompositionCreate =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Crud");
        automationCompositionCreate.setCompositionId(compositionId);
        when(acProvider.saveAutomationComposition(automationCompositionCreate)).thenReturn(automationCompositionCreate);

        var instantiationResponse = instantiationProvider.createAutomationComposition(automationCompositionCreate);
        InstantiationUtils.assertInstantiationResponse(instantiationResponse, automationCompositionCreate);

        verify(acProvider).saveAutomationComposition(automationCompositionCreate);

        when(acProvider.getAutomationCompositions(automationCompositionCreate.getName(),
                automationCompositionCreate.getVersion())).thenReturn(List.of(automationCompositionCreate));

        var automationCompositionsGet = instantiationProvider.getAutomationCompositions(
                automationCompositionCreate.getName(), automationCompositionCreate.getVersion());
        assertThat(automationCompositionCreate)
                .isEqualTo(automationCompositionsGet.getAutomationCompositionList().get(0));

        var automationCompositionUpdate =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_UPDATE_JSON, "Crud");
        automationCompositionUpdate.setCompositionId(compositionId);

        instantiationResponse = instantiationProvider.updateAutomationComposition(automationCompositionUpdate);
        InstantiationUtils.assertInstantiationResponse(instantiationResponse, automationCompositionUpdate);

        verify(acProvider).saveAutomationComposition(automationCompositionUpdate);

        when(acProvider.findAutomationComposition(automationCompositionUpdate.getKey().asIdentifier()))
                .thenReturn(Optional.of(automationCompositionUpdate));
        when(acProvider.findAutomationComposition(automationCompositionUpdate.getName(),
                automationCompositionUpdate.getVersion())).thenReturn(Optional.of(automationCompositionUpdate));
        when(acProvider.deleteAutomationComposition(automationCompositionUpdate.getName(),
                automationCompositionUpdate.getVersion())).thenReturn(automationCompositionUpdate);

        var instantiationCommand =
                InstantiationUtils.getInstantiationCommandFromResource(AC_INSTANTIATION_CHANGE_STATE_JSON, "Crud");
        instantiationResponse = instantiationProvider.issueAutomationCompositionCommand(instantiationCommand);
        InstantiationUtils.assertInstantiationResponse(instantiationResponse, instantiationCommand);

        verify(supervisionHandler).triggerAutomationCompositionSupervision(automationCompositionUpdate);

        // in order to delete a automationComposition the state must be UNINITIALISED
        automationCompositionCreate.setState(AutomationCompositionState.UNINITIALISED);
        instantiationProvider.updateAutomationComposition(automationCompositionCreate);

        instantiationProvider.deleteAutomationComposition(automationCompositionCreate.getName(),
                automationCompositionCreate.getVersion());

        verify(acProvider).deleteAutomationComposition(automationCompositionCreate.getName(),
                automationCompositionCreate.getVersion());
    }

    @Test
    void testInstantiationDelete() {
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Delete");

        var participantProvider = Mockito.mock(ParticipantProvider.class);
        var acProvider = mock(AutomationCompositionProvider.class);
        var supervisionHandler = mock(SupervisionHandler.class);
        var acDefinitionProvider = mock(AcDefinitionProvider.class);

        var instantiationProvider = new AutomationCompositionInstantiationProvider(acProvider, supervisionHandler,
                participantProvider, acDefinitionProvider);

        assertThatThrownBy(() -> instantiationProvider.deleteAutomationComposition(automationComposition.getName(),
                automationComposition.getVersion())).hasMessageMatching(AUTOMATION_COMPOSITION_NOT_FOUND);

        for (var state : AutomationCompositionState.values()) {
            if (!AutomationCompositionState.UNINITIALISED.equals(state)) {
                assertThatDeleteThrownBy(automationComposition, state);
            }
        }
        automationComposition.setState(AutomationCompositionState.UNINITIALISED);

        when(acProvider.findAutomationComposition(automationComposition.getName(), automationComposition.getVersion()))
                .thenReturn(Optional.of(automationComposition));
        when(acProvider.deleteAutomationComposition(automationComposition.getName(),
                automationComposition.getVersion())).thenReturn(automationComposition);

        instantiationProvider.deleteAutomationComposition(automationComposition.getName(),
                automationComposition.getVersion());
    }

    private void assertThatDeleteThrownBy(AutomationComposition automationComposition,
            AutomationCompositionState state) {
        automationComposition.setState(state);
        var participantProvider = Mockito.mock(ParticipantProvider.class);
        var acProvider = mock(AutomationCompositionProvider.class);
        var supervisionHandler = mock(SupervisionHandler.class);
        var acDefinitionProvider = mock(AcDefinitionProvider.class);

        var instantiationProvider = new AutomationCompositionInstantiationProvider(acProvider, supervisionHandler,
                participantProvider, acDefinitionProvider);

        when(acProvider.findAutomationComposition(automationComposition.getName(), automationComposition.getVersion()))
                .thenReturn(Optional.of(automationComposition));

        assertThatThrownBy(() -> instantiationProvider.deleteAutomationComposition(automationComposition.getName(),
                automationComposition.getVersion())).hasMessageMatching(String.format(DELETE_BAD_REQUEST, state));
    }

    @Test
    void testCreateAutomationCompositions_NoDuplicates() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var compositionId = UUID.randomUUID();
        when(acDefinitionProvider.findAcDefinition(compositionId)).thenReturn(Optional.of(serviceTemplate));

        var automationCompositionCreate =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "NoDuplicates");
        automationCompositionCreate.setCompositionId(compositionId);

        var acProvider = mock(AutomationCompositionProvider.class);
        when(acProvider.saveAutomationComposition(automationCompositionCreate)).thenReturn(automationCompositionCreate);

        var participantProvider = Mockito.mock(ParticipantProvider.class);
        var supervisionHandler = mock(SupervisionHandler.class);

        var instantiationProvider = new AutomationCompositionInstantiationProvider(acProvider, supervisionHandler,
                participantProvider, acDefinitionProvider);

        var instantiationResponse = instantiationProvider.createAutomationComposition(automationCompositionCreate);
        InstantiationUtils.assertInstantiationResponse(instantiationResponse, automationCompositionCreate);

        when(acProvider.findAutomationComposition(automationCompositionCreate.getKey().asIdentifier()))
                .thenReturn(Optional.of(automationCompositionCreate));

        assertThatThrownBy(() -> instantiationProvider.createAutomationComposition(automationCompositionCreate))
                .hasMessageMatching(automationCompositionCreate.getKey().asIdentifier() + " already defined");
    }

    @Test
    void testCreateAutomationCompositions_CommissionedAcElementNotFound() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var compositionId = UUID.randomUUID();
        when(acDefinitionProvider.findAcDefinition(compositionId)).thenReturn(Optional.of(serviceTemplate));
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(
                AC_INSTANTIATION_DEFINITION_NAME_NOT_FOUND_JSON, "AcElementNotFound");
        automationComposition.setCompositionId(compositionId);

        var acProvider = mock(AutomationCompositionProvider.class);
        var participantProvider = mock(ParticipantProvider.class);
        var supervisionHandler = mock(SupervisionHandler.class);
        var provider = new AutomationCompositionInstantiationProvider(acProvider, supervisionHandler,
                participantProvider, acDefinitionProvider);

        assertThatThrownBy(() -> provider.createAutomationComposition(automationComposition))
                .hasMessageMatching(AC_ELEMENT_NAME_NOT_FOUND);

        assertThatThrownBy(() -> provider.updateAutomationComposition(automationComposition))
                .hasMessageMatching(AC_ELEMENT_NAME_NOT_FOUND);
    }

    @Test
    void testCreateAutomationCompositions_CommissionedAcNotFound() throws Exception {
        var automationComposition = InstantiationUtils
                .getAutomationCompositionFromResource(AC_INSTANTIATION_AC_DEFINITION_NOT_FOUND_JSON, "AcNotFound");

        var participantProvider = Mockito.mock(ParticipantProvider.class);
        var acProvider = mock(AutomationCompositionProvider.class);
        var supervisionHandler = mock(SupervisionHandler.class);
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var provider = new AutomationCompositionInstantiationProvider(acProvider, supervisionHandler,
                participantProvider, acDefinitionProvider);

        assertThatThrownBy(() -> provider.createAutomationComposition(automationComposition))
                .hasMessageMatching(AC_DEFINITION_NOT_FOUND);

        assertThatThrownBy(() -> provider.updateAutomationComposition(automationComposition))
                .hasMessageMatching(AC_DEFINITION_NOT_FOUND);
    }

    @Test
    void testIssueAutomationCompositionCommand_OrderedStateInvalid() throws AutomationCompositionRuntimeException {
        var participantProvider = Mockito.mock(ParticipantProvider.class);
        var acProvider = mock(AutomationCompositionProvider.class);
        var supervisionHandler = mock(SupervisionHandler.class);
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var instantiationProvider = new AutomationCompositionInstantiationProvider(acProvider, supervisionHandler,
                participantProvider, acDefinitionProvider);
        assertThatThrownBy(() -> instantiationProvider.issueAutomationCompositionCommand(new InstantiationCommand()))
                .hasMessageMatching(ORDERED_STATE_INVALID);
    }
}
