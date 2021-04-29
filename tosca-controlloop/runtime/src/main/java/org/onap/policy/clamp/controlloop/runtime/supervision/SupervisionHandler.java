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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.core.Response;
import org.apache.commons.collections4.CollectionUtils;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopException;
import org.onap.policy.clamp.controlloop.common.handler.ControlLoopHandler;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.Participant;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ControlLoopProvider;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantControlLoopStateChange;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantControlLoopUpdate;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantStatus;
import org.onap.policy.clamp.controlloop.runtime.commissioning.CommissioningHandler;
import org.onap.policy.clamp.controlloop.runtime.commissioning.CommissioningProvider;
import org.onap.policy.clamp.controlloop.runtime.main.parameters.ClRuntimeParameterGroup;
import org.onap.policy.clamp.controlloop.runtime.monitoring.MonitoringHandler;
import org.onap.policy.clamp.controlloop.runtime.monitoring.MonitoringProvider;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ParticipantControlLoopStateChangePublisher;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ParticipantControlLoopUpdatePublisher;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ParticipantStateChangePublisher;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ParticipantStatusListener;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.common.endpoints.listeners.MessageTypeDispatcher;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles supervision of control loop instances, so only one object of this type should be built at a time.
 *
 * </p> It is effectively a singleton that is started at system start.
 */
