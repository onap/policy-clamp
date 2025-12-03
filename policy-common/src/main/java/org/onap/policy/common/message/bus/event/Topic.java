/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2017-2020 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2019 Samsung Electronics Co., Ltd.
 * Copyright (C) 2022,2024 Nordix Foundation.
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

package org.onap.policy.common.message.bus.event;

import java.util.List;
import org.onap.policy.common.capabilities.Lockable;
import org.onap.policy.common.capabilities.Startable;


/**
 * Essential Topic Data.
 */
public interface Topic extends TopicRegisterable, Startable, Lockable {

    /**
     * Underlying Communication infrastructure Types.
     */
    enum CommInfrastructure {
        /**
         * KAFKA Communication Infrastructure.
         */
        KAFKA,
        /**
         * NOOP for internal use only.
         */
        NOOP,
        /**
         * REST Communication Infrastructure.
         */
        REST
    }

    /**
     * Gets the canonical topic name.
     *
     * @return topic name
     */
    String getTopic();

    /**
     * Gets the effective topic that is used in
     * the network communication.  This name is usually
     * the topic name.
     *
     * @return topic name alias
     */
    String getEffectiveTopic();

    /**
     * Gets the communication infrastructure type.
     *
     * @return CommInfrastructure object
     */
    CommInfrastructure getTopicCommInfrastructure();

    /**
     * Return list of servers.
     *
     * @return bus servers
     */
    List<String> getServers();

    /**
     * Get the more recent events in this topic entity.
     *
     * @return array of most recent events
     */
    String[] getRecentEvents();

}
