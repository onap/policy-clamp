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

package org.onap.policy.clamp.controlloop.participant.simulator.main.startstop;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import org.onap.policy.clamp.controlloop.participant.simulator.main.parameters.ParticipantSimulatorParameters;
import org.onap.policy.clamp.controlloop.participant.simulator.main.rest.ParticipantSimulatorAafFilter;
import org.onap.policy.clamp.controlloop.participant.simulator.simulation.SimulationHandler;
import org.onap.policy.common.endpoints.http.server.RestServer;
import org.onap.policy.common.utils.services.ServiceManagerContainer;

/**
 * This class activates the participant simulator component as a complete service together with all its controllers,
 * listeners and handlers.
 */
public class ParticipantSimulatorActivator extends ServiceManagerContainer {
    @Getter
    private final ParticipantSimulatorParameters parameters;

    /**
     * Instantiate the activator for the simulator as a complete service.
     *
     * @param parameters the parameters for the participant service
     */
    public ParticipantSimulatorActivator(final ParticipantSimulatorParameters parameters) {
        this.parameters = parameters;

        final AtomicReference<SimulationHandler> simulationHandler = new AtomicReference<>();
        final AtomicReference<RestServer> restServer = new AtomicReference<>();

        // @formatter:off
        addAction("Simulation Handler",
            () -> simulationHandler.set(new SimulationHandler(parameters)),
            () -> simulationHandler.get().close());

        addAction("Simulation Providers",
            () -> simulationHandler.get().startProviders(),
            () -> simulationHandler.get().stopProviders());

        parameters.getRestServerParameters().setName(parameters.getName());

        addAction("REST server",
            () -> {
                Set<Class<?>> providerClasses = simulationHandler.get().getProviderClasses();

                RestServer server = new RestServer(parameters.getRestServerParameters(),
                        ParticipantSimulatorAafFilter.class,
                        providerClasses.toArray(new Class<?>[providerClasses.size()]));
                restServer.set(server);
                restServer.get().start();
            },
            () -> restServer.get().stop());
        // @formatter:on
    }
}
