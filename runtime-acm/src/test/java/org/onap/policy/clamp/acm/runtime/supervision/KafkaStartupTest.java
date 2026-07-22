
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

import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup;
import org.onap.policy.clamp.acm.runtime.main.parameters.Topics;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaAdmin;

class KafkaStartupTest {

    @Test
    void testKafkaStartup() {
        var topics = new Topics();
        topics.setOperationTopic("operationTopic");
        topics.setSyncTopic("syncTopic");
        var parameterGroup = new AcRuntimeParameterGroup();
        parameterGroup.setTopics(topics);
        parameterGroup.setTopicValidation(true);
        var registry = mock(KafkaListenerEndpointRegistry.class);
        var kafkaAdmin = mock(KafkaAdmin.class);
        var kafkaStartup = new KafkaStartup(registry, kafkaAdmin, parameterGroup);
        kafkaStartup.startListenersWhenReady();
        verify(registry).getListenerContainers();
    }
}
