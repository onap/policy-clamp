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
import lombok.Getter;
import org.onap.policy.clamp.acm.participant.intermediary.api.AutomationCompositionElementListener;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.acm.participant.intermediary.parameters.ParticipantParameters;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeployAck;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDeploy;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUtils;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionDeploy;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionDeployAck;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionStateChange;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.LockOrder;
import org.onap.policy.clamp.models.acm.persistence.provider.AcInstanceStateResolver;
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
    private final AcInstanceStateResolver acInstanceStateResolver;

    @Getter
    private final Map<UUID, AutomationComposition> automationCompositionMap = new LinkedHashMap<>();

    @Getter
    private final Map<UUID, AutomationCompositionElement> elementsOnThisParticipant = new LinkedHashMap<>();

    @Getter
    private final List<AutomationCompositionElementListener> listeners = new ArrayList<>();

    /**
     * Constructor, set the participant ID and messageSender.
     *
     * @param parameters the parameters of the participant
     * @param publisher the ParticipantMessage Publisher
     */
    public AutomationCompositionHandler(ParticipantParameters parameters, ParticipantMessagePublisher publisher) {
        this.participantId = parameters.getIntermediaryParameters().getParticipantId();
        this.publisher = publisher;
        this.acInstanceStateResolver = new AcInstanceStateResolver();
    }

    public void registerAutomationCompositionElementListener(AutomationCompositionElementListener listener) {
        listeners.add(listener);
    }

    /**
     * Handle a automation composition element state change message.
     *
     * @param automationCompositionId the automationComposition Id
     * @param id the automationComposition UUID
     * @param deployState the DeployState state
     */
    public void updateAutomationCompositionElementState(UUID automationCompositionId, UUID id, DeployState deployState,
            LockState lockState) {

        if (id == null) {
            LOGGER.warn("Cannot update Automation composition element state, id is null");
            return;
        }

        // Update states of AutomationCompositionElement in automationCompositionMap
        for (var automationComposition : automationCompositionMap.values()) {
            var element = automationComposition.getElements().get(id);
            if (element != null) {
                element.setDeployState(deployState);
                element.setLockState(lockState);
            }
            var checkOpt = automationComposition.getElements().values().stream()
                    .filter(acElement -> !deployState.equals(acElement.getDeployState())).findAny();
            if (checkOpt.isEmpty()) {
                automationComposition.setDeployState(deployState);
            }
            checkOpt = automationComposition.getElements().values().stream()
                    .filter(acElement -> !lockState.equals(acElement.getLockState())).findAny();
            if (checkOpt.isEmpty()) {
                automationComposition.setLockState(lockState);
            }
        }

        // Update states of AutomationCompositionElement in elementsOnThisParticipant
        var acElement = elementsOnThisParticipant.get(id);
        if (acElement != null) {
            var automationCompositionStateChangeAck =
                    new AutomationCompositionDeployAck(ParticipantMessageType.AUTOMATION_COMPOSITION_STATECHANGE_ACK);
            automationCompositionStateChangeAck.setParticipantId(participantId);
            automationCompositionStateChangeAck.setAutomationCompositionId(automationCompositionId);
            acElement.setDeployState(deployState);
            acElement.setLockState(lockState);
            automationCompositionStateChangeAck.getAutomationCompositionResultMap().put(acElement.getId(),
                    new AcElementDeployAck(deployState, lockState, true,
                            "Automation composition element {} state changed to {}\", id, newState)"));
            LOGGER.debug("Automation composition element {} state changed to {}", id, deployState);
            automationCompositionStateChangeAck
                    .setMessage("AutomationCompositionElement state changed to {} " + deployState);
            automationCompositionStateChangeAck.setResult(true);
            publisher.sendAutomationCompositionAck(automationCompositionStateChangeAck);
        }
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
                    new AutomationCompositionDeployAck(ParticipantMessageType.AUTOMATION_COMPOSITION_STATECHANGE_ACK);
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

        if (!checkConsistantOrderState(automationComposition, stateChangeMsg.getDeployOrderedState(),
                stateChangeMsg.getLockOrderedState())) {
            var automationCompositionAck =
                    new AutomationCompositionDeployAck(ParticipantMessageType.AUTOMATION_COMPOSITION_STATECHANGE_ACK);
            automationCompositionAck.setParticipantId(participantId);
            automationCompositionAck.setMessage("Automation composition is already in state "
                    + stateChangeMsg.getDeployOrderedState() + " and " + stateChangeMsg.getLockOrderedState());
            automationCompositionAck.setResult(false);
            automationCompositionAck.setAutomationCompositionId(automationComposition.getInstanceId());
            publisher.sendAutomationCompositionAck(automationCompositionAck);
            return;
        }

        if (DeployOrder.NONE.equals(stateChangeMsg.getDeployOrderedState())) {
            handleLockOrderState(automationComposition, stateChangeMsg.getLockOrderedState(),
                    stateChangeMsg.getStartPhase(), acElementDefinitions);
        } else {
            handleDeployOrderState(automationComposition, stateChangeMsg.getDeployOrderedState(),
                    stateChangeMsg.getStartPhase(), acElementDefinitions);
        }
    }

    private boolean checkConsistantOrderState(AutomationComposition automationComposition, DeployOrder deployOrder,
            LockOrder lockOrder) {
        return acInstanceStateResolver.resolve(deployOrder, lockOrder, automationComposition.getDeployState(),
                automationComposition.getLockState()) != null;
    }

    /**
     * Method to handle state changes.
     *
     * @param automationComposition participant response
     * @param orderedState automation composition ordered state
     * @param startPhaseMsg startPhase from message
     * @param acElementDefinitions the list of AutomationCompositionElementDefinition
     */
    private void handleDeployOrderState(final AutomationComposition automationComposition, DeployOrder orderedState,
            Integer startPhaseMsg, List<AutomationCompositionElementDefinition> acElementDefinitions) {

        if (DeployOrder.UNDEPLOY.equals(orderedState)) {
            handleUndeployState(automationComposition, startPhaseMsg, acElementDefinitions);
        } else {
            LOGGER.debug("StateChange message has no state, state is null {}", automationComposition.getKey());
        }
    }

    /**
     * Method to handle state changes.
     *
     * @param automationComposition participant response
     * @param orderedState automation composition ordered state
     * @param startPhaseMsg startPhase from message
     * @param acElementDefinitions the list of AutomationCompositionElementDefinition
     */
    private void handleLockOrderState(final AutomationComposition automationComposition, LockOrder orderedState,
            Integer startPhaseMsg, List<AutomationCompositionElementDefinition> acElementDefinitions) {

        switch (orderedState) {
            case LOCK:
                handleLockState(automationComposition, startPhaseMsg, acElementDefinitions);
                break;
            case UNLOCK:
                handleUnlockState(automationComposition, startPhaseMsg, acElementDefinitions);
                break;
            default:
                LOGGER.debug("StateChange message has no state, state is null {}", automationComposition.getKey());
                break;
        }
    }

    /**
     * Handle a automation composition Deploy message.
     *
     * @param updateMsg the Deploy message
     * @param acElementDefinitions the list of AutomationCompositionElementDefinition
     */
    public void handleAutomationCompositionDeploy(AutomationCompositionDeploy updateMsg,
            List<AutomationCompositionElementDefinition> acElementDefinitions) {

        if (updateMsg.getParticipantUpdatesList().isEmpty()) {
            LOGGER.warn("No AutomationCompositionElement updates in message {}",
                    updateMsg.getAutomationCompositionId());
            return;
        }

        for (var participantDeploy : updateMsg.getParticipantUpdatesList()) {
            if (participantId.equals(participantDeploy.getParticipantId())) {
                if (updateMsg.isFirstStartPhase()) {
                    initializeDeploy(updateMsg.getMessageId(), updateMsg.getAutomationCompositionId(),
                            participantDeploy);
                }
                callParticipanDeploy(participantDeploy.getAcElementList(), acElementDefinitions,
                        updateMsg.getStartPhase(), updateMsg.getAutomationCompositionId());
            }
        }
    }

    private void initializeDeploy(UUID messageId, UUID instanceId, ParticipantDeploy participantDeploy) {
        var automationComposition = automationCompositionMap.get(instanceId);

        if (automationComposition != null) {
            var automationCompositionUpdateAck =
                    new AutomationCompositionDeployAck(ParticipantMessageType.AUTOMATION_COMPOSITION_DEPLOY_ACK);
            automationCompositionUpdateAck.setParticipantId(participantId);

            automationCompositionUpdateAck.setMessage(
                    "Automation composition " + instanceId + " already defined on participant " + participantId);
            automationCompositionUpdateAck.setResult(false);
            automationCompositionUpdateAck.setResponseTo(messageId);
            automationCompositionUpdateAck.setAutomationCompositionId(instanceId);
            publisher.sendAutomationCompositionAck(automationCompositionUpdateAck);
            return;
        }

        automationComposition = new AutomationComposition();
        automationComposition.setInstanceId(instanceId);
        var acElements = storeElementsOnThisParticipant(participantDeploy);
        automationComposition.setElements(prepareAcElementMap(acElements));
        automationCompositionMap.put(instanceId, automationComposition);
    }

    private void callParticipanDeploy(List<AcElementDeploy> acElements,
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
                            acElementListener.deploy(automationCompositionId, element, map);
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

    private List<AutomationCompositionElement> storeElementsOnThisParticipant(ParticipantDeploy participantDeploy) {
        List<AutomationCompositionElement> acElementList = new ArrayList<>();
        for (var element : participantDeploy.getAcElementList()) {
            var acElement = new AutomationCompositionElement();
            acElement.setId(element.getId());
            acElement.setDefinition(element.getDefinition());
            acElement.setDeployState(DeployState.DEPLOYING);
            acElement.setLockState(LockState.NONE);
            elementsOnThisParticipant.put(element.getId(), acElement);
            acElementList.add(acElement);
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
     * @param startPhaseMsg startPhase from message
     * @param acElementDefinitions the list of AutomationCompositionElementDefinition
     */
    private void handleUndeployState(final AutomationComposition automationComposition, Integer startPhaseMsg,
            List<AutomationCompositionElementDefinition> acElementDefinitions) {

        automationComposition.getElements().values().stream()
                .forEach(acElement -> automationCompositionElementUndeploy(automationComposition.getInstanceId(),
                        acElement, startPhaseMsg, acElementDefinitions));

        boolean isAllUninitialised = automationComposition.getElements().values().stream()
                .filter(element -> !DeployState.UNDEPLOYED.equals(element.getDeployState())).findAny().isEmpty();
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
     * @param startPhaseMsg startPhase from message
     * @param acElementDefinitions the list of AutomationCompositionElementDefinition
     */
    private void handleLockState(final AutomationComposition automationComposition, Integer startPhaseMsg,
            List<AutomationCompositionElementDefinition> acElementDefinitions) {
        automationComposition.getElements().values().stream()
                .forEach(acElement -> automationCompositionElementLock(automationComposition.getInstanceId(), acElement,
                        startPhaseMsg, acElementDefinitions));
    }

    /**
     * Method to handle when the new state from participant is RUNNING state.
     *
     * @param automationComposition participant response
     * @param startPhaseMsg startPhase from message
     * @param acElementDefinitions the list of AutomationCompositionElementDefinition
     */
    private void handleUnlockState(final AutomationComposition automationComposition, Integer startPhaseMsg,
            List<AutomationCompositionElementDefinition> acElementDefinitions) {
        automationComposition.getElements().values().stream()
                .forEach(acElement -> automationCompositionElementUnlock(automationComposition.getInstanceId(),
                        acElement, startPhaseMsg, acElementDefinitions));
    }

    private void automationCompositionElementLock(UUID instanceId, AutomationCompositionElement acElement,
            Integer startPhaseMsg, List<AutomationCompositionElementDefinition> acElementDefinitions) {
        var acElementNodeTemplate = getAcElementNodeTemplate(acElementDefinitions, acElement.getDefinition());
        if (acElementNodeTemplate != null) {
            int startPhase = ParticipantUtils.findStartPhase(acElementNodeTemplate.getProperties());
            if (startPhaseMsg.equals(startPhase)) {
                updateAutomationCompositionElementState(instanceId, acElement.getId(), DeployState.DEPLOYED,
                        LockState.LOCKED);
            }
        }
    }

    private void automationCompositionElementUnlock(UUID instanceId, AutomationCompositionElement acElement,
            Integer startPhaseMsg, List<AutomationCompositionElementDefinition> acElementDefinitions) {
        var acElementNodeTemplate = getAcElementNodeTemplate(acElementDefinitions, acElement.getDefinition());
        if (acElementNodeTemplate != null) {
            int startPhase = ParticipantUtils.findStartPhase(acElementNodeTemplate.getProperties());
            if (startPhaseMsg.equals(startPhase)) {
                updateAutomationCompositionElementState(instanceId, acElement.getId(), DeployState.DEPLOYED,
                        LockState.UNLOCKED);
            }
        }
    }

    private void automationCompositionElementUndeploy(UUID instanceId, AutomationCompositionElement acElement,
            Integer startPhaseMsg, List<AutomationCompositionElementDefinition> acElementDefinitions) {
        var acElementNodeTemplate = getAcElementNodeTemplate(acElementDefinitions, acElement.getDefinition());
        if (acElementNodeTemplate != null) {
            int startPhase = ParticipantUtils.findStartPhase(acElementNodeTemplate.getProperties());
            if (startPhaseMsg.equals(startPhase)) {
                for (var acElementListener : listeners) {
                    try {
                        acElementListener.undeploy(instanceId, acElement.getId());
                    } catch (PfModelException e) {
                        LOGGER.debug("Automation composition element update failed {}", instanceId);
                    }
                }
            }
        }
    }
}
