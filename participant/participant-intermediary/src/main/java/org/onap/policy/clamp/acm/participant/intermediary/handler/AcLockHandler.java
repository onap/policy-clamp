/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2026 OpenInfra Foundation Europe. All rights reserved.
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

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.participant.intermediary.handler.cache.CacheProvider;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDto;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.clamp.models.acm.dto.InstanceElementDto;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionStateChange;
import org.onap.policy.clamp.models.acm.utils.AcmStageUtils;
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
                    stateChangeMsg.getStartPhase(), stateChangeMsg.getParticipantDtoList());
            case UNLOCK -> handleUnlockState(stateChangeMsg.getMessageId(), automationComposition,
                    stateChangeMsg.getStartPhase(), stateChangeMsg.getParticipantDtoList());
            default -> LOGGER.error("StateChange message has no lock order {}", automationComposition.getInstanceId());
        }
    }

    private void handleLockState(UUID messageId, final AutomationComposition automationComposition,
                                 Integer startPhaseMsg, List<ParticipantDto> participantDtoList) {
        automationComposition.setLockState(LockState.LOCKING);

        var elementDtoMap = ParticipantDtoUtils.getElementDtoMap(participantDtoList, cacheProvider.getParticipantId());

        for (var dto : elementDtoMap.values()) {
            var instanceElement = ParticipantDtoUtils.resolveInstanceElement(dto);
            int startPhase = AcmStageUtils.findStartPhase(dto.getCompositionElement().inProperties());
            if (startPhaseMsg.equals(startPhase)) {
                var element = automationComposition.getElements().get(instanceElement.elementId());
                if (element != null) {
                    element.setLockState(LockState.LOCKING);
                    element.setSubState(SubState.NONE);
                }
                listener.lock(messageId, dto.getCompositionElement(), instanceElement);
            }
        }
    }

    private void handleUnlockState(UUID messageId, final AutomationComposition automationComposition,
                                   Integer startPhaseMsg, List<ParticipantDto> participantDtoList) {
        automationComposition.setLockState(LockState.UNLOCKING);

        var elementDtoMap = ParticipantDtoUtils.getElementDtoMap(participantDtoList, cacheProvider.getParticipantId());

        for (var dto : elementDtoMap.values()) {
            var instanceElement = ParticipantDtoUtils.resolveInstanceElement(dto);
            int startPhase = AcmStageUtils.findStartPhase(dto.getCompositionElement().inProperties());
            if (startPhaseMsg.equals(startPhase)) {
                var element = automationComposition.getElements().get(instanceElement.elementId());
                if (element != null) {
                    element.setLockState(LockState.UNLOCKING);
                    element.setSubState(SubState.NONE);
                }
                listener.unlock(messageId, dto.getCompositionElement(), instanceElement);
            }
        }
    }
}
