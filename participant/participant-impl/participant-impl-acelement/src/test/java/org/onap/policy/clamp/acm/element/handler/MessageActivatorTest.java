/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Nordix Foundation.
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

package org.onap.policy.clamp.acm.element.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.endpoints.parameters.TopicParameterGroup;
import org.onap.policy.common.endpoints.parameters.TopicParameters;

class MessageActivatorTest {

    @Test
    void test() throws Exception {
        var listener = mock(MessageListener.class);
        var publisher = mock(MessagePublisher.class);
        try (var messageActivator = new MessageActivator(listener, publisher)) {

            var topicParameters = new TopicParameters();
            topicParameters.setTopic("topic");
            topicParameters.setServers(List.of("localhost"));
            topicParameters.setFetchTimeout(1000);
            topicParameters.setTopicCommInfrastructure("dmaap");

            var parameters = new TopicParameterGroup();
            parameters.setTopicSinks(List.of(topicParameters));
            parameters.setTopicSources(List.of(topicParameters));

            messageActivator.activate(parameters);
            assertThat(messageActivator.isAlive()).isTrue();

            messageActivator.deactivate();
            assertThat(messageActivator.isAlive()).isFalse();
        }
    }

}
