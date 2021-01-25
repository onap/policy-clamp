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

package org.onap.policy.clamp.controlloop.common.handler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NonNull;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.common.endpoints.listeners.MessageTypeDispatcher;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.provider.PolicyModelsProviderParameters;

/**
 * Abstract class for handlers for sub components in the control loop system
 *
 * <p>Instances are effectively singletons that are started at system start.
 */
public abstract class ControlLoopHandler {
    @Getter
    private final PolicyModelsProviderParameters databaseProviderParameters;

    /**
     * Create a handler.
     *
     * @param databaseProviderParameters the parameters for access to the database
     */
    protected ControlLoopHandler(@NonNull PolicyModelsProviderParameters databaseProviderParameters) {
        this.databaseProviderParameters = databaseProviderParameters;

        Registry.register(this.getClass().getName(), this);
    }

    public void close() {
        Registry.unregister(this.getClass().getName());
    }

    /**
     * Get the provider classes that are used in instantiation.
     *
     * @return the provider classes
     */
    public Set<Class<?>> getProviderClasses() {
        // No REST interfaces are the default
        return new HashSet<>();
    }

    /**
     * Start any topic message listeners for this handler.
     *
     * @param msgDispatcher the message dispatcher with which to register the listener
     */
    public abstract void startAndRegisterListeners(MessageTypeDispatcher msgDispatcher);

    /**
     * Start any topic message publishers for this handler.
     *
     * @param topicSinks the topic sinks on which the publisher can publish
     */
    public abstract void startAndRegisterPublishers(List<TopicSink> topicSinks);

    /**
     * Stop any topic message publishers for this handler.
     */
    public abstract void stopAndUnregisterPublishers();

    /**
     * Stop any topic message listeners for this handler.
     *
     * @param msgDispatcher the message dispatcher from which to unregister the listener
     */
    public abstract void stopAndUnregisterListeners(MessageTypeDispatcher msgDispatcher);
}
