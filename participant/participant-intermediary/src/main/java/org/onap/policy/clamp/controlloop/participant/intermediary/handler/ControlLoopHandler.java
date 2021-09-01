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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ClElementStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElementAck;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElementDefinition;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoops;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantUpdates;
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

        ControlLoopElement clElement = elementsOnThisParticipant.get(id);
        if (clElement != null) {
            var controlLoopStateChangeAck =
                    new ControlLoopAck(ParticipantMessageType.CONTROLLOOP_STATECHANGE_ACK);
            controlLoopStateChangeAck.setParticipantId(participantId);
            controlLoopStateChangeAck.setParticipantType(participantType);
            clElement.setOrderedState(orderedState);
            clElement.setState(newState);
            controlLoopStateChangeAck.getControlLoopResultMap().put(clElement.getId(),
                new  ControlLoopElementAck(true, "Control loop element {} state changed to {}\", id, newState)"));
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
            var controlLoopAck = new ControlLoopAck(ParticipantMessageType.CONTROL_LOOP_STATE_CHANGE);
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

        handleState(controlLoop, stateChangeMsg.getOrderedState());
    }

    /**
     * Method to handle state changes.
     *
     * @param controlLoop participant response
     * @param orderedState controlloop ordered state
     */
    private void handleState(final ControlLoop controlLoop, ControlLoopOrderedState orderedState) {
        switch (orderedState) {
            case UNINITIALISED:
                handleUninitialisedState(controlLoop, orderedState);
                break;
            case PASSIVE:
                handlePassiveState(controlLoop, orderedState);
                break;
            case RUNNING:
                handleRunningState(controlLoop, orderedState);
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

        List<ControlLoopElement> clElements = storeElementsOnThisParticipant(updateMsg.getParticipantUpdatesList());

        try {
            for (ControlLoopElement element : clElements) {
                ToscaNodeTemplate clElementNodeTemplate = getClElementNodeTemplate(
                        clElementDefinitions, element.getDefinition());
                for (ControlLoopElementListener clElementListener : listeners) {
                    clElementListener.controlLoopElementUpdate(element, clElementNodeTemplate);
                }
            }
        } catch (PfModelException e) {
            LOGGER.debug("Control loop element update failed {}", updateMsg.getControlLoopId());
        }

        Map<UUID, ControlLoopElement> clElementMap = prepareClElementMap(clElements);
        controlLoop = new ControlLoop();
        controlLoop.setDefinition(updateMsg.getControlLoopId());
        controlLoop.setElements(clElementMap);
        controlLoopMap.put(updateMsg.getControlLoopId(), controlLoop);
    }

    private ToscaNodeTemplate getClElementNodeTemplate(List<ControlLoopElementDefinition> clElementDefinitions,
                ToscaConceptIdentifier clElementDefId) {
        for (ControlLoopElementDefinition clElementDefinition : clElementDefinitions) {
            if (clElementDefinition.getClElementDefinitionId().equals(clElementDefId)) {
                return clElementDefinition.getControlLoopElementToscaNodeTemplate();
            }
        }
        return null;
    }

    private List<ControlLoopElement> storeElementsOnThisParticipant(List<ParticipantUpdates> participantUpdates) {
        var clElementMap =
                participantUpdates.stream()
                .flatMap(participantUpdate -> participantUpdate.getControlLoopElementList().stream())
                .filter(element -> participantType.equals(element.getParticipantType()))
                .collect(Collectors.toList());

        for (var element : clElementMap) {
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
     */
    private void handleUninitialisedState(final ControlLoop controlLoop, final ControlLoopOrderedState orderedState) {
        handleStateChange(controlLoop, orderedState, ControlLoopState.UNINITIALISED);
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
     */
    private void handlePassiveState(final ControlLoop controlLoop, final ControlLoopOrderedState orderedState) {
        handleStateChange(controlLoop, orderedState, ControlLoopState.PASSIVE);
    }

    /**
     * Method to handle when the new state from participant is RUNNING state.
     *
     * @param controlLoop participant response
     * @param orderedState orderedState
     */
    private void handleRunningState(final ControlLoop controlLoop, final ControlLoopOrderedState orderedState) {
        handleStateChange(controlLoop, orderedState, ControlLoopState.RUNNING);
    }

    /**
     * Method to update the state of control loop elements.
     *
     * @param controlLoop participant status in memory
     * @param orderedState orderedState the new ordered state the participant should have
     * @param newState new state of the control loop elements
     */
    private void handleStateChange(ControlLoop controlLoop, final ControlLoopOrderedState orderedState,
            ControlLoopState newState) {

        if (orderedState.equals(controlLoop.getOrderedState())) {
            var controlLoopAck = new ControlLoopAck(ParticipantMessageType.CONTROL_LOOP_STATE_CHANGE);
            controlLoopAck.setParticipantId(participantId);
            controlLoopAck.setParticipantType(participantType);
            controlLoopAck.setMessage("Control loop is already in state" + orderedState);
            controlLoopAck.setResult(false);
            controlLoopAck.setControlLoopId(controlLoop.getDefinition());
            publisher.sendControlLoopAck(controlLoopAck);
            return;
        }

        if (!CollectionUtils.isEmpty(controlLoop.getElements().values())) {
            controlLoop.getElements().values().forEach(element -> {
                element.setState(newState);
                element.setOrderedState(orderedState);
            });
        }

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
