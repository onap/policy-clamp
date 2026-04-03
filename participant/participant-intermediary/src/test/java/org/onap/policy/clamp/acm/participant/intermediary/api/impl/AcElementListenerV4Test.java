/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025-2026 OpenInfra Foundation Europe. All rights reserved
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.policy.clamp.acm.participant.intermediary.api.impl;

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

class AcElementListenerV4Test {

    private static final UUID COMPOSITION_ID = UUID.randomUUID();
    private static final ToscaConceptIdentifier COMPOSITION_ELEMENT_ID = new ToscaConceptIdentifier();
    private static final CompositionDto COMPOSITION =
            new CompositionDto(COMPOSITION_ID, Map.of(COMPOSITION_ELEMENT_ID, Map.of()), Map.of());
    private static final CompositionElementDto COMPOSITION_ELEMENT =
            new CompositionElementDto(COMPOSITION_ID, COMPOSITION_ELEMENT_ID, Map.of(), Map.of());

    private static final UUID INSTANCE_ID = UUID.randomUUID();
    private static final UUID ELEMENT_ID = UUID.randomUUID();
    private static final InstanceElementDto INSTANCE_ELEMENT =
            new InstanceElementDto(INSTANCE_ID, ELEMENT_ID, Map.of(), Map.of());
    private static final InstanceElementDto INSTANCE_ELEMENT_UPDATED =
            new InstanceElementDto(INSTANCE_ID, ELEMENT_ID, Map.of("key", "value"), Map.of());

    @Test
    void primeTest() throws PfModelException {
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var acElementListener = createAcElementListenerV4(intermediaryApi);
        acElementListener.failUnsupported = true;
        acElementListener.prime(COMPOSITION);
        verify(intermediaryApi).updateCompositionState(COMPOSITION_ID, AcTypeState.COMMISSIONED,
                StateChangeResult.FAILED, AcElementListenerV4.NOT_SUPPORTED);

        acElementListener.failUnsupported = false;
        acElementListener.prime(COMPOSITION);
        verify(intermediaryApi).updateCompositionState(COMPOSITION_ID, AcTypeState.PRIMED,
                StateChangeResult.NO_ERROR, AcElementListenerV4.NOT_IMPLEMENTED);
    }

    @Test
    void deprimeTest() throws PfModelException {
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var acElementListener = createAcElementListenerV4(intermediaryApi);
        acElementListener.failUnsupported = true;
        acElementListener.deprime(COMPOSITION);
        verify(intermediaryApi).updateCompositionState(COMPOSITION_ID, AcTypeState.PRIMED,
                StateChangeResult.FAILED, AcElementListenerV4.NOT_SUPPORTED);

        acElementListener.failUnsupported = false;
        acElementListener.deprime(COMPOSITION);
        verify(intermediaryApi).updateCompositionState(COMPOSITION_ID, AcTypeState.COMMISSIONED,
                StateChangeResult.NO_ERROR, AcElementListenerV4.NOT_IMPLEMENTED);
    }

    @Test
    void lockTest() throws PfModelException {
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var acElementListener = createAcElementListenerV4(intermediaryApi);
        acElementListener.failUnsupported = true;
        acElementListener.lock(COMPOSITION_ELEMENT, INSTANCE_ELEMENT);
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, ELEMENT_ID, null,
                LockState.UNLOCKED, StateChangeResult.FAILED, AcElementListenerV4.NOT_SUPPORTED);

