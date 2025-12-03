/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022-2024 Nordix Foundation.
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

package org.onap.policy.common.message.bus.event.kafka;

import java.util.List;
import java.util.Properties;
import org.onap.policy.common.parameters.topic.BusTopicParams;

/**
 * Kafka Topic Source Factory.
 */
public interface KafkaTopicSourceFactory {

    /**
     * Creates a Kafka Topic Source based on properties files.
     *
     * @param properties Properties containing initialization values
     *
     * @return a Kafka Topic Source
     * @throws IllegalArgumentException if invalid parameters are present
     */
    List<KafkaTopicSource> build(Properties properties);

    /**
     * Instantiates a new Kafka Topic Source.
     *
     * @param busTopicParams parameters object
     * @return a Kafka Topic Source
     */
    KafkaTopicSource build(BusTopicParams busTopicParams);

    /**
     * Instantiates a new Kafka Topic Source.
     *
     * @param servers list of servers
     * @param topic topic name
     *
     * @return a Kafka Topic Source
     * @throws IllegalArgumentException if invalid parameters are present
     */
    KafkaTopicSource build(List<String> servers, String topic);

    /**
     * Destroys a Kafka Topic Source based on a topic.
     *
     * @param topic topic name
     * @throws IllegalArgumentException if invalid parameters are present
     */
    void destroy(String topic);

    /**
     * Destroys all Kafka Topic Sources.
     */
    void destroy();

    /**
     * Gets a Kafka Topic Source based on topic name.
     *
     * @param topic the topic name
     * @return a Kafka Topic Source with topic name
     * @throws IllegalArgumentException if an invalid topic is provided
     * @throws IllegalStateException if the Kafka Topic Source is an incorrect state
     */
    KafkaTopicSource get(String topic);

    /**
     * Provides a snapshot of the Kafka Topic Sources.
     *
     * @return a list of the Kafka Topic Sources
     */
    List<KafkaTopicSource> inventory();
}
