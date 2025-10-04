/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.ElementState;
import org.onap.policy.clamp.acm.participant.intermediary.api.InstanceElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.acm.participant.intermediary.handler.cache.AcDefinition;
import org.onap.policy.clamp.acm.participant.intermediary.handler.cache.CacheProvider;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.MigrationState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDeploy;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUtils;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeploy;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeployAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionMigration;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionStateChange;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.PropertiesUpdate;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/*
 * This class is responsible for managing the state of all automation compositions in the participant.
 */
@Component
@RequiredArgsConstructor
public class AutomationCompositionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutomationCompositionHandler.class);
    private static final String AC_NOT_USED = "Automation composition {} does not use this participant";

    private final CacheProvider cacheProvider;
    private final ParticipantMessagePublisher publisher;
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
            if (DeployOrder.DELETE.equals(stateChangeMsg.getDeployOrderedState())) {
                var automationCompositionAck = new AutomationCompositionDeployAck(
                        ParticipantMessageType.AUTOMATION_COMPOSITION_STATECHANGE_ACK);
                automationCompositionAck.setParticipantId(cacheProvider.getParticipantId());
                automationCompositionAck.setReplicaId(cacheProvider.getReplicaId());
                automationCompositionAck.setMessage("Already deleted or never used");
                automationCompositionAck.setResult(true);
                automationCompositionAck.setStateChangeResult(StateChangeResult.NO_ERROR);
                automationCompositionAck.setResponseTo(stateChangeMsg.getMessageId());
                automationCompositionAck.setAutomationCompositionId(stateChangeMsg.getAutomationCompositionId());
                publisher.sendAutomationCompositionAck(automationCompositionAck);
            } else {
                LOGGER.warn(AC_NOT_USED, stateChangeMsg.getAutomationCompositionId());
            }
            return;
        }

        switch (stateChangeMsg.getDeployOrderedState()) {
            case UNDEPLOY -> handleUndeployState(stateChangeMsg.getMessageId(), automationComposition,
                    stateChangeMsg.getStartPhase());
            case DELETE -> handleDeleteState(stateChangeMsg.getMessageId(), automationComposition,
                    stateChangeMsg.getStartPhase());
            default ->
                    LOGGER.error("StateChange message has no state, state is null {}", automationComposition.getKey());
        }
    }

    /**
     * Handle a automation composition properties update message.
     *
     * @param updateMsg the properties update message
     */
    public void handleAcPropertyUpdate(PropertiesUpdate updateMsg) {

        if (updateMsg.getParticipantUpdatesList().isEmpty()) {
            LOGGER.warn("No AutomationCompositionElement updates in message {}",
                    updateMsg.getAutomationCompositionId());
            return;
        }

        for (var participantDeploy : updateMsg.getParticipantUpdatesList()) {
            if (cacheProvider.getParticipantId().equals(participantDeploy.getParticipantId())) {
                var automationComposition =
                        cacheProvider.getAutomationComposition(updateMsg.getAutomationCompositionId());
                automationComposition.setDeployState(DeployState.UPDATING);
                var acCopy = new AutomationComposition(automationComposition);
                updateExistingElementsOnThisParticipant(updateMsg.getAutomationCompositionId(), participantDeploy);

                callParticipantUpdateProperty(updateMsg.getMessageId(), participantDeploy.getAcElementList(), acCopy);
            }
        }
    }

    /**
     * Handle a automation composition Deploy message.
     *
     * @param deployMsg the Deploy message
     */
    public void handleAutomationCompositionDeploy(AutomationCompositionDeploy deployMsg) {

        if (deployMsg.getParticipantUpdatesList().isEmpty()) {
            LOGGER.warn("No AutomationCompositionElement deploy in message {}", deployMsg.getAutomationCompositionId());
            return;
        }

        for (var participantDeploy : deployMsg.getParticipantUpdatesList()) {
            if (cacheProvider.getParticipantId().equals(participantDeploy.getParticipantId())) {
                if (deployMsg.isFirstStartPhase()) {
                    cacheProvider.initializeAutomationComposition(deployMsg.getCompositionId(),
                            deployMsg.getAutomationCompositionId(), participantDeploy,
                            deployMsg.getRevisionIdInstance());
                }
                callParticipantDeploy(deployMsg.getMessageId(), participantDeploy.getAcElementList(),
                        deployMsg.getStartPhase(), deployMsg.getAutomationCompositionId());
            }
        }
    }

    private void callParticipantDeploy(UUID messageId, List<AcElementDeploy> acElementDeployList, Integer startPhaseMsg,
            UUID instanceId) {
        var automationComposition = cacheProvider.getAutomationComposition(instanceId);
        automationComposition.setDeployState(DeployState.DEPLOYING);
        for (var elementDeploy : acElementDeployList) {
            var element = automationComposition.getElements().get(elementDeploy.getId());
            var compositionInProperties = cacheProvider.getCommonProperties(automationComposition.getCompositionId(),
                    element.getDefinition());
            int startPhase = ParticipantUtils.findStartPhase(compositionInProperties);
            if (startPhaseMsg.equals(startPhase)) {
                var compositionElement =
                        cacheProvider.createCompositionElementDto(automationComposition.getCompositionId(), element);
                var instanceElement =
                        new InstanceElementDto(instanceId, elementDeploy.getId(), elementDeploy.getProperties(),
                                element.getOutProperties());
                listener.deploy(messageId, compositionElement, instanceElement);
            }
        }
    }

    private void callParticipantUpdateProperty(UUID messageId, List<AcElementDeploy> acElements,
            AutomationComposition acCopy) {
        var instanceElementDtoMap = cacheProvider.getInstanceElementDtoMap(acCopy);
        var instanceElementDtoMapUpdated =
                cacheProvider.getInstanceElementDtoMap(cacheProvider.getAutomationComposition(acCopy.getInstanceId()));
        var compositionElementDtoMap = cacheProvider.getCompositionElementDtoMap(acCopy);
        for (var acElement : acElements) {
            listener.update(messageId, compositionElementDtoMap.get(acElement.getId()),
                    instanceElementDtoMap.get(acElement.getId()), instanceElementDtoMapUpdated.get(acElement.getId()));
        }
    }

    private void migrateExistingElementsOnThisParticipant(AutomationComposition automationComposition,
                                                          UUID compositionTargetId, ParticipantDeploy participantDeploy,
                                                          int stage, boolean newParticipant) {
        for (var element : participantDeploy.getAcElementList()) {
            UUID compIdForCommonProperties = null;
            if (MigrationState.REMOVED.equals(element.getMigrationState())) {
                compIdForCommonProperties = automationComposition.getCompositionId();
            } else {
                compIdForCommonProperties = compositionTargetId;
            }
            var compositionInProperties =
                    cacheProvider.getCommonProperties(compIdForCommonProperties, element.getDefinition());
            var stageSet = ParticipantUtils.findStageSetMigrate(compositionInProperties);
            if (MigrationState.REMOVED.equals(element.getMigrationState())) {
                stageSet = Set.of(0);
            }
            if (stageSet.contains(stage)) {
                migrateElement(element, automationComposition, compositionTargetId, stage, newParticipant,
                        participantDeploy);
            }
        }
    }

    private void migrateElement(AcElementDeploy element, AutomationComposition automationComposition,
                                UUID compositionTargetId, int stage, boolean newParticipant,
                                ParticipantDeploy participantDeploy) {
        var acElementList = automationComposition.getElements();
        automationComposition.setCompositionTargetId(compositionTargetId);
        automationComposition.setDeployState(DeployState.MIGRATING);
        var acElement = acElementList.get(element.getId());
        if (acElement == null) {  // NEW element with existing participant
            var newElement = CacheProvider.createAutomationCompositionElement(element);
            newElement.setParticipantId(participantDeploy.getParticipantId());
            newElement.setDeployState(DeployState.MIGRATING);
            newElement.setLockState(LockState.LOCKED);
            newElement.setStage(stage);
            newElement.setMigrationState(MigrationState.NEW);

            acElementList.put(element.getId(), newElement);
            LOGGER.info("New Ac Element with id {} is added in Migration", element.getId());
        } else {
            acElement.setStage(stage);
            acElement.setMigrationState(element.getMigrationState());
            if (! newParticipant) { //DEFAULT element
                AcmUtils.recursiveMerge(acElement.getProperties(), element.getProperties());
                acElement.setDeployState(DeployState.MIGRATING);
                acElement.setDefinition(element.getDefinition());
            }
            LOGGER.info("Cache updated for the migration of element with id {}", element.getId());
        }

    }

    private void updateExistingElementsOnThisParticipant(UUID instanceId, ParticipantDeploy participantDeploy) {
        var acElementList = cacheProvider.getAutomationComposition(instanceId).getElements();
        for (var element : participantDeploy.getAcElementList()) {
            var acElement = acElementList.get(element.getId());
            AcmUtils.recursiveMerge(acElement.getProperties(), element.getProperties());
            acElement.setDeployState(DeployState.UPDATING);
            acElement.setSubState(SubState.NONE);
            acElement.setDefinition(element.getDefinition());
        }
    }

    /**
     * Method to handle when the new state from participant is UNINITIALISED state.
     *
     * @param messageId             the messageId
     * @param automationComposition participant response
     * @param startPhaseMsg         startPhase from message
     */
    private void handleUndeployState(UUID messageId, final AutomationComposition automationComposition,
            Integer startPhaseMsg) {
        automationComposition.setDeployState(DeployState.UNDEPLOYING);
        for (var element : automationComposition.getElements().values()) {
            UUID compositionId = null;
            if (MigrationState.NEW.equals(element.getMigrationState())) {
                compositionId = automationComposition.getCompositionTargetId();
            } else {
                compositionId = automationComposition.getCompositionId();
            }
            var compositionInProperties = cacheProvider.getCommonProperties(compositionId, element.getDefinition());
            int startPhase = ParticipantUtils.findStartPhase(compositionInProperties);
            if (MigrationState.NEW.equals(element.getMigrationState())) {
                // Undeploy newly added element on a Failed Migration
                startPhase = 0;
            }
            if (startPhaseMsg.equals(startPhase)) {
                element.setDeployState(DeployState.UNDEPLOYING);
                var compositionElement =
                        cacheProvider.createCompositionElementDto(compositionId, element);
                var instanceElement = new InstanceElementDto(automationComposition.getInstanceId(), element.getId(),
                        element.getProperties(), element.getOutProperties());
                listener.undeploy(messageId, compositionElement, instanceElement);
            }
        }
    }

    private void handleDeleteState(UUID messageId, final AutomationComposition automationComposition,
            Integer startPhaseMsg) {
        automationComposition.setDeployState(DeployState.DELETING);
        for (var element : automationComposition.getElements().values()) {
            var compositionInProperties = cacheProvider.getCommonProperties(automationComposition.getCompositionId(),
                    element.getDefinition());
            int startPhase = ParticipantUtils.findStartPhase(compositionInProperties);
            if (startPhaseMsg.equals(startPhase)) {
                element.setDeployState(DeployState.DELETING);
                element.setSubState(SubState.NONE);
                var compositionElement =
                        cacheProvider.createCompositionElementDto(automationComposition.getCompositionId(), element);
                var instanceElement = new InstanceElementDto(automationComposition.getInstanceId(), element.getId(),
                        element.getProperties(), element.getOutProperties());
                listener.delete(messageId, compositionElement, instanceElement);
            }
        }
    }

    /**
     * Handles AutomationComposition Migration.
     *
     * @param migrationMsg the AutomationCompositionMigration
     */
    public void handleAutomationCompositionMigration(AutomationCompositionMigration migrationMsg) {
        if (migrationMsg.getAutomationCompositionId() == null || migrationMsg.getCompositionTargetId() == null) {
            return;
        }
        var automationComposition = cacheProvider.getAutomationComposition(migrationMsg.getAutomationCompositionId());
        var acTargetDefinition = cacheProvider.getAcElementsDefinitions().get(migrationMsg.getCompositionTargetId());
        if (Boolean.FALSE.equals(migrationMsg.getRollback())) {
            handleMigration(automationComposition, acTargetDefinition, migrationMsg);
        } else {
            handleRollback(automationComposition, migrationMsg);
        }
    }

    private void handleRollback(AutomationComposition automationComposition,
                                AutomationCompositionMigration migrationMsg) {
        AutomationComposition acCopy = null;
        if (automationComposition == null) {
            LOGGER.warn(AC_NOT_USED, migrationMsg.getAutomationCompositionId());
            return;
        } else {
            LOGGER.info("Rollback operation invoked for the instance {}", migrationMsg.getAutomationCompositionId());
            acCopy = new AutomationComposition(automationComposition);
            automationComposition.setCompositionTargetId(migrationMsg.getCompositionTargetId());
            automationComposition.setDeployState(DeployState.MIGRATION_REVERTING);
        }
        for (var participantDeploy : migrationMsg.getParticipantUpdatesList()) {
            if (cacheProvider.getParticipantId().equals(participantDeploy.getParticipantId())) {
                migrateExistingElementsOnThisParticipant(automationComposition, migrationMsg.getCompositionTargetId(),
                        participantDeploy, migrationMsg.getStage(), false);

                callParticipantMigrate(migrationMsg, participantDeploy.getAcElementList(), acCopy);
            }

        }
    }

    private void handleMigration(AutomationComposition automationComposition, AcDefinition acTargetDefinition,
                                 AutomationCompositionMigration migrationMsg) {
        AutomationComposition acCopy = null;
        if (automationComposition == null) {
            if (acTargetDefinition == null) {
                LOGGER.warn(AC_NOT_USED, migrationMsg.getAutomationCompositionId());
                return;
            }
        } else {
            LOGGER.info("Migration invoked on an existing participant for the instance {}",
                    migrationMsg.getAutomationCompositionId());
            acCopy = new AutomationComposition(automationComposition);
        }
        var newParticipant = false;
        for (var participantDeploy : migrationMsg.getParticipantUpdatesList()) {
            if (cacheProvider.getParticipantId().equals(participantDeploy.getParticipantId())) {
                if (automationComposition == null) {
                    // New element with new participant added in Migration
                    LOGGER.info("Participant newly added in Migration for the instance {}",
                            migrationMsg.getAutomationCompositionId());
                    newParticipant = true;
                    cacheProvider.initializeAutomationComposition(migrationMsg.getCompositionId(),
                            migrationMsg.getCompositionTargetId(), migrationMsg.getAutomationCompositionId(),
                            participantDeploy, DeployState.MIGRATING, SubState.NONE,
                            migrationMsg.getRevisionIdInstance());
                    automationComposition = cacheProvider
                            .getAutomationComposition(migrationMsg.getAutomationCompositionId());
                }
                migrateExistingElementsOnThisParticipant(automationComposition, migrationMsg.getCompositionTargetId(),
                        participantDeploy, migrationMsg.getStage(), newParticipant);

                callParticipantMigrate(migrationMsg, participantDeploy.getAcElementList(), acCopy);
            }
        }
    }

    private void callParticipantMigrate(AutomationCompositionMigration migrationMsg, List<AcElementDeploy> acElements,
                                        AutomationComposition formerAcInstance) {
        var latestAcFromCache = cacheProvider.getAutomationComposition(migrationMsg.getAutomationCompositionId());
        var instanceElementTargetMap = cacheProvider.getInstanceElementDtoMap(latestAcFromCache);
        var compositionElementTargetMap = cacheProvider.getCompositionElementDtoMap(latestAcFromCache,
                migrationMsg.getCompositionTargetId());
        Map<UUID, CompositionElementDto> compositionElementMap = new HashMap<>();
        Map<UUID, InstanceElementDto> instanceElementMap = new HashMap<>();
        if (formerAcInstance != null) { //Existing participant
            compositionElementMap = cacheProvider.getCompositionElementDtoMap(formerAcInstance);
            instanceElementMap = cacheProvider.getInstanceElementDtoMap(formerAcInstance);
        }
        // Call migrate for new and existing elements
        for (var acElement : acElements) {
            UUID compIdForCommonProperties = null;
            if (MigrationState.REMOVED.equals(acElement.getMigrationState())) {
                compIdForCommonProperties = latestAcFromCache.getCompositionId();
            } else {
                compIdForCommonProperties = migrationMsg.getCompositionTargetId();
            }
            var compositionInProperties =
                    cacheProvider.getCommonProperties(compIdForCommonProperties, acElement.getDefinition());
            var stageSet = ParticipantUtils.findStageSetMigrate(compositionInProperties);
            if (MigrationState.REMOVED.equals(acElement.getMigrationState())) {
                stageSet = Set.of(0);
            }
            var rollback = Boolean.TRUE.equals(migrationMsg.getRollback());
            if (stageSet.contains(migrationMsg.getStage())) {
                if (MigrationState.NEW.equals(acElement.getMigrationState())) {
                    var compositionElementDto = new CompositionElementDto(migrationMsg.getCompositionId(),
                            acElement.getDefinition(), Map.of(), Map.of(), ElementState.NOT_PRESENT);
                    var instanceElementDto = new InstanceElementDto(migrationMsg.getAutomationCompositionId(),
                            acElement.getId(), Map.of(), Map.of(), ElementState.NOT_PRESENT);
                    var compositionElementTargetDto =
                            CacheProvider.changeStateToNew(compositionElementTargetMap.get(acElement.getId()));
                    var instanceElementTargetDto =
                            CacheProvider.changeStateToNew(instanceElementTargetMap.get(acElement.getId()));

                    listenerMigrate(migrationMsg.getMessageId(), compositionElementDto, compositionElementTargetDto,
                            instanceElementDto, instanceElementTargetDto, migrationMsg.getStage(), rollback);

                } else if (MigrationState.REMOVED.equals(acElement.getMigrationState())) {
                    var compositionDtoTarget = new CompositionElementDto(migrationMsg.getCompositionTargetId(),
                            acElement.getDefinition(), Map.of(), Map.of(), ElementState.REMOVED);
                    var instanceElementDtoTarget = new InstanceElementDto(migrationMsg.getAutomationCompositionId(),
                            acElement.getId(), Map.of(), Map.of(), ElementState.REMOVED);
                    listenerMigrate(migrationMsg.getMessageId(), compositionElementMap.get(acElement.getId()),
                            compositionDtoTarget, instanceElementMap.get(acElement.getId()), instanceElementDtoTarget,
                            migrationMsg.getStage(), rollback);

                } else { // DEFAULT case
                    listenerMigrate(migrationMsg.getMessageId(), compositionElementMap.get(acElement.getId()),
                            compositionElementTargetMap.get(acElement.getId()),
                            instanceElementMap.get(acElement.getId()), instanceElementTargetMap.get(acElement.getId()),
                            migrationMsg.getStage(), rollback);
                }
            }
        }
    }

    private void listenerMigrate(UUID messageId, CompositionElementDto compositionElement,
            CompositionElementDto compositionElementTarget, InstanceElementDto instanceElement,
            InstanceElementDto instanceElementMigrate, int stage, boolean rollback) {
        if (rollback) {
            listener.rollback(messageId, compositionElement, compositionElementTarget, instanceElement,
                    instanceElementMigrate, stage);
        } else {
            LOGGER.info("Invoking migration of element on the participant for {}", instanceElement.elementId());
            listener.migrate(messageId, compositionElement, compositionElementTarget, instanceElement,
                    instanceElementMigrate, stage);
        }
    }
}
