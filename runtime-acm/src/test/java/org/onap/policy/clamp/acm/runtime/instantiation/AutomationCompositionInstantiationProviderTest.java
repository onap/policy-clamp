/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.onap.policy.clamp.acm.runtime.util.CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup;
import org.onap.policy.clamp.acm.runtime.main.utils.EncryptionUtils;
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionAcHandler;
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionRollback;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.MigrationState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.AcInstanceStateUpdate;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.LockOrder;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.SubOrder;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaAutomationCompositionRollback;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.AcInstanceStateResolver;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ProviderUtils;
import org.onap.policy.clamp.models.acm.utils.AcmStateUtils;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.simple.concepts.JpaToscaServiceTemplate;
import org.springframework.data.domain.Pageable;

/**
 * Class to perform unit test of {@link AutomationCompositionInstantiationProvider}}.
 *
 */
class AutomationCompositionInstantiationProviderTest {

    public static final String MIGRATION_SERVICE_TEMPLATE_YAML = "clamp/acm/pmsh/funtional-pmsh-usecase-migration.yaml";
    private static final String AC_INSTANTIATION_CREATE_JSON = "src/test/resources/rest/acm/AutomationComposition.json";
    private static final String AC_INSTANTIATION_UPDATE_JSON =
            "src/test/resources/rest/acm/AutomationCompositionUpdate.json";
    private static final String AC_MIGRATE_JSON = "src/test/resources/rest/acm/AutomationCompositionMigrate.json";

    private static final String AC_INSTANTIATION_DEFINITION_NAME_NOT_FOUND_JSON =
            "src/test/resources/rest/acm/AutomationCompositionElementsNotFound.json";
    private static final String AC_INSTANTIATION_AC_DEFINITION_NOT_FOUND_JSON =
            "src/test/resources/rest/acm/AutomationCompositionNotFound.json";
    private static final String DELETE_BAD_REQUEST = "Not valid order DELETE;";

    private static final String AC_ELEMENT_NAME_NOT_FOUND = """
                "entry org.onap.domain.pmsh.DCAEMicroservice" INVALID, Not found
                "entry org.onap.domain.pmsh.PMSH_MonitoringPolicyAutomationCompositionElement" INVALID, Not found
            """;
    private static final String AC_DEFINITION_NOT_FOUND = """
            "AutomationComposition" INVALID, item has status INVALID
              item "ServiceTemplate" value "%s" INVALID, Commissioned automation composition definition not found
            """;

    private static final String DO_NOT_MATCH = " do not match with ";

    private static ToscaServiceTemplate serviceTemplate = new ToscaServiceTemplate();
    private static ToscaServiceTemplate serviceTemplateMigration = new ToscaServiceTemplate();

    @BeforeAll
    static void setUpBeforeClass() {
        var st = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        var jpa = ProviderUtils.getJpaAndValidate(st, JpaToscaServiceTemplate::new, "toscaServiceTemplate");
        serviceTemplate = jpa.toAuthorative();

        st = InstantiationUtils.getToscaServiceTemplate(MIGRATION_SERVICE_TEMPLATE_YAML);
        jpa = ProviderUtils.getJpaAndValidate(st, JpaToscaServiceTemplate::new, "migrationServiceTemplate");
        serviceTemplateMigration = jpa.toAuthorative();
    }

    @Test
    void testGetAutomationCompositionsWithNull() {
        var instantiationProvider = new AutomationCompositionInstantiationProvider(
                mock(AutomationCompositionProvider.class), mock(AcDefinitionProvider.class),
                new AcInstanceStateResolver(), mock(SupervisionAcHandler.class), mock(ParticipantProvider.class),
                CommonTestData.getTestParamaterGroup(), new EncryptionUtils(CommonTestData.getTestParamaterGroup()));
        assertThatThrownBy(() -> instantiationProvider
                .getAutomationCompositions(null, null, null, Pageable.unpaged()))
                .hasMessage("compositionId is marked non-null but is null");
        assertThatThrownBy(() -> instantiationProvider
                .getAutomationCompositions(UUID.randomUUID(), null, null, null))
                .hasMessage("pageable is marked non-null but is null");
    }

