/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.onap.policy.clamp.acm.runtime.util.CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.acm.runtime.main.utils.EncryptionUtils;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AcPreparePublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionMigrationPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantSyncPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.scanner.AcDefinitionScanner;
import org.onap.policy.clamp.acm.runtime.supervision.scanner.MonitoringScanner;
import org.onap.policy.clamp.acm.runtime.supervision.scanner.PhaseScanner;
import org.onap.policy.clamp.acm.runtime.supervision.scanner.SimpleScanner;
import org.onap.policy.clamp.acm.runtime.supervision.scanner.StageScanner;
import org.onap.policy.clamp.acm.runtime.supervision.scanner.UpdateSync;
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.MigrationState;
import org.onap.policy.clamp.models.acm.concepts.NodeTemplateState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.clamp.models.acm.document.concepts.DocMessage;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.MessageProvider;
import org.onap.policy.clamp.models.acm.utils.TimestampHelper;

class SupervisionScannerTest {

    private static final String AC_JSON = "src/test/resources/rest/acm/AutomationCompositionSmoke.json";

    private static final UUID COMPOSITION_ID = UUID.randomUUID();
    private static final UUID INSTANCE_ID = UUID.randomUUID();
    private static final String JOB_ID = "JOB_ID";

    private AutomationCompositionDefinition createAutomationCompositionDefinition(AcTypeState acTypeState) {
        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        serviceTemplate.setMetadata(Map.of("compositionId", COMPOSITION_ID));
        var acDefinition = new AutomationCompositionDefinition();
        acDefinition.setState(acTypeState);
        acDefinition.setStateChangeResult(StateChangeResult.NO_ERROR);
        acDefinition.setCompositionId(COMPOSITION_ID);
        acDefinition.setLastMsg(TimestampHelper.now());
        acDefinition.setServiceTemplate(Objects.requireNonNull(serviceTemplate));
        var node = new NodeTemplateState();
        node.setState(AcTypeState.PRIMING);
        node.setNodeTemplateStateId(UUID.randomUUID());
        acDefinition.setElementStateMap(Map.of(node.getNodeTemplateStateId().toString(), node));
        return acDefinition;
    }

    private AcDefinitionProvider createAcDefinitionProvider(AutomationCompositionDefinition acDefinition) {
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        var acTypeState = acDefinition.getState();
        if (AcTypeState.PRIMING.equals(acTypeState) || AcTypeState.DEPRIMING.equals(acTypeState)) {
            Set<UUID> set = new HashSet<>();
            set.add(acDefinition.getCompositionId());
            when(acDefinitionProvider.getAllAcDefinitionsInTransition()).thenReturn(set);
            when(acDefinitionProvider.getAcDefinition(acDefinition.getCompositionId()))
                    .thenReturn(Objects.requireNonNull(acDefinition));
            when(acDefinitionProvider.findAcDefinition(acDefinition.getCompositionId()))
                    .thenReturn(Optional.of(Objects.requireNonNull(acDefinition)));
        }
        when(acDefinitionProvider.getAcDefinition(COMPOSITION_ID)).thenReturn(acDefinition);
        return acDefinitionProvider;
    }

    private AcDefinitionProvider createAcDefinitionProvider(AcTypeState acTypeState) {
        return createAcDefinitionProvider(createAutomationCompositionDefinition(acTypeState));
    }

