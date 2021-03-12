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

package org.onap.policy.clamp.controlloop.participant.simulator.simulation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.onap.policy.clamp.controlloop.common.handler.ControlLoopHandler;
import org.onap.policy.clamp.controlloop.participant.simulator.main.parameters.ParticipantSimulatorParameters;
import org.onap.policy.clamp.controlloop.participant.simulator.simulation.rest.SimulationQueryElementController;
import org.onap.policy.clamp.controlloop.participant.simulator.simulation.rest.SimulationQueryParticipantController;
import org.onap.policy.clamp.controlloop.participant.simulator.simulation.rest.SimulationUpdateElementController;
import org.onap.policy.clamp.controlloop.participant.simulator.simulation.rest.SimulationUpdateParticipantController;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.common.endpoints.listeners.MessageTypeDispatcher;

/**
 * This class handles simulation of participants and control loop elements.
 *
 * <p/>It is effectively a singleton that is started at system start.
 */
public class SimulationHandler extends ControlLoopHandler {
    /**
     * Create a handler.
     *
     * @param parameters the parameters for access to the database
     */
    public SimulationHandler(ParticipantSimulatorParameters parameters) {
        super(parameters.getDatabaseProviderParameters());
    }

    @Override
    public Set<Class<?>> getProviderClasses() {
        Set<Class<?>> providerClasses = new HashSet<>();

        providerClasses.add(SimulationQueryElementController.class);
        providerClasses.add(SimulationQueryParticipantController.class);
        providerClasses.add(SimulationUpdateElementController.class);
        providerClasses.add(SimulationUpdateParticipantController.class);

        return providerClasses;
    }

    @Override
    public void startAndRegisterListeners(MessageTypeDispatcher msgDispatcher) {
        // No topic communication on this handler
    }

    @Override
    public void startAndRegisterPublishers(List<TopicSink> topicSinks) {
        // No topic communication on this handler
    }

    @Override
    public void stopAndUnregisterPublishers() {
        // No topic communication on this handler
    }

    @Override
    public void stopAndUnregisterListeners(MessageTypeDispatcher msgDispatcher) {
        // No topic communication on this handler
    }

    @Override
    public void startProviders() {
        // No providers on this handler
    }

    @Override
    public void stopProviders() {
        // No providers on this handler
    }
}
