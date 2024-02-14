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

    @Override
    public void handleRestartComposition(CompositionDto composition, AcTypeState state) throws PfModelException {
        switch (state) {
            case PRIMING -> prime(composition);
            case DEPRIMING -> deprime(composition);
            default -> intermediaryApi
                .updateCompositionState(composition.compositionId(), state, StateChangeResult.NO_ERROR, "Restarted");
        }
    }

    @Override
    public void handleRestartInstance(CompositionElementDto compositionElement, InstanceElementDto instanceElement,
                                      DeployState deployState, LockState lockState) throws PfModelException {

        if (DeployState.DEPLOYING.equals(deployState)) {
            deploy(compositionElement, instanceElement);
            return;
        }
        if (DeployState.UNDEPLOYING.equals(deployState)) {
            undeploy(compositionElement, instanceElement);
            return;
        }
        if (DeployState.UPDATING.equals(deployState)) {
            update(compositionElement, instanceElement, instanceElement);
            return;
        }
        if (DeployState.DELETING.equals(deployState)) {
            delete(compositionElement, instanceElement);
            return;
        }
        if (LockState.LOCKING.equals(lockState)) {
            lock(compositionElement, instanceElement);
            return;
        }
        if (LockState.UNLOCKING.equals(lockState)) {
            unlock(compositionElement, instanceElement);
            return;
        }
        intermediaryApi.updateAutomationCompositionElementState(instanceElement.instanceId(),
            instanceElement.elementId(), deployState, lockState, StateChangeResult.NO_ERROR, "Restarted");
    }

    @Override
    public void migrate(CompositionElementDto compositionElement, CompositionElementDto compositionElementTarget,
                        InstanceElementDto instanceElement, InstanceElementDto instanceElementMigrate)
        throws PfModelException {
        intermediaryApi.updateAutomationCompositionElementState(instanceElementMigrate.instanceId(),
            instanceElementMigrate.elementId(), DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Migrated");
    }
}
