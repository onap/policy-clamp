/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation.
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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeployAck;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementInfo;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionInfo;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeployAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrimeAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatus;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AutomationCompositionOutHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutomationCompositionOutHandler.class);

    private final ParticipantMessagePublisher publisher;
    private final CacheProvider cacheProvider;

    /**
     * Handle a automation composition element stage change message.
     *
     * @param instance the automationComposition Id
     * @param elementId the automationComposition Element Id
     * @param stage the next stage
     * @param message the message
     * @param stateChangeResult the indicator if error occurs
     */
    public void updateAutomationCompositionElementStage(UUID instance, UUID elementId,
        StateChangeResult stateChangeResult, int stage, String message) {
        if (!validateData(instance, elementId, stateChangeResult)) {
            return;
        }

        var automationComposition = cacheProvider.getAutomationComposition(instance);
        if (automationComposition == null) {
            LOGGER.error("Cannot update Automation composition element stage, Automation composition id {} not present",
                instance);
            return;
        }

        var element = automationComposition.getElements().get(elementId);
        if (element == null) {
            var msg = "Cannot update Automation composition element stage, AC Element id {} not present";
            LOGGER.error(msg, elementId);
            return;
        }

        var automationCompositionStateChangeAck =
            new AutomationCompositionDeployAck(ParticipantMessageType.AUTOMATION_COMPOSITION_STATECHANGE_ACK);
        automationCompositionStateChangeAck.setParticipantId(cacheProvider.getParticipantId());
        automationCompositionStateChangeAck.setMessage(message);
        automationCompositionStateChangeAck.setResponseTo(cacheProvider.getMsgIdentification().get(element.getId()));
        automationCompositionStateChangeAck.setStateChangeResult(stateChangeResult);
        automationCompositionStateChangeAck.setStage(stage);
        automationCompositionStateChangeAck.setAutomationCompositionId(instance);
        automationCompositionStateChangeAck.getAutomationCompositionResultMap().put(element.getId(),
            new AcElementDeployAck(element.getDeployState(), element.getLockState(), element.getOperationalState(),
                element.getUseState(), element.getOutProperties(), true, message));
        LOGGER.debug("Automation composition element {} stage changed to {}", elementId, stage);
        automationCompositionStateChangeAck.setResult(true);
        publisher.sendAutomationCompositionAck(automationCompositionStateChangeAck);
        cacheProvider.getMsgIdentification().remove(element.getId());
    }

    private boolean validateData(UUID instance, UUID elementId, StateChangeResult stateChangeResult) {
        if (instance == null || elementId == null) {
            LOGGER.error("Not valid Ac instance, id is null");
            return false;
        }
        if (stateChangeResult == null) {
            LOGGER.error("Not valid Ac instance, stateChangeResult is null");
            return false;
        }
        if (!StateChangeResult.NO_ERROR.equals(stateChangeResult)
                && !StateChangeResult.FAILED.equals(stateChangeResult)) {
            LOGGER.error("Not valid Ac instance, stateChangeResult is not valid");
            return false;
        }
        return true;
    }

    /**
     * Handle a automation composition element state change message.
     *
     * @param instance the automationComposition Id
     * @param elementId the automationComposition Element Id
     * @param deployState the DeployState state
     * @param lockState the LockState state
     * @param message the message
     * @param stateChangeResult the indicator if error occurs
     */
    public void updateAutomationCompositionElementState(UUID instance, UUID elementId,
            DeployState deployState, LockState lockState, StateChangeResult stateChangeResult, String message) {
        if (!validateData(instance, elementId, stateChangeResult)) {
            return;
        }

        if ((deployState != null && lockState != null) || (deployState == null && lockState == null)
                || AcmUtils.isInTransitionalState(deployState, lockState, SubState.NONE)) {
            LOGGER.error("state error {} and {} cannot be handled", deployState, lockState);
            return;
        }

        var automationComposition = cacheProvider.getAutomationComposition(instance);
        if (automationComposition == null) {
            LOGGER.error("Cannot update Automation composition element state, Automation composition id {} not present",
                instance);
            return;
        }

        var element = automationComposition.getElements().get(elementId);
        if (element == null) {
            var msg = "Cannot update Automation composition element state, AC Element id {} not present";
            LOGGER.error(msg, elementId);
            return;
        }

        if (deployState != null && !SubState.NONE.equals(element.getSubState())) {
            handleSubState(automationComposition, element);
            if (!StateChangeResult.NO_ERROR.equals(stateChangeResult)) {
                stateChangeResult = StateChangeResult.NO_ERROR;
                LOGGER.warn("SubState has always NO_ERROR result!");
            }
        } else if (deployState != null) {
            handleDeployState(automationComposition, element, deployState);
        }
        if (lockState != null) {
            handleLockState(automationComposition, element, lockState);
        }

        var automationCompositionStateChangeAck =
                new AutomationCompositionDeployAck(ParticipantMessageType.AUTOMATION_COMPOSITION_STATECHANGE_ACK);
        automationCompositionStateChangeAck.setParticipantId(cacheProvider.getParticipantId());
        automationCompositionStateChangeAck.setReplicaId(cacheProvider.getReplicaId());
        automationCompositionStateChangeAck.setMessage(message);
        automationCompositionStateChangeAck.setResponseTo(cacheProvider.getMsgIdentification().get(element.getId()));
        automationCompositionStateChangeAck.setStateChangeResult(stateChangeResult);
        automationCompositionStateChangeAck.setAutomationCompositionId(instance);
        automationCompositionStateChangeAck.getAutomationCompositionResultMap().put(element.getId(),
                new AcElementDeployAck(element.getDeployState(), element.getLockState(), element.getOperationalState(),
                        element.getUseState(), element.getOutProperties(), true, message));
        LOGGER.debug("Automation composition element {} state changed to {}", elementId, deployState);
        automationCompositionStateChangeAck.setResult(true);
        publisher.sendAutomationCompositionAck(automationCompositionStateChangeAck);
        cacheProvider.getMsgIdentification().remove(element.getId());
    }

    private void handleDeployState(AutomationComposition automationComposition, AutomationCompositionElement element,
            DeployState deployState) {
        element.setDeployState(deployState);
        element.setLockState(DeployState.DEPLOYED.equals(element.getDeployState()) ? LockState.LOCKED : LockState.NONE);
        var checkOpt = automationComposition.getElements().values().stream()
                .filter(acElement -> !deployState.equals(acElement.getDeployState())).findAny();
        if (checkOpt.isEmpty()) {
            if (DeployState.DEPLOYED.equals(automationComposition.getDeployState())
                    && automationComposition.getCompositionTargetId() != null) {
                // migration scenario
                automationComposition.setCompositionId(automationComposition.getCompositionTargetId());
                automationComposition.setCompositionTargetId(null);
            }
            automationComposition.setDeployState(deployState);
            automationComposition.setLockState(element.getLockState());
            automationComposition.setSubState(SubState.NONE);

            if (DeployState.DELETED.equals(deployState)) {
                cacheProvider.removeAutomationComposition(automationComposition.getInstanceId());
            }
        }
    }

    private void handleLockState(AutomationComposition automationComposition, AutomationCompositionElement element,
            LockState lockState) {
        element.setLockState(lockState);
        var checkOpt = automationComposition.getElements().values().stream()
                .filter(acElement -> !lockState.equals(acElement.getLockState())).findAny();
        if (checkOpt.isEmpty()) {
            automationComposition.setLockState(lockState);
            automationComposition.setSubState(SubState.NONE);
        }
    }

    private void handleSubState(AutomationComposition automationComposition, AutomationCompositionElement element) {
        element.setSubState(SubState.NONE);
        var checkOpt = automationComposition.getElements().values().stream()
                .filter(acElement -> !SubState.NONE.equals(acElement.getSubState())).findAny();
        if (checkOpt.isEmpty()) {
            automationComposition.setSubState(SubState.NONE);
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

        var automationComposition = cacheProvider.getAutomationComposition(automationCompositionId);
        if (automationComposition == null) {
            LOGGER.error("Cannot update Automation composition element state, Automation composition id {} not present",
                    automationCompositionId);
            return;
        }

        var element = automationComposition.getElements().get(elementId);
        if (element == null) {
            var msg = "Cannot update Automation composition element state, AC Element id {} not present";
            LOGGER.error(msg, elementId);
            return;
        }
        element.setOperationalState(operationalState);
        element.setUseState(useState);
        element.setOutProperties(outProperties);

        var acInfo = new AutomationCompositionInfo();
        acInfo.setAutomationCompositionId(automationCompositionId);
        acInfo.setDeployState(automationComposition.getDeployState());
        acInfo.setLockState(automationComposition.getLockState());
        acInfo.setElements(List.of(getAutomationCompositionElementInfo(element)));
        var statusMsg = createParticipantStatus();
        statusMsg.setCompositionId(automationComposition.getCompositionId());
        statusMsg.setAutomationCompositionInfoList(List.of(acInfo));
        publisher.sendParticipantStatus(statusMsg);
    }

    /**
     * Get AutomationCompositionElementInfo from AutomationCompositionElement.
     *
     * @param element the AutomationCompositionElement
     * @return the AutomationCompositionElementInfo
     */
    public AutomationCompositionElementInfo getAutomationCompositionElementInfo(AutomationCompositionElement element) {
        var elementInfo = new AutomationCompositionElementInfo();
        elementInfo.setAutomationCompositionElementId(element.getId());
        elementInfo.setDeployState(element.getDeployState());
        elementInfo.setLockState(element.getLockState());
        elementInfo.setOperationalState(element.getOperationalState());
        elementInfo.setUseState(element.getUseState());
        elementInfo.setOutProperties(element.getOutProperties());
        return elementInfo;
    }

    /**
     * Update Composition State for prime and deprime.
     *
     * @param compositionId the composition id
     * @param state the Composition State
     * @param stateChangeResult the indicator if error occurs
     * @param message the message
     */
    public void updateCompositionState(UUID compositionId, AcTypeState state, StateChangeResult stateChangeResult,
            String message) {
        if (compositionId == null) {
            LOGGER.error("Cannot update Automation composition definition state, id is null");
            return;
        }

        if (stateChangeResult == null) {
            LOGGER.error("Cannot update Automation composition definition state, stateChangeResult is null");
            return;
        }
        if (!StateChangeResult.NO_ERROR.equals(stateChangeResult)
                && !StateChangeResult.FAILED.equals(stateChangeResult)) {
            LOGGER.error("Cannot update Automation composition definition state, stateChangeResult is not valid");
            return;
        }

        if ((state == null) || AcTypeState.PRIMING.equals(state) || AcTypeState.DEPRIMING.equals(state)) {
            LOGGER.error("state invalid {} cannot be handled", state);
            return;
        }

        var participantPrimeAck = new ParticipantPrimeAck();
        participantPrimeAck.setCompositionId(compositionId);
        participantPrimeAck.setMessage(message);
        participantPrimeAck.setResult(true);
        participantPrimeAck.setResponseTo(cacheProvider.getMsgIdentification().get(compositionId));
        participantPrimeAck.setCompositionState(state);
        participantPrimeAck.setStateChangeResult(stateChangeResult);
        participantPrimeAck.setParticipantId(cacheProvider.getParticipantId());
        participantPrimeAck.setReplicaId(cacheProvider.getReplicaId());
        participantPrimeAck.setState(ParticipantState.ON_LINE);
        publisher.sendParticipantPrimeAck(participantPrimeAck);
        cacheProvider.getMsgIdentification().remove(compositionId);
        if (AcTypeState.COMMISSIONED.equals(state) && StateChangeResult.NO_ERROR.equals(stateChangeResult)) {
            cacheProvider.removeElementDefinition(compositionId);
        }
    }

    /**
     * Send Composition Definition Info.
     *
     * @param compositionId the composition id
     * @param elementId the Composition Definition Element id
     * @param outProperties the output Properties Map
     */
    public void sendAcDefinitionInfo(UUID compositionId, ToscaConceptIdentifier elementId,
            Map<String, Object> outProperties) {
        if (compositionId == null) {
            LOGGER.error("Cannot send Composition outProperties, id is null");
            return;
        }
        var statusMsg = createParticipantStatus();
        statusMsg.setCompositionId(compositionId);
        var acElementDefsMap = cacheProvider.getAcElementsDefinitions();
        var acElementsDefinitions = acElementDefsMap.get(compositionId);
        if (acElementsDefinitions == null) {
            LOGGER.error("Cannot send Composition outProperties, id {} is null", compositionId);
            return;
        }
        var acElementDefinition = getAutomationCompositionElementDefinition(acElementsDefinitions, elementId);
        if (acElementDefinition == null) {
            LOGGER.error("Cannot send Composition outProperties, elementId {} not present", elementId);
            return;
        }
        acElementDefinition.setOutProperties(outProperties);
        var participantDefinition = new ParticipantDefinition();
        participantDefinition.setParticipantId(cacheProvider.getParticipantId());
        participantDefinition.setAutomationCompositionElementDefinitionList(List.of(acElementDefinition));
        statusMsg.setParticipantDefinitionUpdates(List.of(participantDefinition));
        publisher.sendParticipantStatus(statusMsg);
    }

    private AutomationCompositionElementDefinition getAutomationCompositionElementDefinition(
            Map<ToscaConceptIdentifier, AutomationCompositionElementDefinition> acElementsDefinition,
            ToscaConceptIdentifier elementId) {

        if (elementId == null) {
            if (acElementsDefinition.size() == 1) {
                return acElementsDefinition.values().iterator().next();
            }
            return null;
        }
        return acElementsDefinition.get(elementId);
    }

    private ParticipantStatus createParticipantStatus() {
        var statusMsg = new ParticipantStatus();
        statusMsg.setParticipantId(cacheProvider.getParticipantId());
        statusMsg.setReplicaId(cacheProvider.getReplicaId());
        statusMsg.setState(ParticipantState.ON_LINE);
        statusMsg.setParticipantSupportedElementType(cacheProvider.getSupportedAcElementTypes());
        return statusMsg;
    }
}
