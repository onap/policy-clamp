
/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.participant.simulator.main.rest;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.onap.policy.clamp.controlloop.common.ControlLoopConstants;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopException;
import org.onap.policy.clamp.controlloop.participant.simulator.main.parameters.CommonTestData;
import org.onap.policy.clamp.controlloop.participant.simulator.main.startstop.Main;
import org.onap.policy.clamp.controlloop.participant.simulator.main.startstop.ParticipantSimulatorActivator;
import org.onap.policy.common.endpoints.event.comm.TopicEndpointManager;
import org.onap.policy.common.endpoints.http.server.HttpServletServerFactoryInstance;
import org.onap.policy.common.gson.GsonMessageBodyHandler;
import org.onap.policy.common.utils.network.NetworkUtil;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to perform Rest unit tests.
 *
 */

public class CommonParticipantRestServer {

    private static final String CONFIG_FILE = "src/test/resources/parameters/TestConfigParameters.json";
    private static final Logger LOGGER = LoggerFactory.getLogger(CommonParticipantRestServer.class);
    public static final String SELF = NetworkUtil.getHostname();
    public static final String ENDPOINT_PREFIX = "onap/participantsim/v2/";
    private static int port;
    private static String httpPrefix;
    private static Main main;

    /**
     * Allocates a port for the server, writes a config file, and then starts Main.
     *
     * @throws Exception if an error occurs
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        setUpBeforeClass(true);
    }

    /**
     * Allocates a port for the server, writes a config file, and then starts Main, if
     * specified.
     *
     * @param shouldStart {@code true} if Main should be started, {@code false} otherwise
     * @throws Exception if an error occurs
     */
    public static void setUpBeforeClass(boolean shouldStart) throws Exception {
        port = NetworkUtil.allocPort();
        httpPrefix = "http://localhost:" + port + "/";

        makeConfigFile();
        HttpServletServerFactoryInstance.getServerFactory().destroy();
        TopicEndpointManager.getManager().shutdown();

        if (shouldStart) {
            startMain();
        }
    }

    /**
     * Stops Main.
     */
    @AfterClass
    public static void teardownAfterClass() {
        try {
            stopMain();

        } catch (ControlLoopException exp) {
            LOGGER.error("cannot stop main", exp);
        }
    }

    /**
     * Set up.
     *
     * @throws Exception if an error occurs
     */
    @Before
    public void setUp() throws Exception {
        // restart, if not currently running
        if (main == null) {
            startMain();
        }
    }

    /**
     * Verifies that an endpoint appears within the swagger response.
     *
     * @param endpoint the endpoint of interest
     * @throws Exception if an error occurs
     */
    protected void testSwagger(final String endpoint) throws Exception {
        final Invocation.Builder invocationBuilder = sendFqeRequest(httpPrefix + "swagger.yaml", true);
        final String resp = invocationBuilder.get(String.class);
        assertTrue(resp.contains(ENDPOINT_PREFIX + endpoint + ":"));
    }

    /**
     * Makes a parameter configuration file.
     *
     * @throws IOException if an error occurs writing the configuration file
     * @throws FileNotFoundException if an error occurs writing the configuration file
     *
     * @throws Exception if an error occurs
     */
    private static void makeConfigFile() throws FileNotFoundException, IOException {
        String json = CommonTestData.getParticipantParameterGroupAsString(port);
        File file = new File(String.format(CONFIG_FILE, port));
        file.deleteOnExit();
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Starts the "Main".
     *
     * @throws InterruptedException
     *
     * @throws Exception if an error occurs
     */
    protected static void startMain() throws InterruptedException {
        // make sure port is available
        if (NetworkUtil.isTcpPortOpen("localhost", port, 1, 1L)) {
            throw new IllegalStateException("port " + port + " is still in use");
        }

        final String[] configParameters = { "-c", CONFIG_FILE };

        main = new Main(configParameters);

        if (!NetworkUtil.isTcpPortOpen("localhost", port, 6, 10000L)) {
            throw new IllegalStateException("server is not listening on port " + port);
        }
    }

    /**
     * Stops the "Main".
     *
     * @throws ControlLoopException
     *
     * @throws Exception if an error occurs
     */
    private static void stopMain() throws ControlLoopException {
        if (main != null) {
            Main main2 = main;
            main = null;
            main2.shutdown();
        }
    }

    /**
     * Sends a request to an endpoint.
     *
     * @param endpoint the target endpoint
     * @return a request builder
     * @throws Exception if an error occurs
     */
    protected Invocation.Builder sendRequest(final String endpoint) throws Exception {
        return sendFqeRequest(httpPrefix + ENDPOINT_PREFIX + endpoint, true);
    }

    /**
     * Sends a request to an endpoint, without any authorization header.
     *
     * @param endpoint the target endpoint
     * @return a request builder
     * @throws Exception if an error occurs
     */
    protected Invocation.Builder sendNoAuthRequest(final String endpoint) throws Exception {
        return sendFqeRequest(httpPrefix + ENDPOINT_PREFIX + endpoint, false);
    }

    /**
     * Sends a request to a fully qualified endpoint.
     *
     * @param fullyQualifiedEndpoint the fully qualified target endpoint
     * @param includeAuth if authorization header should be included
     * @return a request builder
     * @throws Exception if an error occurs
     */
    protected Invocation.Builder sendFqeRequest(final String fullyQualifiedEndpoint, boolean includeAuth)
            throws Exception {
        final Client client = ClientBuilder.newBuilder().build();
        client.property(ClientProperties.METAINF_SERVICES_LOOKUP_DISABLE, "true");
        client.register(GsonMessageBodyHandler.class);
        if (includeAuth) {
            client.register(HttpAuthenticationFeature.basic("healthcheck", "zb!XztG34"));
        }
        final WebTarget webTarget = client.target(fullyQualifiedEndpoint);
        return webTarget.request(MediaType.APPLICATION_JSON);
    }
}
