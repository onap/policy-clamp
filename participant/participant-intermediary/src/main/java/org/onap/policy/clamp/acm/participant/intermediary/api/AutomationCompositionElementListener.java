/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.intermediary.api;

import org.onap.policy.models.base.PfModelException;

/**
 * This interface is implemented by participant implementations to receive updates on automation composition elements.
 */
public interface AutomationCompositionElementListener {
    /**
     * Handle a deploy on a automation composition element.
     *
     * @param compositionElement the information of the Automation Composition Definition Element
     * @param instanceElement the information of the Automation Composition Instance Element
     * @throws PfModelException from Policy framework
     */
    void deploy(CompositionElementDto compositionElement, InstanceElementDto instanceElement) throws PfModelException;

    /**
     * Handle an udeploy on a automation composition element.
     *
     * @param compositionElement the information of the Automation Composition Definition Element
     * @param instanceElement the information of the Automation Composition Instance Element
     * @throws PfModelException in case of a model exception
     */
    void undeploy(CompositionElementDto compositionElement, InstanceElementDto instanceElement) throws PfModelException;

    /**
     * Handle a lock on a automation composition element.
     *
     * @param compositionElement the information of the Automation Composition Definition Element
     * @param instanceElement the information of the Automation Composition Instance Element
     * @throws PfModelException in case of a model exception
     */
    void lock(CompositionElementDto compositionElement, InstanceElementDto instanceElement) throws PfModelException;

    /**
     * Handle an unlock on a automation composition element.
     *
     * @param compositionElement the information of the Automation Composition Definition Element
     * @param instanceElement the information of the Automation Composition Instance Element
     * @throws PfModelException in case of a model exception
     */
    void unlock(CompositionElementDto compositionElement, InstanceElementDto instanceElement) throws PfModelException;

    /**
     * Handle a delete on a automation composition element.
     *
     * @param compositionElement the information of the Automation Composition Definition Element
     * @param instanceElement the information of the Automation Composition Instance Element
     * @throws PfModelException in case of a model exception
     */
    void delete(CompositionElementDto compositionElement, InstanceElementDto instanceElement) throws PfModelException;

    /**
     * Handle an update on a automation composition element.
     *
     * @param compositionElement the information of the Automation Composition Definition Element
     * @param instanceElement the information of the Automation Composition Instance Element
     * @param instanceElementUpdated the information of the Automation Composition Instance Element updated
     * @throws PfModelException from Policy framework
     */
    void update(CompositionElementDto compositionElement, InstanceElementDto instanceElement,
            InstanceElementDto instanceElementUpdated) throws PfModelException;

    void prime(CompositionDto composition) throws PfModelException;

    void deprime(CompositionDto composition) throws PfModelException;

    /**
     * Handle an update on a automation composition element.
     *
     * @param compositionElement the information of the Automation Composition Definition Element
     * @param compositionElementTarget the information of the Automation Composition Definition Element Target
     * @param instanceElement the information of the Automation Composition Instance Element
     * @param instanceElementMigrate the information of the Automation Composition Instance Element updated
     * @throws PfModelException from Policy framework
     */
    void migrate(CompositionElementDto compositionElement, CompositionElementDto compositionElementTarget,
            InstanceElementDto instanceElement, InstanceElementDto instanceElementMigrate) throws PfModelException;
}