    @Test
    void testInstantiationCrud() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var compositionId = acDefinition.getCompositionId();
        when(acDefinitionProvider.getAcDefinition(compositionId)).thenReturn(acDefinition);
        when(acDefinitionProvider.getAcDefinition(compositionId)).thenReturn(acDefinition);
        var acProvider = mock(AutomationCompositionProvider.class);
        var supervisionAcHandler = mock(SupervisionAcHandler.class);
        var participantProvider = mock(ParticipantProvider.class);
        var encryptionUtils = new EncryptionUtils(CommonTestData.getTestParamaterGroup());
        var instantiationProvider = new AutomationCompositionInstantiationProvider(acProvider, acDefinitionProvider,
                new AcInstanceStateResolver(), supervisionAcHandler, participantProvider,
                CommonTestData.getTestParamaterGroup(), encryptionUtils);
        var automationCompositionCreate =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Crud");
        automationCompositionCreate.setCompositionId(compositionId);
        when(acProvider.createAutomationComposition(automationCompositionCreate))
                .thenReturn(automationCompositionCreate);

        var instantiationResponse = instantiationProvider.createAutomationComposition(
                automationCompositionCreate.getCompositionId(), automationCompositionCreate);
        InstantiationUtils.assertInstantiationResponse(instantiationResponse, automationCompositionCreate);

        verify(acProvider).createAutomationComposition(automationCompositionCreate);

        assertEquals(StateChangeResult.NO_ERROR, acProvider.createAutomationComposition(automationCompositionCreate)
                .getStateChangeResult());

        when(acProvider.getAutomationCompositions(compositionId, automationCompositionCreate.getName(),
                automationCompositionCreate.getVersion(), Pageable.unpaged()))
                .thenReturn(List.of(automationCompositionCreate));

        var automationCompositionsGet = instantiationProvider.getAutomationCompositions(compositionId,
                automationCompositionCreate.getName(), automationCompositionCreate.getVersion(), Pageable.unpaged());
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
        when(acDefinitionProvider.getAcDefinition(compositionId)).thenReturn(acDefinition);

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
        var encryptionUtils = new EncryptionUtils(CommonTestData.getTestParamaterGroup());
        var instantiationProvider = new AutomationCompositionInstantiationProvider(acProvider, acDefinitionProvider,
                new AcInstanceStateResolver(), supervisionAcHandler, participantProvider,
                CommonTestData.getTestParamaterGroup(), encryptionUtils);

        var instantiationResponse = instantiationProvider.updateAutomationComposition(
                automationCompositionUpdate.getCompositionId(), automationCompositionUpdate);

        verify(supervisionAcHandler).update(any(), any());
        verify(acProvider).updateAutomationComposition(acmFromDb);
        InstantiationUtils.assertInstantiationResponse(instantiationResponse, automationCompositionUpdate);

