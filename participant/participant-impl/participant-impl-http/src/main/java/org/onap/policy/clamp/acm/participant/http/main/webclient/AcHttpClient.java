/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021,2023 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.http.main.webclient;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.clamp.acm.participant.http.main.exception.HttpWebClientException;
import org.onap.policy.clamp.acm.participant.http.main.models.ConfigRequest;
import org.onap.policy.clamp.acm.participant.http.main.models.ConfigurationEntity;
import org.onap.policy.clamp.acm.participant.http.main.models.RestParams;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Component
public class AcHttpClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Runnable to execute http requests.
     */
    public void run(ConfigRequest configRequest, Map<ToscaConceptIdentifier, Pair<Integer, String>> responseMap) {

        var webClient = WebClient.builder().baseUrl(configRequest.getBaseUrl())
                .defaultHeaders(httpHeaders -> httpHeaders.addAll(createHeaders(configRequest))).build();

        for (var configurationEntity : configRequest.getConfigurationEntities()) {
            LOGGER.info("Executing http requests for the config entity {}",
                    configurationEntity.getConfigurationEntityId());

            executeRequest(webClient, configRequest, configurationEntity, responseMap);
        }
    }

    private void executeRequest(WebClient client, ConfigRequest configRequest, ConfigurationEntity configurationEntity,
            Map<ToscaConceptIdentifier, Pair<Integer, String>> responseMap) {

        // Iterate the sequence of http requests
        for (var request : configurationEntity.getRestSequence()) {
            try {
                var httpMethod = Objects.requireNonNull(HttpMethod.resolve(request.getHttpMethod()));
                var uri = createUriString(request);
                LOGGER.info("Executing HTTP request: {} for the Rest request id: {}", httpMethod,
                        request.getRestRequestId());

                var response = client.method(httpMethod).uri(uri)
                        .body(request.getBody() == null ? BodyInserters.empty()
                                : BodyInserters.fromValue(request.getBody()))
                        .exchangeToMono(
                                clientResponse -> clientResponse.statusCode().value() == request.getExpectedResponse()
                                        ? clientResponse.bodyToMono(String.class)
                                        : Mono.error(new HttpWebClientException(clientResponse.statusCode().value(),
                                                clientResponse.bodyToMono(String.class).toString())))
                        .block(Duration.ofMillis(configRequest.getUninitializedToPassiveTimeout() * 1000L));

                LOGGER.info("HTTP response for the {} request : {}", httpMethod, response);
                responseMap.put(request.getRestRequestId(),
                        new ImmutablePair<>(request.getExpectedResponse(), response));

            } catch (HttpWebClientException ex) {
                LOGGER.error("Error occurred on the HTTP response ", ex);
                responseMap.put(request.getRestRequestId(),
                        new ImmutablePair<>(ex.getStatusCode().value(), ex.getResponseBodyAsString()));
            } catch (WebClientRequestException | IllegalStateException ex) {
                LOGGER.error("Error occurred on the HTTP request ", ex);
                responseMap.put(request.getRestRequestId(), new ImmutablePair<>(404, ex.getMessage()));
            }
        }
    }

    private HttpHeaders createHeaders(ConfigRequest request) {
        var headers = new HttpHeaders();
        for (var entry : request.getHttpHeaders().entrySet()) {
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
            for (var entry : restParams.getQueryParams().entrySet()) {
                uriComponentsBuilder.queryParam(entry.getKey(), entry.getValue());
            }
        }
        return uriComponentsBuilder.build().toUriString();
    }

}
