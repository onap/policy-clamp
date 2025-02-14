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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantSyncPublisher;
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.clamp.models.acm.document.concepts.DocMessage;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.utils.TimestampHelper;

class SimpleScannerTest {

    private static final String AC_JSON = "src/test/resources/rest/acm/AutomationCompositionSmoke.json";
    private static final String ELEMENT_NAME =
            "org.onap.domain.database.Http_PMSHMicroserviceAutomationCompositionElement";

    private static final UUID COMPOSITION_ID = UUID.randomUUID();
    private static final UUID INSTANCE_ID = UUID.randomUUID();
    private static final Map<String, Object> OUT_PROPERTIES = Map.of("key", "value");

    @Test
    void testFailScenario() {
        var automationComposition = createDeploying();
        var elementId = automationComposition.getElements().values().iterator().next().getId();
        var docMessage = new DocMessage();
        docMessage.setMessageType(ParticipantMessageType.AUTOMATION_COMPOSITION_DEPLOY_ACK);
        docMessage.setInstanceId(INSTANCE_ID);
        docMessage.setInstanceElementId(elementId);
        docMessage.setDeployState(DeployState.UNDEPLOYED);
        docMessage.setLockState(LockState.NONE);
        docMessage.setStateChangeResult(StateChangeResult.FAILED);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");
        var acProvider = mock(AutomationCompositionProvider.class);
        var simpleScanner = new SimpleScanner(acProvider, mock(ParticipantSyncPublisher.class),
                acRuntimeParameterGroup);
        var result = simpleScanner.scanMessage(automationComposition, docMessage);
        assertTrue(result.isUpdated());
        assertTrue(result.isToBeSync());
        assertEquals(docMessage.getDeployState(),
                automationComposition.getElements().get(elementId).getDeployState());
        assertEquals(docMessage.getLockState(),
                automationComposition.getElements().get(elementId).getLockState());
        assertEquals(docMessage.getStateChangeResult(), automationComposition.getStateChangeResult());
    }

    @Test
    void testWithWrongData() {
        var automationComposition = createDeploying();
        var elementId = automationComposition.getElements().values().iterator().next().getId();
        var docMessage = new DocMessage();
        docMessage.setInstanceId(INSTANCE_ID);
        docMessage.setInstanceElementId(elementId);
        docMessage.setStateChangeResult(StateChangeResult.NO_ERROR);
        docMessage.setDeployState(DeployState.DEPLOYED);
        docMessage.setLockState(LockState.LOCKED);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");
        var acProvider = mock(AutomationCompositionProvider.class);
        var simpleScanner = new SimpleScanner(acProvider, mock(ParticipantSyncPublisher.class),
                acRuntimeParameterGroup);

        // wrong MessageType
        docMessage.setMessageType(ParticipantMessageType.PARTICIPANT_PRIME_ACK);
        var result = simpleScanner.scanMessage(automationComposition, docMessage);
        assertFalse(result.isUpdated());
        assertFalse(result.isToBeSync());

        // wrong elementId in outProperties
        docMessage.setMessageType(ParticipantMessageType.PARTICIPANT_STATUS);
        docMessage.setInstanceElementId(UUID.randomUUID());
        docMessage.setOutProperties(OUT_PROPERTIES);
        result = simpleScanner.scanMessage(automationComposition, docMessage);
        assertFalse(result.isUpdated());
        assertFalse(result.isToBeSync());

        // wrong elementId in StateChange
        docMessage.setMessageType(ParticipantMessageType.AUTOMATION_COMPOSITION_STATECHANGE_ACK);
        result = simpleScanner.scanMessage(automationComposition, docMessage);
        assertFalse(result.isUpdated());
        assertFalse(result.isToBeSync());
    }

    @Test
    void testScanMessageOutProperties() {
        var automationComposition = createDeploying();
        var elementId = automationComposition.getElements().values().iterator().next().getId();
        var docMessage = new DocMessage();
        docMessage.setMessageType(ParticipantMessageType.PARTICIPANT_STATUS);
        docMessage.setInstanceId(INSTANCE_ID);
        docMessage.setInstanceElementId(elementId);
        docMessage.setOutProperties(Map.of("key", "value"));
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");
        var acProvider = mock(AutomationCompositionProvider.class);
        var simpleScanner = new SimpleScanner(acProvider, mock(ParticipantSyncPublisher.class),
                acRuntimeParameterGroup);
        var result = simpleScanner.scanMessage(automationComposition, docMessage);
        assertTrue(result.isUpdated());
        assertTrue(result.isToBeSync());
        assertEquals(docMessage.getOutProperties(),
                automationComposition.getElements().get(elementId).getOutProperties());
    }

