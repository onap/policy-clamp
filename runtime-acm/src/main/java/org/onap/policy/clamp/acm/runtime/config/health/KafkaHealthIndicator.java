/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.policy.clamp.acm.runtime.config.health;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup;
import org.onap.policy.common.message.bus.event.TopicEndpoint;
import org.onap.policy.common.message.bus.event.TopicEndpointManager;
import org.onap.policy.common.message.bus.healthcheck.TopicHealthCheckFactory;
import org.onap.policy.common.parameters.topic.TopicParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class KafkaHealthIndicator implements HealthIndicator {

    private static final String TOPICS = "topics";
    private final AcRuntimeParameterGroup parameterGroup;

    private final TopicEndpoint topicEndpoint = TopicEndpointManager.getManager();

    @Autowired
    public KafkaHealthIndicator(AcRuntimeParameterGroup parameterGroup) {
        this.parameterGroup = parameterGroup;
    }

    @Override
    public Health getHealth(boolean includeDetails) {
        return withDetails(includeDetails);
    }

    @Override
    public Health health() {
        return withDetails(false);
    }

    private Health withDetails(boolean includeDetails) {
        final var healthBuilder = new Health.Builder();
        healthBuilder.up();
        if (topicEndpoint.isAlive()) {
            checkTopicsStatus(healthBuilder, includeDetails);
        } else {
            healthBuilder.down();
        }
        return healthBuilder.build();
    }

    /**
     * Evaluates the health status of Kafka topics (both sinks and sources) and updates the provided
     * {@link Health.Builder} with the results. If the `includeDetails` flag is set to true, detailed
     * information about topic sinks, topic sources, and the topic endpoint is added to the health details.
     *
     * @param healthBuilder the builder object to which the health status and details will be added
     * @param includeDetails flag indicating whether detailed topic information should be included
     */
    private void checkTopicsStatus(final Health.Builder healthBuilder, boolean includeDetails) {
        AtomicBoolean outOfService = new AtomicBoolean(false);
        var topics = new ArrayList<TopicParameters>();
        topics.addAll(parameterGroup.getTopicParameterGroup().getTopicSinks());
        topics.addAll(parameterGroup.getTopicParameterGroup().getTopicSources());

        var topicDetails = new HashMap<String, Object>();

        if (!topics.isEmpty()) {
            var topicParams = topics.get(0);
            var topicsHealthCheck = new TopicHealthCheckFactory().getTopicHealthCheck(topicParams);

            var topicNames = new ArrayList<String>();
            topics.forEach(t -> topicNames.add(t.getTopic()));

            if (topicsHealthCheck.healthCheck(topicNames)) {
                topicDetails.put(TOPICS, Health.up().withDetail(TOPICS, topics).build());
            } else {
                topicDetails.put(TOPICS, Health.outOfService().withDetail(TOPICS, topics).build());
                outOfService.set(true);
            }
        }

        if (includeDetails) {
            healthBuilder.withDetail("topicEndpoint", topicEndpoint);
            healthBuilder.withDetails(topicDetails);
        }

        if (outOfService.get()) {
            healthBuilder.outOfService();
        }
    }
}