public class SupervisionHandler extends ControlLoopHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SupervisionHandler.class);

    private static final String CONTROL_LOOP_CANNOT_TRANSITION_FROM_STATE = "Control loop can't transition from state ";
    private static final String CONTROL_LOOP_IS_ALREADY_IN_STATE = "Control loop is already in state ";
    private static final String TO_STATE = " to state ";
    private static final String AND_TRANSITIONING_TO_STATE = " and transitioning to state ";

    private ControlLoopProvider controlLoopProvider;
    private ParticipantProvider participantProvider;
    private CommissioningProvider commissioningProvider;
    private MonitoringProvider monitoringProvider;

    // Publishers for participant communication
    private ParticipantStateChangePublisher stateChangePublisher;
    private ParticipantControlLoopUpdatePublisher controlLoopUpdatePublisher;
    private ParticipantControlLoopStateChangePublisher controlLoopStateChangePublisher;

    // Database scanner
    private SupervisionScanner scanner;

    /**
     * Gets the SupervisionHandler.
     *
     * @return SupervisionHandler
     */
    public static SupervisionHandler getInstance() {
        return Registry.get(SupervisionHandler.class.getName());
    }

    /**
     * Create a handler.
     *
     * @param clRuntimeParameterGroup the parameters for the control loop runtime
     */
    public SupervisionHandler(ClRuntimeParameterGroup clRuntimeParameterGroup) {
        super(clRuntimeParameterGroup.getDatabaseProviderParameters());
    }

    /**
     * Supervision trigger called when a command is issued on control loops.
     *
     * </p> Causes supervision to start or continue supervision on the control loops in question.
     *
     * @param controlLoopIdentifierList the control loops for which the supervision command has been issued
     * @throws ControlLoopException on supervision triggering exceptions
     */
    public void triggerControlLoopSupervision(List<ToscaConceptIdentifier> controlLoopIdentifierList)
            throws ControlLoopException {

        LOGGER.debug("triggering control loop supervision on control loops {}", controlLoopIdentifierList);

        if (CollectionUtils.isEmpty(controlLoopIdentifierList)) {
            // This is just to force throwing of the exception in certain circumstances.
            throw new ControlLoopException(Response.Status.NOT_ACCEPTABLE,
                    "The list of control loops for supervision is empty");
        }

        for (ToscaConceptIdentifier controlLoopId : controlLoopIdentifierList) {
            try {
                ControlLoop controlLoop = controlLoopProvider.getControlLoop(controlLoopId);

                superviseControlLoop(controlLoop);

                controlLoopProvider.updateControlLoop(controlLoop);
            } catch (PfModelException pfme) {
                throw new ControlLoopException(pfme.getErrorResponse().getResponseCode(), pfme.getMessage(), pfme);
            }
        }
    }

    @Override
    public void startAndRegisterListeners(MessageTypeDispatcher msgDispatcher) {
        msgDispatcher.register(ParticipantMessageType.PARTICIPANT_STATUS.name(), new ParticipantStatusListener());
    }

    @Override
    public void startAndRegisterPublishers(List<TopicSink> topicSinks) {
        // TODO: Use a parameter for the timeout
        scanner = new SupervisionScanner(controlLoopProvider, 10000);
        controlLoopUpdatePublisher = new ParticipantControlLoopUpdatePublisher(topicSinks, -1);
        stateChangePublisher = new ParticipantStateChangePublisher(topicSinks, 10000);
        controlLoopStateChangePublisher = new ParticipantControlLoopStateChangePublisher(topicSinks, -1);
    }

    @Override
    public void stopAndUnregisterPublishers() {
        controlLoopStateChangePublisher.terminate();
        stateChangePublisher.terminate();
        controlLoopUpdatePublisher.terminate();
        scanner.terminate();
    }

    @Override
    public void stopAndUnregisterListeners(MessageTypeDispatcher msgDispatcher) {
        msgDispatcher.unregister(ParticipantMessageType.PARTICIPANT_STATUS.name());
    }

    /**
     * Handle a ParticipantStatus message from a participant.
     *
     * @param participantStatusMessage the ParticipantStatus message received from a participant
     */
    public void handleParticipantStausMessage(ParticipantStatus participantStatusMessage) {
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
     * Supervise a control loop, performing whatever actions need to be performed on the control loop.
     *
     * @param controlLoop the control loop to supervises
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
                throw new ControlLoopException(Response.Status.NOT_ACCEPTABLE,
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
                throw new ControlLoopException(Response.Status.NOT_ACCEPTABLE,
                        CONTROL_LOOP_IS_ALREADY_IN_STATE + controlLoop.getState().name());

            case UNINITIALISED2PASSIVE:
            case PASSIVE:
                controlLoop.setState(ControlLoopState.PASSIVE2UNINITIALISED);
                sendControlLoopStateChange(controlLoop);
                break;

            case PASSIVE2UNINITIALISED:
                throw new ControlLoopException(Response.Status.NOT_ACCEPTABLE, CONTROL_LOOP_IS_ALREADY_IN_STATE
                        + controlLoop.getState().name() + AND_TRANSITIONING_TO_STATE + controlLoop.getOrderedState());

            default:
                throw new ControlLoopException(Response.Status.NOT_ACCEPTABLE, CONTROL_LOOP_CANNOT_TRANSITION_FROM_STATE
                        + controlLoop.getState().name() + TO_STATE + controlLoop.getOrderedState());
        }
    }

    private void superviseControlLoopPassivation(ControlLoop controlLoop)
            throws ControlLoopException, PfModelException {
        switch (controlLoop.getState()) {
            case PASSIVE:
                throw new ControlLoopException(Response.Status.NOT_ACCEPTABLE,
                        CONTROL_LOOP_IS_ALREADY_IN_STATE + controlLoop.getState().name());

            case UNINITIALISED:
                controlLoop.setState(ControlLoopState.UNINITIALISED2PASSIVE);
                sendControlLoopUpdate(controlLoop);
                break;

            case UNINITIALISED2PASSIVE:
            case RUNNING2PASSIVE:
                throw new ControlLoopException(Response.Status.NOT_ACCEPTABLE, CONTROL_LOOP_IS_ALREADY_IN_STATE
                        + controlLoop.getState().name() + AND_TRANSITIONING_TO_STATE + controlLoop.getOrderedState());

            case RUNNING:
                controlLoop.setState(ControlLoopState.RUNNING2PASSIVE);
                sendControlLoopStateChange(controlLoop);
                break;

            default:
                throw new ControlLoopException(Response.Status.NOT_ACCEPTABLE, CONTROL_LOOP_CANNOT_TRANSITION_FROM_STATE
                        + controlLoop.getState().name() + TO_STATE + controlLoop.getOrderedState());
        }
    }

    private void superviseControlLoopActivation(ControlLoop controlLoop) throws ControlLoopException {
        switch (controlLoop.getState()) {
            case RUNNING:
                throw new ControlLoopException(Response.Status.NOT_ACCEPTABLE,
                        CONTROL_LOOP_IS_ALREADY_IN_STATE + controlLoop.getState().name());

            case PASSIVE2RUNNING:
                throw new ControlLoopException(Response.Status.NOT_ACCEPTABLE, CONTROL_LOOP_IS_ALREADY_IN_STATE
                        + controlLoop.getState().name() + AND_TRANSITIONING_TO_STATE + controlLoop.getOrderedState());

            case PASSIVE:
                controlLoop.setState(ControlLoopState.PASSIVE2RUNNING);
                sendControlLoopStateChange(controlLoop);
                break;

            default:
                throw new ControlLoopException(Response.Status.NOT_ACCEPTABLE, CONTROL_LOOP_CANNOT_TRANSITION_FROM_STATE
                        + controlLoop.getState().name() + TO_STATE + controlLoop.getOrderedState());
        }
    }

    private void sendControlLoopUpdate(ControlLoop controlLoop) throws PfModelException {
        ParticipantControlLoopUpdate pclu = new ParticipantControlLoopUpdate();
        pclu.setControlLoopId(controlLoop.getKey().asIdentifier());
        pclu.setControlLoop(controlLoop);
        // TODO: We should look up the correct TOSCA node template here for the control loop
        // Tiny hack implemented to return the tosca service template entry from the database and be passed onto dmaap
        commissioningProvider = CommissioningHandler.getInstance().getProvider();
        pclu.setControlLoopDefinition(commissioningProvider.getToscaServiceTemplate(null, null));
        controlLoopUpdatePublisher.send(pclu);
    }

    private void sendControlLoopStateChange(ControlLoop controlLoop) {
        ParticipantControlLoopStateChange clsc = new ParticipantControlLoopStateChange();
        clsc.setControlLoopId(controlLoop.getKey().asIdentifier());
        clsc.setMessageId(UUID.randomUUID());
        clsc.setOrderedState(controlLoop.getOrderedState());

        controlLoopStateChangePublisher.send(clsc);
    }

    private void superviseParticipant(ParticipantStatus participantStatusMessage)
            throws PfModelException, ControlLoopException {
        if (participantStatusMessage.getParticipantId() == null) {
            throw new ControlLoopException(Response.Status.NOT_FOUND,
                    "Participant ID on PARTICIPANT_STATUS message is null");
        }

        List<Participant> participantList =
                participantProvider.getParticipants(participantStatusMessage.getParticipantId().getName(),
                        participantStatusMessage.getParticipantId().getVersion());

        if (CollectionUtils.isEmpty(participantList)) {
            Participant participant = new Participant();
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

        monitoringProvider = MonitoringHandler.getInstance().getMonitoringProvider();
        List participantStatisticsList = new ArrayList<>();
        participantStatisticsList.add(participantStatusMessage.getParticipantStatistics());
        monitoringProvider.createParticipantStatistics(participantStatisticsList);
    }

    private void superviseControlLoops(ParticipantStatus participantStatusMessage)
            throws PfModelException, ControlLoopException {
        if (CollectionUtils.isEmpty(participantStatusMessage.getControlLoops().getControlLoopList())) {
            return;
        }

        for (ControlLoop controlLoop : participantStatusMessage.getControlLoops().getControlLoopList()) {
            if (controlLoop == null) {
                throw new ControlLoopException(Response.Status.NOT_FOUND,
                        "PARTICIPANT_STATUS message references unknown control loop: " + controlLoop);
            }

            ControlLoop dbControlLoop = controlLoopProvider
                    .getControlLoop(new ToscaConceptIdentifier(controlLoop.getName(), controlLoop.getVersion()));
            if (dbControlLoop == null) {
                throw new ControlLoopException(Response.Status.NOT_FOUND,
                        "PARTICIPANT_STATUS control loop not found in database: " + controlLoop);
            }

            for (ControlLoopElement element : controlLoop.getElements().values()) {
                ControlLoopElement dbElement = controlLoop.getElements().get(element.getId());

                if (dbElement == null) {
                    throw new ControlLoopException(Response.Status.NOT_FOUND,
                            "PARTICIPANT_STATUS message references unknown control loop element: " + element);
                }

                // Replace element entry in the database
                dbControlLoop.getElements().put(element.getId(), element);
            }
            controlLoopProvider.updateControlLoop(dbControlLoop);
        }

        monitoringProvider = MonitoringHandler.getInstance().getMonitoringProvider();
        for (ControlLoop controlLoop : participantStatusMessage.getControlLoops().getControlLoopList()) {
            monitoringProvider.createClElementStatistics(controlLoop.getControlLoopElementStatisticsList(controlLoop));
        }
    }

    @Override
    public void startProviders() {
        try {
            controlLoopProvider = new ControlLoopProvider(getDatabaseProviderParameters());
            participantProvider = new ParticipantProvider(getDatabaseProviderParameters());
        } catch (PfModelException e) {
            throw new PfModelRuntimeException(e);
        }
    }

    @Override
    public void stopProviders() {
        controlLoopProvider.close();
        participantProvider.close();
    }
}
