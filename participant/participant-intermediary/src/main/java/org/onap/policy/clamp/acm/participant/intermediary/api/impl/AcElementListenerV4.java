/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025-2026 OpenInfra Foundation Europe. All rights reserved.
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
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.models.base.PfModelException;
import org.springframework.beans.factory.annotation.Value;

/**
 * Wrapper of AutomationCompositionElementListener.
 * Valid since 8.1.1 release.
 */
public abstract class AcElementListenerV4 implements AutomationCompositionElementListener {
    protected final ParticipantIntermediaryApi intermediaryApi;
    public static final String NOT_SUPPORTED = "Not supported";
    public static final String NOT_IMPLEMENTED = "Not implemented";

    @Value("${participant.intermediaryParameters.failUnsupportedOperation:true}")
    protected boolean failUnsupportedOperation;

    protected AcElementListenerV4(ParticipantIntermediaryApi intermediaryApi) {
        this.intermediaryApi = intermediaryApi;
    }

    @Override
    public void lock(CompositionElementDto compositionElement, InstanceElementDto instanceElement)
        throws PfModelException {
        if (failUnsupportedOperation) {
            sendLockResult(instanceElement, LockState.UNLOCKED, StateChangeResult.FAILED, NOT_SUPPORTED);
        } else {
            sendLockResult(instanceElement, LockState.LOCKED, StateChangeResult.NO_ERROR, NOT_IMPLEMENTED);
        }
    }

    @Override
    public void unlock(CompositionElementDto compositionElement, InstanceElementDto instanceElement)
        throws PfModelException {
        if (failUnsupportedOperation) {
            sendLockResult(instanceElement, LockState.LOCKED, StateChangeResult.FAILED, NOT_SUPPORTED);
        } else {
            sendLockResult(instanceElement, LockState.UNLOCKED, StateChangeResult.NO_ERROR, NOT_IMPLEMENTED);
        }
    }

    protected void sendLockResult(InstanceElementDto instanceElement, LockState lockState,
            StateChangeResult stateChangeResult, String message) {
        intermediaryApi.updateAutomationCompositionElementState(instanceElement.instanceId(),
                instanceElement.elementId(), null, lockState, stateChangeResult, message);
    }

    @Override
    public void delete(CompositionElementDto compositionElement, InstanceElementDto instanceElement)
        throws PfModelException {
        if (failUnsupportedOperation) {
            sendDeployStatus(instanceElement, DeployState.UNDEPLOYED, StateChangeResult.FAILED, NOT_SUPPORTED);
        } else {
            sendDeployStatus(instanceElement, DeployState.DELETED, StateChangeResult.NO_ERROR, NOT_IMPLEMENTED);
        }
    }

    @Override
    public void update(CompositionElementDto compositionElement, InstanceElementDto instanceElement,
                       InstanceElementDto instanceElementUpdated) throws PfModelException {
        if (failUnsupportedOperation) {
            sendDeployStatus(instanceElementUpdated, DeployState.DEPLOYED, StateChangeResult.FAILED, NOT_SUPPORTED);
        } else {
            var stateChangeResult = AcmUtils
                    .equalMap(instanceElement.inProperties(), instanceElementUpdated.inProperties())
                    ? StateChangeResult.NO_ERROR : StateChangeResult.FAILED;
            sendDeployStatus(instanceElementUpdated, DeployState.DEPLOYED, stateChangeResult, NOT_IMPLEMENTED);
        }
    }

    @Override
    public void prime(CompositionDto composition) throws PfModelException {
        if (failUnsupportedOperation) {
            intermediaryApi.updateCompositionState(composition.compositionId(), AcTypeState.COMMISSIONED,
                    StateChangeResult.FAILED, NOT_SUPPORTED);
        } else {
            intermediaryApi.updateCompositionState(composition.compositionId(), AcTypeState.PRIMED,
                    StateChangeResult.NO_ERROR, NOT_IMPLEMENTED);
        }
    }

