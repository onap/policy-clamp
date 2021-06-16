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

package org.onap.policy.clamp.controlloop.runtime.instantiation;

import java.io.IOException;
import java.util.Set;
import javax.ws.rs.core.Response;
import lombok.Getter;
import org.onap.policy.clamp.controlloop.common.handler.ControlLoopHandler;
import org.onap.policy.clamp.controlloop.runtime.instantiation.rest.InstantiationController;
import org.onap.policy.clamp.controlloop.runtime.main.parameters.ClRuntimeParameterGroup;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelRuntimeException;

/**
 * This class handles instantiation of control loop instances.
 *
 * <p/>It is effectively a singleton that is started at system start
 */
public final class InstantiationHandler extends ControlLoopHandler {

    @Getter
    private ControlLoopInstantiationProvider controlLoopInstantiationProvider;

    /**
     * Gets the InstantiationHandler.
     *
     * @return InstantiationHandler
     */
    public static InstantiationHandler getInstance() {
        return Registry.get(InstantiationHandler.class.getName());
    }

    /**
     * Create a handler.
     *
     * @param controlLoopParameters the parameters for access to the database
     */
    public InstantiationHandler(ClRuntimeParameterGroup controlLoopParameters) {
        super(controlLoopParameters.getDatabaseProviderParameters());
    }

    @Override
    public Set<Class<?>> getProviderClasses() {
        return Set.of(InstantiationController.class);
    }

    @Override
    public void startProviders() {
        controlLoopInstantiationProvider = new ControlLoopInstantiationProvider(getDatabaseProviderParameters());
    }

    @Override
    public void stopProviders() {
        try {
            controlLoopInstantiationProvider.close();
        } catch (IOException e) {
            throw new PfModelRuntimeException(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
