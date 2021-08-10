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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ClElementStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElementDefinition;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoops;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantDefinition;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantUpdates;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ControlLoopAck;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ControlLoopStateChange;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ControlLoopUpdate;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ControlLoopElementListener;
import org.onap.policy.clamp.controlloop.participant.intermediary.comm.MessageSender;
import org.onap.policy.clamp.controlloop.participant.intermediary.parameters.ParticipantIntermediaryParameters;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This class is responsible for managing the state of all control loops in the participant.
 */
@NoArgsConstructor
public class ControlLoopHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ControlLoopHandler.class);

    private ToscaConceptIdentifier participantType = null;
    private ToscaConceptIdentifier participantId = null;
    private MessageSender messageSender = null;

    private final Map<ToscaConceptIdentifier, ControlLoop> controlLoopMap = new LinkedHashMap<>();

    @Getter
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
            LOGGER.warn("Cannot update Control loop element state, id is null");
        }

        ControlLoopAck controlLoopStateChangeAck =
                new ControlLoopAck(ParticipantMessageType.CONTROLLOOP_STATECHANGE_ACK);
        ControlLoopElement clElement = elementsOnThisParticipant.get(id);
        if (clElement != null) {
            clElement.setOrderedState(orderedState);
            clElement.setState(newState);
            controlLoopStateChangeAck.getControlLoopResultMap().put(clElement.getId(),
                Pair.of(true, "Control loop element {} state changed to {}\", id, newState)"));
            LOGGER.debug("Control loop element {} state changed to {}", id, newState);
            controlLoopStateChangeAck.setMessage("ControlLoopElement state changed to {} " + newState);
            controlLoopStateChangeAck.setResult(true);
            messageSender.sendAckResponse(controlLoopStateChangeAck);
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
    public void handleControlLoopStateChange(ControlLoopStateChange stateChangeMsg) {
        if (stateChangeMsg.getControlLoopId() == null) {
            return;
        }

        var controlLoop = controlLoopMap.get(stateChangeMsg.getControlLoopId());

        if (controlLoop == null) {
            LOGGER.debug("Control loop {} does not use this participant", stateChangeMsg.getControlLoopId());
            return;
        }

        var controlLoopStateChangeAck = new ControlLoopAck(ParticipantMessageType.CONTROLLOOP_STATECHANGE_ACK);
        controlLoopStateChangeAck.setResponseTo(stateChangeMsg.getMessageId());
        controlLoopStateChangeAck.setControlLoopId(stateChangeMsg.getControlLoopId());
        handleState(controlLoop, controlLoopStateChangeAck, stateChangeMsg.getOrderedState());
        messageSender.sendAckResponse(controlLoopStateChangeAck);
    }

    /**
     * Method to handle state changes.
     *
     * @param controlLoop participant response
     * @param response participant response
     * @param orderedState controlloop ordered state
     */
    private void handleState(final ControlLoop controlLoop, final ControlLoopAck response,
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
    public void handleControlLoopUpdate(ControlLoopUpdate updateMsg,
                List<ControlLoopElementDefinition> clElementDefinitions) {

        if (!updateMsg.appliesTo(participantType, participantId)) {
            return;
        }

        var controlLoop = controlLoopMap.get(updateMsg.getControlLoopId());

        var controlLoopUpdateAck = new ControlLoopAck(ParticipantMessageType.CONTROLLOOP_UPDATE_ACK);

        // TODO: Updates to existing ControlLoops are not supported yet (Addition/Removal of ControlLoop
        // elements to existing ControlLoop has to be supported).
        if (controlLoop != null) {
            controlLoopUpdateAck.setResponseTo(updateMsg.getMessageId());
            controlLoopUpdateAck.setControlLoopId(updateMsg.getControlLoopId());
            controlLoopUpdateAck.setMessage("Control loop " + updateMsg.getControlLoopId()
                + " already defined on participant " + participantId);
            controlLoopUpdateAck.setResult(false);
            messageSender.sendAckResponse(controlLoopUpdateAck);
            return;
        }

        List<ControlLoopElement> clElements = storeElementsOnThisParticipant(updateMsg.getParticipantUpdatesList());

        for (ControlLoopElementListener clElementListener : listeners) {
            try {
                for (ControlLoopElement element : clElements) {
                    ToscaNodeTemplate clElementNodeTemplate = getClElementNodeTemplate(
                        clElementDefinitions, element.getDefinition());
                    clElementListener.controlLoopElementUpdate(element, clElementNodeTemplate);
                }
            } catch (PfModelException e) {
                LOGGER.debug("Control loop element update failed {}", updateMsg.getControlLoopId());
            }
        }

        Map<UUID, ControlLoopElement> clElementMap = prepareClElementMap(clElements);
        controlLoop = new ControlLoop();
        controlLoop.setDefinition(updateMsg.getControlLoopId());
        controlLoop.setElements(clElementMap);
        controlLoopMap.put(updateMsg.getControlLoopId(), controlLoop);

        controlLoopUpdateAck.setResponseTo(updateMsg.getMessageId());
        controlLoopUpdateAck.setControlLoopId(updateMsg.getControlLoopId());
        controlLoopUpdateAck.setMessage("Control loop " + updateMsg.getControlLoopId()
                + " defined on participant " + participantId);
        controlLoopUpdateAck.setResult(true);
        messageSender.sendAckResponse(controlLoopUpdateAck);
    }

    private ToscaNodeTemplate getClElementNodeTemplate(List<ControlLoopElementDefinition> clElementDefinitions,
        ToscaConceptIdentifier clElementDefId)  {
        for (ControlLoopElementDefinition clElementDefinition : clElementDefinitions) {
            if (clElementDefinition.getClElementDefinitionId().equals(clElementDefId)) {
                return clElementDefinition.getControlLoopElementToscaNodeTemplate();
            }
        }
        return null;
    }

    private List<ControlLoopElement> storeElementsOnThisParticipant(
        List<ParticipantUpdates> participantUpdates) {
        List<ControlLoopElement> clElementMap = new ArrayList<>();
        for (ParticipantUpdates participantUpdate : participantUpdates) {
            if (participantUpdate.getParticipantId().equals(participantType)) {
                clElementMap = participantUpdate.getControlLoopElementList();
            }
        }
        for (ControlLoopElement element : clElementMap) {
            elementsOnThisParticipant.put(element.getId(), element);
        }
        return clElementMap;
    }

    private Map<UUID, ControlLoopElement> prepareClElementMap(List<ControlLoopElement> clElements) {
        Map<UUID, ControlLoopElement> clElementMap = new LinkedHashMap<>();
        for (ControlLoopElement element : clElements) {
            clElementMap.put(element.getId(), element);
        }
        return clElementMap;
    }

    /**
     * Method to handle when the new state from participant is UNINITIALISED state.
     *
     * @param controlLoop participant response
     * @param orderedState orderedState
     * @param response participant response
     */
    private void handleUninitialisedState(final ControlLoop controlLoop, final ControlLoopOrderedState orderedState,
            final ControlLoopAck response) {
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
            final ControlLoopAck response) {
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
            final ControlLoopAck response) {
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
            ControlLoopState newState, ControlLoopAck response) {

        if (orderedState.equals(controlLoop.getOrderedState())) {
            response.setMessage("Control loop is already in state " + orderedState);
            response.setResult(false);
            return;
        }

        if (!CollectionUtils.isEmpty(controlLoop.getElements().values())) {
            controlLoop.getElements().values().forEach(element -> {
                element.setState(newState);
                element.setOrderedState(orderedState);
            });
        }

        response.setMessage("ControlLoop state changed from " + controlLoop.getOrderedState() + " to " + orderedState);
        response.setResult(true);
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
