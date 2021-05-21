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

package org.onap.policy.clamp.controlloop.runtime.monitoring;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.ws.rs.core.Response;
import lombok.Getter;
import org.onap.policy.clamp.controlloop.common.handler.ControlLoopHandler;
import org.onap.policy.clamp.controlloop.runtime.main.parameters.ClRuntimeParameterGroup;
import org.onap.policy.clamp.controlloop.runtime.monitoring.rest.MonitoringQueryController;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.common.endpoints.listeners.MessageTypeDispatcher;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelRuntimeException;

/**
 * This class handles monitoring of control loop definitions,
 * so only one object of this type should be built at a time.
 *
 * <p/>
 * It is effectively a singleton that is started at system start.
 */
public class MonitoringHandler extends ControlLoopHandler {

    @Getter
    private MonitoringProvider monitoringProvider;

    /**
     * Gets the Monitoring Handler.
     *
     * @return MonitoringHandler
     */
    public static MonitoringHandler getInstance() {
        return Registry.get(MonitoringHandler.class.getName());
    }

    /**
     * Create a handler.
     *
     * @param controlLoopParameters the parameters for access to the database
     */
    public MonitoringHandler(ClRuntimeParameterGroup controlLoopParameters) {
        super(controlLoopParameters.getDatabaseProviderParameters());
    }

    @Override
    public Set<Class<?>> getProviderClasses() {
        return Set.of(MonitoringQueryController.class);
    }

    @Override
    public void startProviders() {
        monitoringProvider = new MonitoringProvider(getDatabaseProviderParameters());
    }

    @Override
    public void stopProviders() {
        try {
            monitoringProvider.close();
        } catch (IOException e) {
            throw new PfModelRuntimeException(Response.Status.INTERNAL_SERVER_ERROR, "Cannot stop provider", e);
        }
    }
}
