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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ws.rs.core.Response;
import lombok.Getter;
import org.onap.policy.clamp.controlloop.common.handler.ControlLoopHandler;
import org.onap.policy.clamp.controlloop.runtime.instantiation.rest.InstantiationCommandController;
import org.onap.policy.clamp.controlloop.runtime.instantiation.rest.InstantiationCreateController;
import org.onap.policy.clamp.controlloop.runtime.instantiation.rest.InstantiationDeleteController;
import org.onap.policy.clamp.controlloop.runtime.instantiation.rest.InstantiationQueryController;
import org.onap.policy.clamp.controlloop.runtime.instantiation.rest.InstantiationUpdateController;
import org.onap.policy.clamp.controlloop.runtime.main.parameters.ClRuntimeParameterGroup;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.common.endpoints.listeners.MessageTypeDispatcher;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelRuntimeException;

/**
 * This class handles instantiation of control loop instances,
 * so only one object of this type should be built at a time.
 *
 * <p>It is effectively a singleton that is started at system start</p>
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
        return (InstantiationHandler) Registry.get(InstantiationHandler.class.getName());
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
        Set<Class<?>> providerClasses = new HashSet<>();

        providerClasses.add(InstantiationCreateController.class);
        providerClasses.add(InstantiationUpdateController.class);
        providerClasses.add(InstantiationQueryController.class);
        providerClasses.add(InstantiationDeleteController.class);
        providerClasses.add(InstantiationCommandController.class);

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