        acElementListener.failUnsupported = false;
        acElementListener.lock(COMPOSITION_ELEMENT, INSTANCE_ELEMENT);
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, ELEMENT_ID, null,
                LockState.LOCKED, StateChangeResult.NO_ERROR, AcElementListenerV4.NOT_IMPLEMENTED);
    }

    @Test
    void unlockTest() throws PfModelException {
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var acElementListener = createAcElementListenerV4(intermediaryApi);
        acElementListener.failUnsupported = true;
        acElementListener.unlock(COMPOSITION_ELEMENT, INSTANCE_ELEMENT);
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, ELEMENT_ID, null,
                LockState.LOCKED, StateChangeResult.FAILED, AcElementListenerV4.NOT_SUPPORTED);

        acElementListener.failUnsupported = false;
        acElementListener.unlock(COMPOSITION_ELEMENT, INSTANCE_ELEMENT);
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, ELEMENT_ID, null,
                LockState.UNLOCKED, StateChangeResult.NO_ERROR, AcElementListenerV4.NOT_IMPLEMENTED);
    }

    @Test
    void deleteTest() throws PfModelException {
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var acElementListener = createAcElementListenerV4(intermediaryApi);
        acElementListener.failUnsupported = true;
        acElementListener.delete(COMPOSITION_ELEMENT, INSTANCE_ELEMENT);
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, ELEMENT_ID,
                DeployState.UNDEPLOYED, null, StateChangeResult.FAILED, AcElementListenerV4.NOT_SUPPORTED);

        acElementListener.failUnsupported = false;
        acElementListener.delete(COMPOSITION_ELEMENT, INSTANCE_ELEMENT);
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, ELEMENT_ID, DeployState.DELETED,
                null, StateChangeResult.NO_ERROR, AcElementListenerV4.NOT_IMPLEMENTED);
    }

    @Test
    void updateTest() throws PfModelException {
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var acElementListener = createAcElementListenerV4(intermediaryApi);
        acElementListener.failUnsupported = true;
        acElementListener.update(COMPOSITION_ELEMENT, INSTANCE_ELEMENT, INSTANCE_ELEMENT);
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, ELEMENT_ID, DeployState.DEPLOYED,
                null, StateChangeResult.FAILED, AcElementListenerV4.NOT_SUPPORTED);

        acElementListener.failUnsupported = false;
        acElementListener.update(COMPOSITION_ELEMENT, INSTANCE_ELEMENT, INSTANCE_ELEMENT);
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, ELEMENT_ID, DeployState.DEPLOYED,
                null, StateChangeResult.NO_ERROR, AcElementListenerV4.NOT_IMPLEMENTED);

        acElementListener.update(COMPOSITION_ELEMENT, INSTANCE_ELEMENT, INSTANCE_ELEMENT_UPDATED);
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, ELEMENT_ID, DeployState.DEPLOYED,
                null, StateChangeResult.FAILED, AcElementListenerV4.NOT_IMPLEMENTED);
    }

    @Test
    void migrateTest() throws PfModelException {
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var acElementListener = createAcElementListenerV4(intermediaryApi);
        acElementListener.failUnsupported = true;
        acElementListener.migrate(COMPOSITION_ELEMENT, COMPOSITION_ELEMENT, INSTANCE_ELEMENT, INSTANCE_ELEMENT, 0);
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, ELEMENT_ID, DeployState.DEPLOYED,
                null, StateChangeResult.FAILED, AcElementListenerV4.NOT_SUPPORTED);

        acElementListener.failUnsupported = false;
        acElementListener.migrate(COMPOSITION_ELEMENT, COMPOSITION_ELEMENT, INSTANCE_ELEMENT, INSTANCE_ELEMENT, 0);
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, ELEMENT_ID, DeployState.DEPLOYED,
                null, StateChangeResult.NO_ERROR, AcElementListenerV4.NOT_IMPLEMENTED);

        acElementListener
                .migrate(COMPOSITION_ELEMENT, COMPOSITION_ELEMENT, INSTANCE_ELEMENT, INSTANCE_ELEMENT_UPDATED, 0);
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, ELEMENT_ID, DeployState.DEPLOYED,
                null, StateChangeResult.FAILED, AcElementListenerV4.NOT_IMPLEMENTED);
    }

    @Test
    void testRollbackMigration() {
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var acElementListener = createAcElementListenerV4(intermediaryApi);
        acElementListener.failUnsupported = true;
        acElementListener.rollbackMigration(COMPOSITION_ELEMENT, COMPOSITION_ELEMENT, INSTANCE_ELEMENT,
                INSTANCE_ELEMENT, 0);
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, ELEMENT_ID, DeployState.DEPLOYED,
                null, StateChangeResult.FAILED, AcElementListenerV4.NOT_SUPPORTED);

        acElementListener.failUnsupported = false;
        acElementListener.rollbackMigration(COMPOSITION_ELEMENT, COMPOSITION_ELEMENT, INSTANCE_ELEMENT,
                INSTANCE_ELEMENT, 0);
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, ELEMENT_ID, DeployState.DEPLOYED,
                null, StateChangeResult.NO_ERROR, AcElementListenerV4.NOT_IMPLEMENTED);

        acElementListener.rollbackMigration(COMPOSITION_ELEMENT, COMPOSITION_ELEMENT, INSTANCE_ELEMENT,
                INSTANCE_ELEMENT_UPDATED, 0);
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, ELEMENT_ID, DeployState.DEPLOYED,
                null, StateChangeResult.FAILED, AcElementListenerV4.NOT_IMPLEMENTED);
    }

    @Test
    void reviewTest() throws PfModelException {
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var acElementListener = createAcElementListenerV4(intermediaryApi);
        acElementListener.failUnsupported = true;
        acElementListener.review(COMPOSITION_ELEMENT, INSTANCE_ELEMENT);
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, ELEMENT_ID, DeployState.DEPLOYED,
                null, StateChangeResult.FAILED, AcElementListenerV4.NOT_SUPPORTED);

        acElementListener.failUnsupported = false;
        acElementListener.review(COMPOSITION_ELEMENT, INSTANCE_ELEMENT);
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, ELEMENT_ID, DeployState.DEPLOYED,
                null, StateChangeResult.NO_ERROR, AcElementListenerV4.NOT_IMPLEMENTED);
    }

    @Test
    void testPrepare() throws PfModelException {
        var intermediaryApi = mock(ParticipantIntermediaryApiImpl.class);
        var acElementListener = createAcElementListenerV4(intermediaryApi);
        acElementListener.failUnsupported = true;
        acElementListener.prepare(COMPOSITION_ELEMENT, INSTANCE_ELEMENT, 1);
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, ELEMENT_ID, DeployState.UNDEPLOYED,
                null, StateChangeResult.FAILED, AcElementListenerV4.NOT_SUPPORTED);

        acElementListener.failUnsupported = false;
        acElementListener.prepare(COMPOSITION_ELEMENT, INSTANCE_ELEMENT, 1);
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, ELEMENT_ID, DeployState.UNDEPLOYED,
                null, StateChangeResult.NO_ERROR, AcElementListenerV4.NOT_IMPLEMENTED);
    }

    @Test
    void migratePrecheckTest() throws PfModelException {
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var acElementListener = createAcElementListenerV4(intermediaryApi);
        acElementListener.failUnsupported = true;
        acElementListener.migratePrecheck(COMPOSITION_ELEMENT, COMPOSITION_ELEMENT, INSTANCE_ELEMENT, INSTANCE_ELEMENT);
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, ELEMENT_ID, DeployState.DEPLOYED,
                null, StateChangeResult.FAILED, AcElementListenerV4.NOT_SUPPORTED);

        acElementListener.failUnsupported = false;
        acElementListener.migratePrecheck(COMPOSITION_ELEMENT, COMPOSITION_ELEMENT, INSTANCE_ELEMENT, INSTANCE_ELEMENT);
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, ELEMENT_ID, DeployState.DEPLOYED,
                null, StateChangeResult.NO_ERROR, AcElementListenerV4.NOT_IMPLEMENTED);

        acElementListener
                .migratePrecheck(COMPOSITION_ELEMENT, COMPOSITION_ELEMENT, INSTANCE_ELEMENT, INSTANCE_ELEMENT_UPDATED);
        verify(intermediaryApi).updateAutomationCompositionElementState(INSTANCE_ID, ELEMENT_ID, DeployState.DEPLOYED,
                null, StateChangeResult.FAILED, AcElementListenerV4.NOT_IMPLEMENTED);
    }

    private AcElementListenerV4 createAcElementListenerV4(ParticipantIntermediaryApi intermediaryApi) {
        return new AcElementListenerV4(intermediaryApi) {
            @Override
            public void deploy(CompositionElementDto compositionElement, InstanceElementDto instanceElement) {
                // do nothing
            }

            @Override
            public void undeploy(CompositionElementDto compositionElement, InstanceElementDto instanceElement) {
                // do nothing
            }
        };
    }
}
