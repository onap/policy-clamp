/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Nordix Foundation.
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

package org.onap.policy.clamp.acm.element.handler;

import java.io.IOException;
import java.util.List;
import org.onap.policy.clamp.models.acm.messages.dmaap.element.ElementMessageType;
import org.onap.policy.common.endpoints.event.comm.TopicEndpointManager;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.common.endpoints.event.comm.TopicSource;
import org.onap.policy.common.endpoints.listeners.MessageTypeDispatcher;
import org.onap.policy.common.endpoints.parameters.TopicParameterGroup;
import org.onap.policy.common.utils.services.ServiceManagerContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * This class activates the Kafka together with all its handlers.
 */
@Component
public class MessageActivator extends ServiceManagerContainer implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageActivator.class);

    private static final String[] MSG_TYPE_NAMES = { "messageType" };

    // Topics from which the AC element receives and sends messages
    private List<TopicSink> topicSinks;
    private List<TopicSource> topicSources;

    private final MessageListener listener;
    private final MessagePublisher publisher;

    private MessageTypeDispatcher msgDispatcher;

    /**
     * Constructor.
     *
     * @param listener  MessageListener
     * @param publisher MessagePublisher
     */
    public MessageActivator(MessageListener listener, MessagePublisher publisher) {
        super();
        this.listener = listener;
        this.publisher = publisher;
        msgDispatcher = new MessageTypeDispatcher(MSG_TYPE_NAMES);
    }

    /**
     * Activate publisher and listener messages.
     *
     * @param parameters TopicParameterGroup
     */
    public void activate(final TopicParameterGroup parameters) {
        topicSinks = TopicEndpointManager.getManager().addTopicSinks(parameters.getTopicSinks());
        topicSources = TopicEndpointManager.getManager().addTopicSources(parameters.getTopicSources());

        // @formatter:off
        addAction("Topic endpoint management",
            () -> TopicEndpointManager.getManager().start(),
            () -> TopicEndpointManager.getManager().shutdown());

        addAction("Message Publisher",
            () -> publisher.active(topicSinks), publisher::stop);


        addAction("Message Listener",
            () -> msgDispatcher.register(ElementMessageType.STATUS.name(), listener),
            () -> msgDispatcher.unregister(ElementMessageType.STATUS.name()));

        addAction("Topic Message Dispatcher", this::registerMsgDispatcher, this::unregisterMsgDispatcher);
        // @formatter:on

        start();
        LOGGER.info("Kafka configuration initialised successfully");
    }

    /**
     * Handle ContextClosedEvent.
     *
     * @param ctxClosedEvent ContextClosedEvent
     */
    @EventListener
    public void handleContextClosedEvent(ContextClosedEvent ctxClosedEvent) {
        deactivate();
    }

    /**
     * Deactivate publisher and listener messages.
     */
    public void deactivate() {
        if (isAlive()) {
            stop();
        }
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
            super.shutdown();
            LOGGER.info("Kafka configuration is uninitialised.");
        }
    }
}
