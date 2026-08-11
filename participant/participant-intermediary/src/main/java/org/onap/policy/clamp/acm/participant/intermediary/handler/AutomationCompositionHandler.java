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

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.acm.participant.intermediary.handler.cache.CacheProvider;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.clamp.models.acm.dto.AcElementDto;
import org.onap.policy.clamp.models.acm.dto.ElementState;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeploy;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionMigration;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionStateChange;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.PropertiesUpdate;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.utils.AcmStageUtils;
import org.springframework.stereotype.Component;

/*
 * This class is responsible for managing the state of all automation compositions in the participant.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutomationCompositionHandler {
    private final CacheProvider cacheProvider;
    private final ParticipantMessagePublisher publisher;
    private final ThreadHandler listener;

    /**
     * Handle a automation composition state change message.
     *
     * @param stateChangeMsg the state change message
     */
    public void handleAutomationCompositionStateChange(AutomationCompositionStateChange stateChangeMsg) {
        var elementDtoMap = ParticipantDtoUtils.getElementDtoMap(
                stateChangeMsg.getParticipantDtoList(), cacheProvider.getParticipantId());
        if (elementDtoMap.isEmpty()) {
            log.warn("AutomationCompositionStateChange empty in message {}",
                    stateChangeMsg.getAutomationCompositionId());
            return;
        }
        cacheProvider.fillCacheComposition(stateChangeMsg.getParticipantDtoList());

        var deployState = DeployOrder.UNDEPLOY.equals(stateChangeMsg.getDeployOrderedState())
                ? DeployState.UNDEPLOYING : DeployState.DELETING;
        cacheProvider.createAcInstance(stateChangeMsg.getCompositionId(),
                stateChangeMsg.getAutomationCompositionId(), elementDtoMap,
                deployState, SubState.NONE, stateChangeMsg.getRevisionIdInstance());

        switch (stateChangeMsg.getDeployOrderedState()) {
            case UNDEPLOY -> handleUndeployState(stateChangeMsg.getMessageId(),
                    stateChangeMsg.getStartPhase(), elementDtoMap);
            case DELETE -> handleDeleteState(stateChangeMsg.getMessageId(),
                    stateChangeMsg.getStartPhase(), elementDtoMap);
            default -> log.error(
                    "StateChange message has no state, state is null {}", stateChangeMsg.getAutomationCompositionId());
        }
    }

    /**
     * Handle a automation composition properties update message.
     *
     * @param updateMsg the properties update message
     */
    public void handleAcPropertyUpdate(PropertiesUpdate updateMsg) {
        var elementDtoMap = ParticipantDtoUtils.getElementDtoMap(
                updateMsg.getParticipantDtoList(), cacheProvider.getParticipantId());
        if (elementDtoMap.isEmpty()) {
            log.warn("No AutomationCompositionElement updates in message {}", updateMsg.getAutomationCompositionId());
            return;
        }
        cacheProvider.fillCacheComposition(updateMsg.getParticipantDtoList());

        var deployState = updateMsg.isRollback()
                ? DeployState.UPDATE_REVERTING : DeployState.UPDATING;
        cacheProvider.createAcInstance(updateMsg.getCompositionId(),
                updateMsg.getAutomationCompositionId(), elementDtoMap,
                deployState, SubState.NONE, updateMsg.getRevisionIdInstance());

        callParticipantUpdateProperty(updateMsg.getMessageId(), elementDtoMap);
    }

    private void callParticipantUpdateProperty(UUID messageId, Map<UUID, AcElementDto> elementDtoMap) {
        for (var dto : elementDtoMap.values()) {
            listener.update(messageId, dto.getCompositionElement(),
                    dto.getInstanceElement(), dto.getInstanceElementTarget());
        }
    }

    /**
     * Handle a automation composition Deploy message.
     *
     * @param deployMsg the Deploy message
     */
    public void handleAutomationCompositionDeploy(AutomationCompositionDeploy deployMsg) {
        var elementDtoMap = ParticipantDtoUtils.getElementDtoMap(
                deployMsg.getParticipantDtoList(), cacheProvider.getParticipantId());
        if (elementDtoMap.isEmpty()) {
            log.warn("No AutomationCompositionElement deploy in message {}", deployMsg.getAutomationCompositionId());
            return;
        }
        cacheProvider.fillCacheComposition(deployMsg.getParticipantDtoList());

        cacheProvider.createAcInstance(deployMsg.getCompositionId(), deployMsg.getAutomationCompositionId(),
                elementDtoMap, DeployState.DEPLOYING, SubState.NONE, deployMsg.getRevisionIdInstance());

        callParticipantDeploy(deployMsg.getMessageId(), deployMsg.getStartPhase(), elementDtoMap);
    }

    private void callParticipantDeploy(UUID messageId, Integer startPhaseMsg, Map<UUID, AcElementDto> elementDtoMap) {
        for (var dto : elementDtoMap.values()) {
            int startPhase = AcmStageUtils.findStartPhase(dto.getCompositionElement().inProperties());
            if (startPhaseMsg.equals(startPhase)) {
                listener.deploy(messageId, dto.getCompositionElement(),
                        ParticipantDtoUtils.resolveInstanceElement(dto));
            }
        }
    }

    /**
     * Method to handle when the new state from participant is UNINITIALISED state.
     *
     * @param messageId             the messageId
     */
    private void handleUndeployState(UUID messageId, Integer startPhaseMsg, Map<UUID, AcElementDto> elementDtoMap) {

        for (var dto : elementDtoMap.values()) {
            var instanceElement = ParticipantDtoUtils.resolveInstanceElement(dto);
            int startPhase = AcmStageUtils.findStartPhase(dto.getCompositionElement().inProperties());
            if (startPhaseMsg.equals(startPhase)) {
                listener.undeploy(messageId, dto.getCompositionElement(), instanceElement);
            }
        }
    }

    private void handleDeleteState(UUID messageId, Integer startPhaseMsg, Map<UUID, AcElementDto> elementDtoMap) {
        for (var dto : elementDtoMap.values()) {
            if (DeployState.DELETED.equals(dto.getDeployState())) {
                continue;
            }
            var instanceElement = ParticipantDtoUtils.resolveInstanceElement(dto);
            int startPhase = AcmStageUtils.findStartPhase(dto.getCompositionElement().inProperties());
            if (startPhaseMsg.equals(startPhase)) {
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
        var elementDtoMap = ParticipantDtoUtils.getElementDtoMap(
                migrationMsg.getParticipantDtoList(), cacheProvider.getParticipantId());
        if (elementDtoMap.isEmpty()) {
            log.warn("No AutomationCompositionElement migration in message {}",
                    migrationMsg.getAutomationCompositionId());
            return;
        }
        cacheProvider.fillCacheComposition(migrationMsg.getParticipantDtoList());

        var deployState = Boolean.FALSE.equals(migrationMsg.getRollback())
                ? DeployState.MIGRATING : DeployState.MIGRATION_REVERTING;
        cacheProvider.createAcInstance(migrationMsg.getCompositionId(), migrationMsg.getAutomationCompositionId(),
                elementDtoMap, deployState, SubState.NONE, migrationMsg.getRevisionIdInstance());

        if (Boolean.FALSE.equals(migrationMsg.getRollback())) {
            log.info("Migration invoked for the instance {}", migrationMsg.getAutomationCompositionId());
            handleMigration(migrationMsg.getMessageId(), elementDtoMap, migrationMsg.getStage());
        } else {
            log.info("Rollback operation invoked for the instance {}", migrationMsg.getAutomationCompositionId());
            handleRollback(migrationMsg.getMessageId(), elementDtoMap, migrationMsg.getStage(),
                    migrationMsg.getFirstStage());
        }
    }

    private void handleMigration(UUID messageId, Map<UUID, AcElementDto> elementDtoMap, int stage) {
        for (var dto : elementDtoMap.values()) {
            var stageSet = AcmStageUtils.findStageSetMigrate(dto.getCompositionElementTarget().inProperties());
            if (stageSet.contains(stage)) {
                log.info("Invoking migration of element on the participant for {}",
                        dto.getInstanceElement().elementId());
                listener.migrate(messageId, dto.getCompositionElement(),
                        dto.getCompositionElementTarget(), dto.getInstanceElement(),
                        dto.getInstanceElementTarget(), stage);
            }
        }
    }

    private void handleRollback(UUID messageId, Map<UUID, AcElementDto> elementDtoMap, int stage, boolean firstStage) {
        var defaultValue = firstStage ? stage : stage + 1;

        for (var dto : elementDtoMap.values()) {
            Set<Integer> stageSet;
            if (ElementState.NOT_PRESENT.equals(dto.getCompositionElement().state())) {
                stageSet = AcmStageUtils.findStageSetMigrate(dto.getCompositionElementTarget().inProperties());
            } else if (ElementState.REMOVED.equals(dto.getCompositionElementTarget().state())) {
                stageSet = Set.of(defaultValue);
            } else {
                stageSet = AcmStageUtils.findStageSetMigrate(dto.getCompositionElement().inProperties());
            }
            if (stageSet.contains(stage)) {
                log.info("Invoking rollback of element on the participant for {}",
                        dto.getInstanceElement().elementId());
                listener.rollback(messageId, dto.getCompositionElement(),
                        dto.getCompositionElementTarget(), dto.getInstanceElement(),
                        dto.getInstanceElementTarget(), stage);
            }
        }
    }
}
