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

package org.onap.policy.clamp.acm.participant.intermediary.parameters;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * Kafka connection parameters for ACM participant intermediary.
 *
 * <p>Only truly structural settings (bootstrap servers, consumer group) and operationally
 * critical settings (auth retry) are typed fields.
 * All other Kafka client properties (security, tuning, etc.) go through the generic
 * {@code properties} maps, following the same convention as Spring Boot's
 * {@code spring.kafka.properties.*}, {@code spring.kafka.consumer.properties.*},
 * and {@code spring.kafka.producer.properties.*}.
 */
@Getter
@Setter
public class KafkaParameters {

    @NotBlank
    private String bootstrapServers;

    /**
     * Interval between retries when an authentication exception occurs on the consumer.
     * Prevents tight-loop restarts when Kafka with auth is not yet available (e.g., fresh k8s install).
     */
    @NotNull
    private Duration authExceptionRetryInterval = Duration.ofSeconds(5);

    /**
     * Additional properties applied to both consumer and producer.
     * Equivalent to spring.kafka.properties.*.
     */
    private Map<String, String> properties = new HashMap<>();

    /**
     * Consumer-specific configuration.
     */
    @NotNull
    @Valid
    private ConsumerParameters consumer = new ConsumerParameters();

    /**
     * Producer-specific configuration.
     */
    @NotNull
    @Valid
    private ProducerParameters producer = new ProducerParameters();

    @Getter
    @Setter
    public static class ConsumerParameters {
        /**
         * Consumer group ID for the operation topic.
         * The sync topic uses a randomized group ID for broadcast semantics.
         */
        @NotBlank
        private String groupId;

        /**
         * Additional consumer-specific Kafka properties.
         * Equivalent to spring.kafka.consumer.properties.*.
         */
        private Map<String, String> properties = new HashMap<>();
    }

    @Getter
    @Setter
    public static class ProducerParameters {
        /**
         * Additional producer-specific Kafka properties.
         * Equivalent to spring.kafka.producer.properties.*.
         */
        private Map<String, String> properties = new HashMap<>();
    }
}
