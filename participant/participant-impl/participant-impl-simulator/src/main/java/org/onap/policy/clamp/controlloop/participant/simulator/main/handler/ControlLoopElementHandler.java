/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.participant.simulator.main.handler;

import java.time.Instant;
import java.util.UUID;
import lombok.Setter;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ClElementStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ControlLoopElementListener;
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class handles implementation of controlLoopElement updates.
 */
@Component
public class ControlLoopElementHandler implements ControlLoopElementListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControlLoopElementHandler.class);

    @Setter
    private ParticipantIntermediaryApi intermediaryApi;

    @Override
    public void controlLoopElementStateChange(UUID controlLoopElementId, ControlLoopState currentState,
            ControlLoopOrderedState newState) throws PfModelException {
        switch (newState) {
            case UNINITIALISED:
                break;
            case PASSIVE:
                intermediaryApi.updateControlLoopElementState(controlLoopElementId, newState, ControlLoopState.PASSIVE,
                    ParticipantMessageType.CONTROL_LOOP_STATE_CHANGE);
                break;
            case RUNNING:
                intermediaryApi.updateControlLoopElementState(controlLoopElementId, newState, ControlLoopState.RUNNING,
                    ParticipantMessageType.CONTROL_LOOP_STATE_CHANGE);
                break;
            default:
                LOGGER.debug("Unknown orderedstate {}", newState);
                break;
        }
    }

    @Override
    public void controlLoopElementUpdate(ControlLoopElement element, ToscaNodeTemplate controlLoopElementDefinition)
            throws PfModelException {
        intermediaryApi.updateControlLoopElementState(element.getId(), element.getOrderedState(),
                ControlLoopState.PASSIVE, ParticipantMessageType.CONTROL_LOOP_UPDATE);
    }

    @Override
    public void handleStatistics(UUID controlLoopElementId) throws PfModelException {
        var clElement = intermediaryApi.getControlLoopElement(controlLoopElementId);
        if (clElement != null) {
            var clElementStatistics = new ClElementStatistics();
            clElementStatistics.setControlLoopState(clElement.getState());
            clElementStatistics.setTimeStamp(Instant.now());
            intermediaryApi.updateControlLoopElementStatistics(controlLoopElementId, clElementStatistics);
        }
    }

}
