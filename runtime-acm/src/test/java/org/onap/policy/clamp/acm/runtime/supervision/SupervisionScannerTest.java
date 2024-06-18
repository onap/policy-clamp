/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation.
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.onap.policy.clamp.acm.runtime.util.CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionDeployPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionStateChangePublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantSyncPublisher;
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.NodeTemplateState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.utils.TimestampHelper;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

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
        acDefinition.setServiceTemplate(serviceTemplate);
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
            when(acDefinitionProvider.getAllAcDefinitionsInTransition())
                .thenReturn(List.of(Objects.requireNonNull(acDefinition)));
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
    void testAcDefinitionPrimeFailed() {
        var acDefinitionProvider = createAcDefinitionProvider(AcTypeState.PRIMING, StateChangeResult.FAILED);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");
        var supervisionScanner = new SupervisionScanner(mock(AutomationCompositionProvider.class), acDefinitionProvider,
                mock(AutomationCompositionStateChangePublisher.class), mock(AutomationCompositionDeployPublisher.class),
                mock(ParticipantSyncPublisher.class), acRuntimeParameterGroup);
        supervisionScanner.run();
        verify(acDefinitionProvider, times(0)).updateAcDefinitionState(any(), any(), any(), any());
    }

    @Test
    void testAcDefinitionPrimeTimeout() {
        var acDefinition = createAutomationCompositionDefinition(AcTypeState.PRIMING, StateChangeResult.NO_ERROR);
        var acDefinitionProvider = createAcDefinitionProvider(acDefinition);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");
        var supervisionScanner = new SupervisionScanner(mock(AutomationCompositionProvider.class), acDefinitionProvider,
                mock(AutomationCompositionStateChangePublisher.class), mock(AutomationCompositionDeployPublisher.class),
                mock(ParticipantSyncPublisher.class), acRuntimeParameterGroup);
        supervisionScanner.run();
        // Ac Definition in Priming state
        verify(acDefinitionProvider, times(0)).updateAcDefinitionState(any(), any(), any(), any());

        acRuntimeParameterGroup.getParticipantParameters().setMaxStatusWaitMs(-1);
        supervisionScanner = new SupervisionScanner(mock(AutomationCompositionProvider.class), acDefinitionProvider,
                mock(AutomationCompositionStateChangePublisher.class), mock(AutomationCompositionDeployPublisher.class),
                mock(ParticipantSyncPublisher.class), acRuntimeParameterGroup);
        supervisionScanner.run();
        // set Timeout
        verify(acDefinitionProvider).updateAcDefinitionState(acDefinition.getCompositionId(), acDefinition.getState(),
            StateChangeResult.TIMEOUT, null);

        clearInvocations(acDefinitionProvider);
        acDefinition.setStateChangeResult(StateChangeResult.TIMEOUT);
        supervisionScanner.run();
        // already in Timeout
        verify(acDefinitionProvider, times(0)).updateAcDefinitionState(any(), any(), any(), any());

        clearInvocations(acDefinitionProvider);
        // retry by the user
        acDefinition.setStateChangeResult(StateChangeResult.NO_ERROR);
        supervisionScanner.run();
        // set Timeout
        verify(acDefinitionProvider).updateAcDefinitionState(acDefinition.getCompositionId(), acDefinition.getState(),
            StateChangeResult.TIMEOUT, null);

        clearInvocations(acDefinitionProvider);
        for (var element : acDefinition.getElementStateMap().values()) {
            element.setState(AcTypeState.PRIMED);
        }
        supervisionScanner.run();
        // completed
        verify(acDefinitionProvider).updateAcDefinitionState(acDefinition.getCompositionId(), AcTypeState.PRIMED,
            StateChangeResult.NO_ERROR, null);
    }

    @Test
    void testAcNotInTransitionOrFailed() {
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var automationCompositionStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var automationCompositionDeployPublisher = mock(AutomationCompositionDeployPublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        automationComposition.setCompositionId(compositionId);
        when(automationCompositionProvider.getAcInstancesInTransition()).thenReturn(List.of(automationComposition));

        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, createAcDefinitionProvider(),
                automationCompositionStateChangePublisher, automationCompositionDeployPublisher,
                mock(ParticipantSyncPublisher.class), acRuntimeParameterGroup);

        // not in transition
        supervisionScanner.run();
        verify(automationCompositionProvider, times(0)).updateAutomationComposition(any(AutomationComposition.class));

        automationComposition.setDeployState(DeployState.DEPLOYING);
        automationComposition.setStateChangeResult(StateChangeResult.FAILED);
        supervisionScanner.run();
        // failed
        verify(automationCompositionProvider, times(0)).updateAutomationComposition(any(AutomationComposition.class));
    }

    @Test
    void testAcUndeployCompleted() {
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        automationComposition.setDeployState(DeployState.UNDEPLOYING);
        automationComposition.setLockState(LockState.NONE);
        automationComposition.setCompositionId(compositionId);
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        when(automationCompositionProvider.getAcInstancesInTransition()).thenReturn(List.of(automationComposition));

        var automationCompositionDeployPublisher = mock(AutomationCompositionDeployPublisher.class);
        var automationCompositionStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, createAcDefinitionProvider(),
                automationCompositionStateChangePublisher, automationCompositionDeployPublisher,
                mock(ParticipantSyncPublisher.class), acRuntimeParameterGroup);
        supervisionScanner.run();

        verify(automationCompositionProvider).updateAutomationComposition(any(AutomationComposition.class));
    }

    @Test
    void testAcDeleted() {
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        automationComposition.setDeployState(DeployState.DELETING);
        automationComposition.setLockState(LockState.NONE);
        automationComposition.setCompositionId(compositionId);
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        when(automationCompositionProvider.getAcInstancesInTransition()).thenReturn(List.of(automationComposition));

        var automationCompositionDeployPublisher = mock(AutomationCompositionDeployPublisher.class);
        var automationCompositionStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, createAcDefinitionProvider(),
                automationCompositionStateChangePublisher, automationCompositionDeployPublisher,
                mock(ParticipantSyncPublisher.class), acRuntimeParameterGroup);
        supervisionScanner.run();

        verify(automationCompositionProvider).deleteAutomationComposition(automationComposition.getInstanceId());
    }

    @Test
    void testScanner() {
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var automationComposition = new AutomationComposition();
        automationComposition.setCompositionId(compositionId);
        when(automationCompositionProvider.getAcInstancesInTransition()).thenReturn(List.of(automationComposition));

        var automationCompositionDeployPublisher = mock(AutomationCompositionDeployPublisher.class);
        var automationCompositionStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, createAcDefinitionProvider(),
                automationCompositionStateChangePublisher, automationCompositionDeployPublisher,
                mock(ParticipantSyncPublisher.class), acRuntimeParameterGroup);

        supervisionScanner.run();
        verify(automationCompositionProvider, times(0)).updateAutomationComposition(any(AutomationComposition.class));
    }

    @Test
    void testScannerForTimeout() {
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        automationComposition.setDeployState(DeployState.DEPLOYING);
        automationComposition.setLockState(LockState.NONE);
        automationComposition.setPhase(0);
        automationComposition.setCompositionId(compositionId);
        for (var entry : automationComposition.getElements().entrySet()) {
            entry.getValue().setDeployState(DeployState.DEPLOYING);
        }
        // the first element is already completed
        automationComposition.getElements().entrySet().iterator().next().getValue()
                .setDeployState(DeployState.DEPLOYED);

        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        when(automationCompositionProvider.getAcInstancesInTransition()).thenReturn(List.of(automationComposition));

        var automationCompositionDeployPublisher = mock(AutomationCompositionDeployPublisher.class);
        var automationCompositionStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");
        acRuntimeParameterGroup.getParticipantParameters().setMaxStatusWaitMs(-1);

        // verify timeout scenario
        var scannerObj2 = new SupervisionScanner(automationCompositionProvider, createAcDefinitionProvider(),
                automationCompositionStateChangePublisher, automationCompositionDeployPublisher,
                mock(ParticipantSyncPublisher.class), acRuntimeParameterGroup);

        automationComposition.setStateChangeResult(StateChangeResult.NO_ERROR);
        automationComposition.setLastMsg(TimestampHelper.now());
        scannerObj2.run();
        verify(automationCompositionProvider, times(1)).updateAutomationComposition(any(AutomationComposition.class));
        assertEquals(StateChangeResult.TIMEOUT, automationComposition.getStateChangeResult());

        //already in TIMEOUT
        clearInvocations(automationCompositionProvider);
        scannerObj2.run();
        verify(automationCompositionProvider, times(0)).updateAutomationComposition(any(AutomationComposition.class));

        clearInvocations(automationCompositionProvider);
        for (Map.Entry<UUID, AutomationCompositionElement> entry : automationComposition.getElements().entrySet()) {
            entry.getValue().setDeployState(DeployState.DEPLOYED);
        }
        scannerObj2.run();
        verify(automationCompositionProvider, times(1)).updateAutomationComposition(any(AutomationComposition.class));
        assertEquals(StateChangeResult.NO_ERROR, automationComposition.getStateChangeResult());
    }

    @Test
    void testSendAutomationCompositionMsgStartPhase() {
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        automationComposition.setDeployState(DeployState.DEPLOYING);
        automationComposition.setLockState(LockState.NONE);
        automationComposition.setPhase(0);
        automationComposition.setCompositionId(compositionId);
        for (var element : automationComposition.getElements().values()) {
            if ("org.onap.domain.database.Http_PMSHMicroserviceAutomationCompositionElement"
                    .equals(element.getDefinition().getName())) {
                element.setDeployState(DeployState.DEPLOYING);
                element.setLockState(LockState.NONE);
            } else {
                element.setDeployState(DeployState.DEPLOYED);
                element.setLockState(LockState.LOCKED);
            }
        }

        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        when(automationCompositionProvider.getAcInstancesInTransition()).thenReturn(List.of(automationComposition));

        var automationCompositionDeployPublisher = mock(AutomationCompositionDeployPublisher.class);
        var automationCompositionStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, createAcDefinitionProvider(),
                automationCompositionStateChangePublisher, automationCompositionDeployPublisher,
                mock(ParticipantSyncPublisher.class), acRuntimeParameterGroup);

        supervisionScanner.run();

        verify(automationCompositionDeployPublisher).send(any(AutomationComposition.class),
                any(ToscaServiceTemplate.class), anyInt(), anyBoolean());
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
        // first element is not migrated yet
        automationComposition.getElements().entrySet().iterator().next().getValue()
                .setDeployState(DeployState.MIGRATING);

        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        when(automationCompositionProvider.getAcInstancesInTransition()).thenReturn(List.of(automationComposition));

        var automationCompositionDeployPublisher = mock(AutomationCompositionDeployPublisher.class);
        var automationCompositionStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, createAcDefinitionProvider(),
                automationCompositionStateChangePublisher, automationCompositionDeployPublisher,
                mock(ParticipantSyncPublisher.class), acRuntimeParameterGroup);

        supervisionScanner.run();
        verify(automationCompositionProvider, times(0)).updateAutomationComposition(any(AutomationComposition.class));
        assertEquals(DeployState.MIGRATING, automationComposition.getDeployState());

        // first element is migrated
        automationComposition.getElements().entrySet().iterator().next().getValue()
                .setDeployState(DeployState.DEPLOYED);
        supervisionScanner.run();
        verify(automationCompositionProvider, times(1)).updateAutomationComposition(any(AutomationComposition.class));

        assertEquals(DeployState.DEPLOYED, automationComposition.getDeployState());
        assertEquals(compositionTargetId, automationComposition.getCompositionId());
    }

    @Test
    void testSendAutomationCompositionMsgUnlocking() {
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        automationComposition.setDeployState(DeployState.DEPLOYED);
        automationComposition.setLockState(LockState.UNLOCKING);
        automationComposition.setCompositionId(compositionId);
        automationComposition.setPhase(0);
        for (var element : automationComposition.getElements().values()) {
            if ("org.onap.domain.database.Http_PMSHMicroserviceAutomationCompositionElement"
                    .equals(element.getDefinition().getName())) {
                element.setDeployState(DeployState.DEPLOYED);
                element.setLockState(LockState.UNLOCKING);
            } else {
                element.setDeployState(DeployState.DEPLOYED);
                element.setLockState(LockState.UNLOCKED);
            }
        }

        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        when(automationCompositionProvider.getAcInstancesInTransition()).thenReturn(List.of(automationComposition));

        var automationCompositionDeployPublisher = mock(AutomationCompositionDeployPublisher.class);
        var automationCompositionStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, createAcDefinitionProvider(),
                automationCompositionStateChangePublisher, automationCompositionDeployPublisher,
                mock(ParticipantSyncPublisher.class), acRuntimeParameterGroup);

        supervisionScanner.run();

        verify(automationCompositionStateChangePublisher).send(any(AutomationComposition.class), anyInt(),
                anyBoolean());
    }
}
