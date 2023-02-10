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

import java.util.UUID;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;

/**
 * This interface is used by participant implementations to use the participant intermediary.
 */
public interface ParticipantIntermediaryApi {

    /**
     * Register a listener for automation composition elements that are mediated by the intermediary.
     *
     * @param automationCompositionElementListener The automation composition element listener to register
     */
    void registerAutomationCompositionElementListener(
            AutomationCompositionElementListener automationCompositionElementListener);

    /**
     * Update the state of a automation composition element.
     *
     * @param id the ID of the automation composition element to update the state on
     * @param newState the state of the automation composition element
     */
    void updateAutomationCompositionElementState(UUID automationCompositionId, UUID id, DeployState newState,
            LockState lockState);
}
