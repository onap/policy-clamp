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

package org.onap.policy.clamp.controlloop.participant.intermediary.handler;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.clamp.controlloop.participant.intermediary.comm.ControlLoopStateChangeListener;
import org.onap.policy.clamp.controlloop.participant.intermediary.comm.ControlLoopUpdateListener;
import org.onap.policy.clamp.controlloop.participant.intermediary.comm.ParticipantHealthCheckListener;
import org.onap.policy.clamp.controlloop.participant.intermediary.comm.ParticipantStateChangeListener;
import org.onap.policy.clamp.controlloop.participant.intermediary.parameters.ParticipantParameters;
import org.onap.policy.common.endpoints.event.comm.TopicEndpointManager;
import org.onap.policy.common.endpoints.event.comm.TopicSource;
import org.onap.policy.common.endpoints.listeners.MessageTypeDispatcher;
import org.onap.policy.common.utils.services.ServiceManagerContainer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * This class activates the Participant Intermediary together with all its handlers.
 */
@Component
public class IntermediaryActivator extends ServiceManagerContainer implements Closeable {

    private final ApplicationContext applicationContext;

    // Topics from which the participant receives and to which the participant sends messages
    private List<TopicSource> topicSources;

    /**
     * Instantiate the activator for participant.
     *
     * @param applicationContext ApplicationContext
     * @param parameters the ParticipantParameters
     */
    public IntermediaryActivator(final ApplicationContext applicationContext, final ParticipantParameters parameters) {
        this.applicationContext = applicationContext;

        topicSources = TopicEndpointManager.getManager()
                .addTopicSources(parameters.getIntermediaryParameters().getClampControlLoopTopics().getTopicSources());

        // @formatter:off

        addAction("Topic endpoint management",
            () -> TopicEndpointManager.getManager().start(),
            () -> TopicEndpointManager.getManager().shutdown());

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
            stop();
        }
    }

    /**
     * Registers the dispatcher with the topic source(s).
     */
    private void registerMsgDispatcher() {
        MessageTypeDispatcher msgDispatcher = applicationContext.getBean(MessageTypeDispatcher.class);

        msgDispatcher.register(ParticipantMessageType.PARTICIPANT_STATE_CHANGE.name(),
                applicationContext.getBean(ParticipantStateChangeListener.class));

        msgDispatcher.register(ParticipantMessageType.PARTICIPANT_HEALTH_CHECK.name(),
                applicationContext.getBean(ParticipantHealthCheckListener.class));

        msgDispatcher.register(ParticipantMessageType.PARTICIPANT_CONTROL_LOOP_STATE_CHANGE.name(),
                applicationContext.getBean(ControlLoopStateChangeListener.class));

        msgDispatcher.register(ParticipantMessageType.PARTICIPANT_CONTROL_LOOP_UPDATE.name(),
                applicationContext.getBean(ControlLoopUpdateListener.class));

        for (final TopicSource source : topicSources) {
            source.register(msgDispatcher);
        }
    }

    /**
     * Unregisters the dispatcher from the topic source(s).
     */
    private void unregisterMsgDispatcher() {
        MessageTypeDispatcher msgDispatcher = applicationContext.getBean(MessageTypeDispatcher.class);

        for (final TopicSource source : topicSources) {
            source.unregister(msgDispatcher);
        }
    }

    @Override
    public void close() throws IOException {
        super.shutdown();
    }
}
