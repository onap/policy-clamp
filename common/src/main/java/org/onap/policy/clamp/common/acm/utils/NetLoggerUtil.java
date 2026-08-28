/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025-2026 OpenInfra Foundation Europe. All rights reserved.
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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
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
     * Logs a received message to the network logger at INFO level.
     *
     * @param protocol the protocol used to receive the message
     * @param topic the topic the message came from
     * @param message message to be logged
     */
    public static void logIncoming(String protocol, String topic, String message) {
        networkLogger.info("[IN|{}|{}]{}{}", protocol, topic, SYSTEM_LS, message);
    }

    /**
     * Logs a sent message to the network logger.
     * Success is logged at INFO level, failure at ERROR level.
     *
     * @param protocol the protocol used to send the message
     * @param topic the topic the message was sent to
     * @param messageType the type of message being sent
     * @param message message to be logged
     * @param delivered whether the message was delivered successfully
     */
    public static void logOutgoing(String protocol, String topic, String messageType,
            String message, boolean delivered) {
        if (delivered) {
            networkLogger.info("[OUT|{}|{}] Sent {}{}{}", protocol, topic, messageType, SYSTEM_LS, message);
        } else {
            networkLogger.error("[OUT|{}|{}] Failed to deliver {}{}{}", protocol, topic, messageType,
                    SYSTEM_LS, message);
        }
    }

}
