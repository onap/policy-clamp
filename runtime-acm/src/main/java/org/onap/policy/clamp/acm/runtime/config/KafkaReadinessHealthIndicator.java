/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.runtime.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.Lifecycle;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.stereotype.Component;

@Component("kafkaReadiness")
@RequiredArgsConstructor
public class KafkaReadinessHealthIndicator implements HealthIndicator {

    private final KafkaListenerEndpointRegistry registry;

    @Override
    public Health health() {
        var allRunning = registry.getListenerContainers().stream()
                .allMatch(Lifecycle::isRunning);

        if (allRunning && !registry.getListenerContainers().isEmpty()) {
            return Health.up().build();
        }
        return Health.down()
                .withDetail("reason", "Kafka listener containers not yet running")
                .build();
    }
}
