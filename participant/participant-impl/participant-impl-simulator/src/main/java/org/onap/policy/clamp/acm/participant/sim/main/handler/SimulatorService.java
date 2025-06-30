/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.participant.sim.main.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.acm.participant.sim.model.InternalData;
import org.onap.policy.clamp.acm.participant.sim.model.InternalDatas;
import org.onap.policy.clamp.acm.participant.sim.model.SimConfig;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUtils;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * This class handles implementation of Simulator Service.
 */
@Service
@RequiredArgsConstructor
public class SimulatorService {

    private final ParticipantIntermediaryApi intermediaryApi;

    private static final Logger LOGGER = LoggerFactory.getLogger(SimulatorService.class);
    private static final String INTERNAL_STATE = "InternalState";
    private static final String MIGRATION_PROPERTY = "stage";
    private static final String ROLLBACK_PROPERTY = "rollbackStage";
    private static final String PREPARE_PROPERTY = "prepareStage";
    private static final String STAGE_MSG = "stage %d %s";

    @Getter
    @Setter
    private SimConfig config = new SimConfig();

    /**
     * Get AutomationComposition.
     *
     * @return the AutomationCompositions
     */
    public AutomationCompositions getAutomationCompositions() {
        var result = new AutomationCompositions();
        result.setAutomationCompositionList(new ArrayList<>(intermediaryApi.getAutomationCompositions().values()));
        return result;
    }

    public AutomationComposition getAutomationComposition(UUID instanceId) {
        return intermediaryApi.getAutomationComposition(instanceId);
    }

    /**
     * Set OutProperties.
     *
     * @param instanceId       the automationComposition Id
     * @param elementId        the automationComposition Element Id
     * @param useState         the useState
     * @param operationalState the operationalState
     * @param outProperties    the outProperties
     */
    public void setOutProperties(UUID instanceId, UUID elementId, String useState, String operationalState,
                                 Map<String, Object> outProperties) {
        intermediaryApi.sendAcElementInfo(instanceId, elementId, useState, operationalState,
            outProperties);
    }

    /**
     * Get Instance Data List.
     *
     * @return the InternalDatas
     */
    public InternalDatas getDataList() {
        var result = new InternalDatas();
        var map = intermediaryApi.getAutomationCompositions();
        for (var instance : map.values()) {
            for (var element : instance.getElements().values()) {
                var data = new InternalData();
                data.setCompositionId(instance.getCompositionId());
                data.setAutomationCompositionId(instance.getInstanceId());
                data.setAutomationCompositionElementId(element.getId());
                data.setIntProperties(element.getProperties());
                data.setOperationalState(element.getOperationalState());
                data.setUseState(element.getUseState());
                data.setOutProperties(element.getOutProperties());
                result.getList().add(data);
            }
        }
        return result;
    }

    /**
     * Get Composition Data List.
     *
     * @return the InternalDatas
     */
    public InternalDatas getCompositionDataList() {
        var acElementsDefinitions = intermediaryApi.getAcElementsDefinitions();
        var internalDatas = new InternalDatas();
        for (var entry : acElementsDefinitions.entrySet()) {
            for (var acElementsDefinition : entry.getValue().values()) {
                var internalData = new InternalData();
                internalData.setCompositionId(entry.getKey());
                internalData.setCompositionDefinitionElementId(acElementsDefinition.getAcElementDefinitionId());
                internalData.setIntProperties(
                    acElementsDefinition.getAutomationCompositionElementToscaNodeTemplate().getProperties());
                internalData.setOutProperties(acElementsDefinition.getOutProperties());
                internalDatas.getList().add(internalData);
            }
        }
        return internalDatas;
    }

    public void setCompositionOutProperties(UUID compositionId, ToscaConceptIdentifier compositionDefinitionElementId,
                                            Map<String, Object> outProperties) {
        intermediaryApi.sendAcDefinitionInfo(compositionId, compositionDefinitionElementId, outProperties);

    }

    protected boolean isInterrupted(int timeMs, String msg, UUID elementId) {
        long endTime = System.nanoTime() + (timeMs * 1_000_000L);
        while (System.nanoTime() < endTime) {
            if (Thread.interrupted()) {
                LOGGER.debug(msg, elementId);
                return true;
            }
            LockSupport.parkNanos(10_000_000L);
        }
        return false;
    }

    private void sendAcInternalState(UUID instanceId, UUID elementId, Map<String, Object> outProperties,
            DeployState deployState) {
        outProperties.put(INTERNAL_STATE, deployState.name());
        intermediaryApi.sendAcElementInfo(instanceId, elementId, null, null, outProperties);
    }

