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

package org.onap.policy.clamp.acm.participant.intermediary.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.onap.policy.clamp.acm.participant.intermediary.parameters.ParticipantParameters;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantKafkaMessage;
import org.onap.policy.clamp.models.acm.utils.serialization.ParticipantMessageDeserializer;
import org.onap.policy.clamp.models.acm.utils.serialization.ParticipantMessageSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;

/**
 * Kafka configuration for ACM participant intermediary.
 *
 * <p>Creates qualified beans that are isolated from any Spring Boot Kafka auto-configuration.
 * Downstream participants may still use {@code spring.kafka.*} for their own purposes
 * (e.g., KafkaAdmin topic creation) without conflict.
 *
 * <p>Serializers and deserializers are set programmatically — they are never exposed
 * as configurable properties to downstream consumers.
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    /**
     * Consumer factory for ACM messaging. Key: String, Value: ParticipantKafkaMessage.
     * Uses ErrorHandlingDeserializer wrapping ParticipantMessageDeserializer.
     */
    @Bean("acmConsumerFactory")
    public ConsumerFactory<String, ParticipantKafkaMessage> acmConsumerFactory(
            ParticipantParameters participantParameters) {
        var kafka = participantParameters.getIntermediaryParameters().getKafka();
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        config.put(ConsumerConfig.GROUP_ID_CONFIG, kafka.getConsumer().getGroupId());

        // Shared additional properties (security, etc.)
        config.putAll(kafka.getProperties());
        // Consumer-specific additional properties
        config.putAll(kafka.getConsumer().getProperties());

        // Serialization - set last to prevent override via properties maps
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, ParticipantMessageDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(config);
    }

    /**
     * Producer factory for ACM messaging. Key: String, Value: ParticipantKafkaMessage.
     */
    @Bean("acmProducerFactory")
    public ProducerFactory<String, ParticipantKafkaMessage> acmProducerFactory(
            ParticipantParameters participantParameters) {
        var kafka = participantParameters.getIntermediaryParameters().getKafka();
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());

        // Shared additional properties (security, etc.)
        config.putAll(kafka.getProperties());
        // Producer-specific additional properties
        config.putAll(kafka.getProducer().getProperties());

        // Serialization - set last to prevent override via properties maps
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ParticipantMessageSerializer.class);

        return new DefaultKafkaProducerFactory<>(config);
    }

    /**
     * KafkaTemplate for ACM messaging.
     */
    @Bean("acmKafkaTemplate")
    public KafkaTemplate<String, ParticipantKafkaMessage> acmKafkaTemplate(
            @Qualifier("acmProducerFactory") ProducerFactory<String, ParticipantKafkaMessage> acmProducerFactory) {
        var template = new KafkaTemplate<>(acmProducerFactory);
        template.setObservationEnabled(true);
        return template;
    }

    /**
     * Listener container factory for ACM messaging.
     * Used by {@code @KafkaListener} annotations via {@code containerFactory = "acmListenerContainerFactory"}.
     */
    @Bean("acmListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, ParticipantKafkaMessage> acmListenerContainerFactory(
            @Qualifier("acmConsumerFactory") ConsumerFactory<String, ParticipantKafkaMessage> acmConsumerFactory,
            ParticipantParameters participantParameters) {
        var kafka = participantParameters.getIntermediaryParameters().getKafka();
        var factory = new ConcurrentKafkaListenerContainerFactory<String, ParticipantKafkaMessage>();
        factory.setConsumerFactory(acmConsumerFactory);
        factory.getContainerProperties().setObservationEnabled(true);
        factory.getContainerProperties().setAuthExceptionRetryInterval(kafka.getAuthExceptionRetryInterval());
        return factory;
    }
}
