/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.intermediary.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import org.onap.policy.clamp.acm.participant.intermediary.api.AutomationCompositionElementListener;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.acm.participant.intermediary.parameters.ParticipantParameters;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementAck;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUpdates;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUtils;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionAck;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionStateChange;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionUpdate;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/*
 * This class is responsible for managing the state of all automation compositions in the participant.
 */
@Component
public class AutomationCompositionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutomationCompositionHandler.class);

    private final UUID participantId;
    private final ParticipantMessagePublisher publisher;

    @Getter
    private final Map<UUID, AutomationComposition> automationCompositionMap = new LinkedHashMap<>();

    @Getter
    private final Map<UUID, AutomationCompositionElement> elementsOnThisParticipant = new LinkedHashMap<>();

    @Getter
    private List<AutomationCompositionElementListener> listeners = new ArrayList<>();

    /**
     * Constructor, set the participant ID and messageSender.
     *
     * @param parameters the parameters of the participant
     * @param publisher the ParticipantMessage Publisher
     */
    public AutomationCompositionHandler(ParticipantParameters parameters, ParticipantMessagePublisher publisher) {
        this.participantId = parameters.getIntermediaryParameters().getParticipantId();
        this.publisher = publisher;
    }

    public void registerAutomationCompositionElementListener(AutomationCompositionElementListener listener) {
        listeners.add(listener);
    }

    /**
     * Handle a automation composition element state change message.
     *
     * @param automationCompositionId the automationComposition Id
     * @param id the automationComposition UUID
     * @param orderedState the current state
     * @param newState the ordered state
     * @return automationCompositionElement the updated automation composition element
     */
    public AutomationCompositionElement updateAutomationCompositionElementState(UUID automationCompositionId, UUID id,
            AutomationCompositionOrderedState orderedState, AutomationCompositionState newState) {

        if (id == null) {
            LOGGER.warn("Cannot update Automation composition element state, id is null");
            return null;
        }

        // Update states of AutomationCompositionElement in automationCompositionMap
        for (var automationComposition : automationCompositionMap.values()) {
            var element = automationComposition.getElements().get(id);
            if (element != null) {
                element.setOrderedState(orderedState);
                element.setState(newState);
            }
            var checkOpt = automationComposition.getElements().values().stream()
                    .filter(acElement -> !newState.equals(acElement.getState())).findAny();
            if (checkOpt.isEmpty()) {
                automationComposition.setState(newState);
                automationComposition.setOrderedState(orderedState);
            }
        }

        // Update states of AutomationCompositionElement in elementsOnThisParticipant
        var acElement = elementsOnThisParticipant.get(id);
        if (acElement != null) {
            var automationCompositionStateChangeAck =
                    new AutomationCompositionAck(ParticipantMessageType.AUTOMATION_COMPOSITION_STATECHANGE_ACK);
            automationCompositionStateChangeAck.setParticipantId(participantId);
            automationCompositionStateChangeAck.setAutomationCompositionId(automationCompositionId);
            acElement.setOrderedState(orderedState);
            acElement.setState(newState);
            automationCompositionStateChangeAck.getAutomationCompositionResultMap().put(acElement.getId(),
                    new AutomationCompositionElementAck(newState, true,
                            "Automation composition element {} state changed to {}\", id, newState)"));
            LOGGER.debug("Automation composition element {} state changed to {}", id, newState);
            automationCompositionStateChangeAck
                    .setMessage("AutomationCompositionElement state changed to {} " + newState);
            automationCompositionStateChangeAck.setResult(true);
            publisher.sendAutomationCompositionAck(automationCompositionStateChangeAck);
            return acElement;
        }
        return null;
    }

    /**
     * Handle a automation composition state change message.
     *
     * @param stateChangeMsg the state change message
     * @param acElementDefinitions the list of AutomationCompositionElementDefinition
     */
    public void handleAutomationCompositionStateChange(AutomationCompositionStateChange stateChangeMsg,
            List<AutomationCompositionElementDefinition> acElementDefinitions) {
        if (stateChangeMsg.getAutomationCompositionId() == null) {
            return;
        }

        var automationComposition = automationCompositionMap.get(stateChangeMsg.getAutomationCompositionId());

        if (automationComposition == null) {
            var automationCompositionAck =
                    new AutomationCompositionAck(ParticipantMessageType.AUTOMATION_COMPOSITION_STATECHANGE_ACK);
            automationCompositionAck.setParticipantId(participantId);
            automationCompositionAck.setMessage("Automation composition " + stateChangeMsg.getAutomationCompositionId()
                    + " does not use this participant " + participantId);
            automationCompositionAck.setResult(false);
            automationCompositionAck.setResponseTo(stateChangeMsg.getMessageId());
            automationCompositionAck.setAutomationCompositionId(stateChangeMsg.getAutomationCompositionId());
            publisher.sendAutomationCompositionAck(automationCompositionAck);
            LOGGER.debug("Automation composition {} does not use this participant",
                    stateChangeMsg.getAutomationCompositionId());
            return;
        }

        handleState(automationComposition, stateChangeMsg.getOrderedState(), stateChangeMsg.getStartPhase(),
                acElementDefinitions);
    }

    /**
     * Method to handle state changes.
     *
     * @param automationComposition participant response
     * @param orderedState automation composition ordered state
     * @param startPhaseMsg startPhase from message
     * @param acElementDefinitions the list of AutomationCompositionElementDefinition
     */
    private void handleState(final AutomationComposition automationComposition,
            AutomationCompositionOrderedState orderedState, Integer startPhaseMsg,
            List<AutomationCompositionElementDefinition> acElementDefinitions) {
        switch (orderedState) {
            case UNINITIALISED:
                handleUninitialisedState(automationComposition, orderedState, startPhaseMsg, acElementDefinitions);
                break;
            case PASSIVE:
                handlePassiveState(automationComposition, orderedState, startPhaseMsg, acElementDefinitions);
                break;
            case RUNNING:
                handleRunningState(automationComposition, orderedState, startPhaseMsg, acElementDefinitions);
                break;
            default:
                LOGGER.debug("StateChange message has no state, state is null {}", automationComposition.getKey());
                break;
        }
    }

    /**
     * Handle a automation composition update message.
     *
     * @param updateMsg the update message
     * @param acElementDefinitions the list of AutomationCompositionElementDefinition
     */
    public void handleAutomationCompositionUpdate(AutomationCompositionUpdate updateMsg,
            List<AutomationCompositionElementDefinition> acElementDefinitions) {

        if (!updateMsg.appliesTo(participantId)) {
            return;
        }

        if (0 == updateMsg.getStartPhase()) {
            handleAcUpdatePhase0(updateMsg, acElementDefinitions);
        } else {
            handleAcUpdatePhaseN(updateMsg, acElementDefinitions);
        }
    }

    private void handleAcUpdatePhase0(AutomationCompositionUpdate updateMsg,
            List<AutomationCompositionElementDefinition> acElementDefinitions) {
        var automationComposition = automationCompositionMap.get(updateMsg.getAutomationCompositionId());

        // TODO: Updates to existing AutomationCompositions are not supported yet (Addition/Removal of
        // AutomationComposition
        // elements to existing AutomationComposition has to be supported).
        if (automationComposition != null) {
            var automationCompositionUpdateAck =
                    new AutomationCompositionAck(ParticipantMessageType.AUTOMATION_COMPOSITION_UPDATE_ACK);
            automationCompositionUpdateAck.setParticipantId(participantId);

            automationCompositionUpdateAck.setMessage("Automation composition " + updateMsg.getAutomationCompositionId()
                    + " already defined on participant " + participantId);
            automationCompositionUpdateAck.setResult(false);
            automationCompositionUpdateAck.setResponseTo(updateMsg.getMessageId());
            automationCompositionUpdateAck.setAutomationCompositionId(updateMsg.getAutomationCompositionId());
            publisher.sendAutomationCompositionAck(automationCompositionUpdateAck);
            return;
        }

        if (updateMsg.getParticipantUpdatesList().isEmpty()) {
            LOGGER.warn("No AutomationCompositionElement updates in message {}",
                    updateMsg.getAutomationCompositionId());
            return;
        }

        automationComposition = new AutomationComposition();
        automationComposition.setInstanceId(updateMsg.getAutomationCompositionId());
        var acElements = storeElementsOnThisParticipant(updateMsg.getParticipantUpdatesList());
        var acElementMap = prepareAcElementMap(acElements);
        automationComposition.setElements(acElementMap);
        automationCompositionMap.put(updateMsg.getAutomationCompositionId(), automationComposition);

        handleAutomationCompositionElementUpdate(acElements, acElementDefinitions, updateMsg.getStartPhase(),
                updateMsg.getAutomationCompositionId());
    }

    private void handleAcUpdatePhaseN(AutomationCompositionUpdate updateMsg,
            List<AutomationCompositionElementDefinition> acElementDefinitions) {

        var acElementList = updateMsg.getParticipantUpdatesList().stream()
                .flatMap(participantUpdate -> participantUpdate.getAutomationCompositionElementList().stream())
                .filter(element -> participantId.equals(element.getParticipantId())).collect(Collectors.toList());

        handleAutomationCompositionElementUpdate(acElementList, acElementDefinitions, updateMsg.getStartPhase(),
                updateMsg.getAutomationCompositionId());
    }

    private void handleAutomationCompositionElementUpdate(List<AutomationCompositionElement> acElements,
            List<AutomationCompositionElementDefinition> acElementDefinitions, Integer startPhaseMsg,
            UUID automationCompositionId) {
        try {
            for (var element : acElements) {
                var acElementNodeTemplate = getAcElementNodeTemplate(acElementDefinitions, element.getDefinition());
                if (acElementNodeTemplate != null) {
                    int startPhase = ParticipantUtils.findStartPhase(acElementNodeTemplate.getProperties());
                    if (startPhaseMsg.equals(startPhase)) {
                        for (var acElementListener : listeners) {
                            var map = new HashMap<>(acElementNodeTemplate.getProperties());
                            map.putAll(element.getProperties());
                            acElementListener.automationCompositionElementUpdate(automationCompositionId, element, map);
                        }
                    }
                }
            }
        } catch (PfModelException e) {
            LOGGER.debug("Automation composition element update failed {}", automationCompositionId);
        }

    }

    private ToscaNodeTemplate getAcElementNodeTemplate(
            List<AutomationCompositionElementDefinition> acElementDefinitions, ToscaConceptIdentifier acElementDefId) {

        for (var acElementDefinition : acElementDefinitions) {
            if (acElementDefId.getName().contains(acElementDefinition.getAcElementDefinitionId().getName())) {
                return acElementDefinition.getAutomationCompositionElementToscaNodeTemplate();
            }
        }
        return null;
    }

    private List<AutomationCompositionElement> storeElementsOnThisParticipant(
            List<ParticipantUpdates> participantUpdates) {
        var acElementList = participantUpdates.stream()
                .flatMap(participantUpdate -> participantUpdate.getAutomationCompositionElementList().stream())
                .filter(element -> participantId.equals(element.getParticipantId())).collect(Collectors.toList());

        for (var element : acElementList) {
            elementsOnThisParticipant.put(element.getId(), element);
        }
        return acElementList;
    }

    private Map<UUID, AutomationCompositionElement> prepareAcElementMap(List<AutomationCompositionElement> acElements) {
        Map<UUID, AutomationCompositionElement> acElementMap = new LinkedHashMap<>();
        for (var element : acElements) {
            acElementMap.put(element.getId(), element);
        }
        return acElementMap;
    }

    /**
     * Method to handle when the new state from participant is UNINITIALISED state.
     *
     * @param automationComposition participant response
     * @param orderedState orderedState
     * @param startPhaseMsg startPhase from message
     * @param acElementDefinitions the list of AutomationCompositionElementDefinition
     */
    private void handleUninitialisedState(final AutomationComposition automationComposition,
            final AutomationCompositionOrderedState orderedState, Integer startPhaseMsg,
            List<AutomationCompositionElementDefinition> acElementDefinitions) {
        handleStateChange(automationComposition, orderedState, startPhaseMsg, acElementDefinitions);
        boolean isAllUninitialised = automationComposition.getElements().values().stream()
                .filter(element -> !AutomationCompositionState.UNINITIALISED.equals(element.getState())).findAny()
                .isEmpty();
        if (isAllUninitialised) {
            automationCompositionMap.remove(automationComposition.getInstanceId());
            automationComposition.getElements().values()
                    .forEach(element -> elementsOnThisParticipant.remove(element.getId()));
        }
    }

    /**
     * Method to handle when the new state from participant is PASSIVE state.
     *
     * @param automationComposition participant response
     * @param orderedState orderedState
     * @param startPhaseMsg startPhase from message
     * @param acElementDefinitions the list of AutomationCompositionElementDefinition
     */
    private void handlePassiveState(final AutomationComposition automationComposition,
            final AutomationCompositionOrderedState orderedState, Integer startPhaseMsg,
            List<AutomationCompositionElementDefinition> acElementDefinitions) {
        handleStateChange(automationComposition, orderedState, startPhaseMsg, acElementDefinitions);
    }

    /**
     * Method to handle when the new state from participant is RUNNING state.
     *
     * @param automationComposition participant response
     * @param orderedState orderedState
     * @param startPhaseMsg startPhase from message
     * @param acElementDefinitions the list of AutomationCompositionElementDefinition
     */
    private void handleRunningState(final AutomationComposition automationComposition,
            final AutomationCompositionOrderedState orderedState, Integer startPhaseMsg,
            List<AutomationCompositionElementDefinition> acElementDefinitions) {
        handleStateChange(automationComposition, orderedState, startPhaseMsg, acElementDefinitions);
    }

    /**
     * Method to update the state of automation composition elements.
     *
     * @param automationComposition participant status in memory
     * @param orderedState orderedState the new ordered state the participant should have
     * @param startPhaseMsg startPhase from message
     * @param acElementDefinitions the list of AutomationCompositionElementDefinition
     */
    private void handleStateChange(AutomationComposition automationComposition,
            final AutomationCompositionOrderedState orderedState, Integer startPhaseMsg,
            List<AutomationCompositionElementDefinition> acElementDefinitions) {

        if (orderedState.equals(automationComposition.getOrderedState())) {
            var automationCompositionAck =
                    new AutomationCompositionAck(ParticipantMessageType.AUTOMATION_COMPOSITION_STATECHANGE_ACK);
            automationCompositionAck.setParticipantId(participantId);
            automationCompositionAck.setMessage("Automation composition is already in state " + orderedState);
            automationCompositionAck.setResult(false);
            automationCompositionAck.setAutomationCompositionId(automationComposition.getInstanceId());
            publisher.sendAutomationCompositionAck(automationCompositionAck);
            return;
        }

        automationComposition.getElements().values().stream()
                .forEach(acElement -> automationCompositionElementStateChange(automationComposition, orderedState,
                        acElement, startPhaseMsg, acElementDefinitions));
    }

    private void automationCompositionElementStateChange(AutomationComposition automationComposition,
            AutomationCompositionOrderedState orderedState, AutomationCompositionElement acElement,
            Integer startPhaseMsg, List<AutomationCompositionElementDefinition> acElementDefinitions) {
        var acElementNodeTemplate = getAcElementNodeTemplate(acElementDefinitions, acElement.getDefinition());
        if (acElementNodeTemplate != null) {
            int startPhase = ParticipantUtils.findStartPhase(acElementNodeTemplate.getProperties());
            if (startPhaseMsg.equals(startPhase)) {
                for (var acElementListener : listeners) {
                    try {
                        acElementListener.automationCompositionElementStateChange(automationComposition.getInstanceId(),
                                acElement.getId(), acElement.getState(), orderedState);
                    } catch (PfModelException e) {
                        LOGGER.debug("Automation composition element update failed {}",
                                automationComposition.getInstanceId());
                    }
                }
            }
        }
    }
}
