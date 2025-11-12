/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.onap.policy.clamp.acm.runtime.util.CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.acm.runtime.main.utils.EncryptionUtils;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AcPreparePublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionMigrationPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantSyncPublisher;
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.utils.TimestampHelper;

class StageScannerTest {
    private static final String AC_JSON = "src/test/resources/rest/acm/AutomationCompositionSmoke.json";
    private static final UUID COMPOSITION_ID = UUID.randomUUID();

    @Test
    void testSendAutomationCompositionMigrate() {
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        assert automationComposition != null;
        automationComposition.setCompositionId(COMPOSITION_ID);
        var compositionTargetId = UUID.randomUUID();
        automationComposition.setCompositionTargetId(compositionTargetId);
        CommonTestData.modifyAcState(automationComposition, DeployState.MIGRATING);
        // first element is not migrated yet
        var element = automationComposition.getElements().entrySet().iterator().next().getValue();
        element.setDeployState(DeployState.MIGRATING);

        var acProvider = mock(AutomationCompositionProvider.class);
        when(acProvider.updateAutomationComposition(any())).thenReturn(automationComposition);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");
        var encryptionUtils = new EncryptionUtils(acRuntimeParameterGroup);
        var participantSyncPublisher = mock(ParticipantSyncPublisher.class);
        var supervisionScanner = new StageScanner(acProvider, participantSyncPublisher,
                mock(AutomationCompositionMigrationPublisher.class), mock(AcPreparePublisher.class),
                acRuntimeParameterGroup, encryptionUtils);
        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        var acDefinition = new AutomationCompositionDefinition();
        acDefinition.setServiceTemplate(serviceTemplate);
        supervisionScanner.scanStage(automationComposition, acDefinition, new UpdateSync(), UUID.randomUUID());
        verify(acProvider, times(0)).updateAutomationComposition(any(AutomationComposition.class));
        assertEquals(DeployState.MIGRATING, automationComposition.getDeployState());

        // send message for next stage
        clearInvocations(acProvider);
        var toscaNodeTemplate = serviceTemplate.getToscaTopologyTemplate().getNodeTemplates()
                .get(element.getDefinition().getName());
        toscaNodeTemplate.setProperties(Map.of("stage", List.of(1)));

        supervisionScanner.scanStage(automationComposition, acDefinition, new UpdateSync(), UUID.randomUUID());
        verify(acProvider).updateAutomationComposition(any(AutomationComposition.class));
        assertEquals(DeployState.MIGRATING, automationComposition.getDeployState());

        // first element is migrated
        clearInvocations(acProvider);
        element.setDeployState(DeployState.DEPLOYED);
        supervisionScanner.scanStage(automationComposition, acDefinition, new UpdateSync(), UUID.randomUUID());
        verify(acProvider).updateAutomationComposition(any(AutomationComposition.class));

        assertEquals(DeployState.DEPLOYED, automationComposition.getDeployState());
        assertEquals(compositionTargetId, automationComposition.getCompositionId());

        // remove all element for a participant
        clearInvocations(acProvider);
        clearInvocations(participantSyncPublisher);
        element.setDeployState(DeployState.DELETED);
        supervisionScanner.scanStage(automationComposition, acDefinition, new UpdateSync(), UUID.randomUUID());
        verify(acProvider).updateAutomationComposition(any(AutomationComposition.class));
        assertThat(automationComposition.getElements()).doesNotContainKey(element.getId()); //element deleted
        verify(participantSyncPublisher, times(1)).sendDeleteSync(automationComposition, element.getParticipantId());

        // remove one element; participant retains other elements
        clearInvocations(acProvider);
        clearInvocations(participantSyncPublisher);
        for (var e : automationComposition.getElements().values()) {
            e.setParticipantId(element.getParticipantId());
        }
        automationComposition.getElements().put(element.getId(), element);
        supervisionScanner.scanStage(automationComposition, acDefinition, new UpdateSync(), UUID.randomUUID());
        verify(acProvider).updateAutomationComposition(any(AutomationComposition.class));
        assertThat(automationComposition.getElements()).doesNotContainKey(element.getId()); //element deleted
        verify(participantSyncPublisher, times(0)).sendDeleteSync(automationComposition,
                element.getParticipantId());
    }

