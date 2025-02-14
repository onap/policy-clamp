/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 Nordix Foundation.
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

package org.onap.policy.clamp.acm.runtime.supervision.scanner;

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

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionDeployPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionStateChangePublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantSyncPublisher;
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.utils.TimestampHelper;

class PhaseScannerTest {

    private static final String AC_JSON = "src/test/resources/rest/acm/AutomationCompositionSmoke.json";
    private static final UUID COMPOSITION_ID = UUID.randomUUID();
    private static final UUID INSTANCE_ID = UUID.randomUUID();
    private static final String ELEMENT_NAME =
            "org.onap.domain.database.Http_PMSHMicroserviceAutomationCompositionElement";

    @Test
    void testAcUndeployCompleted() {
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        automationComposition.setInstanceId(INSTANCE_ID);
        automationComposition.setDeployState(DeployState.UNDEPLOYING);
        automationComposition.setLockState(LockState.NONE);
        automationComposition.setCompositionId(COMPOSITION_ID);
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        when(automationCompositionProvider.updateAutomationComposition(any())).thenReturn(automationComposition);

        var acDeployPublisher = mock(AutomationCompositionDeployPublisher.class);
        var acStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var phaseScanner = new PhaseScanner(automationCompositionProvider, mock(ParticipantSyncPublisher.class),
                acStateChangePublisher, acDeployPublisher, acRuntimeParameterGroup);
        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        phaseScanner.scanWithPhase(automationComposition, serviceTemplate, new UpdateSync());

        verify(automationCompositionProvider).updateAutomationComposition(any(AutomationComposition.class));
    }

    @Test
    void testAcDeleted() {
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        automationComposition.setInstanceId(INSTANCE_ID);
        automationComposition.setDeployState(DeployState.DELETING);
        automationComposition.setLockState(LockState.NONE);
        automationComposition.setCompositionId(COMPOSITION_ID);
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var acDeployPublisher = mock(AutomationCompositionDeployPublisher.class);
        var acStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var phaseScanner = new PhaseScanner(automationCompositionProvider, mock(ParticipantSyncPublisher.class),
                acStateChangePublisher, acDeployPublisher, acRuntimeParameterGroup);
        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        phaseScanner.scanWithPhase(automationComposition, serviceTemplate, new UpdateSync());

        verify(automationCompositionProvider).deleteAutomationComposition(automationComposition.getInstanceId());
    }