    @Test
    void testAcDefinition() {
        var acDefinitionProvider = createAcDefinitionProvider(AcTypeState.PRIMING);
        var acDefinitionScanner = mock(AcDefinitionScanner.class);
        when(acDefinitionScanner.scanMessage(any(), any())).thenReturn(new UpdateSync());
        var messageProvider = mock(MessageProvider.class);
        when(messageProvider.createJob(COMPOSITION_ID)).thenReturn(Optional.of(JOB_ID));
        when(messageProvider.findCompositionMessages()).thenReturn(Set.of(COMPOSITION_ID));
        var message = new DocMessage();
        when(messageProvider.getAllMessages(COMPOSITION_ID)).thenReturn(List.of(message));
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var monitoringScanner = new MonitoringScanner(automationCompositionProvider, acDefinitionProvider,
                acDefinitionScanner, mock(StageScanner.class), mock(SimpleScanner.class), mock(PhaseScanner.class),
                messageProvider);
        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, acDefinitionProvider,
                messageProvider, monitoringScanner);
        supervisionScanner.run();
        verify(acDefinitionScanner).scanAutomationCompositionDefinition(any(), any());
        verify(messageProvider).removeMessage(message.getMessageId());
        verify(messageProvider).removeJob(JOB_ID);
    }

    @Test
    void testAcInstanceForMigrationSuccess() {
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        automationComposition.setDeployState(DeployState.MIGRATING);
        automationComposition.setInstanceId(INSTANCE_ID);
        automationComposition.setCompositionId(COMPOSITION_ID);
        var compositionTargetId = UUID.randomUUID();
        automationComposition.setCompositionTargetId(compositionTargetId);
        automationComposition.setLockState(LockState.LOCKED);
        automationComposition.setLastMsg(TimestampHelper.now());
        automationComposition.setPhase(0);
        List<UUID> elementIds = new ArrayList<>(automationComposition.getElements().keySet());

        Map<UUID, MigrationState> migrationStateMap = new HashMap<>();
        List<MigrationState> states = List.of(MigrationState.REMOVED, MigrationState.NEW, MigrationState.DEFAULT);

        for (int i = 0; i < elementIds.size(); i++) {
            migrationStateMap.put(elementIds.get(i), states.get(i));
        }

        for (var entry : automationComposition.getElements().entrySet()) {
            entry.getValue().setMigrationState(migrationStateMap.get(entry.getKey()));
        }

        for (var element : automationComposition.getElements().values()) {
            if (MigrationState.REMOVED.equals(migrationStateMap.get(element.getId()))) {
                element.setDeployState(DeployState.DELETED);
                element.setLockState(LockState.LOCKED);
            } else {
                element.setDeployState(DeployState.DEPLOYED);
                element.setLockState(LockState.LOCKED);
            }
        }

        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        Set<UUID> set = new HashSet<>();
        set.add(automationComposition.getInstanceId());
        when(automationCompositionProvider.getAcInstancesInTransition()).thenReturn(set);
        when(automationCompositionProvider.findAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(Optional.of(automationComposition));

        var acDefinitionTarget = createAutomationCompositionDefinition(AcTypeState.PRIMED);
        acDefinitionTarget.setCompositionId(compositionTargetId);
        var acDefinitionProvider = createAcDefinitionProvider(AcTypeState.PRIMED);
        when(acDefinitionProvider.getAcDefinition(compositionTargetId)).thenReturn(acDefinitionTarget);
        var acDefinition = new AutomationCompositionDefinition();
        acDefinition.setCompositionId(COMPOSITION_ID);
        when(acDefinitionProvider.getAcDefinition(COMPOSITION_ID)).thenReturn(acDefinition);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");
        var stageScanner = new StageScanner(automationCompositionProvider, mock(ParticipantSyncPublisher.class),
                mock(AutomationCompositionMigrationPublisher.class), mock(AcPreparePublisher.class),
                acRuntimeParameterGroup, mock(EncryptionUtils.class));

        var messageProvider = mock(MessageProvider.class);
        when(messageProvider.createJob(automationComposition.getInstanceId())).thenReturn(Optional.of(JOB_ID));
        var monitoringScanner = new MonitoringScanner(automationCompositionProvider, acDefinitionProvider,
                mock(AcDefinitionScanner.class), stageScanner, mock(SimpleScanner.class), mock(PhaseScanner.class),
                messageProvider);
        when(automationCompositionProvider.getAutomationComposition(any())).thenReturn(automationComposition);
        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, acDefinitionProvider,
                messageProvider, monitoringScanner);

        supervisionScanner.run();
        assertEquals(2, automationComposition.getElements().size());
        assertEquals(DeployState.DEPLOYED, automationComposition.getDeployState());
        for (var entry : automationComposition.getElements().entrySet()) {
            assertEquals(MigrationState.DEFAULT, entry.getValue().getMigrationState());
        }
        verify(messageProvider).removeJob(JOB_ID);
    }

    @Test
    void testAcDefinitionJobExist() {
        var acDefinitionProvider = createAcDefinitionProvider(AcTypeState.PRIMING);
        var acDefinitionScanner = mock(AcDefinitionScanner.class);
        var messageProvider = mock(MessageProvider.class);
        when(messageProvider.createJob(COMPOSITION_ID)).thenReturn(Optional.empty());
        when(messageProvider.findCompositionMessages()).thenReturn(Set.of());
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var monitoringScanner = new MonitoringScanner(automationCompositionProvider, acDefinitionProvider,
                acDefinitionScanner, mock(StageScanner.class), mock(SimpleScanner.class), mock(PhaseScanner.class),
                messageProvider);
        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, acDefinitionProvider,
                messageProvider, monitoringScanner);
        supervisionScanner.run();
        verify(acDefinitionScanner, times(0)).scanAutomationCompositionDefinition(any(), any());
    }

    @Test
    void testAcNotInTransitionOrFailed() {
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        automationComposition.setInstanceId(Objects.requireNonNull(INSTANCE_ID));
        automationComposition.setCompositionId(Objects.requireNonNull(COMPOSITION_ID));
        Set<UUID> set = new HashSet<>();
        set.add(automationComposition.getInstanceId());
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        when(automationCompositionProvider.getAcInstancesInTransition()).thenReturn(set);

        var stageScanner = mock(StageScanner.class);
        var simpleScanner = mock(SimpleScanner.class);
        var phaseScanner = mock(PhaseScanner.class);
        var messageProvider = mock(MessageProvider.class);
        var acDefinitionProvider = createAcDefinitionProvider(AcTypeState.PRIMED);
        var monitoringScanner = new MonitoringScanner(automationCompositionProvider, acDefinitionProvider,
                mock(AcDefinitionScanner.class), stageScanner, simpleScanner, phaseScanner, messageProvider);
        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, acDefinitionProvider,
                messageProvider, monitoringScanner);

        // not in transition
        supervisionScanner.run();
        verifyNoInteraction(stageScanner, simpleScanner, phaseScanner);

        // failed
        automationComposition.setDeployState(DeployState.DEPLOYING);
        automationComposition.setStateChangeResult(StateChangeResult.FAILED);
        supervisionScanner.run();
        verifyNoInteraction(stageScanner, simpleScanner, phaseScanner);

        // job already exist
        automationComposition.setStateChangeResult(StateChangeResult.NO_ERROR);
        when(messageProvider.createJob(automationComposition.getInstanceId())).thenReturn(Optional.empty());
        supervisionScanner.run();
        verifyNoInteraction(stageScanner, simpleScanner, phaseScanner);
    }

    @Test
    void testAcRemoved() {
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        automationComposition.setInstanceId(Objects.requireNonNull(INSTANCE_ID));
        automationComposition.setCompositionId(Objects.requireNonNull(COMPOSITION_ID));
        Set<UUID> set = new HashSet<>();
        set.add(automationComposition.getInstanceId());
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        when(automationCompositionProvider.getAcInstancesInTransition()).thenReturn(set);

        var stageScanner = mock(StageScanner.class);
        var simpleScanner = mock(SimpleScanner.class);
        var phaseScanner = mock(PhaseScanner.class);
        var messageProvider = mock(MessageProvider.class);
        when(messageProvider.createJob(automationComposition.getInstanceId())).thenReturn(Optional.of(JOB_ID));
        var acDefinitionProvider = createAcDefinitionProvider(AcTypeState.PRIMED);
        var monitoringScanner = new MonitoringScanner(automationCompositionProvider, acDefinitionProvider,
                mock(AcDefinitionScanner.class), stageScanner, simpleScanner, phaseScanner, messageProvider);
        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, acDefinitionProvider,
                messageProvider, monitoringScanner);

        // automationComposition not present in DB
        supervisionScanner.run();
        verifyNoInteraction(stageScanner, simpleScanner, phaseScanner);
        verify(messageProvider).removeJob(JOB_ID);
    }

    private void verifyNoInteraction(
            StageScanner stageScanner, SimpleScanner simpleScanner, PhaseScanner phaseScanner) {
        verify(stageScanner, times(0)).scanStage(any(), any(), any(), any());
        verify(simpleScanner, times(0)).simpleScan(any(), any());
        verify(phaseScanner, times(0)).scanWithPhase(any(), any(), any());
    }

    @Test
    void testScanner() {
        var automationComposition = new AutomationComposition();
        automationComposition.setInstanceId(INSTANCE_ID);
        automationComposition.setCompositionId(COMPOSITION_ID);
        automationComposition.setDeployState(DeployState.DEPLOYING);
        Set<UUID> set = new HashSet<>();
        set.add(automationComposition.getInstanceId());
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        when(automationCompositionProvider.getAcInstancesInTransition()).thenReturn(set);
        when(automationCompositionProvider.findAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(Optional.of(automationComposition));

        var stageScanner = mock(StageScanner.class);
        var simpleScanner = mock(SimpleScanner.class);
        when(simpleScanner.scanMessage(any(), any())).thenReturn(new UpdateSync());
        var phaseScanner = mock(PhaseScanner.class);

        var messageProvider = mock(MessageProvider.class);
        when(messageProvider.createJob(automationComposition.getInstanceId())).thenReturn(Optional.of(JOB_ID));
        var message = new  DocMessage();
        when(messageProvider.getAllMessages(INSTANCE_ID)).thenReturn(List.of(message));
        when(messageProvider.findInstanceMessages()).thenReturn(Set.of(INSTANCE_ID));

        var acDefinitionProvider = createAcDefinitionProvider(AcTypeState.PRIMED);
        var monitoringScanner = new MonitoringScanner(automationCompositionProvider, acDefinitionProvider,
                mock(AcDefinitionScanner.class), stageScanner, simpleScanner, phaseScanner, messageProvider);
        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, acDefinitionProvider,
                messageProvider, monitoringScanner);

        supervisionScanner.run();
        verify(stageScanner, times(0)).scanStage(any(), any(), any(), any());
        verify(simpleScanner, times(0)).simpleScan(any(), any());
        verify(phaseScanner).scanWithPhase(any(), any(), any());
        verify(messageProvider).removeMessage(message.getMessageId());
        verify(messageProvider).removeJob(JOB_ID);
    }

    @Test
    void testSendAutomationCompositionMigrate() {
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        automationComposition.setDeployState(DeployState.MIGRATING);
        automationComposition.setInstanceId(INSTANCE_ID);
        automationComposition.setCompositionId(COMPOSITION_ID);
        var compositionTargetId = UUID.randomUUID();
        automationComposition.setCompositionTargetId(compositionTargetId);
        automationComposition.setLockState(LockState.LOCKED);
        automationComposition.setLastMsg(TimestampHelper.now());
        automationComposition.setPhase(0);
        for (var element : automationComposition.getElements().values()) {
            element.setDeployState(DeployState.DEPLOYED);
            element.setLockState(LockState.LOCKED);
        }

        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        Set<UUID> set = new HashSet<>();
        set.add(automationComposition.getInstanceId());
        when(automationCompositionProvider.getAcInstancesInTransition()).thenReturn(set);
        when(automationCompositionProvider.findAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(Optional.of(automationComposition));

        var acDefinitionTarget = createAutomationCompositionDefinition(AcTypeState.PRIMED);
        acDefinitionTarget.setCompositionId(compositionTargetId);
        var acDefinitionProvider = createAcDefinitionProvider(AcTypeState.PRIMED);
        when(acDefinitionProvider.getAcDefinition(compositionTargetId)).thenReturn(acDefinitionTarget);
        var acDefinition = new AutomationCompositionDefinition();
        acDefinition.setCompositionId(COMPOSITION_ID);
        when(acDefinitionProvider.getAcDefinition(COMPOSITION_ID)).thenReturn(acDefinition);
        var stageScanner = mock(StageScanner.class);

        var messageProvider = mock(MessageProvider.class);
        when(messageProvider.createJob(automationComposition.getInstanceId())).thenReturn(Optional.of(JOB_ID));
        var monitoringScanner = new MonitoringScanner(automationCompositionProvider, acDefinitionProvider,
                mock(AcDefinitionScanner.class), stageScanner, mock(SimpleScanner.class), mock(PhaseScanner.class),
                messageProvider);
        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, acDefinitionProvider,
                messageProvider, monitoringScanner);

        supervisionScanner.run();
        verify(stageScanner).scanStage(automationComposition, acDefinitionTarget, new UpdateSync(),
                acDefinition.getRevisionId());
        verify(messageProvider).removeJob(JOB_ID);
    }

    @Test
    void testSendAutomationCompositionMigrationReverting() {
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        automationComposition.setDeployState(DeployState.MIGRATION_REVERTING);
        automationComposition.setInstanceId(INSTANCE_ID);
        automationComposition.setCompositionId(COMPOSITION_ID);
        var compositionTargetId = UUID.randomUUID();
        automationComposition.setCompositionTargetId(compositionTargetId);
        automationComposition.setLockState(LockState.LOCKED);
        automationComposition.setLastMsg(TimestampHelper.now());
        automationComposition.setPhase(0);
        for (var element : automationComposition.getElements().values()) {
            element.setDeployState(DeployState.DEPLOYED);
            element.setLockState(LockState.LOCKED);
        }

        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        Set<UUID> set = new HashSet<>();
        set.add(automationComposition.getInstanceId());
        when(automationCompositionProvider.getAcInstancesInTransition()).thenReturn(set);
        when(automationCompositionProvider.findAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(Optional.of(automationComposition));

        var acDefinitionTarget = createAutomationCompositionDefinition(AcTypeState.PRIMED);
        acDefinitionTarget.setCompositionId(compositionTargetId);
        var acDefinitionProvider = createAcDefinitionProvider(AcTypeState.PRIMED);
        when(acDefinitionProvider.getAcDefinition(compositionTargetId)).thenReturn(acDefinitionTarget);
        var acDefinition = new AutomationCompositionDefinition();
        acDefinition.setCompositionId(COMPOSITION_ID);
        when(acDefinitionProvider.getAcDefinition(COMPOSITION_ID)).thenReturn(acDefinition);
        var stageScanner = mock(StageScanner.class);

        var messageProvider = mock(MessageProvider.class);
        when(messageProvider.createJob(automationComposition.getInstanceId())).thenReturn(Optional.of(JOB_ID));
        var monitoringScanner = new MonitoringScanner(automationCompositionProvider, acDefinitionProvider,
                mock(AcDefinitionScanner.class), stageScanner, mock(SimpleScanner.class), mock(PhaseScanner.class),
                messageProvider);
        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, acDefinitionProvider,
                messageProvider, monitoringScanner);

        supervisionScanner.run();
        verify(stageScanner).scanStage(automationComposition, acDefinition, new UpdateSync(),
                acDefinitionTarget.getRevisionId());
        verify(messageProvider).removeJob(JOB_ID);
    }

    @Test
    void testSendAutomationCompositionSimpleScan() {
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        automationComposition.setLockState(LockState.LOCKED);
        automationComposition.setDeployState(DeployState.DEPLOYED);
        automationComposition.setSubState(SubState.MIGRATION_PRECHECKING);
        automationComposition.setLockState(LockState.NONE);
        automationComposition.setInstanceId(INSTANCE_ID);
        automationComposition.setCompositionId(COMPOSITION_ID);
        automationComposition.setLastMsg(TimestampHelper.now());
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        Set<UUID> set = new HashSet<>();
        set.add(automationComposition.getInstanceId());
        when(automationCompositionProvider.getAcInstancesInTransition()).thenReturn(set);
        when(automationCompositionProvider.findAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(Optional.of(automationComposition));

        var messageProvider = mock(MessageProvider.class);
        when(messageProvider.createJob(automationComposition.getInstanceId())).thenReturn(Optional.of(JOB_ID));

        var simpleScanner = mock(SimpleScanner.class);
        var acDefinitionProvider = createAcDefinitionProvider(AcTypeState.PRIMED);
        var monitoringScanner = new MonitoringScanner(automationCompositionProvider, acDefinitionProvider,
                mock(AcDefinitionScanner.class), mock(StageScanner.class), simpleScanner, mock(PhaseScanner.class),
                messageProvider);
        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, acDefinitionProvider,
                messageProvider, monitoringScanner);
        supervisionScanner.run();
        verify(simpleScanner).simpleScan(automationComposition, new UpdateSync());
        verify(messageProvider).removeJob(JOB_ID);

        clearInvocations(simpleScanner);
        clearInvocations(messageProvider);
        automationComposition.setDeployState(DeployState.DEPLOYED);
        automationComposition.setSubState(SubState.REVIEWING);
        supervisionScanner.run();
        verify(simpleScanner).simpleScan(automationComposition, new UpdateSync());
        verify(messageProvider).removeJob(JOB_ID);

        clearInvocations(simpleScanner);
        clearInvocations(messageProvider);
        automationComposition.setDeployState(DeployState.UPDATING);
        automationComposition.setSubState(SubState.NONE);
        supervisionScanner.run();
        verify(simpleScanner).simpleScan(automationComposition, new UpdateSync());
        verify(messageProvider).removeJob(JOB_ID);
    }

    @Test
    void testSaveAcByMessageUpdate() {
        var automationComposition = new AutomationComposition();
        automationComposition.setInstanceId(INSTANCE_ID);
        automationComposition.setCompositionId(COMPOSITION_ID);
        automationComposition.setDeployState(DeployState.DEPLOYED);
        automationComposition.setLockState(LockState.LOCKED);
        automationComposition.setStateChangeResult(StateChangeResult.NO_ERROR);
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        when(automationCompositionProvider.getAcInstancesInTransition()).thenReturn(new HashSet<>());
        when(automationCompositionProvider.findAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(Optional.of(automationComposition));

        var simpleScanner = mock(SimpleScanner.class);
        var updateSync = new UpdateSync();
        updateSync.setUpdated(true);
        when(simpleScanner.scanMessage(any(), any())).thenReturn(updateSync);

        var messageProvider = mock(MessageProvider.class);
        when(messageProvider.createJob(automationComposition.getInstanceId())).thenReturn(Optional.of(JOB_ID));
        var message = new  DocMessage();
        when(messageProvider.getAllMessages(INSTANCE_ID)).thenReturn(List.of(message));
        when(messageProvider.findInstanceMessages()).thenReturn(Set.of(INSTANCE_ID));

        var phaseScanner = mock(PhaseScanner.class);
        var stageScanner = mock(StageScanner.class);
        var acDefinitionProvider = createAcDefinitionProvider(AcTypeState.PRIMED);
        var monitoringScanner = new MonitoringScanner(automationCompositionProvider, acDefinitionProvider,
                mock(AcDefinitionScanner.class), stageScanner, simpleScanner, phaseScanner, messageProvider);
        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, acDefinitionProvider,
                messageProvider, monitoringScanner);

        supervisionScanner.run();
        verifyNoInteraction(stageScanner, simpleScanner, phaseScanner);
        verify(simpleScanner).saveAndSync(any(), any());
        verify(messageProvider).removeMessage(message.getMessageId());
        verify(messageProvider).removeJob(JOB_ID);
    }
}
