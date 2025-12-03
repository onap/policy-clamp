/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2017-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2022-2024 Nordix Foundation.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import lombok.Getter;
import org.onap.policy.common.capabilities.Startable;
import org.onap.policy.common.gson.annotation.GsonJsonIgnore;
import org.onap.policy.common.message.bus.event.kafka.KafkaTopicFactories;
import org.onap.policy.common.message.bus.event.kafka.KafkaTopicSink;
import org.onap.policy.common.message.bus.event.kafka.KafkaTopicSource;
import org.onap.policy.common.message.bus.event.noop.NoopTopicFactories;
import org.onap.policy.common.message.bus.event.noop.NoopTopicSink;
import org.onap.policy.common.message.bus.event.noop.NoopTopicSource;
import org.onap.policy.common.parameters.topic.TopicParameterGroup;
import org.onap.policy.common.parameters.topic.TopicParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This implementation of the Topic Endpoint Manager, proxies operations to the appropriate
 * implementation(s).
 */
@Getter
public class TopicEndpointProxy implements TopicEndpoint {
    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(TopicEndpointProxy.class);

    /**
     * Is this element locked boolean.
     */
    private volatile boolean locked = false;

    /**
     * Is this element alive boolean.
     */
    private volatile boolean alive = false;

    @Override
    public List<Topic> addTopics(Properties properties) {
        List<Topic> topics = new ArrayList<>(addTopicSources(properties));
        topics.addAll(addTopicSinks(properties));
        return topics;
    }

    @Override
    public List<Topic> addTopics(TopicParameterGroup params) {
        List<TopicParameters> sinks =
            (params.getTopicSinks() != null ? params.getTopicSinks() : Collections.emptyList());
        List<TopicParameters> sources =
            (params.getTopicSources() != null ? params.getTopicSources() : Collections.emptyList());

        List<Topic> topics = new ArrayList<>(sinks.size() + sources.size());
        topics.addAll(addTopicSources(sources));
        topics.addAll(addTopicSinks(sinks));
        return topics;
    }

    @Override
    public List<TopicSource> addTopicSources(List<TopicParameters> paramList) {
        List<TopicSource> sources = new ArrayList<>(paramList.size());

        for (TopicParameters param : paramList) {
            switch (Topic.CommInfrastructure.valueOf(param.getTopicCommInfrastructure().toUpperCase())) {
                case KAFKA:
                    sources.add(KafkaTopicFactories.getSourceFactory().build(param));
                    break;
                case NOOP:
                    sources.add(NoopTopicFactories.getSourceFactory().build(param));
                    break;
                default:
                    logger.debug("Unknown source type {} for topic: {}", param.getTopicCommInfrastructure(),
                        param.getTopic());
                    break;
            }
        }

        lockSources(sources);

        return sources;
    }

    @Override
    public List<TopicSource> addTopicSources(Properties properties) {

        // 1. Create KAFKA Sources
        // 2. Create NOOP Sources

        List<TopicSource> sources = new ArrayList<>();

        sources.addAll(KafkaTopicFactories.getSourceFactory().build(properties));
        sources.addAll(NoopTopicFactories.getSourceFactory().build(properties));

        lockSources(sources);

        return sources;
    }

    private void lockSources(List<TopicSource> sources) {
        if (this.isLocked()) {
            sources.forEach(TopicSource::lock);
        }
    }

    @Override
    public List<TopicSink> addTopicSinks(List<TopicParameters> paramList) {
        List<TopicSink> sinks = new ArrayList<>(paramList.size());

        for (TopicParameters param : paramList) {
            switch (Topic.CommInfrastructure.valueOf(param.getTopicCommInfrastructure().toUpperCase())) {
                case KAFKA:
                    sinks.add(KafkaTopicFactories.getSinkFactory().build(param));
                    break;
                case NOOP:
                    sinks.add(NoopTopicFactories.getSinkFactory().build(param));
                    break;
                default:
                    logger.debug("Unknown sink type {} for topic: {}", param.getTopicCommInfrastructure(),
                        param.getTopic());
                    break;
            }
        }

        lockSinks(sinks);

        return sinks;
    }

