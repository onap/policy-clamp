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

package org.onap.policy.clamp.acm.participant.sim.main.handler;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.InstanceElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.acm.participant.intermediary.api.impl.AcElementListenerV2;
import org.onap.policy.clamp.acm.participant.sim.model.InternalData;
import org.onap.policy.clamp.acm.participant.sim.model.InternalDatas;
import org.onap.policy.clamp.acm.participant.sim.model.SimConfig;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
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
public class AutomationCompositionElementHandler extends AcElementListenerV2 {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Getter
    @Setter
    private SimConfig config = new SimConfig();

    public AutomationCompositionElementHandler(ParticipantIntermediaryApi intermediaryApi) {
        super(intermediaryApi);
    }

    /**
     * Handle a deploy on a automation composition element.
     *
     * @param compositionElement the information of the Automation Composition Definition Element
     * @param instanceElement the information of the Automation Composition Instance Element
     * @throws PfModelException from Policy framework
     */
    @Override
    public void deploy(CompositionElementDto compositionElement, InstanceElementDto instanceElement)
            throws PfModelException {
        LOGGER.debug("deploy call compositionElement: {}, instanceElement: {}", compositionElement, instanceElement);

        if (!execution(config.getDeployTimerMs(), "Current Thread deploy is Interrupted during execution {}",
            instanceElement.elementId())) {
            return;
        }

        if (config.isDeploySuccess()) {
            intermediaryApi.updateAutomationCompositionElementState(instanceElement.instanceId(),
                instanceElement.elementId(), DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR,
                "Deployed");
        } else {
            intermediaryApi.updateAutomationCompositionElementState(instanceElement.instanceId(),
                instanceElement.elementId(), DeployState.UNDEPLOYED, null, StateChangeResult.FAILED,
                "Deploy failed!");
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
     * @param compositionElement the information of the Automation Composition Definition Element
     * @param instanceElement the information of the Automation Composition Instance Element
     * @throws PfModelException from Policy framework
     */
    @Override
    public void undeploy(CompositionElementDto compositionElement, InstanceElementDto instanceElement)
        throws PfModelException {
        LOGGER.debug("undeploy call compositionElement: {}, instanceElement: {}", compositionElement, instanceElement);

        if (!execution(config.getUndeployTimerMs(), "Current Thread undeploy is Interrupted during execution {}",
            instanceElement.elementId())) {
            return;
        }

        if (config.isUndeploySuccess()) {
            intermediaryApi.updateAutomationCompositionElementState(instanceElement.instanceId(),
                instanceElement.elementId(), DeployState.UNDEPLOYED, null, StateChangeResult.NO_ERROR,
                    "Undeployed");
        } else {
            intermediaryApi.updateAutomationCompositionElementState(instanceElement.instanceId(),
                instanceElement.elementId(), DeployState.DEPLOYED, null, StateChangeResult.FAILED,
                    "Undeploy failed!");
        }
    }

    @Override
    public void lock(CompositionElementDto compositionElement, InstanceElementDto instanceElement)
        throws PfModelException {
        LOGGER.debug("lock call compositionElement: {}, instanceElement: {}", compositionElement, instanceElement);

        if (!execution(config.getLockTimerMs(), "Current Thread lock is Interrupted during execution {}",
            instanceElement.elementId())) {
            return;
        }

        if (config.isLockSuccess()) {
            intermediaryApi.updateAutomationCompositionElementState(instanceElement.instanceId(),
                instanceElement.elementId(), null, LockState.LOCKED, StateChangeResult.NO_ERROR, "Locked");
        } else {
            intermediaryApi.updateAutomationCompositionElementState(instanceElement.instanceId(),
                instanceElement.elementId(), null, LockState.UNLOCKED, StateChangeResult.FAILED, "Lock failed!");
        }
    }

    @Override
    public void unlock(CompositionElementDto compositionElement, InstanceElementDto instanceElement)
        throws PfModelException {
        LOGGER.debug("unlock call compositionElement: {}, instanceElement: {}", compositionElement, instanceElement);

        if (!execution(config.getUnlockTimerMs(), "Current Thread unlock is Interrupted during execution {}",
            instanceElement.elementId())) {
            return;
        }

        if (config.isUnlockSuccess()) {
            intermediaryApi.updateAutomationCompositionElementState(instanceElement.instanceId(),
                instanceElement.elementId(), null, LockState.UNLOCKED, StateChangeResult.NO_ERROR, "Unlocked");
        } else {
            intermediaryApi.updateAutomationCompositionElementState(instanceElement.instanceId(),
                instanceElement.elementId(), null, LockState.LOCKED, StateChangeResult.FAILED, "Unlock failed!");
        }
    }

    @Override
    public void delete(CompositionElementDto compositionElement, InstanceElementDto instanceElement)
        throws PfModelException {
        LOGGER.debug("delete call compositionElement: {}, instanceElement: {}", compositionElement, instanceElement);

        if (!execution(config.getDeleteTimerMs(), "Current Thread delete is Interrupted during execution {}",
            instanceElement.elementId())) {
            return;
        }

        if (config.isDeleteSuccess()) {
            intermediaryApi.updateAutomationCompositionElementState(instanceElement.instanceId(),
                instanceElement.elementId(), DeployState.DELETED, null, StateChangeResult.NO_ERROR, "Deleted");
        } else {
            intermediaryApi.updateAutomationCompositionElementState(instanceElement.instanceId(),
                instanceElement.elementId(), DeployState.UNDEPLOYED, null, StateChangeResult.FAILED,
                "Delete failed!");
        }
    }

    @Override
    public void update(CompositionElementDto compositionElement, InstanceElementDto instanceElement,
                       InstanceElementDto instanceElementUpdated) throws PfModelException {
        LOGGER.debug("update call compositionElement: {}, instanceElement: {}, instanceElementUpdated: {}",
            compositionElement, instanceElement, instanceElementUpdated);

        if (!execution(config.getUpdateTimerMs(), "Current Thread update is Interrupted during execution {}",
            instanceElement.elementId())) {
            return;
        }

        if (config.isUpdateSuccess()) {
            intermediaryApi.updateAutomationCompositionElementState(
                instanceElement.instanceId(), instanceElement.elementId(),
                DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Updated");
        } else {
            intermediaryApi.updateAutomationCompositionElementState(
                instanceElement.instanceId(), instanceElement.elementId(),
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
    public void prime(CompositionDto composition) throws PfModelException {
        LOGGER.debug("prime call composition: {}", composition);

        if (!execution(config.getPrimeTimerMs(), "Current Thread prime is Interrupted during execution {}",
            composition.compositionId())) {
            return;
        }

        if (config.isPrimeSuccess()) {
            intermediaryApi.updateCompositionState(composition.compositionId(),
                AcTypeState.PRIMED, StateChangeResult.NO_ERROR, "Primed");
        } else {
            intermediaryApi.updateCompositionState(composition.compositionId(),
                AcTypeState.COMMISSIONED, StateChangeResult.FAILED, "Prime failed!");
        }
    }

    @Override
    public void deprime(CompositionDto composition) throws PfModelException {
        LOGGER.debug("deprime call composition: {}", composition);

        if (!execution(config.getDeprimeTimerMs(), "Current Thread deprime is Interrupted during execution {}",
            composition.compositionId())) {
            return;
        }

        if (config.isDeprimeSuccess()) {
            intermediaryApi.updateCompositionState(composition.compositionId(), AcTypeState.COMMISSIONED,
                StateChangeResult.NO_ERROR, "Deprimed");
        } else {
            intermediaryApi.updateCompositionState(composition.compositionId(), AcTypeState.PRIMED,
                StateChangeResult.FAILED, "Deprime failed!");
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
                data.setCompositionDefinitionElementId(element.getDefinition());
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
    public void handleRestartComposition(CompositionDto composition, AcTypeState state) throws PfModelException {
        LOGGER.debug("restart composition definition call");
        switch (state) {
            case PRIMING:
                prime(composition);
                break;

            case DEPRIMING:
                deprime(composition);
                break;

            default:
                intermediaryApi.updateCompositionState(composition.compositionId(), state,
                    StateChangeResult.NO_ERROR, "Restarted");
        }
    }

    @Override
    public void handleRestartInstance(CompositionElementDto compositionElement, InstanceElementDto instanceElement,
        DeployState deployState, LockState lockState) throws PfModelException {
        LOGGER.debug("restart instance call");
        if (!AcmUtils.isInTransitionalState(deployState, lockState)) {
            intermediaryApi.updateAutomationCompositionElementState(
                instanceElement.instanceId(), instanceElement.elementId(), deployState, lockState,
                StateChangeResult.NO_ERROR, "Restarted");
            return;
        }
        if (DeployState.DEPLOYING.equals(deployState)) {
            deploy(compositionElement, instanceElement);
            return;
        }
        if (DeployState.UNDEPLOYING.equals(deployState)) {
            undeploy(compositionElement, instanceElement);
            return;
        }
        if (DeployState.UPDATING.equals(deployState)) {
            update(compositionElement, instanceElement, instanceElement);
            return;
        }
        if (DeployState.DELETING.equals(deployState)) {
            delete(compositionElement, instanceElement);
            return;
        }
        if (LockState.LOCKING.equals(lockState)) {
            lock(compositionElement, instanceElement);
            return;
        }
        if (LockState.UNLOCKING.equals(lockState)) {
            unlock(compositionElement, instanceElement);
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
    public void migrate(CompositionElementDto compositionElement, CompositionElementDto compositionElementTarget,
                        InstanceElementDto instanceElement, InstanceElementDto instanceElementMigrate)
        throws PfModelException {
        LOGGER.debug("migrate call compositionElement: {}, compositionElementTarget: {}, instanceElement: {},"
                + " instanceElementMigrate: {}",
            compositionElement, compositionElementTarget, instanceElement, instanceElementMigrate);

        if (!execution(config.getMigrateTimerMs(), "Current Thread migrate is Interrupted during execution {}",
            instanceElement.elementId())) {
            return;
        }

        if (config.isMigrateSuccess()) {
            intermediaryApi.updateAutomationCompositionElementState(
                instanceElement.instanceId(), instanceElement.elementId(),
                DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Migrated");
        } else {
            intermediaryApi.updateAutomationCompositionElementState(
                instanceElement.instanceId(), instanceElement.elementId(),
                DeployState.DEPLOYED, null, StateChangeResult.FAILED, "Migrate failed!");
        }
    }
}
