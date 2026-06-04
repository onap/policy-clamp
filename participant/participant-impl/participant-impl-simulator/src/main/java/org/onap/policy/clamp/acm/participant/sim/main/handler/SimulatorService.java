/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2026 OpenInfra Foundation Europe. All rights reserved.
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
import org.onap.policy.clamp.acm.participant.intermediary.api.ElementStageDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.ElementStateDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.acm.participant.sim.model.InternalData;
import org.onap.policy.clamp.acm.participant.sim.model.InternalDatas;
import org.onap.policy.clamp.acm.participant.sim.model.SimConfig;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
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

    public void sendCompositionOutProperties(UUID compositionId, ToscaConceptIdentifier compositionDefinitionElementId,
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
            outProperties.put(INTERNAL_STATE, DeployState.DEPLOYED.name());
            intermediaryApi.updateAutomationCompositionElementState(new ElementStateDto(instanceId, elementId,
                    DeployState.DEPLOYED, StateChangeResult.NO_ERROR, "Deployed", outProperties));
        } else {
            outProperties.put(INTERNAL_STATE, DeployState.UNDEPLOYED.name());
            intermediaryApi.updateAutomationCompositionElementState(new ElementStateDto(instanceId, elementId,
                DeployState.UNDEPLOYED, StateChangeResult.FAILED, "Deploy failed!", outProperties));
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
            outProperties.put(INTERNAL_STATE, DeployState.UNDEPLOYED);

            intermediaryApi.updateAutomationCompositionElementState(new ElementStateDto(instanceId, elementId,
                DeployState.UNDEPLOYED, StateChangeResult.NO_ERROR, "Undeployed", outProperties));
        } else {
            outProperties.put(INTERNAL_STATE, DeployState.DEPLOYED);

            intermediaryApi.updateAutomationCompositionElementState(new ElementStateDto(instanceId, elementId,
                DeployState.DEPLOYED, StateChangeResult.FAILED, "Undeploy failed!", outProperties));
        }
    }

    /**
     * Handle locking an automation composition element.
     *
     * @param instanceId    the instanceId
     * @param elementId     the elementId
     * @param outProperties the outProperties
     */
    public void lock(UUID instanceId, UUID elementId, Map<String, Object> outProperties) {
        if (isInterrupted(getConfig().getLockTimerMs(),
            "Current Thread lock is Interrupted during execution {}", elementId)) {
            return;
        }

        if (getConfig().isLockSuccess()) {
            intermediaryApi.updateAutomationCompositionElementState(ElementStateDto.builder()
                            .instance(instanceId)
                            .elementId(elementId)
                            .lockState(LockState.LOCKED)
                            .stateChangeResult(StateChangeResult.NO_ERROR)
                            .message("Locked")
                            .outProperties(outProperties)
                    .build());
        } else {
            intermediaryApi.updateAutomationCompositionElementState(ElementStateDto.builder()
                            .instance(instanceId)
                            .elementId(elementId)
                            .lockState(LockState.UNLOCKED)
                            .stateChangeResult(StateChangeResult.FAILED)
                            .message("Lock failed!")
                            .outProperties(outProperties).build());
        }
    }

    /**
     * Handle unlocking an automation composition element.
     *
     * @param instanceId    the instanceId
     * @param elementId     the elementId
     * @param outProperties the outProperties
     */
    public void unlock(UUID instanceId, UUID elementId, Map<String, Object> outProperties) {
        if (isInterrupted(getConfig().getUnlockTimerMs(),
            "Current Thread unlock is Interrupted during execution {}", elementId)) {
            return;
        }

        if (getConfig().isUnlockSuccess()) {
            intermediaryApi.updateAutomationCompositionElementState(ElementStateDto.builder()
                    .instance(instanceId)
                    .elementId(elementId)
                    .lockState(LockState.UNLOCKED)
                    .stateChangeResult(StateChangeResult.NO_ERROR)
                    .message("Unlocked")
                    .outProperties(outProperties).build());
        } else {
            intermediaryApi.updateAutomationCompositionElementState(ElementStateDto.builder()
                    .instance(instanceId)
                    .elementId(elementId)
                    .lockState(LockState.LOCKED)
                    .stateChangeResult(StateChangeResult.FAILED)
                    .message("Unlock failed!")
                    .outProperties(outProperties).build());
        }
    }

    /**
     * Handle deleting an automation composition element.
     *
     * @param instanceId    the instanceId
     * @param elementId     the elementId
     * @param outProperties the outProperties
     */
    public void delete(UUID instanceId, UUID elementId, Map<String, Object> outProperties) {
        if (isInterrupted(getConfig().getDeleteTimerMs(),
            "Current Thread delete is Interrupted during execution {}", elementId)) {
            return;
        }

        if (getConfig().isDeleteSuccess()) {
            intermediaryApi.deleteAutomationCompositionElementState(instanceId, elementId);
        } else {
            intermediaryApi.updateAutomationCompositionElementState(new ElementStateDto(
                instanceId, elementId, DeployState.UNDEPLOYED, StateChangeResult.FAILED, "Delete failed!",
                outProperties));
        }
    }

    /**
     * Handle deleting an automation composition element in migration.
     *
     * @param instanceId    the instanceId
     * @param elementId     the elementId
     * @param outProperties the outProperties
     */
    public void deleteInMigration(UUID instanceId, UUID elementId, Map<String, Object> outProperties) {
        if (getConfig().isMigrateSuccess()) {
            intermediaryApi.deleteAutomationCompositionElementState(instanceId, elementId);
        } else {
            intermediaryApi.updateAutomationCompositionElementState(new ElementStateDto(
                instanceId, elementId, DeployState.DEPLOYED, StateChangeResult.FAILED,
                "Migration - Delete failed!", outProperties));
        }
    }

    /**
     * Handle deleting an automation composition element in rollback.
     *
     * @param instanceId the instanceId
     * @param elementId  the elementId
     * @param outProperties in Properties from instance element
     */
    public void deleteInRollback(UUID instanceId, UUID elementId, Map<String, Object> outProperties) {
        if (getConfig().isRollback()) {
            intermediaryApi.deleteAutomationCompositionElementState(instanceId, elementId);
        } else {
            intermediaryApi.updateAutomationCompositionElementState(new ElementStateDto(
                instanceId, elementId, DeployState.UNDEPLOYED, StateChangeResult.FAILED,
                "Rollback - Delete failed!", outProperties));
        }
    }

    /**
     * Handle an update on an automation composition element.
     *
     * @param instanceId    the instanceId
     * @param elementId     the elementId
     * @param outProperties in Properties from instance element
     */
    public void update(UUID instanceId, UUID elementId, Map<String, Object> outProperties) {
        if (isInterrupted(getConfig().getUpdateTimerMs(),
            "Current Thread update is Interrupted during execution {}", elementId)) {
            return;
        }

        if (getConfig().isUpdateSuccess()) {
            intermediaryApi.updateAutomationCompositionElementState(new ElementStateDto(
                instanceId, elementId, DeployState.DEPLOYED, StateChangeResult.NO_ERROR,
                "Updated", outProperties));
        } else {
            intermediaryApi.updateAutomationCompositionElementState(new ElementStateDto(
                instanceId, elementId, DeployState.DEPLOYED, StateChangeResult.FAILED,
                "Update failed!", outProperties));
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
            updateCompositionOutProperties(composition, AcTypeState.PRIMED.name());
            intermediaryApi.updateCompositionState(composition.compositionId(), AcTypeState.PRIMED,
                StateChangeResult.NO_ERROR, "Primed", composition.outPropertiesMap());
        } else {
            updateCompositionOutProperties(composition, AcTypeState.COMMISSIONED.name());
            intermediaryApi.updateCompositionState(composition.compositionId(), AcTypeState.COMMISSIONED,
                StateChangeResult.FAILED, "Prime failed!", composition.outPropertiesMap());
        }
    }

    private void updateCompositionOutProperties(CompositionDto composition, String data) {
        for (var elementEntry : composition.outPropertiesMap().entrySet()) {
            elementEntry.getValue().put(INTERNAL_STATE, data);
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
            updateCompositionOutProperties(composition, AcTypeState.COMMISSIONED.name());
            intermediaryApi.updateCompositionState(composition.compositionId(), AcTypeState.COMMISSIONED,
                StateChangeResult.NO_ERROR, "Deprimed", composition.outPropertiesMap());
        } else {
            updateCompositionOutProperties(composition, AcTypeState.PRIMED.name());
            intermediaryApi.updateCompositionState(composition.compositionId(), AcTypeState.PRIMED,
                StateChangeResult.FAILED, "Deprime failed!", composition.outPropertiesMap());
        }
    }

    /**
     * Handle a migration on an automation composition element.
     *
     * @param instanceId              the instanceId
     * @param elementId               the elementId
     * @param stage                   the stage
     * @param nextStage               the next stage
     * @param outProperties           in Properties from instance element
     */
    public void migrate(UUID instanceId, UUID elementId, int stage, int nextStage,
                        Map<String, Object> outProperties) {
        if (isInterrupted(getConfig().getMigrateTimerMs(),
            "Current Thread migrate is Interrupted during execution {}", elementId)) {
            return;
        }

        if (config.isMigrateSuccess()) {
            outProperties.putIfAbsent(MIGRATION_PROPERTY, new ArrayList<>());
            @SuppressWarnings("unchecked")
            var stageList = (List<Integer>) outProperties.get(MIGRATION_PROPERTY);
            stageList.add(stage);
            if (nextStage == stage) {
                intermediaryApi.updateAutomationCompositionElementState(new ElementStateDto(instanceId, elementId,
                    DeployState.DEPLOYED, StateChangeResult.NO_ERROR, "Migrated", outProperties));
            } else {
                var msg = String.format(STAGE_MSG, stage, "Migrated");
                intermediaryApi.updateAutomationCompositionElementStage(new ElementStageDto(
                    instanceId, elementId, msg, nextStage, null, null, outProperties));
            }
        } else {
            intermediaryApi.updateAutomationCompositionElementState(new ElementStateDto(
                instanceId, elementId, DeployState.DEPLOYED, StateChangeResult.FAILED,
                "Migrate failed!", outProperties));
        }
    }

    /**
     * Handle a Migrate Precheck on an automation composition element.
     *
     * @param instanceId    the instanceId
     * @param elementId     the elementId
     * @param outProperties in Properties from instance element
     */
    public void migratePrecheck(UUID instanceId, UUID elementId, Map<String, Object> outProperties) {
        if (isInterrupted(config.getMigratePrecheckTimerMs(),
            "Current Thread migrate precheck is Interrupted during execution {}", elementId)) {
            return;
        }

        if (config.isMigratePrecheck()) {
            intermediaryApi.updateAutomationCompositionElementState(new ElementStateDto(
                instanceId, elementId, DeployState.DEPLOYED, StateChangeResult.NO_ERROR,
                "Migration precheck completed", outProperties));
        } else {
            intermediaryApi.updateAutomationCompositionElementState(new ElementStateDto(
                instanceId, elementId, DeployState.DEPLOYED, StateChangeResult.FAILED,
                "Migration precheck failed", outProperties));
        }
    }

    /**
     * Handle a Prepare on an automation composition element.
     *
     * @param instanceId      the instanceId
     * @param elementId       the elementId
     * @param stage           the stage
     * @param nextStage       the next stage
     * @param outProperties   in Properties from instance element
     */
    public void prepare(UUID instanceId, UUID elementId, int stage, int nextStage, Map<String, Object> outProperties) {
        if (isInterrupted(config.getPrepareTimerMs(),
            "Current Thread prepare is Interrupted during execution {}", elementId)) {
            return;
        }

        if (config.isPrepare()) {
            outProperties.putIfAbsent(PREPARE_PROPERTY, new ArrayList<>());
            @SuppressWarnings("unchecked")
            var stageList = (List<Integer>) outProperties.get(PREPARE_PROPERTY);
            stageList.add(stage);
            if (nextStage == stage) {
                intermediaryApi.updateAutomationCompositionElementState(new ElementStateDto(
                    instanceId, elementId, DeployState.UNDEPLOYED, StateChangeResult.NO_ERROR,
                    "Prepare completed", outProperties));
            } else {
                var msg = String.format(STAGE_MSG, stage, "Prepared");
                intermediaryApi.updateAutomationCompositionElementStage(new ElementStageDto(
                    instanceId, elementId, msg, nextStage, outProperties));
            }
        } else {
            intermediaryApi.updateAutomationCompositionElementState(new ElementStateDto(
                instanceId, elementId, DeployState.UNDEPLOYED, StateChangeResult.FAILED,
                "Prepare failed", outProperties));
        }
    }

    /**
     * Handle a Review on an automation composition element.
     *
     * @param instanceId    the instanceId
     * @param elementId     the elementId
     * @param outProperties in Properties from instance element
     */
    public void review(UUID instanceId, UUID elementId, Map<String, Object> outProperties) {
        if (isInterrupted(config.getReviewTimerMs(),
            "Current Thread review is Interrupted during execution {}", elementId)) {
            return;
        }

        if (config.isReview()) {
            intermediaryApi.updateAutomationCompositionElementState(new ElementStateDto(instanceId, elementId,
                DeployState.DEPLOYED, StateChangeResult.NO_ERROR, "Review completed", outProperties));
        } else {
            intermediaryApi.updateAutomationCompositionElementState(new ElementStateDto(instanceId, elementId,
                DeployState.DEPLOYED, StateChangeResult.FAILED, "Review failed", outProperties));
        }
    }

    /**
     * Handle rollback of an automation composition.
     *
     * @param instanceId              the instanceId
     * @param elementId               the elementId
     * @param stage                   the stage
     * @param nextStage               the next stage
     * @param instanceOutProperties   in Properties from instance element
     */
    public void rollback(UUID instanceId, UUID elementId, int stage, int nextStage,
            Map<String, Object> instanceOutProperties) {
        if (isInterrupted(getConfig().getRollbackTimerMs(),
            "Current Thread for rollback was Interrupted during execution {}", instanceId)) {
            LOGGER.debug("Rollback interrupted");
            return;
        }

        if (config.isRollback()) {
            instanceOutProperties.putIfAbsent(ROLLBACK_PROPERTY, new ArrayList<>());
            @SuppressWarnings("unchecked")
            var stageList = (List<Integer>) instanceOutProperties.get(ROLLBACK_PROPERTY);
            stageList.add(stage);
            if (nextStage == stage) {
                intermediaryApi.updateAutomationCompositionElementState(new ElementStateDto(
                    instanceId, elementId, DeployState.DEPLOYED, StateChangeResult.NO_ERROR,
                    "Migration rollback done", instanceOutProperties));
            } else {
                var msg = String.format(STAGE_MSG, stage, "Migration rollback");
                intermediaryApi.updateAutomationCompositionElementStage(new ElementStageDto(
                    instanceId, elementId, msg, nextStage, instanceOutProperties));
            }
        } else {
            intermediaryApi.updateAutomationCompositionElementState(new ElementStateDto(
                instanceId, elementId, DeployState.DEPLOYED, StateChangeResult.FAILED,
                "Migration rollback failed", instanceOutProperties));
        }
    }
}
