/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

import static org.onap.policy.common.message.bus.properties.MessageBusProperties.PROPERTY_NOOP_SOURCE_TOPICS;

import java.util.List;

/**
 * No Operation Topic Source Factory.
 */
public class NoopTopicSourceFactory extends NoopTopicFactory<NoopTopicSource> {

    /**
     * {@inheritDoc}.
     */
    @Override
    protected String getTopicsPropertyName() {
        return PROPERTY_NOOP_SOURCE_TOPICS;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public NoopTopicSource build(List<String> servers, String topic) {
        return new NoopTopicSource(servers, topic);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public String toString() {
        return "NoopTopicSourceFactory [" + super.toString() + "]";
    }
}
