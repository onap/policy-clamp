/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024 Nordix Foundation.
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

package org.onap.policy.common.message.bus.event.noop;

import static org.onap.policy.common.message.bus.properties.MessageBusProperties.PROPERTY_MANAGED_SUFFIX;
import static org.onap.policy.common.message.bus.properties.MessageBusProperties.PROPERTY_TOPIC_SERVERS_SUFFIX;

import com.google.re2j.Pattern;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.onap.policy.common.message.bus.event.Topic.CommInfrastructure;
import org.onap.policy.common.message.bus.event.base.TopicBaseHashedFactory;

/**
 * Noop Topic Factory.
 */
public abstract class NoopTopicFactory<T extends NoopTopicEndpoint> extends TopicBaseHashedFactory<T> {
    private static final Pattern COMMA_SPACE_PAT = Pattern.compile("\\s*,\\s*");

    /**
     * Get Topics Property Name.
     *
     * @return property name.
     */
    protected abstract String getTopicsPropertyName();

    /**
     * {@inheritDoc}.
     */
    @Override
    protected List<String> getTopicNames(Properties properties) {
        String topics = properties.getProperty(getTopicsPropertyName());
        if (topics == null || topics.isEmpty()) {
            return new ArrayList<>();
        }

        return Arrays.asList(COMMA_SPACE_PAT.split(topics));
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    protected List<String> getServers(String topicName, Properties properties) {
        String servers =
            properties.getProperty(getTopicsPropertyName() + "." + topicName
                + PROPERTY_TOPIC_SERVERS_SUFFIX);

        if (servers == null || servers.isEmpty()) {
            servers = CommInfrastructure.NOOP.toString();
        }

        return new ArrayList<>(Arrays.asList(COMMA_SPACE_PAT.split(servers)));
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    protected boolean isManaged(String topicName, Properties properties) {
        var managedString =
            properties.getProperty(getTopicsPropertyName() + "." + topicName + PROPERTY_MANAGED_SUFFIX);

        var managed = true;
        if (managedString != null && !managedString.isEmpty()) {
            managed = Boolean.parseBoolean(managedString);
        }

        return managed;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public T build(List<String> serverList, String topic, boolean managed) {
        List<String> servers;
        if (serverList == null || serverList.isEmpty()) {
            servers = Collections.singletonList(CommInfrastructure.NOOP.toString());
        } else {
            servers = serverList;
        }

        return super.build(servers, topic, managed);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public String toString() {
        return "NoopTopicFactory[ " + super.toString() + " ]";
    }
}

