/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
 *  Modifications Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.participant.kserve.config;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.Config;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiClientConfig {

    /**
     * Default Api Client bean creation.
     *
     * <p>This bean is mandatory: {@code KserveClient} requires an {@link ApiClient} to be
     * present in the context. It used to be provided by the Spring Cloud Kubernetes
     * client starter, which was removed in POLICY-5552 in favour of the direct
     * {@code io.kubernetes:client-java} dependency. Without this bean the application
     * fails to start with "No qualifying bean of type ApiClient". It must therefore be
     * created unconditionally (not gated behind a Spring profile that is never active in
     * the deployed image).</p>
     *
     * @return ApiClient
     * @throws IOException exception
     */
    @Bean
    @ConditionalOnMissingBean(ApiClient.class)
    public ApiClient defaultApiClient() throws IOException {
        return Config.defaultClient();
    }
}
