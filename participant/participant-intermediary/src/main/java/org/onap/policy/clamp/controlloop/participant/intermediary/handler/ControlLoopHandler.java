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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ControlLoopElementListener;
import org.onap.policy.clamp.controlloop.participant.intermediary.comm.MessageSender;
import org.onap.policy.clamp.controlloop.participant.intermediary.parameters.ParticipantIntermediaryParameters;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This class is responsible for managing the state of all control loops in the participant.
 */
@NoArgsConstructor
public class ControlLoopHandler implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ControlLoopHandler.class);

    private ToscaConceptIdentifier participantType = null;
    private ToscaConceptIdentifier participantId = null;
    private MessageSender messageSender = null;

    private final Map<ToscaConceptIdentifier, ControlLoop> controlLoopMap = new LinkedHashMap<>();

    private final Map<UUID, ControlLoopElement> elementsOnThisParticipant = new LinkedHashMap<>();

    @Getter
    private List<ControlLoopElementListener> listeners = new ArrayList<>();

    /**
     * Constructor, set the participant ID and messageSender.
     *
     * @param parameters the parameters of the participant
     * @param messageSender the messageSender for sending responses to messages
     */
    public ControlLoopHandler(ParticipantIntermediaryParameters parameters, MessageSender messageSender) {
        this.participantType = parameters.getParticipantType();
        this.participantId = parameters.getParticipantId();
        this.messageSender = messageSender;
    }

    @Override
    public void close() {
        // No explicit action on this class
    }

    public void registerControlLoopElementListener(ControlLoopElementListener listener) {
        listeners.add(listener);
    }

    /**
     * Handle a control loop element state change message.
     *
     * @param id controlloop element id
     * @param orderedState the current state
     * @param newState the ordered state
     * @return controlLoopElement the updated controlloop element
     */
    public ControlLoopElement updateControlLoopElementState(UUID id, ControlLoopOrderedState orderedState,
            ControlLoopState newState) {

        if (id == null) {
            return null;
        }

        ControlLoopElement clElement = elementsOnThisParticipant.get(id);
        if (clElement != null) {
            clElement.setOrderedState(orderedState);
            clElement.setState(newState);
            LOGGER.debug("Control loop element {} state changed to {}", id, newState);
            var response = new ParticipantResponseDetails();
            response.setResponseStatus(ParticipantResponseStatus.SUCCESS);
            response.setResponseMessage("ControlLoopElement state changed to {} " + newState);
            messageSender.sendResponse(response);
            return clElement;
        }

        return null;
    }

    /**
     * Handle a control loop element statistics.
     *
     * @param id controlloop element id
     * @param elementStatistics control loop element Statistics
     */
    public void updateControlLoopElementStatistics(UUID id, ClElementStatistics elementStatistics) {
        ControlLoopElement clElement = elementsOnThisParticipant.get(id);
        if (clElement != null) {
            elementStatistics.setParticipantId(participantId);
            elementStatistics.setId(id);
            clElement.setClElementStatistics(elementStatistics);
        }
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

        var controlLoop = controlLoopMap.get(stateChangeMsg.getControlLoopId());

        if (controlLoop == null) {
            LOGGER.debug("Control loop {} does not use this participant", stateChangeMsg.getControlLoopId());
            return;
        }

        var response = new ParticipantResponseDetails(stateChangeMsg);
        handleState(controlLoop, response, stateChangeMsg.getOrderedState());
        messageSender.sendResponse(response);
    }

    /**
     * Method to handle state changes.
     *
     * @param controlLoop participant response
     * @param response participant response
     * @param orderedState controlloop ordered state
     */
    private void handleState(final ControlLoop controlLoop, final ParticipantResponseDetails response,
            ControlLoopOrderedState orderedState) {
        switch (orderedState) {
            case UNINITIALISED:
                handleUninitialisedState(controlLoop, orderedState, response);
                break;
            case PASSIVE:
                handlePassiveState(controlLoop, orderedState, response);
                break;
            case RUNNING:
                handleRunningState(controlLoop, orderedState, response);
                break;
            default:
                LOGGER.debug("StateChange message has no state, state is null {}", controlLoop.getDefinition());
                break;
        }
    }

    /**
     * Handle a control loop update message.
     *
     * @param updateMsg the update message
     */
    public void handleControlLoopUpdate(ParticipantControlLoopUpdate updateMsg) {

        if (!updateMsg.appliesTo(participantType, participantId)) {
            return;
        }

        var controlLoop = controlLoopMap.get(updateMsg.getControlLoopId());

        var response = new ParticipantResponseDetails(updateMsg);

        // TODO: Updates to existing ControlLoops are not supported yet (Addition/Removal of ControlLoop
        // elements to existing ControlLoop has to be supported).
        if (controlLoop != null) {
            response.setResponseStatus(ParticipantResponseStatus.FAIL);
            response.setResponseMessage("Control loop " + updateMsg.getControlLoopId()
                    + " already defined on participant " + participantId);

            messageSender.sendResponse(response);
            return;
        }

        controlLoop = updateMsg.getControlLoop();
        controlLoop.getElements().values().removeIf(element -> !participantType.equals(element.getParticipantType()));

        controlLoopMap.put(updateMsg.getControlLoopId(), controlLoop);
        for (ControlLoopElement element : updateMsg.getControlLoop().getElements().values()) {
            element.setState(element.getOrderedState().asState());
            element.setParticipantId(participantId);
            elementsOnThisParticipant.put(element.getId(), element);
        }

        for (ControlLoopElementListener clElementListener : listeners) {
            try {
                for (ControlLoopElement element : updateMsg.getControlLoop().getElements().values()) {
                    clElementListener.controlLoopElementUpdate(element, updateMsg.getControlLoopDefinition());
                }
            } catch (PfModelException e) {
                LOGGER.debug("Control loop element update failed {}", updateMsg.getControlLoopId());
            }
        }

        response.setResponseStatus(ParticipantResponseStatus.SUCCESS);
        response.setResponseMessage(
                "Control loop " + updateMsg.getControlLoopId() + " defined on participant " + participantId);

        messageSender.sendResponse(response);
    }

    /**
     * Method to handle when the new state from participant is UNINITIALISED state.
     *
     * @param controlLoop participant response
     * @param orderedState orderedState
     * @param response participant response
     */
    private void handleUninitialisedState(final ControlLoop controlLoop, final ControlLoopOrderedState orderedState,
            final ParticipantResponseDetails response) {
        handleStateChange(controlLoop, orderedState, ControlLoopState.UNINITIALISED, response);
        controlLoopMap.remove(controlLoop.getKey().asIdentifier());

        for (ControlLoopElementListener clElementListener : listeners) {
            try {
                for (ControlLoopElement element : controlLoop.getElements().values()) {
                    clElementListener.controlLoopElementStateChange(element.getId(), element.getState(), orderedState);
                }
            } catch (PfModelException e) {
                LOGGER.debug("Control loop element update failed {}", controlLoop.getDefinition());
            }
        }
    }

    /**
     * Method to handle when the new state from participant is PASSIVE state.
     *
     * @param controlLoop participant response
     * @param orderedState orderedState
     * @param response participant response
     */
    private void handlePassiveState(final ControlLoop controlLoop, final ControlLoopOrderedState orderedState,
            final ParticipantResponseDetails response) {
        handleStateChange(controlLoop, orderedState, ControlLoopState.PASSIVE, response);
    }

    /**
     * Method to handle when the new state from participant is RUNNING state.
     *
     * @param controlLoop participant response
     * @param orderedState orderedState
     * @param response participant response
     */
    private void handleRunningState(final ControlLoop controlLoop, final ControlLoopOrderedState orderedState,
            final ParticipantResponseDetails response) {
        handleStateChange(controlLoop, orderedState, ControlLoopState.RUNNING, response);
    }

    /**
     * Method to update the state of control loop elements.
     *
     * @param controlLoop participant status in memory
     * @param orderedState orderedState the new ordered state the participant should have
     * @param newState new state of the control loop elements
     * @param response the response to the state change request
     */
    private void handleStateChange(ControlLoop controlLoop, final ControlLoopOrderedState orderedState,
            ControlLoopState newState, ParticipantResponseDetails response) {

        if (orderedState.equals(controlLoop.getOrderedState())) {
            response.setResponseStatus(ParticipantResponseStatus.SUCCESS);
            response.setResponseMessage("Control loop is already in state " + orderedState);
            return;
        }

        if (!CollectionUtils.isEmpty(controlLoop.getElements().values())) {
            controlLoop.getElements().values().forEach(element -> {
                element.setState(newState);
                element.setOrderedState(orderedState);
            });
        }

        response.setResponseStatus(ParticipantResponseStatus.SUCCESS);
        response.setResponseMessage(
                "ControlLoop state changed from " + controlLoop.getOrderedState() + " to " + orderedState);
        controlLoop.setOrderedState(orderedState);
    }

    /**
     * Get control loops as a {@link ConrolLoops} class.
     *
     * @return the control loops
     */
    public ControlLoops getControlLoops() {
        var controlLoops = new ControlLoops();
        controlLoops.setControlLoopList(new ArrayList<>(controlLoopMap.values()));
        return controlLoops;
    }
}
