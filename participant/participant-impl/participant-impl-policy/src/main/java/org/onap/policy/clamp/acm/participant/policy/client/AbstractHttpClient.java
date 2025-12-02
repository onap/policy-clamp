/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021, 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.participant.policy.client;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.onap.policy.clamp.acm.participant.policy.main.parameters.RestClientParameters;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public abstract class AbstractHttpClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractHttpClient.class);
    private static final Coder CODER = new StandardCoder();
    private final RestClientParameters parameters;

    protected String executePost(String path, final Object entity) {
        var webClient = WebClient.builder().baseUrl(this.getBaseUrl())
                .defaultHeaders(this::headersConsumer).build();
        return webClient.post()
                .uri(path)
                .body(BodyInserters.fromValue(encode(entity)))
                .exchangeToMono(this::responseHandler).block();
    }

    private String encode(Object entity) {
        try {
            return CODER.encode(entity);
        } catch (CoderException e) {
            throw new WebClientResponseException(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null, null, null);
        }
    }

    private String getBaseUrl() {
        return "http://" + parameters.getHostname() + ":" + parameters.getPort();
    }

    private Mono<String> responseHandler(ClientResponse clientResponse) {
        if (clientResponse.statusCode().is2xxSuccessful()) {
            return clientResponse.bodyToMono(String.class);
        } else {
            LOGGER.error("Invocation Post failed Response status: {}", clientResponse.statusCode());
            return clientResponse.createException().flatMap(Mono::error);
        }
    }

    private void headersConsumer(HttpHeaders headers) {
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.isNotBlank(parameters.getUserName())
                && StringUtils.isNotBlank(parameters.getPassword())) {
            headers.setBasicAuth(parameters.getUserName(), parameters.getPassword());
        }
    }

    protected void executeDelete(String path) {
        var webClient = WebClient.builder().baseUrl(this.getBaseUrl())
                .defaultHeaders(this::headersConsumer).build();
        webClient.delete()
                .uri(path)
                .exchangeToMono(this::responseDeleteHandler).block();
    }

    private Mono<Void> responseDeleteHandler(ClientResponse clientResponse) {
        if (clientResponse.statusCode().is2xxSuccessful()) {
            return clientResponse.releaseBody();
        } else {
            LOGGER.error("Invocation Delete failed Response status: {}", clientResponse.statusCode());
            return clientResponse.createException().flatMap(Mono::error);
        }
    }
}
