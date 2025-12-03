/*-
 * ============LICENSE_START===============================================
 * Copyright (C) 2024 Nordix Foundation.
 * ========================================================================
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
 * ============LICENSE_END=================================================
 */

package org.onap.policy.common.message.bus.properties;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MessageBusProperties {

    /* Generic property suffixes */

    public static final String PROPERTY_TOPIC_SERVERS_SUFFIX = ".servers";
    public static final String PROPERTY_TOPIC_EFFECTIVE_TOPIC_SUFFIX = ".effectiveTopic";

    public static final String PROPERTY_TOPIC_SOURCE_CONSUMER_GROUP_SUFFIX = ".consumerGroup";
    public static final String PROPERTY_TOPIC_SOURCE_CONSUMER_INSTANCE_SUFFIX = ".consumerInstance";
    public static final String PROPERTY_TOPIC_SOURCE_FETCH_TIMEOUT_SUFFIX = ".fetchTimeout";
    public static final String PROPERTY_TOPIC_SOURCE_FETCH_LIMIT_SUFFIX = ".fetchLimit";
    public static final String PROPERTY_MANAGED_SUFFIX = ".managed";
    public static final String PROPERTY_ADDITIONAL_PROPS_SUFFIX = ".additionalProps";

    public static final String PROPERTY_TOPIC_SINK_PARTITION_KEY_SUFFIX = ".partitionKey";

    public static final String PROPERTY_ALLOW_SELF_SIGNED_CERTIFICATES_SUFFIX = ".selfSignedCertificates";

    public static final String PROPERTY_NOOP_SOURCE_TOPICS = "noop.source.topics";
    public static final String PROPERTY_NOOP_SINK_TOPICS = "noop.sink.topics";

    /* KAFKA Properties */

    public static final String PROPERTY_KAFKA_SOURCE_TOPICS = "kafka.source.topics";
    public static final String PROPERTY_KAFKA_SINK_TOPICS = "kafka.sink.topics";

    /* HTTP Server Properties */

    public static final String PROPERTY_HTTP_HTTPS_SUFFIX = ".https";

    /* Topic Sink Values */

    /* Topic Source values */

    /**
     * Default Timeout fetching in milliseconds.
     */
    public static final int DEFAULT_TIMEOUT_MS_FETCH = 15000;

    /**
     * Default maximum number of messages fetch at the time.
     */
    public static final int DEFAULT_LIMIT_FETCH = 100;

    /**
     * Definition of No Timeout fetching.
     */
    public static final int NO_TIMEOUT_MS_FETCH = -1;

    /**
     * Definition of No limit fetching.
     */
    public static final int NO_LIMIT_FETCH = -1;
}
