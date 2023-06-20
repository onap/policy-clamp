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

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDeploy;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUtils;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionDeploy;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionDeployAck;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionStateChange;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.PropertiesUpdate;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.LockOrder;
import org.onap.policy.clamp.models.acm.persistence.provider.AcInstanceStateResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/*
 * This class is responsible for managing the state of all automation compositions in the participant.
 */
@Component
public class AutomationCompositionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutomationCompositionHandler.class);

    private final CacheProvider cacheProvider;
    private final ParticipantMessagePublisher publisher;
    private final ThreadHandler listener;
    private final AcInstanceStateResolver acInstanceStateResolver;

    /**
     * Constructor, set the participant ID and messageSender.
     *
     * @param cacheProvider the Cache Provider
     * @param publisher the ParticipantMessage Publisher
     * @param listener the ThreadHandler Listener
     */
    public AutomationCompositionHandler(CacheProvider cacheProvider, ParticipantMessagePublisher publisher,
            ThreadHandler listener) {
        this.cacheProvider = cacheProvider;
        this.publisher = publisher;
        this.listener = listener;
        this.acInstanceStateResolver = new AcInstanceStateResolver();
    }

    /**
     * Handle a automation composition state change message.
     *
     * @param stateChangeMsg the state change message
     */
    public void handleAutomationCompositionStateChange(AutomationCompositionStateChange stateChangeMsg) {
        if (stateChangeMsg.getAutomationCompositionId() == null) {
            return;
        }

        var automationComposition = cacheProvider.getAutomationComposition(stateChangeMsg.getAutomationCompositionId());

        if (automationComposition == null) {
            if (DeployOrder.DELETE.equals(stateChangeMsg.getDeployOrderedState())) {
                var automationCompositionAck = new AutomationCompositionDeployAck(
                        ParticipantMessageType.AUTOMATION_COMPOSITION_STATECHANGE_ACK);
                automationCompositionAck.setParticipantId(cacheProvider.getParticipantId());
                automationCompositionAck.setMessage("Already deleted or never used");
                automationCompositionAck.setResult(true);
                automationCompositionAck.setStateChangeResult(StateChangeResult.NO_ERROR);
                automationCompositionAck.setResponseTo(stateChangeMsg.getMessageId());
                automationCompositionAck.setAutomationCompositionId(stateChangeMsg.getAutomationCompositionId());
                publisher.sendAutomationCompositionAck(automationCompositionAck);
            } else {
                LOGGER.debug("Automation composition {} does not use this participant",
                        stateChangeMsg.getAutomationCompositionId());
            }
            return;
        }

        if (!checkConsistantOrderState(automationComposition, stateChangeMsg.getDeployOrderedState(),
                stateChangeMsg.getLockOrderedState())) {
            LOGGER.warn("Not Consistant OrderState Automation composition {}",
                    stateChangeMsg.getAutomationCompositionId());
            return;
        }

        if (DeployOrder.NONE.equals(stateChangeMsg.getDeployOrderedState())) {
            handleLockOrderState(stateChangeMsg.getMessageId(), automationComposition,
                    stateChangeMsg.getLockOrderedState(), stateChangeMsg.getStartPhase());
        } else {
            handleDeployOrderState(stateChangeMsg.getMessageId(), automationComposition,
                    stateChangeMsg.getDeployOrderedState(), stateChangeMsg.getStartPhase());
        }
    }

    private boolean checkConsistantOrderState(AutomationComposition automationComposition, DeployOrder deployOrder,
            LockOrder lockOrder) {
        if (DeployOrder.UPDATE.equals(deployOrder)) {
            return true;
        }
        return acInstanceStateResolver.resolve(deployOrder, lockOrder, automationComposition.getDeployState(),
                automationComposition.getLockState(), automationComposition.getStateChangeResult()) != null;
    }

    /**
     * Method to handle state changes.
     *
     * @param messageId the messageId
     * @param automationComposition participant response
     * @param orderedState automation composition ordered state
     * @param startPhaseMsg startPhase from message
     */
    private void handleDeployOrderState(UUID messageId, final AutomationComposition automationComposition,
            DeployOrder orderedState, Integer startPhaseMsg) {

        switch (orderedState) {
            case UNDEPLOY:
                handleUndeployState(messageId, automationComposition, startPhaseMsg);
                break;
            case DELETE:
                handleDeleteState(messageId, automationComposition, startPhaseMsg);
                break;

            default:
                LOGGER.error("StateChange message has no state, state is null {}", automationComposition.getKey());
                break;
        }
    }

    /**
     * Method to handle state changes.
     *
     * @param messageId the messageId
     * @param automationComposition participant response
     * @param orderedState automation composition ordered state
     * @param startPhaseMsg startPhase from message
     */
    private void handleLockOrderState(UUID messageId, final AutomationComposition automationComposition,
            LockOrder orderedState, Integer startPhaseMsg) {

        switch (orderedState) {
            case LOCK:
                handleLockState(messageId, automationComposition, startPhaseMsg);
                break;
            case UNLOCK:
                handleUnlockState(messageId, automationComposition, startPhaseMsg);
                break;
            default:
                LOGGER.error("StateChange message has no state, state is null {}", automationComposition.getKey());
                break;
        }
    }

    /**
     * Handle a automation composition properties update message.
     *
     * @param updateMsg the properties update message
     */
    public void handleAcPropertyUpdate(PropertiesUpdate updateMsg) {

        if (updateMsg.getParticipantUpdatesList().isEmpty()) {
            LOGGER.warn("No AutomationCompositionElement updates in message {}",
                    updateMsg.getAutomationCompositionId());
            return;
        }

        for (var participantDeploy : updateMsg.getParticipantUpdatesList()) {
            if (cacheProvider.getParticipantId().equals(participantDeploy.getParticipantId())) {

                updateExistingElementsOnThisParticipant(updateMsg.getAutomationCompositionId(), participantDeploy);

                callParticipantUpdateProperty(updateMsg.getMessageId(), participantDeploy.getAcElementList(),
                        updateMsg.getAutomationCompositionId());
            }
        }
    }

    /**
     * Handle a automation composition Deploy message.
     *
     * @param deployMsg the Deploy message
     */
    public void handleAutomationCompositionDeploy(AutomationCompositionDeploy deployMsg) {

        if (deployMsg.getParticipantUpdatesList().isEmpty()) {
            LOGGER.warn("No AutomationCompositionElement deploy in message {}", deployMsg.getAutomationCompositionId());
            return;
        }

        for (var participantDeploy : deployMsg.getParticipantUpdatesList()) {
            if (cacheProvider.getParticipantId().equals(participantDeploy.getParticipantId())) {
                if (deployMsg.isFirstStartPhase()) {
                    cacheProvider.initializeAutomationComposition(deployMsg.getCompositionId(),
                            deployMsg.getAutomationCompositionId(), participantDeploy);
                }
                callParticipanDeploy(deployMsg.getMessageId(), participantDeploy.getAcElementList(),
                        deployMsg.getStartPhase(), deployMsg.getAutomationCompositionId());
            }
        }
    }

    private void callParticipanDeploy(UUID messageId, List<AcElementDeploy> acElements, Integer startPhaseMsg,
            UUID instanceId) {
        for (var element : acElements) {
            var commonProperties = cacheProvider.getCommonProperties(instanceId, element.getId());
            int startPhase = ParticipantUtils.findStartPhase(commonProperties);
            if (startPhaseMsg.equals(startPhase)) {
                var map = new HashMap<>(commonProperties);
                map.putAll(element.getProperties());
                listener.deploy(messageId, instanceId, element, map);
            }
        }
    }

    private void callParticipantUpdateProperty(UUID messageId, List<AcElementDeploy> acElements, UUID instanceId) {
        for (var element : acElements) {
            listener.update(messageId, instanceId, element, element.getProperties());
        }
    }

    private void updateExistingElementsOnThisParticipant(UUID instanceId, ParticipantDeploy participantDeploy) {
        var acElementList = cacheProvider.getAutomationComposition(instanceId).getElements();
        for (var element : participantDeploy.getAcElementList()) {
            var acElement = acElementList.get(element.getId());
            acElement.getProperties().putAll(element.getProperties());
        }
    }

    /**
     * Method to handle when the new state from participant is UNINITIALISED state.
     *
     * @param messageId the messageId
     * @param automationComposition participant response
     * @param startPhaseMsg startPhase from message
     */
    private void handleUndeployState(UUID messageId, final AutomationComposition automationComposition,
            Integer startPhaseMsg) {
        for (var acElement : automationComposition.getElements().values()) {
            int startPhase = ParticipantUtils.findStartPhase(
                    cacheProvider.getCommonProperties(automationComposition.getInstanceId(), acElement.getId()));
            if (startPhaseMsg.equals(startPhase)) {
                listener.undeploy(messageId, automationComposition.getInstanceId(), acElement.getId());
            }
        }
    }

    private void handleDeleteState(UUID messageId, final AutomationComposition automationComposition,
            Integer startPhaseMsg) {
        for (var acElement : automationComposition.getElements().values()) {
            int startPhase = ParticipantUtils.findStartPhase(
                    cacheProvider.getCommonProperties(automationComposition.getInstanceId(), acElement.getId()));
            if (startPhaseMsg.equals(startPhase)) {
                listener.delete(messageId, automationComposition.getInstanceId(), acElement.getId());
            }
        }
    }

    /**
     * Method to handle when the new state from participant is PASSIVE state.
     *
     * @param messageId the messageId
     * @param automationComposition participant response
     * @param startPhaseMsg startPhase from message
     */
    private void handleLockState(UUID messageId, final AutomationComposition automationComposition,
            Integer startPhaseMsg) {
        for (var acElement : automationComposition.getElements().values()) {
            int startPhase = ParticipantUtils.findStartPhase(
                    cacheProvider.getCommonProperties(automationComposition.getInstanceId(), acElement.getId()));
            if (startPhaseMsg.equals(startPhase)) {
                listener.lock(messageId, automationComposition.getInstanceId(), acElement.getId());
            }
        }
    }

    /**
     * Method to handle when the new state from participant is RUNNING state.
     *
     * @param messageId the messageId
     * @param automationComposition participant response
     * @param startPhaseMsg startPhase from message
     */
    private void handleUnlockState(UUID messageId, final AutomationComposition automationComposition,
            Integer startPhaseMsg) {
        for (var acElement : automationComposition.getElements().values()) {
            int startPhase = ParticipantUtils.findStartPhase(
                    cacheProvider.getCommonProperties(automationComposition.getInstanceId(), acElement.getId()));
            if (startPhaseMsg.equals(startPhase)) {
                listener.unlock(messageId, automationComposition.getInstanceId(), acElement.getId());
            }
        }
    }

    /**
     * Handles prime a Composition Definition.
     *
     * @param messageId the messageId
     * @param compositionId the compositionId
     * @param list the list of AutomationCompositionElementDefinition
     */
    public void prime(UUID messageId, UUID compositionId, List<AutomationCompositionElementDefinition> list) {
        listener.prime(messageId, compositionId, list);
    }

    /**
     * Handles deprime a Composition Definition.
     *
     * @param messageId the messageId
     * @param compositionId the compositionId
     */
    public void deprime(UUID messageId, UUID compositionId) {
        listener.deprime(messageId, compositionId);
    }
}
