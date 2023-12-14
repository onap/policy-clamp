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

package org.onap.policy.clamp.acm.runtime.supervision;

import io.micrometer.core.annotation.Timed;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.MapUtils;
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantDeregisterAckPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantRegisterAckPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantRestartPublisher;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantSupportedElementType;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantDeregister;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantRegister;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatus;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class handles supervision of participant status.
 */
@Component
@AllArgsConstructor
public class SupervisionParticipantHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SupervisionParticipantHandler.class);

    private final ParticipantProvider participantProvider;
    private final ParticipantRegisterAckPublisher participantRegisterAckPublisher;
    private final ParticipantDeregisterAckPublisher participantDeregisterAckPublisher;
    private final AutomationCompositionProvider automationCompositionProvider;
    private final AcDefinitionProvider acDefinitionProvider;
    private final ParticipantRestartPublisher participantRestartPublisher;
    private final AcRuntimeParameterGroup acRuntimeParameterGroup;

    /**
     * Handle a ParticipantRegister message from a participant.
     *
     * @param participantRegisterMsg the ParticipantRegister message received from a participant
     */
    @MessageIntercept
    @Timed(value = "listener.participant_register", description = "PARTICIPANT_REGISTER messages received")
    public void handleParticipantMessage(ParticipantRegister participantRegisterMsg) {
        var participantOpt = participantProvider.findParticipant(participantRegisterMsg.getParticipantId());

        if (participantOpt.isPresent()) {
            var participant = participantOpt.get();
            checkOnline(participant);
            handleRestart(participant.getParticipantId());
        } else {
            var participant = createParticipant(participantRegisterMsg.getParticipantId(),
                    listToMap(participantRegisterMsg.getParticipantSupportedElementType()));
            participantProvider.saveParticipant(participant);

        }

        participantRegisterAckPublisher.send(participantRegisterMsg.getMessageId(),
                participantRegisterMsg.getParticipantId());
    }

    /**
     * Handle a ParticipantDeregister message from a participant.
     *
     * @param participantDeregisterMsg the ParticipantDeregister message received from a participant
     */
    @MessageIntercept
    @Timed(value = "listener.participant_deregister", description = "PARTICIPANT_DEREGISTER messages received")
    public void handleParticipantMessage(ParticipantDeregister participantDeregisterMsg) {
        var participantOpt = participantProvider.findParticipant(participantDeregisterMsg.getParticipantId());

        if (participantOpt.isPresent()) {
            var participant = participantOpt.get();
            participant.setParticipantState(ParticipantState.OFF_LINE);
            participantProvider.updateParticipant(participant);
        }

        participantDeregisterAckPublisher.send(participantDeregisterMsg.getMessageId());
    }

    /**
     * Handle a ParticipantStatus message from a participant.
     *
     * @param participantStatusMsg the ParticipantStatus message received from a participant
     */
    @MessageIntercept
    @Timed(value = "listener.participant_status", description = "PARTICIPANT_STATUS messages received")
    public void handleParticipantMessage(ParticipantStatus participantStatusMsg) {

        var participantOpt = participantProvider.findParticipant(participantStatusMsg.getParticipantId());
        if (participantOpt.isEmpty()) {
            var participant = createParticipant(participantStatusMsg.getParticipantId(),
                    listToMap(participantStatusMsg.getParticipantSupportedElementType()));
            participantProvider.saveParticipant(participant);
        } else {
            checkOnline(participantOpt.get());
        }
        if (!participantStatusMsg.getAutomationCompositionInfoList().isEmpty()) {
            automationCompositionProvider.upgradeStates(participantStatusMsg.getAutomationCompositionInfoList());
        }
        if (!participantStatusMsg.getParticipantDefinitionUpdates().isEmpty()
                && participantStatusMsg.getCompositionId() != null) {
            updateAcDefinitionOutProperties(participantStatusMsg.getCompositionId(),
                    participantStatusMsg.getParticipantDefinitionUpdates());
        }
    }

    private void updateAcDefinitionOutProperties(UUID composotionId, List<ParticipantDefinition> list) {
        var acDefinitionOpt = acDefinitionProvider.findAcDefinition(composotionId);
        if (acDefinitionOpt.isEmpty()) {
            LOGGER.error("Ac Definition with id {} not found", composotionId);
            return;
        }
        var acDefinition = acDefinitionOpt.get();
        for (var acElements : list) {
            for (var element : acElements.getAutomationCompositionElementDefinitionList()) {
                var state = acDefinition.getElementStateMap().get(element.getAcElementDefinitionId().getName());
                if (state != null) {
                    state.setOutProperties(element.getOutProperties());
                }
            }
        }
        acDefinitionProvider.updateAcDefinition(acDefinition,
                acRuntimeParameterGroup.getAcmParameters().getToscaCompositionName());
    }

    private void checkOnline(Participant participant) {
        if (ParticipantState.OFF_LINE.equals(participant.getParticipantState())) {
            participant.setParticipantState(ParticipantState.ON_LINE);
            participantProvider.saveParticipant(participant);
        }
    }

    private void handleRestart(UUID participantId) {
        var compositionIds = participantProvider.getCompositionIds(participantId);
        for (var compositionId : compositionIds) {
            var acDefinition = acDefinitionProvider.getAcDefinition(compositionId);
            LOGGER.debug("Scan Composition {} for restart", acDefinition.getCompositionId());
            handleRestart(participantId, acDefinition);
        }
    }

    private void handleRestart(UUID participantId, AutomationCompositionDefinition acDefinition) {
        if (AcTypeState.COMMISSIONED.equals(acDefinition.getState())) {
            LOGGER.debug("Composition {} COMMISSIONED", acDefinition.getCompositionId());
            return;
        }
        LOGGER.debug("Composition to be send in Restart message {}", acDefinition.getCompositionId());
        for (var elementState : acDefinition.getElementStateMap().values()) {
            if (participantId.equals(elementState.getParticipantId())) {
                elementState.setRestarting(true);
            }
        }
        var automationCompositionList =
                automationCompositionProvider.getAcInstancesByCompositionId(acDefinition.getCompositionId());
        List<AutomationComposition> automationCompositions = new ArrayList<>();
        for (var automationComposition : automationCompositionList) {
            if (isAcToBeRestarted(participantId, automationComposition)) {
                automationCompositions.add(automationComposition);
            }
        }
        // expected final state
        if (StateChangeResult.TIMEOUT.equals(acDefinition.getStateChangeResult())) {
            acDefinition.setStateChangeResult(StateChangeResult.NO_ERROR);
        }
        acDefinition.setRestarting(true);
        acDefinitionProvider.updateAcDefinition(acDefinition,
                acRuntimeParameterGroup.getAcmParameters().getToscaCompositionName());
        participantRestartPublisher.send(participantId, acDefinition, automationCompositions);
    }

    private boolean isAcToBeRestarted(UUID participantId, AutomationComposition automationComposition) {
        boolean toAdd = false;
        for (var element : automationComposition.getElements().values()) {
            if (participantId.equals(element.getParticipantId())) {
                element.setRestarting(true);
                toAdd = true;
            }
        }
        if (toAdd) {
            automationComposition.setRestarting(true);
            // expected final state
            if (StateChangeResult.TIMEOUT.equals(automationComposition.getStateChangeResult())) {
                automationComposition.setStateChangeResult(StateChangeResult.NO_ERROR);
            }
            automationCompositionProvider.updateAutomationComposition(automationComposition);
        }
        return toAdd;
    }

    private Participant createParticipant(UUID participantId,
            Map<UUID, ParticipantSupportedElementType> participantSupportedElementType) {
        var participant = new Participant();
        participant.setParticipantId(participantId);
        participant.setParticipantSupportedElementTypes(participantSupportedElementType);
        participant.setParticipantState(ParticipantState.ON_LINE);
        return participant;
    }

    private Map<UUID, ParticipantSupportedElementType> listToMap(List<ParticipantSupportedElementType> elementList) {
        Map<UUID, ParticipantSupportedElementType> map = new HashMap<>();
        MapUtils.populateMap(map, elementList, ParticipantSupportedElementType::getId);
        return map;
    }
}
