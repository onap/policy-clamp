/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 Nordix Foundation.
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

package org.onap.policy.common.message.bus.healthcheck.kafka;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.message.bus.healthcheck.TopicHealthCheck;
import org.onap.policy.common.parameters.topic.TopicParameters;

class KafkaHealthCheckTest {

    @Test
    void testAdminClient() {
        var param = new TopicParameters();
        param.setServers(List.of("localhost"));
        var healthCheck = new KafkaHealthCheck(param);
        var result = healthCheck.healthCheck(List.of());
        assertFalse(result);

        param.setAdditionalProps(Map.of("key", "value"));
        result = healthCheck.healthCheck(List.of());
        assertFalse(result);
    }

    @Test
    void testMockAdminClientWithError() throws ExecutionException, InterruptedException {
        var param = new TopicParameters();
        param.setServers(List.of("localhost"));
        var adminClient = mock(AdminClient.class);
        KafkaFuture<Collection<Node>> kafkaFuture = mock(KafkaFuture.class);
        var describeCluster = mock(DescribeClusterResult.class);
        when(describeCluster.nodes()).thenReturn(kafkaFuture);
        when(adminClient.describeCluster()).thenReturn(describeCluster);
        when(kafkaFuture.get()).thenThrow(new InterruptedException());
        var healthCheck = createKafkaHealthCheck(adminClient, param);
        var result = healthCheck.healthCheck(List.of());
        Assertions.assertFalse(result);
    }

    @Test
    void testMockAdminClient() {
        var param = new TopicParameters();
        var adminClient = mock(AdminClient.class);
        // no server address
        var healthCheck = createKafkaHealthCheck(adminClient, param);
        var result = healthCheck.healthCheck(List.of());
        Assertions.assertTrue(result);

        param.setServers(List.of());
        result = healthCheck.healthCheck(List.of());
        Assertions.assertTrue(result);

        // no node Kafka
        param.setServers(List.of("localhost"));
        healthCheck = createKafkaHealthCheck(adminClient, param);
        var describeCluster = mock(DescribeClusterResult.class);
        when(describeCluster.nodes()).thenReturn(KafkaFuture.completedFuture(null));
        when(adminClient.describeCluster()).thenReturn(describeCluster);
        result = healthCheck.healthCheck(List.of());
        Assertions.assertFalse(result);

        // Kafka is UP
        var node = new Node(1, "localhost", 9092);
        when(describeCluster.nodes()).thenReturn(KafkaFuture.completedFuture(List.of(node)));
        result = healthCheck.healthCheck(List.of());
        Assertions.assertTrue(result);

        // Kafka topics not available
        var listTopics = mock(ListTopicsResult.class);
        when(adminClient.listTopics()).thenReturn(listTopics);
        when(listTopics.names()).thenReturn(KafkaFuture.completedFuture(Set.of()));
        result = healthCheck.healthCheck(List.of("topic"));
        Assertions.assertFalse(result);

        when(listTopics.names()).thenReturn(KafkaFuture.completedFuture(Set.of("topic")));
        result = healthCheck.healthCheck(List.of("wrongTopic"));
        Assertions.assertFalse(result);

        // Kafka topics available
        result = healthCheck.healthCheck(List.of("topic"));
        Assertions.assertTrue(result);
    }

    private TopicHealthCheck createKafkaHealthCheck(AdminClient adminClient, TopicParameters param) {
        return new KafkaHealthCheck(param) {
            @Override
            protected AdminClient createAdminClient() {
                return adminClient;
            }
        };
    }
}
