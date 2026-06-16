/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2026 OpenInfra Foundation Europe. All rights reserved.
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.acm.participant.intermediary.handler.cache.CacheProvider;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.MigrationState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.clamp.models.acm.dto.AcElementDto;
import org.onap.policy.clamp.models.acm.dto.CompositionElementDto;
import org.onap.policy.clamp.models.acm.dto.ElementState;
import org.onap.policy.clamp.models.acm.dto.InstanceElementDto;
import org.onap.policy.clamp.models.acm.dto.ParticipantDto;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeploy;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeployAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionMigration;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionStateChange;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.PropertiesUpdate;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.utils.AcmStageUtils;
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
        var automationComposition = cacheProvider.getAutomationComposition(stateChangeMsg.getAutomationCompositionId());

        if (automationComposition == null) {
            if (DeployOrder.DELETE.equals(stateChangeMsg.getDeployOrderedState())) {
                var automationCompositionAck = new AutomationCompositionDeployAck(
                        ParticipantMessageType.AUTOMATION_COMPOSITION_STATECHANGE_ACK);
                automationCompositionAck.setParticipantId(cacheProvider.getParticipantId());
                automationCompositionAck.setReplicaId(cacheProvider.getReplicaId());
                automationCompositionAck.setMessage("Already deleted or never used");
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
                    stateChangeMsg.getStartPhase(), stateChangeMsg.getParticipantDtoList());
            case DELETE -> handleDeleteState(stateChangeMsg.getMessageId(), automationComposition,
                    stateChangeMsg.getStartPhase(), stateChangeMsg.getParticipantDtoList());
            default -> LOGGER.error(
                    "StateChange message has no state, state is null {}", automationComposition.getInstanceId());
        }
    }

    /**
     * Handle a automation composition properties update message.
     *
     * @param updateMsg the properties update message
     */
    public void handleAcPropertyUpdate(PropertiesUpdate updateMsg) {

        if (updateMsg.getParticipantDtoList().isEmpty()) {
            LOGGER.warn("No AutomationCompositionElement updates in message {}",
                    updateMsg.getAutomationCompositionId());
            return;
        }

        var automationComposition =
                cacheProvider.getAutomationComposition(updateMsg.getAutomationCompositionId());
        if (updateMsg.isRollback()) {
            automationComposition.setDeployState(DeployState.UPDATE_REVERTING);
        } else {
            automationComposition.setDeployState(DeployState.UPDATING);
        }

        var elementDtoMap = ParticipantDtoUtils.getElementDtoMap(
                updateMsg.getParticipantDtoList(), cacheProvider.getParticipantId());
        updateExistingElementsOnThisParticipant(updateMsg.getAutomationCompositionId(),
                elementDtoMap, updateMsg.isRollback());
        callParticipantUpdateProperty(updateMsg.getMessageId(), elementDtoMap);
    }

    /**
     * Handle a automation composition Deploy message.
     *
     * @param deployMsg the Deploy message
     */
    public void handleAutomationCompositionDeploy(AutomationCompositionDeploy deployMsg) {

        if (deployMsg.getParticipantDtoList().isEmpty()) {
            LOGGER.warn("No AutomationCompositionElement deploy in message {}", deployMsg.getAutomationCompositionId());
            return;
        }

        var elementDtoMap = ParticipantDtoUtils.getElementDtoMap(
                deployMsg.getParticipantDtoList(), cacheProvider.getParticipantId());

        if (deployMsg.isFirstStartPhase()) {
            cacheProvider.createAcInstance(deployMsg.getCompositionId(), null,
                    deployMsg.getAutomationCompositionId(), elementDtoMap,
                    DeployState.DEPLOYING, SubState.NONE, deployMsg.getRevisionIdInstance());
        }

        callParticipantDeploy(deployMsg.getMessageId(), deployMsg.getStartPhase(),
                deployMsg.getAutomationCompositionId(), deployMsg.getParticipantDtoList());
    }

    private void callParticipantDeploy(UUID messageId, Integer startPhaseMsg,
            UUID instanceId, List<ParticipantDto> participantDtoList) {
        var automationComposition = cacheProvider.getAutomationComposition(instanceId);
        automationComposition.setDeployState(DeployState.DEPLOYING);

        var elementDtoMap = ParticipantDtoUtils.getElementDtoMap(participantDtoList, cacheProvider.getParticipantId());

        for (var dto : elementDtoMap.values()) {
            int startPhase = AcmStageUtils.findStartPhase(dto.getCompositionElement().inProperties());
            if (startPhaseMsg.equals(startPhase)) {
                listener.deploy(messageId, dto.getCompositionElement(),
                        ParticipantDtoUtils.resolveInstanceElement(dto));
            }
        }
    }

    private void callParticipantUpdateProperty(UUID messageId, Map<UUID, AcElementDto> elementDtoMap) {
        for (var dto : elementDtoMap.values()) {
            listener.update(messageId, dto.getCompositionElement(),
                    dto.getInstanceElement(), dto.getInstanceElementTarget());
        }
    }

    private void migrateAutomationComposition(AutomationComposition automationComposition,
            Map<UUID, AcElementDto> elementDtoMap, int stage) {
        for (var dto : elementDtoMap.values()) {
            var stageSet = AcmStageUtils.findStageSetMigrate(
                    dto.getCompositionElementTarget().inProperties());
            if (stageSet.contains(stage)) {
                migrateElement(dto, automationComposition, stage);
            }
        }
    }

    private void rollbackAutomationComposition(AutomationComposition automationComposition,
            Map<UUID, AcElementDto> elementDtoMap, int stage, int defaultValue) {
        for (var dto : elementDtoMap.values()) {
            Set<Integer> stageSet;
            if (ElementState.NOT_PRESENT.equals(dto.getCompositionElement().state())) {
                stageSet = AcmStageUtils.findStageSetMigrate(dto.getCompositionElementTarget().inProperties());
            } else if (ElementState.REMOVED.equals(dto.getCompositionElementTarget().state())) {
                stageSet = Set.of(defaultValue);
            } else {
                stageSet = AcmStageUtils.findStageSetMigrate(
                        dto.getCompositionElement().inProperties());
            }
            if (stageSet.contains(stage)) {
                migrateElement(dto, automationComposition, stage);
            }
        }
    }

    private void migrateElement(AcElementDto dto, AutomationComposition automationComposition, int stage) {
        var elementId = dto.getInstanceElementTarget().elementId();
        var acElementList = automationComposition.getElements();
        var acElement = acElementList.get(elementId);
        if (acElement == null) {
            // NEW element
            var newElement = new AutomationCompositionElement();
            newElement.setId(elementId);
            newElement.setDefinition(dto.getCompositionElementTarget().elementDefinitionId());
            newElement.setParticipantId(cacheProvider.getParticipantId());
            newElement.setDeployState(automationComposition.getDeployState());
            newElement.setLockState(LockState.LOCKED);
            newElement.setStage(stage);
            newElement.setMigrationState(MigrationState.NEW);
            acElementList.put(elementId, newElement);
            LOGGER.info("New Ac Element with id {} is added in Migration", elementId);
        } else {
            acElement.setStage(stage);
            acElement.setDeployState(automationComposition.getDeployState());
            acElement.setDefinition(dto.getCompositionElementTarget().elementDefinitionId());
            if (ElementState.REMOVED.equals(dto.getCompositionElementTarget().state())) {
                acElement.setMigrationState(MigrationState.REMOVED);
            } else {
                acElement.setMigrationState(MigrationState.DEFAULT);
                AcmUtils.recursiveMerge(acElement.getProperties(),
                        dto.getInstanceElementTarget().inProperties());
            }
            LOGGER.info("Cache updated for the migration of element with id {}", elementId);
        }
    }

    private void updateExistingElementsOnThisParticipant(UUID instanceId,
            Map<UUID, AcElementDto> elementDtoMap, boolean rollback) {
        var acElementList = cacheProvider.getAutomationComposition(instanceId).getElements();
        for (var dto : elementDtoMap.values()) {
            var acElement = acElementList.get(dto.getInstanceElement().elementId());
            AcmUtils.recursiveMerge(acElement.getProperties(), dto.getInstanceElement().inProperties());
            if (rollback) {
                acElement.setDeployState(DeployState.UPDATE_REVERTING);
            } else {
                acElement.setDeployState(DeployState.UPDATING);
            }
            acElement.setSubState(SubState.NONE);
            acElement.setDefinition(dto.getCompositionElement().elementDefinitionId());
        }
    }

    /**
     * Method to handle when the new state from participant is UNINITIALISED state.
     *
     * @param messageId             the messageId
     * @param automationComposition participant response
     */
    private void handleUndeployState(UUID messageId, final AutomationComposition automationComposition,
            Integer startPhaseMsg, List<ParticipantDto> participantDtoList) {
        automationComposition.setDeployState(DeployState.UNDEPLOYING);

        var elementDtoMap = ParticipantDtoUtils.getElementDtoMap(participantDtoList, cacheProvider.getParticipantId());

        for (var dto : elementDtoMap.values()) {
            var instanceElement = ParticipantDtoUtils.resolveInstanceElement(dto);
            int startPhase = AcmStageUtils.findStartPhase(dto.getCompositionElement().inProperties());
            var element = automationComposition.getElements()
                    .get(instanceElement.elementId());
            if (element != null && MigrationState.NEW.equals(element.getMigrationState())) {
                startPhase = 0;
            }
            if (startPhaseMsg.equals(startPhase)) {
                if (element != null) {
                    element.setDeployState(DeployState.UNDEPLOYING);
                }
                listener.undeploy(messageId, dto.getCompositionElement(), instanceElement);
            }
        }
    }

    private void handleDeleteState(UUID messageId, final AutomationComposition automationComposition,
            Integer startPhaseMsg, List<ParticipantDto> participantDtoList) {
        automationComposition.setDeployState(DeployState.DELETING);

        var elementDtoMap = ParticipantDtoUtils.getElementDtoMap(participantDtoList, cacheProvider.getParticipantId());

        for (var dto : elementDtoMap.values()) {
            var instanceElement = ParticipantDtoUtils.resolveInstanceElement(dto);
            int startPhase = AcmStageUtils.findStartPhase(dto.getCompositionElement().inProperties());
            if (startPhaseMsg.equals(startPhase)) {
                var element = automationComposition.getElements()
                        .get(instanceElement.elementId());
                if (element != null) {
                    element.setDeployState(DeployState.DELETING);
                    element.setSubState(SubState.NONE);
                }
                listener.delete(messageId, dto.getCompositionElement(), instanceElement);
            }
        }
    }

    /**
     * Handles AutomationComposition Migration.
     *
     * @param migrationMsg the AutomationCompositionMigration
     */
    public void handleAutomationCompositionMigration(AutomationCompositionMigration migrationMsg) {
        var automationComposition = cacheProvider.getAutomationComposition(migrationMsg.getAutomationCompositionId());
        if (Boolean.FALSE.equals(migrationMsg.getRollback())) {
            handleMigration(automationComposition, migrationMsg);
        } else {
            handleRollback(automationComposition, migrationMsg);
        }
    }

    private void handleRollback(AutomationComposition automationComposition,
                                AutomationCompositionMigration migrationMsg) {
        LOGGER.info("Rollback operation invoked for the instance {}", migrationMsg.getAutomationCompositionId());
        automationComposition.setCompositionTargetId(migrationMsg.getCompositionTargetId());
        automationComposition.setDeployState(DeployState.MIGRATION_REVERTING);
        var elementDtoMap = ParticipantDtoUtils.getElementDtoMap(
                migrationMsg.getParticipantDtoList(), cacheProvider.getParticipantId());
        var defaultValue = Boolean.TRUE.equals(migrationMsg.getFirstStage())
                ? migrationMsg.getStage() : migrationMsg.getStage() + 1;
        rollbackAutomationComposition(automationComposition, elementDtoMap,
                migrationMsg.getStage(), defaultValue);
        callParticipantRollback(migrationMsg);
    }

    private void handleMigration(AutomationComposition automationComposition,
            AutomationCompositionMigration migrationMsg) {
        LOGGER.info("Migration invoked for the instance {}", migrationMsg.getAutomationCompositionId());
        automationComposition.setCompositionTargetId(migrationMsg.getCompositionTargetId());
        automationComposition.setDeployState(DeployState.MIGRATING);
        var elementDtoMap = ParticipantDtoUtils.getElementDtoMap(
                migrationMsg.getParticipantDtoList(), cacheProvider.getParticipantId());
        migrateAutomationComposition(automationComposition, elementDtoMap, migrationMsg.getStage());
        callParticipantMigrate(migrationMsg);
    }

    private void callParticipantMigrate(AutomationCompositionMigration migrationMsg) {
        var elementDtoMap = ParticipantDtoUtils.getElementDtoMap(migrationMsg.getParticipantDtoList(),
                cacheProvider.getParticipantId());

        for (var dto : elementDtoMap.values()) {
            var stageSet = AcmStageUtils.findStageSetMigrate(dto.getCompositionElementTarget().inProperties());
            if (stageSet.contains(migrationMsg.getStage())) {
                listenerMigrate(migrationMsg.getMessageId(), dto.getCompositionElement(),
                        dto.getCompositionElementTarget(), dto.getInstanceElement(),
                        dto.getInstanceElementTarget(), migrationMsg.getStage());
            }
        }
    }

    private void callParticipantRollback(AutomationCompositionMigration migrationMsg) {
        var elementDtoMap = ParticipantDtoUtils.getElementDtoMap(migrationMsg.getParticipantDtoList(),
                cacheProvider.getParticipantId());
        var defaultValue = Boolean.TRUE.equals(migrationMsg.getFirstStage())
                ? migrationMsg.getStage() : migrationMsg.getStage() + 1;

        for (var dto : elementDtoMap.values()) {
            Set<Integer> stageSet;
            if (ElementState.NOT_PRESENT.equals(dto.getCompositionElement().state())) {
                stageSet = AcmStageUtils.findStageSetMigrate(dto.getCompositionElementTarget().inProperties());
            } else if (ElementState.REMOVED.equals(dto.getCompositionElementTarget().state())) {
                stageSet = Set.of(defaultValue);
            } else {
                stageSet = AcmStageUtils.findStageSetMigrate(
                        dto.getCompositionElement().inProperties());
            }
            if (stageSet.contains(migrationMsg.getStage())) {
                listenerRollback(migrationMsg.getMessageId(), dto.getCompositionElement(),
                        dto.getCompositionElementTarget(), dto.getInstanceElement(),
                        dto.getInstanceElementTarget(), migrationMsg.getStage());
            }
        }
    }

    private void listenerMigrate(UUID messageId, CompositionElementDto compositionElement,
            CompositionElementDto compositionElementTarget, InstanceElementDto instanceElement,
            InstanceElementDto instanceElementMigrate, int stage) {
        LOGGER.info("Invoking migration of element on the participant for {}", instanceElement.elementId());
        listener.migrate(messageId, compositionElement, compositionElementTarget, instanceElement,
                    instanceElementMigrate, stage);
    }

    private void listenerRollback(UUID messageId, CompositionElementDto compositionElement,
            CompositionElementDto compositionElementTarget, InstanceElementDto instanceElement,
            InstanceElementDto instanceElementMigrate, int stage) {
        LOGGER.info("Invoking rollback of element on the participant for {}", instanceElement.elementId());
        listener.rollback(messageId, compositionElement, compositionElementTarget, instanceElement,
                instanceElementMigrate, stage);
    }
}
