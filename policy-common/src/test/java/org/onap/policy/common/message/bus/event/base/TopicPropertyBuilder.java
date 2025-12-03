/*
 * ============LICENSE_START=======================================================
 * ONAP Policy Engine - Common Modules
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.common.message.bus.event.base;

import java.util.Properties;

/**
 * Builder of properties used when configuring topics.
 */
public abstract class TopicPropertyBuilder {
    private final Properties properties = new Properties();
    private final String prefix;
    private String topicPrefix;

    /**
     * Constructs the object.
     *
     * @param prefix the prefix for the properties to be built
     */
    public TopicPropertyBuilder(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Constructs the properties from the builder.
     *
     * @return a copy of the properties
     */
    public Properties build() {
        Properties props = new Properties();
        props.putAll(properties);

        return props;
    }

    /**
     * Adds a topic to the list of topics, configuring all of its properties with default
     * values.
     * 
     * @param topic the topic to be added
     * @return this builder
     */
    public abstract TopicPropertyBuilder makeTopic(String topic);

    /**
     * Adds a topic to the list of topics. Also sets the current topic so that subsequent
     * invocations of property methods will manipulate the topic's properties.
     *
     * @param topic the topic to be added
     * @return this builder
     */
    public TopicPropertyBuilder addTopic(String topic) {
        // add topic to the list of topics
        String topicList = properties.getProperty(prefix);
        if (topicList == null || topicList.isEmpty()) {
            topicList = topic;
        } else {
            topicList += "," + topic;
        }

        properties.setProperty(prefix, topicList);

        setTopic(topic);

        return this;
    }

    /**
     * Sets the topic for which subsequent properties will be managed.
     *
     * @param topic the topic
     * @return this builder
     */
    public TopicPropertyBuilder setTopic(String topic) {
        this.topicPrefix = prefix + "." + topic;
        return this;
    }

    /**
     * Sets a topic's property.
     *
     * @param name name of the property
     * @param value value to which the property should be set
     * @return this builder
     */
    public TopicPropertyBuilder setTopicProperty(String name, Object value) {
        properties.setProperty(topicPrefix + name, value.toString());
        return this;
    }

    /**
     * Removes a topic's property.
     *
     * @param name name of the property
     * @return this builder
     */
    public TopicPropertyBuilder removeTopicProperty(String name) {
        properties.remove(topicPrefix + name);
        return this;
    }
}

