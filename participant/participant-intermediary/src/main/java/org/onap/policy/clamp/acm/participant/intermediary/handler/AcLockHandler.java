/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.intermediary.handler;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.participant.intermediary.api.InstanceElementDto;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUtils;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionStateChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AcLockHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcLockHandler.class);

    private final CacheProvider cacheProvider;
    private final ThreadHandler listener;

    /**
     * Handle a automation composition state change message.
     *
     * @param stateChangeMsg the state change message
     */
    public void handleAutomationCompositionStateChange(AutomationCompositionStateChange stateChangeMsg) {
        if (stateChangeMsg.getAutomationCompositionId() == null) {
            return;
        }

        var automationComposition = cacheProvider.getAutomationComposition(stateChangeMsg.getAutomationCompositionId());

        if (automationComposition == null) {
            LOGGER.debug("Automation composition {} does not use this participant",
                    stateChangeMsg.getAutomationCompositionId());
            return;
        }

        switch (stateChangeMsg.getLockOrderedState()) {
            case LOCK -> handleLockState(stateChangeMsg.getMessageId(), automationComposition,
                    stateChangeMsg.getStartPhase());
            case UNLOCK -> handleUnlockState(stateChangeMsg.getMessageId(), automationComposition,
                    stateChangeMsg.getStartPhase());
            default -> LOGGER.error("StateChange message has no lock order {}", automationComposition.getKey());
        }
    }

    private void handleLockState(UUID messageId, final AutomationComposition automationComposition,
                                 Integer startPhaseMsg) {
        automationComposition.setLockState(LockState.LOCKING);
        var serviceTemplateFragment = cacheProvider
                .getServiceTemplateFragmentMap().get(automationComposition.getCompositionId());
        for (var element : automationComposition.getElements().values()) {
            var compositionInProperties = cacheProvider
                    .getCommonProperties(automationComposition.getCompositionId(), element.getDefinition());
            int startPhase = ParticipantUtils.findStartPhase(compositionInProperties);
            if (startPhaseMsg.equals(startPhase)) {
                element.setLockState(LockState.LOCKING);
                element.setSubState(SubState.NONE);
                var compositionElement = cacheProvider.createCompositionElementDto(
                        automationComposition.getCompositionId(), element, compositionInProperties);
                var instanceElement = new InstanceElementDto(automationComposition.getInstanceId(), element.getId(),
                        serviceTemplateFragment, element.getProperties(), element.getOutProperties());
                listener.lock(messageId, compositionElement, instanceElement);
            }
        }
    }

    private void handleUnlockState(UUID messageId, final AutomationComposition automationComposition,
                                   Integer startPhaseMsg) {
        automationComposition.setLockState(LockState.UNLOCKING);
        var serviceTemplateFragment = cacheProvider
                .getServiceTemplateFragmentMap().get(automationComposition.getCompositionId());
        for (var element : automationComposition.getElements().values()) {
            var compositionInProperties = cacheProvider
                    .getCommonProperties(automationComposition.getCompositionId(), element.getDefinition());
            int startPhase = ParticipantUtils.findStartPhase(compositionInProperties);
            if (startPhaseMsg.equals(startPhase)) {
                element.setLockState(LockState.UNLOCKING);
                element.setSubState(SubState.NONE);
                var compositionElement = cacheProvider.createCompositionElementDto(
                        automationComposition.getCompositionId(), element, compositionInProperties);
                var instanceElement = new InstanceElementDto(automationComposition.getInstanceId(), element.getId(),
                        serviceTemplateFragment, element.getProperties(), element.getOutProperties());
                listener.unlock(messageId, compositionElement, instanceElement);
            }
        }
    }
}
