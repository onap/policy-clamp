/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025 Nordix Foundation.
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

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import org.onap.policy.clamp.acm.participant.intermediary.parameters.ParticipantParameters;
import org.onap.policy.common.message.bus.event.Topic;
import org.onap.policy.common.message.bus.healthcheck.TopicHealthCheck;
import org.onap.policy.common.message.bus.healthcheck.TopicHealthCheckFactory;
import org.onap.policy.common.parameters.topic.TopicParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class BrokerStarter<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrokerStarter.class);
    private final IntermediaryActivator activator;
    private final ParticipantHandler participantHandler;
    private final TopicHealthCheck topicHealthCheck;

    private final ParticipantParameters parameters;
    private final List<Publisher> publishers;
    private final List<Listener<T>> listeners;

    /**
     * Constructor.
     *
     * @param parameters ParticipantParameters
     * @param activator IntermediaryActivator
     * @param participantHandler participantHandler
     */
    public BrokerStarter(ParticipantParameters parameters,
            List<Publisher> publishers, List<Listener<T>> listeners, IntermediaryActivator activator,
            ParticipantHandler participantHandler) {
        this.parameters = parameters;
        this.listeners = listeners;
        this.publishers = publishers;
        this.activator = activator;
        this.participantHandler = participantHandler;
        var topic = parameters.getIntermediaryParameters().getClampAdminTopics();
        if (topic == null) {
            topic = new TopicParameters();
            topic.setTopicCommInfrastructure(Topic.CommInfrastructure.NOOP.name());
        }
        this.topicHealthCheck = createTopicHealthCheck(topic);
    }

    protected TopicHealthCheck createTopicHealthCheck(TopicParameters topic) {
        return new TopicHealthCheckFactory().getTopicHealthCheck(topic);
    }

    /**
     * Handle ContextRefreshEvent.
     *
     * @param ctxRefreshedEvent ContextRefreshedEvent
     */
    @EventListener
    public void handleContextRefreshEvent(ContextRefreshedEvent ctxRefreshedEvent) {
        if (!activator.isAlive()) {
            runTopicHealthCheck();
            start();
        }
    }

    private void runTopicHealthCheck() {
        var fetchTimeout = getFetchTimeout();
        while (!topicHealthCheck.healthCheck(getTopics())) {
            LOGGER.debug(" Broker not up yet!");
            try {
                Thread.sleep(fetchTimeout);
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
    }

    private List<String> getTopics() {
        var opTopic = parameters.getIntermediaryParameters().getTopics().getOperationTopic();
        var syncTopic = parameters.getIntermediaryParameters().getTopics().getSyncTopic();
        return Boolean.TRUE.equals(parameters.getIntermediaryParameters().getTopicValidation())
                ? List.of(opTopic, syncTopic) : List.<String>of();
    }

    private int getFetchTimeout() {
        int fetchTimeout = parameters.getIntermediaryParameters().getClampAdminTopics() == null
                ? 0 : parameters.getIntermediaryParameters().getClampAdminTopics().getFetchTimeout();
        return Math.max(fetchTimeout, 5000);
    }

    private void start() {
        activator.config(parameters, publishers, listeners);
        activator.start();
        var task = new TimerTask() {
            @Override
            public void run() {
                new Thread(participantHandler::sendParticipantRegister).start();
            }
        };
        new Timer().schedule(task, 5000);
    }


    /**
     * Handle ContextClosedEvent.
     *
     * @param ctxClosedEvent ContextClosedEvent
     */
    @EventListener
    public void handleContextClosedEvent(ContextClosedEvent ctxClosedEvent) {
        if (activator.isAlive()) {
            participantHandler.sendParticipantDeregister();
            activator.stop();
        }
    }
}
