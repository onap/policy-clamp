/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.sim.utils;

import static org.junit.Assert.assertEquals;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.onap.policy.common.gson.GsonMessageBodyHandler;
import org.onap.policy.common.utils.network.NetworkUtil;

/**
 * Class to perform Rest unit tests.
 *
 */
public class CommonActuatorController {

    public static final String SELF = NetworkUtil.getHostname();
    public static final String CONTEXT_PATH = "onap/policy/clamp/acm/simparticipant/";

    private static String httpPrefix;

    /**
     * Sends a request to an actuator endpoint.
     *
     * @param endpoint the target endpoint
     * @return a request builder
     */
    protected Invocation.Builder sendActRequest(final String endpoint) {
        return sendFqeRequest(httpPrefix + CONTEXT_PATH + endpoint, true);
    }

    /**
     * Sends a request to an actuator endpoint, without any authorization header.
     *
     * @param endpoint the target endpoint
     * @return a request builder
     */
    protected Invocation.Builder sendNoAuthActRequest(final String endpoint) {
        return sendFqeRequest(httpPrefix + CONTEXT_PATH + endpoint, false);
    }

    /**
     * Sends a request to a fully qualified endpoint.
     *
     * @param fullyQualifiedEndpoint the fully qualified target endpoint
     * @param includeAuth if authorization header should be included
     * @return a request builder
     */
    protected Invocation.Builder sendFqeRequest(final String fullyQualifiedEndpoint, boolean includeAuth) {
        final Client client = ClientBuilder.newBuilder().build();

        client.property(ClientProperties.METAINF_SERVICES_LOOKUP_DISABLE, "true");
        client.register(GsonMessageBodyHandler.class);

        if (includeAuth) {
            client.register(HttpAuthenticationFeature.basic("participantUser", "zb!XztG34"));
        }

        final WebTarget webTarget = client.target(fullyQualifiedEndpoint);

        return webTarget.request(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN);
    }

    /**
     * Assert that GET call to actuator endpoint is Unauthorized.
     *
     * @param endPoint the endpoint
     */
    protected void assertUnauthorizedActGet(final String endPoint) {
        Response rawresp = sendNoAuthActRequest(endPoint).buildGet().invoke();
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), rawresp.getStatus());
    }

    /**
     * Set Up httpPrefix.
     *
     * @param port the port
     */
    protected void setHttpPrefix(int port) {
        httpPrefix = "http://" + SELF + ":" + port + "/";
    }

}
