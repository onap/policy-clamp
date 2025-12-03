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

package org.onap.policy.common.message.bus.event.base;

import java.util.List;
import java.util.Properties;
import org.onap.policy.common.message.bus.event.Topic;
import org.onap.policy.common.parameters.topic.BusTopicParams;

/**
 * Topic Base Factory.
 *
 * @param <T> Type.
 */
public interface TopicBaseFactory<T extends Topic> {

    /**
     * build a TopicBase instance.
     *
     * @param properties properties.
     * @return T instance.
     */
    List<T> build(Properties properties);

    /**
     * build a TopicBase instance.
     *
     * @param servers servers.
     * @param topic topic.
     * @param managed managed.
     * @return T instance.
     */
    T build(List<String> servers, String topic, boolean managed);

    /**
     * Construct an instance of an endpoint.
     *
     * @param param parameters
     * @return an instance of T.
     */
    T build(BusTopicParams param);

    /**
     * destroy TopicBase instance.
     * @param topic topic.
     */
    void destroy(String topic);

    /**
     * destroy.
     */
    void destroy();

    /**
     * get T instance.
     *
     * @param topic topic.
     * @return T instance.
     */
    T get(String topic);

    /**
     * inventory of T instances.
     *
     * @return T instance list.
     */
    List<T> inventory();
}
