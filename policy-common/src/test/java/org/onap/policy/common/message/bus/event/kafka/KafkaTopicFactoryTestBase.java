/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022-2024 Nordix Foundation.
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
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.common.message.bus.event.kafka;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Collections;
import org.onap.policy.common.message.bus.event.Topic;
import org.onap.policy.common.message.bus.event.base.BusTopicFactoryTestBase;

/**
 * Base class for KafkaTopicXxxFactory tests.
 *
 * @param <T> type of topic managed by the factory
 */
public abstract class KafkaTopicFactoryTestBase<T extends Topic> extends BusTopicFactoryTestBase<T> {

    @Override
    public void testBuildBusTopicParams_Ex() {

        super.testBuildBusTopicParams_Ex();

        // null servers
        assertThatIllegalArgumentException().as("null servers")
                        .isThrownBy(() -> buildTopic(makeBuilder().servers(null).build()));

        // empty servers
        assertThatIllegalArgumentException().as("empty servers")
                        .isThrownBy(() -> buildTopic(makeBuilder().servers(Collections.emptyList()).build()));
    }
}
