/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation.
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

package org.onap.policy.clamp.acm.runtime.util.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
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
public class CommonRestController {

    public static final String SELF = NetworkUtil.getHostname();
    public static final String CONTEXT_PATH = "onap/policy/clamp/acm";
    public static final String ENDPOINT_PREFIX = CONTEXT_PATH + "/v2/";
    public static final String ACTUATOR_ENDPOINT = CONTEXT_PATH + "/";

    private static String httpPrefix;
    protected Client client;

    /**
     * Verifies that an endpoint appears within the swagger response.
     *
     * @param endpoint the endpoint of interest
     */
    protected void testSwagger(final String endpoint) {
        final var invocationBuilder = sendActRequest();
        final var resp = invocationBuilder.get(String.class);

        assertThat(resp).contains(endpoint);
    }

    /**
     * Sends a request to an endpoint.
     *
     * @param endpoint the target endpoint
     * @return a request builder
     */
    protected Invocation.Builder sendRequest(final String endpoint) {
        return sendFqeRequest(httpPrefix + ENDPOINT_PREFIX + endpoint, true);
    }

    /**
     * Sends a request to an actuator endpoint.
     *
     * @return a request builder
     */
    protected Invocation.Builder sendActRequest() {
        return sendFqeRequest(httpPrefix + ACTUATOR_ENDPOINT + "v3/api-docs", true);
    }

    /**
     * Sends a request to a Rest Api endpoint, without any authorization header.
     *
     * @param endpoint the target endpoint
     * @return a request builder
     */
    protected Invocation.Builder sendNoAuthRequest(final String endpoint) {
        return sendFqeRequest(httpPrefix + ENDPOINT_PREFIX + endpoint, false);
    }

    /**
     * Sends a request to a fully qualified endpoint.
     *
     * @param fullyQualifiedEndpoint the fully qualified target endpoint
     * @param includeAuth if authorization header should be included
     * @return a request builder
     */
    protected Invocation.Builder sendFqeRequest(final String fullyQualifiedEndpoint, boolean includeAuth) {
        client = ClientBuilder.newBuilder().build();

        client.property(ClientProperties.METAINF_SERVICES_LOOKUP_DISABLE, "true");
        client.register(GsonMessageBodyHandler.class);

        if (includeAuth) {
            client.register(HttpAuthenticationFeature.basic("runtimeUser", "zb!XztG34"));
        }

        final var webTarget = client.target(fullyQualifiedEndpoint);

        return webTarget.request(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN);
    }

    /**
     * Assert that POST call is Unauthorized.
     *
     * @param endPoint the endpoint
     * @param entity the entity of the body
     */
    protected void assertUnauthorizedPost(final String endPoint, final Entity<?> entity) {
        var rawresp = sendNoAuthRequest(endPoint).post(entity);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), rawresp.getStatus());
        rawresp.close();
    }

    /**
     * Assert that PUT call is Unauthorized.
     *
     * @param endPoint the endpoint
     * @param entity the entity ofthe body
     */
    protected void assertUnauthorizedPut(final String endPoint, final Entity<?> entity) {
        var rawresp = sendNoAuthRequest(endPoint).put(entity);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), rawresp.getStatus());
        rawresp.close();
    }

    /**
     * Assert that GET call is Unauthorized.
     *
     * @param endPoint the endpoint
     */
    protected void assertUnauthorizedGet(final String endPoint) {
        var rawresp = sendNoAuthRequest(endPoint).buildGet().invoke();
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), rawresp.getStatus());
        rawresp.close();
    }

    /**
     * Assert that DELETE call is Unauthorized.
     *
     * @param endPoint the endpoint
     */
    protected void assertUnauthorizedDelete(final String endPoint) {
        var rawresp = sendNoAuthRequest(endPoint).delete();
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), rawresp.getStatus());
        rawresp.close();
    }

    /**
     * Set Up httpPrefix.
     *
     * @param port the port
     */
    public void setHttpPrefix(int port) {
        httpPrefix = "http://" + SELF + ":" + port + "/";
    }

}
