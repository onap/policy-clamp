
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

package org.onap.policy.clamp.acm.runtime.supervision;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.common.message.bus.healthcheck.TopicHealthCheck;
import org.onap.policy.common.message.bus.healthcheck.kafka.KafkaHealthCheck;
import org.onap.policy.common.message.bus.healthcheck.noop.NoopHealthCheck;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaStartup {
    private final List<String> topics;
    private final KafkaListenerEndpointRegistry registry;
    private final TopicHealthCheck topicHealthCheck;

    /**
     * Constructor.
     *
     * @param registry the Kafka Registry
     * @param kafkaAdmin the Spring Kafka Admin
     * @param parameterGroup the parameters
     */
    public KafkaStartup(KafkaListenerEndpointRegistry registry, KafkaAdmin kafkaAdmin,
            AcRuntimeParameterGroup parameterGroup) {
        this.registry = registry;
        this.topicHealthCheck = parameterGroup.isKafkaHealthCheck()
                ? new KafkaHealthCheck(kafkaAdmin.getConfigurationProperties()) : new NoopHealthCheck();
        this.topics = parameterGroup.isTopicValidation()
                ? List.of(parameterGroup.getTopics().getOperationTopic(), parameterGroup.getTopics().getSyncTopic())
                : List.of();
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void startListenersWhenReady() {
        runTopicHealthCheck();
        start();
    }

    private void runTopicHealthCheck() {
        var fetchTimeout = 5000;
        while (!topicHealthCheck.healthCheck(topics)) {
            log.debug("Kafka Broker not up yet!");
            AcmUtils.pause(fetchTimeout);
        }
    }

    private void start() {
        log.info("Kafka broker available. Starting listener containers.");
        registry.getListenerContainers().forEach(container -> {
            if (!container.isRunning()) {
                container.start();
            }
        });
    }
}
