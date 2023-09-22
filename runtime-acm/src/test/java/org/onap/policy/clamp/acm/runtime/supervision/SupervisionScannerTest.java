/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation.
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
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
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

class SupervisionScannerTest {

    private static final String AC_JSON = "src/test/resources/rest/acm/AutomationCompositionSmoke.json";

    private static final UUID compositionId = UUID.randomUUID();

    private AcDefinitionProvider createAcDefinitionProvider(AcTypeState acTypeState,
            StateChangeResult stateChangeResult) {
        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        var acDefinition = new AutomationCompositionDefinition();
        acDefinition.setState(acTypeState);
        acDefinition.setStateChangeResult(stateChangeResult);
        acDefinition.setCompositionId(compositionId);
        acDefinition.setServiceTemplate(serviceTemplate);
        var acDefinitionProvider = mock(AcDefinitionProvider.class);
        when(acDefinitionProvider.getAllAcDefinitions()).thenReturn(List.of(Objects.requireNonNull(acDefinition)));
        return acDefinitionProvider;
    }

    private AcDefinitionProvider createAcDefinitionProvider() {
        return createAcDefinitionProvider(AcTypeState.PRIMED, StateChangeResult.NO_ERROR);
    }

    @Test
    void testScannerOrderedFailed() {
        var acDefinitionProvider = createAcDefinitionProvider(AcTypeState.PRIMING, StateChangeResult.FAILED);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");
        var supervisionScanner = new SupervisionScanner(mock(AutomationCompositionProvider.class), acDefinitionProvider,
                mock(AutomationCompositionStateChangePublisher.class), mock(AutomationCompositionDeployPublisher.class),
                acRuntimeParameterGroup);
        supervisionScanner.run();
        verify(acDefinitionProvider, times(0)).updateAcDefinition(any(AutomationCompositionDefinition.class),
                eq(CommonTestData.TOSCA_COMP_NAME));
    }

    @Test
    void testScannerOrderedPriming() {
        var acDefinitionProvider = createAcDefinitionProvider(AcTypeState.PRIMING, StateChangeResult.NO_ERROR);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");
        var supervisionScanner = new SupervisionScanner(mock(AutomationCompositionProvider.class), acDefinitionProvider,
                mock(AutomationCompositionStateChangePublisher.class), mock(AutomationCompositionDeployPublisher.class),
                acRuntimeParameterGroup);
        supervisionScanner.run();
        verify(acDefinitionProvider, times(0)).updateAcDefinition(any(AutomationCompositionDefinition.class),
                eq(CommonTestData.TOSCA_COMP_NAME));

        acRuntimeParameterGroup.getParticipantParameters().setMaxStatusWaitMs(-1);
        supervisionScanner = new SupervisionScanner(mock(AutomationCompositionProvider.class), acDefinitionProvider,
                mock(AutomationCompositionStateChangePublisher.class), mock(AutomationCompositionDeployPublisher.class),
                acRuntimeParameterGroup);
        supervisionScanner.run();
        verify(acDefinitionProvider).updateAcDefinition(any(AutomationCompositionDefinition.class),
                eq(CommonTestData.TOSCA_COMP_NAME));
    }

    @Test
    void testScannerOrderedStateEqualsToState() {
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var automationCompositionStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var automationCompositionDeployPublisher = mock(AutomationCompositionDeployPublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        when(automationCompositionProvider.getAcInstancesByCompositionId(compositionId))
                .thenReturn(List.of(automationComposition));

        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, createAcDefinitionProvider(),
                automationCompositionStateChangePublisher, automationCompositionDeployPublisher,
                acRuntimeParameterGroup);
        supervisionScanner.run();

        verify(automationCompositionProvider, times(0)).updateAutomationComposition(any(AutomationComposition.class));
    }

    @Test
    void testScannerOrderedStateDifferentToState() {
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        automationComposition.setDeployState(DeployState.UNDEPLOYING);
        automationComposition.setLockState(LockState.NONE);
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        when(automationCompositionProvider.getAcInstancesByCompositionId(compositionId))
                .thenReturn(List.of(automationComposition));

        var automationCompositionDeployPublisher = mock(AutomationCompositionDeployPublisher.class);
        var automationCompositionStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, createAcDefinitionProvider(),
                automationCompositionStateChangePublisher, automationCompositionDeployPublisher,
                acRuntimeParameterGroup);
        supervisionScanner.run();

