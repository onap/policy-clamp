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

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.KafkaException;
import org.onap.policy.common.message.bus.healthcheck.TopicHealthCheck;
import org.onap.policy.common.parameters.topic.TopicParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaHealthCheck implements TopicHealthCheck {

    private static final Logger logger = LoggerFactory.getLogger(KafkaHealthCheck.class);
    private final TopicParameters parameters;

    public KafkaHealthCheck(TopicParameters parameters) {
        this.parameters = parameters;
    }

    /**
     * Check that Kafka is OnLine and topics are available.
     *
     * @return true if Kafka is OnLine
     */
    public boolean healthCheck(List<String> topics) {
        if (parameters.getServers() == null || parameters.getServers().isEmpty()) {
            logger.warn("Kafka Address not defined!");
            return true;
        }
        try (var client = createAdminClient()) {
            if (!checkConnection(client)) {
                logger.warn("Kafka not UP yet!");
                return false;
            }
            if (topics.isEmpty()) {
                logger.warn("Kafka is UP");
                return true;
            }

            return checkTopics(client, topics);
        } catch (KafkaException | ExecutionException e) {
            logger.error(e.getMessage());
            return false;
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean checkConnection(AdminClient client) throws ExecutionException, InterruptedException {
        var nodes = client.describeCluster().nodes().get();
        if (nodes == null || nodes.isEmpty()) {
            return false;
        }
        nodes.forEach(node -> logger.debug("nodeId {}", node.id()));
        return true;
    }

    private boolean checkTopics(AdminClient client, List<String> topics)
            throws ExecutionException, InterruptedException {
        var listTopics = client.listTopics().names().get();
        if (listTopics == null || listTopics.isEmpty()) {
            logger.warn("Kafka topics not available!");
            return false;
        }
        var setTopics = listTopics.stream().map(String::toLowerCase).collect(Collectors.toSet());
        for (var topic : topics) {
            if (!setTopics.contains(topic.toLowerCase())) {
                logger.warn("Kafka topic {} not available!", topic);
                return false;
            }
        }
        logger.info("Kafka is UP and topics available!");
        return true;
    }

    protected AdminClient createAdminClient() {
        var kafkaProps = new Properties();
        kafkaProps.setProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, parameters.getServers().get(0));

        if (parameters.isAdditionalPropsValid()) {
            kafkaProps.putAll(parameters.getAdditionalProps());
        }
        return AdminClient.create(kafkaProps);
    }
}
