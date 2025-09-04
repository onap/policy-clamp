/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025 Nordix Foundation.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.clamp.common.acm.utils;

import lombok.Getter;
import org.onap.policy.common.message.bus.event.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetLoggerUtil {

    /**
     * Loggers.
     */
    @Getter
    private static final Logger networkLogger = LoggerFactory.getLogger("acm-network");

    /**
     * Constant for the system line separator.
     */
    public static final String SYSTEM_LS = System.lineSeparator();

    /**
     * Specifies if the message is coming in or going out.
     */
    public enum EventType {
        IN, OUT
    }

    /**
     * Logs a message to the network logger.
     *
     * @param type can either be IN or OUT
     * @param protocol the protocol used to receive/send the message
     * @param topic the topic the message came from or null if the type is REST
     * @param message message to be logged
     */
    public static void log(EventType type, Topic.CommInfrastructure protocol, String topic, String message) {
        networkLogger.info("[{}|{}|{}]{}{}", type, protocol, topic, SYSTEM_LS, message);
    }

}
