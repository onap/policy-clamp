/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021,2024-2025 Nordix Foundation.
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
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.onap.policy.clamp.acm.participant.intermediary.parameters.ParticipantParameters;
import org.onap.policy.clamp.acm.participant.intermediary.parameters.Topics;
import org.onap.policy.common.endpoints.listeners.MessageTypeDispatcher;
import org.onap.policy.common.message.bus.event.TopicEndpointManager;
import org.onap.policy.common.message.bus.event.TopicSink;
import org.onap.policy.common.message.bus.event.TopicSource;
import org.onap.policy.common.utils.services.ServiceManagerContainer;
import org.springframework.stereotype.Component;

/**
 * This class activates the Participant Intermediary together with all its handlers.
 */
@Component
public class IntermediaryActivator extends ServiceManagerContainer implements Closeable {

    private static final String[] MSG_TYPE_NAMES = {"messageType"};

    // Topics from which the participant receives and to which the participant sends messages
    private final List<TopicSink> topicSinks = new ArrayList<>();
    private final List<TopicSource> topicSources = new ArrayList<>();

    @Getter
    private final MessageTypeDispatcher msgDispatcher;

    @Getter
    private final MessageTypeDispatcher syncMsgDispatcher;

    /**
     * Constructor.
     */
    public IntermediaryActivator() {
        msgDispatcher = new MessageTypeDispatcher(MSG_TYPE_NAMES);
        syncMsgDispatcher = new MessageTypeDispatcher(MSG_TYPE_NAMES);
    }

    /**
     * Instantiate the activator for participant.
     *
     * @param parameters the ParticipantParameters
     * @param publishers list of Publishers
     * @param listeners list of Listeners
     */
    public <T> void config(ParticipantParameters parameters,
            List<Publisher> publishers, List<Listener<T>> listeners) {

        topicSinks.addAll(TopicEndpointManager.getManager().addTopicSinks(
                parameters.getIntermediaryParameters().getClampAutomationCompositionTopics().getTopicSinks()));

        topicSources.addAll(TopicEndpointManager.getManager().addTopicSources(
            parameters.getIntermediaryParameters().getClampAutomationCompositionTopics().getTopicSources()));


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
