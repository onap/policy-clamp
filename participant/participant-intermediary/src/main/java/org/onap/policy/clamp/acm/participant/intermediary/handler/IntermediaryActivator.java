/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021,2024 Nordix Foundation.
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
import java.util.Timer;
import java.util.TimerTask;
import lombok.Getter;
import org.onap.policy.clamp.acm.participant.intermediary.parameters.ParticipantParameters;
import org.onap.policy.clamp.acm.participant.intermediary.parameters.Topics;
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
    private final List<TopicSink> topicSinks;
    private final List<TopicSource> topicSources;

    private final ParticipantHandler participantHandler;

    @Getter
    private final MessageTypeDispatcher msgDispatcher;

    @Getter
    private final MessageTypeDispatcher syncMsgDispatcher;

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

        syncMsgDispatcher = new MessageTypeDispatcher(MSG_TYPE_NAMES);

        // @formatter:off
        addAction("Topic endpoint management",
            () -> TopicEndpointManager.getManager().start(),
            () -> TopicEndpointManager.getManager().shutdown());

        listeners.stream().filter(Listener::isDefaultTopic)
                .forEach(listener -> addAction("Listener " + listener.getClass().getSimpleName(),
                        () -> msgDispatcher.register(listener.getType(), listener.getScoListener()),
                        () -> msgDispatcher.unregister(listener.getType())));

        listeners.stream().filter(l -> ! l.isDefaultTopic())
                .forEach(listener -> addAction("Listener " + listener.getClass().getSimpleName(),
                        () -> syncMsgDispatcher.register(listener.getType(), listener.getScoListener()),
                        () -> syncMsgDispatcher.unregister(listener.getType())));

        publishers.forEach(publisher ->
            addAction("Publisher " + publisher.getClass().getSimpleName(),
                () -> publisher.active(topicSinks),
                publisher::stop));

        var topics = parameters.getIntermediaryParameters().getTopics();

        addAction("Topic Message Dispatcher", () -> this.registerMsgDispatcher(topics),
                () -> this.unregisterMsgDispatcher(topics));
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
            var task = new TimerTask() {
                @Override
                public void run() {
                    new Thread(participantHandler::sendParticipantRegister).start();
                }
            };
            new Timer().schedule(task, 5000);
        }
    }

    /**
     * Handle ContextClosedEvent.
     *
     * @param ctxClosedEvent ContextClosedEvent
     */
    @EventListener
    public void handleContextClosedEvent(ContextClosedEvent ctxClosedEvent) {
        if (isAlive()) {
            sendParticipantDeregister();
            stop();
        }
    }

    private void sendParticipantDeregister() {
        participantHandler.sendParticipantDeregister();
    }

    /**
     * Registers the dispatcher with the topic source(s).
     */
    private void registerMsgDispatcher(Topics topics) {
        for (final var source : topicSources) {
            if (source.getTopic().equals(topics.getOperationTopic())) {
                source.register(msgDispatcher);
            } else if (source.getTopic().equals(topics.getSyncTopic())) {
                source.register(syncMsgDispatcher);
            }
        }
    }

    /**
     * Unregisters the dispatcher from the topic source(s).
     */
    private void unregisterMsgDispatcher(Topics topics) {
        for (final var source : topicSources) {
            if (source.getTopic().equals(topics.getOperationTopic())) {
                source.unregister(msgDispatcher);
            } else if (source.getTopic().equals(topics.getSyncTopic())) {
                source.unregister(syncMsgDispatcher);
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (isAlive()) {
            super.shutdown();
        }
    }
}
