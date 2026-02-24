/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024,2026 OpenInfra Foundation Europe. All rights reserved.
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

import java.util.Base64;
import org.onap.policy.common.utils.network.NetworkUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

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

    /**
     * Verifies that an endpoint appears within the swagger response.
     *
     * @param endpoint the endpoint of interest
     */
    protected void testSwagger(final String endpoint) {
        var resp = sendGet(httpPrefix + ACTUATOR_ENDPOINT + "v3/api-docs", true, false)
                .retrieve()
                .body(String.class);

        assertThat(resp).contains(endpoint);
    }

    /**
     * Sends a GET request to an endpoint.
     *
     * @param endpoint the target endpoint
     * @return a RequestHeadersSpec
     */
    protected RestClient.RequestHeadersSpec<?> sendGet(final String endpoint) {
        return sendGet(httpPrefix + ENDPOINT_PREFIX + endpoint, true, true);
    }

    private RestClient.RequestHeadersSpec<?> sendGet(String fullUrl, boolean includeAuth, boolean suppressErrors) {
        return getRestClient(includeAuth, suppressErrors).get().uri(fullUrl);
    }

    /**
     * Sends a GET request to an endpoint without authorization.
     *
     * @param endpoint the target endpoint
     * @return a RequestHeadersSpec
     */
    protected RestClient.RequestHeadersSpec<?> sendGetNoAuth(final String endpoint) {
        return sendGet(httpPrefix + ENDPOINT_PREFIX + endpoint, false, false);
    }

    /**
     * Sends a POST request to an endpoint.
     *
     * @param endpoint the target endpoint
     * @return a RequestBodySpec
     */
    protected RestClient.RequestBodySpec sendPost(final String endpoint) {
        return sendPost(httpPrefix + ENDPOINT_PREFIX + endpoint, true, true);
    }

    private RestClient.RequestBodySpec sendPost(String fullUrl, boolean includeAuth, boolean suppressErrors) {
        return getRestClient(includeAuth, suppressErrors).post().uri(fullUrl);
    }

    /**
     * Sends a POST request to an endpoint without authorization.
     *
     * @param endpoint the target endpoint
     * @return a RequestBodySpec
     */
    protected RestClient.RequestBodySpec sendPostNoAuth(final String endpoint) {
        return sendPost(httpPrefix + ENDPOINT_PREFIX + endpoint, false, false);
    }

    /**
     * Sends a PUT request to an endpoint.
     *
     * @param endpoint the target endpoint
     * @return a RequestBodySpec
     */
    protected RestClient.RequestBodySpec sendPut(final String endpoint) {
        return sendPut(httpPrefix + ENDPOINT_PREFIX + endpoint, true, true);
    }

    private RestClient.RequestBodySpec sendPut(String fullUrl, boolean includeAuth, boolean suppressErrors) {
        return getRestClient(includeAuth, suppressErrors).put().uri(fullUrl);
    }

    /**
     * Sends a PUT request to an endpoint without authorization.
     *
     * @param endpoint the target endpoint
     * @return a RequestBodySpec
     */
    protected RestClient.RequestBodySpec sendPutNoAuth(final String endpoint) {
        return sendPut(httpPrefix + ENDPOINT_PREFIX + endpoint, false, false);
    }

    /**
     * Sends a DELETE request to an endpoint.
     *
     * @param endpoint the target endpoint
     * @return a RequestHeadersSpec
     */
    protected RestClient.RequestHeadersSpec<?> sendDelete(final String endpoint) {
        return sendDelete(httpPrefix + ENDPOINT_PREFIX + endpoint, true, true);
    }

    private RestClient.RequestHeadersSpec<?> sendDelete(String fullUrl, boolean includeAuth, boolean suppressErrors) {
        return getRestClient(includeAuth, suppressErrors).delete().uri(fullUrl);
    }

    /**
     * Sends a DELETE request to an endpoint without authorization.
     *
     * @param endpoint the target endpoint
     * @return a RequestHeadersSpec
     */
    protected RestClient.RequestHeadersSpec<?> sendDeleteNoAuth(final String endpoint) {
        return sendDelete(httpPrefix + ENDPOINT_PREFIX + endpoint, false, false);
    }

    /**
     * Assert that POST call is Unauthorized.
     *
     * @param endPoint the endpoint
     * @param body the body
     */
    protected void assertUnauthorizedPost(final String endPoint, final Object body) {
        try {
            sendPostNoAuth(endPoint).body(body).retrieve().toBodilessEntity();
            throw new AssertionError("Expected HttpClientErrorException was not thrown");
        } catch (HttpClientErrorException ex) {
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        }
    }

    /**
     * Assert that PUT call is Unauthorized.
     *
     * @param endPoint the endpoint
     * @param body the body
     */
    protected void assertUnauthorizedPut(final String endPoint, final Object body) {
        try {
            sendPutNoAuth(endPoint).body(body).retrieve().toBodilessEntity();
            throw new AssertionError("Expected HttpClientErrorException was not thrown");
        } catch (HttpClientErrorException ex) {
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        }
    }

    /**
     * Assert that GET call is Unauthorized.
     *
     * @param endPoint the endpoint
     */
    protected void assertUnauthorizedGet(final String endPoint) {
        try {
            sendGetNoAuth(endPoint).retrieve().toBodilessEntity();
            throw new AssertionError("Expected HttpClientErrorException was not thrown");
        } catch (HttpClientErrorException ex) {
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        }
    }

    /**
     * Assert that DELETE call is Unauthorized.
     *
     * @param endPoint the endpoint
     */
    protected void assertUnauthorizedDelete(final String endPoint) {
        try {
            sendDeleteNoAuth(endPoint).retrieve().toBodilessEntity();
            throw new AssertionError("Expected HttpClientErrorException was not thrown");
        } catch (HttpClientErrorException ex) {
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        }
    }

    /**
     * Set Up httpPrefix.
     *
     * @param port the port
     */
    public void setHttpPrefix(int port) {
        httpPrefix = "http://" + SELF + ":" + port + "/";
    }

    private RestClient getRestClient(boolean includeAuth, boolean suppressErrors) {
        var builder = RestClient.builder()
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE);
        
        if (includeAuth) {
            builder.defaultHeader("Authorization", "Basic "
                    + Base64.getEncoder().encodeToString("runtimeUser:zb!XztG34".getBytes()));
        }
        
        if (suppressErrors) {
            builder.defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                // Allow tests to handle error responses without throwing exceptions
            });
        }
        
        return builder.build();
    }

}
