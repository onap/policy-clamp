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

import jakarta.ws.rs.core.Response;
import org.onap.policy.clamp.acm.participant.intermediary.api.AutomationCompositionElementListener;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.InstanceElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.models.base.PfModelException;

/**
 * Wrapper of AutomationCompositionElementListener.
 * Valid since 7.1.1 release.
 */
public abstract class AcElementListenerV2 implements AutomationCompositionElementListener {
    protected final ParticipantIntermediaryApi intermediaryApi;

    private static final String NOT_SUPPORTED = "not supported!";

    protected AcElementListenerV2(ParticipantIntermediaryApi intermediaryApi) {
        this.intermediaryApi = intermediaryApi;
    }

    @Override
    public void lock(CompositionElementDto compositionElement, InstanceElementDto instanceElement)
        throws PfModelException {
        intermediaryApi.updateAutomationCompositionElementState(instanceElement.instanceId(),
            instanceElement.elementId(), null, LockState.LOCKED, StateChangeResult.NO_ERROR, "Locked");
    }

    @Override
    public void unlock(CompositionElementDto compositionElement, InstanceElementDto instanceElement)
        throws PfModelException {
        intermediaryApi.updateAutomationCompositionElementState(instanceElement.instanceId(),
            instanceElement.elementId(), null, LockState.UNLOCKED, StateChangeResult.NO_ERROR, "Unlocked");
    }

    @Override
    public void delete(CompositionElementDto compositionElement, InstanceElementDto instanceElement)
        throws PfModelException {
        intermediaryApi.updateAutomationCompositionElementState(instanceElement.instanceId(),
            instanceElement.elementId(), DeployState.DELETED, null, StateChangeResult.NO_ERROR, "Deleted");
    }

    @Override
    public void update(CompositionElementDto compositionElement, InstanceElementDto instanceElement,
                       InstanceElementDto instanceElementUpdated) throws PfModelException {
        intermediaryApi.updateAutomationCompositionElementState(instanceElement.instanceId(),
            instanceElement.elementId(), DeployState.DEPLOYED, null,
            StateChangeResult.NO_ERROR, "Update not supported");

    }

    @Override
    public void prime(CompositionDto composition) throws PfModelException {
        intermediaryApi.updateCompositionState(composition.compositionId(), AcTypeState.PRIMED,
            StateChangeResult.NO_ERROR, "Primed");
    }

    @Override
    public void deprime(CompositionDto composition) throws PfModelException {
        intermediaryApi.updateCompositionState(composition.compositionId(), AcTypeState.COMMISSIONED,
            StateChangeResult.NO_ERROR, "Deprimed");
    }

    public void handleRestartComposition(CompositionDto composition, AcTypeState state) throws PfModelException {
        throw new PfModelException(Response.Status.BAD_REQUEST, NOT_SUPPORTED);
    }

    public void handleRestartInstance(CompositionElementDto compositionElement, InstanceElementDto instanceElement,
                                      DeployState deployState, LockState lockState) throws PfModelException {
        throw new PfModelException(Response.Status.BAD_REQUEST, NOT_SUPPORTED);
    }

    @Override
    public void migrate(CompositionElementDto compositionElement, CompositionElementDto compositionElementTarget,
                        InstanceElementDto instanceElement, InstanceElementDto instanceElementMigrate)
        throws PfModelException {
        intermediaryApi.updateAutomationCompositionElementState(instanceElementMigrate.instanceId(),
            instanceElementMigrate.elementId(), DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Migrated");
    }
}
