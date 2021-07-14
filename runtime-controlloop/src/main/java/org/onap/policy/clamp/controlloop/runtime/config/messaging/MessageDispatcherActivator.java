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

package org.onap.policy.clamp.controlloop.runtime.config.messaging;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import javax.ws.rs.core.Response.Status;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopRuntimeException;
import org.onap.policy.clamp.controlloop.runtime.main.parameters.ClRuntimeParameterGroup;
import org.onap.policy.common.endpoints.event.comm.TopicEndpointManager;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.common.endpoints.event.comm.TopicSource;
import org.onap.policy.common.endpoints.listeners.MessageTypeDispatcher;
import org.onap.policy.common.utils.services.ServiceManagerContainer;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class MessageDispatcherActivator extends ServiceManagerContainer implements Closeable {

    private static final String[] MSG_TYPE_NAMES = {"messageType"};

    // Topics from which the application receives and to which the application sends messages
    private List<TopicSink> topicSinks;
    private List<TopicSource> topicSources;

    private final MessageTypeDispatcher msgDispatcher;

    /**
     * Constructor.
     *
     * @param clRuntimeParameterGroup the parameters for the control loop runtime service
     * @param publishers array of Publishers
     * @param listeners array of Listeners
     * @throws ControlLoopRuntimeException if the activator does not start
     */
    public MessageDispatcherActivator(final ClRuntimeParameterGroup clRuntimeParameterGroup, Publisher[] publishers,
            Listener[] listeners) {
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

        Stream.of(publishers).forEach(publisher ->
            addAction("Publishers",
                () -> publisher.active(topicSinks),
                () -> publisher.stop()));

        Stream.of(listeners).forEach(listener ->
            addAction("Listeners",
                    () -> msgDispatcher.register(listener.getName(), listener.getScoListener()),
                    () -> msgDispatcher.unregister(listener.getName())));

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

    /**
     * Start Manager after the application is Started.
     *
     * @param cre Refreshed Event
     */
    @EventListener
    public void handleContextStart(ContextRefreshedEvent cre) {
        if (!isAlive()) {
            start();
        }
    }

    @Override
    public void close() throws IOException {
        if (isAlive()) {
            stop();
        }
    }
}
