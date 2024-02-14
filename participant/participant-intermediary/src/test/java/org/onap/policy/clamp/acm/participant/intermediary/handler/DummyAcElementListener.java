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

import org.onap.policy.clamp.acm.participant.intermediary.api.AutomationCompositionElementListener;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.InstanceElementDto;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.models.base.PfModelException;

public class DummyAcElementListener implements AutomationCompositionElementListener {
    @Override
    public void deploy(CompositionElementDto compositionElement, InstanceElementDto instanceElement)
        throws PfModelException {

    }

    @Override
    public void undeploy(CompositionElementDto compositionElement, InstanceElementDto instanceElement)
        throws PfModelException {

    }

    @Override
    public void lock(CompositionElementDto compositionElement, InstanceElementDto instanceElement)
        throws PfModelException {

    }

    @Override
    public void unlock(CompositionElementDto compositionElement, InstanceElementDto instanceElement)
        throws PfModelException {

    }

    @Override
    public void delete(CompositionElementDto compositionElement, InstanceElementDto instanceElement)
        throws PfModelException {
    }

    @Override
    public void update(CompositionElementDto compositionElement, InstanceElementDto instanceElement,
                       InstanceElementDto instanceElementUpdated)
            throws PfModelException {
    }

    @Override
    public void prime(CompositionDto composition) throws PfModelException {
    }

    @Override
    public void deprime(CompositionDto composition) throws PfModelException {
    }

    @Override
    public void handleRestartComposition(CompositionDto composition, AcTypeState state) throws PfModelException {
    }

    @Override
    public void handleRestartInstance(CompositionElementDto compositionElement, InstanceElementDto instanceElement,
        DeployState deployState, LockState lockState) throws PfModelException {
    }

    @Override
    public void migrate(CompositionElementDto compositionElement, CompositionElementDto compositionElementTarget,
                        InstanceElementDto instanceElement, InstanceElementDto instanceElementMigrate)
        throws PfModelException {
    }
}