    @Test
    void testScannerForTimeout() {
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        automationComposition.setInstanceId(INSTANCE_ID);
        automationComposition.setDeployState(DeployState.DEPLOYING);
        automationComposition.setLockState(LockState.NONE);
        automationComposition.setPhase(0);
        automationComposition.setCompositionId(COMPOSITION_ID);
        for (var entry : automationComposition.getElements().entrySet()) {
            entry.getValue().setDeployState(DeployState.DEPLOYING);
        }
        // the first element is already completed
        automationComposition.getElements().entrySet().iterator().next().getValue()
                .setDeployState(DeployState.DEPLOYED);

        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        when(automationCompositionProvider.updateAutomationComposition(any())).thenReturn(automationComposition);
        var acDeployPublisher = mock(AutomationCompositionDeployPublisher.class);
        var acStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var participantSyncPublisher = mock(ParticipantSyncPublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");
        acRuntimeParameterGroup.getParticipantParameters().setMaxStatusWaitMs(-1);

        // verify timeout scenario
        var phaseScanner = new PhaseScanner(automationCompositionProvider, participantSyncPublisher,
                acStateChangePublisher, acDeployPublisher, acRuntimeParameterGroup);

        automationComposition.setStateChangeResult(StateChangeResult.NO_ERROR);
        automationComposition.setLastMsg(TimestampHelper.now());
        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        phaseScanner.scanWithPhase(automationComposition, serviceTemplate, new UpdateSync());
        verify(automationCompositionProvider).updateAutomationComposition(any(AutomationComposition.class));
        verify(participantSyncPublisher).sendSync(any(AutomationComposition.class));
        assertEquals(StateChangeResult.TIMEOUT, automationComposition.getStateChangeResult());

        //already in TIMEOUT
        clearInvocations(automationCompositionProvider);
        clearInvocations(participantSyncPublisher);
        phaseScanner.scanWithPhase(automationComposition, serviceTemplate, new UpdateSync());
        verify(automationCompositionProvider, times(0)).updateAutomationComposition(any(AutomationComposition.class));
        verify(participantSyncPublisher, times(0))
                .sendSync(any(AutomationComposition.class));

        clearInvocations(automationCompositionProvider);
        clearInvocations(participantSyncPublisher);
        for (Map.Entry<UUID, AutomationCompositionElement> entry : automationComposition.getElements().entrySet()) {
            entry.getValue().setDeployState(DeployState.DEPLOYED);
        }
        phaseScanner.scanWithPhase(automationComposition, serviceTemplate, new UpdateSync());
        verify(automationCompositionProvider).updateAutomationComposition(any(AutomationComposition.class));
        verify(participantSyncPublisher).sendSync(any(AutomationComposition.class));
        assertEquals(StateChangeResult.NO_ERROR, automationComposition.getStateChangeResult());
    }

    @Test
    void testSendAutomationCompositionMsgStartPhase() {
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        automationComposition.setInstanceId(INSTANCE_ID);
        automationComposition.setDeployState(DeployState.DEPLOYING);
        automationComposition.setLockState(LockState.NONE);
        automationComposition.setPhase(0);
        automationComposition.setCompositionId(COMPOSITION_ID);
        for (var element : automationComposition.getElements().values()) {
            if (ELEMENT_NAME.equals(element.getDefinition().getName())) {
                element.setDeployState(DeployState.DEPLOYING);
                element.setLockState(LockState.NONE);
            } else {
                element.setDeployState(DeployState.DEPLOYED);
                element.setLockState(LockState.LOCKED);
            }
        }

        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var acDeployPublisher = mock(AutomationCompositionDeployPublisher.class);
        var acStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var phaseScanner = new PhaseScanner(automationCompositionProvider, mock(ParticipantSyncPublisher.class),
                acStateChangePublisher, acDeployPublisher, acRuntimeParameterGroup);

        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        phaseScanner.scanWithPhase(automationComposition, serviceTemplate, new UpdateSync());

        verify(acDeployPublisher).send(any(AutomationComposition.class), anyInt(), anyBoolean());
    }

    @Test
    void testStartPhaseWithNull() {
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        automationComposition.setInstanceId(INSTANCE_ID);
        automationComposition.setDeployState(DeployState.DEPLOYING);
        automationComposition.setLockState(LockState.NONE);
        automationComposition.setPhase(0);
        automationComposition.setLastMsg(TimestampHelper.now());
        automationComposition.setCompositionId(COMPOSITION_ID);
        for (var element : automationComposition.getElements().values()) {
            if (ELEMENT_NAME.equals(element.getDefinition().getName())) {
                element.setDeployState(DeployState.DEPLOYING);
                element.getDefinition().setName("NotExistElement");
                element.setLockState(LockState.NONE);
            } else {
                element.setDeployState(DeployState.DEPLOYING);
                element.getDefinition().setVersion("0.0.0");
                element.setLockState(LockState.NONE);
            }
        }

        var acProvider = mock(AutomationCompositionProvider.class);
        var acDeployPublisher = mock(AutomationCompositionDeployPublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var phaseScanner = new PhaseScanner(acProvider, mock(ParticipantSyncPublisher.class),
                mock(AutomationCompositionStateChangePublisher.class), acDeployPublisher,
                acRuntimeParameterGroup);

        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        phaseScanner.scanWithPhase(automationComposition, serviceTemplate, new UpdateSync());

        verify(acDeployPublisher, times(0))
                .send(any(AutomationComposition.class), anyInt(), anyBoolean());
    }

    @Test
    void testSendAutomationCompositionMsgUnlocking() {
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        automationComposition.setInstanceId(INSTANCE_ID);
        automationComposition.setDeployState(DeployState.DEPLOYED);
        automationComposition.setLockState(LockState.UNLOCKING);
        automationComposition.setCompositionId(COMPOSITION_ID);
        automationComposition.setPhase(0);
        for (var element : automationComposition.getElements().values()) {
            if (ELEMENT_NAME.equals(element.getDefinition().getName())) {
                element.setDeployState(DeployState.DEPLOYED);
                element.setLockState(LockState.UNLOCKING);
            } else {
                element.setDeployState(DeployState.DEPLOYED);
                element.setLockState(LockState.UNLOCKED);
            }
        }

        var acProvider = mock(AutomationCompositionProvider.class);
        var acDeployPublisher = mock(AutomationCompositionDeployPublisher.class);
        var acStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");

        var phaseScanner = new PhaseScanner(acProvider, mock(ParticipantSyncPublisher.class),
                acStateChangePublisher, acDeployPublisher, acRuntimeParameterGroup);

        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        phaseScanner.scanWithPhase(automationComposition, serviceTemplate, new UpdateSync());

        verify(acStateChangePublisher).send(any(AutomationComposition.class), anyInt(),
                anyBoolean());
    }
}
