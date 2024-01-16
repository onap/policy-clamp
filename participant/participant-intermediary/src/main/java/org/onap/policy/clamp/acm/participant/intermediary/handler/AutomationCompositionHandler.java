/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation.
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.InstanceElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDeploy;
import org.onap.policy.clamp.models.acm.concepts.ParticipantRestartAc;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUtils;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeploy;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeployAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionMigration;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionStateChange;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrimeAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.PropertiesUpdate;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.LockOrder;
import org.onap.policy.clamp.models.acm.persistence.provider.AcInstanceStateResolver;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
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

                var acCopy = new AutomationComposition(cacheProvider.getAutomationComposition(
                    updateMsg.getAutomationCompositionId()));
                updateExistingElementsOnThisParticipant(updateMsg.getAutomationCompositionId(), participantDeploy,
                        DeployState.UPDATING);

                callParticipantUpdateProperty(updateMsg.getMessageId(), participantDeploy.getAcElementList(), acCopy);
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

    private void callParticipanDeploy(UUID messageId, List<AcElementDeploy> acElementDeployList,
            Integer startPhaseMsg, UUID instanceId) {
        var automationComposition = cacheProvider.getAutomationComposition(instanceId);
        for (var elementDeploy : acElementDeployList) {
            var element = automationComposition.getElements().get(elementDeploy.getId());
            var compositionInProperties = cacheProvider
                .getCommonProperties(automationComposition.getCompositionId(), element.getDefinition());
            int startPhase = ParticipantUtils.findStartPhase(compositionInProperties);
            if (startPhaseMsg.equals(startPhase)) {
                var compositionElement = createCompositionElementDto(automationComposition.getCompositionId(),
                    element, compositionInProperties);
                var instanceElement = new InstanceElementDto(instanceId, elementDeploy.getId(),
                    elementDeploy.getToscaServiceTemplateFragment(),
                    elementDeploy.getProperties(), element.getOutProperties());
                listener.deploy(messageId, compositionElement, instanceElement);
            }
        }
    }

    private CompositionElementDto createCompositionElementDto(UUID compositionId, AutomationCompositionElement element,
        Map<String, Object> compositionInProperties) {
        var compositionOutProperties = cacheProvider.getAcElementsDefinitions()
            .get(compositionId).get(element.getDefinition()).getOutProperties();
        return new CompositionElementDto(compositionId,
            element.getDefinition(), compositionInProperties, compositionOutProperties);
    }

    private Map<UUID, CompositionElementDto> getCompositionElementDtoMap(AutomationComposition automationComposition,
        UUID compositionId) {
        Map<UUID, CompositionElementDto> map = new HashMap<>();
        for (var element : automationComposition.getElements().values()) {
            var compositionInProperties = cacheProvider.getCommonProperties(compositionId, element.getDefinition());
            var compositionElement = createCompositionElementDto(compositionId, element, compositionInProperties);
            map.put(element.getId(), compositionElement);
        }
        return map;
    }

    private Map<UUID, CompositionElementDto> getCompositionElementDtoMap(AutomationComposition automationComposition) {
        return getCompositionElementDtoMap(automationComposition, automationComposition.getCompositionId());
    }

    private Map<UUID, InstanceElementDto> getInstanceElementDtoMap(AutomationComposition automationComposition) {
        Map<UUID, InstanceElementDto> map = new HashMap<>();
        for (var element : automationComposition.getElements().values()) {
            var instanceElement = new InstanceElementDto(automationComposition.getInstanceId(), element.getId(),
                null, element.getProperties(), element.getOutProperties());
            map.put(element.getId(), instanceElement);
        }
        return map;
    }

    private void callParticipantUpdateProperty(UUID messageId, List<AcElementDeploy> acElements,
        AutomationComposition acCopy) {
        var instanceElementDtoMap = getInstanceElementDtoMap(acCopy);
        var instanceElementDtoMapUpdated = getInstanceElementDtoMap(
            cacheProvider.getAutomationComposition(acCopy.getInstanceId()));
        var compositionElementDtoMap = getCompositionElementDtoMap(acCopy);
        for (var acElement : acElements) {
            listener.update(messageId, compositionElementDtoMap.get(acElement.getId()),
                instanceElementDtoMap.get(acElement.getId()), instanceElementDtoMapUpdated.get(acElement.getId()));
        }
    }

    private void updateExistingElementsOnThisParticipant(UUID instanceId, ParticipantDeploy participantDeploy,
        DeployState deployState) {
        var acElementList = cacheProvider.getAutomationComposition(instanceId).getElements();
        for (var element : participantDeploy.getAcElementList()) {
            var acElement = acElementList.get(element.getId());
            AcmUtils.recursiveMerge(acElement.getProperties(), element.getProperties());
            acElement.setDeployState(deployState);
            acElement.setDefinition(element.getDefinition());
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
        automationComposition.setCompositionTargetId(null);
        for (var element : automationComposition.getElements().values()) {
            var compositionInProperties = cacheProvider
                .getCommonProperties(automationComposition.getCompositionId(), element.getDefinition());
            int startPhase = ParticipantUtils.findStartPhase(compositionInProperties);
            if (startPhaseMsg.equals(startPhase)) {
                element.setDeployState(DeployState.UNDEPLOYING);
                var compositionElement = createCompositionElementDto(automationComposition.getCompositionId(),
                    element, compositionInProperties);
                var instanceElement = new InstanceElementDto(automationComposition.getInstanceId(), element.getId(),
                    null, element.getProperties(), element.getOutProperties());
                listener.undeploy(messageId, compositionElement, instanceElement);
            }
        }
    }

    private void handleDeleteState(UUID messageId, final AutomationComposition automationComposition,
            Integer startPhaseMsg) {
        for (var element : automationComposition.getElements().values()) {
            var compositionInProperties = cacheProvider
                .getCommonProperties(automationComposition.getCompositionId(), element.getDefinition());
            int startPhase = ParticipantUtils.findStartPhase(compositionInProperties);
            if (startPhaseMsg.equals(startPhase)) {
                element.setDeployState(DeployState.DELETING);
                var compositionElement = createCompositionElementDto(automationComposition.getCompositionId(),
                    element, compositionInProperties);
                var instanceElement = new InstanceElementDto(automationComposition.getInstanceId(), element.getId(),
                    null, element.getProperties(), element.getOutProperties());
                listener.delete(messageId, compositionElement, instanceElement);
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
        for (var element : automationComposition.getElements().values()) {
            var compositionInProperties = cacheProvider
                .getCommonProperties(automationComposition.getCompositionId(), element.getDefinition());
            int startPhase = ParticipantUtils.findStartPhase(compositionInProperties);
            if (startPhaseMsg.equals(startPhase)) {
                element.setLockState(LockState.LOCKING);
                var compositionElement = createCompositionElementDto(automationComposition.getCompositionId(),
                    element, compositionInProperties);
                var instanceElement = new InstanceElementDto(automationComposition.getInstanceId(), element.getId(),
                    null, element.getProperties(), element.getOutProperties());
                listener.lock(messageId, compositionElement, instanceElement);
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
        for (var element : automationComposition.getElements().values()) {
            var compositionInProperties = cacheProvider
                .getCommonProperties(automationComposition.getCompositionId(), element.getDefinition());
            int startPhase = ParticipantUtils.findStartPhase(compositionInProperties);
            if (startPhaseMsg.equals(startPhase)) {
                element.setLockState(LockState.UNLOCKING);
                var compositionElement = createCompositionElementDto(automationComposition.getCompositionId(),
                    element, compositionInProperties);
                var instanceElement = new InstanceElementDto(automationComposition.getInstanceId(), element.getId(),
                    null, element.getProperties(), element.getOutProperties());
                listener.unlock(messageId, compositionElement, instanceElement);
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
        var inPropertiesMap = list.stream().collect(Collectors.toMap(
            AutomationCompositionElementDefinition::getAcElementDefinitionId,
            el -> el.getAutomationCompositionElementToscaNodeTemplate().getProperties()));
        var outPropertiesMap = list.stream().collect(Collectors.toMap(
            AutomationCompositionElementDefinition::getAcElementDefinitionId,
            AutomationCompositionElementDefinition::getOutProperties));
        listener.prime(messageId, new CompositionDto(compositionId, inPropertiesMap, outPropertiesMap));
    }

    /**
     * Handles deprime a Composition Definition.
     *
     * @param messageId the messageId
     * @param compositionId the compositionId
     */
    public void deprime(UUID messageId, UUID compositionId) {
        var acElementsDefinitions = cacheProvider.getAcElementsDefinitions().get(compositionId);
        if (acElementsDefinitions == null) {
            // this participant does not handle this composition
            var participantPrimeAck = new ParticipantPrimeAck();
            participantPrimeAck.setCompositionId(compositionId);
            participantPrimeAck.setMessage("Already deprimed or never primed");
            participantPrimeAck.setResult(true);
            participantPrimeAck.setResponseTo(messageId);
            participantPrimeAck.setCompositionState(AcTypeState.COMMISSIONED);
            participantPrimeAck.setStateChangeResult(StateChangeResult.NO_ERROR);
            participantPrimeAck.setParticipantId(cacheProvider.getParticipantId());
            participantPrimeAck.setState(ParticipantState.ON_LINE);
            publisher.sendParticipantPrimeAck(participantPrimeAck);
            return;
        }
        var list = new ArrayList<>(acElementsDefinitions.values());
        var inPropertiesMap = list.stream().collect(Collectors.toMap(
            AutomationCompositionElementDefinition::getAcElementDefinitionId,
            el -> el.getAutomationCompositionElementToscaNodeTemplate().getProperties()));
        var outPropertiesMap = list.stream().collect(Collectors.toMap(
            AutomationCompositionElementDefinition::getAcElementDefinitionId,
            AutomationCompositionElementDefinition::getOutProperties));
        listener.deprime(messageId, new CompositionDto(compositionId, inPropertiesMap, outPropertiesMap));
    }

    /**
     * Handles restarted scenario.
     *
     * @param messageId the messageId
     * @param compositionId the compositionId
     * @param list the list of AutomationCompositionElementDefinition
     * @param state the state of the composition
     * @param automationCompositionList list of ParticipantRestartAc
     */
    public void restarted(UUID messageId, UUID compositionId, List<AutomationCompositionElementDefinition> list,
            AcTypeState state, List<ParticipantRestartAc> automationCompositionList) {

        for (var automationcomposition : automationCompositionList) {
            cacheProvider.initializeAutomationComposition(compositionId, automationcomposition);
        }
        var inPropertiesMap = list.stream().collect(Collectors.toMap(
            AutomationCompositionElementDefinition::getAcElementDefinitionId,
            el -> el.getAutomationCompositionElementToscaNodeTemplate().getProperties()));
        var outPropertiesMap = list.stream().collect(Collectors.toMap(
            AutomationCompositionElementDefinition::getAcElementDefinitionId,
            AutomationCompositionElementDefinition::getOutProperties));
        var composition = new CompositionDto(compositionId, inPropertiesMap, outPropertiesMap);
        listener.restarted(messageId, composition, state, automationCompositionList);
    }

    /**
     * Handles AutomationComposition Migration.
     *
     * @param migrationMsg the AutomationCompositionMigration
     */
    public void handleAutomationCompositionMigration(AutomationCompositionMigration migrationMsg) {
        if (migrationMsg.getAutomationCompositionId() == null || migrationMsg.getCompositionTargetId() == null) {
            return;
        }

        var automationComposition = cacheProvider.getAutomationComposition(migrationMsg.getAutomationCompositionId());
        if (automationComposition == null) {
            LOGGER.debug("Automation composition {} does not use this participant",
                    migrationMsg.getAutomationCompositionId());
            return;
        }
        var acCopy = new AutomationComposition(automationComposition);
        automationComposition.setCompositionTargetId(migrationMsg.getCompositionTargetId());
        for (var participantDeploy : migrationMsg.getParticipantUpdatesList()) {
            if (cacheProvider.getParticipantId().equals(participantDeploy.getParticipantId())) {

                updateExistingElementsOnThisParticipant(migrationMsg.getAutomationCompositionId(), participantDeploy,
                        DeployState.MIGRATING);

                callParticipantMigrate(migrationMsg.getMessageId(), participantDeploy.getAcElementList(),
                    acCopy, migrationMsg.getCompositionTargetId());
            }
        }
    }

    private void callParticipantMigrate(UUID messageId, List<AcElementDeploy> acElements,
            AutomationComposition acCopy, UUID compositionTargetId) {
        var compositionElementMap = getCompositionElementDtoMap(acCopy);
        var instanceElementMap = getInstanceElementDtoMap(acCopy);
        var automationComposition = cacheProvider.getAutomationComposition(acCopy.getInstanceId());
        var compositionElementTargetMap = getCompositionElementDtoMap(automationComposition, compositionTargetId);
        var instanceElementMigrateMap = getInstanceElementDtoMap(automationComposition);

        for (var acElement : acElements) {
            listener.migrate(messageId, compositionElementMap.get(acElement.getId()),
                compositionElementTargetMap.get(acElement.getId()),
                instanceElementMap.get(acElement.getId()), instanceElementMigrateMap.get(acElement.getId()));
        }
    }
}
