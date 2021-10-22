/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
 *  Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ClElementStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElementAck;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElementDefinition;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoops;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantUpdates;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantUtils;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ControlLoopAck;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ControlLoopStateChange;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ControlLoopUpdate;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ControlLoopElementListener;
import org.onap.policy.clamp.controlloop.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.controlloop.participant.intermediary.parameters.ParticipantParameters;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/*
 * This class is responsible for managing the state of all control loops in the participant.
 */
@Component
public class ControlLoopHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ControlLoopHandler.class);

    private final ToscaConceptIdentifier participantType;
    private final ToscaConceptIdentifier participantId;
    private final ParticipantMessagePublisher publisher;

    @Getter
    private final Map<ToscaConceptIdentifier, ControlLoop> controlLoopMap = new LinkedHashMap<>();

    @Getter
    private final Map<UUID, ControlLoopElement> elementsOnThisParticipant = new LinkedHashMap<>();

    @Getter
    private List<ControlLoopElementListener> listeners = new ArrayList<>();

    /**
     * Constructor, set the participant ID and messageSender.
     *
     * @param parameters the parameters of the participant
     * @param publisher the ParticipantMessage Publisher
     */
    public ControlLoopHandler(ParticipantParameters parameters, ParticipantMessagePublisher publisher) {
        this.participantType = parameters.getIntermediaryParameters().getParticipantType();
        this.participantId = parameters.getIntermediaryParameters().getParticipantId();
        this.publisher = publisher;
    }

    public void registerControlLoopElementListener(ControlLoopElementListener listener) {
        listeners.add(listener);
    }

    /**
     * Handle a control loop element state change message.
     *
     * @param controlLoopId the controlLoop Id
     * @param id the controlLoop UUID
     * @param orderedState the current state
     * @param newState the ordered state
     * @return controlLoopElement the updated controlloop element
     */
    public ControlLoopElement updateControlLoopElementState(ToscaConceptIdentifier controlLoopId, UUID id,
            ControlLoopOrderedState orderedState, ControlLoopState newState) {

        if (id == null) {
            LOGGER.warn("Cannot update Control loop element state, id is null");
            return null;
        }

        // Update states of ControlLoopElement in controlLoopMap
        for (var controlLoop : controlLoopMap.values()) {
            var element = controlLoop.getElements().get(id);
            if (element != null) {
                element.setOrderedState(orderedState);
                element.setState(newState);
            }
            var checkOpt = controlLoop.getElements().values().stream()
                    .filter(clElement -> !newState.equals(clElement.getState())).findAny();
            if (checkOpt.isEmpty()) {
                controlLoop.setState(newState);
                controlLoop.setOrderedState(orderedState);
            }
        }

        // Update states of ControlLoopElement in elementsOnThisParticipant
        var clElement = elementsOnThisParticipant.get(id);
        if (clElement != null) {
            var controlLoopStateChangeAck = new ControlLoopAck(ParticipantMessageType.CONTROLLOOP_STATECHANGE_ACK);
            controlLoopStateChangeAck.setParticipantId(participantId);
            controlLoopStateChangeAck.setParticipantType(participantType);
            controlLoopStateChangeAck.setControlLoopId(controlLoopId);
            clElement.setOrderedState(orderedState);
            clElement.setState(newState);
            controlLoopStateChangeAck.getControlLoopResultMap().put(clElement.getId(), new ControlLoopElementAck(
                    newState, true, "Control loop element {} state changed to {}\", id, newState)"));
            LOGGER.debug("Control loop element {} state changed to {}", id, newState);
            controlLoopStateChangeAck.setMessage("ControlLoopElement state changed to {} " + newState);
            controlLoopStateChangeAck.setResult(true);
            publisher.sendControlLoopAck(controlLoopStateChangeAck);
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
        var clElement = elementsOnThisParticipant.get(id);
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
     * @param clElementDefinitions the list of ControlLoopElementDefinition
     */
    public void handleControlLoopStateChange(ControlLoopStateChange stateChangeMsg,
            List<ControlLoopElementDefinition> clElementDefinitions) {
        if (stateChangeMsg.getControlLoopId() == null) {
            return;
        }

        var controlLoop = controlLoopMap.get(stateChangeMsg.getControlLoopId());

        if (controlLoop == null) {
            var controlLoopAck = new ControlLoopAck(ParticipantMessageType.CONTROLLOOP_STATECHANGE_ACK);
            controlLoopAck.setParticipantId(participantId);
            controlLoopAck.setParticipantType(participantType);
            controlLoopAck.setMessage("Control loop " + stateChangeMsg.getControlLoopId()
                    + " does not use this participant " + participantId);
            controlLoopAck.setResult(false);
            controlLoopAck.setResponseTo(stateChangeMsg.getMessageId());
            controlLoopAck.setControlLoopId(stateChangeMsg.getControlLoopId());
            publisher.sendControlLoopAck(controlLoopAck);
            LOGGER.debug("Control loop {} does not use this participant", stateChangeMsg.getControlLoopId());
            return;
        }

        handleState(controlLoop, stateChangeMsg.getOrderedState(), stateChangeMsg.getStartPhase(),
                clElementDefinitions);
    }

    /**
     * Method to handle state changes.
     *
     * @param controlLoop participant response
     * @param orderedState controlloop ordered state
     * @param startPhaseMsg startPhase from message
     * @param clElementDefinitions the list of ControlLoopElementDefinition
     */
    private void handleState(final ControlLoop controlLoop, ControlLoopOrderedState orderedState, Integer startPhaseMsg,
            List<ControlLoopElementDefinition> clElementDefinitions) {
        switch (orderedState) {
            case UNINITIALISED:
                handleUninitialisedState(controlLoop, orderedState, startPhaseMsg, clElementDefinitions);
                break;
            case PASSIVE:
                handlePassiveState(controlLoop, orderedState, startPhaseMsg, clElementDefinitions);
                break;
            case RUNNING:
                handleRunningState(controlLoop, orderedState, startPhaseMsg, clElementDefinitions);
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
     * @param clElementDefinitions the list of ControlLoopElementDefinition
     */
    public void handleControlLoopUpdate(ControlLoopUpdate updateMsg,
            List<ControlLoopElementDefinition> clElementDefinitions) {

        if (!updateMsg.appliesTo(participantType, participantId)) {
            return;
        }

        if (0 == updateMsg.getStartPhase()) {
            handleClUpdatePhase0(updateMsg, clElementDefinitions);
        } else {
            handleClUpdatePhaseN(updateMsg, clElementDefinitions);
        }
    }

    private void handleClUpdatePhase0(ControlLoopUpdate updateMsg,
            List<ControlLoopElementDefinition> clElementDefinitions) {
        var controlLoop = controlLoopMap.get(updateMsg.getControlLoopId());

        // TODO: Updates to existing ControlLoops are not supported yet (Addition/Removal of ControlLoop
        // elements to existing ControlLoop has to be supported).
        if (controlLoop != null) {
            var controlLoopUpdateAck = new ControlLoopAck(ParticipantMessageType.CONTROLLOOP_UPDATE_ACK);
            controlLoopUpdateAck.setParticipantId(participantId);
            controlLoopUpdateAck.setParticipantType(participantType);

            controlLoopUpdateAck.setMessage("Control loop " + updateMsg.getControlLoopId()
                    + " already defined on participant " + participantId);
            controlLoopUpdateAck.setResult(false);
            controlLoopUpdateAck.setResponseTo(updateMsg.getMessageId());
            controlLoopUpdateAck.setControlLoopId(updateMsg.getControlLoopId());
            publisher.sendControlLoopAck(controlLoopUpdateAck);
            return;
        }

        if (updateMsg.getParticipantUpdatesList().isEmpty()) {
            LOGGER.warn("No ControlLoopElement updates in message {}", updateMsg.getControlLoopId());
            return;
        }

        var clElements = storeElementsOnThisParticipant(updateMsg.getParticipantUpdatesList());

        var clElementMap = prepareClElementMap(clElements);
        controlLoop = new ControlLoop();
        controlLoop.setDefinition(updateMsg.getControlLoopId());
        controlLoop.setElements(clElementMap);
        controlLoopMap.put(updateMsg.getControlLoopId(), controlLoop);

        handleControlLoopElementUpdate(clElements, clElementDefinitions, updateMsg.getStartPhase(),
                updateMsg.getControlLoopId());
    }

    private void handleClUpdatePhaseN(ControlLoopUpdate updateMsg,
            List<ControlLoopElementDefinition> clElementDefinitions) {

        var clElementList = updateMsg.getParticipantUpdatesList().stream()
                .flatMap(participantUpdate -> participantUpdate.getControlLoopElementList().stream())
                .filter(element -> participantType.equals(element.getParticipantType())).collect(Collectors.toList());

        handleControlLoopElementUpdate(clElementList, clElementDefinitions, updateMsg.getStartPhase(),
                updateMsg.getControlLoopId());
    }

    private void handleControlLoopElementUpdate(List<ControlLoopElement> clElements,
            List<ControlLoopElementDefinition> clElementDefinitions, Integer startPhaseMsg,
            ToscaConceptIdentifier controlLoopId) {
        try {
            for (var element : clElements) {
                var clElementNodeTemplate = getClElementNodeTemplate(clElementDefinitions, element.getDefinition());
                if (clElementNodeTemplate != null) {
                    int startPhase = ParticipantUtils.findStartPhase(clElementNodeTemplate.getProperties());
                    if (startPhaseMsg.equals(startPhase)) {
                        for (var clElementListener : listeners) {
                            clElementListener.controlLoopElementUpdate(controlLoopId, element, clElementNodeTemplate);
                        }
                    }
                }
            }
        } catch (PfModelException e) {
            LOGGER.debug("Control loop element update failed {}", controlLoopId);
        }

    }

    private ToscaNodeTemplate getClElementNodeTemplate(List<ControlLoopElementDefinition> clElementDefinitions,
            ToscaConceptIdentifier clElementDefId) {

        for (var clElementDefinition : clElementDefinitions) {
            if (clElementDefId.getName().contains(clElementDefinition.getClElementDefinitionId().getName())) {
                return clElementDefinition.getControlLoopElementToscaNodeTemplate();
            }
        }
        return null;
    }

    private List<ControlLoopElement> storeElementsOnThisParticipant(List<ParticipantUpdates> participantUpdates) {
        var clElementList = participantUpdates.stream()
                .flatMap(participantUpdate -> participantUpdate.getControlLoopElementList().stream())
                .filter(element -> participantType.equals(element.getParticipantType())).collect(Collectors.toList());

        for (var element : clElementList) {
            elementsOnThisParticipant.put(element.getId(), element);
        }
        return clElementList;
    }

    private Map<UUID, ControlLoopElement> prepareClElementMap(List<ControlLoopElement> clElements) {
        Map<UUID, ControlLoopElement> clElementMap = new LinkedHashMap<>();
        for (var element : clElements) {
            clElementMap.put(element.getId(), element);
        }
        return clElementMap;
    }

    /**
     * Method to handle when the new state from participant is UNINITIALISED state.
     *
     * @param controlLoop participant response
     * @param orderedState orderedState
     * @param startPhaseMsg startPhase from message
     * @param clElementDefinitions the list of ControlLoopElementDefinition
     */
    private void handleUninitialisedState(final ControlLoop controlLoop, final ControlLoopOrderedState orderedState,
            Integer startPhaseMsg, List<ControlLoopElementDefinition> clElementDefinitions) {
        handleStateChange(controlLoop, orderedState, startPhaseMsg, clElementDefinitions);
        boolean isAllUninitialised = controlLoop.getElements().values().stream()
                .filter(element -> !ControlLoopState.UNINITIALISED.equals(element.getState())).findAny().isEmpty();
        if (isAllUninitialised) {
            controlLoopMap.remove(controlLoop.getDefinition());
            controlLoop.getElements().values().forEach(element -> elementsOnThisParticipant.remove(element.getId()));
        }
    }

    /**
     * Method to handle when the new state from participant is PASSIVE state.
     *
     * @param controlLoop participant response
     * @param orderedState orderedState
     * @param startPhaseMsg startPhase from message
     * @param clElementDefinitions the list of ControlLoopElementDefinition
     */
    private void handlePassiveState(final ControlLoop controlLoop, final ControlLoopOrderedState orderedState,
            Integer startPhaseMsg, List<ControlLoopElementDefinition> clElementDefinitions) {
        handleStateChange(controlLoop, orderedState, startPhaseMsg, clElementDefinitions);
    }

    /**
     * Method to handle when the new state from participant is RUNNING state.
     *
     * @param controlLoop participant response
     * @param orderedState orderedState
     * @param startPhaseMsg startPhase from message
     * @param clElementDefinitions the list of ControlLoopElementDefinition
     */
    private void handleRunningState(final ControlLoop controlLoop, final ControlLoopOrderedState orderedState,
            Integer startPhaseMsg, List<ControlLoopElementDefinition> clElementDefinitions) {
        handleStateChange(controlLoop, orderedState, startPhaseMsg, clElementDefinitions);
    }

    /**
     * Method to update the state of control loop elements.
     *
     * @param controlLoop participant status in memory
     * @param orderedState orderedState the new ordered state the participant should have
     * @param startPhaseMsg startPhase from message
     * @param clElementDefinitions the list of ControlLoopElementDefinition
     */
    private void handleStateChange(ControlLoop controlLoop, final ControlLoopOrderedState orderedState,
            Integer startPhaseMsg, List<ControlLoopElementDefinition> clElementDefinitions) {

        if (orderedState.equals(controlLoop.getOrderedState())) {
            var controlLoopAck = new ControlLoopAck(ParticipantMessageType.CONTROLLOOP_STATECHANGE_ACK);
            controlLoopAck.setParticipantId(participantId);
            controlLoopAck.setParticipantType(participantType);
            controlLoopAck.setMessage("Control loop is already in state " + orderedState);
            controlLoopAck.setResult(false);
            controlLoopAck.setControlLoopId(controlLoop.getDefinition());
            publisher.sendControlLoopAck(controlLoopAck);
            return;
        }

        controlLoop.getElements().values().stream().forEach(clElement -> controlLoopElementStateChange(controlLoop,
                orderedState, clElement, startPhaseMsg, clElementDefinitions));
    }

    private void controlLoopElementStateChange(ControlLoop controlLoop, ControlLoopOrderedState orderedState,
            ControlLoopElement clElement, Integer startPhaseMsg,
            List<ControlLoopElementDefinition> clElementDefinitions) {
        var clElementNodeTemplate = getClElementNodeTemplate(clElementDefinitions, clElement.getDefinition());
        if (clElementNodeTemplate != null) {
            int startPhase = ParticipantUtils.findStartPhase(clElementNodeTemplate.getProperties());
            if (startPhaseMsg.equals(startPhase)) {
                for (var clElementListener : listeners) {
                    try {
                        clElementListener.controlLoopElementStateChange(controlLoop.getDefinition(), clElement.getId(),
                                clElement.getState(), orderedState);
                    } catch (PfModelException e) {
                        LOGGER.debug("Control loop element update failed {}", controlLoop.getDefinition());
                    }
                }
            }
        }
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

    /**
     * Get properties of a controlloopelement.
     *
     * @param id the control loop element id
     * @return the instance properties
     */
    public Map<String, ToscaProperty> getClElementInstanceProperties(UUID id) {
        Map<String, ToscaProperty> propertiesMap = new HashMap<>();
        for (var controlLoop : controlLoopMap.values()) {
            var element = controlLoop.getElements().get(id);
            if (element != null) {
                propertiesMap.putAll(element.getPropertiesMap());
            }
        }
        return propertiesMap;
    }
}
