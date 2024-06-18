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
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantSyncPublisher;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantReplica;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantSupportedElementType;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantDeregister;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantRegister;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatus;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.models.acm.utils.TimestampHelper;
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
    private final ParticipantSyncPublisher participantSyncPublisher;
    private final AcRuntimeParameterGroup acRuntimeParameterGroup;

    /**
     * Handle a ParticipantRegister message from a participant.
     *
     * @param participantRegisterMsg the ParticipantRegister message received from a participant
     */
    @Timed(value = "listener.participant_register", description = "PARTICIPANT_REGISTER messages received")
    public void handleParticipantMessage(ParticipantRegister participantRegisterMsg) {
        saveIfNotPresent(participantRegisterMsg.getReplicaId(),
                participantRegisterMsg.getParticipantId(),
                participantRegisterMsg.getParticipantSupportedElementType(), true);

        participantRegisterAckPublisher.send(participantRegisterMsg.getMessageId(),
                participantRegisterMsg.getParticipantId());
    }

    /**
     * Handle a ParticipantDeregister message from a participant.
     *
     * @param participantDeregisterMsg the ParticipantDeregister message received from a participant
     */
    @Timed(value = "listener.participant_deregister", description = "PARTICIPANT_DEREGISTER messages received")
    public void handleParticipantMessage(ParticipantDeregister participantDeregisterMsg) {
        var replicaId = participantDeregisterMsg.getReplicaId() != null
                ? participantDeregisterMsg.getReplicaId() : participantDeregisterMsg.getParticipantId();
        var replicaOpt = participantProvider.findParticipantReplica(replicaId);
        if (replicaOpt.isPresent()) {
            participantProvider.deleteParticipantReplica(replicaId);
        }

        participantDeregisterAckPublisher.send(participantDeregisterMsg.getMessageId());
    }

    /**
     * Handle a ParticipantStatus message from a participant.
     *
     * @param participantStatusMsg the ParticipantStatus message received from a participant
     */
    @Timed(value = "listener.participant_status", description = "PARTICIPANT_STATUS messages received")
    public void handleParticipantMessage(ParticipantStatus participantStatusMsg) {
        saveIfNotPresent(participantStatusMsg.getReplicaId(), participantStatusMsg.getParticipantId(),
                participantStatusMsg.getParticipantSupportedElementType(), false);

        if (!participantStatusMsg.getAutomationCompositionInfoList().isEmpty()) {
            automationCompositionProvider.upgradeStates(participantStatusMsg.getAutomationCompositionInfoList());
        }
        if (!participantStatusMsg.getParticipantDefinitionUpdates().isEmpty()
                && participantStatusMsg.getCompositionId() != null) {
            updateAcDefinitionOutProperties(participantStatusMsg.getCompositionId(),
                participantStatusMsg.getReplicaId(), participantStatusMsg.getParticipantDefinitionUpdates());
        }
    }

    private void saveIfNotPresent(UUID msgReplicaId, UUID participantId,
            List<ParticipantSupportedElementType> participantSupportedElementType, boolean registration) {
        var replicaId = msgReplicaId != null ? msgReplicaId : participantId;
        var replicaOpt = participantProvider.findParticipantReplica(replicaId);
        if (replicaOpt.isPresent()) {
            var replica = replicaOpt.get();
            checkOnline(replica);
        } else {
            var participant = getParticipant(participantId, listToMap(participantSupportedElementType));
            participant.getReplicas().put(replicaId, createReplica(replicaId));
            participantProvider.saveParticipant(participant);
        }
        if (registration) {
            handleRestart(participantId, replicaId);
        }
    }

    private Participant getParticipant(UUID participantId,
            Map<UUID, ParticipantSupportedElementType> participantSupportedElementType) {
        var participantOpt = participantProvider.findParticipant(participantId);
        return participantOpt.orElseGet(() -> createParticipant(participantId, participantSupportedElementType));
    }

    private ParticipantReplica createReplica(UUID replicaId) {
        var replica = new ParticipantReplica();
        replica.setReplicaId(replicaId);
        replica.setParticipantState(ParticipantState.ON_LINE);
        replica.setLastMsg(TimestampHelper.now());
        return replica;

    }

    private void updateAcDefinitionOutProperties(UUID compositionId, UUID replicaId, List<ParticipantDefinition> list) {
        var acDefinitionOpt = acDefinitionProvider.findAcDefinition(compositionId);
        if (acDefinitionOpt.isEmpty()) {
            LOGGER.error("Ac Definition with id {} not found", compositionId);
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
        participantSyncPublisher.sendSync(acDefinition, replicaId);
    }

    private void checkOnline(ParticipantReplica replica) {
        if (ParticipantState.OFF_LINE.equals(replica.getParticipantState())) {
            replica.setParticipantState(ParticipantState.ON_LINE);
        }
        replica.setLastMsg(TimestampHelper.now());
        participantProvider.saveParticipantReplica(replica);
    }

    private void handleRestart(UUID participantId, UUID replicaId) {
        var compositionIds = participantProvider.getCompositionIds(participantId);
        var oldParticipant = participantId.equals(replicaId);
        for (var compositionId : compositionIds) {
            var acDefinition = acDefinitionProvider.getAcDefinition(compositionId);
            LOGGER.debug("Scan Composition {} for restart", acDefinition.getCompositionId());
            if (oldParticipant) {
                handleRestart(participantId, acDefinition);
            } else {
                handleSyncRestart(participantId, replicaId, acDefinition);
            }
        }
    }

    private void handleRestart(final UUID participantId, AutomationCompositionDefinition acDefinition) {
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
        // expected final state
        if (StateChangeResult.TIMEOUT.equals(acDefinition.getStateChangeResult())) {
            acDefinition.setStateChangeResult(StateChangeResult.NO_ERROR);
        }
        acDefinition.setRestarting(true);
        acDefinitionProvider.updateAcDefinition(acDefinition,
                acRuntimeParameterGroup.getAcmParameters().getToscaCompositionName());

        var automationCompositionList =
                automationCompositionProvider.getAcInstancesByCompositionId(acDefinition.getCompositionId());
        var automationCompositions = automationCompositionList.stream()
                .filter(ac -> isAcToBeRestarted(participantId, ac)).toList();
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

    private void handleSyncRestart(final UUID participantId, UUID replicaId,
            AutomationCompositionDefinition acDefinition) {
        if (AcTypeState.COMMISSIONED.equals(acDefinition.getState())) {
            LOGGER.debug("Composition {} COMMISSIONED", acDefinition.getCompositionId());
            return;
        }
        LOGGER.debug("Composition to be send in Restart message {}", acDefinition.getCompositionId());
        var automationCompositionList =
                automationCompositionProvider.getAcInstancesByCompositionId(acDefinition.getCompositionId());
        var automationCompositions = automationCompositionList.stream()
                .filter(ac -> isAcToBeSyncRestarted(participantId, ac)).toList();
        participantSyncPublisher.sendRestartMsg(participantId, replicaId, acDefinition, automationCompositions);
    }

    private boolean isAcToBeSyncRestarted(UUID participantId, AutomationComposition automationComposition) {
        for (var element : automationComposition.getElements().values()) {
            if (participantId.equals(element.getParticipantId())) {
                return true;
            }
        }
        return false;
    }

    private Participant createParticipant(UUID participantId,
            Map<UUID, ParticipantSupportedElementType> participantSupportedElementType) {
        var participant = new Participant();
        participant.setParticipantId(participantId);
        participant.setParticipantSupportedElementTypes(participantSupportedElementType);
        return participant;
    }

    private Map<UUID, ParticipantSupportedElementType> listToMap(List<ParticipantSupportedElementType> elementList) {
        Map<UUID, ParticipantSupportedElementType> map = new HashMap<>();
        MapUtils.populateMap(map, elementList, ParticipantSupportedElementType::getId);
        return map;
    }
}