        var elements = new ArrayList<>(automationCompositionUpdate.getElements().values());
        automationCompositionUpdate.getElements().clear();
        for (var element : elements) {
            element.setId(UUID.randomUUID());
            automationCompositionUpdate.getElements().put(element.getId(), element);
        }
        acmFromDb.getElements().values().forEach(element ->
                element.setDeployState(DeployState.DEPLOYED));
        acmFromDb.setDeployState(DeployState.DEPLOYED);
        assertThatThrownBy(
                () -> instantiationProvider.updateAutomationComposition(compositionId, automationCompositionUpdate))
                .hasMessageStartingWith("Element id not present ");

    }

    @Test
    void testUpdateBadRequest() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var compositionId = acDefinition.getCompositionId();
        when(acDefinitionProvider.getAcDefinition(compositionId)).thenReturn(acDefinition);

        var automationCompositionUpdate =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_UPDATE_JSON, "Crud");
        automationCompositionUpdate.setDeployState(DeployState.DEPLOYING);
        automationCompositionUpdate.setLockState(LockState.NONE);
        automationCompositionUpdate.setCompositionId(compositionId);
        var acProvider = mock(AutomationCompositionProvider.class);
        var encryptionUtils = mock(EncryptionUtils.class);
        when(acProvider.getAutomationComposition(automationCompositionUpdate.getInstanceId()))
                .thenReturn(automationCompositionUpdate);

        var instantiationProvider =
                new AutomationCompositionInstantiationProvider(acProvider, acDefinitionProvider,
                        new AcInstanceStateResolver(), mock(SupervisionAcHandler.class),
                        mock(ParticipantProvider.class),
                        mock(AcRuntimeParameterGroup.class), encryptionUtils);


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
    void testMigrationAddRemoveElements() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var compositionId = acDefinition.getCompositionId();
        when(acDefinitionProvider.getAcDefinition(compositionId)).thenReturn(acDefinition);
        var instanceId = UUID.randomUUID();

        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Crud");
        automationComposition.setCompositionId(compositionId);
        automationComposition.setInstanceId(instanceId);
        automationComposition.setDeployState(DeployState.DEPLOYED);
        automationComposition.setLockState(LockState.LOCKED);
        var acProvider = mock(AutomationCompositionProvider.class);
        when(acProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);

        var automationCompositionTarget =
                InstantiationUtils.getAutomationCompositionFromResource(AC_MIGRATE_JSON, "Migrate");
        automationCompositionTarget.setInstanceId(instanceId);
        automationCompositionTarget.setCompositionId(compositionId);
        var acDefinitionTarget = CommonTestData.createAcDefinition(serviceTemplateMigration, AcTypeState.PRIMED);
        var compositionTargetId = acDefinitionTarget.getCompositionId();
        automationCompositionTarget.setCompositionTargetId(compositionTargetId);
        when(acDefinitionProvider.getAcDefinition(compositionTargetId)).thenReturn(acDefinitionTarget);
        when(acDefinitionProvider.getAcDefinition(compositionTargetId)).thenReturn(acDefinitionTarget);
        when(acProvider.updateAutomationComposition(any())).thenReturn(automationCompositionTarget);

        var supervisionAcHandler = mock(SupervisionAcHandler.class);
        var participantProvider = mock(ParticipantProvider.class);
        var encryptionUtils = new EncryptionUtils(CommonTestData.getTestParamaterGroup());
        var instantiationProvider = new AutomationCompositionInstantiationProvider(acProvider, acDefinitionProvider,
                new AcInstanceStateResolver(), supervisionAcHandler, participantProvider,
                new AcRuntimeParameterGroup(), encryptionUtils);

        automationCompositionTarget.setPrecheck(true);
        var preCheckResponse = instantiationProvider.updateAutomationComposition(compositionId,
                automationCompositionTarget);
        verify(supervisionAcHandler).migratePrecheck(any(), any(), any());
        InstantiationUtils.assertInstantiationResponse(preCheckResponse, automationCompositionTarget);

        automationCompositionTarget.setPrecheck(false);
        AcmStateUtils.setCascadedState(automationComposition, DeployState.DEPLOYED, LockState.LOCKED,
                SubState.NONE);
        automationComposition.getElements().values()
                .forEach(el -> el.setMigrationState(MigrationState.DEFAULT));
        var instantiationResponse = instantiationProvider.updateAutomationComposition(compositionId,
                automationCompositionTarget);

        verify(supervisionAcHandler).migrate(any(), any(), any());
        InstantiationUtils.assertInstantiationResponse(instantiationResponse, automationCompositionTarget);
    }

    @Test
    void testInstantiationMigration() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var compositionId = acDefinition.getCompositionId();
        when(acDefinitionProvider.getAcDefinition(compositionId)).thenReturn(acDefinition);

        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_UPDATE_JSON, "Crud");
        automationComposition.setCompositionId(compositionId);
        automationComposition.setDeployState(DeployState.DEPLOYED);
        automationComposition.setLockState(LockState.LOCKED);
        automationComposition.setCompositionTargetId(UUID.randomUUID());
        when(acDefinitionProvider.getAcDefinition(automationComposition.getCompositionTargetId()))
                .thenThrow(new PfModelRuntimeException(Status.NOT_FOUND,
                        String.format(AC_DEFINITION_NOT_FOUND, automationComposition.getCompositionTargetId())));
        var acProvider = mock(AutomationCompositionProvider.class);
        when(acProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        when(acProvider.updateAutomationComposition(automationComposition)).thenReturn(automationComposition);

        var supervisionAcHandler = mock(SupervisionAcHandler.class);
        var participantProvider = mock(ParticipantProvider.class);
        var encryptionUtils = new EncryptionUtils(CommonTestData.getTestParamaterGroup());
        var instantiationProvider = new AutomationCompositionInstantiationProvider(acProvider, acDefinitionProvider,
                new AcInstanceStateResolver(), supervisionAcHandler, participantProvider, new AcRuntimeParameterGroup(),
                encryptionUtils);

        assertThatThrownBy(() -> instantiationProvider
                .updateAutomationComposition(automationComposition.getCompositionId(), automationComposition))
                .hasMessageMatching(
                        String.format(AC_DEFINITION_NOT_FOUND, automationComposition.getCompositionTargetId()));

        var acDefinitionTarget = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var compositionTargetId = acDefinitionTarget.getCompositionId();
        when(acDefinitionProvider.getAcDefinition(compositionTargetId)).thenReturn(acDefinitionTarget);

        automationComposition.setCompositionTargetId(compositionTargetId);

        var instantiationResponse = instantiationProvider
                .updateAutomationComposition(automationComposition.getCompositionId(), automationComposition);

        verify(supervisionAcHandler).migrate(any(), any(), any());
        verify(acProvider).updateAutomationComposition(automationComposition);
        InstantiationUtils.assertInstantiationResponse(instantiationResponse, automationComposition);
    }

    @Test
    void testInstantiationMigrationPrecheck() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var compositionId = acDefinition.getCompositionId();
        when(acDefinitionProvider.getAcDefinition(compositionId)).thenReturn(acDefinition);

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
        var encryptionUtils = mock(EncryptionUtils.class);
        var instantiationProvider = new AutomationCompositionInstantiationProvider(acProvider, acDefinitionProvider,
                new AcInstanceStateResolver(), supervisionAcHandler, acmParticipantProvider,
                new AcRuntimeParameterGroup(), encryptionUtils);

        var acDefinitionTarget = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var compositionTargetId = acDefinitionTarget.getCompositionId();
        when(acDefinitionProvider.getAcDefinition(compositionTargetId)).thenReturn(acDefinitionTarget);
        automationComposition.setCompositionTargetId(compositionTargetId);

        var instantiationResponse = instantiationProvider
                .updateAutomationComposition(automationComposition.getCompositionId(), automationComposition);

        verify(supervisionAcHandler).migratePrecheck(any(), any(), any());
        verify(acProvider).updateAutomationComposition(automationComposition);
        InstantiationUtils.assertInstantiationResponse(instantiationResponse, automationComposition);

        automationComposition.setSubState(SubState.NONE);
        when(acDefinitionProvider.getAcDefinition(compositionTargetId))
                .thenThrow(new PfModelRuntimeException(Status.NOT_FOUND,
                        String.format(AC_DEFINITION_NOT_FOUND, compositionTargetId)));
        assertThatThrownBy(() -> instantiationProvider
                .updateAutomationComposition(automationComposition.getCompositionId(), automationComposition))
                .hasMessageMatching(String.format(AC_DEFINITION_NOT_FOUND, compositionTargetId));
    }

    @Test
    void testMigrateBadRequest() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var compositionId = acDefinition.getCompositionId();
        when(acDefinitionProvider.getAcDefinition(compositionId)).thenReturn(acDefinition);

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
        when(acDefinitionProvider.getAcDefinition(compositionTargetId)).thenReturn(acDefinitionTarget);

        var acMigrate = new AutomationComposition(automationComposition);
        acMigrate.setCompositionTargetId(compositionTargetId);
        automationComposition.setDeployState(DeployState.DEPLOYING);

        var supervisionAcHandler = mock(SupervisionAcHandler.class);
        var participantProvider = mock(ParticipantProvider.class);
        var encryptionUtils = mock(EncryptionUtils.class);
        var instantiationProvider = new AutomationCompositionInstantiationProvider(acProvider, acDefinitionProvider,
                new AcInstanceStateResolver(), supervisionAcHandler, participantProvider,
                new AcRuntimeParameterGroup(), encryptionUtils);

        assertThatThrownBy(() -> instantiationProvider
                .updateAutomationComposition(automationComposition.getCompositionId(), acMigrate))
                .hasMessageStartingWith("Not allowed to MIGRATE in the state DEPLOYING");
    }

    @Test
    void testMigratePreCheckBadRequest() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var compositionId = acDefinition.getCompositionId();
        when(acDefinitionProvider.getAcDefinition(compositionId)).thenReturn(acDefinition);

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
        when(acDefinitionProvider.getAcDefinition(compositionTargetId)).thenReturn(acDefinitionTarget);

        var acMigrate = new AutomationComposition(automationComposition);
        acMigrate.setCompositionTargetId(compositionTargetId);
        automationComposition.setDeployState(DeployState.DEPLOYING);
        automationComposition.setPrecheck(true);

        var supervisionAcHandler = mock(SupervisionAcHandler.class);
        var participantProvider = mock(ParticipantProvider.class);
        var encryptionUtils = mock(EncryptionUtils.class);
        var instantiationProvider = new AutomationCompositionInstantiationProvider(acProvider, acDefinitionProvider,
                new AcInstanceStateResolver(), supervisionAcHandler, participantProvider,
                new AcRuntimeParameterGroup(), encryptionUtils);

        assertThatThrownBy(() -> instantiationProvider
                .updateAutomationComposition(automationComposition.getCompositionId(), acMigrate))
                .hasMessageStartingWith("Not allowed to NONE in the state DEPLOYING");
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
        var encryptionUtils = mock(EncryptionUtils.class);

        var instantiationProvider = new AutomationCompositionInstantiationProvider(acProvider, acDefinitionProvider,
                new AcInstanceStateResolver(), supervisionAcHandler, participantProvider, acRuntimeParameterGroup,
                encryptionUtils);

        when(acProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);

        var wrongCompositionId = UUID.randomUUID();
        var instanceId = automationComposition.getInstanceId();
        assertThatThrownBy(() -> instantiationProvider.deleteAutomationComposition(wrongCompositionId, instanceId))
                .hasMessageMatching(compositionId + DO_NOT_MATCH + wrongCompositionId);

        automationComposition.setDeployState(DeployState.UNDEPLOYED);
        automationComposition.setLockState(LockState.NONE);
        when(acProvider.deleteAutomationComposition(instanceId)).thenReturn(automationComposition);
        instantiationProvider.deleteAutomationComposition(compositionId, instanceId);
        verify(supervisionAcHandler).delete(any(), any());
    }

    @Test
    void testInstantiationDeleteError() {
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Delete");
        automationComposition.setStateChangeResult(StateChangeResult.NO_ERROR);
        assertThatDeleteThrownBy(automationComposition, DeployState.DEPLOYED, LockState.LOCKED);
        assertThatDeleteThrownBy(automationComposition, DeployState.DEPLOYING, LockState.NONE);
        assertThatDeleteThrownBy(automationComposition, DeployState.UNDEPLOYING, LockState.LOCKED);
        assertThatDeleteThrownBy(automationComposition, DeployState.DELETING, LockState.NONE);
    }

    @Test
    void testRollbackFailure() {
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var compositionId = acDefinition.getCompositionId();
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Rollback");
        automationComposition.setCompositionId(compositionId);
        automationComposition.setInstanceId(UUID.randomUUID());
        automationComposition.setDeployState(DeployState.MIGRATION_REVERTING);
        automationComposition.setCompositionTargetId(UUID.randomUUID());

        var acProvider = mock(AutomationCompositionProvider.class);
        when(acProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        var rollbackRecord = new JpaAutomationCompositionRollback();
        when(acProvider.getAutomationCompositionRollback(any(UUID.class))).thenReturn(rollbackRecord.toAuthorative());

        final var acDefinitionProvider = mock(AcDefinitionProvider.class);
        final var supervisionAcHandler = mock(SupervisionAcHandler.class);
        final var participantProvider = mock(ParticipantProvider.class);
        final var encryptionUtils = new EncryptionUtils(CommonTestData.getTestParamaterGroup());
        var instantiationProvider = new AutomationCompositionInstantiationProvider(acProvider, acDefinitionProvider,
                new AcInstanceStateResolver(), supervisionAcHandler, participantProvider, new AcRuntimeParameterGroup(),
                encryptionUtils);

        var instanceId = automationComposition.getInstanceId();
        assertThrows(PfModelRuntimeException.class, () -> instantiationProvider.rollback(compositionId, instanceId));

        // DeployState != MIGRATION_REVERTING
        when(acProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        when(acProvider.getAutomationCompositionRollback(any(UUID.class))).thenReturn(rollbackRecord.toAuthorative());

        automationComposition.setDeployState(DeployState.DELETING);
        assertThrows(PfModelRuntimeException.class, () -> instantiationProvider.rollback(compositionId, instanceId));

        // SubState != NONE
        automationComposition.setDeployState(DeployState.DEPLOYED);
        automationComposition.setSubState(SubState.PREPARING);
        assertThrows(PfModelRuntimeException.class, () -> instantiationProvider.rollback(compositionId, instanceId));

        // StateChangeResult != NO_ERROR
        automationComposition.setSubState(SubState.NONE);
        automationComposition.setStateChangeResult(StateChangeResult.FAILED);
        assertThrows(PfModelRuntimeException.class, () -> instantiationProvider.rollback(compositionId, instanceId));

        // !compositionId.equals(compId)
        automationComposition.setStateChangeResult(StateChangeResult.NO_ERROR);
        automationComposition.setCompositionId(UUID.randomUUID());
        assertThrows(PfModelRuntimeException.class, () -> instantiationProvider.rollback(compositionId, instanceId));
        verify(acProvider, never()).updateAutomationComposition(any());
    }

    @Test
    void testRollbackSuccess() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var compositionId = acDefinition.getCompositionId();
        when(acDefinitionProvider.getAcDefinition(compositionId)).thenReturn(acDefinition);

        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_MIGRATE_JSON, "Crud");
        var instanceId = UUID.randomUUID();
        var compositionTargetId = UUID.randomUUID();
        automationComposition.setInstanceId(instanceId);
        automationComposition.setCompositionId(compositionId);
        automationComposition.setCompositionTargetId(compositionTargetId);
        automationComposition.setDeployState(DeployState.MIGRATING);
        automationComposition.setLockState(LockState.LOCKED);
        automationComposition.setStateChangeResult(StateChangeResult.FAILED);
        automationComposition.setSubState(SubState.NONE);
        var acProvider = mock(AutomationCompositionProvider.class);
        when(acProvider.getAutomationComposition(instanceId)).thenReturn(automationComposition);
        when(acProvider.updateAutomationComposition(automationComposition)).thenReturn(automationComposition);

        var acRollback =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_UPDATE_JSON, "Crud");

        var rollbackRecord = new AutomationCompositionRollback();
        rollbackRecord.setCompositionId(compositionTargetId);
        rollbackRecord.setInstanceId(instanceId);
        rollbackRecord.setElements(acRollback.getElements());
        when(acProvider.getAutomationCompositionRollback(instanceId)).thenReturn(rollbackRecord);

        var supervisionAcHandler = mock(SupervisionAcHandler.class);
        var participantProvider = mock(ParticipantProvider.class);
        var encryptionUtils = new EncryptionUtils(CommonTestData.getTestParamaterGroup());
        var instantiationProvider = new AutomationCompositionInstantiationProvider(acProvider, acDefinitionProvider,
                new AcInstanceStateResolver(), supervisionAcHandler, participantProvider, new AcRuntimeParameterGroup(),
                encryptionUtils);
        var acDefinitionTarget = CommonTestData.createAcDefinition(serviceTemplateMigration, AcTypeState.PRIMED);
        when(acDefinitionProvider.getAcDefinition(compositionTargetId)).thenReturn(acDefinitionTarget);

        instantiationProvider.rollback(compositionId, automationComposition.getInstanceId());
        verify(acProvider).updateAutomationComposition(any(AutomationComposition.class));

        when(acDefinitionProvider.getAcDefinition(compositionTargetId))
                .thenThrow(new PfModelRuntimeException(Response.Status.NOT_FOUND,
                        String.format(AC_DEFINITION_NOT_FOUND, compositionTargetId)));
        assertThatThrownBy(() -> instantiationProvider.rollback(compositionId, instanceId))
                .hasMessageMatching(String.format(AC_DEFINITION_NOT_FOUND, compositionTargetId));
    }

    private void assertThatDeleteThrownBy(AutomationComposition automationComposition, DeployState deployState,
                                          LockState lockState) {
        automationComposition.setDeployState(deployState);
        automationComposition.setLockState(lockState);
        var acProvider = mock(AutomationCompositionProvider.class);
        when(acProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var acRuntimeParamaterGroup = mock(AcRuntimeParameterGroup.class);

        var instantiationProvider =
                new AutomationCompositionInstantiationProvider(acProvider, acDefinitionProvider,
                        new AcInstanceStateResolver(), null, mock(ParticipantProvider.class),
                        acRuntimeParamaterGroup, null);

        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var compositionId = acDefinition.getCompositionId();
        when(acDefinitionProvider.getAcDefinition(compositionId)).thenReturn(acDefinition);
        automationComposition.setCompositionId(compositionId);

        var instanceId = automationComposition.getInstanceId();
        assertThatThrownBy(() -> instantiationProvider.deleteAutomationComposition(compositionId, instanceId))
                .hasMessageStartingWith(String.format(DELETE_BAD_REQUEST));
    }

    @Test
    void testCreateAutomationCompositions_NoDuplicates() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var compositionId = acDefinition.getCompositionId();
        when(acDefinitionProvider.getAcDefinition(compositionId)).thenReturn(acDefinition);

        var automationCompositionCreate =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "NoDuplicates");
        automationCompositionCreate.setCompositionId(compositionId);
        automationCompositionCreate.setInstanceId(UUID.randomUUID());

        var acProvider = mock(AutomationCompositionProvider.class);
        when(acProvider.createAutomationComposition(automationCompositionCreate))
                .thenReturn(automationCompositionCreate);
        var acIdentifier = automationCompositionCreate.getKey().asIdentifier();
        var participantProvider = mock(ParticipantProvider.class);
        var encryptionUtils = new EncryptionUtils(CommonTestData.getTestParamaterGroup());

        var instantiationProvider = new AutomationCompositionInstantiationProvider(acProvider, acDefinitionProvider,
                null, null, participantProvider,
                CommonTestData.getTestParamaterGroup(), encryptionUtils);

        var instantiationResponse = instantiationProvider.createAutomationComposition(
                automationCompositionCreate.getCompositionId(), automationCompositionCreate);
        InstantiationUtils.assertInstantiationResponse(instantiationResponse, automationCompositionCreate);

        doThrow(new PfModelRuntimeException(Status.BAD_REQUEST, acIdentifier + " already defined"))
                .when(acProvider).validateNameVersion(acIdentifier);
        assertThatThrownBy(
                () -> instantiationProvider.createAutomationComposition(compositionId, automationCompositionCreate))
                .hasMessageMatching(acIdentifier + " already defined");
    }

    @Test
    void testCreateAutomationCompositions_CommissionedAcElementNotFound() {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var participantProvider = mock(ParticipantProvider.class);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var compositionId = acDefinition.getCompositionId();
        when(acDefinitionProvider.getAcDefinition(compositionId)).thenReturn(acDefinition);
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(
                AC_INSTANTIATION_DEFINITION_NAME_NOT_FOUND_JSON, "AcElementNotFound");
        automationComposition.setCompositionId(compositionId);

        var acProvider = mock(AutomationCompositionProvider.class);
        var encryptionUtils = mock(EncryptionUtils.class);
        var provider = new AutomationCompositionInstantiationProvider(acProvider, acDefinitionProvider, null, null,
                participantProvider, CommonTestData.getTestParamaterGroup(), encryptionUtils);

        assertThatThrownBy(() -> provider.createAutomationComposition(compositionId, automationComposition))
                .hasMessageContaining(AC_ELEMENT_NAME_NOT_FOUND);

        when(acProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);

        assertThatThrownBy(() -> provider.updateAutomationComposition(compositionId, automationComposition))
                .hasMessageContaining(AC_ELEMENT_NAME_NOT_FOUND);
    }

    @Test
    void testAcDefinitionNotFound() {
        var automationComposition = InstantiationUtils
                .getAutomationCompositionFromResource(AC_INSTANTIATION_AC_DEFINITION_NOT_FOUND_JSON, "AcNotFound");

        var acProvider = mock(AutomationCompositionProvider.class);
        when(acProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var provider = new AutomationCompositionInstantiationProvider(acProvider, acDefinitionProvider,
                null, null, null,
                mock(AcRuntimeParameterGroup.class), null);

        var compositionId = automationComposition.getCompositionId();
        when(acDefinitionProvider.getAcDefinition(automationComposition.getCompositionId()))
                .thenThrow(new PfModelRuntimeException(Status.NOT_FOUND,
                        String.format(AC_DEFINITION_NOT_FOUND, automationComposition.getCompositionId())));
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
                mock(AcRuntimeParameterGroup.class), null);

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
        when(acDefinitionProvider.getAcDefinition(compositionId)).thenReturn(acDefinition);
        var acProvider = mock(AutomationCompositionProvider.class);
        var provider =
                new AutomationCompositionInstantiationProvider(acProvider, acDefinitionProvider, null, null, null,
                        null, null);

        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Crud");
        automationComposition.setCompositionId(compositionId);
        var message = compositionId + " Commissioned automation composition definition not primed";

        assertThatThrownBy(() -> provider.createAutomationComposition(compositionId, automationComposition))
                .hasMessageMatching(message);
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
                mock(AcRuntimeParameterGroup.class), null);

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
        verify(supervisionAcHandler).prepare(any(AutomationComposition.class),
                any(AutomationCompositionDefinition.class));

        automationComposition.setDeployState(DeployState.DEPLOYED);
        automationComposition.setLockState(LockState.LOCKED);
        acInstanceStateUpdate.setDeployOrder(DeployOrder.NONE);
        acInstanceStateUpdate.setLockOrder(LockOrder.NONE);
        acInstanceStateUpdate.setSubOrder(SubOrder.REVIEW);
        provider.compositionInstanceState(compositionId, instanceId, acInstanceStateUpdate);
        verify(supervisionAcHandler).review(any(AutomationComposition.class),
                any(AutomationCompositionDefinition.class));
    }
}
