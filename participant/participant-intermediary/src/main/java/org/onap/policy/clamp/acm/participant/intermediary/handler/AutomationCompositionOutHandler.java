/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
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
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementInfo;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionInfo;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionDeployAck;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantPrimeAck;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantStatus;
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
     * Handle a automation composition element state change message.
     *
     * @param automationCompositionId the automationComposition Id
     * @param elementId the automationComposition Element Id
     * @param deployState the DeployState state
     * @param lockState the LockState state
     * @param message the message
     * @param stateChangeResult the indicator if error occurs
     */
    public void updateAutomationCompositionElementState(UUID automationCompositionId, UUID elementId,
            DeployState deployState, LockState lockState, StateChangeResult stateChangeResult, String message) {

        if (automationCompositionId == null || elementId == null) {
            LOGGER.error("Cannot update Automation composition element state, id is null");
            return;
        }

        if ((deployState != null && lockState != null) || (deployState == null && lockState == null)) {
            LOGGER.error("state error {} and {} cannot be handled", deployState, lockState);
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

        if (deployState != null) {
            handleDeployState(automationComposition, element, deployState);
        }
        if (lockState != null) {
            handleLockState(automationComposition, element, lockState);
        }

        var automationCompositionStateChangeAck =
                new AutomationCompositionDeployAck(ParticipantMessageType.AUTOMATION_COMPOSITION_STATECHANGE_ACK);
        automationCompositionStateChangeAck.setParticipantId(cacheProvider.getParticipantId());
        automationCompositionStateChangeAck.setMessage(message);
        automationCompositionStateChangeAck.setStateChangeResult(stateChangeResult);
        automationCompositionStateChangeAck.setAutomationCompositionId(automationCompositionId);
        automationCompositionStateChangeAck.getAutomationCompositionResultMap().put(element.getId(),
                new AcElementDeployAck(element.getDeployState(), element.getLockState(), element.getOperationalState(),
                        element.getUseState(), element.getOutProperties(), true, message));
        LOGGER.debug("Automation composition element {} state changed to {}", elementId, deployState);
        automationCompositionStateChangeAck.setResult(true);
        publisher.sendAutomationCompositionAck(automationCompositionStateChangeAck);
    }

    private void handleDeployState(AutomationComposition automationComposition, AutomationCompositionElement element,
            DeployState deployState) {
        element.setDeployState(deployState);
        element.setLockState(DeployState.DEPLOYED.equals(element.getDeployState()) ? LockState.LOCKED : LockState.NONE);
        var checkOpt = automationComposition.getElements().values().stream()
                .filter(acElement -> !deployState.equals(acElement.getDeployState())).findAny();
        if (checkOpt.isEmpty()) {
            automationComposition.setDeployState(deployState);
            automationComposition.setLockState(element.getLockState());

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
        statusMsg.setParticipantId(cacheProvider.getParticipantId());
        statusMsg.setState(ParticipantState.ON_LINE);
        statusMsg.setParticipantSupportedElementType(cacheProvider.getSupportedAcElementTypes());
        var acInfo = new AutomationCompositionInfo();
        acInfo.setAutomationCompositionId(automationCompositionId);
        acInfo.setDeployState(automationComposition.getDeployState());
        acInfo.setLockState(automationComposition.getLockState());
        acInfo.setElements(List.of(getAutomationCompositionElementInfo(element)));
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
        var participantPrimeAck = new ParticipantPrimeAck();
        participantPrimeAck.setCompositionId(compositionId);
        participantPrimeAck.setMessage(message);
        participantPrimeAck.setResult(true);
        participantPrimeAck.setCompositionState(state);
        participantPrimeAck.setStateChangeResult(stateChangeResult);
        participantPrimeAck.setParticipantId(cacheProvider.getParticipantId());
        participantPrimeAck.setState(ParticipantState.ON_LINE);
        publisher.sendParticipantPrimeAck(participantPrimeAck);
    }
}
