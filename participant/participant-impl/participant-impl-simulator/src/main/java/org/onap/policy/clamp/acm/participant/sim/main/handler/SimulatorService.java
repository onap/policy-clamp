/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation.
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
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
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
     * @param instanceId the automationComposition Id
     * @param elementId the automationComposition Element Id
     * @param useState the useState
     * @param operationalState the operationalState
     * @param outProperties the outProperties
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

    private boolean execution(int timeMs, String msg, UUID elementId) {
        long endTime = System.currentTimeMillis() + timeMs;
        while (System.currentTimeMillis() < endTime) {
            try {
                if (Thread.currentThread().isInterrupted()) {
                    LOGGER.debug(msg, elementId);
                    return false;
                }
                Thread.sleep(10L);
            } catch (InterruptedException e) {
                LOGGER.debug(msg, elementId);
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }

    /**
     * Handle a deploy on a automation composition element.
     *
     * @param instanceId the instanceId
     * @param elementId the elementId
     */
    public void deploy(UUID instanceId, UUID elementId) {
        if (!execution(getConfig().getDeployTimerMs(),
                "Current Thread deploy is Interrupted during execution {}", elementId)) {
            return;
        }

        if (getConfig().isDeploySuccess()) {
            intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId,
                    DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Deployed");
        } else {
            intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId,
                    DeployState.UNDEPLOYED, null, StateChangeResult.FAILED, "Deploy failed!");
        }
    }

    /**
     * Handle an udeploy on a automation composition element.
     *
     * @param instanceId the instanceId
     * @param elementId the elementId
     */
    public void undeploy(UUID instanceId, UUID elementId) {
        if (!execution(getConfig().getUndeployTimerMs(),
                "Current Thread undeploy is Interrupted during execution {}", elementId)) {
            return;
        }

        if (getConfig().isUndeploySuccess()) {
            intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId,
                    DeployState.UNDEPLOYED, null, StateChangeResult.NO_ERROR, "Undeployed");
        } else {
            intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId,
                    DeployState.DEPLOYED, null, StateChangeResult.FAILED, "Undeploy failed!");
        }
    }

    /**
     * Handle a lock on a automation composition element.
     *
     * @param instanceId the instanceId
     * @param elementId the elementId
     */
    public void lock(UUID instanceId, UUID elementId) {
        if (!execution(getConfig().getLockTimerMs(),
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
     * Handle an unlock on a automation composition element.
     *
     * @param instanceId the instanceId
     * @param elementId the elementId
     */
    public void unlock(UUID instanceId, UUID elementId) {
        if (!execution(getConfig().getUnlockTimerMs(),
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
     * Handle a delete on a automation composition element.
     *
     * @param instanceId the instanceId
     * @param elementId the elementId
     */
    public void delete(UUID instanceId, UUID elementId) {
        if (!execution(getConfig().getDeleteTimerMs(),
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
     * Handle an update on a automation composition element.
     *
     * @param instanceId the instanceId
     * @param elementId the elementId
     */
    public void update(UUID instanceId, UUID elementId) {
        if (!execution(getConfig().getUpdateTimerMs(),
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
     * Handle a prime on a automation composition definition.
     *
     * @param compositionId the compositionId
     */
    public void prime(UUID compositionId) {
        if (!execution(getConfig().getPrimeTimerMs(),
                "Current Thread prime is Interrupted during execution {}", compositionId)) {
            return;
        }

        if (getConfig().isPrimeSuccess()) {
            intermediaryApi.updateCompositionState(compositionId, AcTypeState.PRIMED, StateChangeResult.NO_ERROR,
                    "Primed");
        } else {
            intermediaryApi.updateCompositionState(compositionId, AcTypeState.COMMISSIONED, StateChangeResult.FAILED,
                    "Prime failed!");
        }
    }

    /**
     * Handle a deprime on a automation composition definition.
     *
     * @param compositionId the compositionId
     */
    public void deprime(UUID compositionId) {
        if (!execution(getConfig().getDeprimeTimerMs(),
                "Current Thread deprime is Interrupted during execution {}", compositionId)) {
            return;
        }

        if (getConfig().isDeprimeSuccess()) {
            intermediaryApi.updateCompositionState(compositionId, AcTypeState.COMMISSIONED, StateChangeResult.NO_ERROR,
                    "Deprimed");
        } else {
            intermediaryApi.updateCompositionState(compositionId, AcTypeState.PRIMED, StateChangeResult.FAILED,
                    "Deprime failed!");
        }
    }

    /**
     * Handle a migrate on a automation composition element.
     *
     * @param instanceId the instanceId
     * @param elementId the elementId
     */
    public void migrate(UUID instanceId, UUID elementId) {
        if (!execution(getConfig().getMigrateTimerMs(),
                "Current Thread migrate is Interrupted during execution {}", elementId)) {
            return;
        }

        if (getConfig().isMigrateSuccess()) {
            intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId,
                    DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Migrated");
        } else {
            intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId,
                    DeployState.DEPLOYED, null, StateChangeResult.FAILED, "Migrate failed!");
        }
    }

    /**
     * Handle a Migrate Precheck on a automation composition element.
     *
     * @param instanceId the instanceId
     * @param elementId the elementId
     */
    public void migratePrecheck(UUID instanceId, UUID elementId) {
        if (!execution(config.getMigratePrecheckTimerMs(),
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
     * Handle a Prepare on a automation composition element.
     *
     * @param instanceId the instanceId
     * @param elementId the elementId
     */
    public void prepare(UUID instanceId, UUID elementId) {
        if (!execution(config.getPrepareTimerMs(),
                "Current Thread prepare is Interrupted during execution {}", elementId)) {
            return;
        }

        if (config.isPrepare()) {
            intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId,
                    DeployState.UNDEPLOYED, null, StateChangeResult.NO_ERROR, "Prepare completed");
        } else {
            intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId,
                    DeployState.UNDEPLOYED, null, StateChangeResult.FAILED, "Prepare failed");
        }
    }

    /**
     * Handle a Review on a automation composition element.
     *
     * @param instanceId the instanceId
     * @param elementId the elementId
     */
    public void review(UUID instanceId, UUID elementId) {
        if (!execution(config.getReviewTimerMs(),
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
}
