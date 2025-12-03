/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022, 2024 Nordix Foundation.
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
 * KAFKA Topic Sink Factory.
 */
public interface KafkaTopicSinkFactory {

    /**
     * Instantiates a new KAFKA Topic Writer.
     *
     * @param busTopicParams parameters object
     * @return an KAFKA Topic Sink
     */
    KafkaTopicSink build(BusTopicParams busTopicParams);

    /**
     * Creates an KAFKA Topic Writer based on properties files.
     *
     * @param properties Properties containing initialization values
     *
     * @return an KAFKA Topic Writer
     * @throws IllegalArgumentException if invalid parameters are present
     */
    List<KafkaTopicSink> build(Properties properties);

    /**
     * Instantiates a new KAFKA Topic Writer.
     *
     * @param servers list of servers
     * @param topic topic name
     *
     * @return an KAFKA Topic Writer
     * @throws IllegalArgumentException if invalid parameters are present
     */
    KafkaTopicSink build(List<String> servers, String topic);

    /**
     * Destroys an KAFKA Topic Writer based on a topic.
     *
     * @param topic topic name
     * @throws IllegalArgumentException if invalid parameters are present
     */
    void destroy(String topic);

    /**
     * Destroys all KAFKA Topic Writers.
     */
    void destroy();

    /**
     * gets an KAFKA Topic Writer based on topic name.
     *
     * @param topic the topic name
     *
     * @return an KAFKA Topic Writer with topic name
     * @throws IllegalArgumentException if an invalid topic is provided
     * @throws IllegalStateException if the KAFKA Topic Reader is an incorrect state
     */
    KafkaTopicSink get(String topic);

    /**
     * Provides a snapshot of the KAFKA Topic Writers.
     *
     * @return a list of the KAFKA Topic Writers
     */
    List<KafkaTopicSink> inventory();
}
