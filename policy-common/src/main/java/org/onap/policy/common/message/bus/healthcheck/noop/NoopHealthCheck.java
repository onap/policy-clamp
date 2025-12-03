/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.common.message.bus.healthcheck.noop;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.onap.policy.common.message.bus.event.Topic;
import org.onap.policy.common.message.bus.event.TopicEndpoint;
import org.onap.policy.common.message.bus.event.TopicEndpointManager;
import org.onap.policy.common.message.bus.healthcheck.TopicHealthCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoopHealthCheck implements TopicHealthCheck {

    private final Logger logger = LoggerFactory.getLogger(NoopHealthCheck.class);

    private final TopicEndpoint topicEndpoint = TopicEndpointManager.getManager();

    private Map<String, Topic> actualTopics;

    @Override
    public boolean healthCheck(List<String> topics) {
        var topicsHealthy = true;

        this.populateActualTopics();

        if (topicEndpoint.isAlive()) {
            for (String topic : topics) {
                var actualTopic = actualTopics.get(topic.toLowerCase());
                if (!actualTopic.isAlive()) {
                    logger.warn("Topic {} is not alive!", topic);
                    topicsHealthy = false;
                    break;
                }
            }
        } else {
            logger.warn("Topic Endpoint is not alive!");
            return false;
        }

        return topicsHealthy;
    }

    private void populateActualTopics() {
        actualTopics = new HashMap<>();
        topicEndpoint.getNoopTopicSinks().forEach(sink -> actualTopics.put(sink.getTopic(), sink));
        topicEndpoint.getNoopTopicSources().forEach(source -> actualTopics.put(source.getTopic(), source));
    }
}
