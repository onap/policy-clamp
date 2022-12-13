/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation.
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

import java.util.Map;
import java.util.UUID;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.models.base.PfModelException;

/**
 * This interface is implemented by participant implementations to receive updates on automation composition elements.
 */
public interface AutomationCompositionElementListener {
    /**
     * Handle a automation composition element state change.
     *
     * @param automationCompositionElementId the ID of the automation composition element
     * @param currentState the current state of the automation composition element
     * @param newState the state to which the automation composition element is changing to
     * @throws PfModelException in case of a model exception
     */
    public void automationCompositionElementStateChange(UUID automationCompositionId,
        UUID automationCompositionElementId, AutomationCompositionState currentState,
        AutomationCompositionOrderedState newState) throws PfModelException;

    /**
     * Handle an update on a automation composition element.
     *
     * @param automationCompositionId the automationComposition Id
     * @param element the information on the automation composition element
     * @param properties properties Map
     * @throws PfModelException from Policy framework
     */
    public void automationCompositionElementUpdate(UUID automationCompositionId,
        AutomationCompositionElement element, Map<String, Object> properties) throws PfModelException;
}
