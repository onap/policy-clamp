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

import java.io.Closeable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ControlLoopProvider;
import org.onap.policy.clamp.controlloop.runtime.main.parameters.ClRuntimeParameterGroup;
import org.onap.policy.models.base.PfModelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class is used to scan the control loops in the database and check if they are in the correct state.
 */
@Component
public class SupervisionScanner implements Runnable, Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(SupervisionScanner.class);

    private ControlLoopProvider controlLoopProvider;
    private ScheduledExecutorService timerPool;

    /**
     * Constructor for instantiating SupervisionScanner.
     *
     * @param clRuntimeParameterGroup the parameters for the control loop runtime
     * @param controlLoopProvider the provider to use to read control loops from the database
     */
    public SupervisionScanner(final ControlLoopProvider controlLoopProvider,
            ClRuntimeParameterGroup clRuntimeParameterGroup) {
        this.controlLoopProvider = controlLoopProvider;

        // Kick off the timer
        timerPool = makeTimerPool();
        timerPool.scheduleAtFixedRate(this, 0, clRuntimeParameterGroup.getSupervisionScannerIntervalSec(),
                TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        LOGGER.debug("Scanning control loops in the database . . .");

        try {
            for (ControlLoop controlLoop : controlLoopProvider.getControlLoops(null, null)) {
                scanControlLoop(controlLoop);
            }
        } catch (PfModelException pfme) {
            LOGGER.warn("error reading control loops from database", pfme);
        }

        LOGGER.debug("Control loop scan complete . . .");
    }

    @Override
    public void close() {
        timerPool.shutdown();
    }

    private void scanControlLoop(final ControlLoop controlLoop) throws PfModelException {
        LOGGER.debug("scanning control loop {} . . .", controlLoop.getKey().asIdentifier());

        if (controlLoop.getState().equals(controlLoop.getOrderedState().asState())) {
            LOGGER.debug("control loop {} scanned, OK", controlLoop.getKey().asIdentifier());
            return;
        }

        for (ControlLoopElement element : controlLoop.getElements().values()) {
            if (!element.getState().equals(element.getOrderedState().asState())) {
                LOGGER.debug("control loop scan: transitioning from state {} to {}", controlLoop.getState(),
                        controlLoop.getOrderedState());
                return;
            }
        }

        LOGGER.debug("control loop scan: transition from state {} to {} completed", controlLoop.getState(),
                controlLoop.getOrderedState());

        controlLoop.setState(controlLoop.getOrderedState().asState());
        controlLoopProvider.updateControlLoop(controlLoop);
    }

    /**
     * Makes a new timer pool.
     *
     * @return a new timer pool
     */
    protected ScheduledExecutorService makeTimerPool() {
        return Executors.newScheduledThreadPool(1);
    }
}
