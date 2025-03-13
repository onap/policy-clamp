/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.InstanceElementDto;
import org.onap.policy.models.base.PfModelException;

public interface AutomationCompositionElementListenerV3 {

    void deploy(CompositionElementDto compositionElement, InstanceElementDto instanceElement) throws PfModelException;

    void undeploy(CompositionElementDto compositionElement, InstanceElementDto instanceElement) throws PfModelException;

    void lock(CompositionElementDto compositionElement, InstanceElementDto instanceElement) throws PfModelException;

    void unlock(CompositionElementDto compositionElement, InstanceElementDto instanceElement) throws PfModelException;

    void delete(CompositionElementDto compositionElement, InstanceElementDto instanceElement) throws PfModelException;

    void update(CompositionElementDto compositionElement, InstanceElementDto instanceElement,
            InstanceElementDto instanceElementUpdated) throws PfModelException;

    void prime(CompositionDto composition) throws PfModelException;

    void deprime(CompositionDto composition) throws PfModelException;

    void migrate(CompositionElementDto compositionElement, CompositionElementDto compositionElementTarget,
            InstanceElementDto instanceElement, InstanceElementDto instanceElementMigrate,
            int nextStage) throws PfModelException;

    void migratePrecheck(CompositionElementDto compositionElement, CompositionElementDto compositionElementTarget,
            InstanceElementDto instanceElement, InstanceElementDto instanceElementMigrate) throws PfModelException;

    void review(CompositionElementDto compositionElement, InstanceElementDto instanceElement)
        throws PfModelException;

    void prepare(CompositionElementDto compositionElement, InstanceElementDto instanceElement)
        throws PfModelException;
}
