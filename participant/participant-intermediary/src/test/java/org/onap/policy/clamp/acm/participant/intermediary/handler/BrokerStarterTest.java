/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantStatusReqListener;
import org.onap.policy.clamp.acm.participant.intermediary.main.parameters.CommonTestData;
import org.onap.policy.common.message.bus.event.Topic;
import org.onap.policy.common.message.bus.event.TopicEndpoint;
import org.onap.policy.common.message.bus.event.TopicEndpointManager;
import org.onap.policy.common.message.bus.healthcheck.TopicHealthCheck;
import org.onap.policy.common.parameters.topic.TopicParameters;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;

class BrokerStarterTest {

    @BeforeEach
    void setUp() {
        TopicEndpoint endpointManager = TopicEndpointManager.getManager();
        endpointManager.start();
    }

    @Test
    void testWithClampAdminTopicsNull() {
        var parameters = CommonTestData.getParticipantParameters();
        parameters.getIntermediaryParameters().setClampAdminTopics(null);
        var publishers = List.of(mock(Publisher.class));
        var listeners = List.of(mock(ParticipantStatusReqListener.class));
        var activator = mock(IntermediaryActivator.class);
        var participantHandler = mock(ParticipantHandler.class);
        var brokerStarter = new BrokerStarter(parameters, publishers, listeners, activator, participantHandler);
        when(activator.isAlive()).thenReturn(false);

        brokerStarter.handleContextRefreshEvent(mock(ContextRefreshedEvent.class));
        verify(activator).start();

        brokerStarter.handleContextClosedEvent(mock(ContextClosedEvent.class));
        verify(participantHandler, times(0)).sendParticipantDeregister();
        verify(activator, times(0)).stop();
    }

    @Test
    void testAlreadyAlive() {
        var parameters = CommonTestData.getParticipantParameters();
        var topic = new TopicParameters();
        topic.setTopicCommInfrastructure(Topic.CommInfrastructure.NOOP.name());
        parameters.getIntermediaryParameters().setClampAdminTopics(topic);
        var publishers = List.of(mock(Publisher.class));
        var listeners = List.of(mock(ParticipantStatusReqListener.class));
        var activator = mock(IntermediaryActivator.class);
        var participantHandler = mock(ParticipantHandler.class);
        var brokerStarter = new BrokerStarter(parameters, publishers, listeners, activator, participantHandler);

        when(activator.isAlive()).thenReturn(true);
        brokerStarter.handleContextRefreshEvent(mock(ContextRefreshedEvent.class));
        verify(activator, times(0)).start();

        brokerStarter.handleContextClosedEvent(mock(ContextClosedEvent.class));
        verify(activator).stop();
        verify(participantHandler).sendParticipantDeregister();
    }

    private static class DummyTopicHealthCheck implements TopicHealthCheck {

        int count = 0;

        // first call is false, next will be true
        @Override
        public boolean healthCheck(List<String> list) {
            return (count++) > 0;
        }
    }

    @Test
    void testWithClampAdminTopics() {
        var parameters = CommonTestData.getParticipantParameters();
        var topic = new TopicParameters();
        topic.setTopicCommInfrastructure(Topic.CommInfrastructure.NOOP.name());
        parameters.getIntermediaryParameters().setClampAdminTopics(topic);
        var publishers = List.of(mock(Publisher.class));
        var listeners = List.of(mock(ParticipantStatusReqListener.class));
        var activator = mock(IntermediaryActivator.class);
        var participantHandler = mock(ParticipantHandler.class);
        var topicHealthCheck = new DummyTopicHealthCheck();
        var brokerStarter = new BrokerStarter(parameters, publishers, listeners, activator, participantHandler) {
            @Override
            protected TopicHealthCheck createTopicHealthCheck(TopicParameters topic) {
                return topicHealthCheck;
            }
        };

        when(activator.isAlive()).thenReturn(false);
        brokerStarter.handleContextRefreshEvent(mock(ContextRefreshedEvent.class));
        verify(activator).start();
    }
}
