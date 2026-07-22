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

package org.onap.policy.common.message.bus.healthcheck.kafka;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.KafkaException;
import org.onap.policy.common.message.bus.healthcheck.TopicHealthCheck;

@Slf4j
@RequiredArgsConstructor
public class KafkaHealthCheck implements TopicHealthCheck {

    private final Map<String, Object> properties;

    /**
     * Check that Kafka is OnLine and topics are available.
     *
     * @return true if Kafka is OnLine
     */
    public boolean healthCheck(List<String> topics) {
        var server = properties.get(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG);
        if (server == null || server.toString().isEmpty()) {
            log.warn("Kafka Address not defined!");
            return true;
        }
        try (var client = createAdminClient()) {
            if (!checkConnection(client)) {
                log.warn("Kafka not UP yet!");
                return false;
            }
            if (topics.isEmpty()) {
                log.warn("Kafka is UP");
                return true;
            }

            return checkTopics(client, topics);
        } catch (KafkaException | ExecutionException e) {
            log.error(e.getMessage());
            return false;
        } catch (InterruptedException e) {
            log.error(e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean checkConnection(AdminClient client) throws ExecutionException, InterruptedException {
        var nodes = client.describeCluster().nodes().get();
        if (nodes == null || nodes.isEmpty()) {
            return false;
        }
        nodes.forEach(node -> log.debug("nodeId {}", node.id()));
        return true;
    }

    private boolean checkTopics(AdminClient client, List<String> topics)
            throws ExecutionException, InterruptedException {
        var listTopics = client.listTopics().names().get();
        if (listTopics == null || listTopics.isEmpty()) {
            log.warn("Kafka topics not available!");
            return false;
        }
        var setTopics = listTopics.stream().map(String::toLowerCase).collect(Collectors.toSet());
        for (var topic : topics) {
            if (!setTopics.contains(topic.toLowerCase())) {
                log.warn("Kafka topic {} not available!", topic);
                return false;
            }
        }
        log.info("Kafka is UP and topics available!");
        return true;
    }

    protected AdminClient createAdminClient() {
        var kafkaProps = new Properties();
        kafkaProps.putAll(properties);
        return AdminClient.create(kafkaProps);
    }
}
