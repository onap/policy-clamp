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

package org.onap.policy.clamp.acm.participant.a1.webclient;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.participant.a1.exception.PolicyServiceException;
import org.onap.policy.clamp.acm.participant.a1.models.PolicyServiceEntity;
import org.onap.policy.clamp.acm.participant.a1.parameters.PmsParameters;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AcPmsClient {

    private final PmsParameters pmsParameters;

    /**
     * Get webclient for PMS.
     *
     * @return webClient
     */
    private WebClient getPmsClient() {
        return WebClient.builder().baseUrl(pmsParameters.getBaseUrl())
                       .defaultHeaders(httpHeaders -> httpHeaders.addAll(createHeaders())).build();
    }

    /**
     * Get PMS health status.
     *
     * @return whether PMS is healthy
     */
    public boolean isPmsHealthy() {
        return Objects.equals(Boolean.TRUE,
                getPmsClient().method(HttpMethod.GET).uri(pmsParameters.getEndpoints().getHealth())
                        .exchangeToMono(clientResponse -> Mono.just(clientResponse.statusCode().is2xxSuccessful()))
                        .block());
    }


    /**
     * Create service in PMS.
     * @param policyServiceEntities List of service entities
     * @throws PolicyServiceException Exception on creating service
     */
    public void createService(List<PolicyServiceEntity> policyServiceEntities) throws PolicyServiceException {
        policyServiceEntities.forEach(policyServiceEntity ->
                                              getPmsClient().method(HttpMethod.PUT)
                                                 .uri(pmsParameters.getEndpoints().getServices())
                                                 .bodyValue(policyServiceEntity).retrieve()
                                                 .onStatus(HttpStatus::isError,
                                                         clientResponse -> Mono.error(
                                                                 new PolicyServiceException(
                                                                         clientResponse.statusCode().value(),
                                                                         "Error in creating policy service")))
                                                 .toBodilessEntity().block());

    }

    /**
     * Delete service in PMS.
     * @param policyServiceEntities List of service entities
     * @throws PolicyServiceException Exception on deleting service
     */
    public void deleteService(List<PolicyServiceEntity> policyServiceEntities) throws PolicyServiceException {
        policyServiceEntities.forEach(policyServiceEntity ->
                                              getPmsClient().method(HttpMethod.DELETE)
                                                 .uri(pmsParameters.getEndpoints().getService(),
                                                         policyServiceEntity.getServiceId())
                                                 .bodyValue(policyServiceEntity).retrieve()
                                                 .onStatus(HttpStatus::isError,
                                                         clientResponse -> Mono.error(
                                                                 new PolicyServiceException(
                                                                         clientResponse.statusCode().value(),
                                                                         "Error in deleting policy service")))
                                                 .toBodilessEntity().block());
    }

    /**
     * Prepare the Http headers to call PMS.
     *
     * @return httpHeaders
     */
    private HttpHeaders createHeaders() {
        var headers = new HttpHeaders();
        for (Map.Entry<String, String> entry : pmsParameters.getHeaders().entrySet()) {
            headers.add(entry.getKey(), entry.getValue());
        }
        return headers;
    }
}
