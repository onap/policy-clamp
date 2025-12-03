/*
 * ============LICENSE_START=======================================================
 * policy-utils
 * ================================================================================
 * Copyright (C) 2018-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024 Nordix Foundation
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class NetworkUtilTest {
    protected static Logger logger = LoggerFactory.getLogger(NetworkUtilTest.class);

    private static final String LOCALHOST = "localhost";

    @Test
    void test() throws InterruptedException, IOException {
        assertNotNull(NetworkUtil.IPV4_WILDCARD_ADDRESS);
        assertFalse(NetworkUtil.isTcpPortOpen(LOCALHOST, NetworkUtil.allocPort(), 1, 5));
        assertNotNull(NetworkUtil.getHostname());
        assertNotNull(NetworkUtil.getHostIp());
    }

    @Test
    void testAlwaysTrustManager() throws Exception {
        TrustManager[] mgrarr = NetworkUtil.getAlwaysTrustingManager();
        assertEquals(1, mgrarr.length);
        assertInstanceOf(X509TrustManager.class, mgrarr[0]);

        X509TrustManager mgr = (X509TrustManager) mgrarr[0];
        assertNotNull(mgr.getAcceptedIssuers());
        assertEquals(0, mgr.getAcceptedIssuers().length);

        // these should not throw exceptions
        mgr.checkClientTrusted(null, null);
        mgr.checkServerTrusted(null, null);
    }

    @Test
    void testAllocPort_testAllocPortString__testAllocPortInetSocketAddress() throws Exception {
        // allocate wild-card port
        int wildCardPort = NetworkUtil.allocPort();
        assertNotEquals(0, wildCardPort);

        // verify that we can listen on the port
        try (ServerSocket wildSocket = new ServerSocket(wildCardPort)) {
            new Accepter(wildSocket).start();
            assertTrue(NetworkUtil.isTcpPortOpen(LOCALHOST, wildCardPort, 5, 1000L));
        }


        // allocate port using host name
        int localPort = NetworkUtil.allocPort(LOCALHOST);
        assertNotEquals(0, localPort);

        // the OS should have allocated a new port, even though the first has been closed
        assertNotEquals(wildCardPort, localPort);

        try (ServerSocket localSocket = new ServerSocket()) {
            localSocket.bind(new InetSocketAddress(LOCALHOST, localPort));
            new Accepter(localSocket).start();
            assertTrue(NetworkUtil.isTcpPortOpen(LOCALHOST, localPort, 5, 1000L));
        }
    }

    @Test
    void testGenUniqueName() {
        String name = NetworkUtil.genUniqueName(LOCALHOST);
        assertThat(name).isNotBlank().isNotEqualTo(LOCALHOST);

        // second call should generate a different value
        assertThat(NetworkUtil.genUniqueName(LOCALHOST)).isNotEqualTo(name);
    }

    /**
     * Thread that accepts a connection on a socket.
     */
    private static class Accepter extends Thread {
        private ServerSocket socket;

        public Accepter(ServerSocket socket) {
            this.socket = socket;
            setDaemon(true);
        }

        @Override
        public void run() {
            try (Socket server = socket.accept()) { //NOSONAR
                // do nothing

            } catch (IOException e) {
                logger.error("socket not accepted", e);
            }
        }
    }
}