    @Test
    void testSendAutomationCompositionMigrationReverting() {
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        assert automationComposition != null;
        automationComposition.setCompositionId(COMPOSITION_ID);
        automationComposition.setCompositionTargetId(UUID.randomUUID());
        CommonTestData.modifyAcState(automationComposition, DeployState.MIGRATION_REVERTING);

        // first element is not migrated yet
        var element = automationComposition.getElements().entrySet().iterator().next().getValue();
        element.setDeployState(DeployState.MIGRATION_REVERTING);

        var acProvider = mock(AutomationCompositionProvider.class);
        when(acProvider.updateAutomationComposition(any())).thenReturn(automationComposition);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");
        var encryptionUtils = new EncryptionUtils(acRuntimeParameterGroup);
        var supervisionScanner = new StageScanner(acProvider, mock(ParticipantSyncPublisher.class),
                mock(AutomationCompositionMigrationPublisher.class), mock(AcPreparePublisher.class),
                acRuntimeParameterGroup, encryptionUtils);
        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        var acDefinition = new AutomationCompositionDefinition();
        acDefinition.setServiceTemplate(serviceTemplate);
        supervisionScanner.scanStage(automationComposition, acDefinition, new UpdateSync(), UUID.randomUUID());
        verify(acProvider, times(0)).updateAutomationComposition(any(AutomationComposition.class));
        assertEquals(DeployState.MIGRATION_REVERTING, automationComposition.getDeployState());

        // send message for next stage
        clearInvocations(acProvider);
        var toscaNodeTemplate = serviceTemplate.getToscaTopologyTemplate().getNodeTemplates()
                .get(element.getDefinition().getName());
        toscaNodeTemplate.setProperties(Map.of("stage", List.of(1)));

        supervisionScanner.scanStage(automationComposition, acDefinition, new UpdateSync(), UUID.randomUUID());
        verify(acProvider).updateAutomationComposition(any(AutomationComposition.class));
        assertEquals(DeployState.MIGRATION_REVERTING, automationComposition.getDeployState());

        // first element is migrated
        clearInvocations(acProvider);
        element.setDeployState(DeployState.DEPLOYED);
        supervisionScanner.scanStage(automationComposition, acDefinition, new UpdateSync(), UUID.randomUUID());
        verify(acProvider).updateAutomationComposition(any(AutomationComposition.class));

        assertEquals(DeployState.DEPLOYED, automationComposition.getDeployState());
    }

    @Test
    void testSendAutomationCompositionPrepare() {
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        automationComposition.setDeployState(DeployState.UNDEPLOYED);
        automationComposition.setSubState(SubState.PREPARING);
        automationComposition.setLockState(LockState.NONE);
        automationComposition.setCompositionId(COMPOSITION_ID);
        automationComposition.setLastMsg(TimestampHelper.now());
        automationComposition.setPhase(0);
        for (var element : automationComposition.getElements().values()) {
            element.setDeployState(DeployState.UNDEPLOYED);
            element.setLockState(LockState.NONE);
            element.setSubState(SubState.NONE);
        }
        // first element is not prepared yet
        var element = automationComposition.getElements().entrySet().iterator().next().getValue();
        element.setSubState(SubState.PREPARING);

        var acProvider = mock(AutomationCompositionProvider.class);
        when(acProvider.updateAutomationComposition(any())).thenReturn(automationComposition);

        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");
        var encryptionUtils = new EncryptionUtils(acRuntimeParameterGroup);
        var supervisionScanner = new StageScanner(acProvider, mock(ParticipantSyncPublisher.class),
                mock(AutomationCompositionMigrationPublisher.class), mock(AcPreparePublisher.class),
                acRuntimeParameterGroup, encryptionUtils);

        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        var acDefinition = new AutomationCompositionDefinition();
        acDefinition.setServiceTemplate(serviceTemplate);
        supervisionScanner.scanStage(automationComposition, acDefinition, new UpdateSync(), UUID.randomUUID());
        verify(acProvider, times(0)).updateAutomationComposition(any(AutomationComposition.class));
        assertEquals(SubState.PREPARING, automationComposition.getSubState());

        // send message for next stage
        clearInvocations(acProvider);
        var toscaNodeTemplate = serviceTemplate.getToscaTopologyTemplate().getNodeTemplates()
                .get(element.getDefinition().getName());
        var prepare = Map.of("prepare", List.of(1));
        toscaNodeTemplate.setProperties(Map.of("stage", prepare));

        supervisionScanner.scanStage(automationComposition, acDefinition, new UpdateSync(), UUID.randomUUID());
        verify(acProvider).updateAutomationComposition(any(AutomationComposition.class));
        assertEquals(SubState.PREPARING, automationComposition.getSubState());

        // first element is prepared
        clearInvocations(acProvider);
        element.setSubState(SubState.NONE);
        supervisionScanner.scanStage(automationComposition, acDefinition, new UpdateSync(), UUID.randomUUID());
        verify(acProvider).updateAutomationComposition(any(AutomationComposition.class));

        assertEquals(SubState.NONE, automationComposition.getSubState());
    }
}
