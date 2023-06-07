/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.a1pms.handler;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.a1pms.exception.A1PolicyServiceException;
import org.onap.policy.clamp.acm.participant.a1pms.utils.CommonTestData;
import org.onap.policy.clamp.acm.participant.a1pms.utils.ToscaUtils;
import org.onap.policy.clamp.acm.participant.a1pms.webclient.AcA1PmsClient;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

class AcElementHandlerTest {

    private final AcA1PmsClient acA1PmsClient = mock(AcA1PmsClient.class);

    private final CommonTestData commonTestData = new CommonTestData();

    private static ToscaServiceTemplate serviceTemplate;
    private static final String A1_AUTOMATION_COMPOSITION_ELEMENT =
            "org.onap.domain.database.A1PMSAutomationCompositionElement";

    @BeforeAll
    static void init() {
        serviceTemplate = ToscaUtils.readAutomationCompositionFromTosca();
    }

    @BeforeEach
    void startMocks() throws A1PolicyServiceException {
        when(acA1PmsClient.isPmsHealthy()).thenReturn(Boolean.TRUE);
        doNothing().when(acA1PmsClient).createService(any());
    }

    @Test
    void test_automationCompositionElementStateChange() throws A1PolicyServiceException {
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, acA1PmsClient);

        var automationCompositionId = commonTestData.getAutomationCompositionId();
        var element = commonTestData.getAutomationCompositionElement();
        var automationCompositionElementId = element.getId();

        var nodeTemplatesMap = serviceTemplate.getToscaTopologyTemplate().getNodeTemplates();
        automationCompositionElementHandler.deploy(commonTestData.getAutomationCompositionId(), element,
                nodeTemplatesMap.get(A1_AUTOMATION_COMPOSITION_ELEMENT).getProperties());
        verify(participantIntermediaryApi).updateAutomationCompositionElementState(automationCompositionId,
                automationCompositionElementId, DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Deployed");

        automationCompositionElementHandler.undeploy(automationCompositionId, automationCompositionElementId);
        verify(participantIntermediaryApi).updateAutomationCompositionElementState(automationCompositionId,
                automationCompositionElementId, DeployState.UNDEPLOYED, null, StateChangeResult.NO_ERROR, "Undeployed");

        when(acA1PmsClient.isPmsHealthy()).thenReturn(Boolean.FALSE);
        assertThrows(A1PolicyServiceException.class, () -> automationCompositionElementHandler
                .undeploy(automationCompositionId, automationCompositionElementId));
    }

    @Test
    void test_AutomationCompositionElementUpdate() throws A1PolicyServiceException {
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, acA1PmsClient);

        var element = commonTestData.getAutomationCompositionElement();
        var nodeTemplatesMap = serviceTemplate.getToscaTopologyTemplate().getNodeTemplates();
        automationCompositionElementHandler.deploy(commonTestData.getAutomationCompositionId(), element,
                nodeTemplatesMap.get(A1_AUTOMATION_COMPOSITION_ELEMENT).getProperties());
        verify(participantIntermediaryApi).updateAutomationCompositionElementState(
                commonTestData.getAutomationCompositionId(), element.getId(), DeployState.DEPLOYED, null,
                StateChangeResult.NO_ERROR, "Deployed");
    }

    @Test
    void test_AutomationCompositionElementUpdateWithUnhealthyA1pms() {
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, acA1PmsClient);

        var element = commonTestData.getAutomationCompositionElement();
        when(acA1PmsClient.isPmsHealthy()).thenReturn(Boolean.FALSE);

        var nodeTemplatesMap = serviceTemplate.getToscaTopologyTemplate().getNodeTemplates();
        assertThrows(A1PolicyServiceException.class,
                () -> automationCompositionElementHandler.deploy(commonTestData.getAutomationCompositionId(), element,
                        nodeTemplatesMap.get(A1_AUTOMATION_COMPOSITION_ELEMENT).getProperties()));
    }

    @Test
    void test_AutomationCompositionElementUpdateWithInvalidConfiguration() {
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, acA1PmsClient);

        var element = commonTestData.getAutomationCompositionElement();
        assertThrows(A1PolicyServiceException.class, () -> automationCompositionElementHandler
                .deploy(commonTestData.getAutomationCompositionId(), element, Map.of()));
    }

    @Test
    void testLock() throws PfModelException {
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, acA1PmsClient);

        var automationCompositionId = UUID.randomUUID();
        var elementId = UUID.randomUUID();
        automationCompositionElementHandler.lock(automationCompositionId, elementId);

        verify(participantIntermediaryApi).updateAutomationCompositionElementState(automationCompositionId, elementId,
                null, LockState.LOCKED, StateChangeResult.NO_ERROR, "Locked");
    }

    @Test
    void testUnlock() throws PfModelException {
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, acA1PmsClient);

        var automationCompositionId = UUID.randomUUID();
        var elementId = UUID.randomUUID();
        automationCompositionElementHandler.unlock(automationCompositionId, elementId);

        verify(participantIntermediaryApi).updateAutomationCompositionElementState(automationCompositionId, elementId,
                null, LockState.UNLOCKED, StateChangeResult.NO_ERROR, "Unlocked");
    }

    @Test
    void testUpdate() throws PfModelException {
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, acA1PmsClient);

        var automationCompositionId = UUID.randomUUID();
        var element = commonTestData.getAutomationCompositionElement();
        automationCompositionElementHandler.update(automationCompositionId, element, Map.of());

        verify(participantIntermediaryApi).updateAutomationCompositionElementState(automationCompositionId,
                element.getId(), DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Update not supported");
    }

    @Test
    void testDelete() throws PfModelException {
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, acA1PmsClient);

        var automationCompositionId = UUID.randomUUID();
        var elementId = UUID.randomUUID();
        automationCompositionElementHandler.delete(automationCompositionId, elementId);

        verify(participantIntermediaryApi).updateAutomationCompositionElementState(automationCompositionId, elementId,
                DeployState.DELETED, null, StateChangeResult.NO_ERROR, "Deleted");
    }

    @Test
    void testPrime() throws PfModelException {
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, acA1PmsClient);

        var compositionId = UUID.randomUUID();
        automationCompositionElementHandler.prime(compositionId, List.of());

        verify(participantIntermediaryApi).updateCompositionState(compositionId, AcTypeState.PRIMED,
                StateChangeResult.NO_ERROR, "Primed");
    }

    @Test
    void testDeprime() throws PfModelException {
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, acA1PmsClient);

        var compositionId = UUID.randomUUID();
        automationCompositionElementHandler.deprime(compositionId);

        verify(participantIntermediaryApi).updateCompositionState(compositionId, AcTypeState.COMMISSIONED,
                StateChangeResult.NO_ERROR, "Deprimed");
    }
}
