/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2017-2019 AT&T Intellectual Property. All rights reserved.
 * Copyright (C) 2022,2024 Nordix Foundation.
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

package org.onap.policy.common.message.bus.event;

import java.util.List;
import java.util.Properties;
import org.onap.policy.common.capabilities.Lockable;
import org.onap.policy.common.capabilities.Startable;
import org.onap.policy.common.message.bus.event.kafka.KafkaTopicSink;
import org.onap.policy.common.message.bus.event.kafka.KafkaTopicSource;
import org.onap.policy.common.message.bus.event.noop.NoopTopicSink;
import org.onap.policy.common.message.bus.event.noop.NoopTopicSource;
import org.onap.policy.common.parameters.topic.TopicParameterGroup;
import org.onap.policy.common.parameters.topic.TopicParameters;

/**
 * Abstraction to manage the system's Networked Topic Endpoints, sources of all events input into
 * the System.
 */
public interface TopicEndpoint extends Startable, Lockable {

    /**
     * Add topics configuration (sources and sinks) into a single list.
     *
     * @param properties topic configuration
     * @return topic list
     * @throws IllegalArgumentException when invalid arguments are provided
     */
    List<Topic> addTopics(Properties properties);

    /**
     * Add topics configuration (sources and sinks) into a single list.
     *
     * @param params parameters to configure topic
     * @return topic list
     * @throws IllegalArgumentException when invalid arguments are provided
     */
    List<Topic> addTopics(TopicParameterGroup params);

    /**
     * Add Topic Sources to the communication infrastructure initialized per properties.
     *
     * @param properties properties for Topic Source construction
     * @return a list of generic Topic Sources
     * @throws IllegalArgumentException when invalid arguments are provided
     */
    List<TopicSource> addTopicSources(Properties properties);


    /**
     * Add Topic Sources to the communication infrastructure initialized per properties.
     *
     * @param paramList parameters for Topic Source construction
     * @return a list of generic Topic Sources
     * @throws IllegalArgumentException when invalid arguments are provided
     */
    List<TopicSource> addTopicSources(List<TopicParameters> paramList);

    /**
     * Add Topic Sinks to the communication infrastructure initialized per properties.
     *
     * @param properties properties for Topic Sink construction
     * @return a list of generic Topic Sinks
     * @throws IllegalArgumentException when invalid arguments are provided
     */
    List<TopicSink> addTopicSinks(Properties properties);

    /**
     * Add Topic Sinks to the communication infrastructure initialized per properties.
     *
     * @param paramList parameters for Topic Sink construction
     * @return a list of generic Topic Sinks
     * @throws IllegalArgumentException when invalid arguments are provided
     */
    List<TopicSink> addTopicSinks(List<TopicParameters> paramList);

    /**
     * Gets all Topic Sources.
     *
     * @return the Topic Source List
     */
    List<TopicSource> getTopicSources();

    /**
     * Get the Topic Sources for the given topic name.
     *
     * @param topicNames the topic name
     *
     * @return the Topic Source List
     * @throws IllegalStateException if the entity is in an invalid state
     * @throws IllegalArgumentException if invalid parameters are present
     */
    List<TopicSource> getTopicSources(List<String> topicNames);

    /**
     * Gets the Topic Source for the given topic name and underlying communication infrastructure
     * type.
     *
     * @param commType communication infrastructure type
     * @param topicName the topic name
     *
     * @return the Topic Source
     * @throws IllegalStateException if the entity is in an invalid state, for example multiple
     *         TopicReaders for a topic name and communication infrastructure
     * @throws IllegalArgumentException if invalid parameters are present
     * @throws UnsupportedOperationException if the operation is not supported.
     */
    TopicSource getTopicSource(Topic.CommInfrastructure commType, String topicName);

    /**
     * Get the Noop Source for the given topic name.
     *
     * @param topicName the topic name.
     * @return the Noop Source.
     */
    NoopTopicSource getNoopTopicSource(String topicName);

    /**
     * Get the Kafka Source for the given topic name.
     *
     * @param topicName the topic name.
     * @return the Kafka Source.
     */
    KafkaTopicSource getKafkaTopicSource(String topicName);

    /**
     * Get the Topic Sinks for the given topic name.
     *
     * @param topicNames the topic names
     * @return the Topic Sink List
     */
    List<TopicSink> getTopicSinks(List<String> topicNames);

    /**
     * Get the Topic Sinks for the given topic name and all the underlying communication
     * infrastructure type.
     *
     * @param topicName the topic name
     *
     * @return the Topic Sink List
     * @throws IllegalStateException if the entity is in an invalid state, for example multiple
     *         TopicWriters for a topic name and communication infrastructure
     * @throws IllegalArgumentException if invalid parameters are present
     */
    List<TopicSink> getTopicSinks(String topicName);

    /**
     * Gets all Topic Sinks.
     *
     * @return the Topic Sink List
     */
    List<TopicSink> getTopicSinks();

    /**
     * Get the Topic Sinks for the given topic name and underlying communication infrastructure type.
     *
     * @param topicName the topic name
     * @param commType communication infrastructure type
     *
     * @return the Topic Sink List
     * @throws IllegalStateException if the entity is in an invalid state, for example multiple
     *         TopicWriters for a topic name and communication infrastructure
     * @throws IllegalArgumentException if invalid parameters are present
     */
    TopicSink getTopicSink(Topic.CommInfrastructure commType, String topicName);

    /**
     * Get the no-op Topic Sink for the given topic name.
     *
     * @param topicName the topic name
     *
     * @return the Topic Source
     * @throws IllegalStateException if the entity is in an invalid state, for example multiple
     *         TopicReaders for a topic name and communication infrastructure
     * @throws IllegalArgumentException if invalid parameters are present
     */
    NoopTopicSink getNoopTopicSink(String topicName);

    /**
     * Get the KAFKA Topic Source for the given topic name.
     *
     * @param topicName the topic name
     *
     * @return the Topic Source
     * @throws IllegalStateException if the entity is in an invalid state, for example multiple
     *         TopicReaders for a topic name and communication infrastructure
     * @throws IllegalArgumentException if invalid parameters are present
     */
    KafkaTopicSink getKafkaTopicSink(String topicName);

    /**
     * Gets only the KAFKA Topic Sources.
     *
     * @return the KAFKA Topic Source List
     */
    List<KafkaTopicSource> getKafkaTopicSources();

    /**
     * Gets only the NOOP Topic Sources.
     *
     * @return the NOOP Topic Source List
     */
    List<NoopTopicSource> getNoopTopicSources();

    /**
     * Gets only the KAFKA Topic Sinks.
     *
     * @return the KAFKA Topic Sinks List
     */
    List<KafkaTopicSink> getKafkaTopicSinks();

    /**
     * Gets only the NOOP Topic Sinks.
     *
     * @return the NOOP Topic Sinks List
     */
    List<NoopTopicSink> getNoopTopicSinks();

}
