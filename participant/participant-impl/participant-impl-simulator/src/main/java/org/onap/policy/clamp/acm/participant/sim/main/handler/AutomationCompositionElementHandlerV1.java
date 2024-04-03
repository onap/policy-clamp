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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.acm.participant.intermediary.api.impl.AcElementListenerV1;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.models.base.PfModelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@ConditionalOnExpression("'${element.handler}'=='AcElementHandlerV1'")
@Component
public class AutomationCompositionElementHandlerV1 extends AcElementListenerV1 {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutomationCompositionElementHandlerV1.class);

    private final SimulatorService simulatorService;

    public AutomationCompositionElementHandlerV1(ParticipantIntermediaryApi intermediaryApi,
        SimulatorService simulatorService) {
        super(intermediaryApi);
        this.simulatorService = simulatorService;
    }

    @Override
    public void deploy(UUID instanceId, AcElementDeploy element, Map<String, Object> properties)
            throws PfModelException {
        LOGGER.debug("deploy call instanceId: {}, element: {}, properties: {}", instanceId, element, properties);
        simulatorService.deploy(instanceId, element.getId());
    }

    @Override
    public void undeploy(UUID instanceId, UUID elementId) throws PfModelException {
        LOGGER.debug("undeploy call instanceId: {}, elementId: {}", instanceId, elementId);
        simulatorService.undeploy(instanceId, elementId);
    }

    @Override
    public void lock(UUID instanceId, UUID elementId) throws PfModelException {
        LOGGER.debug("lock call instanceId: {}, elementId: {}", instanceId, elementId);
        simulatorService.lock(instanceId, elementId);
    }

    @Override
    public void unlock(UUID instanceId, UUID elementId) throws PfModelException {
        LOGGER.debug("unlock call instanceId: {}, elementId: {}", instanceId, elementId);
        simulatorService.unlock(instanceId, elementId);
    }

    @Override
    public void delete(UUID instanceId, UUID elementId) throws PfModelException {
        LOGGER.debug("delete call instanceId: {}, elementId: {}", instanceId, elementId);
        simulatorService.delete(instanceId, elementId);
    }

    @Override
    public void update(UUID instanceId, AcElementDeploy element, Map<String, Object> properties)
            throws PfModelException {
        LOGGER.debug("update call instanceId: {}, element: {}, properties: {}", instanceId, element, properties);
        simulatorService.update(instanceId, element.getId());
    }

    @Override
    public void prime(UUID compositionId, List<AutomationCompositionElementDefinition> elementDefinitionList)
            throws PfModelException {
        LOGGER.debug("prime call compositionId: {}, elementDefinitionList: {}", compositionId, elementDefinitionList);
        simulatorService.prime(compositionId);
    }

    @Override
    public void deprime(UUID compositionId) throws PfModelException {
        LOGGER.debug("deprime call compositionId: {}", compositionId);
        simulatorService.deprime(compositionId);
    }

    @Override
    public void handleRestartComposition(UUID compositionId,
        List<AutomationCompositionElementDefinition> elementDefinitionList, AcTypeState state)
            throws PfModelException {
        LOGGER.debug("restart composition definition call compositionId: {}, elementDefinitionList: {}, state: {}",
                compositionId, elementDefinitionList, state);

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
    public void handleRestartInstance(UUID instanceId, AcElementDeploy element,
        Map<String, Object> properties, DeployState deployState, LockState lockState) throws PfModelException {
        LOGGER.debug("restart instance call instanceId: {}, element: {}, properties: {},"
                + "deployState: {}, lockState: {}", instanceId, element, properties, deployState, lockState);

        if (!AcmUtils.isInTransitionalState(deployState, lockState)) {
            intermediaryApi.updateAutomationCompositionElementState(instanceId, element.getId(),
                    deployState, lockState, StateChangeResult.NO_ERROR, "Restarted");
            return;
        }
        if (DeployState.DEPLOYING.equals(deployState)) {
            deploy(instanceId, element, properties);
            return;
        }
        if (DeployState.UNDEPLOYING.equals(deployState)) {
            undeploy(instanceId, element.getId());
            return;
        }
        if (DeployState.UPDATING.equals(deployState)) {
            update(instanceId, element, properties);
            return;
        }
        if (DeployState.DELETING.equals(deployState)) {
            delete(instanceId, element.getId());
            return;
        }
        if (LockState.LOCKING.equals(lockState)) {
            lock(instanceId, element.getId());
            return;
        }
        if (LockState.UNLOCKING.equals(lockState)) {
            unlock(instanceId, element.getId());
        }
    }

    @Override
    public void migrate(UUID instanceId, AcElementDeploy element, UUID compositionTargetId,
        Map<String, Object> properties) throws PfModelException {
        LOGGER.debug("migrate call instanceId: {}, element: {}, compositionTargetId: {}, properties: {}",
                instanceId, element, compositionTargetId, properties);
        simulatorService.migrate(instanceId, element.getId());
    }
}
