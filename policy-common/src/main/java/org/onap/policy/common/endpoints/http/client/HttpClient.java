/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2017-2020 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023 Nordix Foundation.
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

package org.onap.policy.common.endpoints.http.client;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.InvocationCallback;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.concurrent.Future;
import org.onap.policy.common.capabilities.Startable;

/**
 * Http Client interface. Supports both synchronous and asynchronous operations.
 *
 */
public interface HttpClient extends Startable {

    /**
     * GET request.
     *
     * @param path context uri path.
     * @return response
     */
    Response get(String path);

    /**
     * GET request.
     *
     * @return response
     */
    Response get();

    /**
     * Asynchronous GET request.
     *
     * @param callback callback to be invoked, asynchronously, when the request completes
     * @param path context uri path
     * @param headers request headers
     *
     * @return future that can be used to cancel the request or await the response
     */
    Future<Response> get(InvocationCallback<Response> callback, String path, Map<String, Object> headers);

    /**
     * Asynchronous GET request.
     *
     * @param callback callback to be invoked, asynchronously, when the request completes
     * @param headers request headers
     * @return future that can be used to cancel the request or await the response
     */
    Future<Response> get(InvocationCallback<Response> callback, Map<String, Object> headers);

    /**
     * PUT request.
     *
     * @param path context uri path
     * @param entity body
     * @param headers headers
     *
     * @return response.
     */
    Response put(String path, Entity<?> entity, Map<String, Object> headers);

    /**
     * Asynchronous PUT request.
     *
     * @param callback callback to be invoked, asynchronously, when the request completes
     * @param path context uri path
     * @param entity body
     * @param headers headers
     *
     * @return future that can be used to cancel the request or await the response
     */
    Future<Response> put(InvocationCallback<Response> callback, String path, Entity<?> entity,
                    Map<String, Object> headers);

    /**
     * POST request.
     *
     * @param path context uri path
     * @param entity body
     * @param headers headers
     *
     * @return response.
     */
    Response post(String path, Entity<?> entity, Map<String, Object> headers);

    /**
     * Asynchronous POST request.
     *
     * @param callback callback to be invoked, asynchronously, when the request completes
     * @param path context uri path
     * @param entity body
     * @param headers headers
     *
     * @return future that can be used to cancel the request or await the response
     */
    Future<Response> post(InvocationCallback<Response> callback, String path, Entity<?> entity,
                    Map<String, Object> headers);

    /**
     * DELETE request.
     *
     * @param path context uri path
     * @param headers headers
     *
     * @return response.
     */
    Response delete(String path, Map<String, Object> headers);

    /**
     * Asynchronous DELETE request.
     *
     * @param callback callback to be invoked, asynchronously, when the request completes
     * @param path context uri path
     * @param headers headers
     *
     * @return future that can be used to cancel the request or await the response
     */
    Future<Response> delete(InvocationCallback<Response> callback, String path, Map<String, Object> headers);

    /**
     * Retrieve the body from the HTTP transaction.
     *
     * @param response response.
     * @param entityType body type.
     * @param <T> body class.
     *
     * @return response.
     */
    static <T> T getBody(Response response, Class<T> entityType) {
        return response.readEntity(entityType);
    }

    /**
     * Get the client name.
     * @return name
     */
    String getName();

    /**
     * HTTPS support.
     *
     * @return if the client uses https
     */
    boolean isHttps();

    /**
     * Self-signed certificates.
     *
     * @return if the self-signed certificates are allowed
     */
    boolean isSelfSignedCerts();

    /**
     * Get the host name.
     *
     * @return host name
     */
    String getHostname();

    /**
     * Get the port.
     *
     * @return port
     */
    int getPort();

    /**
     * Get the base path.
     *
     * @return base path
     */
    String getBasePath();

    /**
     * Get the username.
     *
     * @return the username
     */
    String getUserName();

    /**
     * Get the password.
     *
     * @return the password
     */
    String getPassword();

    /**
     * Get the base URL.
     *
     * @return the base URL
     */
    String getBaseUrl();

    /**
     * Gets a web target associated with the base URL.
     *
     * @return a webtarget
     */
    WebTarget getWebTarget();
}
