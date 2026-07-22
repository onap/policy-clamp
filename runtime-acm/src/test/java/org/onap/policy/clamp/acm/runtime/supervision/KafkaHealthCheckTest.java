/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025-2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.runtime.supervision;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.junit.jupiter.api.Test;


class KafkaHealthCheckTest {

    @Test
    void testAdminClientNoServer() {
        var healthCheck = new KafkaHealthCheck();
        var map  = new HashMap<String, Object>();
        var result = healthCheck.healthCheck(map, List.of());
        assertTrue(result);
        map.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "");
        result = healthCheck.healthCheck(map, List.of());
        assertTrue(result);
    }

    @Test
    void testMockAdminClientWithError() throws ExecutionException, InterruptedException {
        var adminClient = mock(AdminClient.class);
        KafkaFuture<Collection<Node>> kafkaFuture = mock(KafkaFuture.class);
        var describeCluster = mock(DescribeClusterResult.class);
        when(describeCluster.nodes()).thenReturn(kafkaFuture);
        when(adminClient.describeCluster()).thenReturn(describeCluster);
        when(kafkaFuture.get()).thenThrow(new InterruptedException());
        var healthCheck = createKafkaHealthCheck(adminClient);
        var map  = new HashMap<String, Object>();
        map.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9292");
        var result = healthCheck.healthCheck(map, List.of());
        assertFalse(result);
    }

    @Test
    void testMockAdminClient() {
        // no node Kafka
        var describeCluster = mock(DescribeClusterResult.class);
        when(describeCluster.nodes()).thenReturn(KafkaFuture.completedFuture(null));
        var adminClient = mock(AdminClient.class);
        when(adminClient.describeCluster()).thenReturn(describeCluster);
        var map  = new HashMap<String, Object>();
        map.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9292");
        var healthCheck = createKafkaHealthCheck(adminClient);
        var result = healthCheck.healthCheck(map, List.of());
        assertFalse(result);

        // Kafka is UP
        var node = new Node(1, "localhost", 9092);
        when(describeCluster.nodes()).thenReturn(KafkaFuture.completedFuture(List.of(node)));
        result = healthCheck.healthCheck(map, List.of());
        assertTrue(result);

        // Kafka topics not available
        var listTopics = mock(ListTopicsResult.class);
        when(adminClient.listTopics()).thenReturn(listTopics);
        when(listTopics.names()).thenReturn(KafkaFuture.completedFuture(Set.of()));
        result = healthCheck.healthCheck(map, List.of("topic"));
        assertFalse(result);

        when(listTopics.names()).thenReturn(KafkaFuture.completedFuture(Set.of("topic")));
        result = healthCheck.healthCheck(map, List.of("wrongTopic"));
        assertFalse(result);

        // Kafka topics available
        result = healthCheck.healthCheck(map, List.of("topic"));
        assertTrue(result);
    }

    private KafkaHealthCheck createKafkaHealthCheck(AdminClient adminClient) {
        return new KafkaHealthCheck() {
            @Override
            protected AdminClient createAdminClient(Map<String, Object> properties) {
                return adminClient;
            }
        };
    }
}
