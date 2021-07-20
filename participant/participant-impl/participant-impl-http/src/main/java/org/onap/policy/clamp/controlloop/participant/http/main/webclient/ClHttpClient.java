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

package org.onap.policy.clamp.controlloop.participant.http.main.webclient;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.onap.policy.clamp.controlloop.participant.http.main.exception.HttpWebClientException;
import org.onap.policy.clamp.controlloop.participant.http.main.handler.ControlLoopElementHandler;
import org.onap.policy.clamp.controlloop.participant.http.main.models.ConfigRequest;
import org.onap.policy.clamp.controlloop.participant.http.main.models.ConfigurationEntity;
import org.onap.policy.clamp.controlloop.participant.http.main.models.RestParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

public class ClHttpClient implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private ConfigRequest configRequest;

    // Default timeout
    private int requestTimeout = 30;

    /**
     * Constructor.
     */
    public ClHttpClient(ConfigRequest configRequest) {
        this.configRequest = configRequest;
    }

    /**
     * Runnable to execute http requests.
     */
    @Override
    public void run() {

        var webClient = WebClient.builder()
            .baseUrl(configRequest.getBaseUrl())
            .defaultHeaders(httpHeaders -> httpHeaders.addAll(createHeaders(configRequest)))
            .build();

        if (configRequest.getUninitializedToPassiveTimeout() != 0) {
            requestTimeout = configRequest.getUninitializedToPassiveTimeout();
        }

        for (ConfigurationEntity configurationEntity : configRequest.getConfigurationEntities()) {
            LOGGER.info("Executing http requests for the config entity {}",
                configurationEntity.getConfigurationEntityId());

            executeRequest(webClient, configurationEntity, requestTimeout);
        }
    }

    private void executeRequest(WebClient client, ConfigurationEntity configurationEntity, int requestTimeout)  {

        // Iterate the sequence of http requests
        for (RestParams request: configurationEntity.getRestSequence()) {
            Map<Integer, String> restResponse = new HashMap<>();
            String response = null;
            try {
                var httpMethod = Objects.requireNonNull(HttpMethod.resolve(request.getHttpMethod()));
                var uri = createUriString(request);

                // For POST, PUT
                if (request.getBody() != null) {
                    LOGGER.info("Executing HTTP request: {} for the Rest request id: {}", httpMethod,
                        request.getRestRequestId());

                    response = client.method(httpMethod)
                        .uri(uri)
                        .body(BodyInserters.fromValue(request.getBody()))
                        .exchangeToMono(clientResponse ->
                            clientResponse.statusCode().value() == request.getExpectedResponse()
                                ? clientResponse.bodyToMono(String.class)
                                : Mono.error(new HttpWebClientException(clientResponse.statusCode().value(),
                                clientResponse.bodyToMono(String.class).toString())))
                        .block(Duration.ofMillis(requestTimeout * 1000L));

                } else {
                    LOGGER.info("Executing HTTP request: {} for the Rest request id: {}", httpMethod,
                        request.getRestRequestId());
                    response = client.method(httpMethod)
                        .uri(uri)
                        .exchangeToMono(clientResponse ->
                            clientResponse.statusCode().value() == request.getExpectedResponse()
                                ? clientResponse.bodyToMono(String.class)
                                : Mono.error(new HttpWebClientException(clientResponse.statusCode().value(),
                                clientResponse.bodyToMono(String.class).toString())))
                        .block(Duration.ofMillis(requestTimeout * 1000L));

                }
                LOGGER.info("HTTP response for the {} request : {}", httpMethod, response);

                restResponse.put(request.getExpectedResponse(), response);
                ControlLoopElementHandler.getRestResponseMap().put(request.getRestRequestId(), restResponse);

            } catch (HttpWebClientException ex) {
                LOGGER.error("Error occurred on the HTTP request ", ex);
                restResponse.put(ex.getStatusCode().value(), ex.getResponseBodyAsString());
                ControlLoopElementHandler.getRestResponseMap().put(request.getRestRequestId(), restResponse);
            }
        }
    }

    private HttpHeaders createHeaders(ConfigRequest request) {
        var headers = new HttpHeaders();
        for (Map.Entry<String, String> entry: request.getHttpHeaders().entrySet()) {
            headers.add(entry.getKey(), entry.getValue());
        }
        return headers;
    }

    private String createUriString(RestParams restParams) {
        var uriComponentsBuilder = UriComponentsBuilder.fromUriString(restParams.getPath());
        // Add path params if present
        if (restParams.getPathParams() != null) {
            uriComponentsBuilder.uriVariables(restParams.getPathParams());
        }
        // Add query params if present
        if (restParams.getQueryParams() != null) {
            for (Map.Entry<String, String> entry : restParams.getQueryParams().entrySet()) {
                uriComponentsBuilder.queryParam(entry.getKey(), entry.getValue());
            }
        }
        return uriComponentsBuilder.build().toUriString();
    }

}
