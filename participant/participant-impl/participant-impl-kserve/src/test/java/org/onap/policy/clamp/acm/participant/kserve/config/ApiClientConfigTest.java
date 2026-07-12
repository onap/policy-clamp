/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2026 Deutsche Telekom. All rights reserved.
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

import static org.assertj.core.api.Assertions.assertThat;

import io.kubernetes.client.openapi.ApiClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ApiClientConfigTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(ApiClientConfig.class);

    /**
     * KserveClient has a mandatory constructor dependency on {@link ApiClient}. Before
     * POLICY-5552 the bean was supplied by the Spring Cloud Kubernetes client starter;
     * after it was removed, {@link ApiClientConfig} is the only provider. This asserts
     * the bean is created with no active profile - i.e. exactly the deployed image's
     * runtime (default profile). With the previous {@code @Profile("kubernetes")} guard
     * this context would contain no ApiClient and the participant failed to start with
     * "No qualifying bean of type ApiClient".
     */
    @Test
    void apiClientBeanIsCreatedWithNoActiveProfile() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ApiClient.class);
        });
    }
}
