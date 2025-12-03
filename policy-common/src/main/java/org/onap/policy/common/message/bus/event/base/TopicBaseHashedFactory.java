/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024 Nordix Foundation.
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

package org.onap.policy.common.message.bus.event.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import org.onap.policy.common.message.bus.event.Topic;
import org.onap.policy.common.parameters.topic.BusTopicParams;

/**
 * Topic Factory implementation that indexes T instances in a hash table.
 */
public abstract class TopicBaseHashedFactory<T extends Topic> implements TopicBaseFactory<T> {

    protected static final String MISSING_TOPIC_MESSAGE = "A topic must be provided";
    protected static final String MISSING_SERVERS_MESSAGE = "Servers must be provided";

    /**
     * endpoints.
     */
    protected final HashMap<String, T> endpoints = new HashMap<>();

    /**
     * get the topic names.
     *
     * @param properties properties.
     * @return list of topic names.
     */
    protected abstract List<String> getTopicNames(Properties properties);

    /**
     * get the servers that this topic uses.
     *
     * @param topicName name.
     * @param properties properties.
     * @return list of servers.
     */
    protected abstract List<String> getServers(String topicName, Properties properties);

    /**
     * Determines if this topic is managed.
     *
     * @param topicName name.
     * @param properties properties.
     * @return if managed.
     */
    protected abstract boolean isManaged(String topicName, Properties properties);

    /**
     * construct an instance of an endpoint.
     *
     * @param servers servers,
     * @param topic topic.
     * @return an instance of T.
     */
    public abstract T build(List<String> servers, String topic);

    /**
     * {@inheritDoc}.
     */
    @Override
    public List<T> build(Properties properties) {
        List<String> topicNames = getTopicNames(properties);
        if (topicNames == null || topicNames.isEmpty()) {
            return Collections.emptyList();
        }

        List<T> newEndpoints = new ArrayList<>();
        synchronized (this) {
            for (String name : topicNames) {
                if (this.endpoints.containsKey(name)) {
                    newEndpoints.add(this.endpoints.get(name));
                    continue;
                }

                newEndpoints.add(this.build(getServers(name, properties), name, isManaged(name, properties)));
            }
        }
        return newEndpoints;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public T build(BusTopicParams param) {
        return this.build(param.getServers(), param.getTopic(), param.isManaged());
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public T build(List<String> servers, String topic, boolean managed) {
        if (servers == null || servers.isEmpty()) {
            throw new IllegalArgumentException(MISSING_SERVERS_MESSAGE);
        }

        if (topic == null || topic.isEmpty()) {
            throw new IllegalArgumentException(MISSING_TOPIC_MESSAGE);
        }

        synchronized (this) {
            if (this.endpoints.containsKey(topic)) {
                return this.endpoints.get(topic);
            }

            var endpoint = build(servers, topic);
            if (managed) {
                this.endpoints.put(topic, endpoint);
            }

            return endpoint;
        }
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void destroy(String topic) {
        if (topic == null || topic.isEmpty()) {
            throw new IllegalArgumentException(MISSING_TOPIC_MESSAGE);
        }

        T endpoint;
        synchronized (this) {
            if (!this.endpoints.containsKey(topic)) {
                return;
            }

            endpoint = this.endpoints.remove(topic);
        }
        endpoint.shutdown();
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void destroy() {
        final List<T> snapshotEndpoints = this.inventory();
        for (final T snapshot : snapshotEndpoints) {
            snapshot.shutdown();
        }

        synchronized (this) {
            this.endpoints.clear();
        }
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public T get(String topic) {
        if (topic == null || topic.isEmpty()) {
            throw new IllegalArgumentException(MISSING_TOPIC_MESSAGE);
        }

        synchronized (this) {
            if (this.endpoints.containsKey(topic)) {
                return this.endpoints.get(topic);
            } else {
                throw new IllegalStateException(topic + " not found");
            }
        }
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public List<T> inventory() {
        return new ArrayList<>(this.endpoints.values());
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public String toString() {
        return "TopicBaseHashedFactory[ " + super.toString() + " ]";
    }
}
