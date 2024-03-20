/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.intermediary.api.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.InstanceElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class AcElementListenerV2Test {

    @Test
    void lockTest() throws PfModelException {
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var acElementListenerV2 = createAcElementListenerV2(intermediaryApi);
        var compositionElement = new CompositionElementDto(UUID.randomUUID(), new ToscaConceptIdentifier(),
            Map.of(), Map.of());
        var instanceElement = new InstanceElementDto(UUID.randomUUID(), UUID.randomUUID(), null, Map.of(), Map.of());
        acElementListenerV2.lock(compositionElement, instanceElement);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceElement.instanceId(),
            instanceElement.elementId(), null, LockState.LOCKED, StateChangeResult.NO_ERROR, "Locked");
    }

    @Test
    void deleteTest() throws PfModelException {
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var acElementListenerV2 = createAcElementListenerV2(intermediaryApi);
        var compositionElement = new CompositionElementDto(UUID.randomUUID(), new ToscaConceptIdentifier(),
            Map.of(), Map.of());
        var instanceElement = new InstanceElementDto(UUID.randomUUID(), UUID.randomUUID(), null, Map.of(), Map.of());
        acElementListenerV2.delete(compositionElement, instanceElement);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceElement.instanceId(),
            instanceElement.elementId(), DeployState.DELETED, null, StateChangeResult.NO_ERROR, "Deleted");
    }

    @Test
    void updateTest() throws PfModelException {
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var acElementListenerV2 = createAcElementListenerV2(intermediaryApi);
        var compositionElement = new CompositionElementDto(UUID.randomUUID(), new ToscaConceptIdentifier(),
            Map.of(), Map.of());
        var instanceElement = new InstanceElementDto(UUID.randomUUID(), UUID.randomUUID(), null, Map.of(), Map.of());
        acElementListenerV2.update(compositionElement, instanceElement, instanceElement);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceElement.instanceId(),
            instanceElement.elementId(), DeployState.DEPLOYED, null,
            StateChangeResult.NO_ERROR, "Update not supported");
    }

    @Test
    void unlockTest() throws PfModelException {
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var acElementListenerV2 = createAcElementListenerV2(intermediaryApi);
        var compositionElement = new CompositionElementDto(UUID.randomUUID(), new ToscaConceptIdentifier(),
            Map.of(), Map.of());
        var instanceElement = new InstanceElementDto(UUID.randomUUID(), UUID.randomUUID(), null, Map.of(), Map.of());
        acElementListenerV2.unlock(compositionElement, instanceElement);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceElement.instanceId(),
            instanceElement.elementId(), null, LockState.UNLOCKED, StateChangeResult.NO_ERROR, "Unlocked");
    }

    @Test
    void primeTest() throws PfModelException {
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var acElementListenerV2 = createAcElementListenerV2(intermediaryApi);
        var compositionId = UUID.randomUUID();
        var toscaConceptIdentifier = new ToscaConceptIdentifier();
        var composition = new CompositionDto(compositionId, Map.of(toscaConceptIdentifier, Map.of()), Map.of());
        acElementListenerV2.prime(composition);
        verify(intermediaryApi)
            .updateCompositionState(compositionId, AcTypeState.PRIMED, StateChangeResult.NO_ERROR, "Primed");
    }

    @Test
    void deprimeTest() throws PfModelException {
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var acElementListenerV2 = createAcElementListenerV2(intermediaryApi);
        var compositionId = UUID.randomUUID();
        var toscaConceptIdentifier = new ToscaConceptIdentifier();
        var composition = new CompositionDto(compositionId, Map.of(toscaConceptIdentifier, Map.of()), Map.of());
        acElementListenerV2.deprime(composition);
        verify(intermediaryApi)
            .updateCompositionState(compositionId, AcTypeState.COMMISSIONED, StateChangeResult.NO_ERROR, "Deprimed");
    }

    @Test
    void handleRestartComposition() {
        var acElementListenerV2 = createAcElementListenerV2(mock(ParticipantIntermediaryApi.class));
        assertThatThrownBy(() -> acElementListenerV2.handleRestartComposition(null, null))
                .isInstanceOf(PfModelException.class);
    }

    @Test
    void handleRestartInstance() {
        var acElementListenerV2 = createAcElementListenerV2(mock(ParticipantIntermediaryApi.class));
        assertThatThrownBy(() -> acElementListenerV2.handleRestartInstance(null, null,
                null, null)).isInstanceOf(PfModelException.class);
    }

    @Test
    void migrateTest() throws PfModelException {
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var acElementListenerV2 = createAcElementListenerV2(intermediaryApi);
        var compositionElement = new CompositionElementDto(UUID.randomUUID(), new ToscaConceptIdentifier(),
            Map.of(), Map.of());
        var instanceElement = new InstanceElementDto(UUID.randomUUID(), UUID.randomUUID(), null, Map.of(), Map.of());
        acElementListenerV2.migrate(compositionElement, compositionElement, instanceElement, instanceElement, 0);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceElement.instanceId(),
            instanceElement.elementId(), DeployState.DEPLOYED, null,
            StateChangeResult.NO_ERROR, "Migrated");
    }

    @Test
    void migratePrecheckTest() throws PfModelException {
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var acElementListenerV1 = createAcElementListenerV2(intermediaryApi);
        var compositionElement = new CompositionElementDto(UUID.randomUUID(), new ToscaConceptIdentifier(),
                Map.of(), Map.of());
        var instanceElement = new InstanceElementDto(UUID.randomUUID(), UUID.randomUUID(), null, Map.of(), Map.of());
        acElementListenerV1.migratePrecheck(compositionElement, compositionElement, instanceElement, instanceElement);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceElement.instanceId(),
                instanceElement.elementId(), DeployState.DEPLOYED, null,
                StateChangeResult.NO_ERROR, "Migration Precheck completed");
    }

    @Test
    void reviewTest() throws PfModelException {
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var acElementListenerV1 = createAcElementListenerV2(intermediaryApi);
        var compositionElement = new CompositionElementDto(UUID.randomUUID(), new ToscaConceptIdentifier(),
                Map.of(), Map.of());
        var instanceElement = new InstanceElementDto(UUID.randomUUID(), UUID.randomUUID(), null, Map.of(), Map.of());
        acElementListenerV1.review(compositionElement, instanceElement);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceElement.instanceId(),
                instanceElement.elementId(), DeployState.DEPLOYED, null,
                StateChangeResult.NO_ERROR, "Review completed");
    }

    @Test
    void prepareTest() throws PfModelException {
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var acElementListenerV1 = createAcElementListenerV2(intermediaryApi);
        var compositionElement = new CompositionElementDto(UUID.randomUUID(), new ToscaConceptIdentifier(),
                Map.of(), Map.of());
        var instanceElement = new InstanceElementDto(UUID.randomUUID(), UUID.randomUUID(), null, Map.of(), Map.of());
        acElementListenerV1.prepare(compositionElement, instanceElement);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceElement.instanceId(),
                instanceElement.elementId(), DeployState.UNDEPLOYED, null,
                StateChangeResult.NO_ERROR, "Prepare completed");
    }

    private AcElementListenerV2 createAcElementListenerV2(ParticipantIntermediaryApi intermediaryApi) {
        return new AcElementListenerV2(intermediaryApi) {
            @Override
            public void deploy(CompositionElementDto compositionElement, InstanceElementDto instanceElement) {
                // dummy implementation
            }

            @Override
            public void undeploy(CompositionElementDto compositionElement, InstanceElementDto instanceElement) {
                // dummy implementation
            }
        };
    }
}
