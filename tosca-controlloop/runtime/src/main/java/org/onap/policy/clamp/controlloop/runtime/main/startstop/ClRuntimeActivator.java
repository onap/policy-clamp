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

package org.onap.policy.clamp.controlloop.runtime.main.startstop;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.core.Response.Status;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopRuntimeException;
import org.onap.policy.clamp.controlloop.common.handler.ControlLoopHandler;
import org.onap.policy.clamp.controlloop.runtime.commissioning.CommissioningHandler;
import org.onap.policy.clamp.controlloop.runtime.instantiation.InstantiationHandler;
import org.onap.policy.clamp.controlloop.runtime.main.parameters.ClRuntimeParameterGroup;
import org.onap.policy.clamp.controlloop.runtime.main.rest.ControlLoopAafFilter;
import org.onap.policy.common.endpoints.event.comm.TopicEndpointManager;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.common.endpoints.event.comm.TopicSource;
import org.onap.policy.common.endpoints.http.server.RestServer;
import org.onap.policy.common.endpoints.listeners.MessageTypeDispatcher;
import org.onap.policy.common.parameters.ParameterService;
import org.onap.policy.common.utils.services.ServiceManagerContainer;

/**
 * This class activates the control loop runtime component as a complete service together with all its controllers,
 * listeners & handlers.
 */
public class ClRuntimeActivator extends ServiceManagerContainer {
    // Name of the message type for messages on topics
    private static final String[] MSG_TYPE_NAMES = {"messageType"};

    private final ClRuntimeParameterGroup clRuntimeParameterGroup;

    // Topics from which the application receives and to which the application sends messages
    private List<TopicSink> topicSinks;
    private List<TopicSource> topicSources;

    /**
     * Listens for messages on the topic, decodes them into a message, and then dispatches them.
     */
    private final MessageTypeDispatcher msgDispatcher;

    /**
     * Instantiate the activator for the control loop runtime as a complete service.
     *
     * @param clRuntimeParameterGroup the parameters for the control loop runtime service
     */
    public ClRuntimeActivator(final ClRuntimeParameterGroup clRuntimeParameterGroup) {
        if (clRuntimeParameterGroup == null || !clRuntimeParameterGroup.isValid()) {
            throw new ControlLoopRuntimeException(Status.INTERNAL_SERVER_ERROR, "ParameterGroup not valid");
        }

        this.clRuntimeParameterGroup = clRuntimeParameterGroup;

        topicSinks = TopicEndpointManager.getManager()
                .addTopicSinks(clRuntimeParameterGroup.getTopicParameterGroup().getTopicSinks());

        topicSources = TopicEndpointManager.getManager()
                .addTopicSources(clRuntimeParameterGroup.getTopicParameterGroup().getTopicSources());

        try {
            msgDispatcher = new MessageTypeDispatcher(MSG_TYPE_NAMES);
        } catch (final RuntimeException e) {
            throw new ControlLoopRuntimeException(Status.INTERNAL_SERVER_ERROR,
                    "topic message dispatcher failed to start", e);
        }

        final AtomicReference<ControlLoopHandler> commissioningHandler = new AtomicReference<>();
        final AtomicReference<ControlLoopHandler> instantiationHandler = new AtomicReference<>();

        final AtomicReference<RestServer> restServer = new AtomicReference<>();
        // @formatter:off
        addAction("Control loop runtime parameters",
                () -> ParameterService.register(clRuntimeParameterGroup),
                () -> ParameterService.deregister(clRuntimeParameterGroup.getName()));
        addAction("Topic endpoint management",
                () -> TopicEndpointManager.getManager().start(),
                () -> TopicEndpointManager.getManager().shutdown());
        addAction("Commisssioning Handler",
                () -> commissioningHandler.set(new CommissioningHandler(clRuntimeParameterGroup)),
                () -> commissioningHandler.get().close());
        addAction("Instantiation Handler",
                () -> instantiationHandler.set(new InstantiationHandler(clRuntimeParameterGroup)),
                () -> instantiationHandler.get().close());

        addHandlerActions("Commisssioning", commissioningHandler);
        addHandlerActions("Instantiation", instantiationHandler);

        addAction("Topic Message Dispatcher", this::registerMsgDispatcher, this::unregisterMsgDispatcher);

        clRuntimeParameterGroup.getRestServerParameters().setName(clRuntimeParameterGroup.getName());

        addAction("REST server",
                () -> {
                    Set<Class<?>> providerClasses = commissioningHandler.get().getProviderClasses();
                    providerClasses.addAll(instantiationHandler.get().getProviderClasses());

                    RestServer server = new RestServer(clRuntimeParameterGroup.getRestServerParameters(),
                            ControlLoopAafFilter.class,
                            providerClasses.toArray(new Class<?>[providerClasses.size()]));
                    restServer.set(server);
                    restServer.get().start();
                },
                () -> restServer.get().stop());
        // @formatter:on
    }

    private void addHandlerActions(final String name, final AtomicReference<ControlLoopHandler> handler) {
        addAction(name + " Providers",
                () -> handler.get().startProviders(),
                () -> handler.get().stopProviders());

        addAction(name + " Listeners",
                () -> handler.get().startAndRegisterListeners(msgDispatcher),
                () -> handler.get().stopAndUnregisterListeners(msgDispatcher));

        addAction(name + " Publishers",
                () -> handler.get().startAndRegisterPublishers(topicSinks),
                () -> handler.get().stopAndUnregisterPublishers());
    }

    /**
     * Registers the dispatcher with the topic source(s).
     */
    private void registerMsgDispatcher() {
        for (final TopicSource source : topicSources) {
            source.register(msgDispatcher);
        }
    }

    /**
     * Unregisters the dispatcher from the topic source(s).
     */
    private void unregisterMsgDispatcher() {
        for (final TopicSource source : topicSources) {
            source.unregister(msgDispatcher);
        }
    }

    /**
     * Get the parameters used by the activator.
     *
     * @return the parameters of the activator
     */
    public ClRuntimeParameterGroup getParameterGroup() {
        return clRuntimeParameterGroup;
    }
}
