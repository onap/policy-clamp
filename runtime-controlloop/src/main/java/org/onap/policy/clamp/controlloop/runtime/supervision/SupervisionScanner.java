/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 Nordix Foundation.
 * ================================================================================
 * Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.Participant;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantHealthStatus;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantUtils;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ControlLoopProvider;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ServiceTemplateProvider;
import org.onap.policy.clamp.controlloop.runtime.main.parameters.ClRuntimeParameterGroup;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ControlLoopStateChangePublisher;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ControlLoopUpdatePublisher;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ParticipantStatusReqPublisher;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ParticipantUpdatePublisher;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class is used to scan the control loops in the database and check if they are in the correct state.
 */
@Component
public class SupervisionScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(SupervisionScanner.class);

    private final HandleCounter<ToscaConceptIdentifier> controlLoopCounter = new HandleCounter<>();
    private final HandleCounter<ToscaConceptIdentifier> participantStatusCounter = new HandleCounter<>();
    private final HandleCounter<Pair<ToscaConceptIdentifier, ToscaConceptIdentifier>> participantUpdateCounter =
            new HandleCounter<>();

    private final Map<ToscaConceptIdentifier, Integer> phaseMap = new HashMap<>();

    private final ControlLoopProvider controlLoopProvider;
    private final ServiceTemplateProvider serviceTemplateProvider;
    private final ControlLoopStateChangePublisher controlLoopStateChangePublisher;
    private final ControlLoopUpdatePublisher controlLoopUpdatePublisher;
    private final ParticipantProvider participantProvider;
    private final ParticipantStatusReqPublisher participantStatusReqPublisher;
    private final ParticipantUpdatePublisher participantUpdatePublisher;

    /**
     * Constructor for instantiating SupervisionScanner.
     *
     * @param controlLoopProvider the provider to use to read control loops from the database
     * @param serviceTemplateProvider the Policy Models Provider
     * @param controlLoopStateChangePublisher the ControlLoop StateChange Publisher
     * @param controlLoopUpdatePublisher the ControlLoopUpdate Publisher
     * @param participantProvider the Participant Provider
     * @param participantStatusReqPublisher the Participant StatusReq Publisher
     * @param participantUpdatePublisher the Participant Update Publisher
     * @param clRuntimeParameterGroup the parameters for the control loop runtime
     */
    public SupervisionScanner(final ControlLoopProvider controlLoopProvider,
            ServiceTemplateProvider serviceTemplateProvider,
            final ControlLoopStateChangePublisher controlLoopStateChangePublisher,
            ControlLoopUpdatePublisher controlLoopUpdatePublisher, ParticipantProvider participantProvider,
            ParticipantStatusReqPublisher participantStatusReqPublisher,
            ParticipantUpdatePublisher participantUpdatePublisher,
            final ClRuntimeParameterGroup clRuntimeParameterGroup) {
        this.controlLoopProvider = controlLoopProvider;
        this.serviceTemplateProvider = serviceTemplateProvider;
        this.controlLoopStateChangePublisher = controlLoopStateChangePublisher;
        this.controlLoopUpdatePublisher = controlLoopUpdatePublisher;
        this.participantProvider = participantProvider;
        this.participantStatusReqPublisher = participantStatusReqPublisher;
        this.participantUpdatePublisher = participantUpdatePublisher;

        controlLoopCounter.setMaxRetryCount(
                clRuntimeParameterGroup.getParticipantParameters().getUpdateParameters().getMaxRetryCount());
        controlLoopCounter.setMaxWaitMs(clRuntimeParameterGroup.getParticipantParameters().getMaxStatusWaitMs());

        participantUpdateCounter.setMaxRetryCount(
                clRuntimeParameterGroup.getParticipantParameters().getUpdateParameters().getMaxRetryCount());
        participantUpdateCounter
                .setMaxWaitMs(clRuntimeParameterGroup.getParticipantParameters().getUpdateParameters().getMaxWaitMs());

        participantStatusCounter.setMaxRetryCount(
                clRuntimeParameterGroup.getParticipantParameters().getUpdateParameters().getMaxRetryCount());
        participantStatusCounter.setMaxWaitMs(clRuntimeParameterGroup.getParticipantParameters().getMaxStatusWaitMs());
    }

    /**
     * Run Scanning.
     *
     * @param counterCheck if true activate counter and retry
     */
    public void run(boolean counterCheck) {
        LOGGER.debug("Scanning control loops in the database . . .");

        if (counterCheck) {
            try {
                for (var participant : participantProvider.getParticipants()) {
                    scanParticipantStatus(participant);
                }
            } catch (PfModelException pfme) {
                LOGGER.warn("error reading participant from database", pfme);
                return;
            }
        }

        try {
            var list = serviceTemplateProvider.getServiceTemplateList(null, null);
            if (list != null && !list.isEmpty()) {
                ToscaServiceTemplate toscaServiceTemplate = list.get(0);

                for (ControlLoop controlLoop : controlLoopProvider.getControlLoops(null, null)) {
                    scanControlLoop(controlLoop, toscaServiceTemplate, counterCheck);
                }
            }
        } catch (PfModelException pfme) {
            LOGGER.warn("error reading control loops from database", pfme);
        }

        if (counterCheck) {
            scanParticipantUpdate();
        }

        LOGGER.debug("Control loop scan complete . . .");
    }

    private void scanParticipantUpdate() {
        LOGGER.debug("Scanning participants to update . . .");

        for (var id : participantUpdateCounter.keySet()) {
            if (participantUpdateCounter.isFault(id)) {
                LOGGER.debug("report Participant Update fault");

            } else if (participantUpdateCounter.getDuration(id) > participantUpdateCounter.getMaxWaitMs()) {

                if (participantUpdateCounter.count(id)) {
                    LOGGER.debug("retry message ParticipantUpdate");
                    participantUpdatePublisher.sendCommissioning(null, null, id.getLeft(), id.getRight());
                } else {
                    LOGGER.debug("report Participant Update fault");
                    participantUpdateCounter.setFault(id);
                }
            }
        }

        LOGGER.debug("Participants to update scan complete . . .");
    }

    private void scanParticipantStatus(Participant participant) throws PfModelException {
        ToscaConceptIdentifier id = participant.getKey().asIdentifier();
        if (participantStatusCounter.isFault(id)) {
            LOGGER.debug("report Participant fault");
            return;
        }
        if (participantStatusCounter.getDuration(id) > participantStatusCounter.getMaxWaitMs()) {
            if (participantStatusCounter.count(id)) {
                LOGGER.debug("retry message ParticipantStatusReq");
                participantStatusReqPublisher.send(id);
                participant.setHealthStatus(ParticipantHealthStatus.NOT_HEALTHY);
            } else {
                LOGGER.debug("report Participant fault");
                participantStatusCounter.setFault(id);
                participant.setHealthStatus(ParticipantHealthStatus.OFF_LINE);
            }
            participantProvider.saveParticipant(participant);
        }
    }

    /**
     * handle participant Status message.
     */
    public void handleParticipantStatus(ToscaConceptIdentifier id) {
        participantStatusCounter.clear(id);
    }

    public void handleParticipantRegister(Pair<ToscaConceptIdentifier, ToscaConceptIdentifier> id) {
        participantUpdateCounter.clear(id);
    }

    public void handleParticipantUpdateAck(Pair<ToscaConceptIdentifier, ToscaConceptIdentifier> id) {
        participantUpdateCounter.remove(id);
    }

    private void scanControlLoop(final ControlLoop controlLoop, ToscaServiceTemplate toscaServiceTemplate,
            boolean counterCheck) throws PfModelException {
        LOGGER.debug("scanning control loop {} . . .", controlLoop.getKey().asIdentifier());

        if (controlLoop.getState().equals(controlLoop.getOrderedState().asState())) {
            LOGGER.debug("control loop {} scanned, OK", controlLoop.getKey().asIdentifier());

            // Clear missed report counter on Control Loop
            clearFaultAndCounter(controlLoop);
            return;
        }

        var completed = true;
        var minSpNotCompleted = 1000; // min startPhase not completed
        var maxSpNotCompleted = 0; // max startPhase not completed
        var defaultMin = 1000; // min startPhase
        var defaultMax = 0; // max startPhase
        for (ControlLoopElement element : controlLoop.getElements().values()) {
            ToscaNodeTemplate toscaNodeTemplate = toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates()
                    .get(element.getDefinition().getName());
            int startPhase = ParticipantUtils.findStartPhase(toscaNodeTemplate.getProperties());
            defaultMin = Math.min(defaultMin, startPhase);
            defaultMax = Math.max(defaultMax, startPhase);
            if (!element.getState().equals(element.getOrderedState().asState())) {
                completed = false;
                minSpNotCompleted = Math.min(minSpNotCompleted, startPhase);
                maxSpNotCompleted = Math.max(maxSpNotCompleted, startPhase);
            }
        }

        if (completed) {
            LOGGER.debug("control loop scan: transition from state {} to {} completed", controlLoop.getState(),
                    controlLoop.getOrderedState());

            controlLoop.setState(controlLoop.getOrderedState().asState());
            controlLoopProvider.updateControlLoop(controlLoop);

            // Clear missed report counter on Control Loop
            clearFaultAndCounter(controlLoop);
        } else {
            LOGGER.debug("control loop scan: transition from state {} to {} not completed", controlLoop.getState(),
                    controlLoop.getOrderedState());

            var nextSpNotCompleted = ControlLoopState.UNINITIALISED2PASSIVE.equals(controlLoop.getState())
                    || ControlLoopState.PASSIVE2RUNNING.equals(controlLoop.getState()) ? minSpNotCompleted
                            : maxSpNotCompleted;

            var firstStartPhase = ControlLoopState.UNINITIALISED2PASSIVE.equals(controlLoop.getState())
                    || ControlLoopState.PASSIVE2RUNNING.equals(controlLoop.getState()) ? defaultMin
                            : defaultMax;

            if (nextSpNotCompleted != phaseMap.getOrDefault(controlLoop.getKey().asIdentifier(), firstStartPhase)) {
                phaseMap.put(controlLoop.getKey().asIdentifier(), nextSpNotCompleted);
                sendControlLoopMsg(controlLoop, nextSpNotCompleted);
            } else if (counterCheck) {
                phaseMap.put(controlLoop.getKey().asIdentifier(), nextSpNotCompleted);
                handleCounter(controlLoop, nextSpNotCompleted);
            }
        }
    }

    private void clearFaultAndCounter(ControlLoop controlLoop) {
        controlLoopCounter.clear(controlLoop.getKey().asIdentifier());
        phaseMap.clear();
    }

    private void handleCounter(ControlLoop controlLoop, int startPhase) {
        ToscaConceptIdentifier id = controlLoop.getKey().asIdentifier();
        if (controlLoopCounter.isFault(id)) {
            LOGGER.debug("report ControlLoop fault");
            return;
        }

        if (controlLoopCounter.getDuration(id) > controlLoopCounter.getMaxWaitMs()) {
            if (controlLoopCounter.count(id)) {
                phaseMap.put(id, startPhase);
                sendControlLoopMsg(controlLoop, startPhase);
            } else {
                LOGGER.debug("report ControlLoop fault");
                controlLoopCounter.setFault(id);
            }
        }
    }

    private void sendControlLoopMsg(ControlLoop controlLoop, int startPhase) {
        if (ControlLoopState.UNINITIALISED2PASSIVE.equals(controlLoop.getState())) {
            LOGGER.debug("retry message ControlLoopUpdate");
            controlLoopUpdatePublisher.send(controlLoop, startPhase);
        } else {
            LOGGER.debug("retry message ControlLoopStateChange");
            controlLoopStateChangePublisher.send(controlLoop, startPhase);
        }
    }
}
