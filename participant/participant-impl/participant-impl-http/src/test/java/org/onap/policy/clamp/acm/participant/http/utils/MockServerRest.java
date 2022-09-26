/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.http.utils;

import org.onap.policy.common.endpoints.http.server.HttpServletServer;
import org.onap.policy.common.endpoints.http.server.HttpServletServerFactoryInstance;
import org.onap.policy.common.gson.GsonMessageBodyHandler;

/**
 * The Class MockServerRest that manages test servers for REST requests for the test.
 */
public class MockServerRest implements AutoCloseable {
    private static final String HOST = "localhost";
    private HttpServletServer restServer;

    /**
     * Instantiates a new REST simulator.
     */
    public MockServerRest(int restServerPort) {
        restServer = HttpServletServerFactoryInstance.getServerFactory().build("MockRestEndpoint", false, HOST,
                restServerPort, "/", false, false);
        restServer.addServletClass(null, MockRestEndpoint.class.getName());
        restServer.setSerializationProvider(GsonMessageBodyHandler.class.getName());
        restServer.start();
    }

    @Override
    public void close() throws Exception {
        if (restServer != null) {
            restServer.stop();
            restServer = null;
        }
    }
}
