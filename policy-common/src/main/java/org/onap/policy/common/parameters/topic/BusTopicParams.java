/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2018 Samsung Electronics Co., Ltd. All rights reserved.
 * Modifications Copyright (C) 2018-2019, 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2019, 2023-2024 Nordix Foundation.
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

package org.onap.policy.common.parameters.topic;

import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

/**
 * Member variables of this Params class are as follows.
 *
 * <p>servers Kafka servers
 * topic Kafka Topic to be monitored
 * apiKey Kafka API Key (optional)
 * apiSecret Kafka API Secret (optional)
 * consumerGroup kafka Reader Consumer Group
 * consumerInstance Kafka Reader Instance
 * fetchTimeout kafka fetch timeout
 * fetchLimit Kafka fetch limit
 * environment DME2 Environment
 * aftEnvironment DME2 AFT Environment
 * partner DME2 Partner
 * latitude DME2 Latitude
 * longitude DME2 Longitude
 * additionalProps Additional properties to pass to DME2
 * useHttps does connection use HTTPS?
 * allowTracing is message tracing allowed?
 * allowSelfSignedCerts are self-signed certificates allow
 */
@Getter
@Setter
public class BusTopicParams {

    private int port;
    private List<String> servers;
    private Map<String, String> additionalProps;
    private String topic;
    private String effectiveTopic;
    private String apiKey;
    private String apiSecret;
    private String consumerGroup;
    private String consumerInstance;
    private int fetchTimeout;
    private int fetchLimit;
    private boolean useHttps;
    private boolean allowTracing;
    private boolean allowSelfSignedCerts;
    private boolean managed;

    private String userName;
    private String password;
    private String environment;
    private String aftEnvironment;
    private String partner;
    private String latitude;
    private String longitude;
    private String partitionId;
    private String clientName;
    private String hostname;
    private String basePath;
    @Getter
    private String serializationProvider;

    public static TopicParamsBuilder builder() {
        return new TopicParamsBuilder();
    }

    /**
     * Methods to Check if the property is INVALID.
     */

    boolean isEnvironmentInvalid() {
        return StringUtils.isBlank(environment);
    }

    boolean isAftEnvironmentInvalid() {
        return StringUtils.isBlank(aftEnvironment);
    }

    boolean isLatitudeInvalid() {
        return StringUtils.isBlank(latitude);
    }

    boolean isLongitudeInvalid() {
        return StringUtils.isBlank(longitude);
    }

    public boolean isConsumerInstanceInvalid() {
        return StringUtils.isBlank(consumerInstance);
    }

    public boolean isConsumerGroupInvalid() {
        return StringUtils.isBlank(consumerGroup);
    }

    public boolean isClientNameInvalid() {
        return StringUtils.isBlank(clientName);
    }

    boolean isPartnerInvalid() {
        return StringUtils.isBlank(partner);
    }

    boolean isServersInvalid() {
        return (servers == null || servers.isEmpty()
            || (servers.size() == 1 && ("".equals(servers.get(0)))));
    }

    public boolean isTopicInvalid() {
        return StringUtils.isBlank(topic);
    }

    public boolean isPartitionIdInvalid() {
        return StringUtils.isBlank(partitionId);
    }

    public boolean isHostnameInvalid() {
        return StringUtils.isBlank(hostname);
    }

    public boolean isPortInvalid() {
        return (getPort() <= 0 || getPort() >= 65535);
    }

    /**
     * Methods to Check if the property is Valid.
     */

    boolean isApiKeyValid() {
        return StringUtils.isNotBlank(apiKey);
    }

    boolean isApiSecretValid() {
        return StringUtils.isNotBlank(apiSecret);
    }

    boolean isUserNameValid() {
        return StringUtils.isNotBlank(userName);
    }

    boolean isPasswordValid() {
        return StringUtils.isNotBlank(password);
    }

    public boolean isAdditionalPropsValid() {
        return additionalProps != null;
    }

    public void setEffectiveTopic(String effectiveTopic) {
        this.effectiveTopic = topicToLowerCase(effectiveTopic);
    }

