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

import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.ws.rs.core.Response;
import lombok.Getter;
import org.onap.policy.clamp.controlloop.common.handler.ControlLoopHandler;
import org.onap.policy.clamp.controlloop.participant.intermediary.parameters.ParticipantIntermediaryParameters;
import org.onap.policy.clamp.controlloop.participant.simulator.main.parameters.ParticipantSimulatorParameters;
import org.onap.policy.clamp.controlloop.participant.simulator.simulation.rest.SimulationElementController;
import org.onap.policy.clamp.controlloop.participant.simulator.simulation.rest.SimulationParticipantController;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.common.endpoints.listeners.MessageTypeDispatcher;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelRuntimeException;

/**
 * This class handles simulation of participants and control loop elements.
 *
 * <p/>It is effectively a singleton that is started at system start.
 */
public class SimulationHandler extends ControlLoopHandler {
    private final ParticipantIntermediaryParameters participantParameters;

    @Getter
    private SimulationProvider simulationProvider;

    /**
     * Create a handler.
     *
     * @param parameters the parameters for access to the database
     */
    public SimulationHandler(ParticipantSimulatorParameters parameters) {
        super(parameters.getDatabaseProviderParameters());
        participantParameters = parameters.getIntermediaryParameters();
    }

    public static SimulationHandler getInstance() {
        return Registry.get(SimulationHandler.class.getName());
    }

    @Override
    public Set<Class<?>> getProviderClasses() {
        return Set.of(SimulationElementController.class, SimulationParticipantController.class);
    }

    @Override
    public void startProviders() {
        simulationProvider = new SimulationProvider(participantParameters);
    }

    @Override
    public void stopProviders() {
        try {
            simulationProvider.close();
        } catch (IOException e) {
            throw new PfModelRuntimeException(Response.Status.INTERNAL_SERVER_ERROR, "Stop providers failed ", e);
        }
    }
}
