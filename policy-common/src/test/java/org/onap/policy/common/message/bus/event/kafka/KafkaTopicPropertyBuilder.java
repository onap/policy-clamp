/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022, 2024 Nordix Foundation.
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

package org.onap.policy.common.message.bus.event.kafka;

import static org.onap.policy.common.message.bus.event.base.TopicTestBase.MY_EFFECTIVE_TOPIC;
import static org.onap.policy.common.message.bus.event.base.TopicTestBase.MY_PARTITION;
import static org.onap.policy.common.message.bus.properties.MessageBusProperties.PROPERTY_HTTP_HTTPS_SUFFIX;
import static org.onap.policy.common.message.bus.properties.MessageBusProperties.PROPERTY_MANAGED_SUFFIX;
import static org.onap.policy.common.message.bus.properties.MessageBusProperties.PROPERTY_TOPIC_EFFECTIVE_TOPIC_SUFFIX;
import static org.onap.policy.common.message.bus.properties.MessageBusProperties.PROPERTY_TOPIC_SERVERS_SUFFIX;
import static org.onap.policy.common.message.bus.properties.MessageBusProperties.PROPERTY_TOPIC_SINK_PARTITION_KEY_SUFFIX;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.onap.policy.common.message.bus.event.base.TopicPropertyBuilder;
import org.onap.policy.common.parameters.topic.TopicParameters;

@Getter
public class KafkaTopicPropertyBuilder extends TopicPropertyBuilder {

    public static final String SERVER = "localhost:9092";
    public static final String TOPIC2 = "my-topic-2";
    public static final String ADDITIONAL_PROPS = "{\"security.protocol\": \"SASL_PLAINTEXT\","
        + "\"sasl.mechanism\": \"SCRAM-SHA-512\",\"sasl.jaas.config\": "
        + "\"org.apache.kafka.common.security.plain.PlainLoginModule "
        + "required username=abc password=abc serviceName=kafka;\"}";

    private final TopicParameters params = new TopicParameters();

    /**
     * Constructs the object.
     *
     * @param prefix the prefix for the properties to be built
     */
    public KafkaTopicPropertyBuilder(String prefix) {
        super(prefix);
    }

    /**
     * Adds a topic and configures it's properties with default values.
     *
     * @param topic the topic to be added
     * @return this builder
     */
    public KafkaTopicPropertyBuilder makeTopic(String topic) {
        addTopic(topic);

        setTopicProperty(PROPERTY_TOPIC_EFFECTIVE_TOPIC_SUFFIX, MY_EFFECTIVE_TOPIC);
        setTopicProperty(PROPERTY_MANAGED_SUFFIX, "true");
        setTopicProperty(PROPERTY_HTTP_HTTPS_SUFFIX, "true");
        setTopicProperty(PROPERTY_TOPIC_SINK_PARTITION_KEY_SUFFIX, MY_PARTITION);
        setTopicProperty(PROPERTY_TOPIC_SERVERS_SUFFIX, SERVER);
        setTopicProperty(".additionalProps", ADDITIONAL_PROPS);

        params.setTopicCommInfrastructure("kafka");
        params.setTopic(topic);
        params.setEffectiveTopic(MY_EFFECTIVE_TOPIC);
        params.setManaged(true);
        params.setUseHttps(true);
        params.setPartitionId(MY_PARTITION);
        params.setServers(List.of(SERVER));
        params.setAdditionalProps(getAdditionalProps());

        return this;
    }

    private Map<String, String> getAdditionalProps() {
        try {
            return new ObjectMapper().readValue(ADDITIONAL_PROPS, Map.class);
        } catch (JsonProcessingException e) {
            return Collections.emptyMap();
        }
    }
}
