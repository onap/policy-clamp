/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 Nordix Foundation.
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
import lombok.Getter;
import lombok.Setter;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ControlLoopProvider;
import org.onap.policy.clamp.controlloop.runtime.main.parameters.ClRuntimeParameterGroup;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ParticipantControlLoopStateChangePublisher;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ParticipantControlLoopUpdatePublisher;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class is used to scan the control loops in the database and check if they are in the correct state.
 */
@Component
public class SupervisionScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(SupervisionScanner.class);

    @Getter
    @Setter
    static class HandleCounter {
        private int maxRetryCount;
        private long maxWaitMs;
        private Map<ToscaConceptIdentifier, Integer> mapCounter = new HashMap<>();
        private Map<ToscaConceptIdentifier, Boolean> mapFault = new HashMap<>();

        public void clear(ToscaConceptIdentifier id) {
            mapCounter.put(id, 0);
            mapFault.put(id, false);
        }

        public void setFault(ToscaConceptIdentifier id) {
            mapCounter.put(id, 0);
            mapFault.put(id, true);
        }

        public boolean isFault(ToscaConceptIdentifier id) {
            return Boolean.TRUE.equals(mapFault.getOrDefault(id, false));
        }

        public int getCounter(ToscaConceptIdentifier id) {
            return mapCounter.getOrDefault(id, 0);
        }
    }

    private HandleCounter stateChange = new HandleCounter();

    private final ControlLoopProvider controlLoopProvider;
    private final ParticipantControlLoopStateChangePublisher controlLoopStateChangePublisher;
    private final ParticipantControlLoopUpdatePublisher controlLoopUpdatePublisher;

    /**
     * Constructor for instantiating SupervisionScanner.
     *
     * @param controlLoopProvider the provider to use to read control loops from the database
     * @param controlLoopStateChangePublisher the ControlLoopStateChange Publisher
     * @param clRuntimeParameterGroup the parameters for the control loop runtime
     */
    public SupervisionScanner(final ControlLoopProvider controlLoopProvider,
            final ParticipantControlLoopStateChangePublisher controlLoopStateChangePublisher,
            ParticipantControlLoopUpdatePublisher controlLoopUpdatePublisher,
            final ClRuntimeParameterGroup clRuntimeParameterGroup) {
        this.controlLoopProvider = controlLoopProvider;
        this.controlLoopStateChangePublisher = controlLoopStateChangePublisher;
        this.controlLoopUpdatePublisher = controlLoopUpdatePublisher;

        stateChange.setMaxRetryCount(
                clRuntimeParameterGroup.getParticipantParameters().getStateChangeParameters().getMaxRetryCount());
        stateChange.setMaxWaitMs(
                clRuntimeParameterGroup.getParticipantParameters().getStateChangeParameters().getMaxWaitMs());
    }

    /**
     * Run Scanning.
     *
     * @param counterCheck if true active counter and retry
     */
    public void run(boolean counterCheck) {
        LOGGER.debug("Scanning control loops in the database . . .");

        try {
            for (ControlLoop controlLoop : controlLoopProvider.getControlLoops(null, null)) {
                scanControlLoop(controlLoop, counterCheck);
            }
        } catch (PfModelException pfme) {
            LOGGER.warn("error reading control loops from database", pfme);
        }

        LOGGER.debug("Control loop scan complete . . .");
    }

    private void scanControlLoop(final ControlLoop controlLoop, boolean counterCheck) throws PfModelException {
        LOGGER.debug("scanning control loop {} . . .", controlLoop.getKey().asIdentifier());

        if (controlLoop.getState().equals(controlLoop.getOrderedState().asState())) {
            LOGGER.debug("control loop {} scanned, OK", controlLoop.getKey().asIdentifier());

            // Clear missed report counter on Control Loop
            clearFaultAndCounter(controlLoop);
            return;
        }

        boolean completed = true;
        for (ControlLoopElement element : controlLoop.getElements().values()) {
            if (!element.getState().equals(element.getOrderedState().asState())) {
                completed = false;
                break;
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
            if (counterCheck) {
                handleCounter(controlLoop);
            }
        }
    }

    private void clearFaultAndCounter(ControlLoop controlLoop) {
        stateChange.clear(controlLoop.getKey().asIdentifier());
    }

    private void handleCounter(ControlLoop controlLoop) {
        ToscaConceptIdentifier id = controlLoop.getKey().asIdentifier();
        if (stateChange.isFault(id)) {
            LOGGER.debug("report ControlLoop fault");
            return;
        }

        int counter = stateChange.getCounter(id) + 1;
        if (counter <= stateChange.getMaxRetryCount()) {
            stateChange.getMapCounter().put(id, counter);

            if (ControlLoopState.UNINITIALISED2PASSIVE.equals(controlLoop.getState())) {
                LOGGER.debug("retry message ControlLoopUpdate");
                controlLoopUpdatePublisher.send(controlLoop);
            } else {
                LOGGER.debug("retry message ControlLoopStateChange");
                controlLoopStateChangePublisher.send(controlLoop);
            }
        } else {
            LOGGER.debug("report ControlLoop fault");
            stateChange.setFault(id);
        }
    }
}
