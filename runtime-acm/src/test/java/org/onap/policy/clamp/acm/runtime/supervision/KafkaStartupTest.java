
/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.runtime.supervision;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.core.retry.RetryState;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;

class KafkaStartupTest {

    @Test
    void testKafkaStartupFail() {
        var registry = mock(KafkaListenerEndpointRegistry.class);
        var kafkaStartup = new KafkaStartup(registry);
        var retryState = mock(RetryState.class);
        when(retryState.isSuccessful()).thenReturn(false);
        when(retryState.getLastException()).thenReturn(new RuntimeException());
        kafkaStartup.checkFailed(retryState);
        verify(registry).getListenerContainers();
    }

    @Test
    void testKafkaStartupSuccess() {
        var registry = mock(KafkaListenerEndpointRegistry.class);
        var kafkaStartup = new KafkaStartup(registry);
        kafkaStartup.startListenersWhenReady();
        verify(registry).getListenerContainers();
    }
}