    @Test
    void testScanMessageStateChange() {
        var automationComposition = createDeploying();
        var elementId = automationComposition.getElements().values().iterator().next().getId();
        var docMessage = new DocMessage();
        docMessage.setMessageType(ParticipantMessageType.AUTOMATION_COMPOSITION_DEPLOY_ACK);
        docMessage.setStateChangeResult(StateChangeResult.NO_ERROR);
        docMessage.setInstanceId(INSTANCE_ID);
        docMessage.setInstanceElementId(elementId);
        docMessage.setDeployState(DeployState.DEPLOYED);
        docMessage.setLockState(LockState.LOCKED);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");
        var acProvider = mock(AutomationCompositionProvider.class);
        var simpleScanner = new SimpleScanner(acProvider, mock(ParticipantSyncPublisher.class),
                acRuntimeParameterGroup);
        var result = simpleScanner.scanMessage(automationComposition, docMessage);
        assertTrue(result.isUpdated());
        assertFalse(result.isToBeSync());
        assertEquals(docMessage.getDeployState(),
                automationComposition.getElements().get(elementId).getDeployState());
        assertEquals(docMessage.getLockState(),
                automationComposition.getElements().get(elementId).getLockState());
    }

    private AutomationComposition createDeploying() {
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        automationComposition.setInstanceId(INSTANCE_ID);
        automationComposition.setDeployState(DeployState.DEPLOYING);
        automationComposition.setLockState(LockState.NONE);
        automationComposition.setPhase(0);
        automationComposition.setLastMsg(TimestampHelper.now());
        automationComposition.setCompositionId(COMPOSITION_ID);
        for (var element : automationComposition.getElements().values()) {
            element.setDeployState(DeployState.DEPLOYING);
            element.setLockState(LockState.NONE);
        }
        return automationComposition;
    }

    @Test
    void testSendAutomationCompositionMigratingPrecheck() {
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        automationComposition.setLockState(LockState.LOCKED);
        automationComposition.setDeployState(DeployState.DEPLOYED);
        automationComposition.setSubState(SubState.MIGRATION_PRECHECKING);
        for (var element : automationComposition.getElements().values()) {
            element.setDeployState(DeployState.DEPLOYED);
            element.setSubState(SubState.NONE);
            element.setLockState(LockState.LOCKED);
            if (ELEMENT_NAME.equals(element.getDefinition().getName())) {
                element.setSubState(SubState.MIGRATION_PRECHECKING);
            }
        }
        testSimpleScan(automationComposition, element -> element.setSubState(SubState.NONE));
    }

    @Test
    void testSendAutomationCompositionPrepare() {
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        automationComposition.setLockState(LockState.NONE);
        automationComposition.setDeployState(DeployState.UNDEPLOYED);
        automationComposition.setSubState(SubState.PREPARING);
        for (var element : automationComposition.getElements().values()) {
            element.setDeployState(DeployState.UNDEPLOYED);
            element.setSubState(SubState.NONE);
            element.setLockState(LockState.NONE);
            if (ELEMENT_NAME.equals(element.getDefinition().getName())) {
                element.setSubState(SubState.PREPARING);
            }
        }
        testSimpleScan(automationComposition, element -> element.setSubState(SubState.NONE));
    }

    @Test
    void testSendAutomationCompositionUpdate() {
        var automationComposition = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud");
        automationComposition.setLockState(LockState.LOCKED);
        automationComposition.setDeployState(DeployState.UPDATING);
        for (var element : automationComposition.getElements().values()) {
            element.setSubState(SubState.NONE);
            element.setLockState(LockState.LOCKED);
            if (ELEMENT_NAME.equals(element.getDefinition().getName())) {
                element.setDeployState(DeployState.UPDATING);
            } else {
                element.setDeployState(DeployState.DEPLOYED);
            }
        }
        testSimpleScan(automationComposition, element -> element.setDeployState(DeployState.DEPLOYED));
    }

    private void testSimpleScan(AutomationComposition automationComposition, Consumer<AutomationCompositionElement> c) {
        automationComposition.setInstanceId(INSTANCE_ID);
        automationComposition.setLockState(LockState.NONE);
        automationComposition.setCompositionId(COMPOSITION_ID);
        automationComposition.setLastMsg(TimestampHelper.now());
        var acProvider = mock(AutomationCompositionProvider.class);
        var acRuntimeParameterGroup = CommonTestData.geParameterGroup("dbScanner");
        var simpleScanner = new SimpleScanner(acProvider, mock(ParticipantSyncPublisher.class),
                acRuntimeParameterGroup);
        simpleScanner.simpleScan(automationComposition, new UpdateSync());
        verify(acProvider, times(0)).updateAutomationComposition(any());

        automationComposition.getElements().values().forEach(c);
        simpleScanner.simpleScan(automationComposition, new UpdateSync());
        verify(acProvider).updateAutomationComposition(any());
    }
}
