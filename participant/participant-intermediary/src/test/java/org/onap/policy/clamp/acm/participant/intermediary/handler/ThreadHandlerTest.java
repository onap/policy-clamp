/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.intermediary.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import jakarta.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.api.AutomationCompositionElementListener;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.InstanceElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.AcElementRestart;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantRestartAc;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class ThreadHandlerTest {

    private static final int TIMEOUT = 400;

    @Test
    void test() throws PfModelException, IOException {
        var listener = mock(AutomationCompositionElementListener.class);
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        try (var threadHandler = new ThreadHandler(listener, intermediaryApi, mock(CacheProvider.class))) {

            var compositionId = UUID.randomUUID();
            var messageId = UUID.randomUUID();
            var composition = new CompositionDto(compositionId, Map.of(), Map.of());
            threadHandler.prime(messageId, composition);
            verify(listener, timeout(TIMEOUT)).prime(composition);

            clearInvocations(listener);
            Map<String, Object> properties = Map.of("key", "value");
            var compositionElement = new CompositionElementDto(UUID.randomUUID(), new ToscaConceptIdentifier(),
                properties, properties);
            var instanceElement = new InstanceElementDto(UUID.randomUUID(), UUID.randomUUID(),
                null, properties, properties);
            threadHandler.deploy(messageId, compositionElement, instanceElement);
            verify(listener, timeout(TIMEOUT)).deploy(compositionElement, instanceElement);

            clearInvocations(listener);
            var element = new AcElementDeploy();
            var elementId = UUID.randomUUID();
            element.setId(elementId);
            var instanceElementUpdated = new InstanceElementDto(instanceElement.instanceId(),
                instanceElement.elementId(), null, properties, properties);
            threadHandler.update(messageId, compositionElement, instanceElement, instanceElementUpdated);
            verify(listener, timeout(TIMEOUT)).update(compositionElement, instanceElement, instanceElementUpdated);

            clearInvocations(listener);
            var compositionTargetId = UUID.randomUUID();
            var compositionElementTarget = new CompositionElementDto(compositionTargetId, new ToscaConceptIdentifier(),
                properties, properties);
            threadHandler.migrate(messageId, compositionElement, compositionElementTarget,
                instanceElement, instanceElementUpdated);
            verify(listener, timeout(TIMEOUT)).migrate(compositionElement, compositionElementTarget,
                instanceElement, instanceElementUpdated);

            clearInvocations(listener);
            threadHandler.lock(messageId, compositionElement, instanceElement);
            verify(listener, timeout(TIMEOUT)).lock(compositionElement, instanceElement);

            clearInvocations(listener);
            threadHandler.unlock(messageId, compositionElement, instanceElement);
            verify(listener, timeout(TIMEOUT)).unlock(compositionElement, instanceElement);

            clearInvocations(listener);
            threadHandler.undeploy(messageId, compositionElement, instanceElement);
            verify(listener, timeout(TIMEOUT)).undeploy(compositionElement, instanceElement);

            clearInvocations(listener);
            threadHandler.delete(messageId, compositionElement, instanceElement);
            verify(listener, timeout(TIMEOUT)).delete(compositionElement, instanceElement);

            clearInvocations(listener);
            threadHandler.deprime(messageId, composition);
            verify(listener, timeout(TIMEOUT)).deprime(composition);
        }
    }

    @Test
    void testException() throws PfModelException, IOException {
        var listener = mock(AutomationCompositionElementListener.class);
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        try (var threadHandler = new ThreadHandler(listener, intermediaryApi, mock(CacheProvider.class))) {

            var compositionId = UUID.randomUUID();
            var composition = new CompositionDto(compositionId, Map.of(), Map.of());
            doThrow(new PfModelException(Status.INTERNAL_SERVER_ERROR, "Error")).when(listener)
                .prime(composition);
            var messageId = UUID.randomUUID();
            threadHandler.prime(messageId, composition);
            verify(intermediaryApi, timeout(TIMEOUT)).updateCompositionState(compositionId, AcTypeState.COMMISSIONED,
                    StateChangeResult.FAILED, "Composition Defintion prime failed");

            clearInvocations(intermediaryApi);
            Map<String, Object> properties = Map.of("key", "value");
            var compositionElement = new CompositionElementDto(UUID.randomUUID(), new ToscaConceptIdentifier(),
                properties, properties);
            var instanceId = UUID.randomUUID();
            var elementId = UUID.randomUUID();
            var instanceElement = new InstanceElementDto(instanceId, elementId, null, properties, properties);
            var element = new AcElementDeploy();
            element.setId(elementId);
            doThrow(new PfModelException(Status.INTERNAL_SERVER_ERROR, "Error")).when(listener)
                .deploy(compositionElement, instanceElement);
            threadHandler.deploy(messageId, compositionElement, instanceElement);
            verify(intermediaryApi, timeout(TIMEOUT)).updateAutomationCompositionElementState(instanceId, elementId,
                    DeployState.UNDEPLOYED, null, StateChangeResult.FAILED,
                    "Automation composition element deploy failed");

            clearInvocations(listener);
            var instanceElementUpdated = new InstanceElementDto(instanceElement.instanceId(),
                instanceElement.elementId(), null, properties, properties);
            doThrow(new PfModelException(Status.INTERNAL_SERVER_ERROR, "Error")).when(listener)
                .update(compositionElement, instanceElement, instanceElementUpdated);
            threadHandler.update(messageId, compositionElement, instanceElement, instanceElementUpdated);
            verify(intermediaryApi, timeout(TIMEOUT)).updateAutomationCompositionElementState(instanceId, elementId,
                    DeployState.DEPLOYED, null, StateChangeResult.FAILED,
                    "Automation composition element update failed");

            clearInvocations(listener);
            doThrow(new PfModelException(Status.INTERNAL_SERVER_ERROR, "Error")).when(listener)
                .lock(compositionElement, instanceElement);
            threadHandler.lock(messageId, compositionElement, instanceElement);
            verify(intermediaryApi, timeout(TIMEOUT)).updateAutomationCompositionElementState(instanceId, elementId,
                    null, LockState.UNLOCKED, StateChangeResult.FAILED, "Automation composition element lock failed");

            clearInvocations(listener);
            doThrow(new PfModelException(Status.INTERNAL_SERVER_ERROR, "Error")).when(listener)
                .unlock(compositionElement, instanceElement);
            threadHandler.unlock(messageId, compositionElement, instanceElement);
            verify(intermediaryApi, timeout(TIMEOUT)).updateAutomationCompositionElementState(instanceId, elementId,
                    null, LockState.LOCKED, StateChangeResult.FAILED, "Automation composition element unlock failed");

            clearInvocations(listener);
            doThrow(new PfModelException(Status.INTERNAL_SERVER_ERROR, "Error")).when(listener)
                .undeploy(compositionElement, instanceElement);
            threadHandler.undeploy(messageId, compositionElement, instanceElement);
            verify(intermediaryApi, timeout(TIMEOUT)).updateAutomationCompositionElementState(instanceId, elementId,
                    DeployState.DEPLOYED, null, StateChangeResult.FAILED,
                    "Automation composition element undeploy failed");

            clearInvocations(listener);
            doThrow(new PfModelException(Status.INTERNAL_SERVER_ERROR, "Error")).when(listener)
                .delete(compositionElement, instanceElement);
            threadHandler.delete(messageId, compositionElement, instanceElement);
            verify(intermediaryApi, timeout(TIMEOUT)).updateAutomationCompositionElementState(instanceId, elementId,
                    DeployState.UNDEPLOYED, null, StateChangeResult.FAILED,
                    "Automation composition element delete failed");

            clearInvocations(listener);
            doThrow(new PfModelException(Status.INTERNAL_SERVER_ERROR, "Error")).when(listener).deprime(composition);
            threadHandler.deprime(messageId, composition);
            verify(intermediaryApi, timeout(TIMEOUT)).updateCompositionState(compositionId, AcTypeState.PRIMED,
                    StateChangeResult.FAILED, "Composition Defintion deprime failed");

            clearInvocations(listener);
            doThrow(new PfModelException(Status.INTERNAL_SERVER_ERROR, "Error")).when(listener)
                    .handleRestartComposition(composition, AcTypeState.PRIMING);
            threadHandler.restarted(messageId, composition, AcTypeState.PRIMING, List.of());
            verify(intermediaryApi).updateCompositionState(compositionId, AcTypeState.PRIMED, StateChangeResult.FAILED,
                    "Composition Defintion deprime failed");
        }
    }

    @Test
    void testRestarted() throws IOException, PfModelException {
        var listener = mock(AutomationCompositionElementListener.class);
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var cacheProvider = mock(CacheProvider.class);
        try (var threadHandler = new ThreadHandler(listener, intermediaryApi, cacheProvider)) {
            var messageId = UUID.randomUUID();
            var compositionId = UUID.randomUUID();
            var participantRestartAc = new ParticipantRestartAc();
            participantRestartAc.setAutomationCompositionId(UUID.randomUUID());
            participantRestartAc.getAcElementList().add(new AcElementRestart());
            var composition = new CompositionDto(compositionId, Map.of(), Map.of());
            threadHandler.restarted(messageId, composition, AcTypeState.PRIMED, List.of(participantRestartAc));
            verify(listener, timeout(TIMEOUT)).handleRestartInstance(any(), any(), any(), any());
        }
    }
}
