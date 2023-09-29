/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
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

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.onap.policy.clamp.acm.participant.intermediary.api.AutomationCompositionElementListener;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.acm.participant.sim.model.InternalData;
import org.onap.policy.clamp.acm.participant.sim.model.InternalDatas;
import org.onap.policy.clamp.acm.participant.sim.model.SimConfig;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class handles implementation of automationCompositionElement updates.
 */
@Component
@RequiredArgsConstructor
public class AutomationCompositionElementHandler implements AutomationCompositionElementListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ParticipantIntermediaryApi intermediaryApi;

    @Getter
    @Setter
    private SimConfig config = new SimConfig();

    /**
     * Callback method to handle an update on a automation composition element.
     *
     * @param automationCompositionId the automationComposition Id
     * @param element the information on the automation composition element
     * @param properties properties Map
     * @throws PfModelException in case of a exception
     */
    @Override
    public void deploy(UUID automationCompositionId, AcElementDeploy element, Map<String, Object> properties)
            throws PfModelException {
        LOGGER.debug("deploy call");

        if (!execution(config.getDeployTimerMs(), "Current Thread deploy is Interrupted during execution {}",
                element.getId())) {
            return;
        }

        if (config.isDeploySuccess()) {
            intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, element.getId(),
                    DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Deployed");
        } else {
            intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, element.getId(),
                    DeployState.UNDEPLOYED, null, StateChangeResult.FAILED, "Deploy failed!");
        }
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
     * Handle a automation composition element state change.
     *
     * @param automationCompositionElementId the ID of the automation composition element
     */
    @Override
    public void undeploy(UUID automationCompositionId, UUID automationCompositionElementId) throws PfModelException {
        LOGGER.debug("undeploy call");

        if (!execution(config.getUndeployTimerMs(), "Current Thread undeploy is Interrupted during execution {}",
                automationCompositionElementId)) {
            return;
        }

        if (config.isUndeploySuccess()) {
            intermediaryApi.updateAutomationCompositionElementState(automationCompositionId,
                    automationCompositionElementId, DeployState.UNDEPLOYED, null, StateChangeResult.NO_ERROR,
                    "Undeployed");
        } else {
            intermediaryApi.updateAutomationCompositionElementState(automationCompositionId,
                    automationCompositionElementId, DeployState.DEPLOYED, null, StateChangeResult.FAILED,
                    "Undeploy failed!");
        }
    }

    @Override
    public void lock(UUID automationCompositionId, UUID automationCompositionElementId) throws PfModelException {
        LOGGER.debug("lock call");

        if (!execution(config.getLockTimerMs(), "Current Thread lock is Interrupted during execution {}",
                automationCompositionElementId)) {
            return;
        }

        if (config.isLockSuccess()) {
            intermediaryApi.updateAutomationCompositionElementState(automationCompositionId,
                    automationCompositionElementId, null, LockState.LOCKED, StateChangeResult.NO_ERROR, "Locked");
        } else {
            intermediaryApi.updateAutomationCompositionElementState(automationCompositionId,
                    automationCompositionElementId, null, LockState.UNLOCKED, StateChangeResult.FAILED, "Lock failed!");
        }
    }

    @Override
    public void unlock(UUID automationCompositionId, UUID automationCompositionElementId) throws PfModelException {
        LOGGER.debug("unlock call");

        if (!execution(config.getUnlockTimerMs(), "Current Thread unlock is Interrupted during execution {}",
                automationCompositionElementId)) {
            return;
        }

        if (config.isUnlockSuccess()) {
            intermediaryApi.updateAutomationCompositionElementState(automationCompositionId,
                    automationCompositionElementId, null, LockState.UNLOCKED, StateChangeResult.NO_ERROR, "Unlocked");
        } else {
            intermediaryApi.updateAutomationCompositionElementState(automationCompositionId,
                    automationCompositionElementId, null, LockState.LOCKED, StateChangeResult.FAILED, "Unlock failed!");
        }
    }

    @Override
    public void delete(UUID automationCompositionId, UUID automationCompositionElementId) throws PfModelException {
        LOGGER.debug("delete call");

        if (!execution(config.getDeleteTimerMs(), "Current Thread delete is Interrupted during execution {}",
                automationCompositionElementId)) {
            return;
        }

        if (config.isDeleteSuccess()) {
            intermediaryApi.updateAutomationCompositionElementState(automationCompositionId,
                    automationCompositionElementId, DeployState.DELETED, null, StateChangeResult.NO_ERROR, "Deleted");
        } else {
            intermediaryApi.updateAutomationCompositionElementState(automationCompositionId,
                    automationCompositionElementId, DeployState.UNDEPLOYED, null, StateChangeResult.FAILED,
                    "Delete failed!");
        }
    }

    @Override
    public void update(UUID automationCompositionId, AcElementDeploy element, Map<String, Object> properties)
            throws PfModelException {
        LOGGER.debug("update call");

        if (!execution(config.getUpdateTimerMs(), "Current Thread update is Interrupted during execution {}",
                element.getId())) {
            return;
        }

        if (config.isUpdateSuccess()) {
            intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, element.getId(),
                    DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Updated");
        } else {
            intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, element.getId(),
                    DeployState.DEPLOYED, null, StateChangeResult.FAILED, "Update failed!");
        }
    }

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
     * @param automationCompositionId the automationComposition Id
     * @param elementId the automationComposition Element Id
     * @param useState the useState
     * @param operationalState the operationalState
     * @param outProperties the outProperties
     */
    public void setOutProperties(UUID automationCompositionId, UUID elementId, String useState, String operationalState,
            Map<String, Object> outProperties) {
        intermediaryApi.sendAcElementInfo(automationCompositionId, elementId, useState, operationalState,
                outProperties);
    }

    @Override
    public void prime(UUID compositionId, List<AutomationCompositionElementDefinition> elementDefinitionList)
            throws PfModelException {
        LOGGER.debug("prime call");

        if (!execution(config.getPrimeTimerMs(), "Current Thread prime is Interrupted during execution {}",
                compositionId)) {
            return;
        }

        if (config.isPrimeSuccess()) {
            intermediaryApi.updateCompositionState(compositionId, AcTypeState.PRIMED, StateChangeResult.NO_ERROR,
                    "Primed");
        } else {
            intermediaryApi.updateCompositionState(compositionId, AcTypeState.COMMISSIONED, StateChangeResult.FAILED,
                    "Prime failed!");
        }
    }

    @Override
    public void deprime(UUID compositionId) throws PfModelException {
        LOGGER.debug("deprime call");

        if (!execution(config.getDeprimeTimerMs(), "Current Thread deprime is Interrupted during execution {}",
                compositionId)) {
            return;
        }

        if (config.isDeprimeSuccess()) {
            intermediaryApi.updateCompositionState(compositionId, AcTypeState.COMMISSIONED, StateChangeResult.NO_ERROR,
                    "Deprimed");
        } else {
            intermediaryApi.updateCompositionState(compositionId, AcTypeState.PRIMED, StateChangeResult.FAILED,
                    "Deprime failed!");
        }
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

    @Override
    public void handleRestartComposition(UUID compositionId,
            List<AutomationCompositionElementDefinition> elementDefinitionList, AcTypeState state)
            throws PfModelException {
        LOGGER.debug("restart composition definition call");
        switch (state) {
            case PRIMING:
                prime(compositionId, elementDefinitionList);
                break;

            case DEPRIMING:
                deprime(compositionId);
                break;

            default:
                intermediaryApi.updateCompositionState(compositionId, state, StateChangeResult.NO_ERROR, "Restarted");
        }
    }

    @Override
    public void handleRestartInstance(UUID automationCompositionId, AcElementDeploy element,
            Map<String, Object> properties, DeployState deployState, LockState lockState) throws PfModelException {
        LOGGER.debug("restart instance call");
        if (!AcmUtils.isInTransitionalState(deployState, lockState)) {
            intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, element.getId(),
                    deployState, lockState, StateChangeResult.NO_ERROR, "Restarted");
            return;
        }
        if (DeployState.DEPLOYING.equals(deployState)) {
            deploy(automationCompositionId, element, properties);
            return;
        }
        if (DeployState.UNDEPLOYING.equals(deployState)) {
            undeploy(automationCompositionId, element.getId());
            return;
        }
        if (DeployState.UPDATING.equals(deployState)) {
            update(automationCompositionId, element, properties);
            return;
        }
        if (DeployState.DELETING.equals(deployState)) {
            delete(automationCompositionId, element.getId());
            return;
        }
        if (LockState.LOCKING.equals(lockState)) {
            lock(automationCompositionId, element.getId());
            return;
        }
        if (LockState.UNLOCKING.equals(lockState)) {
            unlock(automationCompositionId, element.getId());
        }
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

    @Override
    public void migrate(UUID automationCompositionId, AcElementDeploy element, UUID compositionTargetId,
            Map<String, Object> properties) throws PfModelException {
        LOGGER.debug("migrate call");

        if (!execution(config.getMigrateTimerMs(), "Current Thread migrate is Interrupted during execution {}",
                element.getId())) {
            return;
        }

        if (config.isMigrateSuccess()) {
            intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, element.getId(),
                    DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Migrated");
        } else {
            intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, element.getId(),
                    DeployState.DEPLOYED, null, StateChangeResult.FAILED, "Migrate failed!");
        }
    }
}
