/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.a1pms.webclient;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.participant.a1pms.exception.A1PolicyServiceException;
import org.onap.policy.clamp.acm.participant.a1pms.models.A1PolicyServiceEntity;
import org.onap.policy.clamp.acm.participant.a1pms.parameters.A1PmsParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AcA1PmsClient {

    private final A1PmsParameters a1PmsParameters;

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Get webclient for A1PMS.
     *
     * @return webClient
     */
    private WebClient getPmsClient() {
        return WebClient.builder().baseUrl(a1PmsParameters.getBaseUrl())
                       .defaultHeaders(httpHeaders -> httpHeaders.addAll(createHeaders())).build();
    }

    /**
     * Get A1PMS health status.
     *
     * @return whether A1PMS is healthy
     */
    public boolean isPmsHealthy() {
        return Objects.equals(Boolean.TRUE,
                getPmsClient().method(HttpMethod.GET).uri(a1PmsParameters.getEndpoints().getHealth())
                        .exchangeToMono(clientResponse -> Mono.just(clientResponse.statusCode().is2xxSuccessful()))
                        .block());
    }


    /**
     * Create service in A1PMS.
     * @param policyServiceEntities List of service entities
     * @throws A1PolicyServiceException Exception on creating service
     */
    public void createService(List<A1PolicyServiceEntity> policyServiceEntities) throws A1PolicyServiceException {
        policyServiceEntities.forEach(
                a1PolicyServiceEntity -> getPmsClient().method(HttpMethod.PUT)
                                           .uri(a1PmsParameters.getEndpoints().getServices())
                                           .bodyValue(a1PolicyServiceEntity).retrieve()
                                           .onStatus(HttpStatus::isError,
                                                   clientResponse -> Mono.error(new A1PolicyServiceException(
                                                                   clientResponse.statusCode().value(),
                                                                   "Error in creating policy service")))
                                           .onStatus(Predicate.isEqual(HttpStatus.OK),
                                                   clientResponse -> {
                                                       LOGGER.warn("Client {} already exists and the configuration "
                                                                           + "is updated",
                                                               a1PolicyServiceEntity.getClientId());
                                                       return Mono.empty();
                                                   })
                                           .toBodilessEntity()
                                           .block());
    }

    /**
     * Delete service in A1PMS.
     * @param policyServiceEntities List of service entities
     * @throws A1PolicyServiceException Exception on deleting service
     */
    public void deleteService(List<A1PolicyServiceEntity> policyServiceEntities) throws A1PolicyServiceException {
        policyServiceEntities.forEach(
                a1PolicyServiceEntity -> getPmsClient().method(HttpMethod.DELETE)
                                             .uri(a1PmsParameters.getEndpoints().getService(),
                                                     a1PolicyServiceEntity.getClientId())
                                             .bodyValue(a1PolicyServiceEntity).retrieve()
                                             .onStatus(HttpStatus::isError,
                                                     clientResponse -> Mono.error(
                                                             new A1PolicyServiceException(
                                                                     clientResponse.statusCode().value(),
                                                                     "Error in deleting policy service")))
                                             .toBodilessEntity()
                                             .block());
    }

    /**
     * Prepare the Http headers to call A1PMS.
     *
     * @return httpHeaders
     */
    private HttpHeaders createHeaders() {
        var headers = new HttpHeaders();
        for (Map.Entry<String, String> entry : a1PmsParameters.getHeaders().entrySet()) {
            headers.add(entry.getKey(), entry.getValue());
        }
        return headers;
    }
}
