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

package org.onap.policy.clamp.acm.runtime.supervision;

import io.micrometer.core.annotation.Timed;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionDeployPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionStateChangePublisher;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionException;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeployAck;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUtils;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionDeployAck;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantPrimeAck;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class handles supervision of automation composition instances, so only one object of this type should be built
 * at a time.
 *
 * <p/>
 * It is effectively a singleton that is started at system start.
 */
@Component
@AllArgsConstructor
public class SupervisionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SupervisionHandler.class);

    private static final String AUTOMATION_COMPOSITION_CANNOT_TRANSITION_FROM_STATE =
        "Automation composition can't transition from state ";
    private static final String AUTOMATION_COMPOSITION_IS_ALREADY_IN_STATE =
        "Automation composition is already in state ";
    private static final String TO_STATE = " to state ";
    private static final String AND_TRANSITIONING_TO_STATE = " and transitioning to state ";

    private final AutomationCompositionProvider automationCompositionProvider;
    private final AcDefinitionProvider acDefinitionProvider;

    // Publishers for participant communication
    private final AutomationCompositionDeployPublisher automationCompositionDeployPublisher;
    private final AutomationCompositionStateChangePublisher automationCompositionStateChangePublisher;

    /**
     * Handle a AutomationComposition update acknowledge message from a participant.
     *
     * @param automationCompositionAckMessage the AutomationCompositionAck message received from a participant
     */
    @MessageIntercept
    @Timed(
        value = "listener.automation_composition_deploy_ack",
        description = "AUTOMATION_COMPOSITION_DEPLOY_ACK messages received")
    public void handleAutomationCompositionUpdateAckMessage(
            AutomationCompositionDeployAck automationCompositionAckMessage) {
        LOGGER.debug("AutomationComposition Update Ack message received {}", automationCompositionAckMessage);
        setAcElementStateInDb(automationCompositionAckMessage);
    }

    /**
     * Handle a ParticipantPrimeAck message from a participant.
     *
     * @param participantPrimeAckMessage the ParticipantPrimeAck message received from a participant
     */
    @Timed(value = "listener.participant_prime_ack", description = "PARTICIPANT_PRIME_ACK messages received")
    public void handleParticipantMessage(ParticipantPrimeAck participantPrimeAckMessage) {
        LOGGER.debug("Participant Prime Ack message received {}", participantPrimeAckMessage);
        var acDefinitionOpt = acDefinitionProvider.findAcDefinition(participantPrimeAckMessage.getCompositionId());
        if (acDefinitionOpt.isEmpty()) {
            LOGGER.warn("AC Definition not found in database {}", participantPrimeAckMessage.getCompositionId());
            return;
        }
        var acDefinition = acDefinitionOpt.get();
        if (!AcTypeState.PRIMING.equals(acDefinition.getState())
                && !AcTypeState.DEPRIMING.equals(acDefinition.getState())) {
            LOGGER.warn("AC Definition {} already primed/deprimed with participant {}",
                    participantPrimeAckMessage.getCompositionId(), participantPrimeAckMessage.getParticipantId());
            return;
        }
        var state = AcTypeState.PRIMING.equals(acDefinition.getState()) ? AcTypeState.PRIMED : AcTypeState.COMMISSIONED;
        boolean completed = true;
        for (var element : acDefinition.getElementStateMap().values()) {
            if (participantPrimeAckMessage.getParticipantId().equals(element.getParticipantId())) {
                element.setState(state);
            } else if (!state.equals(element.getState())) {
                completed = false;
            }
        }
        if (completed) {
            acDefinition.setState(state);
        }
        acDefinitionProvider.updateAcDefinition(acDefinition);
    }

    /**
     * Handle a AutomationComposition statechange acknowledge message from a participant.
     *
     * @param automationCompositionAckMessage the AutomationCompositionAck message received from a participant
     */
    @MessageIntercept
    @Timed(
        value = "listener.automation_composition_statechange_ack",
        description = "AUTOMATION_COMPOSITION_STATECHANGE_ACK messages received")
    public void handleAutomationCompositionStateChangeAckMessage(
        AutomationCompositionDeployAck automationCompositionAckMessage) {
        LOGGER.debug("AutomationComposition StateChange Ack message received {}", automationCompositionAckMessage);
        setAcElementStateInDb(automationCompositionAckMessage);
    }

    private void setAcElementStateInDb(AutomationCompositionDeployAck automationCompositionAckMessage) {
        if (automationCompositionAckMessage.getAutomationCompositionResultMap() != null) {
            var automationComposition = automationCompositionProvider
                .findAutomationComposition(automationCompositionAckMessage.getAutomationCompositionId());
            if (automationComposition.isPresent()) {
                var updated = updateState(automationComposition.get(),
                    automationCompositionAckMessage.getAutomationCompositionResultMap().entrySet());
                updated |= setPrimed(automationComposition.get());
                if (updated) {
                    automationCompositionProvider.updateAutomationComposition(automationComposition.get());
                }
            } else {
                LOGGER.warn("AutomationComposition not found in database {}",
                    automationCompositionAckMessage.getAutomationCompositionId());
            }
        }
    }

    private boolean updateState(AutomationComposition automationComposition,
                                Set<Map.Entry<UUID, AcElementDeployAck>> automationCompositionResultSet) {
        var updated = false;
        for (var acElementAck : automationCompositionResultSet) {
            var element = automationComposition.getElements().get(acElementAck.getKey());
            if (element != null) {
                element.setState(acElementAck.getValue().getState());
                updated = true;
            }
        }
        return updated;
    }

    private boolean setPrimed(AutomationComposition automationComposition) {
        var acElements = automationComposition.getElements().values();
        if (acElements != null) {
            Boolean primedFlag = true;
            var checkOpt = automationComposition.getElements().values().stream()
                .filter(acElement -> (!acElement.getState().equals(AutomationCompositionState.PASSIVE)
                    || !acElement.getState().equals(AutomationCompositionState.RUNNING)))
                .findAny();
            if (checkOpt.isEmpty()) {
                primedFlag = false;
            }
            automationComposition.setPrimed(primedFlag);
            return true;
        }

        return false;
    }

    /**
     * Supervise a automation composition, performing whatever actions need to be performed on the automation
     * composition.
     *
     * @param automationComposition the automation composition to supervises
     * @throws AutomationCompositionException on supervision errors
     */
    public void triggerAutomationCompositionSupervision(AutomationComposition automationComposition)
        throws AutomationCompositionException {
        switch (automationComposition.getOrderedState()) {
            case UNINITIALISED:
                superviseAutomationCompositionUninitialization(automationComposition);
                break;

            case PASSIVE:
                superviseAutomationCompositionPassivation(automationComposition);
                break;

            case RUNNING:
                superviseAutomationCompositionActivation(automationComposition);
                break;

            default:
                exceptionOccured(Response.Status.NOT_ACCEPTABLE,
                    "A automation composition cannot be commanded to go into state "
                        + automationComposition.getOrderedState().name());
        }
    }

    /**
     * Supervise a automation composition uninitialisation, performing whatever actions need to be performed on the
     * automation composition,
     * automation composition ordered state is UNINITIALIZED.
     *
     * @param automationComposition the automation composition to supervises
     * @throws AutomationCompositionException on supervision errors
     */
    private void superviseAutomationCompositionUninitialization(AutomationComposition automationComposition)
        throws AutomationCompositionException {
        switch (automationComposition.getState()) {
            case UNINITIALISED:
                exceptionOccured(Response.Status.NOT_ACCEPTABLE,
                    AUTOMATION_COMPOSITION_IS_ALREADY_IN_STATE + automationComposition.getState().name());
                break;

            case UNINITIALISED2PASSIVE:
            case PASSIVE:
                automationComposition.setState(AutomationCompositionState.PASSIVE2UNINITIALISED);
                automationCompositionStateChangePublisher.send(automationComposition,
                    getFirstStartPhase(automationComposition));
                break;

            case PASSIVE2UNINITIALISED:
                exceptionOccured(Response.Status.NOT_ACCEPTABLE,
                    AUTOMATION_COMPOSITION_IS_ALREADY_IN_STATE + automationComposition.getState().name()
                        + AND_TRANSITIONING_TO_STATE + automationComposition.getOrderedState());
                break;

            default:
                exceptionOccured(Response.Status.NOT_ACCEPTABLE, AUTOMATION_COMPOSITION_CANNOT_TRANSITION_FROM_STATE
                    + automationComposition.getState().name() + TO_STATE + automationComposition.getOrderedState());
                break;
        }
    }

    private void superviseAutomationCompositionPassivation(AutomationComposition automationComposition)
        throws AutomationCompositionException {
        switch (automationComposition.getState()) {
            case PASSIVE:
                exceptionOccured(Response.Status.NOT_ACCEPTABLE,
                    AUTOMATION_COMPOSITION_IS_ALREADY_IN_STATE + automationComposition.getState().name());
                break;
            case UNINITIALISED:
                automationComposition.setState(AutomationCompositionState.UNINITIALISED2PASSIVE);
                automationCompositionDeployPublisher.send(automationComposition);
                break;

            case UNINITIALISED2PASSIVE:
            case RUNNING2PASSIVE:
                exceptionOccured(Response.Status.NOT_ACCEPTABLE,
                    AUTOMATION_COMPOSITION_IS_ALREADY_IN_STATE + automationComposition.getState().name()
                        + AND_TRANSITIONING_TO_STATE + automationComposition.getOrderedState());
                break;

            case RUNNING:
                automationComposition.setState(AutomationCompositionState.RUNNING2PASSIVE);
                automationCompositionStateChangePublisher.send(automationComposition,
                    getFirstStartPhase(automationComposition));
                break;

            default:
                exceptionOccured(Response.Status.NOT_ACCEPTABLE, AUTOMATION_COMPOSITION_CANNOT_TRANSITION_FROM_STATE
                    + automationComposition.getState().name() + TO_STATE + automationComposition.getOrderedState());
                break;
        }
    }

    private void superviseAutomationCompositionActivation(AutomationComposition automationComposition)
        throws AutomationCompositionException {
        switch (automationComposition.getState()) {
            case RUNNING:
                exceptionOccured(Response.Status.NOT_ACCEPTABLE,
                    AUTOMATION_COMPOSITION_IS_ALREADY_IN_STATE + automationComposition.getState().name());
                break;

            case PASSIVE2RUNNING:
                exceptionOccured(Response.Status.NOT_ACCEPTABLE,
                    AUTOMATION_COMPOSITION_IS_ALREADY_IN_STATE + automationComposition.getState().name()
                        + AND_TRANSITIONING_TO_STATE + automationComposition.getOrderedState());
                break;

            case PASSIVE:
                automationComposition.setState(AutomationCompositionState.PASSIVE2RUNNING);
                automationCompositionStateChangePublisher.send(automationComposition,
                    getFirstStartPhase(automationComposition));
                break;

            default:
                exceptionOccured(Response.Status.NOT_ACCEPTABLE, AUTOMATION_COMPOSITION_CANNOT_TRANSITION_FROM_STATE
                    + automationComposition.getState().name() + TO_STATE + automationComposition.getOrderedState());
                break;
        }
    }

    private int getFirstStartPhase(AutomationComposition automationComposition) {
        var toscaServiceTemplate =
                acDefinitionProvider.getAcDefinition(automationComposition.getCompositionId()).getServiceTemplate();
        return ParticipantUtils.getFirstStartPhase(automationComposition, toscaServiceTemplate);
    }

    private void exceptionOccured(Response.Status status, String reason) throws AutomationCompositionException {
        throw new AutomationCompositionException(status, reason);
    }
}
