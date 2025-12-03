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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.UUID;
import javax.net.ssl.TrustManager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.net.util.TrustManagerUtils;
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
     * A trust manager that always trusts certificates.
     */
    private static final TrustManager[] ALWAYS_TRUST_MANAGER = { TrustManagerUtils.getAcceptAllTrustManager() };

    /**
     * Allocates an available port on which a server may listen.
     *
     * @return an available port
     * @throws IOException if a socket cannot be created
     */
    public static int allocPort() throws IOException {
        return allocPort((InetSocketAddress) null);
    }

    /**
     * Allocates an available port on which a server may listen.
     *
     * @param hostName the server's host name
     * @return an available port
     * @throws IOException if a socket cannot be created
     */
    public static int allocPort(String hostName) throws IOException {
        return allocPort(new InetSocketAddress(hostName, 0));
    }

    /**
     * Allocates an available port on which a server may listen.
     *
     * @param hostAddr the server's host address on which to listen
     * @return an available port
     * @throws IOException if a socket cannot be created
     */
    public static int allocPort(InetSocketAddress hostAddr) throws IOException {
        /*
         * The socket is only used to find an unused address for a new server. As a
         * result, it poses no security risk, thus the sonar issue can be ignored.
         */
        try (ServerSocket socket = new ServerSocket()) {    // NOSONAR
            socket.bind(hostAddr);

            return socket.getLocalPort();
        }
    }

    /**
     * Gets a trust manager that accepts all certificates.
     *
     * @return a trust manager that accepts all certificates
     */
    public static TrustManager[] getAlwaysTrustingManager() {
        return ALWAYS_TRUST_MANAGER;
    }

    /**
     * try to connect to $host:$port $retries times while we are getting connection failures.
     *
     * @param host host
     * @param port port
     * @param retries number of attempts
     * @return true is port is open, false otherwise
     * @throws InterruptedException if execution has been interrupted
     */
    public static boolean isTcpPortOpen(String host, int port, int retries, long interval)
            throws InterruptedException {
        var retry = 0;
        while (retry < retries) {
            /*
             * As with the server socket, this is only used to see if the port is open,
             * thus the sonar issue can be ignored.
             */
            try (Socket s = new Socket(host, port)) {   // NOSONAR
                logger.debug("{}:{} connected - retries={} interval={}", host, port, retries, interval);
                return true;
            } catch (final IOException e) {
                retry++;
                logger.trace("{}:{} connected - retries={} interval={}", host, port, retries, interval, e);
                Thread.sleep(interval);
            }
        }

        logger.warn("{}:{} closed = retries={} interval={}", host, port, retries, interval);
        return false;
    }

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

    /**
     * Gets host's IP.
     *
     * @return host IP
     */
    public static String getHostIp() {

        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            logger.warn("cannot resolve local hostname", e);
            /* continue */
        }

        return "127.0.0.1";
    }

    /**
     * Generates a globally unique name, typically for use in PDP messages, to uniquely
     * identify a PDP (or PAP), regardless on what cluster it resides.
     *
     * @param prefix text to be prepended to the generated value
     * @return a globally unique name
     */
    public static String genUniqueName(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
