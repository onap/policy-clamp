/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation.
 *  Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;

/**
 * This interface is used by participant implementations to use the participant intermediary.
 */
public interface ParticipantIntermediaryApi {

    /**
     * Update the state of a automation composition element.
     *
     * @param automationCompositionId the ID of the automation composition to update the state on
     * @param elementId the ID of the automation composition element to update the state on
     * @param deployState the Deploy State of the automation composition element
     * @param lockState the Lock State of the automation composition element
     * @param stateChangeResult the indicator if error occurs
     * @param message the message
     */
    void updateAutomationCompositionElementState(UUID automationCompositionId, UUID elementId, DeployState deployState,
            LockState lockState, StateChangeResult stateChangeResult, String message);

    /**
     * Get a copy of all AutomationCompositions.
     *
     * @return get all AutomationCompositions
     */
    Map<UUID, AutomationComposition> getAutomationCompositions();

    /**
     * Get a copy of the AutomationCompositionElement by automationCompositionId and elementId.
     *
     * @param automationCompositionId the ID of the automation composition to update the state on
     * @param elementId the ID of the automation composition element to update the state on
     * @return get the AutomationCompositionElement
     */
    AutomationCompositionElement getAutomationCompositionElement(UUID automationCompositionId, UUID elementId);

    /**
     * Send Automation Composition Element update Info to AC-runtime.
     *
     * @param automationCompositionId the ID of the automation composition to update the states
     * @param id the ID of the automation composition element to update the states
     * @param useState the use State
     * @param operationalState the operational State
     * @param outProperties the output Properties Map
     */
    void sendAcElementInfo(UUID automationCompositionId, UUID id, String useState, String operationalState,
            Map<String, Object> outProperties);

    void updateCompositionState(UUID compositionId, AcTypeState state, StateChangeResult stateChangeResult,
            String message);
}