    public void setTopic(String topic) {
        this.topic = topicToLowerCase(topic);
    }

    public String getEffectiveTopic() {
        return topicToLowerCase(effectiveTopic);
    }

    public String getTopic() {
        return topicToLowerCase(topic);
    }

    private String topicToLowerCase(String topic) {
        return (topic == null || topic.isEmpty()) ? topic : topic.toLowerCase();
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class TopicParamsBuilder {

        final BusTopicParams params = new BusTopicParams();

        public TopicParamsBuilder servers(List<String> servers) {
            this.params.servers = servers;
            return this;
        }

        public TopicParamsBuilder topic(String topic) {
            this.params.setTopic(topic);
            return this;
        }

        public TopicParamsBuilder effectiveTopic(String effectiveTopic) {
            this.params.setEffectiveTopic(effectiveTopic);
            return this;
        }

        public TopicParamsBuilder apiKey(String apiKey) {
            this.params.apiKey = apiKey;
            return this;
        }

        public TopicParamsBuilder apiSecret(String apiSecret) {
            this.params.apiSecret = apiSecret;
            return this;
        }

        public TopicParamsBuilder consumerGroup(String consumerGroup) {
            this.params.consumerGroup = consumerGroup;
            return this;
        }

        public TopicParamsBuilder consumerInstance(String consumerInstance) {
            this.params.consumerInstance = consumerInstance;
            return this;
        }

        public TopicParamsBuilder fetchTimeout(int fetchTimeout) {
            this.params.fetchTimeout = fetchTimeout;
            return this;
        }

        public TopicParamsBuilder fetchLimit(int fetchLimit) {
            this.params.fetchLimit = fetchLimit;
            return this;
        }

        public TopicParamsBuilder useHttps(boolean useHttps) {
            this.params.useHttps = useHttps;
            return this;
        }

        public TopicParamsBuilder allowTracing(boolean allowTracing) {
            this.params.allowTracing = allowTracing;
            return this;
        }

        public TopicParamsBuilder allowSelfSignedCerts(boolean allowSelfSignedCerts) {
            this.params.allowSelfSignedCerts = allowSelfSignedCerts;
            return this;
        }

        public TopicParamsBuilder userName(String userName) {
            this.params.userName = userName;
            return this;
        }

        public TopicParamsBuilder password(String password) {
            this.params.password = password;
            return this;
        }

        public TopicParamsBuilder environment(String environment) {
            this.params.environment = environment;
            return this;
        }

        public TopicParamsBuilder aftEnvironment(String aftEnvironment) {
            this.params.aftEnvironment = aftEnvironment;
            return this;
        }

        public TopicParamsBuilder partner(String partner) {
            this.params.partner = partner;
            return this;
        }

        public TopicParamsBuilder latitude(String latitude) {
            this.params.latitude = latitude;
            return this;
        }

        public TopicParamsBuilder longitude(String longitude) {
            this.params.longitude = longitude;
            return this;
        }

        public TopicParamsBuilder additionalProps(Map<String, String> additionalProps) {
            this.params.additionalProps = additionalProps;
            return this;
        }

        public TopicParamsBuilder partitionId(String partitionId) {
            this.params.partitionId = partitionId;
            return this;
        }

        public BusTopicParams build() {
            return params;
        }

        public TopicParamsBuilder managed(boolean managed) {
            this.params.managed = managed;
            return this;
        }

        public TopicParamsBuilder hostname(String hostname) {
            this.params.hostname = hostname;
            return this;
        }

        public TopicParamsBuilder clientName(String clientName) {
            this.params.clientName = clientName;
            return this;
        }

        public TopicParamsBuilder port(int port) {
            this.params.port = port;
            return this;
        }

        public TopicParamsBuilder basePath(String basePath) {
            this.params.basePath = basePath;
            return this;
        }

        public TopicParamsBuilder serializationProvider(String serializationProvider) {
            this.params.serializationProvider = serializationProvider;
            return this;
        }
    }
}

