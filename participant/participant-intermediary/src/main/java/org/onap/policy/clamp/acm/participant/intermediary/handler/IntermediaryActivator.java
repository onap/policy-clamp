/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation.
 *  Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.clamp.acm.participant.intermediary.handler;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import lombok.Getter;
import org.onap.policy.clamp.acm.participant.intermediary.parameters.ParticipantParameters;
import org.onap.policy.common.endpoints.event.comm.TopicEndpointManager;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.common.endpoints.event.comm.TopicSource;
import org.onap.policy.common.endpoints.listeners.MessageTypeDispatcher;
import org.onap.policy.common.utils.services.ServiceManagerContainer;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * This class activates the Participant Intermediary together with all its handlers.
 */
@Component
public class IntermediaryActivator extends ServiceManagerContainer implements Closeable {

    private static final String[] MSG_TYPE_NAMES = {"messageType"};

    // Topics from which the participant receives and to which the participant sends messages
    private List<TopicSink> topicSinks;
    private List<TopicSource> topicSources;

    private ParticipantHandler participantHandler;

    @Getter
    private final MessageTypeDispatcher msgDispatcher;

    /**
     * Instantiate the activator for participant.
     *
     * @param parameters the ParticipantParameters
     * @param participantHandler the ParticipantHandler
     * @param publishers list of Publishers
     * @param listeners list of Listeners
     */
    public <T> IntermediaryActivator(final ParticipantParameters parameters, ParticipantHandler participantHandler,
            List<Publisher> publishers, List<Listener<T>> listeners) {
        this.participantHandler = participantHandler;

        topicSinks = TopicEndpointManager.getManager().addTopicSinks(
                parameters.getIntermediaryParameters().getClampAutomationCompositionTopics().getTopicSinks());

        topicSources = TopicEndpointManager.getManager().addTopicSources(
                parameters.getIntermediaryParameters().getClampAutomationCompositionTopics().getTopicSources());

        msgDispatcher = new MessageTypeDispatcher(MSG_TYPE_NAMES);

        // @formatter:off
        addAction("Topic endpoint management",
            () -> TopicEndpointManager.getManager().start(),
            () -> TopicEndpointManager.getManager().shutdown());

        publishers.forEach(publisher ->
            addAction("Publisher " + publisher.getClass().getSimpleName(),
                () -> publisher.active(topicSinks),
                publisher::stop));

        listeners.forEach(listener ->
            addAction("Listener " + listener.getClass().getSimpleName(),
                () -> msgDispatcher.register(listener.getType(), listener.getScoListener()),
                () -> msgDispatcher.unregister(listener.getType())));

        addAction("Topic Message Dispatcher", this::registerMsgDispatcher, this::unregisterMsgDispatcher);
        // @formatter:on
    }

    /**
     * Handle ContextRefreshEvent.
     *
     * @param ctxRefreshedEvent ContextRefreshedEvent
     */
    @EventListener
    public void handleContextRefreshEvent(ContextRefreshedEvent ctxRefreshedEvent) {
        if (!isAlive()) {
            start();
            sendParticipantRegister();
        }
    }

    /**
     * Handle ContextClosedEvent.
     *
     * @param ctxClosedEvent ContextClosedEvent
     * @throws InterruptedException if execution has been interrupted
     */
    @EventListener
    public void handleContextClosedEvent(ContextClosedEvent ctxClosedEvent) throws InterruptedException {
        if (isAlive()) {
            sendParticipantDeregister();
            Thread.sleep(1000L);
            stop();
        }
    }

    private void sendParticipantRegister() {
        participantHandler.sendParticipantRegister();
    }

    private void sendParticipantDeregister() {
        participantHandler.sendParticipantDeregister();
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
        }
    }
}
