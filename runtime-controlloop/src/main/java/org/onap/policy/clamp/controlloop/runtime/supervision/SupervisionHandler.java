/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopException;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElementDefinition;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.Participant;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ControlLoopProvider;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantControlLoopStateChange;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantControlLoopUpdate;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantDeregister;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantDeregisterAck;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantRegister;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantRegisterAck;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantStatus;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantUpdate;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantUpdateAck;
import org.onap.policy.clamp.controlloop.runtime.commissioning.CommissioningProvider;
import org.onap.policy.clamp.controlloop.runtime.main.parameters.ClRuntimeParameterGroup;
import org.onap.policy.clamp.controlloop.runtime.monitoring.MonitoringProvider;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ParticipantControlLoopStateChangePublisher;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ParticipantControlLoopUpdatePublisher;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ParticipantDeregisterAckPublisher;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ParticipantRegisterAckPublisher;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ParticipantStateChangePublisher;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ParticipantStatusListener;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ParticipantUpdatePublisher;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.common.endpoints.listeners.MessageTypeDispatcher;
import org.onap.policy.common.utils.services.ServiceManager;
import org.onap.policy.common.utils.services.ServiceManagerException;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
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
    private final CommissioningProvider commissioningProvider;

    // Publishers for participant communication
    private final ParticipantControlLoopUpdatePublisher controlLoopUpdatePublisher;
    private final ParticipantControlLoopStateChangePublisher controlLoopStateChangePublisher;
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
    public void handleParticipantMessage(ParticipantRegister participantRegisterMessage) {
        LOGGER.debug("Participant Register received {}", participantRegisterMessage);
        sendParticipantAckMessage(participantRegisterMessage);
        sendParticipantUpdate(participantRegisterMessage);
    }

    /**
     * Handle a ParticipantDeregister message from a participant.
     *
     * @param participantDeregisterMessage the ParticipantDeregister message received from a participant
     */
    public void handleParticipantMessage(ParticipantDeregister participantDeregisterMessage) {
        LOGGER.debug("Participant Deregister received {}", participantDeregisterMessage);
        sendParticipantAckMessage(participantDeregisterMessage);
    }

    /**
     * Handle a ParticipantUpdateAck message from a participant.
     *
     * @param participantUpdateAckMessage the ParticipantUpdateAck message received from a participant
     */
    public void handleParticipantMessage(ParticipantUpdateAck participantUpdateAckMessage) {
        LOGGER.debug("Participant Update Ack received {}", participantUpdateAckMessage);
    }

    /**
     * Supervise a control loop, performing whatever actions need to be performed on the control loop.
     *
     * @param controlLoop the control loop to supervises
     * @throws PfModelException on accessing models in the database
     * @throws ControlLoopException on supervision errors
     */
    private void superviseControlLoop(ControlLoop controlLoop) throws ControlLoopException, PfModelException {
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
                controlLoopStateChangePublisher.send(controlLoop);
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

    private void superviseControlLoopPassivation(ControlLoop controlLoop)
            throws ControlLoopException, PfModelException {
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
                controlLoopStateChangePublisher.send(controlLoop);
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
                controlLoopStateChangePublisher.send(controlLoop);
                break;

            default:
                exceptionOccured(Response.Status.NOT_ACCEPTABLE, CONTROL_LOOP_CANNOT_TRANSITION_FROM_STATE
                        + controlLoop.getState().name() + TO_STATE + controlLoop.getOrderedState());
                break;
        }
    }

    private void sendControlLoopUpdate(ControlLoop controlLoop) throws PfModelException {
        var pclu = new ParticipantControlLoopUpdate();
        pclu.setControlLoopId(controlLoop.getKey().asIdentifier());
        pclu.setControlLoop(controlLoop);
        // TODO: We should look up the correct TOSCA node template here for the control loop
        // Tiny hack implemented to return the tosca service template entry from the database and be passed onto dmaap
        pclu.setControlLoopDefinition(commissioningProvider.getToscaServiceTemplate(null, null));
        controlLoopUpdatePublisher.send(pclu);
    }

    private void sendControlLoopStateChange(ControlLoop controlLoop) {
        var clsc = new ParticipantControlLoopStateChange();
        clsc.setControlLoopId(controlLoop.getKey().asIdentifier());
        clsc.setMessageId(UUID.randomUUID());
        clsc.setOrderedState(controlLoop.getOrderedState());
        controlLoopStateChangePublisher.send(clsc);
    }

    private void sendParticipantUpdate(ParticipantRegister participantRegisterMessage) {
        var message = new ParticipantUpdate();
        message.setParticipantId(participantRegisterMessage.getParticipantId());
        message.setParticipantType(participantRegisterMessage.getParticipantType());
        message.setTimestamp(Instant.now());

        ControlLoopElementDefinition clDefinition = new ControlLoopElementDefinition();
        clDefinition.setId(UUID.randomUUID());

        try {
            clDefinition.setControlLoopElementToscaServiceTemplate(commissioningProvider
                    .getToscaServiceTemplate(null, null));
        } catch (PfModelException pfme) {
            LOGGER.warn("Get of tosca service template failed, cannot send participantupdate", pfme);
            return;
        }

        Map<UUID, ControlLoopElementDefinition> controlLoopElementDefinitionMap = new LinkedHashMap<>();
        controlLoopElementDefinitionMap.put(UUID.randomUUID(), clDefinition);

        Map<ToscaConceptIdentifier, Map<UUID, ControlLoopElementDefinition>>
            participantDefinitionUpdateMap = new LinkedHashMap<>();
        participantDefinitionUpdateMap.put(participantRegisterMessage.getParticipantId(),
                      controlLoopElementDefinitionMap);
        message.setParticipantDefinitionUpdateMap(participantDefinitionUpdateMap);

        LOGGER.debug("Participant Update sent", message);
        participantUpdatePublisher.send(message);
    }

    private void sendParticipantAckMessage(ParticipantRegister participantRegisterMessage) {
        var message = new ParticipantRegisterAck();
        message.setResponseTo(participantRegisterMessage.getMessageId());
        message.setMessage("Participant Register Ack");
        message.setResult(true);
        participantRegisterAckPublisher.send(message);
    }

    private void sendParticipantAckMessage(ParticipantDeregister participantDeregisterMessage) {
        var message = new ParticipantDeregisterAck();
        message.setResponseTo(participantDeregisterMessage.getMessageId());
        message.setMessage("Participant Deregister Ack");
        message.setResult(true);
        participantDeregisterAckPublisher.send(message);
    }

    private void superviseParticipant(ParticipantStatus participantStatusMessage)
            throws PfModelException, ControlLoopException {
        if (participantStatusMessage.getParticipantId() == null) {
            exceptionOccured(Response.Status.NOT_FOUND, "Participant ID on PARTICIPANT_STATUS message is null");
        }

        List<Participant> participantList =
                participantProvider.getParticipants(participantStatusMessage.getParticipantId().getName(),
                        participantStatusMessage.getParticipantId().getVersion());

        if (CollectionUtils.isEmpty(participantList)) {
            var participant = new Participant();
            participant.setName(participantStatusMessage.getParticipantId().getName());
            participant.setVersion(participantStatusMessage.getParticipantId().getVersion());
            participant.setDefinition(new ToscaConceptIdentifier("unknown", "0.0.0"));
            participant.setParticipantState(participantStatusMessage.getState());
            participant.setHealthStatus(participantStatusMessage.getHealthStatus());

            participantList.add(participant);
            participantProvider.createParticipants(participantList);
        } else {
            for (Participant participant : participantList) {
                participant.setParticipantState(participantStatusMessage.getState());
                participant.setHealthStatus(participantStatusMessage.getHealthStatus());
            }
            participantProvider.updateParticipants(participantList);
        }

        monitoringProvider.createParticipantStatistics(List.of(participantStatusMessage.getParticipantStatistics()));
    }

    private void superviseControlLoops(ParticipantStatus participantStatusMessage)
            throws PfModelException, ControlLoopException {
        if (CollectionUtils.isEmpty(participantStatusMessage.getControlLoops().getControlLoopList())) {
            return;
        }

        for (ControlLoop controlLoop : participantStatusMessage.getControlLoops().getControlLoopList()) {
            if (controlLoop == null) {
                exceptionOccured(Response.Status.NOT_FOUND,
                        "PARTICIPANT_STATUS message references unknown control loop: " + controlLoop);
            }

            var dbControlLoop = controlLoopProvider
                    .getControlLoop(new ToscaConceptIdentifier(controlLoop.getName(), controlLoop.getVersion()));
            if (dbControlLoop == null) {
                exceptionOccured(Response.Status.NOT_FOUND,
                        "PARTICIPANT_STATUS control loop not found in database: " + controlLoop);
            }

            for (ControlLoopElement element : controlLoop.getElements().values()) {
                ControlLoopElement dbElement = dbControlLoop.getElements().get(element.getId());

                if (dbElement == null) {
                    exceptionOccured(Response.Status.NOT_FOUND,
                            "PARTICIPANT_STATUS message references unknown control loop element: " + element);
                }

                // Replace element entry in the database
                dbControlLoop.getElements().put(element.getId(), element);
            }
            controlLoopProvider.updateControlLoop(dbControlLoop);
        }

        for (ControlLoop controlLoop : participantStatusMessage.getControlLoops().getControlLoopList()) {
            monitoringProvider.createClElementStatistics(controlLoop.getControlLoopElementStatisticsList(controlLoop));
        }
    }

    private void exceptionOccured(Response.Status status, String reason) throws ControlLoopException {
        throw new ControlLoopException(status, reason);
    }
}
