/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2017-2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.common.utils.network;

import java.net.InetAddress;
import java.net.UnknownHostException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Network Utilities.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NetworkUtil {

    public static final Logger logger = LoggerFactory.getLogger(NetworkUtil.class.getName());

    /**
     * IPv4 Wildcard IP address.
     */
    public static final String IPV4_WILDCARD_ADDRESS = "0.0.0.0";

    /**
     * Gets host name.
     *
     * @return host name
     */
    public static String getHostname() {

        String hostname = System.getenv("HOSTNAME");
        if (hostname != null && !hostname.isEmpty()) {
            return hostname;
        }

        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            logger.warn("cannot resolve local hostname", e);
            /* continue */
        }

        return "localhost";
    }
}
