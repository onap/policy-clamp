/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
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

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.core.Response.Status;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.api.AutomationCompositionElementListener;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.models.base.PfModelException;

class ThreadHandlerTest {

    private static final int TIMEOUT = 400;

    @Test
    void test() throws PfModelException {
        var listener = mock(AutomationCompositionElementListener.class);
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var threadHandler = new ThreadHandler(listener, intermediaryApi, mock(CacheProvider.class));

        var compositionId = UUID.randomUUID();
        var list = List.of(new AutomationCompositionElementDefinition());
        var messageId = UUID.randomUUID();
        threadHandler.prime(messageId, compositionId, list);
        verify(listener, timeout(TIMEOUT)).prime(compositionId, list);

        clearInvocations(listener);
        var element = new AcElementDeploy();
        var elementId = UUID.randomUUID();
        element.setId(elementId);
        Map<String, Object> properties = Map.of("key", "value");
        var instanceId = UUID.randomUUID();
        threadHandler.deploy(messageId, instanceId, element, properties);
        verify(listener, timeout(TIMEOUT)).deploy(instanceId, element, properties);

        clearInvocations(listener);
        threadHandler.update(messageId, instanceId, element, properties);
        verify(listener, timeout(TIMEOUT)).update(instanceId, element, properties);

        clearInvocations(listener);
        threadHandler.lock(messageId, instanceId, elementId);
        verify(listener, timeout(TIMEOUT)).lock(instanceId, elementId);

        clearInvocations(listener);
        threadHandler.unlock(messageId, instanceId, elementId);
        verify(listener, timeout(TIMEOUT)).unlock(instanceId, elementId);

        clearInvocations(listener);
        threadHandler.undeploy(messageId, instanceId, elementId);
        verify(listener, timeout(TIMEOUT)).undeploy(instanceId, elementId);

        clearInvocations(listener);
        threadHandler.delete(messageId, instanceId, elementId);
        verify(listener, timeout(TIMEOUT)).delete(instanceId, elementId);

        clearInvocations(listener);
        threadHandler.deprime(messageId, compositionId);
        verify(listener, timeout(TIMEOUT)).deprime(compositionId);
    }

    @Test
    void testException() throws PfModelException {
        var listener = mock(AutomationCompositionElementListener.class);
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var threadHandler = new ThreadHandler(listener, intermediaryApi, mock(CacheProvider.class));

        var compositionId = UUID.randomUUID();
        var list = List.of(new AutomationCompositionElementDefinition());
        doThrow(new PfModelException(Status.INTERNAL_SERVER_ERROR, "Error")).when(listener).prime(compositionId, list);
        var messageId = UUID.randomUUID();
        threadHandler.prime(messageId, compositionId, list);
        verify(intermediaryApi, timeout(TIMEOUT)).updateCompositionState(compositionId, AcTypeState.COMMISSIONED,
                StateChangeResult.FAILED, "Composition Defintion prime failed");

        clearInvocations(intermediaryApi);
        var element = new AcElementDeploy();
        var elementId = UUID.randomUUID();
        element.setId(elementId);
        Map<String, Object> properties = Map.of("key", "value");
        var instanceId = UUID.randomUUID();
        doThrow(new PfModelException(Status.INTERNAL_SERVER_ERROR, "Error")).when(listener).deploy(instanceId, element,
                properties);
        threadHandler.deploy(messageId, instanceId, element, properties);
        verify(intermediaryApi, timeout(TIMEOUT)).updateAutomationCompositionElementState(instanceId, elementId,
                DeployState.UNDEPLOYED, null, StateChangeResult.FAILED, "Automation composition element deploy failed");

        clearInvocations(listener);
        doThrow(new PfModelException(Status.INTERNAL_SERVER_ERROR, "Error")).when(listener).update(instanceId, element,
                properties);
        threadHandler.update(messageId, instanceId, element, properties);
        verify(intermediaryApi, timeout(TIMEOUT)).updateAutomationCompositionElementState(instanceId, elementId,
                DeployState.DEPLOYED, null, StateChangeResult.FAILED, "Automation composition element update failed");

        clearInvocations(listener);
        doThrow(new PfModelException(Status.INTERNAL_SERVER_ERROR, "Error")).when(listener).lock(instanceId, elementId);
        threadHandler.lock(messageId, instanceId, elementId);
        verify(intermediaryApi, timeout(TIMEOUT)).updateAutomationCompositionElementState(instanceId, elementId, null,
                LockState.UNLOCKED, StateChangeResult.FAILED, "Automation composition element lock failed");

        clearInvocations(listener);
        doThrow(new PfModelException(Status.INTERNAL_SERVER_ERROR, "Error")).when(listener).unlock(instanceId,
                elementId);
        threadHandler.unlock(messageId, instanceId, elementId);
        verify(intermediaryApi, timeout(TIMEOUT)).updateAutomationCompositionElementState(instanceId, elementId, null,
                LockState.LOCKED, StateChangeResult.FAILED, "Automation composition element unlock failed");

        clearInvocations(listener);
        doThrow(new PfModelException(Status.INTERNAL_SERVER_ERROR, "Error")).when(listener).undeploy(instanceId,
                elementId);
        threadHandler.undeploy(messageId, instanceId, elementId);
        verify(intermediaryApi, timeout(TIMEOUT)).updateAutomationCompositionElementState(instanceId, elementId,
                DeployState.DEPLOYED, null, StateChangeResult.FAILED, "Automation composition element undeploy failed");

        clearInvocations(listener);
        doThrow(new PfModelException(Status.INTERNAL_SERVER_ERROR, "Error")).when(listener).delete(instanceId,
                elementId);
        threadHandler.delete(messageId, instanceId, elementId);
        verify(intermediaryApi, timeout(TIMEOUT)).updateAutomationCompositionElementState(instanceId, elementId,
                DeployState.UNDEPLOYED, null, StateChangeResult.FAILED, "Automation composition element delete failed");

        clearInvocations(listener);
        doThrow(new PfModelException(Status.INTERNAL_SERVER_ERROR, "Error")).when(listener).deprime(compositionId);
        threadHandler.deprime(messageId, compositionId);
        verify(intermediaryApi, timeout(TIMEOUT)).updateCompositionState(compositionId,
                AcTypeState.PRIMED, StateChangeResult.FAILED, "Composition Defintion deprime failed");
    }
}