    /**
     * Handle deploying an automation composition element.
     *
     * @param instanceId    the instanceId
     * @param elementId     the elementId
     * @param outProperties the outProperties
     */
    public void deploy(UUID instanceId, UUID elementId, Map<String, Object> outProperties) {
        sendAcInternalState(instanceId, elementId, outProperties, DeployState.DEPLOYING);

        if (isInterrupted(getConfig().getDeployTimerMs(),
            "Current Thread deploy is Interrupted during execution {}", elementId)) {
            return;
        }

        if (getConfig().isDeploySuccess()) {
            sendAcInternalState(instanceId, elementId, outProperties, DeployState.DEPLOYED);

            intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId,
                DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Deployed");
        } else {
            sendAcInternalState(instanceId, elementId, outProperties, DeployState.UNDEPLOYED);

            intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId,
                DeployState.UNDEPLOYED, null, StateChangeResult.FAILED, "Deploy failed!");
        }
    }

    /**
     * Handle undeploying an automation composition element.
     *
     * @param instanceId    the instanceId
     * @param elementId     the elementId
     * @param outProperties the outProperties
     */
    public void undeploy(UUID instanceId, UUID elementId, Map<String, Object> outProperties) {
        sendAcInternalState(instanceId, elementId, outProperties, DeployState.UNDEPLOYING);

        if (isInterrupted(getConfig().getUndeployTimerMs(),
            "Current Thread undeploy is Interrupted during execution {}", elementId)) {
            return;
        }

        if (getConfig().isUndeploySuccess()) {
            sendAcInternalState(instanceId, elementId, outProperties, DeployState.UNDEPLOYED);

            intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId,
                DeployState.UNDEPLOYED, null, StateChangeResult.NO_ERROR, "Undeployed");
        } else {
            sendAcInternalState(instanceId, elementId, outProperties, DeployState.DEPLOYED);

            intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId,
                DeployState.DEPLOYED, null, StateChangeResult.FAILED, "Undeploy failed!");
        }
    }

    /**
     * Handle locking an automation composition element.
     *
     * @param instanceId the instanceId
     * @param elementId  the elementId
     */
    public void lock(UUID instanceId, UUID elementId) {
        if (isInterrupted(getConfig().getLockTimerMs(),
            "Current Thread lock is Interrupted during execution {}", elementId)) {
            return;
        }

        if (getConfig().isLockSuccess()) {
            intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId,
                null, LockState.LOCKED, StateChangeResult.NO_ERROR, "Locked");
        } else {
            intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId,
                null, LockState.UNLOCKED, StateChangeResult.FAILED, "Lock failed!");
        }
    }

    /**
     * Handle unlocking an automation composition element.
     *
     * @param instanceId the instanceId
     * @param elementId  the elementId
     */
    public void unlock(UUID instanceId, UUID elementId) {
        if (isInterrupted(getConfig().getUnlockTimerMs(),
            "Current Thread unlock is Interrupted during execution {}", elementId)) {
            return;
        }

        if (getConfig().isUnlockSuccess()) {
            intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId,
                null, LockState.UNLOCKED, StateChangeResult.NO_ERROR, "Unlocked");
        } else {
            intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId,
                null, LockState.LOCKED, StateChangeResult.FAILED, "Unlock failed!");
        }
    }

    /**
     * Handle deleting an automation composition element.
     *
     * @param instanceId the instanceId
     * @param elementId  the elementId
     */
    public void delete(UUID instanceId, UUID elementId) {
        if (isInterrupted(getConfig().getDeleteTimerMs(),
            "Current Thread delete is Interrupted during execution {}", elementId)) {
            return;
        }

        if (getConfig().isDeleteSuccess()) {
            intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId,
                DeployState.DELETED, null, StateChangeResult.NO_ERROR, "Deleted");
        } else {
            intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId,
                DeployState.UNDEPLOYED, null, StateChangeResult.FAILED, "Delete failed!");
        }
    }

    /**
     * Handle an update on an automation composition element.
     *
     * @param instanceId the instanceId
     * @param elementId  the elementId
     */
    public void update(UUID instanceId, UUID elementId) {
        if (isInterrupted(getConfig().getUpdateTimerMs(),
            "Current Thread update is Interrupted during execution {}", elementId)) {
            return;
        }

        if (getConfig().isUpdateSuccess()) {
            intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId,
                DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Updated");
        } else {
            intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId,
                DeployState.DEPLOYED, null, StateChangeResult.FAILED, "Update failed!");
        }
    }

    /**
     * Handle a prime on an automation composition definition.
     *
     * @param composition the information of the Automation Composition Definition
     */
    public void prime(CompositionDto composition) {
        if (isInterrupted(getConfig().getPrimeTimerMs(),
            "Current Thread prime is Interrupted during execution {}", composition.compositionId())) {
            return;
        }

        if (getConfig().isPrimeSuccess()) {
            sendOutProperties(composition, AcTypeState.PRIMED.name());
            intermediaryApi.updateCompositionState(composition.compositionId(), AcTypeState.PRIMED,
                StateChangeResult.NO_ERROR, "Primed");
        } else {
            sendOutProperties(composition, AcTypeState.COMMISSIONED.name());
            intermediaryApi.updateCompositionState(composition.compositionId(), AcTypeState.COMMISSIONED,
                StateChangeResult.FAILED, "Prime failed!");
        }
    }

    private void sendOutProperties(CompositionDto composition, String data) {
        for (var elementEntry : composition.outPropertiesMap().entrySet()) {
            elementEntry.getValue().put(INTERNAL_STATE, data);
            intermediaryApi.sendAcDefinitionInfo(
                composition.compositionId(), elementEntry.getKey(), elementEntry.getValue());
        }
    }

    /**
     * Handle a deprime on an automation composition definition.
     *
     * @param composition the information of the Automation Composition Definition
     */
    public void deprime(CompositionDto composition) {
        if (isInterrupted(getConfig().getDeprimeTimerMs(),
            "Current Thread deprime is Interrupted during execution {}", composition.compositionId())) {
            return;
        }

        if (getConfig().isDeprimeSuccess()) {
            sendOutProperties(composition, AcTypeState.COMMISSIONED.name());
            intermediaryApi.updateCompositionState(composition.compositionId(), AcTypeState.COMMISSIONED,
                StateChangeResult.NO_ERROR, "Deprimed");
        } else {
            sendOutProperties(composition, AcTypeState.PRIMED.name());
            intermediaryApi.updateCompositionState(composition.compositionId(), AcTypeState.PRIMED,
                StateChangeResult.FAILED, "Deprime failed!");
        }
    }

    /**
     * Handle a migration on an automation composition element.
     *
     * @param instanceId              the instanceId
     * @param elementId               the elementId
     * @param stage                   the stage
     * @param compositionInProperties in Properties from composition definition element
     * @param instanceOutProperties   in Properties from instance element
     */
    public void migrate(UUID instanceId, UUID elementId, int stage, Map<String, Object> compositionInProperties,
                        Map<String, Object> instanceOutProperties) {
        if (isInterrupted(getConfig().getMigrateTimerMs(),
            "Current Thread migrate is Interrupted during execution {}", elementId)) {
            return;
        }

        if (config.isMigrateSuccess()) {
            var stageSet = ParticipantUtils.findStageSetMigrate(compositionInProperties);
            var nextStage = 1000;
            for (var s : stageSet) {
                if (s > stage) {
                    nextStage = Math.min(s, nextStage);
                }
            }
            instanceOutProperties.putIfAbsent(MIGRATION_PROPERTY, new ArrayList<>());
            @SuppressWarnings("unchecked")
            var stageList = (List<Integer>) instanceOutProperties.get(MIGRATION_PROPERTY);
            stageList.add(stage);
            intermediaryApi.sendAcElementInfo(instanceId, elementId, null, null, instanceOutProperties);
            if (nextStage == 1000) {
                intermediaryApi.updateAutomationCompositionElementState(
                    instanceId, elementId,
                    DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Migrated");
            } else {
                intermediaryApi.updateAutomationCompositionElementStage(
                    instanceId, elementId,
                    StateChangeResult.NO_ERROR, nextStage, String.format(STAGE_MSG, stage, "Migrated"));
            }
        } else {
            intermediaryApi.updateAutomationCompositionElementState(
                instanceId, elementId,
                DeployState.DEPLOYED, null, StateChangeResult.FAILED, "Migrate failed!");
        }
    }

    /**
     * Handle a Migrate Precheck on an automation composition element.
     *
     * @param instanceId the instanceId
     * @param elementId  the elementId
     */
    public void migratePrecheck(UUID instanceId, UUID elementId) {
        if (isInterrupted(config.getMigratePrecheckTimerMs(),
            "Current Thread migrate precheck is Interrupted during execution {}", elementId)) {
            return;
        }

        if (config.isMigratePrecheck()) {
            intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId,
                DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Migration precheck completed");
        } else {
            intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId,
                DeployState.DEPLOYED, null, StateChangeResult.FAILED, "Migration precheck failed");
        }
    }

    /**
     * Handle a Prepare on an automation composition element.
     *
     * @param instanceId              the instanceId
     * @param elementId               the elementId
     * @param stage                   the stage
     * @param compositionInProperties in Properties from composition definition element
     * @param instanceOutProperties   in Properties from instance element
     */
    public void prepare(UUID instanceId, UUID elementId, int stage, Map<String, Object> compositionInProperties,
                        Map<String, Object> instanceOutProperties) {
        if (isInterrupted(config.getPrepareTimerMs(),
            "Current Thread prepare is Interrupted during execution {}", elementId)) {
            return;
        }

        if (config.isPrepare()) {
            var stageSet = ParticipantUtils.findStageSetPrepare(compositionInProperties);
            var nextStage = 1000;
            for (var s : stageSet) {
                if (s > stage) {
                    nextStage = Math.min(s, nextStage);
                }
            }
            instanceOutProperties.putIfAbsent(PREPARE_PROPERTY, new ArrayList<>());
            @SuppressWarnings("unchecked")
            var stageList = (List<Integer>) instanceOutProperties.get(PREPARE_PROPERTY);
            stageList.add(stage);
            intermediaryApi.sendAcElementInfo(instanceId, elementId, null, null, instanceOutProperties);
            if (nextStage == 1000) {
                intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId,
                    DeployState.UNDEPLOYED, null, StateChangeResult.NO_ERROR, "Prepare completed");
            } else {
                intermediaryApi.updateAutomationCompositionElementStage(
                    instanceId, elementId,
                    StateChangeResult.NO_ERROR, nextStage, String.format(STAGE_MSG, stage, "Prepared"));
            }
        } else {
            intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId,
                DeployState.UNDEPLOYED, null, StateChangeResult.FAILED, "Prepare failed");
        }
    }

    /**
     * Handle a Review on an automation composition element.
     *
     * @param instanceId the instanceId
     * @param elementId  the elementId
     */
    public void review(UUID instanceId, UUID elementId) {
        if (isInterrupted(config.getReviewTimerMs(),
            "Current Thread review is Interrupted during execution {}", elementId)) {
            return;
        }

        if (config.isReview()) {
            intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId,
                DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Review completed");
        } else {
            intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId,
                DeployState.DEPLOYED, null, StateChangeResult.FAILED, "Review failed");
        }
    }

    /**
     * Handle rollback of an automation composition.
     *
     * @param instanceId              the instanceId
     * @param elementId               the elementId
     * @param stage                   the stage
     * @param compositionInProperties in Properties from composition definition element
     * @param instanceOutProperties   in Properties from instance element
     */
    public void rollback(UUID instanceId, UUID elementId, int stage, Map<String, Object> compositionInProperties,
            Map<String, Object> instanceOutProperties) {
        if (isInterrupted(getConfig().getRollbackTimerMs(),
            "Current Thread for rollback was Interrupted during execution {}", instanceId)) {
            LOGGER.debug("Rollback interrupted");
            return;
        }

        if (config.isRollback()) {
            var stageSet = ParticipantUtils.findStageSetMigrate(compositionInProperties);
            var nextStage = 1000;
            for (var s : stageSet) {
                if (s > stage) {
                    nextStage = Math.min(s, nextStage);
                }
            }
            instanceOutProperties.putIfAbsent(ROLLBACK_PROPERTY, new ArrayList<>());
            @SuppressWarnings("unchecked")
            var stageList = (List<Integer>) instanceOutProperties.get(ROLLBACK_PROPERTY);
            stageList.add(stage);
            intermediaryApi.sendAcElementInfo(instanceId, elementId, null, null, instanceOutProperties);
            if (nextStage == 1000) {
                intermediaryApi.updateAutomationCompositionElementState(
                        instanceId, elementId,
                        DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Migration rollback done");
            } else {
                intermediaryApi.updateAutomationCompositionElementStage(
                        instanceId, elementId,
                        StateChangeResult.NO_ERROR, nextStage, String.format(STAGE_MSG, stage, "Migration rollback"));
            }
        } else {
            intermediaryApi.updateAutomationCompositionElementState(
                    instanceId, elementId,
                    DeployState.DEPLOYED, null, StateChangeResult.FAILED, "Migration rollback failed");
        }
    }
}
