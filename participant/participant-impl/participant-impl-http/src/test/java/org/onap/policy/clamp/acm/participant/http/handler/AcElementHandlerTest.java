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

package org.onap.policy.clamp.acm.participant.http.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.http.main.handler.AutomationCompositionElementHandler;
import org.onap.policy.clamp.acm.participant.http.main.models.ConfigRequest;
import org.onap.policy.clamp.acm.participant.http.main.webclient.AcHttpClient;
import org.onap.policy.clamp.acm.participant.http.utils.CommonTestData;
import org.onap.policy.clamp.acm.participant.http.utils.ToscaUtils;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.springframework.http.HttpStatus;

class AcElementHandlerTest {

    private final CommonTestData commonTestData = new CommonTestData();
    private static final String HTTP_AUTOMATION_COMPOSITION_ELEMENT =
            "org.onap.domain.database.Http_PMSHMicroserviceAutomationCompositionElement";

    @Test
    void testUndeploy() {
        var instanceId = commonTestData.getAutomationCompositionId();
        var element = commonTestData.getAutomationCompositionElement();
        var acElementId = element.getId();
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);

        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, mock(AcHttpClient.class));
        automationCompositionElementHandler.undeploy(instanceId, acElementId);
        verify(participantIntermediaryApi).updateAutomationCompositionElementState(instanceId, acElementId,
                DeployState.UNDEPLOYED, null, StateChangeResult.NO_ERROR, "");

    }

    @Test
    void testDeployConstraintViolations() {
        var instanceId = commonTestData.getAutomationCompositionId();
        var element = commonTestData.getAutomationCompositionElement();
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);

        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, mock(AcHttpClient.class));

        Map<String, Object> map = new HashMap<>();
        automationCompositionElementHandler.deploy(instanceId, element, map);
        verify(participantIntermediaryApi).updateAutomationCompositionElementState(instanceId, element.getId(),
                DeployState.UNDEPLOYED, null, StateChangeResult.FAILED, "Constraint violations in the config request");
    }

    @Test
    void testDeployError() {
        var instanceId = commonTestData.getAutomationCompositionId();
        var element = commonTestData.getAutomationCompositionElement();
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);

        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, mock(AcHttpClient.class));

        Map<String, Object> map = new HashMap<>();
        map.put("httpHeaders", 1);
        automationCompositionElementHandler.deploy(instanceId, element, map);
        verify(participantIntermediaryApi).updateAutomationCompositionElementState(instanceId, element.getId(),
                DeployState.UNDEPLOYED, null, StateChangeResult.FAILED, "Error extracting ConfigRequest ");

    }

    @Test
    void testDeployFailed() {
        var serviceTemplate = ToscaUtils.readAutomationCompositionFromTosca();
        var nodeTemplatesMap = serviceTemplate.getToscaTopologyTemplate().getNodeTemplates();
        var map = new HashMap<>(nodeTemplatesMap.get(HTTP_AUTOMATION_COMPOSITION_ELEMENT).getProperties());
        var element = commonTestData.getAutomationCompositionElement();
        map.putAll(element.getProperties());
        var instanceId = commonTestData.getAutomationCompositionId();
        var acHttpClient = mock(AcHttpClient.class);
        when(acHttpClient.run(any())).thenReturn(Map.of(new ToscaConceptIdentifier(),
                Pair.of(HttpStatus.BAD_REQUEST.value(), "")));
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);

        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, acHttpClient);

        automationCompositionElementHandler.deploy(instanceId, element, map);
        verify(acHttpClient).run(any(ConfigRequest.class));
        verify(participantIntermediaryApi).updateAutomationCompositionElementState(instanceId, element.getId(),
                DeployState.UNDEPLOYED, null, StateChangeResult.FAILED,
                "Error on Invoking the http request: [(400,)]");
    }

    @Test
    void testDeploy() {
        var serviceTemplate = ToscaUtils.readAutomationCompositionFromTosca();
        var nodeTemplatesMap = serviceTemplate.getToscaTopologyTemplate().getNodeTemplates();
        var map = new HashMap<>(nodeTemplatesMap.get(HTTP_AUTOMATION_COMPOSITION_ELEMENT).getProperties());
        var element = commonTestData.getAutomationCompositionElement();
        map.putAll(element.getProperties());
        var instanceId = commonTestData.getAutomationCompositionId();
        var acHttpClient = mock(AcHttpClient.class);
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);

        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, acHttpClient);

        automationCompositionElementHandler.deploy(instanceId, element, map);
        verify(acHttpClient).run(any(ConfigRequest.class));
        verify(participantIntermediaryApi).updateAutomationCompositionElementState(instanceId, element.getId(),
                DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Deployed");
    }

    @Test
    void testUpdate() throws PfModelException {
        var instanceId = commonTestData.getAutomationCompositionId();
        var element = commonTestData.getAutomationCompositionElement();
        var acElementId = element.getId();
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);

        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, mock(AcHttpClient.class));

        automationCompositionElementHandler.update(instanceId, element, Map.of());
        verify(participantIntermediaryApi).updateAutomationCompositionElementState(instanceId, acElementId,
                DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Update not supported");
    }

    @Test
    void testLock() throws PfModelException {
        var instanceId = commonTestData.getAutomationCompositionId();
        var acElementId = UUID.randomUUID();
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);

        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, mock(AcHttpClient.class));

        automationCompositionElementHandler.lock(instanceId, acElementId);
        verify(participantIntermediaryApi).updateAutomationCompositionElementState(instanceId, acElementId, null,
                LockState.LOCKED, StateChangeResult.NO_ERROR, "Locked");
    }

    @Test
    void testUnlock() throws PfModelException {
        var instanceId = commonTestData.getAutomationCompositionId();
        var acElementId = UUID.randomUUID();
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);

        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, mock(AcHttpClient.class));

        automationCompositionElementHandler.unlock(instanceId, acElementId);
        verify(participantIntermediaryApi).updateAutomationCompositionElementState(instanceId, acElementId, null,
                LockState.UNLOCKED, StateChangeResult.NO_ERROR, "Unlocked");
    }

    @Test
    void testDelete() throws PfModelException {
        var instanceId = commonTestData.getAutomationCompositionId();
        var acElementId = UUID.randomUUID();
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);

        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, mock(AcHttpClient.class));

        automationCompositionElementHandler.delete(instanceId, acElementId);
        verify(participantIntermediaryApi).updateAutomationCompositionElementState(instanceId, acElementId,
                DeployState.DELETED, null, StateChangeResult.NO_ERROR, "Deleted");
    }

    @Test
    void testPrime() throws PfModelException {
        var compositionId = UUID.randomUUID();
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);

        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, mock(AcHttpClient.class));

        automationCompositionElementHandler.prime(compositionId, List.of());
        verify(participantIntermediaryApi).updateCompositionState(compositionId, AcTypeState.PRIMED,
                StateChangeResult.NO_ERROR, "Primed");
    }

    @Test
    void testDeprime() throws PfModelException {
        var compositionId = UUID.randomUUID();
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);

        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, mock(AcHttpClient.class));

        automationCompositionElementHandler.deprime(compositionId);
        verify(participantIntermediaryApi).updateCompositionState(compositionId, AcTypeState.COMMISSIONED,
                StateChangeResult.NO_ERROR, "Deprimed");
    }

    @Test
    void testMigrate() throws PfModelException {
        var instanceId = commonTestData.getAutomationCompositionId();
        var element = commonTestData.getAutomationCompositionElement();
        var acElementId = element.getId();
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);

        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, mock(AcHttpClient.class));

        automationCompositionElementHandler.migrate(instanceId, element, UUID.randomUUID(), Map.of());
        verify(participantIntermediaryApi).updateAutomationCompositionElementState(instanceId, acElementId,
                DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Migrated");
    }
}