    @Override
    public void deprime(CompositionDto composition) throws PfModelException {
        if (failUnsupportedOperation) {
            intermediaryApi.updateCompositionState(composition.compositionId(), AcTypeState.PRIMED,
                    StateChangeResult.FAILED, NOT_SUPPORTED);
        } else {
            intermediaryApi.updateCompositionState(composition.compositionId(), AcTypeState.COMMISSIONED,
                    StateChangeResult.NO_ERROR, NOT_IMPLEMENTED);
        }
    }

    @Override
    public void migrate(CompositionElementDto compositionElement, CompositionElementDto compositionElementTarget,
                        InstanceElementDto instanceElement, InstanceElementDto instanceElementMigrate, int stage)
        throws PfModelException {
        if (failUnsupportedOperation) {
            sendDeployStatus(instanceElementMigrate, DeployState.DEPLOYED, StateChangeResult.FAILED, NOT_SUPPORTED);
        } else {
            var stateChangeResult = AcmUtils
                    .equalMap(instanceElement.inProperties(), instanceElementMigrate.inProperties())
                    ? StateChangeResult.NO_ERROR : StateChangeResult.FAILED;
            sendDeployStatus(instanceElementMigrate, DeployState.DEPLOYED, stateChangeResult, NOT_IMPLEMENTED);
        }
    }

    @Override
    public void migratePrecheck(CompositionElementDto compositionElement,
                                CompositionElementDto compositionElementTarget, InstanceElementDto instanceElement,
                                InstanceElementDto instanceElementMigrate) throws PfModelException {
        if (failUnsupportedOperation) {
            sendDeployStatus(instanceElementMigrate, DeployState.DEPLOYED, StateChangeResult.FAILED, NOT_SUPPORTED);
        } else {
            var stateChangeResult = AcmUtils
                    .equalMap(instanceElement.inProperties(), instanceElementMigrate.inProperties())
                    ? StateChangeResult.NO_ERROR : StateChangeResult.FAILED;
            sendDeployStatus(instanceElementMigrate, DeployState.DEPLOYED, stateChangeResult, NOT_IMPLEMENTED);
        }
    }

    @Override
    public void review(CompositionElementDto compositionElement, InstanceElementDto instanceElement)
        throws PfModelException {
        if (failUnsupportedOperation) {
            sendDeployStatus(instanceElement, DeployState.DEPLOYED, StateChangeResult.FAILED, NOT_SUPPORTED);
        } else {
            sendDeployStatus(instanceElement, DeployState.DEPLOYED, StateChangeResult.NO_ERROR, NOT_IMPLEMENTED);
        }
    }

    @Override
    public void prepare(CompositionElementDto compositionElement, InstanceElementDto instanceElement, int nextStage)
        throws PfModelException {
        if (failUnsupportedOperation) {
            sendDeployStatus(instanceElement, DeployState.UNDEPLOYED, StateChangeResult.FAILED, NOT_SUPPORTED);
        } else {
            sendDeployStatus(instanceElement, DeployState.UNDEPLOYED, StateChangeResult.NO_ERROR, NOT_IMPLEMENTED);
        }
    }

    @Override
    public void rollbackMigration(CompositionElementDto compositionElement,
            CompositionElementDto compositionElementRollback, InstanceElementDto instanceElement,
            InstanceElementDto instanceElementRollback, int stage) {
        if (failUnsupportedOperation) {
            sendDeployStatus(instanceElementRollback, DeployState.DEPLOYED, StateChangeResult.FAILED, NOT_SUPPORTED);
        } else {
            var stateChangeResult = AcmUtils
                    .equalMap(instanceElement.inProperties(), instanceElementRollback.inProperties())
                    ? StateChangeResult.NO_ERROR : StateChangeResult.FAILED;
            sendDeployStatus(instanceElementRollback, DeployState.DEPLOYED, stateChangeResult, NOT_IMPLEMENTED);
        }
    }

    protected void sendDeployStatus(InstanceElementDto instanceElement, DeployState deployState,
            StateChangeResult stateChangeResult, String message) {
        intermediaryApi.updateAutomationCompositionElementState(instanceElement.instanceId(),
                instanceElement.elementId(), deployState, null, stateChangeResult, message);
    }
}
