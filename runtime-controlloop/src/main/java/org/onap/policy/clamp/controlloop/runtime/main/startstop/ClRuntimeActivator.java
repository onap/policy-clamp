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

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import javax.ws.rs.core.Response.Status;
import lombok.Getter;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopRuntimeException;
import org.onap.policy.clamp.controlloop.runtime.main.parameters.ClRuntimeParameterGroup;
import org.onap.policy.clamp.controlloop.runtime.supervision.SupervisionHandler;
import org.onap.policy.common.endpoints.event.comm.TopicEndpointManager;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.common.endpoints.event.comm.TopicSource;
import org.onap.policy.common.endpoints.listeners.MessageTypeDispatcher;
import org.onap.policy.common.utils.services.ServiceManagerContainer;

/**
 * This class activates the control loop runtime component as a complete service together with all its controllers,
 * listeners & handlers.
 */
public class ClRuntimeActivator extends ServiceManagerContainer implements Closeable {
    // Name of the message type for messages on topics
    private static final String[] MSG_TYPE_NAMES = {"messageType"};

    @Getter
    private final ClRuntimeParameterGroup parameterGroup;

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
     * @param supervisionHandler SupervisionHandler
     * @throws ControlLoopRuntimeException if the activator does not start
     */
    public ClRuntimeActivator(final ClRuntimeParameterGroup clRuntimeParameterGroup,
            SupervisionHandler supervisionHandler) {
        this.parameterGroup = clRuntimeParameterGroup;

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

        // @formatter:off
        addAction("Topic endpoint management",
            () -> TopicEndpointManager.getManager().start(),
            () -> TopicEndpointManager.getManager().shutdown());

        addAction("Supervision Providers", () -> supervisionHandler.startProviders(),
                () -> supervisionHandler.stopProviders());
        addAction("Supervision Listeners", () -> supervisionHandler.startAndRegisterListeners(msgDispatcher),
                () -> supervisionHandler.stopAndUnregisterListeners(msgDispatcher));
        addAction("Supervision Publishers", () -> supervisionHandler.startAndRegisterPublishers(topicSinks),
                () -> supervisionHandler.stopAndUnregisterPublishers());

        addAction("Topic Message Dispatcher", this::registerMsgDispatcher, this::unregisterMsgDispatcher);
        // @formatter:on
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

    @Override
    public void close() throws IOException {
        if (isAlive()) {
            stop();
        }
    }
}
