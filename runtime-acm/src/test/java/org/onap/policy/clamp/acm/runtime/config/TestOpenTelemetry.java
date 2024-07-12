/*-
 * ============LICENSE_START===============================================
 *  Copyright (C) 2024 Nordix Foundation.
 * ========================================================================
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
 * ============LICENSE_END=================================================
 */

package org.onap.policy.clamp.acm.runtime.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("tracing")
@SpringBootTest(classes = OpenTelConfiguration.class)
class TestOpenTelemetry {

    @Autowired
    ApplicationContext context;

    @Test
    void testOpenTelemetry() {
        assertThat(context).isNotNull();
        assertTrue(context.containsBean("otlpGrpcSpanExporter"));
        assertTrue(context.containsBean("jaegerRemoteSampler"));
        assertFalse(context.containsBean("otlpHttpSpanExporter"));
    }
}
