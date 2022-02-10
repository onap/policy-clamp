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

package org.onap.policy.clamp.acm.runtime.supervision;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.onap.policy.clamp.acm.runtime.monitoring.MonitoringProvider;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionStateChangePublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionUpdatePublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantDeregisterAckPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantRegisterAckPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantUpdatePublisher;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionException;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementAck;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionInfo;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.concepts.ParticipantHealthStatus;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUtils;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionAck;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantDeregister;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessage;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantRegister;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantStatus;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantUpdateAck;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ServiceTemplateProvider;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
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
    private final ParticipantProvider participantProvider;
    private final MonitoringProvider monitoringProvider;
    private final ServiceTemplateProvider serviceTemplateProvider;

    // Publishers for participant communication
    private final AutomationCompositionUpdatePublisher automationCompositionUpdatePublisher;
    private final AutomationCompositionStateChangePublisher automationCompositionStateChangePublisher;
    private final ParticipantRegisterAckPublisher participantRegisterAckPublisher;
    private final ParticipantDeregisterAckPublisher participantDeregisterAckPublisher;
    private final ParticipantUpdatePublisher participantUpdatePublisher;

    /**
     * Supervision trigger called when a command is issued on automation compositions.
     *
     * <p/>
     * Causes supervision to start or continue supervision on the automation compositions in question.
     *
     * @param automationCompositionIdentifierList the automation compositions for which the supervision command has been
     *        issued
     * @throws AutomationCompositionException on supervision triggering exceptions
     */
    public void triggerAutomationCompositionSupervision(
        List<ToscaConceptIdentifier> automationCompositionIdentifierList) throws AutomationCompositionException {

        LOGGER.debug("triggering automation composition supervision on automation compositions {}",
            automationCompositionIdentifierList);

        if (CollectionUtils.isEmpty(automationCompositionIdentifierList)) {
            // This is just to force throwing of the exception in certain circumstances.
            exceptionOccured(Response.Status.NOT_ACCEPTABLE,
                "The list of automation compositions for supervision is empty");
        }

        for (ToscaConceptIdentifier automationCompositionId : automationCompositionIdentifierList) {
            try {
                var automationComposition =
                    automationCompositionProvider.getAutomationComposition(automationCompositionId);

                superviseAutomationComposition(automationComposition);

                automationCompositionProvider.saveAutomationComposition(automationComposition);
            } catch (PfModelException pfme) {
                throw new AutomationCompositionException(pfme.getErrorResponse().getResponseCode(), pfme.getMessage(),
                    pfme);
            }
        }
    }

    /**
     * Handle a ParticipantStatus message from a participant.
     *
     * @param participantStatusMessage the ParticipantStatus message received from a participant
     */
    @MessageIntercept
    public void handleParticipantMessage(ParticipantStatus participantStatusMessage) {
        LOGGER.debug("Participant Status received {}", participantStatusMessage);
        try {
            superviseParticipant(participantStatusMessage);
        } catch (PfModelException | AutomationCompositionException svExc) {
            LOGGER.warn("error supervising participant {}", participantStatusMessage.getParticipantId(), svExc);
            return;
        }

        try {
            superviseAutomationCompositions(participantStatusMessage);
        } catch (PfModelException | AutomationCompositionException svExc) {
            LOGGER.warn("error supervising participant {}", participantStatusMessage.getParticipantId(), svExc);
        }
    }

    /**
     * Handle a ParticipantRegister message from a participant.
     *
     * @param participantRegisterMessage the ParticipantRegister message received from a participant
     */
    @MessageIntercept
    public boolean handleParticipantMessage(ParticipantRegister participantRegisterMessage) {
        LOGGER.debug("Participant Register received {}", participantRegisterMessage);
        try {
            checkParticipant(participantRegisterMessage, ParticipantState.UNKNOWN, ParticipantHealthStatus.UNKNOWN);
        } catch (PfModelException | AutomationCompositionException svExc) {
            LOGGER.warn("error saving participant {}", participantRegisterMessage.getParticipantId(), svExc);
        }

        var isCommissioning = participantUpdatePublisher.sendCommissioning(null, null,
            participantRegisterMessage.getParticipantId(), participantRegisterMessage.getParticipantType());

        participantRegisterAckPublisher.send(participantRegisterMessage.getMessageId(),
            participantRegisterMessage.getParticipantId(), participantRegisterMessage.getParticipantType());
        return isCommissioning;
    }

    /**
     * Handle a ParticipantDeregister message from a participant.
     *
     * @param participantDeregisterMessage the ParticipantDeregister message received from a participant
     */
    @MessageIntercept
    public void handleParticipantMessage(ParticipantDeregister participantDeregisterMessage) {
        LOGGER.debug("Participant Deregister received {}", participantDeregisterMessage);
        try {
            var participantOpt =
                participantProvider.findParticipant(participantDeregisterMessage.getParticipantId().getName(),
                    participantDeregisterMessage.getParticipantId().getVersion());

            if (participantOpt.isPresent()) {
                var participant = participantOpt.get();
                participant.setParticipantState(ParticipantState.TERMINATED);
                participant.setHealthStatus(ParticipantHealthStatus.OFF_LINE);
                participantProvider.saveParticipant(participant);
            }
        } catch (PfModelException pfme) {
            LOGGER.warn("Model exception occured with participant id {}",
                participantDeregisterMessage.getParticipantId());
        }

        participantDeregisterAckPublisher.send(participantDeregisterMessage.getMessageId());
    }

    /**
     * Handle a ParticipantUpdateAck message from a participant.
     *
     * @param participantUpdateAckMessage the ParticipantUpdateAck message received from a participant
     */
    @MessageIntercept
    public void handleParticipantMessage(ParticipantUpdateAck participantUpdateAckMessage) {
        LOGGER.debug("Participant Update Ack received {}", participantUpdateAckMessage);
        try {
            var participantOpt =
                participantProvider.findParticipant(participantUpdateAckMessage.getParticipantId().getName(),
                    participantUpdateAckMessage.getParticipantId().getVersion());

            if (participantOpt.isPresent()) {
                var participant = participantOpt.get();
                participant.setParticipantState(participantUpdateAckMessage.getState());
                participantProvider.saveParticipant(participant);
            } else {
                LOGGER.warn("Participant not found in database {}", participantUpdateAckMessage.getParticipantId());
            }
        } catch (PfModelException pfme) {
            LOGGER.warn("Model exception occured with participant id {}",
                participantUpdateAckMessage.getParticipantId());
        }
    }

    /**
     * Send commissioning update message to dmaap.
     *
     * @param name the ToscaServiceTemplate name
     * @param version the ToscaServiceTemplate version
     */
    public void handleSendCommissionMessage(String name, String version) {
        LOGGER.debug("Participant update message with serviveTemplate {} {} being sent to all participants", name,
            version);
        participantUpdatePublisher.sendComissioningBroadcast(name, version);
    }

    /**
     * Send decommissioning update message to dmaap.
     *
     */
    public void handleSendDeCommissionMessage() {
        LOGGER.debug("Participant update message being sent");
        participantUpdatePublisher.sendDecomisioning();
    }

    /**
     * Handle a AutomationComposition update acknowledge message from a participant.
     *
     * @param automationCompositionAckMessage the AutomationCompositionAck message received from a participant
     */
    @MessageIntercept
    public void handleAutomationCompositionUpdateAckMessage(AutomationCompositionAck automationCompositionAckMessage) {
        LOGGER.debug("AutomationComposition Update Ack message received {}", automationCompositionAckMessage);
        setAcElementStateInDb(automationCompositionAckMessage);
    }

    /**
     * Handle a AutomationComposition statechange acknowledge message from a participant.
     *
     * @param automationCompositionAckMessage the AutomationCompositionAck message received from a participant
     */
    @MessageIntercept
    public void handleAutomationCompositionStateChangeAckMessage(
        AutomationCompositionAck automationCompositionAckMessage) {
        LOGGER.debug("AutomationComposition StateChange Ack message received {}", automationCompositionAckMessage);
        setAcElementStateInDb(automationCompositionAckMessage);
    }

    private void setAcElementStateInDb(AutomationCompositionAck automationCompositionAckMessage) {
        if (automationCompositionAckMessage.getAutomationCompositionResultMap() != null) {
            try {
                var automationComposition = automationCompositionProvider
                    .getAutomationComposition(automationCompositionAckMessage.getAutomationCompositionId());
                if (automationComposition != null) {
                    var updated = updateState(automationComposition,
                        automationCompositionAckMessage.getAutomationCompositionResultMap().entrySet());
                    updated |= setPrimed(automationComposition);
                    if (updated) {
                        automationCompositionProvider.saveAutomationComposition(automationComposition);
                    }
                } else {
                    LOGGER.warn("AutomationComposition not found in database {}",
                        automationCompositionAckMessage.getAutomationCompositionId());
                }
            } catch (PfModelException pfme) {
                LOGGER.warn("Model exception occured with AutomationComposition Id {}",
                    automationCompositionAckMessage.getAutomationCompositionId());
            }
        }
    }

    private boolean updateState(AutomationComposition automationComposition,
        Set<Map.Entry<UUID, AutomationCompositionElementAck>> automationCompositionResultSet) {
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
    private void superviseAutomationComposition(AutomationComposition automationComposition)
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
                automationCompositionUpdatePublisher.send(automationComposition);
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
        ToscaServiceTemplate toscaServiceTemplate = null;
        try {
            toscaServiceTemplate = serviceTemplateProvider.getAllServiceTemplates().get(0);
        } catch (PfModelException e) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST, "Canont load ToscaServiceTemplate from DB", e);
        }
        return ParticipantUtils.getFirstStartPhase(automationComposition, toscaServiceTemplate);
    }

    private void checkParticipant(ParticipantMessage participantMessage, ParticipantState participantState,
        ParticipantHealthStatus healthStatus) throws AutomationCompositionException, PfModelException {
        if (participantMessage.getParticipantId() == null) {
            exceptionOccured(Response.Status.NOT_FOUND, "Participant ID on PARTICIPANT_STATUS message is null");
        }
        var participantOpt = participantProvider.findParticipant(participantMessage.getParticipantId().getName(),
            participantMessage.getParticipantId().getVersion());

        if (participantOpt.isEmpty()) {
            var participant = new Participant();
            participant.setName(participantMessage.getParticipantId().getName());
            participant.setVersion(participantMessage.getParticipantId().getVersion());
            participant.setDefinition(participantMessage.getParticipantId());
            participant.setParticipantType(participantMessage.getParticipantType());
            participant.setParticipantState(participantState);
            participant.setHealthStatus(healthStatus);

            participantProvider.saveParticipant(participant);
        } else {
            var participant = participantOpt.get();
            participant.setParticipantState(participantState);
            participant.setHealthStatus(healthStatus);

            participantProvider.saveParticipant(participant);
        }
    }

    private void superviseParticipant(ParticipantStatus participantStatusMessage)
        throws PfModelException, AutomationCompositionException {

        checkParticipant(participantStatusMessage, participantStatusMessage.getState(),
            participantStatusMessage.getHealthStatus());

        monitoringProvider.createParticipantStatistics(List.of(participantStatusMessage.getParticipantStatistics()));
    }

    private void superviseAutomationCompositions(ParticipantStatus participantStatusMessage)
        throws PfModelException, AutomationCompositionException {
        if (participantStatusMessage.getAutomationCompositionInfoList() != null) {
            for (AutomationCompositionInfo acEntry : participantStatusMessage.getAutomationCompositionInfoList()) {
                var dbAutomationComposition = automationCompositionProvider
                    .getAutomationComposition(new ToscaConceptIdentifier(acEntry.getAutomationCompositionId()));
                if (dbAutomationComposition == null) {
                    exceptionOccured(Response.Status.NOT_FOUND,
                        "PARTICIPANT_STATUS automation composition not found in database: "
                            + acEntry.getAutomationCompositionId());
                }
                dbAutomationComposition.setState(acEntry.getState());
                monitoringProvider.createAcElementStatistics(
                    acEntry.getAutomationCompositionStatistics().getAcElementStatisticsList().getAcElementStatistics());
            }
        }
    }

    private void exceptionOccured(Response.Status status, String reason) throws AutomationCompositionException {
        throw new AutomationCompositionException(status, reason);
    }
}
