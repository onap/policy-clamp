/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2025 Nordix Foundation.
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
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.onap.policy.clamp.acm.runtime.util.CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.acm.runtime.supervision.scanner.AcDefinitionScanner;
import org.onap.policy.clamp.acm.runtime.supervision.scanner.PhaseScanner;
import org.onap.policy.clamp.acm.runtime.supervision.scanner.SimpleScanner;
import org.onap.policy.clamp.acm.runtime.supervision.scanner.StageScanner;
import org.onap.policy.clamp.acm.runtime.supervision.scanner.UpdateSync;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.NodeTemplateState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.utils.TimestampHelper;

class SupervisionScannerTest {

    private static final String AC_JSON = "src/test/resources/rest/acm/AutomationCompositionSmoke.json";

    private static final UUID compositionId = UUID.randomUUID();

    private AutomationCompositionDefinition createAutomationCompositionDefinition(AcTypeState acTypeState,
                                                                                  StateChangeResult stateChangeResult) {
        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        var acDefinition = new AutomationCompositionDefinition();
        acDefinition.setState(acTypeState);
        acDefinition.setStateChangeResult(stateChangeResult);
        acDefinition.setCompositionId(compositionId);
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
            when(acDefinitionProvider.getAllAcDefinitionsInTransition()).thenReturn(List.of(acDefinition));
            when(acDefinitionProvider.getAcDefinition(acDefinition.getCompositionId()))
                    .thenReturn(Objects.requireNonNull(acDefinition));
            when(acDefinitionProvider.findAcDefinition(acDefinition.getCompositionId()))
                    .thenReturn(Optional.of(Objects.requireNonNull(acDefinition)));
        }
        when(acDefinitionProvider.getAcDefinition(compositionId)).thenReturn(acDefinition);
        return acDefinitionProvider;
    }

    private AcDefinitionProvider createAcDefinitionProvider(AcTypeState acTypeState,
        StateChangeResult stateChangeResult) {
        return createAcDefinitionProvider(createAutomationCompositionDefinition(acTypeState, stateChangeResult));
    }

    private AcDefinitionProvider createAcDefinitionProvider() {
        return createAcDefinitionProvider(AcTypeState.PRIMED, StateChangeResult.NO_ERROR);
    }

    @Test
    void testAcDefinition() {
        var acDefinitionProvider = createAcDefinitionProvider(AcTypeState.PRIMING, StateChangeResult.NO_ERROR);
        var acDefinitionScanner = mock(AcDefinitionScanner.class);
        var supervisionScanner = new SupervisionScanner(mock(AutomationCompositionProvider.class), acDefinitionProvider,
                acDefinitionScanner, mock(StageScanner.class), mock(SimpleScanner.class), mock(PhaseScanner.class));
        supervisionScanner.run();
        verify(acDefinitionScanner).scanAutomationCompositionDefinition(any(), any());
    }

    @Test
    void testAcNotInTransitionOrFailed() {
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);

        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        automationComposition.setCompositionId(Objects.requireNonNull(compositionId));
        when(automationCompositionProvider.getAcInstancesInTransition()).thenReturn(List.of(automationComposition));

        var stageScanner = mock(StageScanner.class);
        var simpleScanner = mock(SimpleScanner.class);
        var phaseScanner = mock(PhaseScanner.class);
        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, createAcDefinitionProvider(),
                mock(AcDefinitionScanner.class), stageScanner, simpleScanner, phaseScanner);

        // not in transition
        supervisionScanner.run();
        verify(stageScanner, times(0)).scanStage(any(), any(), any());
        verify(simpleScanner, times(0)).simpleScan(any(), any());
        verify(phaseScanner, times(0)).scanWithPhase(any(), any(), any());

        automationComposition.setDeployState(DeployState.DEPLOYING);
        automationComposition.setStateChangeResult(StateChangeResult.FAILED);
        supervisionScanner.run();
        // failed
        verify(stageScanner, times(0)).scanStage(any(), any(), any());
        verify(simpleScanner, times(0)).simpleScan(any(), any());
        verify(phaseScanner, times(0)).scanWithPhase(any(), any(), any());
    }

    @Test
    void testScanner() {
        var automationComposition = new AutomationComposition();
        automationComposition.setCompositionId(compositionId);
        automationComposition.setDeployState(DeployState.DEPLOYING);
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        when(automationCompositionProvider.getAcInstancesInTransition()).thenReturn(List.of(automationComposition));
        when(automationCompositionProvider.findAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(Optional.of(automationComposition));

        var stageScanner = mock(StageScanner.class);
        var simpleScanner = mock(SimpleScanner.class);
        var phaseScanner = mock(PhaseScanner.class);

        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, createAcDefinitionProvider(),
                mock(AcDefinitionScanner.class), stageScanner, simpleScanner, phaseScanner);

        supervisionScanner.run();
        verify(stageScanner, times(0)).scanStage(any(), any(), any());
        verify(simpleScanner, times(0)).simpleScan(any(), any());
        verify(phaseScanner).scanWithPhase(any(), any(), any());
    }

    @Test
    void testSendAutomationCompositionMigrate() {
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        automationComposition.setDeployState(DeployState.MIGRATING);
        automationComposition.setCompositionId(compositionId);
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
        when(automationCompositionProvider.getAcInstancesInTransition()).thenReturn(List.of(automationComposition));
        when(automationCompositionProvider.findAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(Optional.of(automationComposition));

        var definitionTarget = createAutomationCompositionDefinition(AcTypeState.PRIMED, StateChangeResult.NO_ERROR);
        definitionTarget.setCompositionId(compositionTargetId);
        var acDefinitionProvider = createAcDefinitionProvider();
        when(acDefinitionProvider.getAcDefinition(compositionTargetId)).thenReturn(definitionTarget);
        var stageScanner = mock(StageScanner.class);

        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, acDefinitionProvider,
                mock(AcDefinitionScanner.class), stageScanner, mock(SimpleScanner.class), mock(PhaseScanner.class));

        supervisionScanner.run();
        verify(stageScanner).scanStage(automationComposition, definitionTarget.getServiceTemplate(),
                new UpdateSync());
    }

    @Test
    void testSendAutomationCompositionSimpleScan() {
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        automationComposition.setLockState(LockState.LOCKED);
        automationComposition.setDeployState(DeployState.DEPLOYED);
        automationComposition.setSubState(SubState.MIGRATION_PRECHECKING);
        automationComposition.setLockState(LockState.NONE);
        automationComposition.setCompositionId(compositionId);
        automationComposition.setLastMsg(TimestampHelper.now());
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        when(automationCompositionProvider.getAcInstancesInTransition()).thenReturn(List.of(automationComposition));
        when(automationCompositionProvider.findAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(Optional.of(automationComposition));

        var simpleScanner = mock(SimpleScanner.class);
        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, createAcDefinitionProvider(),
                mock(AcDefinitionScanner.class), mock(StageScanner.class), simpleScanner, mock(PhaseScanner.class));
        supervisionScanner.run();
        verify(simpleScanner).simpleScan(automationComposition, new UpdateSync());

        clearInvocations(simpleScanner);
        automationComposition.setDeployState(DeployState.UNDEPLOYED);
        automationComposition.setSubState(SubState.PREPARING);
        supervisionScanner.run();
        verify(simpleScanner).simpleScan(automationComposition, new UpdateSync());

        clearInvocations(simpleScanner);
        automationComposition.setDeployState(DeployState.DEPLOYED);
        automationComposition.setSubState(SubState.REVIEWING);
        supervisionScanner.run();
        verify(simpleScanner).simpleScan(automationComposition, new UpdateSync());

        clearInvocations(simpleScanner);
        automationComposition.setDeployState(DeployState.UPDATING);
        automationComposition.setSubState(SubState.NONE);
        supervisionScanner.run();
        verify(simpleScanner).simpleScan(automationComposition, new UpdateSync());
    }
}
