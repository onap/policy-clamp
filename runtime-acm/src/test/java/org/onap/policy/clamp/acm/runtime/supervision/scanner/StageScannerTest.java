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
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionMigrationPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantSyncPublisher;
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.utils.TimestampHelper;

class StageScannerTest {
    private static final String AC_JSON = "src/test/resources/rest/acm/AutomationCompositionSmoke.json";
    private static final UUID COMPOSITION_ID = UUID.randomUUID();

    @Test
    void testSendAutomationCompositionMigrate() {
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        automationComposition.setInstanceId(UUID.randomUUID());
        automationComposition.setDeployState(DeployState.MIGRATING);
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
        // first element is not migrated yet
        var element = automationComposition.getElements().entrySet().iterator().next().getValue();
        element.setDeployState(DeployState.MIGRATING);

        var acProvider = mock(AutomationCompositionProvider.class);
        when(acProvider.updateAutomationComposition(any())).thenReturn(automationComposition);

        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");
        var encryptionUtils = new EncryptionUtils(acRuntimeParameterGroup);
        var supervisionScanner = new StageScanner(acProvider, mock(ParticipantSyncPublisher.class),
                mock(AutomationCompositionMigrationPublisher.class), acRuntimeParameterGroup, encryptionUtils);

        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        supervisionScanner.scanStage(automationComposition, serviceTemplate, new UpdateSync());
        verify(acProvider, times(0)).updateAutomationComposition(any(AutomationComposition.class));
        assertEquals(DeployState.MIGRATING, automationComposition.getDeployState());

        // send message for next stage
        clearInvocations(acProvider);
        var toscaNodeTemplate = serviceTemplate.getToscaTopologyTemplate().getNodeTemplates()
                .get(element.getDefinition().getName());
        toscaNodeTemplate.setProperties(Map.of("stage", List.of(1)));

        supervisionScanner.scanStage(automationComposition, serviceTemplate, new UpdateSync());
        verify(acProvider).updateAutomationComposition(any(AutomationComposition.class));
        assertEquals(DeployState.MIGRATING, automationComposition.getDeployState());

        // first element is migrated
        clearInvocations(acProvider);
        element.setDeployState(DeployState.DEPLOYED);
        supervisionScanner.scanStage(automationComposition, serviceTemplate, new UpdateSync());
        verify(acProvider).updateAutomationComposition(any(AutomationComposition.class));

        assertEquals(DeployState.DEPLOYED, automationComposition.getDeployState());
        assertEquals(compositionTargetId, automationComposition.getCompositionId());
    }
}
