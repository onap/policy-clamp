/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2022-2024 Nordix Foundation.
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

package org.onap.policy.common.message.bus.utils;

import static org.onap.policy.common.message.bus.properties.MessageBusProperties.PROPERTY_ADDITIONAL_PROPS_SUFFIX;
import static org.onap.policy.common.message.bus.properties.MessageBusProperties.PROPERTY_MANAGED_SUFFIX;
import static org.onap.policy.common.message.bus.properties.MessageBusProperties.PROPERTY_TOPIC_EFFECTIVE_TOPIC_SUFFIX;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.re2j.Pattern;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.onap.policy.common.parameters.topic.BusTopicParams;
import org.onap.policy.common.parameters.topic.BusTopicParams.TopicParamsBuilder;
import org.onap.policy.common.utils.properties.PropertyUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class KafkaPropertyUtils {
    private static final Pattern COMMA_SPACE_PAT = Pattern.compile("\\s*,\\s*");

    /**
     * Makes a topic builder, configuring it with properties that are common to both
     * sources and sinks.
     *
     * @param props   properties to be used to configure the builder
     * @param topic   topic being configured
     * @param servers target servers
     * @return a topic builder
     */
    public static TopicParamsBuilder makeBuilder(PropertyUtils props, String topic, String servers) {

        final List<String> serverList = new ArrayList<>(Arrays.asList(COMMA_SPACE_PAT.split(servers)));
        return BusTopicParams.builder()
            .servers(serverList)
            .topic(topic)
            .effectiveTopic(props.getString(PROPERTY_TOPIC_EFFECTIVE_TOPIC_SUFFIX, topic))
            .managed(props.getBoolean(PROPERTY_MANAGED_SUFFIX, true))
            .additionalProps(getAdditionalProps(props.getString(PROPERTY_ADDITIONAL_PROPS_SUFFIX, "")));
    }

    private static Map<String, String> getAdditionalProps(String additionalPropsString) {
        try {
            Map<String, String> additionalProps = new HashMap<>();
            var converted = new ObjectMapper().readValue(additionalPropsString, Map.class);
            converted.forEach((k, v) -> {
                if (k instanceof String key && v instanceof String value) {
                    additionalProps.put(key, value);
                }
            });
            return additionalProps;
        } catch (Exception e) {
            return Collections.emptyMap();
        }

    }
}
