/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.clamp.common.acm.kafka;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaAdmin;

/**
 * Abstract base for Kafka lifecycle management.
 *
 * <p>Waits for broker and required topics to become available before starting
 * listener containers, with a configurable timeout. Subclasses override
 * {@link #onContainersStarted()} and {@link #onContainersStopping()} for
 * module-specific behavior such as sending registration or status request messages.
 */
@Slf4j
public abstract class AbstractKafkaLifecycle implements SmartLifecycle {

    private static final Duration TOPIC_CHECK_RETRY_INTERVAL = Duration.ofSeconds(5);
    private static final Duration TOPIC_CHECK_TIMEOUT = Duration.ofSeconds(120);

    private final KafkaListenerEndpointRegistry registry;
    private final KafkaAdmin kafkaAdmin;
    private final String operationTopic;
    private final String syncTopic;
    private final List<String> listenerContainerIds;

    private volatile boolean running;

    protected AbstractKafkaLifecycle(KafkaListenerEndpointRegistry registry, KafkaAdmin kafkaAdmin,
            String operationTopic, String syncTopic, String... listenerContainerIds) {
        this.registry = registry;
        this.kafkaAdmin = kafkaAdmin;
        this.operationTopic = operationTopic;
        this.syncTopic = syncTopic;
        this.listenerContainerIds = List.of(listenerContainerIds);
    }

    @Override
    public void start() {
        waitForTopics();
        startListenerContainers();
        onContainersStarted();
        running = true;
        log.info("KafkaLifecycle started");
    }

    @Override
    public void stop() {
        onContainersStopping();
        stopListenerContainers();
        running = false;
        log.info("KafkaLifecycle stopped");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 50;
    }

    /**
     * Called after listener containers are started. Subclasses can send initial
     * messages (e.g. participant registration, status request).
     */
    protected void onContainersStarted() {
        // default no-op
    }

    /**
     * Called before listener containers are stopped. Subclasses can send shutdown
     * messages (e.g. participant deregistration).
     */
    protected void onContainersStopping() {
        // default no-op
    }

    private void startListenerContainers() {
        listenerContainerIds.forEach(id -> registry.getListenerContainer(id).start());
        log.info("Kafka listener containers started: {}", listenerContainerIds);
    }

    private void stopListenerContainers() {
        listenerContainerIds.forEach(id -> registry.getListenerContainer(id).stop());
        log.info("Kafka listener containers stopped: {}", listenerContainerIds);
    }

    private void waitForTopics() {
        var deadline = Instant.now().plus(TOPIC_CHECK_TIMEOUT);
        while (!topicsExist()) {
            if (Instant.now().isAfter(deadline)) {
                throw new IllegalStateException("Kafka topics [" + operationTopic + ", " + syncTopic + "]"
                        + " not available after " + TOPIC_CHECK_TIMEOUT.toSeconds() + "s");
            }
            log.warn("Kafka topics [{}, {}] not available, retrying in {}s",
                    operationTopic, syncTopic, TOPIC_CHECK_RETRY_INTERVAL.toSeconds());
            pause(TOPIC_CHECK_RETRY_INTERVAL);
        }
        log.info("Kafka topics [{}, {}] available", operationTopic, syncTopic);
    }

    private boolean topicsExist() {
        try {
            kafkaAdmin.describeTopics(operationTopic, syncTopic);
            return true;
        } catch (Exception e) {
            log.debug("Topic check failed: {}", e.getMessage());
            return false;
        }
    }

    private void pause(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for Kafka topics", e);
        }
    }
}
