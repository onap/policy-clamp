/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.intermediary.api.impl;

import java.util.UUID;
import org.onap.policy.clamp.acm.participant.intermediary.api.AutomationCompositionElementListener;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.acm.participant.intermediary.handler.AutomationCompositionHandler;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageType;
import org.springframework.stereotype.Component;

/**
 * This class is api implementation used by participant intermediary.
 */
@Component
public class ParticipantIntermediaryApiImpl implements ParticipantIntermediaryApi {

    // The handler for the automationComposition intermediary
    private final AutomationCompositionHandler automationCompositionHandler;

    /**
     * Constructor.
     *
     * @param automationCompositionHandler AutomationCompositionHandler
     */
    public ParticipantIntermediaryApiImpl(AutomationCompositionHandler automationCompositionHandler) {
        this.automationCompositionHandler = automationCompositionHandler;
    }

    @Override
    public void registerAutomationCompositionElementListener(
            AutomationCompositionElementListener automationCompositionElementListener) {
        automationCompositionHandler.registerAutomationCompositionElementListener(automationCompositionElementListener);
    }

    @Override
    public AutomationCompositionElement updateAutomationCompositionElementState(UUID automationCompositionId, UUID id,
            AutomationCompositionOrderedState currentState, AutomationCompositionState newState,
            ParticipantMessageType messageType) {
        return automationCompositionHandler.updateAutomationCompositionElementState(automationCompositionId, id,
                currentState, newState);
    }
}
