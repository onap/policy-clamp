/*
 * ============LICENSE_START=======================================================
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

package org.onap.policy.common.message.bus.utils;

import lombok.Getter;
import org.onap.policy.common.message.bus.event.Topic.CommInfrastructure;
import org.onap.policy.common.message.bus.features.NetLoggerFeatureProviders;
import org.onap.policy.common.utils.services.FeatureApiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A network logging utility class that allows drools applications code to access the
 * network log (or other specified loggers) and logging features.
 *
 */
public class NetLoggerUtil {

    /**
     * Loggers.
     */
    private static final Logger logger = LoggerFactory.getLogger(NetLoggerUtil.class);
    @Getter
    private static final Logger networkLogger = LoggerFactory.getLogger("network");

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
    public static void log(EventType type, CommInfrastructure protocol, String topic, String message) {
        log(networkLogger, type, protocol, topic, message);
    }

    /**
     * Logs a message to the specified logger (i.e. a controller logger).
     *
     * @param eventLogger the logger that will have the message appended
     * @param type can either be IN or OUT
     * @param protocol the protocol used to receive/send the message
     * @param topic the topic the message came from or null if the type is REST
     * @param message message to be logged
     */
    public static void log(Logger eventLogger, EventType type, CommInfrastructure protocol, String topic,
                    String message) {
        if (eventLogger == null) {
            logger.debug("the logger is null, defaulting to network logger");
            eventLogger = networkLogger;
        }

        if (featureBeforeLog(eventLogger, type, protocol, topic, message)) {
            return;
        }

        eventLogger.info("[{}|{}|{}]{}{}", type, protocol, topic, SYSTEM_LS, message);

        featureAfterLog(eventLogger, type, protocol, topic, message);
    }

    /**
     * Executes features that pre-process a message before it is logged.
     *
     * @param eventLogger the logger that will have the message appended
     * @param type can either be IN or OUT
     * @param protocol the protocol used to receive/send the message
     * @param topic the topic the message came from or null if the type is REST
     * @param message message to be logged
     *
     * @return true if this feature intercepts and takes ownership of the operation
     *         preventing the invocation of lower priority features. False, otherwise
     */
    private static boolean featureBeforeLog(Logger eventLogger, EventType type, CommInfrastructure protocol,
                    String topic, String message) {

        return FeatureApiUtils.apply(NetLoggerFeatureProviders.getProviders().getList(),
            feature -> feature.beforeLog(eventLogger, type, protocol, topic, message),
            (feature, ex) -> logger.error("feature {} before-log failure because of {}",
                            feature.getClass().getName(), ex.getMessage(), ex));
    }

    /**
     * Executes features that post-process a message after it is logged.
     *
     * @param eventLogger the logger that will have the message appended
     * @param type can either be IN or OUT
     * @param protocol the protocol used to receive/send the message
     * @param topic the topic the message came from or null if the type is rest
     * @param message message to be logged
     *
     * @return true if this feature intercepts and takes ownership of the operation
     *         preventing the invocation of lower priority features. False, otherwise
     */
    private static boolean featureAfterLog(Logger eventLogger, EventType type, CommInfrastructure protocol,
                    String topic, String message) {

        return FeatureApiUtils.apply(NetLoggerFeatureProviders.getProviders().getList(),
            feature -> feature.afterLog(eventLogger, type, protocol, topic, message),
            (feature, ex) -> logger.error("feature {} after-log failure because of {}",
                            feature.getClass().getName(), ex.getMessage(), ex));
    }

}