    @Override
    public List<TopicSink> addTopicSinks(Properties properties) {
        // 1. Create KAFKA Sinks
        // 2. Create NOOP Sinks

        final List<TopicSink> sinks = new ArrayList<>();

        sinks.addAll(KafkaTopicFactories.getSinkFactory().build(properties));
        sinks.addAll(NoopTopicFactories.getSinkFactory().build(properties));

        lockSinks(sinks);

        return sinks;
    }

    private void lockSinks(List<TopicSink> sinks) {
        if (this.isLocked()) {
            sinks.forEach(TopicSink::lock);
        }
    }

    @Override
    public List<TopicSource> getTopicSources() {

        final List<TopicSource> sources = new ArrayList<>();

        sources.addAll(KafkaTopicFactories.getSourceFactory().inventory());
        sources.addAll(NoopTopicFactories.getSourceFactory().inventory());

        return sources;
    }

    @Override
    public List<TopicSource> getTopicSources(List<String> topicNames) {

        if (topicNames == null) {
            throw new IllegalArgumentException("must provide a list of topics");
        }

        final List<TopicSource> sources = new ArrayList<>();

        topicNames.forEach(topic -> {
            try {
                sources.add(Objects.requireNonNull(this.getKafkaTopicSource(topic)));
            } catch (final Exception e) {
                logger.debug("No KAFKA source for topic: {}", topic, e);
            }

            try {
                sources.add(Objects.requireNonNull(this.getNoopTopicSource(topic)));
            } catch (final Exception e) {
                logger.debug("No NOOP source for topic: {}", topic, e);
            }
        });

        return sources;
    }

    @Override
    public List<TopicSink> getTopicSinks() {

        final List<TopicSink> sinks = new ArrayList<>();

        sinks.addAll(KafkaTopicFactories.getSinkFactory().inventory());
        sinks.addAll(NoopTopicFactories.getSinkFactory().inventory());

        return sinks;
    }

    @Override
    public List<TopicSink> getTopicSinks(List<String> topicNames) {

        if (topicNames == null) {
            throw new IllegalArgumentException("must provide a list of topics");
        }

        final List<TopicSink> sinks = new ArrayList<>();
        for (final String topic : topicNames) {
            try {
                sinks.add(Objects.requireNonNull(this.getKafkaTopicSink(topic)));
            } catch (final Exception e) {
                logger.debug("No KAFKA sink for topic: {}", topic, e);
            }

            try {
                sinks.add(Objects.requireNonNull(this.getNoopTopicSink(topic)));
            } catch (final Exception e) {
                logger.debug("No NOOP sink for topic: {}", topic, e);
            }
        }
        return sinks;
    }

    @Override
    public List<TopicSink> getTopicSinks(String topicName) {
        if (topicName == null) {
            throw paramException(null);
        }

        final List<TopicSink> sinks = new ArrayList<>();

        try {
            sinks.add(this.getKafkaTopicSink(topicName));
        } catch (final Exception e) {
            logNoSink(topicName, e);
        }

        try {
            sinks.add(this.getNoopTopicSink(topicName));
        } catch (final Exception e) {
            logNoSink(topicName, e);
        }

        return sinks;
    }

    @GsonJsonIgnore
    @Override
    public List<KafkaTopicSource> getKafkaTopicSources() {
        return KafkaTopicFactories.getSourceFactory().inventory();
    }

    @GsonJsonIgnore
    @Override
    public List<NoopTopicSource> getNoopTopicSources() {
        return NoopTopicFactories.getSourceFactory().inventory();
    }

    @Override
    @GsonJsonIgnore
    public List<KafkaTopicSink> getKafkaTopicSinks() {
        return KafkaTopicFactories.getSinkFactory().inventory();
    }

    @GsonJsonIgnore
    @Override
    public List<NoopTopicSink> getNoopTopicSinks() {
        return NoopTopicFactories.getSinkFactory().inventory();
    }

