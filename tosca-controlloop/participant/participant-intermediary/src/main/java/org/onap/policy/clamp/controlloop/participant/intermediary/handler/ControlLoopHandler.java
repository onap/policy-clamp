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

package org.onap.policy.clamp.controlloop.participant.intermediary.handler;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.collections4.CollectionUtils;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ClElementStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoops;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantControlLoopStateChange;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantControlLoopUpdate;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantResponseDetails;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantResponseStatus;
import org.onap.policy.clamp.controlloop.participant.intermediary.dispatcher.MessageDispatcher;
import org.onap.policy.clamp.controlloop.participant.intermediary.parameters.ParticipantIntermediaryParameters;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This class is responsible for managing the state of all control loops in the participant.
 */
public class ControlLoopHandler implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ControlLoopHandler.class);

    private ToscaConceptIdentifier participantId = null;
    private MessageDispatcher dispatcher = null;

    private Map<ToscaConceptIdentifier, ControlLoop> controlLoopMap = new LinkedHashMap<>();

    public ControlLoopHandler() {
    }

    /**
     * Constructor, set the participant ID and dispatcher.
     *
     * @param parameters the parameters of the participant
     * @param dispatcher the dispatcher for sending responses to messages
     */
    public ControlLoopHandler(ParticipantIntermediaryParameters parameters, MessageDispatcher dispatcher) {
        this.participantId = parameters.getParticipantId();
        this.dispatcher = dispatcher;
    }

    @Override
    public void close() {
        // No explicit action on this class
    }

    /**
     * Handle a control loop element state change message.
     *
     * @param id controlloop element id
     * @param state the updated state
     * @return controlLoopElement the updated controlloop element
     */
    public ControlLoopElement updateControlLoopElementState(UUID id, ControlLoopOrderedState state) {
        if (id == null) {
            return null;
        }

        for (ControlLoop controlLoop : getControlLoops().getControlLoopList()) {
            for (ControlLoopElement element : controlLoop.getElements()) {
                if (id.equals(element.getId())) {
                    element.setOrderedState(state);
                    LOGGER.debug("Control loop element {} ordered state changed to {}", id, state);
                    ParticipantResponseDetails response = new ParticipantResponseDetails();
                    handleState(controlLoop, response, state);
                    dispatcher.dispatchResponse(response);
                    return element;
                }
            }
        }
        return null;
    }

    public void updateControlLoopElementStatistics(ClElementStatistics elementStatistics) {
        // TODO Handle statistics coming from a participant implementation
    }

    /**
     * Handle a control loop state change message.
     *
     * @param definition controlloop id
     * @param state the updated state
     * @return controlLoop the updated controlloop
     */
    public ControlLoop updateControlLoopState(ToscaConceptIdentifier definition, ControlLoopOrderedState state) {
        if (definition == null) {
            return null;
        }

        ControlLoop controlLoop = controlLoopMap.get(definition);
        if (controlLoop == null) {
            LOGGER.debug("Control loop {} does not use this participant", definition.getName());
            return null;
        }

        ParticipantResponseDetails response = new ParticipantResponseDetails();
        handleState(controlLoop, response, state);
        dispatcher.dispatchResponse(response);
        return controlLoop;
    }

    /**
     * Handle a control loop state change message.
     *
     * @param stateChangeMsg the state change message
     */
    public void handleControlLoopStateChange(ParticipantControlLoopStateChange stateChangeMsg) {
        if (stateChangeMsg.getControlLoopId() == null) {
            return;
        }

        ControlLoop controlLoop = controlLoopMap.get(stateChangeMsg.getControlLoopId());

        if (controlLoop == null) {
            LOGGER.debug("Control loop {} does not use this participant", stateChangeMsg.getControlLoopId());
            return;
        }

        ParticipantResponseDetails response = new ParticipantResponseDetails(stateChangeMsg);
        handleState(controlLoop, response, stateChangeMsg.getOrderedState());
        dispatcher.dispatchResponse(response);
    }

    /**
     * Method to handle state changes.
     *
     * @param controlLoop participant response
     * @param response participant response
     * @param state controlloop ordered state
     */
    private void handleState(final ControlLoop controlLoop, final ParticipantResponseDetails response,
            ControlLoopOrderedState state) {
        switch (state) {
            case UNINITIALISED:
                handleUninitialisedState(controlLoop, response);
                break;
            case PASSIVE:
                handlePassiveState(controlLoop, response);
                break;
            case RUNNING:
                handleRunningState(controlLoop, response);
                break;
            default:
                break;
        }
    }

    /**
     * Handle a control loop update message.
     *
     * @param updateMsg the update message
     */
    public void handleControlLoopUpdate(ParticipantControlLoopUpdate updateMsg) {
        if (!updateMsg.appliesTo(participantId)) {
            return;
        }

        ControlLoop controlLoop = controlLoopMap.get(updateMsg.getControlLoopId());

        ParticipantResponseDetails response = new ParticipantResponseDetails(updateMsg);

        if (controlLoop != null) {
            response.setResponseStatus(ParticipantResponseStatus.FAIL);
            response.setResponseMessage("Control loop " + updateMsg.getControlLoopId()
                    + " already defined on participant " + participantId);

            dispatcher.dispatchResponse(response);
            return;
        }

        controlLoop = updateMsg.getControlLoop();
        controlLoop.getElements().removeIf(element -> participantId.equals(element.getParticipantId()));

        controlLoopMap.put(updateMsg.getControlLoopId(), controlLoop);

        response.setResponseStatus(ParticipantResponseStatus.SUCCESS);
        response.setResponseMessage(
                "Control loop " + updateMsg.getControlLoopId() + " defined on participant " + participantId);

        dispatcher.dispatchResponse(response);
    }

    /**
     * Method to handle when the new state from participant is UNINITIALISED state.
     *
     * @param controlLoop participant response
     * @param response participant response
     */
    private void handleUninitialisedState(final ControlLoop controlLoop, final ParticipantResponseDetails response) {
        handleStateChange(controlLoop, ControlLoopState.UNINITIALISED, response);
        controlLoopMap.remove(controlLoop.getKey().asIdentifier());
    }

    /**
     * Method to handle when the new state from participant is PASSIVE state.
     *
     * @param controlLoop participant response
     * @param response participant response
     */
    private void handlePassiveState(final ControlLoop controlLoop, final ParticipantResponseDetails response) {
        handleStateChange(controlLoop, ControlLoopState.PASSIVE, response);
    }

    /**
     * Method to handle when the new state from participant is RUNNING state.
     *
     * @param controlLoop participant response
     * @param response participant response
     */
    private void handleRunningState(final ControlLoop controlLoop, final ParticipantResponseDetails response) {
        handleStateChange(controlLoop, ControlLoopState.RUNNING, response);
    }

    /**
     * Method to update the state of control loop elements.
     *
     * @param controlLoop participant status in memory
     * @param state new state of the control loop elements
     */
    private void handleStateChange(ControlLoop controlLoop, ControlLoopState newState,
            ParticipantResponseDetails response) {

        if (newState.equals(controlLoop.getState())) {
            response.setResponseStatus(ParticipantResponseStatus.SUCCESS);
            response.setResponseMessage("Control loop is already in state " + newState);
            return;
        }

        if (!CollectionUtils.isEmpty(controlLoop.getElements())) {
            controlLoop.getElements().forEach(element -> element.setState(newState));
        }

        response.setResponseStatus(ParticipantResponseStatus.SUCCESS);
        response.setResponseMessage("ControlLoop state changed from " + controlLoop.getState() + " to " + newState);
        controlLoop.setState(newState);
    }

    /**
     * Get control loops as a {@link ConrolLoops} class.
     *
     * @return the control loops
     */
    public ControlLoops getControlLoops() {
        ControlLoops controlLoops = new ControlLoops();
        controlLoops.setControlLoopList(new ArrayList<>(controlLoopMap.values()));
        return controlLoops;
    }
}