        verify(automationCompositionProvider).updateAutomationComposition(any(AutomationComposition.class));
    }

    @Test
    void testScannerDelete() {
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        automationComposition.setDeployState(DeployState.DELETING);
        automationComposition.setLockState(LockState.NONE);
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        when(automationCompositionProvider.getAcInstancesByCompositionId(compositionId))
                .thenReturn(List.of(automationComposition));

        var automationCompositionDeployPublisher = mock(AutomationCompositionDeployPublisher.class);
        var automationCompositionStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, createAcDefinitionProvider(),
                automationCompositionStateChangePublisher, automationCompositionDeployPublisher,
                acRuntimeParameterGroup);
        supervisionScanner.run();

        verify(automationCompositionProvider).deleteAutomationComposition(automationComposition.getInstanceId());
    }

    @Test
    void testScanner() {
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var automationComposition = new AutomationComposition();
        when(automationCompositionProvider.getAcInstancesByCompositionId(compositionId))
                .thenReturn(List.of(automationComposition));

        var automationCompositionDeployPublisher = mock(AutomationCompositionDeployPublisher.class);
        var automationCompositionStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, createAcDefinitionProvider(),
                automationCompositionStateChangePublisher, automationCompositionDeployPublisher,
                acRuntimeParameterGroup);

        supervisionScanner.run();
        verify(automationCompositionProvider, times(0)).updateAutomationComposition(any(AutomationComposition.class));
    }

    @Test
    void testScannerForTimeout() {
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        automationComposition.setDeployState(DeployState.DEPLOYING);
        automationComposition.setLockState(LockState.NONE);
        for (Map.Entry<UUID, AutomationCompositionElement> entry : automationComposition.getElements().entrySet()) {
            entry.getValue().setDeployState(DeployState.DEPLOYING);
        }
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        when(automationCompositionProvider.getAcInstancesByCompositionId(compositionId))
                .thenReturn(List.of(automationComposition));

        var automationCompositionDeployPublisher = mock(AutomationCompositionDeployPublisher.class);
        var automationCompositionStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");
        acRuntimeParameterGroup.getParticipantParameters().setMaxStatusWaitMs(-1);

        // verify timeout scenario
        var scannerObj2 = new SupervisionScanner(automationCompositionProvider, createAcDefinitionProvider(),
                automationCompositionStateChangePublisher, automationCompositionDeployPublisher,
                acRuntimeParameterGroup);

        scannerObj2.run();
        verify(automationCompositionProvider, times(1)).updateAutomationComposition(any(AutomationComposition.class));

    }

    @Test
    void testSendAutomationCompositionMsgUpdate() {
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        automationComposition.setDeployState(DeployState.DEPLOYING);
        automationComposition.setLockState(LockState.NONE);
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
        when(automationCompositionProvider.getAcInstancesByCompositionId(compositionId))
                .thenReturn(List.of(automationComposition));

        var automationCompositionDeployPublisher = mock(AutomationCompositionDeployPublisher.class);
        var automationCompositionStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, createAcDefinitionProvider(),
                automationCompositionStateChangePublisher, automationCompositionDeployPublisher,
                acRuntimeParameterGroup);

        supervisionScanner.run();

        verify(automationCompositionDeployPublisher).send(any(AutomationComposition.class),
                any(ToscaServiceTemplate.class), anyInt(), anyBoolean());
    }

    @Test
    void testSendAutomationCompositionMsgUnlocking() {
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        automationComposition.setDeployState(DeployState.DEPLOYED);
        automationComposition.setLockState(LockState.UNLOCKING);
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
        when(automationCompositionProvider.getAcInstancesByCompositionId(compositionId))
                .thenReturn(List.of(automationComposition));

        var automationCompositionDeployPublisher = mock(AutomationCompositionDeployPublisher.class);
        var automationCompositionStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var supervisionScanner = new SupervisionScanner(automationCompositionProvider, createAcDefinitionProvider(),
                automationCompositionStateChangePublisher, automationCompositionDeployPublisher,
                acRuntimeParameterGroup);

        supervisionScanner.run();

        verify(automationCompositionStateChangePublisher).send(any(AutomationComposition.class), anyInt(),
                anyBoolean());
    }
}
