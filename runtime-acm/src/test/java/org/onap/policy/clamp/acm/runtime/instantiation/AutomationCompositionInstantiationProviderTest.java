/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation.
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.onap.policy.clamp.acm.runtime.util.CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup;
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionAcHandler;
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.AcInstanceStateUpdate;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.LockOrder;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.SubOrder;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.AcInstanceStateResolver;
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

    private static final String AC_INSTANTIATION_DEFINITION_NAME_NOT_FOUND_JSON =
            "src/test/resources/rest/acm/AutomationCompositionElementsNotFound.json";
    private static final String AC_INSTANTIATION_AC_DEFINITION_NOT_FOUND_JSON =
            "src/test/resources/rest/acm/AutomationCompositionNotFound.json";
    private static final String DELETE_BAD_REQUEST = "Automation composition state is still %s";

    private static final String AC_ELEMENT_NAME_NOT_FOUND = """
            "AutomationComposition" INVALID, item has status INVALID
              "entry PMSHInstance0AcElementNotFound" INVALID, item has status INVALID
                "entry org.onap.domain.pmsh.DCAEMicroservice" INVALID, Not found
                "entry org.onap.domain.pmsh.PMSH_MonitoringPolicyAutomationCompositionElement" INVALID, Not found
            """;
    private static final String AC_DEFINITION_NOT_FOUND = """
            "AutomationComposition" INVALID, item has status INVALID
              item "ServiceTemplate" value "%s" INVALID, Commissioned automation composition definition not found
            """;

    private static final String DO_NOT_MATCH = " do not match with ";

    private static ToscaServiceTemplate serviceTemplate = new ToscaServiceTemplate();

    @BeforeAll
    public static void setUpBeforeClass() {
        serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        var jpa =
                ProviderUtils.getJpaAndValidate(serviceTemplate, JpaToscaServiceTemplate::new, "toscaServiceTemplate");
        serviceTemplate = jpa.toAuthorative();
    }

    @Test
    void testInstantiationCrud() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var compositionId = acDefinition.getCompositionId();
        when(acDefinitionProvider.findAcDefinition(compositionId)).thenReturn(Optional.of(acDefinition));
        when(acDefinitionProvider.getAcDefinition(compositionId)).thenReturn(acDefinition);
        var acProvider = mock(AutomationCompositionProvider.class);
        var supervisionAcHandler = mock(SupervisionAcHandler.class);
        var participantProvider = mock(ParticipantProvider.class);
        var instantiationProvider = new AutomationCompositionInstantiationProvider(acProvider, acDefinitionProvider,
                null, supervisionAcHandler, participantProvider,
                CommonTestData.getTestParamaterGroup());
        var automationCompositionCreate =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Crud");
        automationCompositionCreate.setCompositionId(compositionId);
        when(acProvider.createAutomationComposition(automationCompositionCreate))
                .thenReturn(automationCompositionCreate);

        var instantiationResponse = instantiationProvider.createAutomationComposition(
                automationCompositionCreate.getCompositionId(), automationCompositionCreate);
        InstantiationUtils.assertInstantiationResponse(instantiationResponse, automationCompositionCreate);

        verify(acProvider).createAutomationComposition(automationCompositionCreate);

        when(acProvider.getAutomationCompositions(compositionId, automationCompositionCreate.getName(),
                automationCompositionCreate.getVersion())).thenReturn(List.of(automationCompositionCreate));

        var automationCompositionsGet = instantiationProvider.getAutomationCompositions(compositionId,
                automationCompositionCreate.getName(), automationCompositionCreate.getVersion());
        assertThat(automationCompositionCreate)
                .isEqualTo(automationCompositionsGet.getAutomationCompositionList().get(0));

        var automationCompositionUpdate =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_UPDATE_JSON, "Crud");
        automationCompositionUpdate.setCompositionId(compositionId);
        when(acProvider.getAutomationComposition(automationCompositionUpdate.getInstanceId()))
                .thenReturn(automationCompositionUpdate);
        when(acProvider.updateAutomationComposition(automationCompositionUpdate))
                .thenReturn(automationCompositionUpdate);

        instantiationResponse = instantiationProvider.updateAutomationComposition(
                automationCompositionUpdate.getCompositionId(), automationCompositionUpdate);
        InstantiationUtils.assertInstantiationResponse(instantiationResponse, automationCompositionUpdate);

        verify(acProvider).updateAutomationComposition(automationCompositionUpdate);

        when(acProvider.deleteAutomationComposition(automationCompositionUpdate.getInstanceId()))
                .thenReturn(automationCompositionUpdate);
        doNothing().when(participantProvider).verifyParticipantState(any());
        instantiationProvider.deleteAutomationComposition(automationCompositionCreate.getCompositionId(),
                automationCompositionCreate.getInstanceId());

        verify(supervisionAcHandler).delete(any(), any());
    }

    @Test
    void testInstantiationUpdate() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var compositionId = acDefinition.getCompositionId();
        when(acDefinitionProvider.findAcDefinition(compositionId)).thenReturn(Optional.of(acDefinition));

        var automationCompositionUpdate =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_UPDATE_JSON, "Crud");
        automationCompositionUpdate.setCompositionId(compositionId);
        automationCompositionUpdate.setInstanceId(UUID.randomUUID());
        automationCompositionUpdate.setDeployState(DeployState.DEPLOYED);
        automationCompositionUpdate.setLockState(LockState.LOCKED);
        var acProvider = mock(AutomationCompositionProvider.class);
        var acmFromDb = new AutomationComposition(automationCompositionUpdate);
        when(acProvider.getAutomationComposition(automationCompositionUpdate.getInstanceId())).thenReturn(acmFromDb);
        when(acProvider.updateAutomationComposition(acmFromDb)).thenReturn(acmFromDb);

        var supervisionAcHandler = mock(SupervisionAcHandler.class);
        var participantProvider = mock(ParticipantProvider.class);
        var instantiationProvider = new AutomationCompositionInstantiationProvider(acProvider, acDefinitionProvider,
                new AcInstanceStateResolver(), supervisionAcHandler, participantProvider,
                CommonTestData.getTestParamaterGroup());
        var instantiationResponse = instantiationProvider.updateAutomationComposition(
                automationCompositionUpdate.getCompositionId(), automationCompositionUpdate);

        verify(supervisionAcHandler).update(any());
        verify(acProvider).updateAutomationComposition(acmFromDb);
        InstantiationUtils.assertInstantiationResponse(instantiationResponse, automationCompositionUpdate);

        var elements = new ArrayList<>(automationCompositionUpdate.getElements().values());
        automationCompositionUpdate.getElements().clear();
        for (var element : elements) {
            element.setId(UUID.randomUUID());
            automationCompositionUpdate.getElements().put(element.getId(), element);
        }
        assertThatThrownBy(
            () -> instantiationProvider.updateAutomationComposition(compositionId, automationCompositionUpdate))
            .hasMessageStartingWith("Element id not present ");
    }

    @Test
    void testUpdateBadRequest() {
        var automationCompositionUpdate =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_UPDATE_JSON, "Crud");
        automationCompositionUpdate.setDeployState(DeployState.DEPLOYING);
        automationCompositionUpdate.setLockState(LockState.NONE);
        var acProvider = mock(AutomationCompositionProvider.class);
        when(acProvider.getAutomationComposition(automationCompositionUpdate.getInstanceId()))
                .thenReturn(automationCompositionUpdate);

        var instantiationProvider =
            new AutomationCompositionInstantiationProvider(acProvider, mock(AcDefinitionProvider.class),
                new AcInstanceStateResolver(), mock(SupervisionAcHandler.class), mock(ParticipantProvider.class),
                mock(AcRuntimeParameterGroup.class));

        var compositionId = automationCompositionUpdate.getCompositionId();
        assertThatThrownBy(
                () -> instantiationProvider.updateAutomationComposition(compositionId, automationCompositionUpdate))
                        .hasMessageMatching(
                                "Not allowed to UPDATE in the state " + automationCompositionUpdate.getDeployState());

        automationCompositionUpdate.setDeployState(DeployState.UPDATING);
        automationCompositionUpdate.setLockState(LockState.LOCKED);
        automationCompositionUpdate.setCompositionTargetId(UUID.randomUUID());
        assertThatThrownBy(
                () -> instantiationProvider.updateAutomationComposition(compositionId, automationCompositionUpdate))
                        .hasMessageMatching(
                                "Not allowed to MIGRATE in the state " + automationCompositionUpdate.getDeployState());
    }

    @Test
    void testUpdateRestartedBadRequest() {
        var automationCompositionUpdate =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_UPDATE_JSON, "Crud");
        automationCompositionUpdate.setDeployState(DeployState.DEPLOYED);
        automationCompositionUpdate.setLockState(LockState.LOCKED);
        automationCompositionUpdate.setRestarting(true);
        var acProvider = mock(AutomationCompositionProvider.class);
        when(acProvider.getAutomationComposition(automationCompositionUpdate.getInstanceId()))
                .thenReturn(automationCompositionUpdate);

        var instantiationProvider =
                new AutomationCompositionInstantiationProvider(acProvider, mock(AcDefinitionProvider.class), null,
                        mock(SupervisionAcHandler.class), mock(ParticipantProvider.class),
                        mock(AcRuntimeParameterGroup.class));

        var compositionId = automationCompositionUpdate.getCompositionId();
        assertThatThrownBy(
                () -> instantiationProvider.updateAutomationComposition(compositionId, automationCompositionUpdate))
                        .hasMessageMatching("There is a restarting process, Update not allowed");

        automationCompositionUpdate.setCompositionTargetId(UUID.randomUUID());
        assertThatThrownBy(
                () -> instantiationProvider.updateAutomationComposition(compositionId, automationCompositionUpdate))
                .hasMessageMatching("There is a restarting process, Update not allowed");

        automationCompositionUpdate.setDeployState(DeployState.UNDEPLOYED);
        automationCompositionUpdate.setLockState(LockState.NONE);

        var instanceId = automationCompositionUpdate.getInstanceId();
        assertThatThrownBy(() -> instantiationProvider.deleteAutomationComposition(compositionId, instanceId))
                .hasMessageMatching("There is a restarting process, Delete not allowed");
    }

    @Test
    void testUpdateCompositionRestartedBadRequest() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        acDefinition.setRestarting(true);
        var compositionId = acDefinition.getCompositionId();
        when(acDefinitionProvider.findAcDefinition(compositionId)).thenReturn(Optional.of(acDefinition));

        var automationCompositionUpdate =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_UPDATE_JSON, "Crud");
        automationCompositionUpdate.setCompositionId(compositionId);
        automationCompositionUpdate.setDeployState(DeployState.DEPLOYED);
        automationCompositionUpdate.setLockState(LockState.LOCKED);
        var acProvider = mock(AutomationCompositionProvider.class);
        when(acProvider.getAutomationComposition(automationCompositionUpdate.getInstanceId()))
                .thenReturn(automationCompositionUpdate);
        when(acProvider.updateAutomationComposition(automationCompositionUpdate))
                .thenReturn(automationCompositionUpdate);

        var supervisionAcHandler = mock(SupervisionAcHandler.class);
        var participantProvider = mock(ParticipantProvider.class);
        var instantiationProvider = new AutomationCompositionInstantiationProvider(acProvider, acDefinitionProvider,
                new AcInstanceStateResolver(), supervisionAcHandler, participantProvider,
                mock(AcRuntimeParameterGroup.class));
        assertThatThrownBy(
                () -> instantiationProvider.updateAutomationComposition(compositionId, automationCompositionUpdate))
                        .hasMessageMatching("\"AutomationComposition\" INVALID, item has status INVALID\n"
                                + "  item \"ServiceTemplate.restarting\" value \"true\" INVALID,"
                                + " There is a restarting process in composition\n");
    }

    @Test
    void testInstantiationMigration() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var compositionId = acDefinition.getCompositionId();
        when(acDefinitionProvider.findAcDefinition(compositionId)).thenReturn(Optional.of(acDefinition));

        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_UPDATE_JSON, "Crud");
        automationComposition.setCompositionId(compositionId);
        automationComposition.setDeployState(DeployState.DEPLOYED);
        automationComposition.setLockState(LockState.LOCKED);
        automationComposition.setCompositionTargetId(UUID.randomUUID());
        var acProvider = mock(AutomationCompositionProvider.class);
        when(acProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        when(acProvider.updateAutomationComposition(automationComposition)).thenReturn(automationComposition);

        var supervisionAcHandler = mock(SupervisionAcHandler.class);
        var participantProvider = mock(ParticipantProvider.class);
        var instantiationProvider = new AutomationCompositionInstantiationProvider(acProvider, acDefinitionProvider,
            new AcInstanceStateResolver(), supervisionAcHandler, participantProvider, new AcRuntimeParameterGroup());

        assertThatThrownBy(() -> instantiationProvider
                .updateAutomationComposition(automationComposition.getCompositionId(), automationComposition))
                        .hasMessageMatching(
                                String.format(AC_DEFINITION_NOT_FOUND, automationComposition.getCompositionTargetId()));

        var acDefinitionTarget = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var compositionTargetId = acDefinitionTarget.getCompositionId();
        when(acDefinitionProvider.findAcDefinition(compositionTargetId)).thenReturn(Optional.of(acDefinitionTarget));

        automationComposition.setCompositionTargetId(compositionTargetId);

        var instantiationResponse = instantiationProvider
                .updateAutomationComposition(automationComposition.getCompositionId(), automationComposition);

        verify(supervisionAcHandler).migrate(any());
        verify(acProvider).updateAutomationComposition(automationComposition);
        InstantiationUtils.assertInstantiationResponse(instantiationResponse, automationComposition);
    }


    @Test
    void testInstantiationMigrationPrecheck() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var compositionId = acDefinition.getCompositionId();
        when(acDefinitionProvider.findAcDefinition(compositionId)).thenReturn(Optional.of(acDefinition));

        var automationComposition =
            InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_UPDATE_JSON, "Crud");
        automationComposition.setCompositionId(compositionId);
        automationComposition.setDeployState(DeployState.DEPLOYED);
        automationComposition.setLockState(LockState.LOCKED);
        automationComposition.setCompositionTargetId(UUID.randomUUID());
        automationComposition.setPrecheck(true);
        var acProvider = mock(AutomationCompositionProvider.class);
        when(acProvider.getAutomationComposition(automationComposition.getInstanceId()))
            .thenReturn(automationComposition);
        when(acProvider.updateAutomationComposition(automationComposition)).thenReturn(automationComposition);

        var supervisionAcHandler = mock(SupervisionAcHandler.class);
        var acmParticipantProvider = mock(ParticipantProvider.class);
        var instantiationProvider = new AutomationCompositionInstantiationProvider(acProvider, acDefinitionProvider,
            new AcInstanceStateResolver(), supervisionAcHandler, acmParticipantProvider, new AcRuntimeParameterGroup());

        assertThatThrownBy(() -> instantiationProvider
            .updateAutomationComposition(automationComposition.getCompositionId(), automationComposition))
            .hasMessageMatching(
                String.format(AC_DEFINITION_NOT_FOUND, automationComposition.getCompositionTargetId()));

        var acDefinitionTarget = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var compositionTargetId = acDefinitionTarget.getCompositionId();
        when(acDefinitionProvider.findAcDefinition(compositionTargetId)).thenReturn(Optional.of(acDefinitionTarget));

        automationComposition.setCompositionTargetId(compositionTargetId);

        var instantiationResponse = instantiationProvider
            .updateAutomationComposition(automationComposition.getCompositionId(), automationComposition);

        verify(supervisionAcHandler).migratePrecheck(any());
        verify(acProvider).updateAutomationComposition(automationComposition);
        InstantiationUtils.assertInstantiationResponse(instantiationResponse, automationComposition);
    }

    @Test
    void testMigrateBadRequest() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var compositionId = acDefinition.getCompositionId();
        when(acDefinitionProvider.findAcDefinition(compositionId)).thenReturn(Optional.of(acDefinition));

        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_UPDATE_JSON, "Crud");
        automationComposition.setCompositionId(compositionId);
        automationComposition.setDeployState(DeployState.DEPLOYED);
        automationComposition.setLockState(LockState.LOCKED);
        var acProvider = mock(AutomationCompositionProvider.class);
        when(acProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        when(acProvider.updateAutomationComposition(automationComposition)).thenReturn(automationComposition);

        var acDefinitionTarget = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var compositionTargetId = acDefinitionTarget.getCompositionId();
        when(acDefinitionProvider.findAcDefinition(compositionTargetId)).thenReturn(Optional.of(acDefinitionTarget));

        var acMigrate = new AutomationComposition(automationComposition);
        acMigrate.setCompositionTargetId(compositionTargetId);
        automationComposition.getElements().clear();

        var supervisionAcHandler = mock(SupervisionAcHandler.class);
        var participantProvider = mock(ParticipantProvider.class);
        var instantiationProvider = new AutomationCompositionInstantiationProvider(acProvider, acDefinitionProvider,
                new AcInstanceStateResolver(), supervisionAcHandler, participantProvider,
                new AcRuntimeParameterGroup());

        assertThatThrownBy(() -> instantiationProvider
                .updateAutomationComposition(automationComposition.getCompositionId(), acMigrate))
                .hasMessageStartingWith("Element id not present");
    }

    @Test
    void testMigratePrecheckBadRequest() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var compositionId = acDefinition.getCompositionId();
        when(acDefinitionProvider.findAcDefinition(compositionId)).thenReturn(Optional.of(acDefinition));

        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_UPDATE_JSON, "Crud");
        automationComposition.setCompositionId(compositionId);
        automationComposition.setDeployState(DeployState.DEPLOYED);
        automationComposition.setLockState(LockState.LOCKED);
        automationComposition.setPrecheck(true);
        var acProvider = mock(AutomationCompositionProvider.class);
        when(acProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        when(acProvider.updateAutomationComposition(automationComposition)).thenReturn(automationComposition);

        var acDefinitionTarget = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var compositionTargetId = acDefinitionTarget.getCompositionId();
        when(acDefinitionProvider.findAcDefinition(compositionTargetId)).thenReturn(Optional.of(acDefinitionTarget));

        var acMigrate = new AutomationComposition(automationComposition);
        acMigrate.setCompositionTargetId(compositionTargetId);
        automationComposition.getElements().clear();

        var supervisionAcHandler = mock(SupervisionAcHandler.class);
        var participantProvider = mock(ParticipantProvider.class);
        var instantiationProvider = new AutomationCompositionInstantiationProvider(acProvider, acDefinitionProvider,
                new AcInstanceStateResolver(), supervisionAcHandler, participantProvider,
                new AcRuntimeParameterGroup());

        assertThatThrownBy(() -> instantiationProvider
                .updateAutomationComposition(automationComposition.getCompositionId(), acMigrate))
                .hasMessageStartingWith("Element id not present");
    }

    @Test
    void testInstantiationDelete() {
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Delete");
        automationComposition.setStateChangeResult(StateChangeResult.NO_ERROR);
        var acProvider = mock(AutomationCompositionProvider.class);
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var compositionId = acDefinition.getCompositionId();
        when(acDefinitionProvider.getAcDefinition(compositionId)).thenReturn(acDefinition);
        automationComposition.setCompositionId(compositionId);
        var supervisionAcHandler = mock(SupervisionAcHandler.class);
        var participantProvider = mock(ParticipantProvider.class);
        var acRuntimeParameterGroup = mock(AcRuntimeParameterGroup.class);

        var instantiationProvider = new AutomationCompositionInstantiationProvider(acProvider, acDefinitionProvider,
                null, supervisionAcHandler, participantProvider, acRuntimeParameterGroup);

        when(acProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);

        var wrongCompositionId = UUID.randomUUID();
        var instanceId = automationComposition.getInstanceId();
        assertThatThrownBy(() -> instantiationProvider.deleteAutomationComposition(wrongCompositionId, instanceId))
                .hasMessageMatching(compositionId + DO_NOT_MATCH + wrongCompositionId);

        assertThatDeleteThrownBy(automationComposition, DeployState.DEPLOYED, LockState.LOCKED);
        assertThatDeleteThrownBy(automationComposition, DeployState.DEPLOYING, LockState.NONE);
        assertThatDeleteThrownBy(automationComposition, DeployState.UNDEPLOYING, LockState.LOCKED);
        assertThatDeleteThrownBy(automationComposition, DeployState.DELETING, LockState.NONE);

        automationComposition.setDeployState(DeployState.UNDEPLOYED);
        automationComposition.setLockState(LockState.NONE);
        when(acProvider.deleteAutomationComposition(instanceId)).thenReturn(automationComposition);

        instantiationProvider.deleteAutomationComposition(compositionId, instanceId);
        verify(supervisionAcHandler).delete(any(), any());
    }

    private void assertThatDeleteThrownBy(AutomationComposition automationComposition, DeployState deployState,
            LockState lockState) {
        automationComposition.setDeployState(deployState);
        automationComposition.setLockState(lockState);
        var acProvider = mock(AutomationCompositionProvider.class);
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var acRuntimeParamaterGroup = mock(AcRuntimeParameterGroup.class);

        var instantiationProvider =
                new AutomationCompositionInstantiationProvider(acProvider, acDefinitionProvider, null, null, null,
                        acRuntimeParamaterGroup);

        when(acProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);

        var compositionId = automationComposition.getCompositionId();
        var instanceId = automationComposition.getInstanceId();
        assertThatThrownBy(() -> instantiationProvider.deleteAutomationComposition(compositionId, instanceId))
                .hasMessageMatching(String.format(DELETE_BAD_REQUEST, deployState));
    }

    @Test
    void testCreateAutomationCompositions_NoDuplicates() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var compositionId = acDefinition.getCompositionId();
        when(acDefinitionProvider.findAcDefinition(compositionId)).thenReturn(Optional.of(acDefinition));

        var automationCompositionCreate =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "NoDuplicates");
        automationCompositionCreate.setCompositionId(compositionId);
        automationCompositionCreate.setInstanceId(UUID.randomUUID());

        var acProvider = mock(AutomationCompositionProvider.class);
        when(acProvider.createAutomationComposition(automationCompositionCreate))
                .thenReturn(automationCompositionCreate);
        var participantProvider = mock(ParticipantProvider.class);

        var instantiationProvider = new AutomationCompositionInstantiationProvider(acProvider, acDefinitionProvider,
                null, null, participantProvider,
                CommonTestData.getTestParamaterGroup());

        var instantiationResponse = instantiationProvider.createAutomationComposition(
                automationCompositionCreate.getCompositionId(), automationCompositionCreate);
        InstantiationUtils.assertInstantiationResponse(instantiationResponse, automationCompositionCreate);

        when(acProvider.findAutomationComposition(automationCompositionCreate.getKey().asIdentifier()))
                .thenReturn(Optional.of(automationCompositionCreate));

        assertThatThrownBy(
                () -> instantiationProvider.createAutomationComposition(compositionId, automationCompositionCreate))
                        .hasMessageMatching(automationCompositionCreate.getKey().asIdentifier() + " already defined");
    }

    @Test
    void testCreateAutomationCompositions_CommissionedAcElementNotFound() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var participantProvider = mock(ParticipantProvider.class);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var compositionId = acDefinition.getCompositionId();
        when(acDefinitionProvider.findAcDefinition(compositionId)).thenReturn(Optional.of(acDefinition));
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(
                AC_INSTANTIATION_DEFINITION_NAME_NOT_FOUND_JSON, "AcElementNotFound");
        automationComposition.setCompositionId(compositionId);

        var acProvider = mock(AutomationCompositionProvider.class);
        var provider = new AutomationCompositionInstantiationProvider(acProvider, acDefinitionProvider, null, null,
                participantProvider, CommonTestData.getTestParamaterGroup());

        assertThatThrownBy(() -> provider.createAutomationComposition(compositionId, automationComposition))
                .hasMessageMatching(AC_ELEMENT_NAME_NOT_FOUND);

        when(acProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);

        assertThatThrownBy(() -> provider.updateAutomationComposition(compositionId, automationComposition))
                .hasMessageMatching(AC_ELEMENT_NAME_NOT_FOUND);
    }

    @Test
    void testAcDefinitionNotFound() {
        var automationComposition = InstantiationUtils
                .getAutomationCompositionFromResource(AC_INSTANTIATION_AC_DEFINITION_NOT_FOUND_JSON, "AcNotFound");

        var acProvider = mock(AutomationCompositionProvider.class);
        when(acProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        var provider = new AutomationCompositionInstantiationProvider(acProvider, mock(AcDefinitionProvider.class),
                null, null, null,
                mock(AcRuntimeParameterGroup.class));

        var compositionId = automationComposition.getCompositionId();
        assertThatThrownBy(() -> provider.createAutomationComposition(compositionId, automationComposition))
                .hasMessageMatching(String.format(AC_DEFINITION_NOT_FOUND, compositionId));

        assertThatThrownBy(() -> provider.updateAutomationComposition(compositionId, automationComposition))
                .hasMessageMatching(String.format(AC_DEFINITION_NOT_FOUND, compositionId));
    }

    @Test
    void testCompositionIdDoNotMatch() {
        var automationComposition = InstantiationUtils
                .getAutomationCompositionFromResource(AC_INSTANTIATION_AC_DEFINITION_NOT_FOUND_JSON, "AcNotFound");

        var acProvider = mock(AutomationCompositionProvider.class);
        when(acProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        var provider = new AutomationCompositionInstantiationProvider(acProvider, mock(AcDefinitionProvider.class),
                null, null, null,
                mock(AcRuntimeParameterGroup.class));

        var compositionId = automationComposition.getCompositionId();
        var wrongCompositionId = UUID.randomUUID();
        assertThatThrownBy(() -> provider.createAutomationComposition(wrongCompositionId, automationComposition))
                .hasMessageMatching(compositionId + DO_NOT_MATCH + wrongCompositionId);

        assertThatThrownBy(() -> provider.updateAutomationComposition(wrongCompositionId, automationComposition))
                .hasMessageMatching(compositionId + DO_NOT_MATCH + wrongCompositionId);

        assertThatThrownBy(
                () -> provider.getAutomationComposition(wrongCompositionId, automationComposition.getInstanceId()))
                        .hasMessageMatching(compositionId + DO_NOT_MATCH + wrongCompositionId);
        assertThatThrownBy(() -> provider.compositionInstanceState(wrongCompositionId,
                automationComposition.getInstanceId(), new AcInstanceStateUpdate()))
                        .hasMessageMatching(compositionId + DO_NOT_MATCH + wrongCompositionId);

        var compositionTargetId = UUID.randomUUID();
        automationComposition.setCompositionTargetId(compositionTargetId);
        assertThatThrownBy(
                () -> provider.getAutomationComposition(wrongCompositionId, automationComposition.getInstanceId()))
                        .hasMessageMatching(compositionId + DO_NOT_MATCH + wrongCompositionId);

        var result = provider.getAutomationComposition(compositionTargetId, automationComposition.getInstanceId());
        assertThat(result).isNotNull();
    }

    @Test
    void testCompositionNotPrimed() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.COMMISSIONED);
        var compositionId = acDefinition.getCompositionId();
        when(acDefinitionProvider.findAcDefinition(compositionId)).thenReturn(Optional.of(acDefinition));
        var acProvider = mock(AutomationCompositionProvider.class);
        var provider =
                new AutomationCompositionInstantiationProvider(acProvider, acDefinitionProvider, null, null, null,
                        null);

        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Crud");
        automationComposition.setCompositionId(compositionId);
        assertThatThrownBy(() -> provider.createAutomationComposition(compositionId, automationComposition))
                .hasMessageMatching("\"AutomationComposition\" INVALID, item has status INVALID\n"
                        + "  item \"ServiceTemplate.state\" value \"COMMISSIONED\" INVALID,"
                        + " Commissioned automation composition definition not primed\n");
    }

    @Test
    void testCompositionInstanceState() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.COMMISSIONED);
        var compositionId = acDefinition.getCompositionId();
        when(acDefinitionProvider.getAcDefinition(compositionId)).thenReturn(acDefinition);

        var automationComposition = InstantiationUtils
                .getAutomationCompositionFromResource(AC_INSTANTIATION_AC_DEFINITION_NOT_FOUND_JSON, "AcNotFound");
        var instanceId = UUID.randomUUID();
        automationComposition.setCompositionId(compositionId);
        automationComposition.setInstanceId(instanceId);
        var acProvider = mock(AutomationCompositionProvider.class);
        when(acProvider.getAutomationComposition(instanceId)).thenReturn(automationComposition);

        var supervisionAcHandler = mock(SupervisionAcHandler.class);
        var participantProvider = mock(ParticipantProvider.class);
        var provider = new AutomationCompositionInstantiationProvider(acProvider, acDefinitionProvider,
                new AcInstanceStateResolver(), supervisionAcHandler, participantProvider,
                mock(AcRuntimeParameterGroup.class));

        var acInstanceStateUpdate = new AcInstanceStateUpdate();
        acInstanceStateUpdate.setDeployOrder(DeployOrder.DEPLOY);
        acInstanceStateUpdate.setLockOrder(LockOrder.NONE);
        provider.compositionInstanceState(compositionId, instanceId, acInstanceStateUpdate);
        verify(supervisionAcHandler).deploy(any(AutomationComposition.class),
                any(AutomationCompositionDefinition.class));

        automationComposition.setDeployState(DeployState.DEPLOYED);
        automationComposition.setLockState(LockState.LOCKED);
        acInstanceStateUpdate.setDeployOrder(DeployOrder.UNDEPLOY);
        provider.compositionInstanceState(compositionId, instanceId, acInstanceStateUpdate);
        verify(supervisionAcHandler).undeploy(any(AutomationComposition.class),
                any(AutomationCompositionDefinition.class));

        automationComposition.setDeployState(DeployState.DEPLOYED);
        automationComposition.setLockState(LockState.LOCKED);
        acInstanceStateUpdate.setDeployOrder(DeployOrder.NONE);
        acInstanceStateUpdate.setLockOrder(LockOrder.UNLOCK);
        provider.compositionInstanceState(compositionId, instanceId, acInstanceStateUpdate);
        verify(supervisionAcHandler).unlock(any(AutomationComposition.class),
                any(AutomationCompositionDefinition.class));

        automationComposition.setDeployState(DeployState.DEPLOYED);
        automationComposition.setLockState(LockState.UNLOCKED);
        acInstanceStateUpdate.setDeployOrder(DeployOrder.NONE);
        acInstanceStateUpdate.setLockOrder(LockOrder.LOCK);
        provider.compositionInstanceState(compositionId, instanceId, acInstanceStateUpdate);
        verify(supervisionAcHandler).lock(any(AutomationComposition.class), any(AutomationCompositionDefinition.class));

        automationComposition.setDeployState(DeployState.UNDEPLOYED);
        automationComposition.setLockState(LockState.NONE);
        acInstanceStateUpdate.setDeployOrder(DeployOrder.NONE);
        acInstanceStateUpdate.setLockOrder(LockOrder.NONE);
        acInstanceStateUpdate.setSubOrder(SubOrder.PREPARE);
        provider.compositionInstanceState(compositionId, instanceId, acInstanceStateUpdate);
        verify(supervisionAcHandler).prepare(any(AutomationComposition.class));

        automationComposition.setDeployState(DeployState.DEPLOYED);
        automationComposition.setLockState(LockState.LOCKED);
        acInstanceStateUpdate.setDeployOrder(DeployOrder.NONE);
        acInstanceStateUpdate.setLockOrder(LockOrder.NONE);
        acInstanceStateUpdate.setSubOrder(SubOrder.REVIEW);
        provider.compositionInstanceState(compositionId, instanceId, acInstanceStateUpdate);
        verify(supervisionAcHandler).review(any(AutomationComposition.class));
    }
}
