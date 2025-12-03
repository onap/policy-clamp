/*
 * ============LICENSE_START=======================================================
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

package org.onap.policy.common.message.bus.features;

import org.onap.policy.common.message.bus.event.Topic.CommInfrastructure;
import org.onap.policy.common.message.bus.utils.NetLoggerUtil.EventType;
import org.onap.policy.common.utils.services.OrderedService;
import org.slf4j.Logger;

/**
 * Logging Feature API. Provides interception points before and after logging a message.
 */
public interface NetLoggerFeatureApi extends OrderedService {

    /**
     * Intercepts a message before it is logged.
     *
     * @return true if this feature intercepts and takes ownership of the operation
     *         preventing the invocation of lower priority features. False, otherwise.
     */
    default boolean beforeLog(Logger eventLogger, EventType type, CommInfrastructure protocol, String topic,
                    String message) {
        return false;
    }

    /**
     * Intercepts a message after it is logged.
     *
     * @return true if this feature intercepts and takes ownership of the operation
     *         preventing the invocation of lower priority features. False, otherwise.
     */
    default boolean afterLog(Logger eventLogger, EventType type, CommInfrastructure protocol, String topic,
                    String message) {
        return false;
    }

}
