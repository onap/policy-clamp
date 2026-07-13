/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2026 OpenInfra Foundation Europe. All rights reserved.
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
import jakarta.validation.ValidationException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.onap.policy.clamp.acm.runtime.main.utils.EncryptionUtils;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantDeregisterAckPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantRegisterAckPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantSyncPublisher;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.NodeTemplateState;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.concepts.ParticipantReplica;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantSupportedElementType;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantDeregister;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantRegister;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantReqSync;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatus;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.MessageProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.models.acm.utils.AcmStageUtils;
import org.onap.policy.clamp.models.acm.utils.TimestampHelper;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
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
    private final ParticipantSyncPublisher participantSyncPublisher;
    private final MessageProvider messageProvider;
    private final EncryptionUtils encryptionUtils;

    private void validation(UUID participantId,
            List<ParticipantSupportedElementType> participantSupportedElementTypes) {
        var map = participantProvider.getSupportedElementMap();
        for (var element : participantSupportedElementTypes) {
            var supportedElement = new ToscaConceptIdentifier(element.getTypeName(), element.getTypeVersion());
            var result = map.get(supportedElement);
            if (result != null && !result.equals(participantId)) {
                var msg = "The supplied " + supportedElement + " is already used by " + result;
                throw new ValidationException(msg);
            }
        }
    }

    /**
     * Handle a ParticipantRegister message from a participant.
     *
     * @param participantRegisterMsg the ParticipantRegister message received from a participant
     */
    @Timed(value = "listener.participant_register", description = "PARTICIPANT_REGISTER messages received")
    public void handleParticipantMessage(ParticipantRegister participantRegisterMsg) {
        validation(participantRegisterMsg.getParticipantId(),
                participantRegisterMsg.getParticipantSupportedElementType());
        saveIfNotPresent(participantRegisterMsg.getReplicaId(), participantRegisterMsg.getParticipantId(),
                participantRegisterMsg.getParticipantSupportedElementType());

        participantRegisterAckPublisher.send(participantRegisterMsg.getMessageId(),
                participantRegisterMsg.getParticipantId(), participantRegisterMsg.getReplicaId());
    }

    /**
     * Handle a ParticipantDeregister message from a participant.
     *
     * @param participantDeregisterMsg the ParticipantDeregister message received from a participant
     */
    @Timed(value = "listener.participant_deregister", description = "PARTICIPANT_DEREGISTER messages received")
    public void handleParticipantMessage(ParticipantDeregister participantDeregisterMsg) {
        var replicaId = participantDeregisterMsg.getReplicaId();
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
    @MessageIntercept
    @Timed(value = "listener.participant_status", description = "PARTICIPANT_STATUS messages received")
    public void handleParticipantMessage(ParticipantStatus participantStatusMsg) {
        if (!participantStatusMsg.getAutomationCompositionInfoList().isEmpty()) {
            messageProvider.saveInstanceOutProperties(participantStatusMsg);
        }
        if (!participantStatusMsg.getParticipantDefinitionUpdates().isEmpty()
                && participantStatusMsg.getCompositionId() != null) {
            var acDefinition = acDefinitionProvider.findAcDefinition(participantStatusMsg.getCompositionId());
            if (acDefinition.isPresent()) {
                var map = acDefinition.get().getElementStateMap().values().stream()
                        .collect(Collectors.toMap(NodeTemplateState::getNodeTemplateId, UnaryOperator.identity()));
                messageProvider.saveCompositionOutProperties(participantStatusMsg, map);
            } else {
                LOGGER.error("Not valid ParticipantStatus message");
            }
        }
        saveIfNotPresent(participantStatusMsg.getReplicaId(), participantStatusMsg.getParticipantId(),
                participantStatusMsg.getParticipantSupportedElementType());
    }

    private void saveIfNotPresent(UUID replicaId, UUID participantId,
            List<ParticipantSupportedElementType> participantSupportedElementType) {
        var participant = getParticipant(participantId, listToMap(participantSupportedElementType));
        var replica = participant.getReplicas().get(replicaId);
        if (replica != null) {
            updateTimestamp(replica);
        } else {
            participant.getReplicas().put(replicaId, createReplica(replicaId));
        }
        var toRestart = replica == null;
        if (!CollectionUtils.isEqualCollection(
                participant.getParticipantSupportedElementTypes().values()
                        .stream().map(this::toToscaConceptIdentifier).toList(),
                participantSupportedElementType.stream().map(this::toToscaConceptIdentifier).toList())) {
            participant.setParticipantSupportedElementTypes(listToMap(participantSupportedElementType));
            toRestart = true;
        }
        if (toRestart) {
            participantProvider.saveParticipant(participant);
            handleRestart(participantId, replicaId);
        }
    }

    private PfConceptKey toToscaConceptIdentifier(ParticipantSupportedElementType elementType) {
        return new PfConceptKey(elementType.getTypeName(), elementType.getTypeVersion());
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

    private void updateTimestamp(ParticipantReplica replica) {
        replica.setLastMsg(TimestampHelper.now());
        participantProvider.saveParticipantReplica(replica);
    }

    /**
     * Handle restart of a participant.
     *
     * @param participantId     ID of the participant to restart
     * @param replicaId         ID of the participant replica
     */
    public void handleRestart(UUID participantId, UUID replicaId) {
        var compositionIds = participantProvider.getCompositionIds(participantId);
        for (var compositionId : compositionIds) {
            var acDefinition = acDefinitionProvider.getAcDefinition(compositionId);
            LOGGER.debug("Scan Composition {} for restart", acDefinition.getCompositionId());
            handleSyncRestart(participantId, replicaId, acDefinition);
        }
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
        encryptionUtils.decryptInstanceProperties(automationCompositionList);
        var automationCompositions =
                automationCompositionList.stream().filter(ac -> isAcToBeSyncRestarted(participantId, ac)).toList();
        participantSyncPublisher.sendRestartMsg(participantId, replicaId, acDefinition, automationCompositions);
    }

    /**
     * Handle restart of all participants.
     */
    public void handleRestartOfAllParticipants() {
        var participants = participantProvider.getParticipants();
        for (var participant:participants) {
            handleRestart(participant.getParticipantId(), null);
        }
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
        return elementList.stream()
                .collect(Collectors.toMap(ParticipantSupportedElementType::getId, UnaryOperator.identity()));
    }

    /**
     * Handle a participantReqSync message from a participant.
     *
     * @param participantReqSync the message received from a participant
     */
    @Timed(value = "listener.participant_req_sync", description = "PARTICIPANT_REQ_SYNC_MSG messages received")
    public void handleParticipantReqSync(ParticipantReqSync participantReqSync) {
        if (participantReqSync.getCompositionTargetId() != null) {
            // outdated Composition Target
            var acDefinition = acDefinitionProvider.getAcDefinition(participantReqSync.getCompositionTargetId());
            participantSyncPublisher.sendRestartMsg(participantReqSync.getParticipantId(), null,
                    acDefinition, List.of());
        }
        if (participantReqSync.getCompositionId() == null
                && participantReqSync.getAutomationCompositionId() != null) {
            // outdated AutomationComposition
            var automationComposition =
                    getAutomationCompositionForSync(participantReqSync.getAutomationCompositionId());
            participantSyncPublisher.sendSync(automationComposition);
        }
        if (participantReqSync.getCompositionId() != null) {
            // outdated Composition
            var acDefinition = acDefinitionProvider.getAcDefinition(participantReqSync.getCompositionId());
            var automationCompositions = participantReqSync.getAutomationCompositionId() != null
                    ? List.of(getAutomationCompositionForSync(participantReqSync.getAutomationCompositionId())) :
                    List.<AutomationComposition>of();
            participantSyncPublisher.sendRestartMsg(participantReqSync.getParticipantId(), null,
                    acDefinition, automationCompositions);
        }
    }

    private AutomationComposition getAutomationCompositionForSync(UUID automationCompositionId) {
        var automationComposition = automationCompositionProvider.getAutomationComposition(automationCompositionId);
        encryptionUtils.decryptInstanceProperties(automationComposition);
        if (DeployState.MIGRATING.equals(automationComposition.getDeployState())) {
            var acDefinition = acDefinitionProvider.getAcDefinition(automationComposition.getCompositionTargetId());
            var stage = AcmStageUtils.getFirstStage(automationComposition, acDefinition.getServiceTemplate());
            if (automationComposition.getPhase().equals(stage)) {
                // scenario first stage migration
                var rollback = automationCompositionProvider.getAutomationCompositionRollback(automationCompositionId);
                automationComposition.setElements(rollback.getElements().values().stream()
                    .collect(Collectors.toMap(AutomationCompositionElement::getId, AutomationCompositionElement::new)));
            }
        }
        return automationComposition;
    }
}
