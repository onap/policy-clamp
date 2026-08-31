/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.runtime.config

import io.opentelemetry.sdk.trace.samplers.Sampler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

/**
 * The effective {@link Sampler} must be the one Spring Boot autoconfigures from
 * {@code management.tracing.sampling.probability}.
 *
 * <p>Any application-declared {@code Sampler} bean silently wins over Boot's
 * {@code @ConditionalOnMissingBean otelSampler()}, which makes that property inert.
 */
@SpringBootTest(properties = [
        "management.tracing.export.enabled=true",
        // no collector is reachable from a unit test, so keep the OTLP exporter out of the context
        "management.tracing.export.otlp.enabled=false",
        "management.tracing.sampling.probability=0.25",
        // A deployment may still inject this; it must not displace the probability sampler.
        "management.opentelemetry.tracing.jaeger-remote-sampler.endpoint=http://jaeger:14250"
])
// Without this, Boot's TracingContextCustomizerFactory force-sets
// management.tracing.export.enabled=false at a precedence above @SpringBootTest properties.
@AutoConfigureTracing
@EmbeddedKafka
@ActiveProfiles(["test", "default"])
@DirtiesContext
class TracingSamplerSpec extends Specification {

    @Autowired
    Sampler sampler

    def "the effective sampler honours management.tracing.sampling.probability"() {
        expect: "Spring Boot's autoconfigured parent-based ratio sampler, not a remote or always-off one"
        sampler.description == Sampler.parentBased(Sampler.traceIdRatioBased(0.25d)).description
    }
}
