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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.retry.RetryListener;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryState;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.core.retry.Retryable;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaStartup {

    private final KafkaListenerEndpointRegistry registry;

    /**
     * Run Kafka with RetryTemplate support.
     */
    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void startListenersWhenReady() {
        var retry = new RetryTemplate(RetryPolicy.builder().backOff(new FixedBackOff()).build());
        retry.setRetryListener(new RetryListener() {
            @Override
            public void onRetryableExecution(RetryPolicy retryPolicy, Retryable<?> retryable, RetryState retryState) {
                checkFailed(retryState);
            }
        });
        retry.invoke(this::execute);
    }

    protected void checkFailed(RetryState retryState) {
        if (!retryState.isSuccessful()) {
            log.error("Kafka connection failed {}", retryState.getLastException().getMessage());
            registry.getListenerContainers().stream()
                    .filter(MessageListenerContainer::isRunning)
                    .forEach(MessageListenerContainer::stop);
        }
    }

    private void execute() {
        log.info("Start Kafka connection");
        registry.getListenerContainers().stream()
                .filter(container -> !container.isRunning())
                .forEach(MessageListenerContainer::start);
        log.info("Kafka connection started");
    }
}
