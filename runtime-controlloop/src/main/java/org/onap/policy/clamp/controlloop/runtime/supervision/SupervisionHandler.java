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

package org.onap.policy.clamp.controlloop.runtime.supervision;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopException;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElementAck;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopInfo;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.Participant;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantHealthStatus;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantUtils;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ControlLoopProvider;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ServiceTemplateProvider;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ControlLoopAck;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantDeregister;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantMessage;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantRegister;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantStatus;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantUpdateAck;
import org.onap.policy.clamp.controlloop.runtime.monitoring.MonitoringProvider;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ControlLoopStateChangePublisher;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ControlLoopUpdatePublisher;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ParticipantDeregisterAckPublisher;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ParticipantRegisterAckPublisher;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ParticipantUpdatePublisher;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class handles supervision of control loop instances, so only one object of this type should be built at a time.
 *
 * <p/>
 * It is effectively a singleton that is started at system start.
 */
@Component
@AllArgsConstructor
public class SupervisionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SupervisionHandler.class);

    private static final String CONTROL_LOOP_CANNOT_TRANSITION_FROM_STATE = "Control loop can't transition from state ";
    private static final String CONTROL_LOOP_IS_ALREADY_IN_STATE = "Control loop is already in state ";
    private static final String TO_STATE = " to state ";
    private static final String AND_TRANSITIONING_TO_STATE = " and transitioning to state ";

    private final ControlLoopProvider controlLoopProvider;
    private final ParticipantProvider participantProvider;
    private final MonitoringProvider monitoringProvider;
    private final ServiceTemplateProvider serviceTemplateProvider;

    // Publishers for participant communication
    private final ControlLoopUpdatePublisher controlLoopUpdatePublisher;
    private final ControlLoopStateChangePublisher controlLoopStateChangePublisher;
    private final ParticipantRegisterAckPublisher participantRegisterAckPublisher;
    private final ParticipantDeregisterAckPublisher participantDeregisterAckPublisher;
    private final ParticipantUpdatePublisher participantUpdatePublisher;

    /**
     * Supervision trigger called when a command is issued on control loops.
     *
     * <p/>
     * Causes supervision to start or continue supervision on the control loops in question.
     *
     * @param controlLoopIdentifierList the control loops for which the supervision command has been issued
     * @throws ControlLoopException on supervision triggering exceptions
     */
    public void triggerControlLoopSupervision(List<ToscaConceptIdentifier> controlLoopIdentifierList)
            throws ControlLoopException {

        LOGGER.debug("triggering control loop supervision on control loops {}", controlLoopIdentifierList);

        if (CollectionUtils.isEmpty(controlLoopIdentifierList)) {
            // This is just to force throwing of the exception in certain circumstances.
            exceptionOccured(Response.Status.NOT_ACCEPTABLE, "The list of control loops for supervision is empty");
        }

        for (ToscaConceptIdentifier controlLoopId : controlLoopIdentifierList) {
            try {
                var controlLoop = controlLoopProvider.getControlLoop(controlLoopId);

                superviseControlLoop(controlLoop);

                controlLoopProvider.updateControlLoop(controlLoop);
            } catch (PfModelException pfme) {
                throw new ControlLoopException(pfme.getErrorResponse().getResponseCode(), pfme.getMessage(), pfme);
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
        } catch (PfModelException | ControlLoopException svExc) {
            LOGGER.warn("error supervising participant {}", participantStatusMessage.getParticipantId(), svExc);
            return;
        }

        try {
            superviseControlLoops(participantStatusMessage);
        } catch (PfModelException | ControlLoopException svExc) {
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
        } catch (PfModelException | ControlLoopException svExc) {
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
            var participantList =
                    participantProvider.getParticipants(participantDeregisterMessage.getParticipantId().getName(),
                            participantDeregisterMessage.getParticipantId().getVersion());

            if (participantList != null) {
                for (Participant participant : participantList) {
                    participant.setParticipantState(ParticipantState.TERMINATED);
                    participant.setHealthStatus(ParticipantHealthStatus.OFF_LINE);
                }
                participantProvider.updateParticipants(participantList);
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
            var participantList =
                    participantProvider.getParticipants(participantUpdateAckMessage.getParticipantId().getName(),
                            participantUpdateAckMessage.getParticipantId().getVersion());

            if (participantList != null) {
                for (Participant participant : participantList) {
                    participant.setParticipantState(participantUpdateAckMessage.getState());
                }
                participantProvider.updateParticipants(participantList);
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
     * Handle a ControlLoop update acknowledge message from a participant.
     *
     * @param controlLoopAckMessage the ControlLoopAck message received from a participant
     */
    @MessageIntercept
    public void handleControlLoopUpdateAckMessage(ControlLoopAck controlLoopAckMessage) {
        LOGGER.debug("ControlLoop Update Ack message received {}", controlLoopAckMessage);
        setClElementStateInDb(controlLoopAckMessage);
    }

    /**
     * Handle a ControlLoop statechange acknowledge message from a participant.
     *
     * @param controlLoopAckMessage the ControlLoopAck message received from a participant
     */
    @MessageIntercept
    public void handleControlLoopStateChangeAckMessage(ControlLoopAck controlLoopAckMessage) {
        LOGGER.debug("ControlLoop StateChange Ack message received {}", controlLoopAckMessage);
        setClElementStateInDb(controlLoopAckMessage);
    }

    private void setClElementStateInDb(ControlLoopAck controlLoopAckMessage) {
        if (controlLoopAckMessage.getControlLoopResultMap() != null) {
            try {
                var controlLoop = controlLoopProvider.getControlLoop(controlLoopAckMessage.getControlLoopId());
                if (controlLoop != null) {
                    var updated = updateState(controlLoop, controlLoopAckMessage.getControlLoopResultMap().entrySet());
                    updated |= setPrimed(controlLoop);
                    if (updated) {
                        controlLoopProvider.updateControlLoop(controlLoop);
                    }
                } else {
                    LOGGER.warn("ControlLoop not found in database {}", controlLoopAckMessage.getControlLoopId());
                }
            } catch (PfModelException pfme) {
                LOGGER.warn("Model exception occured with ControlLoop Id {}", controlLoopAckMessage.getControlLoopId());
            }
        }
    }

    private boolean updateState(ControlLoop controlLoop,
            Set<Map.Entry<UUID, ControlLoopElementAck>> controlLoopResultSet) {
        var updated = false;
        for (var clElementAck : controlLoopResultSet) {
            var element = controlLoop.getElements().get(clElementAck.getKey());
            if (element != null) {
                element.setState(clElementAck.getValue().getState());
                updated = true;
            }
        }
        return updated;
    }

    private boolean setPrimed(ControlLoop controlLoop) {
        var clElements = controlLoop.getElements().values();
        if (clElements != null) {
            Boolean primedFlag = true;
            var checkOpt = controlLoop.getElements().values().stream()
                    .filter(clElement -> (!clElement.getState().equals(ControlLoopState.PASSIVE)
                            || !clElement.getState().equals(ControlLoopState.RUNNING)))
                    .findAny();
            if (checkOpt.isEmpty()) {
                primedFlag = false;
            }
            controlLoop.setPrimed(primedFlag);
            return true;
        }

        return false;
    }

    /**
     * Supervise a control loop, performing whatever actions need to be performed on the control loop.
     *
     * @param controlLoop the control loop to supervises
     * @throws ControlLoopException on supervision errors
     */
    private void superviseControlLoop(ControlLoop controlLoop) throws ControlLoopException {
        switch (controlLoop.getOrderedState()) {
            case UNINITIALISED:
                superviseControlLoopUninitialization(controlLoop);
                break;

            case PASSIVE:
                superviseControlLoopPassivation(controlLoop);
                break;

            case RUNNING:
                superviseControlLoopActivation(controlLoop);
                break;

            default:
                exceptionOccured(Response.Status.NOT_ACCEPTABLE,
                        "A control loop cannot be commanded to go into state " + controlLoop.getOrderedState().name());
        }
    }

    /**
     * Supervise a control loop uninitialisation, performing whatever actions need to be performed on the control loop,
     * control loop ordered state is UNINITIALIZED.
     *
     * @param controlLoop the control loop to supervises
     * @throws ControlLoopException on supervision errors
     */
    private void superviseControlLoopUninitialization(ControlLoop controlLoop) throws ControlLoopException {
        switch (controlLoop.getState()) {
            case UNINITIALISED:
                exceptionOccured(Response.Status.NOT_ACCEPTABLE,
                        CONTROL_LOOP_IS_ALREADY_IN_STATE + controlLoop.getState().name());
                break;

            case UNINITIALISED2PASSIVE:
            case PASSIVE:
                controlLoop.setState(ControlLoopState.PASSIVE2UNINITIALISED);
                controlLoopStateChangePublisher.send(controlLoop, getFirstStartPhase(controlLoop));
                break;

            case PASSIVE2UNINITIALISED:
                exceptionOccured(Response.Status.NOT_ACCEPTABLE, CONTROL_LOOP_IS_ALREADY_IN_STATE
                        + controlLoop.getState().name() + AND_TRANSITIONING_TO_STATE + controlLoop.getOrderedState());
                break;

            default:
                exceptionOccured(Response.Status.NOT_ACCEPTABLE, CONTROL_LOOP_CANNOT_TRANSITION_FROM_STATE
                        + controlLoop.getState().name() + TO_STATE + controlLoop.getOrderedState());
                break;
        }
    }

    private void superviseControlLoopPassivation(ControlLoop controlLoop) throws ControlLoopException {
        switch (controlLoop.getState()) {
            case PASSIVE:
                exceptionOccured(Response.Status.NOT_ACCEPTABLE,
                        CONTROL_LOOP_IS_ALREADY_IN_STATE + controlLoop.getState().name());
                break;
            case UNINITIALISED:
                controlLoop.setState(ControlLoopState.UNINITIALISED2PASSIVE);
                controlLoopUpdatePublisher.send(controlLoop);
                break;

            case UNINITIALISED2PASSIVE:
            case RUNNING2PASSIVE:
                exceptionOccured(Response.Status.NOT_ACCEPTABLE, CONTROL_LOOP_IS_ALREADY_IN_STATE
                        + controlLoop.getState().name() + AND_TRANSITIONING_TO_STATE + controlLoop.getOrderedState());
                break;

            case RUNNING:
                controlLoop.setState(ControlLoopState.RUNNING2PASSIVE);
                controlLoopStateChangePublisher.send(controlLoop, getFirstStartPhase(controlLoop));
                break;

            default:
                exceptionOccured(Response.Status.NOT_ACCEPTABLE, CONTROL_LOOP_CANNOT_TRANSITION_FROM_STATE
                        + controlLoop.getState().name() + TO_STATE + controlLoop.getOrderedState());
                break;
        }
    }

    private void superviseControlLoopActivation(ControlLoop controlLoop) throws ControlLoopException {
        switch (controlLoop.getState()) {
            case RUNNING:
                exceptionOccured(Response.Status.NOT_ACCEPTABLE,
                        CONTROL_LOOP_IS_ALREADY_IN_STATE + controlLoop.getState().name());
                break;

            case PASSIVE2RUNNING:
                exceptionOccured(Response.Status.NOT_ACCEPTABLE, CONTROL_LOOP_IS_ALREADY_IN_STATE
                        + controlLoop.getState().name() + AND_TRANSITIONING_TO_STATE + controlLoop.getOrderedState());
                break;

            case PASSIVE:
                controlLoop.setState(ControlLoopState.PASSIVE2RUNNING);
                controlLoopStateChangePublisher.send(controlLoop, getFirstStartPhase(controlLoop));
                break;

            default:
                exceptionOccured(Response.Status.NOT_ACCEPTABLE, CONTROL_LOOP_CANNOT_TRANSITION_FROM_STATE
                        + controlLoop.getState().name() + TO_STATE + controlLoop.getOrderedState());
                break;
        }
    }

    private int getFirstStartPhase(ControlLoop controlLoop) {
        ToscaServiceTemplate toscaServiceTemplate = null;
        try {
            toscaServiceTemplate = serviceTemplateProvider.getServiceTemplateList(null, null).get(0);
        } catch (PfModelException e) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST, "Canont load ToscaServiceTemplate from DB", e);
        }
        return ParticipantUtils.getFirstStartPhase(controlLoop, toscaServiceTemplate);
    }

    private void checkParticipant(ParticipantMessage participantMessage, ParticipantState participantState,
            ParticipantHealthStatus healthStatus) throws ControlLoopException, PfModelException {
        if (participantMessage.getParticipantId() == null) {
            exceptionOccured(Response.Status.NOT_FOUND, "Participant ID on PARTICIPANT_STATUS message is null");
        }
        List<Participant> participantList = participantProvider.getParticipants(
                participantMessage.getParticipantId().getName(), participantMessage.getParticipantId().getVersion());

        if (CollectionUtils.isEmpty(participantList)) {
            var participant = new Participant();
            participant.setName(participantMessage.getParticipantId().getName());
            participant.setVersion(participantMessage.getParticipantId().getVersion());
            participant.setDefinition(participantMessage.getParticipantId());
            participant.setParticipantType(participantMessage.getParticipantType());
            participant.setParticipantState(participantState);
            participant.setHealthStatus(healthStatus);

            participantList.add(participant);
            participantProvider.createParticipants(participantList);
        } else {
            for (Participant participant : participantList) {
                participant.setParticipantState(participantState);
                participant.setHealthStatus(healthStatus);
            }
            participantProvider.updateParticipants(participantList);
        }
    }

    private void superviseParticipant(ParticipantStatus participantStatusMessage)
            throws PfModelException, ControlLoopException {

        checkParticipant(participantStatusMessage, participantStatusMessage.getState(),
                participantStatusMessage.getHealthStatus());

        monitoringProvider.createParticipantStatistics(List.of(participantStatusMessage.getParticipantStatistics()));
    }

    private void superviseControlLoops(ParticipantStatus participantStatusMessage)
            throws PfModelException, ControlLoopException {
        if (participantStatusMessage.getControlLoopInfoList() != null) {
            for (ControlLoopInfo clEntry : participantStatusMessage.getControlLoopInfoList()) {
                var dbControlLoop =
                        controlLoopProvider.getControlLoop(new ToscaConceptIdentifier(clEntry.getControlLoopId()));
                if (dbControlLoop == null) {
                    exceptionOccured(Response.Status.NOT_FOUND,
                            "PARTICIPANT_STATUS control loop not found in database: " + clEntry.getControlLoopId());
                }
                dbControlLoop.setState(clEntry.getState());
                monitoringProvider.createClElementStatistics(
                        clEntry.getControlLoopStatistics().getClElementStatisticsList().getClElementStatistics());
            }
        }
    }

    private void exceptionOccured(Response.Status status, String reason) throws ControlLoopException {
        throw new ControlLoopException(status, reason);
    }
}
