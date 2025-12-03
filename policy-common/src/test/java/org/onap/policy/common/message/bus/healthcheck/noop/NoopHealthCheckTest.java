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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.message.bus.event.CommonTestData;
import org.onap.policy.common.message.bus.event.TopicEndpoint;
import org.onap.policy.common.message.bus.event.TopicEndpointManager;

class NoopHealthCheckTest {

    @Test
    void testBuild() {
        TopicEndpoint topicEndpoint = TopicEndpointManager.getManager();
        topicEndpoint.start();
        var healthCheck = new NoopHealthCheck();
        var result = healthCheck.healthCheck(List.of());
        assertTrue(result);
    }

    @Test
    void testBuild_Failure() {
        TopicEndpoint topicEndpoint = TopicEndpointManager.getManager();
        topicEndpoint.start();
        var healthCheck = new NoopHealthCheck();
        topicEndpoint.stop();
        var result = healthCheck.healthCheck(List.of());
        assertFalse(result);
    }

    @Test
    void test_TopicIsAlive() {
        TopicEndpoint topicEndpoint = TopicEndpointManager.getManager();

        var topicSource = CommonTestData.getTopicParameters("topicSource", "noop", "localhost");
        var topicSink = CommonTestData.getTopicParameters("topicSink", "noop", "localhost");

        topicEndpoint.addTopicSources(List.of(topicSource));
        topicEndpoint.addTopicSinks(List.of(topicSink));

        topicEndpoint.start();
        var healthCheck = new NoopHealthCheck();
        var result = healthCheck.healthCheck(List.of("topicSource", "topicSink"));
        assertTrue(result);
    }

    @Test
    void test_TopicIsNotAlive() {
        TopicEndpoint topicEndpoint = TopicEndpointManager.getManager();

        var topicSource = CommonTestData.getTopicParameters("topicSource", "noop", "localhost");
        var topicSink = CommonTestData.getTopicParameters("topicSink", "noop", "localhost");

        topicEndpoint.addTopicSources(List.of(topicSource));
        topicEndpoint.addTopicSinks(List.of(topicSink));

        topicEndpoint.start();

        var topic = topicEndpoint.getNoopTopicSource("topicsource");
        topic.stop();
        var healthCheck = new NoopHealthCheck();
        var result = healthCheck.healthCheck(List.of("topicSource", "topicSink"));
        assertFalse(result);
    }
}
