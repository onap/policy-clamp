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

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.core.Response.Status;
import lombok.Getter;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopRuntimeException;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantControlLoopStateChange;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantControlLoopUpdate;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantHealthCheck;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantStateChange;
import org.onap.policy.clamp.controlloop.participant.intermediary.comm.ControlLoopStateChangeListener;
import org.onap.policy.clamp.controlloop.participant.intermediary.comm.ControlLoopUpdateListener;
import org.onap.policy.clamp.controlloop.participant.intermediary.comm.ParticipantHealthCheckListener;
import org.onap.policy.clamp.controlloop.participant.intermediary.comm.ParticipantStateChangeListener;
import org.onap.policy.clamp.controlloop.participant.intermediary.comm.ParticipantStatusPublisher;
import org.onap.policy.clamp.controlloop.participant.intermediary.parameters.ParticipantIntermediaryParameters;
import org.onap.policy.common.endpoints.event.comm.TopicEndpointManager;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.common.endpoints.event.comm.TopicSource;
import org.onap.policy.common.endpoints.listeners.MessageTypeDispatcher;
import org.onap.policy.common.endpoints.listeners.ScoListener;
import org.onap.policy.common.utils.services.ServiceManagerContainer;

/**
 * This class activates the Participant Intermediary together with all its handlers.
 */
public class IntermediaryActivator extends ServiceManagerContainer {
    // Name of the message type for messages on topics
    private static final String[] MSG_TYPE_NAMES = {"messageType"};

    @Getter
    private final ParticipantIntermediaryParameters parameters;

    // Topics from which the participant receives and to which the participant sends messages
    private List<TopicSink> topicSinks;
    private List<TopicSource> topicSources;

    // The participant handler for this intermediary
    final AtomicReference<ParticipantHandler> participantHandler = new AtomicReference<>();

    /**
     * Listens for messages on the topic, decodes them into a message, and then dispatches them.
     */
    private final MessageTypeDispatcher msgDispatcher;

    /**
     * Instantiate the activator for participant.
     *
     * @param parameters the parameters for the participant intermediary
     * @throws ControlLoopRuntimeException when the activation fails
     */
    public IntermediaryActivator(final ParticipantIntermediaryParameters parameters) {
        this.parameters = parameters;

        topicSinks =
            TopicEndpointManager.getManager().addTopicSinks(parameters.getClampControlLoopTopics().getTopicSinks());

        topicSources =
            TopicEndpointManager.getManager().addTopicSources(parameters.getClampControlLoopTopics().getTopicSources());

        try {
            this.msgDispatcher = new MessageTypeDispatcher(MSG_TYPE_NAMES);
        } catch (final RuntimeException e) {
            throw new ControlLoopRuntimeException(Status.INTERNAL_SERVER_ERROR,
                "topic message dispatcher failed to start", e);
        }

        // @formatter:off
        final AtomicReference<ParticipantStatusPublisher>     statusPublisher                = new AtomicReference<>();
        final AtomicReference<ParticipantStateChangeListener> participantStateChangeListener = new AtomicReference<>();
        final AtomicReference<ParticipantHealthCheckListener> participantHealthCheckListener = new AtomicReference<>();
        final AtomicReference<ControlLoopStateChangeListener> controlLoopStateChangeListener = new AtomicReference<>();
        final AtomicReference<ControlLoopUpdateListener>      controlLoopUpdateListener      = new AtomicReference<>();

        addAction("Topic endpoint management",
            () -> TopicEndpointManager.getManager().start(),
            () -> TopicEndpointManager.getManager().shutdown());

        addAction("Participant Status Publisher",
            () -> statusPublisher.set(new ParticipantStatusPublisher(topicSinks)),
            () -> statusPublisher.get().close());

        addAction("Participant Handler",
            () -> participantHandler.set(new ParticipantHandler(parameters, statusPublisher.get())),
            () -> participantHandler.get().close());

        addAction("Participant State Change Listener",
            () -> participantStateChangeListener.set(new ParticipantStateChangeListener(participantHandler.get())),
            () -> participantStateChangeListener.get().close());

        addAction("Participant Health Check Listener",
            () -> participantHealthCheckListener.set(new ParticipantHealthCheckListener(participantHandler.get())),
            () -> participantHealthCheckListener.get().close());

        addAction("Control Loop State Change Listener",
            () -> controlLoopStateChangeListener.set(new ControlLoopStateChangeListener(participantHandler.get())),
            () -> controlLoopStateChangeListener.get().close());

        addAction("Control Loop Update Listener",
            () -> controlLoopUpdateListener.set(new ControlLoopUpdateListener(participantHandler.get())),
            () -> controlLoopUpdateListener.get().close());

        addAction("Topic Message Dispatcher", this::registerMsgDispatcher, this::unregisterMsgDispatcher);
        // @formatter:on
    }

    /**
     * Registers the dispatcher with the topic source(s).
     */
    private void registerMsgDispatcher() {
        msgDispatcher.register(ParticipantMessageType.PARTICIPANT_STATE_CHANGE.name(),
            (ScoListener<ParticipantStateChange>) new ParticipantStateChangeListener(participantHandler.get()));
        msgDispatcher.register(ParticipantMessageType.PARTICIPANT_HEALTH_CHECK.name(),
            (ScoListener<ParticipantHealthCheck>) new ParticipantHealthCheckListener(participantHandler.get()));
        msgDispatcher.register(ParticipantMessageType.PARTICIPANT_CONTROL_LOOP_STATE_CHANGE.name(),
            (ScoListener<ParticipantControlLoopStateChange>) new ControlLoopStateChangeListener(
                participantHandler.get()));
        msgDispatcher.register(ParticipantMessageType.PARTICIPANT_CONTROL_LOOP_UPDATE.name(),
            (ScoListener<ParticipantControlLoopUpdate>) new ControlLoopUpdateListener(participantHandler.get()));
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
     * Return the participant handler.
     *
     * @return the participant handler
     */
    public ParticipantHandler getParticipantHandler() {
        return participantHandler.get();
    }
}
