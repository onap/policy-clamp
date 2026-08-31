
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

import lombok.extern.slf4j.Slf4j;
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaStartup {
    private final KafkaListenerEndpointRegistry registry;
    private final boolean enableTopicValidation;
    private final KafkaAdmin kafkaAdmin;
    private final String operationTopic;
    private final String syncTopic;

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
        this.kafkaAdmin = kafkaAdmin;
        this.enableTopicValidation = parameterGroup.isTopicValidation();
        this.operationTopic = parameterGroup.getTopics().getOperationTopic();
        this.syncTopic = parameterGroup.getTopics().getSyncTopic();
    }

    /**
     * Run Topic HealthCheck and start Kafka configuration.
     */
    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void startListenersWhenReady() {
        if (enableTopicValidation) {
            waitForTopics();
        }
        start();
    }

    private void waitForTopics() {
        var fetchTimeout = 5000;
        while (!topicsExist()) {
            log.warn("Kafka topics [{}, {}] not available, retrying in {}ms",
                    operationTopic, syncTopic, fetchTimeout);
            AcmUtils.pause(fetchTimeout);
        }
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

    private void start() {
        log.info("Kafka broker available. Starting listener containers.");
        registry.getListenerContainers().forEach(MessageListenerContainer::start);
    }

}
