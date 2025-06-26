/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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
public abstract class AcElementListenerV2 extends AcElementListenerV3
        implements AutomationCompositionElementListenerV2 {

    protected static final String NOT_SUPPORTED = "not supported!";

    protected AcElementListenerV2(ParticipantIntermediaryApi intermediaryApi) {
        super(intermediaryApi);
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
                        InstanceElementDto instanceElement, InstanceElementDto instanceElementMigrate, int stage)
        throws PfModelException {
        migrate(compositionElement, compositionElementTarget, instanceElement, instanceElementMigrate);
    }

    @Override
    public void migrate(CompositionElementDto compositionElement, CompositionElementDto compositionElementTarget,
                        InstanceElementDto instanceElement, InstanceElementDto instanceElementMigrate)
        throws PfModelException {
        intermediaryApi.updateAutomationCompositionElementState(instanceElementMigrate.instanceId(),
            instanceElementMigrate.elementId(), DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Migrated");
    }
}
