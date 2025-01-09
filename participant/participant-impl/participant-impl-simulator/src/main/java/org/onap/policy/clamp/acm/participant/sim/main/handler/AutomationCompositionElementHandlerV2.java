/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2025 Nordix Foundation.
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

import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.ElementState;
import org.onap.policy.clamp.acm.participant.intermediary.api.InstanceElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.acm.participant.intermediary.api.impl.AcElementListenerV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * This class handles implementation of automationCompositionElement updates.
 */
@ConditionalOnExpression("'${element.handler}'=='AcElementHandlerV2'")
@Component
public class AutomationCompositionElementHandlerV2 extends AcElementListenerV2 {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutomationCompositionElementHandlerV2.class);

    private final SimulatorService simulatorService;

    public AutomationCompositionElementHandlerV2(ParticipantIntermediaryApi intermediaryApi,
        SimulatorService simulatorService) {
        super(intermediaryApi);
        this.simulatorService = simulatorService;
    }

    /**
     * Handle a deploy on a automation composition element.
     *
     * @param compositionElement the information of the Automation Composition Definition Element
     * @param instanceElement the information of the Automation Composition Instance Element
     */
    @Override
    public void deploy(CompositionElementDto compositionElement, InstanceElementDto instanceElement) {
        LOGGER.debug("deploy call compositionElement: {}, instanceElement: {}", compositionElement, instanceElement);
        simulatorService.deploy(instanceElement.instanceId(), instanceElement.elementId(),
                instanceElement.outProperties());
    }

    /**
     * Handle a automation composition element state change.
     *
     * @param compositionElement the information of the Automation Composition Definition Element
     * @param instanceElement the information of the Automation Composition Instance Element
     */
    @Override
    public void undeploy(CompositionElementDto compositionElement, InstanceElementDto instanceElement) {
        LOGGER.debug("undeploy call compositionElement: {}, instanceElement: {}", compositionElement, instanceElement);
        simulatorService.undeploy(instanceElement.instanceId(), instanceElement.elementId(),
                instanceElement.outProperties());
    }

    @Override
    public void lock(CompositionElementDto compositionElement, InstanceElementDto instanceElement) {
        LOGGER.debug("lock call compositionElement: {}, instanceElement: {}", compositionElement, instanceElement);
        simulatorService.lock(instanceElement.instanceId(), instanceElement.elementId());
    }

    @Override
    public void unlock(CompositionElementDto compositionElement, InstanceElementDto instanceElement) {
        LOGGER.debug("unlock call compositionElement: {}, instanceElement: {}", compositionElement, instanceElement);
        simulatorService.unlock(instanceElement.instanceId(), instanceElement.elementId());
    }

    @Override
    public void delete(CompositionElementDto compositionElement, InstanceElementDto instanceElement) {
        LOGGER.debug("delete call compositionElement: {}, instanceElement: {}", compositionElement, instanceElement);
        simulatorService.delete(instanceElement.instanceId(), instanceElement.elementId());
    }

    @Override
    public void update(CompositionElementDto compositionElement, InstanceElementDto instanceElement,
                       InstanceElementDto instanceElementUpdated) {
        LOGGER.debug("update call compositionElement: {}, instanceElement: {}, instanceElementUpdated: {}",
                compositionElement, instanceElement, instanceElementUpdated);
        simulatorService.update(instanceElement.instanceId(), instanceElement.elementId());
    }

    @Override
    public void prime(CompositionDto composition) {
        LOGGER.debug("prime call composition: {}", composition);
        simulatorService.prime(composition);
    }

    @Override
    public void deprime(CompositionDto composition) {
        LOGGER.debug("deprime call composition: {}", composition);
        simulatorService.deprime(composition);
    }

    @Override
    public void migrate(CompositionElementDto compositionElement, CompositionElementDto compositionElementTarget,
                        InstanceElementDto instanceElement, InstanceElementDto instanceElementMigrate) {
        LOGGER.debug("migrate call compositionElement: {}, compositionElementTarget: {}, instanceElement: {},"
                        + " instanceElementMigrate: {}",
                compositionElement, compositionElementTarget, instanceElement, instanceElementMigrate);

        if (ElementState.NEW.equals(instanceElementMigrate.state())) {
            LOGGER.debug("new element scenario");

        }
        if (ElementState.REMOVED.equals(instanceElementMigrate.state())) {
            simulatorService.undeploy(instanceElement.instanceId(), instanceElement.elementId(),
                    instanceElement.outProperties());
            simulatorService.delete(instanceElement.instanceId(), instanceElement.elementId());
        } else {
            simulatorService.migrate(instanceElement.instanceId(), instanceElement.elementId(),
                    0, compositionElementTarget.inProperties(), instanceElementMigrate.outProperties());
        }
    }

    @Override
    public void migratePrecheck(CompositionElementDto compositionElement,
                                CompositionElementDto compositionElementTarget, InstanceElementDto instanceElement,
                                InstanceElementDto instanceElementMigrate) {
        LOGGER.debug("migrate precheck call compositionElement: {}, compositionElementTarget: {}, instanceElement: {},"
                        + " instanceElementMigrate: {}",
                compositionElement, compositionElementTarget, instanceElement, instanceElementMigrate);
        simulatorService.migratePrecheck(instanceElement.instanceId(), instanceElement.elementId());
    }

    @Override
    public void prepare(CompositionElementDto compositionElement, InstanceElementDto instanceElement) {
        LOGGER.debug("prepare call compositionElement: {}, instanceElement: {}", compositionElement, instanceElement);
        simulatorService.prepare(instanceElement.instanceId(), instanceElement.elementId());
    }

    @Override
    public void review(CompositionElementDto compositionElement, InstanceElementDto instanceElement) {
        LOGGER.debug("review call compositionElement: {}, instanceElement: {}", compositionElement, instanceElement);
        simulatorService.review(instanceElement.instanceId(), instanceElement.elementId());
    }
}