    @Override
    public boolean start() {

        synchronized (this) {
            if (this.locked) {
                throw new IllegalStateException(this + " is locked");
            }

            if (this.alive) {
                return true;
            }

            this.alive = true;
        }

        final List<Startable> endpoints = this.getEndpoints();

        var success = true;
        for (final Startable endpoint : endpoints) {
            try {
                success = endpoint.start() && success;
            } catch (final Exception e) {
                success = false;
                logger.error("Problem starting endpoint: {}", endpoint, e);
            }
        }

        return success;
    }

    @Override
    public boolean stop() {

        /*
         * stop regardless if it is locked, in other words, stop operation has precedence over
         * locks.
         */
        synchronized (this) {
            this.alive = false;
        }

        final List<Startable> endpoints = this.getEndpoints();

        var success = true;
        for (final Startable endpoint : endpoints) {
            try {
                success = endpoint.stop() && success;
            } catch (final Exception e) {
                success = false;
                logger.error("Problem stopping endpoint: {}", endpoint, e);
            }
        }

        return success;
    }

    /**
     * Gets the endpoints.
     *
     * @return list of managed endpoints
     */
    @GsonJsonIgnore
    protected List<Startable> getEndpoints() {
        final List<Startable> endpoints = new ArrayList<>();

        endpoints.addAll(this.getTopicSources());
        endpoints.addAll(this.getTopicSinks());

        return endpoints;
    }

    @Override
    public void shutdown() {
        this.stop();

        KafkaTopicFactories.getSourceFactory().destroy();
        KafkaTopicFactories.getSinkFactory().destroy();

        NoopTopicFactories.getSinkFactory().destroy();
        NoopTopicFactories.getSourceFactory().destroy();

    }

    @Override
    public boolean lock() {
        boolean shouldLock;

        synchronized (this) {
            shouldLock = !this.locked;
            this.locked = true;
        }

        if (shouldLock) {
            for (final TopicSource source : this.getTopicSources()) {
                source.lock();
            }

            for (final TopicSink sink : this.getTopicSinks()) {
                sink.lock();
            }
        }

        return true;
    }

    @Override
    public boolean unlock() {
        boolean shouldUnlock;

        synchronized (this) {
            shouldUnlock = this.locked;
            this.locked = false;
        }

        if (shouldUnlock) {
            for (final TopicSource source : this.getTopicSources()) {
                source.unlock();
            }

            for (final TopicSink sink : this.getTopicSinks()) {
                sink.unlock();
            }
        }

        return true;
    }

    @Override
    public TopicSource getTopicSource(Topic.CommInfrastructure commType, String topicName) {

        if (commType == null) {
            throw paramException(topicName);
        }

        if (topicName == null) {
            throw paramException(null);
        }

        return switch (commType) {
            case KAFKA -> this.getKafkaTopicSource(topicName);
            case NOOP -> this.getNoopTopicSource(topicName);
            default -> throw new UnsupportedOperationException("Unsupported " + commType.name());
        };
    }

    @Override
    public TopicSink getTopicSink(Topic.CommInfrastructure commType, String topicName) {
        if (commType == null) {
            throw paramException(topicName);
        }

        if (topicName == null) {
            throw paramException(null);
        }

        return switch (commType) {
            case KAFKA -> this.getKafkaTopicSink(topicName);
            case NOOP -> this.getNoopTopicSink(topicName);
            default -> throw new UnsupportedOperationException("Unsupported " + commType.name());
        };
    }

    @Override
    public KafkaTopicSource getKafkaTopicSource(String topicName) {
        return KafkaTopicFactories.getSourceFactory().get(topicName);
    }

    @Override
    public NoopTopicSource getNoopTopicSource(String topicName) {
        return NoopTopicFactories.getSourceFactory().get(topicName);
    }

    @Override
    public KafkaTopicSink getKafkaTopicSink(String topicName) {
        return KafkaTopicFactories.getSinkFactory().get(topicName);
    }

    @Override
    public NoopTopicSink getNoopTopicSink(String topicName) {
        return NoopTopicFactories.getSinkFactory().get(topicName);
    }

    private IllegalArgumentException paramException(String topicName) {
        return new IllegalArgumentException(
            "Invalid parameter: a communication infrastructure required to fetch " + topicName);
    }

    private void logNoSink(String topicName, Exception ex) {
        logger.debug("No sink for topic: {}", topicName, ex);
    }

}
