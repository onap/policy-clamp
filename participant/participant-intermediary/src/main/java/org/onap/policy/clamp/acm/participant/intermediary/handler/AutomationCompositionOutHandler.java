/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2026 OpenInfra Foundation Europe. All rights reserved.
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
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.participant.intermediary.api.ElementStageDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.ElementStateDto;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.acm.participant.intermediary.handler.cache.AcDefinition;
import org.onap.policy.clamp.acm.participant.intermediary.handler.cache.CacheProvider;
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
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.clamp.models.acm.dto.PrimeElementAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeployAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrimeAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatus;
import org.onap.policy.clamp.models.acm.utils.AcmStateUtils;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AutomationCompositionOutHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutomationCompositionOutHandler.class);
    private static final String MSG_NOT_PRESENT
            = "Cannot update Automation composition element {}, {} id {} not present";
    private static final String MSG_STATE_CHANGE = "Automation composition element {} state changed to {}";
    private static final String MSG_AC = "Automation composition";
    private static final String MSG_AC_ELEMENT = "AC Element";
    private static final String MSG_STAGE = "stage";

    private final ParticipantMessagePublisher publisher;
    private final CacheProvider cacheProvider;

    /**
     * Handle a automation composition element stage change message.
     *
     * @param elementStageDto all data related to the change stage
     * @param applyOutProperties if true apply OutPoperties
     */
    public void updateAutomationCompositionElementStage(
            @NonNull ElementStageDto elementStageDto, boolean applyOutProperties) {
        if (!validateData(elementStageDto.instance(), elementStageDto.elementId(), StateChangeResult.NO_ERROR)) {
            return;
        }

        var automationComposition = cacheProvider.getAutomationComposition(elementStageDto.instance());
        if (automationComposition == null) {
            LOGGER.error(MSG_NOT_PRESENT, MSG_STAGE, MSG_AC, elementStageDto.instance());
            return;
        }

        var element = automationComposition.getElements().get(elementStageDto.elementId());
        if (element == null) { // NOSONAR
            LOGGER.error(MSG_NOT_PRESENT, MSG_STAGE, MSG_AC_ELEMENT, elementStageDto.elementId());
            return;
        }
        if (applyOutProperties) {
            element.setUseState(elementStageDto.useState());
            element.setOperationalState(elementStageDto.operationalState());
            element.setOutProperties(elementStageDto.outProperties());
        }

        var acStateChangeAck = createAutomationCompositionDeployAck(elementStageDto.instance(),
                elementStageDto.elementId(), StateChangeResult.NO_ERROR, elementStageDto.message());
        acStateChangeAck.setStage(elementStageDto.nextStage());
        acStateChangeAck.setOutPropertiesUpdated(applyOutProperties);
        acStateChangeAck.getAutomationCompositionResultMap().put(elementStageDto.elementId(),
            new AcElementDeployAck(element.getDeployState(), element.getLockState(), element.getOperationalState(),
                element.getUseState(), element.getOutProperties(), true, elementStageDto.message()));
        LOGGER.debug("Automation composition element {} stage changed to {}", elementStageDto.elementId(),
                elementStageDto.nextStage());
        publisher.sendAutomationCompositionAck(acStateChangeAck);
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

    private boolean validateData(UUID instance, UUID elementId, StateChangeResult stateChangeResult,
            DeployState deployState, LockState lockState) {
        if (!validateData(instance, elementId, stateChangeResult)) {
            return false;
        }

        if ((deployState != null && lockState != null) || (deployState == null && lockState == null)
                || AcmStateUtils.isInTransitionalState(deployState, lockState, SubState.NONE)) {
            LOGGER.error("state error {} and {} cannot be handled", deployState, lockState);
            return false;
        }
        return true;
    }

    /**
     * Handle a automation composition element state change message.
     *
     * @param elementStateDto all data related to the change state
     * @param applyOutProperties apply OutPoperties
     */
    public void updateAutomationCompositionElementState(@NonNull ElementStateDto elementStateDto,
            boolean applyOutProperties) {
        if (!validateData(elementStateDto.instance(), elementStateDto.elementId(),
                elementStateDto.stateChangeResult(), elementStateDto.deployState(), elementStateDto.lockState())) {
            return;
        }

        var automationComposition = cacheProvider.getAutomationComposition(elementStateDto.instance());
        if (automationComposition == null) {
            LOGGER.error(MSG_NOT_PRESENT, "state", MSG_AC, elementStateDto.instance());
            return;
        }

        var element = automationComposition.getElements().get(elementStateDto.elementId());
        if (element == null) { // NOSONAR
            checkElement(automationComposition, elementStateDto, applyOutProperties);
            return;
        }

        if (!SubState.NONE.equals(element.getSubState())) {
            if (!StateChangeResult.FAILED.equals(elementStateDto.stateChangeResult())) {
                element.setSubState(SubState.NONE);
            }
        } else if (elementStateDto.deployState() != null) {
            element.setDeployState(elementStateDto.deployState());
            element.setLockState(
                    DeployState.DEPLOYED.equals(element.getDeployState()) ? LockState.LOCKED : LockState.NONE);
        }
        if (elementStateDto.lockState() != null) {
            element.setLockState(elementStateDto.lockState());
        }
        if (applyOutProperties) {
            element.setUseState(elementStateDto.useState());
            element.setOperationalState(elementStateDto.operationalState());
            element.setOutProperties(elementStateDto.outProperties());
        }
        var message = elementStateDto.message();
        var acStateChangeAck = createAutomationCompositionDeployAck(elementStateDto.instance(),
                elementStateDto.elementId(), elementStateDto.stateChangeResult(), message);
        acStateChangeAck.setOutPropertiesUpdated(applyOutProperties);
        acStateChangeAck.getAutomationCompositionResultMap().put(elementStateDto.elementId(),
                new AcElementDeployAck(element.getDeployState(), element.getLockState(), element.getOperationalState(),
                        element.getUseState(), element.getOutProperties(), true, message));
        LOGGER.debug(MSG_STATE_CHANGE, elementStateDto.elementId(), elementStateDto.deployState());
        publisher.sendAutomationCompositionAck(acStateChangeAck);
        cacheProvider.getMsgIdentification().remove(element.getId());
    }

    private void checkElement(AutomationComposition automationComposition, ElementStateDto elementStateDto,
            boolean sendOutput) {
        if ((DeployState.MIGRATING.equals(automationComposition.getDeployState())
                || DeployState.MIGRATION_REVERTING.equals(automationComposition.getDeployState()))) {
            var acStateChangeAck = createAutomationCompositionDeployAck(elementStateDto.instance(),
                    elementStateDto.elementId(), elementStateDto.stateChangeResult(), elementStateDto.message());
            acStateChangeAck.setOutPropertiesUpdated(sendOutput);
            acStateChangeAck.getAutomationCompositionResultMap().put(elementStateDto.elementId(),
                    new AcElementDeployAck(elementStateDto.deployState(), LockState.NONE,
                            elementStateDto.operationalState(), elementStateDto.useState(),
                            elementStateDto.outProperties(), true, elementStateDto.message()));
            LOGGER.debug(MSG_STATE_CHANGE, elementStateDto.elementId(), elementStateDto.deployState());
            publisher.sendAutomationCompositionAck(acStateChangeAck);
            cacheProvider.getMsgIdentification().remove(elementStateDto.elementId());
        } else {
            LOGGER.error(MSG_NOT_PRESENT, "state", MSG_AC_ELEMENT, elementStateDto.elementId());
        }
    }

    private AutomationCompositionDeployAck createAutomationCompositionDeployAck(UUID instance, UUID elementId,
            StateChangeResult stateChangeResult, String message) {
        var acStateChangeAck =
                new AutomationCompositionDeployAck(ParticipantMessageType.AUTOMATION_COMPOSITION_STATECHANGE_ACK);
        acStateChangeAck.setParticipantId(cacheProvider.getParticipantId());
        acStateChangeAck.setReplicaId(cacheProvider.getReplicaId());
        acStateChangeAck.setAutomationCompositionId(instance);
        acStateChangeAck.setStateChangeResult(stateChangeResult);
        acStateChangeAck.setMessage(AcmUtils.validatedMessage(message));
        acStateChangeAck.setResponseTo(cacheProvider.getMsgIdentification().get(elementId));
        return acStateChangeAck;
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
            // automationCompositionId is a UUID, safe to log
            LOGGER.error(MSG_NOT_PRESENT, "outProperties", MSG_AC, automationCompositionId); // NOSONAR
            return;
        }

        var element = automationComposition.getElements().get(elementId);
        if (element == null) { // NOSONAR
            // elementId is a UUID, safe to log
            LOGGER.error(MSG_NOT_PRESENT, "outProperties", MSG_AC_ELEMENT, elementId); // NOSONAR
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
     * @param outPropertiesMap the outProperties for each element
     */
    public void updateCompositionState(UUID compositionId, AcTypeState state, StateChangeResult stateChangeResult,
            String message, Map<ToscaConceptIdentifier, Map<String, Object>> outPropertiesMap) {
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
        participantPrimeAck.setMessage(AcmUtils.validatedMessage(message));
        participantPrimeAck.setResponseTo(cacheProvider.getMsgIdentification().get(compositionId));
        participantPrimeAck.setCompositionState(state);
        participantPrimeAck.setStateChangeResult(stateChangeResult);
        participantPrimeAck.setParticipantId(cacheProvider.getParticipantId());
        participantPrimeAck.setReplicaId(cacheProvider.getReplicaId());
        if (outPropertiesMap != null) {
            participantPrimeAck.setOutPropertiesList(outPropertiesMap.entrySet().stream()
                    .map(element -> new PrimeElementAck(element.getKey(), element.getValue()))
                    .toList());
        }
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
        var acDefinition = acElementDefsMap.get(compositionId);
        if (acDefinition == null) {
            LOGGER.error("Cannot send Composition outProperties, id {} is null", compositionId);
            return;
        }
        var acElementDefinition = getAutomationCompositionElementDefinition(acDefinition, elementId);
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
            AcDefinition acElementsDefinition,
            ToscaConceptIdentifier elementId) {

        if (elementId == null) {
            if (acElementsDefinition.getElements().size() == 1) {
                return acElementsDefinition.getElements().values().iterator().next();
            }
            return null;
        }
        return acElementsDefinition.getElements().get(elementId);
    }

    private ParticipantStatus createParticipantStatus() {
        var statusMsg = new ParticipantStatus();
        statusMsg.setParticipantId(cacheProvider.getParticipantId());
        statusMsg.setReplicaId(cacheProvider.getReplicaId());
        statusMsg.setParticipantSupportedElementType(cacheProvider.getSupportedAcElementTypes());
        return statusMsg;
    }
}
