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
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementInfo;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionInfo;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDeploy;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantSupportedElementType;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUtils;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionDeploy;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionDeployAck;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionStateChange;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantStatus;
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
    private final List<ParticipantSupportedElementType> supportedAcElementTypes;
    private final List<AutomationCompositionElementListener> listeners = new ArrayList<>();

    @Getter
    private final Map<UUID, AutomationComposition> automationCompositionMap = new LinkedHashMap<>();

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
        this.supportedAcElementTypes = parameters.getIntermediaryParameters().getParticipantSupportedElementTypes();
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
     * @param lockState the LockState state
     */
    public void updateAutomationCompositionElementState(UUID automationCompositionId, UUID id, DeployState deployState,
            LockState lockState, String message) {

        if (automationCompositionId == null || id == null) {
            LOGGER.error("Cannot update Automation composition element state, id is null");
            return;
        }

        if ((deployState != null && lockState != null) || (deployState == null && lockState == null)) {
            LOGGER.error("state error {} and {} cannot be handled", deployState, lockState);
            return;
        }

        var automationComposition = automationCompositionMap.get(automationCompositionId);
        if (automationComposition == null) {
            LOGGER.error("Cannot update Automation composition element state, Automation composition id {} not present",
                    automationComposition);
            return;
        }

        var element = automationComposition.getElements().get(id);
        if (element == null) {
            var msg = "Cannot update Automation composition element state, AC Element id {} not present";
            LOGGER.error(msg, automationComposition);
            return;
        }

        if (deployState != null) {
            element.setDeployState(deployState);
            element.setLockState(
                    DeployState.DEPLOYED.equals(element.getDeployState()) ? LockState.LOCKED : LockState.NONE);
            var checkOpt = automationComposition.getElements().values().stream()
                    .filter(acElement -> !deployState.equals(acElement.getDeployState())).findAny();
            if (checkOpt.isEmpty()) {
                automationComposition.setDeployState(deployState);
                automationComposition.setLockState(element.getLockState());
            }
        }
        if (lockState != null) {
            element.setLockState(lockState);
            var checkOpt = automationComposition.getElements().values().stream()
                    .filter(acElement -> !lockState.equals(acElement.getLockState())).findAny();
            if (checkOpt.isEmpty()) {
                automationComposition.setLockState(lockState);
            }
        }

        var automationCompositionStateChangeAck =
                new AutomationCompositionDeployAck(ParticipantMessageType.AUTOMATION_COMPOSITION_STATECHANGE_ACK);
        automationCompositionStateChangeAck.setParticipantId(participantId);
        automationCompositionStateChangeAck.setMessage(message);
        automationCompositionStateChangeAck.setAutomationCompositionId(automationCompositionId);
        automationCompositionStateChangeAck.getAutomationCompositionResultMap().put(element.getId(),
                new AcElementDeployAck(element.getDeployState(), element.getLockState(), element.getOperationalState(),
                        element.getUseState(), element.getOutProperties(), true,
                        "Automation composition element {} state changed to {}\", id, newState)"));
        LOGGER.debug("Automation composition element {} state changed to {}", id, deployState);
        automationCompositionStateChangeAck.setResult(true);
        publisher.sendAutomationCompositionAck(automationCompositionStateChangeAck);
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
            LOGGER.warn("Not Consistant OrderState Automation composition {}",
                    stateChangeMsg.getAutomationCompositionId());
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

        switch (orderedState) {
            case UNDEPLOY:
                handleUndeployState(automationComposition, startPhaseMsg, acElementDefinitions);
                break;
            case DELETE:
                handleDeleteState(automationComposition, startPhaseMsg, acElementDefinitions);
                break;

            default:
                LOGGER.debug("StateChange message has no state, state is null {}", automationComposition.getKey());
                break;
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
        var automationComposition = new AutomationComposition();
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
            acElement.setParticipantId(participantDeploy.getParticipantId());
            acElement.setDefinition(element.getDefinition());
            acElement.setDeployState(DeployState.DEPLOYING);
            acElement.setLockState(LockState.NONE);
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
    }

    private void handleDeleteState(final AutomationComposition automationComposition, Integer startPhaseMsg,
            List<AutomationCompositionElementDefinition> acElementDefinitions) {

        automationComposition.getElements().values().stream()
                .forEach(acElement -> automationCompositionElementDelete(automationComposition.getInstanceId(),
                        acElement, startPhaseMsg, acElementDefinitions));

        boolean isAllUninitialised = automationComposition.getElements().values().stream()
                .filter(element -> !DeployState.DELETED.equals(element.getDeployState())).findAny().isEmpty();
        if (isAllUninitialised) {
            automationCompositionMap.remove(automationComposition.getInstanceId());
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
                for (var acElementListener : listeners) {
                    try {
                        acElementListener.lock(instanceId, acElement.getId());
                        updateAutomationCompositionElementState(instanceId, acElement.getId(), null, LockState.LOCKED,
                                "Locked");
                    } catch (PfModelException e) {
                        LOGGER.error("Automation composition element lock failed {}", instanceId);
                    }
                }
            }
        }
    }

    private void automationCompositionElementUnlock(UUID instanceId, AutomationCompositionElement acElement,
            Integer startPhaseMsg, List<AutomationCompositionElementDefinition> acElementDefinitions) {
        var acElementNodeTemplate = getAcElementNodeTemplate(acElementDefinitions, acElement.getDefinition());
        if (acElementNodeTemplate != null) {
            int startPhase = ParticipantUtils.findStartPhase(acElementNodeTemplate.getProperties());
            if (startPhaseMsg.equals(startPhase)) {
                for (var acElementListener : listeners) {
                    try {
                        acElementListener.unlock(instanceId, acElement.getId());
                        updateAutomationCompositionElementState(instanceId, acElement.getId(), null, LockState.UNLOCKED,
                                "Unlocked");
                    } catch (PfModelException e) {
                        LOGGER.error("Automation composition element unlock failed {}", instanceId);
                    }
                }
            }
        }
    }

    private void automationCompositionElementUndeploy(UUID instanceId, AutomationCompositionElement acElement,
            Integer startPhaseMsg, List<AutomationCompositionElementDefinition> acElementDefinitions) {
        var acElementNodeTemplate = getAcElementNodeTemplate(acElementDefinitions, acElement.getDefinition());
        if (acElementNodeTemplate != null) {
            int startPhase = ParticipantUtils.findStartPhase(acElementNodeTemplate.getProperties());
            if (startPhaseMsg.equals(startPhase)) {
                undeployInstanceElements(instanceId, acElement.getId());
            }
        }
    }

    private void automationCompositionElementDelete(UUID instanceId, AutomationCompositionElement acElement,
            Integer startPhaseMsg, List<AutomationCompositionElementDefinition> acElementDefinitions) {
        var acElementNodeTemplate = getAcElementNodeTemplate(acElementDefinitions, acElement.getDefinition());
        if (acElementNodeTemplate != null) {
            int startPhase = ParticipantUtils.findStartPhase(acElementNodeTemplate.getProperties());
            if (startPhaseMsg.equals(startPhase)) {
                for (var acElementListener : listeners) {
                    try {
                        acElementListener.delete(instanceId, acElement.getId());
                        updateAutomationCompositionElementState(instanceId, acElement.getId(), DeployState.DELETED,
                                null, "Deleted");
                    } catch (PfModelException e) {
                        LOGGER.error("Automation composition element unlock failed {}", instanceId);
                    }
                }
            }
        }
    }

    /**
     * Undeploy Instance Elements On Participant.
     */
    public void undeployInstances() {
        automationCompositionMap.values().forEach(this::undeployInstance);
    }

    private void undeployInstance(AutomationComposition automationComposition) {
        automationComposition.getElements().values().forEach(element -> {
            if (element.getParticipantId().equals(participantId)) {
                undeployInstanceElements(automationComposition.getInstanceId(), element.getId());
            }
        });
    }

    private void undeployInstanceElements(UUID instanceId, UUID elementId) {
        for (var acElementListener : listeners) {
            try {
                acElementListener.undeploy(instanceId, elementId);
            } catch (PfModelException e) {
                LOGGER.error("Automation composition element update failed {}", instanceId);
            }
        }
    }

    /**
     * Send Ac Element Info.
     *
     * @param automationCompositionId the automationComposition Id
     * @param elementId the automationComposition Element id
     * @param useState the use State
     * @param operationalState the operational State
     * @param outProperties the output Properties Map
     */
    public void sendAcElementInfo(UUID automationCompositionId, UUID elementId, String useState,
            String operationalState, Map<String, Object> outProperties) {

        if (automationCompositionId == null || elementId == null) {
            LOGGER.error("Cannot update Automation composition element state, id is null");
            return;
        }

        var automationComposition = automationCompositionMap.get(automationCompositionId);
        if (automationComposition == null) {
            LOGGER.error("Cannot update Automation composition element state, Automation composition id {} not present",
                    automationComposition);
            return;
        }

        var element = automationComposition.getElements().get(elementId);
        if (element == null) {
            var msg = "Cannot update Automation composition element state, AC Element id {} not present";
            LOGGER.error(msg, automationComposition);
            return;
        }
        element.setOperationalState(operationalState);
        element.setUseState(useState);
        element.setOutProperties(outProperties);

        var statusMsg = new ParticipantStatus();
        statusMsg.setParticipantId(participantId);
        statusMsg.setState(ParticipantState.ON_LINE);
        statusMsg.setParticipantSupportedElementType(new ArrayList<>(supportedAcElementTypes));
        var acInfo = new AutomationCompositionInfo();
        acInfo.setAutomationCompositionId(automationCompositionId);
        acInfo.setDeployState(automationComposition.getDeployState());
        acInfo.setLockState(automationComposition.getLockState());
        acInfo.setElements(List.of(getAutomationCompositionElementInfo(element)));
        statusMsg.setAutomationCompositionInfoList(List.of(acInfo));
        publisher.sendParticipantStatus(statusMsg);
    }

    /**
     * get AutomationComposition Info List.
     *
     * @return list of AutomationCompositionInfo
     */
    public List<AutomationCompositionInfo> getAutomationCompositionInfoList() {
        List<AutomationCompositionInfo> automationCompositionInfoList = new ArrayList<>();
        for (var entry : automationCompositionMap.entrySet()) {
            var acInfo = new AutomationCompositionInfo();
            acInfo.setAutomationCompositionId(entry.getKey());
            acInfo.setDeployState(entry.getValue().getDeployState());
            acInfo.setLockState(entry.getValue().getLockState());
            for (var element : entry.getValue().getElements().values()) {
                acInfo.getElements().add(getAutomationCompositionElementInfo(element));
            }
            automationCompositionInfoList.add(acInfo);
        }
        return automationCompositionInfoList;
    }

    private AutomationCompositionElementInfo getAutomationCompositionElementInfo(AutomationCompositionElement element) {
        var elementInfo = new AutomationCompositionElementInfo();
        elementInfo.setAutomationCompositionElementId(element.getId());
        elementInfo.setDeployState(element.getDeployState());
        elementInfo.setLockState(element.getLockState());
        elementInfo.setOperationalState(element.getOperationalState());
        elementInfo.setUseState(element.getUseState());
        elementInfo.setOutProperties(element.getOutProperties());
        return elementInfo;
    }
}
